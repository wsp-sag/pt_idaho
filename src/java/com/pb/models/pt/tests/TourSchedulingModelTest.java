package com.pb.models.pt.tests;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.util.ResourceUtil;
import com.pb.models.utils.Tracer;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.Scheduler;
import com.pb.models.pt.Tour;
import com.pb.models.pt.TourSchedulingModel;
import com.pb.models.pt.PTOccupationReferencer;
import com.pb.models.pt.util.SkimsInMemory;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import static com.pb.models.pt.tests.MockDataBuilder.personFactory;
import static com.pb.models.pt.tests.MockDataBuilder.householdFactory;

public class TourSchedulingModelTest extends TestCase {
    private Logger logger = Logger.getLogger(PTPersonTest.class);

    private Tracer tracer = Tracer.getTracer();

    private TourSchedulingModel model;

    private SkimsInMemory skims;

    private ResourceBundle ptRb = ResourceUtil.getResourceBundle("pt");

    private ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");

    public TourSchedulingModelTest() {
        super();

        tracer.readTraceSettings(ptRb);
        skims = MockSkimsFactory.skimsInMemoryFactory(globalRb);

        PTOccupationReferencer myReferencer = PTOccupation.NO_OCCUPATION;

        logger.info("Building a new TourSchedulingModel.");
        model = new TourSchedulingModel(ptRb, myReferencer);
        model.buildModel();
    }

     public void testTourSchedulingProblem2() {
        PTHousehold household = householdFactory(2, 70000, 1);
        PTPerson person = personFactory(household, 23, true, true,
                "HOHCHOHOHRH");
        person.initScheduler(18);

        int[] starts = { 3, 13, 1, 9, 12 };
        int[] ends = { 9, 16, 2, 11, 13 };
        Scheduler scheduler = person.getScheduler();

        
        for (int j = 0; j < person.getTourCount(); ++j) {
            Tour tour = person.getTourByPriority(j);
            
            int order = tour.getOrder();
            int priority = tour.getPriority();

                logger.info("Scheduling tour " + order + " with priority "
                    + tour.getPriority());

            boolean match = false;
            for (int i = order - 1; i >= 0; --i) {
                int previous = person.weekdayTours[i].getPriority();
                if (previous < priority) {
                    logger.info("Previous tour "+i+" priority "+previous+" is less than current tour priority "+priority);
                    int end = scheduler.getEventEnd(previous);
                    scheduler.scheduleEvent(end, end);
                    scheduler.setEventWindow(priority);
                    match = true;
                    break;
                }
            }
            
            if (!match) {
                scheduler.scheduleEvent(0, 0);
                scheduler.setEventWindow(priority);
            }
            
            scheduler.scheduleEvent(starts[j], ends[j]);
        }

    }

    /**
     * A couple of persons could not get there tours scheduled correctly. This
     * test is an exploration to find out why.
     * 
     */
    public void testTourSchedulingProblem() {
        Scheduler scheduler = new Scheduler(15);

        PTHousehold household = householdFactory(2, 23000, 1);
        PTPerson person = personFactory(household, 32, false, true, "hwhohrhrh");

        // start and end periods by priority
        int starts[] = { 3, 7, 8, 6 };
        int ends[] = { 5, 7, 9, 7 };
        int expectEnds[] = { 0, 5, 5, 7 };

        // Here's how this needs to work. If there was a tour with a lower
        // order number and a lower priority number, schedule this tour
        // right after that tour. Else, schedule at 0, 0.

        for (int j = 0; j < person.getTourCount(); ++j) {
            Tour tour = person.getTourByPriority(j);
            int order = tour.getOrder();
            int priority = tour.getPriority();

            logger.info("Scheduling tour " + order + " with priority " + tour.getPriority());

            boolean match = false;
            for (int i = order - 1; i >= 0; --i) {
                int previous = person.weekdayTours[i].getPriority();
                if (previous < priority) {
                    int end = scheduler.getEventEnd(previous);
                    scheduler.scheduleEvent(end, end);
                    scheduler.setEventWindow(0);
                    match = true;

                    try {
                        assertEquals(expectEnds[order], end);
                    } catch (AssertionFailedError e) {
                        logger.error("Tour " + order + " did not end in " + expectEnds[order]);
                        logger.error("The previous tour is " + i
                                + " with priority " + previous);
                    }

                    break;
                }
            }

            if (!match) {
                assertEquals(true, tour.getPriority() == 0);
                scheduler.scheduleEvent(0, 0);
            }

            scheduler.setEventWindow(priority);

            scheduler.rescheduleEvent(starts[j], ends[j]);
        }
    }

    public void testTourSchedulingProblem3() {
        PTHousehold household = householdFactory(2, 70000, 1);
        PTPerson person = personFactory(household, 23, true, true,
                "HWHOHOHSH");
        person.initScheduler(18);

        int[] starts = { 4, 15, 16, 13 };
        int[] ends = { 13, 15, 16, 13 };
        Scheduler scheduler = person.getScheduler();

        for (int j = 0; j < person.getTourCount(); ++j) {
            Tour tour = person.getTourByPriority(j);
            
            int order = tour.getOrder();
            int priority = tour.getPriority();

            logger.info("Scheduling tour " + order + " with priority "
                    + tour.getPriority());

            boolean match = false;
            for (int i = order - 1; i >= 0; --i) {
                int previous = person.weekdayTours[i].getPriority();
                if (previous < priority) {
                    logger
                            .info("Previous tour " + i + " priority "
                                    + previous
                                    + " is less than current tour priority "
                                    + priority);
                    int end = scheduler.getEventEnd(previous);
                    scheduler.scheduleEvent(end, end);
                    scheduler.setEventWindow(0);
                    logger.info("Set window using period " + end);
                    match = true;
                    break;
                }
            }

            if (!match) {
                scheduler.scheduleEvent(0, 0);
                scheduler.setEventWindow(priority);
            }

            for (int i = 0; i < scheduler.getPeriods(); ++i) {
                logger.info("Period " + i + ": " + scheduler.isInWindow(i));
            }
            
            logger.info("Window available: "
                    + scheduler.isWindowAvailable(starts[j], ends[j]));
            logger.info("'Chosen' schedule: " + starts[j] + " -> " + ends[j]);
            scheduler.rescheduleEvent(starts[j], ends[j]);
        }

    }
    
    public void testTourSchedulingProblem4() {
        PTHousehold household = householdFactory(2, 70000, 1);
        PTPerson person = personFactory(household, 23, true, true,
                "HSHSHSH");
        person.initScheduler(18);

        int[] starts = { 5, 15, 16, 13 };
        int[] ends = { 5, 15, 16, 13 };
        Scheduler scheduler = person.getScheduler();

        for (int j = 0; j < person.getTourCount(); ++j) {
            Tour tour = person.getTourByPriority(j);
            
            int order = tour.getOrder();
            int priority = tour.getPriority();

            logger.info("Scheduling tour " + order + " with priority "
                    + tour.getPriority());

            boolean match = false;
            for (int i = order - 1; i >= 0; --i) {
                int previous = person.weekdayTours[i].getPriority();
                if (previous < priority) {
                    logger
                            .info("Previous tour " + i + " priority "
                                    + previous
                                    + " is less than current tour priority "
                                    + priority);
                    int end = scheduler.getEventEnd(previous);
                    scheduler.scheduleEvent(end, end);
                    scheduler.setEventWindow(priority);
                    logger.info("Set window using period " + end);
                    match = true;
                    break;
                }
            }

            if (!match) {
                scheduler.scheduleEvent(0, 0);
                scheduler.setEventWindow(priority);
            }

            for (int i = 0; i < scheduler.getPeriods(); ++i) {
                logger.info("Period " + i + ": " + scheduler.isInWindow(i));
            }
            
            logger.info("Window available: "
                    + scheduler.isWindowAvailable(starts[j], ends[j]));
            logger.info("'Chosen' schedule: " + starts[j] + " -> " + ends[j]);
            scheduler.rescheduleEvent(starts[j], ends[j]);
        }

    }
    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(TourSchedulingModelTest.class);
    }
}
