package com.pb.models.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;
import static com.pb.models.pt.StopDurationParameters.*;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * Intermediate Stop Duration Model
 * 
 * The intermediate stop duration model predicts the lengths of intermediate
 * stops after considering time window availibility.
 * 
 * @author Stryker
 */
public class StopDurationModel extends TimedModel {
    static Logger logger = Logger.getLogger(StopDurationModel.class);

    private Tracer tracer = Tracer.getTracer();

    private boolean trace = false;

    protected LogitModel durationModel;

    private float[][] parameters;

    private final int periods;

    private Tour tour;

    private Activity stop;

    private SkimsInMemory skims;

    private ResourceBundle rb;

    private final int morning;

    private final int midday;

    private final int evening;

    private final int night;

    /**
     * Constructor.
     * @param appRb PT ResourceBundle
     * @param globalRb Global ResourceBundle
     */
    public StopDurationModel(ResourceBundle appRb, ResourceBundle globalRb) {
        startTiming();
        this.rb = appRb;

        morning = Integer.parseInt(ResourceUtil.getProperty(globalRb, "am.peak.start"));
        midday = Integer.parseInt(ResourceUtil.getProperty(globalRb, "offpeak.start"));
        evening = Integer.parseInt(ResourceUtil.getProperty(globalRb, "pm.peak.start"));
        night = Integer.parseInt(ResourceUtil.getProperty(globalRb, "nt.peak.start"));
        periods = ALTERNATIVES;

        readParameters();
        endTiming();
    }

    /**
     * Read parameters from the PT properities file.
     */
    public void readParameters() {
        startTiming();
        String fileName = ResourceUtil.getProperty(rb,
                "sdt.stop.duration.parameters");

        logger.info("Reading stop duration parameters file " + fileName);

        try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fileName));
            parameters = table.getValues();
        } catch (IOException e) {
            logger.fatal("Can not read stop duration parameters file "
                    + fileName);
            logger.fatal(e);
            throw new RuntimeException(e);
        }

        if (tracer.isTraceOn()) {
            logger.info("Read parameter values (across by purpose)");
            for (int j = 0; j < parameters[0].length; ++j) {
                String s = "" + j;
                for (float[] parameter : parameters) {
                    s += "," + parameter[j];
                }
                logger.info(s);
            }
        }
        endTiming();
    }

    /**
     * Build the model.
     * 
     * Since the alternatives are simple and pretty much describe themselves,
     * they are instances of a ConcreteAlternative class.
     * @param skims SkimsInMemory object
     */
    public void buildModel(SkimsInMemory skims) {
        startTiming();
        durationModel = new LogitModel("Stop duration model");

        durationModel.addAlternative(new ConcreteAlternative(
                "less than 1 period", 0));

        for (int i = 1; i < periods; ++i) {
            durationModel.addAlternative(new ConcreteAlternative(i + " period",
                    i));
        }
        
        this.skims=skims;
        endTiming();
    }

    /**
     * Calculate the utilities.
     * @param person PTPerson
     * @param tour Tour object
     * @param stop Stop Activity
     * @return double utilities for Stop.
     */
    public double calculateUtilities(PTPerson person, Tour tour, Activity stop) {
        startTiming();
        this.tour = tour;
        this.stop = stop;
        trace = tracer.isTracePerson(person.hhID + "_" + person.memberID);

        ActivityPurpose purpose = tour.primaryDestination.activityPurpose;

        // constrain the stop by tour window and travel time
        int start = getStopWindowStart(tour, stop) / 100;
        int end = getStopWindowEnd(tour, stop) / 100;
        int maxDuration = end - start;

        if (trace) {
            logger.info("Calculating stop duration utilities for " + (person.hhID + "_" + person.memberID)
                    + " on tour " + tour.getOrder());
            logger.info("The tour has purpose: " + purpose);
            logger.info("The window for the stop begins at " + start
                    + " and ends at " + end);
         }

        // calculate utilities for each alternative
        // note that most parameters are interacted with the duration
        for (int duration = 0; duration < periods; ++duration) {
            ConcreteAlternative alt = (ConcreteAlternative) durationModel
                    .getAlternative(duration);
            float param;
            boolean outbound = stop == tour.intermediateStop1;

            alt.setAvailability(true);
            // the zero period alternative is the base alternative -- ensure
            // that it is available, set the availability and move to the next
            // alternative
            if (duration == 0) {
                alt.setAvailability(true);
                alt.setUtility(0);

                if (trace) {
                    logger.info("Utility for a 0-period alternative set to 0.");
                }

                continue;
            }

            // tackle non-availbility
            if (duration > maxDuration) {
                alt.setAvailability(false);

                if (trace) {
                    logger.info("Duration " + duration + " is not available.");
                }

                continue;
            }

            // utility calculation - ASC first
            param = parameters[purpose.ordinal()][duration + 1];
            double utility = param;

            if (trace) {
                logger.info("Stop duration utility "+duration);
                logger.info("  ASC: " + utility);
            }

            // outbound stop
            param = parameters[purpose.ordinal()][OUTBOUND];
            if (outbound && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  outbound stop: " + term);
                }
            }

            // morning stop
            param = parameters[purpose.ordinal()][MORNING];
            if (start <= morning && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  morning stop: " + term);
                }
            }

            // adult worker
            param = parameters[purpose.ordinal()][ADULT_WORKER];
            if (person.employed && person.age > 18 && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  adult worker: " + term);
                }
            }

            // time pressure
            int tours = person.weekdayTours.length;
            Pattern pattern = person.weekdayPattern;
            int acts = pattern.homeActivities + pattern.workActivities
                    + pattern.schoolActivities + pattern.shopActivities
                    + pattern.recreateActivities + pattern.otherActivities;
            int stops = acts - tours * 3;

            // a two tour day
            param = parameters[purpose.ordinal()][DAILY_TOURS_2];
            if (tours == 2 && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  two tour day: " + term);
                }
            }

            param = parameters[purpose.ordinal()][DAILY_TOURS_3];
            if (tours == 3 && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  three tour day: " + term);
                }

            }

            param = parameters[purpose.ordinal()][DAILY_TOURS_3P];
            if (tours >= 3 && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  three or more tour day: " + term);
                }

            }

            param = parameters[purpose.ordinal()][DAILY_TOURS_4P];
            if (tours == 4 && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  four tour day: " + term);
                }
            }

            param = parameters[purpose.ordinal()][DAILY_STOPS_2P];
            if (stops >= 2 && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("two or more stops: " + term);
                }
            }

            param = parameters[purpose.ordinal()][DAILY_ACTS_6_7];
            if ((acts == 6 || acts == 7) && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  six or seven activities: " + term);
                }
            }

            param = parameters[purpose.ordinal()][DAILY_ACTS_6P];
            if (acts >= 6 && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  six or more activities: " + term);
                }
            }

            param = parameters[purpose.ordinal()][DAILY_ACTS_8P];
            if (acts >= 8 && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  eight or more activities: " + term);
                }
            }

            param = parameters[purpose.ordinal()][SHOP_STOP];
            if (stop.activityPurpose == ActivityPurpose.SHOP && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  shop stop: " + term);
                }
            }

            param = parameters[purpose.ordinal()][SHOP_STOP_SHOP_TOURS];
            if (param > -999
                    && stop.activityPurpose == ActivityPurpose.SHOP
                    && (pattern.nShopTours > 2 || (purpose == ActivityPurpose.SHOP && pattern.nShopTours > 1))) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  shop stop with other shop tours: " + term);
                }
            }

            param = parameters[purpose.ordinal()][REC_STOP];
            if (stop.activityPurpose == ActivityPurpose.RECREATE
                    && param > -999) {
                double term = param * duration;
                utility += term;
                if (trace) {
                    logger.info("  recreation stop: " + term);
                }
            }

            param = parameters[purpose.ordinal()][DEVIATION_DISTANCE];
            if (param > -999) {
                float[] dists = skims.getAdditionalAutoDistance(
                        tour.begin.location.zoneNumber,
                        tour.primaryDestination.location.zoneNumber,
                        stop.location.zoneNumber, person.homeTaz,start);
                float deviation = dists[1] - dists[0];
                double term = param * duration * deviation;
                utility += term;
                if (trace) {
                    logger.info("  deviation distance: " + term);
                }
            }

            if (trace) {
                logger.info("  Total utility " + utility);
            }

            alt.setUtility(utility);
        }

        durationModel.setDebug(trace);

        if(trace){
            durationModel.writeAvailabilities();
            durationModel.writeUtilityHeader();
        }

        double compUtility = durationModel.getUtility();

        if (trace) {
            logger.info("Composite utility: " + compUtility);
        }
        endTiming();
        return compUtility;
    }

    /**
     * Using the utilities computed with the calculate utilities method, use
     * Monte Carlo simulation to choose a period length.
     * @param random Random number
     * @return period length (0 -> longest available alternative)
     */
    public int chooseDuration(Random random) {
        startTiming();
        int duration;

        logger.debug("Choosing a duration");

        // no need to perform calculations if there is not a choice
        if (trace) {
            durationModel.writeProbabilityHeader();
        }

        durationModel.calculateProbabilities();

        try {
            duration = (Integer) ((ConcreteAlternative) durationModel
                    .chooseAlternative(random)).getAlternative();
        } catch (Exception e) {
            logger.error("Caught exception while choosing a stop duration.");
            logger.error("  Problem is in class" + StopDurationModel.class); 
            logger.error("  Assigning a default duration of 0"); 
            tour.print(); 
            stop.print();             
            duration = 0;             
        }

        if (trace) {
            logger.info("Chose stop duration " + duration);
        }

        // set the activity start and end periods and as much of the destination
        // time as is known
        if (stop == tour.intermediateStop1) {
            stop.startTime = (short) (getStopWindowStart(tour, stop));
            stop.endTime = (short) (stop.startTime + duration * 100);
            tour.primaryDestination.startTime = stop.endTime;


            if(trace){
                logger.info("stop 1 start: " + stop.startTime);
                logger.info("stop 1 end: " + stop.endTime);
                logger.info("dest start: " + tour.primaryDestination.startTime);
            }

            if (tour.intermediateStop2 == null) {
                tour.primaryDestination.endTime = tour.end.startTime;
                if(trace)
                    logger.info("dest end: " + tour.primaryDestination.endTime);
            }

        } else {
            stop.endTime = (short) getStopWindowEnd(tour, stop);
            stop.startTime = (short) (stop.endTime - duration * 100);

            tour.primaryDestination.endTime =stop.startTime;

            if(trace){
                logger.info("stop 2 start: " + stop.startTime);
                logger.info("stop 2 end: " + stop.endTime);
                logger.info("dest end: " + tour.primaryDestination.endTime);
            }

            if (tour.intermediateStop1 == null){
                tour.primaryDestination.startTime = tour.begin.endTime;
                if(trace)
                    logger.info("dest start: " + tour.primaryDestination.startTime);
            }
        }


        endTiming();
        return duration;
    }

    /**
     * @return time in 24 format
     */
    public short addTravelMinutes(Activity place1, Activity place2) {
        int minutes = place1.endTime % 100;
        int hours = place1.endTime / 100;
        int time = getTravelTime(place1, place2);

        minutes += time % 60;
        hours += time / 60;

        return (short) ((hours * 100) + minutes);
    }

    /**
     * @return time in 24 format
     */
    public short substractTravelMinutes(Activity place1, Activity place2) {
        // 'minutes' is minutes past midnight
        int minutes = place2.startTime % 100 + (place2.startTime / 100) * 60;
        int time = getTravelTime(place1, place2);
        minutes -= time;
        int startHour = minutes / 60;
        int startMintue = minutes % 60;

        return (short) ((startHour * 100) + startMintue);
    }

    /**
     * Calculate the stop start period of an outbound stop.
     * This method does not consider travel time.
     *
     * The stop start time considers the time spent at a
     * previous stop for second stops.
     *
     * @param tour The tour that the stop is part of.
     * @param stop The stop on the tour.
     * @return 24 hour time
     */
    public int getStopWindowStart(Tour tour, Activity stop) {

         if (stop == tour.intermediateStop2) {
            if (tour.intermediateStop1 != null)
                return tour.intermediateStop1.endTime;
         }

        return tour.begin.endTime;
    }

    /**
     * Calculate the stop end time.  This method does not consider travel time.
     *
     * The end window period of the return journey stop is the end of tour period.
     *
     * @param tour  The tour that the stop is a part of.
     * @param stop  The stop.
     * @return end time in 24 hour format
     */
    public int getStopWindowEnd(Tour tour, Activity stop) {
        return tour.end.startTime;
    }
     /**
     * Get the auto travel time between two activities.
     * 
     * @param from
     *            Activity
     * @param to
     *            Activity
     * @return time in minutes
     */
    public int getTravelTime(Activity from, Activity to) {
        int minutes = 0;

        int time;

        // try to get a reasonable travel time
        if (from.endTime != 0) {
            time = from.endTime;
        } else if (to.startTime != 0) {
            time = to.startTime;
        } else {
            time = tour.begin.endTime;
        }

        if (time >= morning && time < midday) {
            minutes = (int) (skims.pkTime.getValueAt(from.location.zoneNumber,
                    to.location.zoneNumber));
        } else if (time >= midday && time < evening) {
            minutes = (int) (skims.opTime.getValueAt(from.location.zoneNumber,
                    to.location.zoneNumber));
        } else if (time >= evening && time < night) {
            minutes = (int) (skims.pkTime.getValueAt(to.location.zoneNumber,
                    from.location.zoneNumber));
        } else {
            minutes = (int) (skims.pkTime.getValueAt(from.location.zoneNumber,
                    to.location.zoneNumber));
        }

        return minutes;
    }


}
