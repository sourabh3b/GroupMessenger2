package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;
import java.util.LinkedList;

/**
 * Created by sourabh on 3/12/18.
 */

//class for message list
public class MessageList {

    //class for message list (with message sender ID and seq no.)
    String messageSenderID;
    int messageGivenSequenceNumber;

    //linkedlist maintained to keep track of messages in client side
    static LinkedList<MessageList> messageMessageList = new LinkedList<MessageList>();

}