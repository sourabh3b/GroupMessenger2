package edu.buffalo.cse.cse486586.groupmessenger2;

import android.net.Uri;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Created by sourabh on 2/20/18.
 * This class contains all constants needed for the program.
 * Idea is to keep constants separate so that there single configuration point for the program that can be changed in a single class
 * rather than changing through out the program.
 */

public class Constants {

    //Starting and Ending port (Other ports are incremented by value of 4)
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    //list of all ports for all AVDs
    static final String[] PortList = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};

    //server port
    static final int SERVER_PORT = 10000;

    //namespace for content provider for 2
    static final String contentProviderURI = "content://edu.buffalo.cse.cse486586.groupmessenger2.provider";

    //Content Uri corresponding to given contentProviderURI
    static final Uri CONTENT_URL = Uri.parse(contentProviderURI);

    //string constants for key and value
    static final String KEY = "key";
    static final String VALUE = "value";

    //matrix columns used
    static String[] matrixColumns = {KEY, VALUE};

    //sequence number to keep track of messages
    static int clientMessageSequenceNumber = 0;

    //delimiter for separating message metadata
    static String messageDelimiter = "###";

    //by default considering failed AVD as 0th, this will changes once socket IOexception is handled
    static String failureAvdID = "0";

    //startAVD ID
    static int startAvdID = 0;

    static int endAvdID = 5;

    //initial priority queue capacity (Got this value by running test script multiple times),
    //since there are 24 messages sent by test script, therefore, keeping max capacity = 24 for maximum allowable messages in queue
    static int priorityQueueInitialCapacity = 24;


    //thread sleep time
    static long sleepTime = 500;

    //global priority queue for message with comparator
    //return 1 if message2 has low sequence number
    //Reference : [1] : https://www.geeksforgeeks.org/java-util-priorityqueue-class-java ; [2] : https://stackoverflow.com/questions/683041/how-do-i-use-a-priorityqueue
    static PriorityQueue<Message> holdBackQueue = new PriorityQueue<Message>(priorityQueueInitialCapacity, new Comparator<Message>() {

        public int compare(Message m1, Message m2) {
            if (m1.messageSequenceNumber > m2.messageSequenceNumber) {
                return 1;
            } else if (m1.messageSequenceNumber < m2.messageSequenceNumber) {
                return -1;
            }
            return 1;
        }
    });

    //iterator for the hold back queue
    static Iterator<Message> messageIterator = holdBackQueue.iterator();


    //helper function to check of holdBack QueueHas PipeLine Message
    static boolean hbQHasPipelineMessage() {
        return messageIterator.hasNext();
    }


    //function to clean up data from failure AVD port
    //this function essentially removes messages from hold back queue for failed AVD port
    //Reference : https://www.geeksforgeeks.org/java-util-priorityqueue-class-java/
    static void cleanUP(String potentialFailurePort) {
        //if failure port exist then remove messages corresponding to failure ADV port
        Constants.failureAvdID = potentialFailurePort;
        if (Integer.parseInt(Constants.failureAvdID) != 0) {
            Iterator<Message> holdBackQueueIterator = Constants.holdBackQueue.iterator();
            while (holdBackQueueIterator.hasNext()) {
                Message current = holdBackQueueIterator.next();
                if (Integer.parseInt(current.messageSender) == Integer.parseInt(Constants.failureAvdID)) {
                    Constants.holdBackQueue.remove(current);
                }
            }
        }
    }


    static String getPotentialFailedAVD(int category, String[] messageObject) {

        //failure
        if (category == 1) {
            return messageObject[5];
        } else if (category == 3) { //multicast
            return messageObject[6];
        }
        return null;
    }


    /*
    * I am considering 3 types of message passing based on type of message sent by client
    * 0 - not used
    * 1 - failed (This indicates that system had detected a failure, messages will be sent with appending this enum
    * 2 - this enum indicates that message is coming from category
    * 3 - this category specifies that message is done multicast from client
    * */
    public static enum Category {
        Test(0),
        Failed(1),
        FromFailed(2),
        Multicast(3);

        //helper function to get value for the enum
        //Ref : https://stackoverflow.com/questions/7996335/how-to-match-int-to-enum/7996473#7996473
        private int enumValue;

        Category(int Value) {
            this.enumValue = Value;
        }

        public int getValue() {
            return enumValue;
        }

    }


}