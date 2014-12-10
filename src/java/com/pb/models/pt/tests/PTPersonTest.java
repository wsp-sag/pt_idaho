package com.pb.models.pt.tests;

import org.apache.log4j.Logger;

import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.Tour;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Test PTPerson functionality.
 * 
 * @author Stryker
 */
public class PTPersonTest extends TestCase {
    private transient Logger logger = Logger.getLogger(PTPersonTest.class);

    /**
     * Constructor creates a person without attributes.
     */
    public PTPersonTest() {
        super();
    }

    /**
     * Test prioritizing tours.
     * 
     * This test passes when it can prioritize a complex day pattern.
     */
    public void testPrioritizeTours() {
        logger.info("Test prioritizing tours.");

        // create a series of tours to represent this unlikely day-pattern:
        // HSHCHBHOHRHWHOHSHGH
        //
        // Purpose Priority
        // S 4
        // C 0
        // B 1
        // O 7
        // R 6
        // W 2
        // O 8
        // S 5
        // G 3

        int[] expect = { 4, 0, 1, 7, 6, 2, 8, 5, 3 };

        PTPerson person = createPersonWithTours();

        logger.info("Prioritizing tours.");
        person.prioritizeTours();

        Tour[] tours = person.weekdayTours;

        for (int i = 0; i < tours.length; ++i) {
            try {
                assertEquals(expect[i], tours[i].getPriority());
            } catch (AssertionFailedError e) {
                logger
                        .info("For tour " + i + ", priority should be "
                                + expect[i] + " but is "
                                + tours[i].getPriority() + ".");
                throw e;
            }
        }
    }

    /**
     * Create a complex series of tours.
     * 
     * @return PTPerson with only a tour array.
     */
    private PTPerson createPersonWithTours() {
        logger.info("Creating a person with a complex series of tours.");

        PTPerson person = new PTPerson();
        logger.info("Created a person.");
        char[] purposes = { 's', 'c', 'b', 'o', 'r', 'w', 'o', 's', 'g' };
        Tour[] tours = new Tour[purposes.length];

        // set-up the week day tour array
        for (int i = 0; i < tours.length; ++i) {
            logger.info("Creating tour " + i);
            tours[i] = new Tour();
            tours[i].primaryDestination.activityPurpose = ActivityPurpose
                    .getActivityPurpose(purposes[i]);
        }

        person.weekdayTours = tours;

        logger.info("Exiting the person creation.");
        return person;
    }

    /**
     * Run tests from the command line.
     * 
     * @param args
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(PTPersonTest.class);
    }
}
