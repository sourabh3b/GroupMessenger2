package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;
import java.util.LinkedList;

/**
 * Created by sourabh on 3/7/18.
 */

public class Message {
    int messageID;//unique identification for the message
    String data; //actual data of the message
    int messageSequenceNumber; //sequence number in integer
    String messageSender; //message sender
    String messageDestination; //destination
    boolean isMessageDeliverable; //flag to check if message is deliverable


    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getMessageSequenceNumber() {
        return messageSequenceNumber;
    }

    public void setMessageSequenceNumber(int messageSequenceNumber) {
        this.messageSequenceNumber = messageSequenceNumber;
    }

    public String getMessageSender() {
        return messageSender;
    }

    public void setMessageSender(String messageSender) {
        this.messageSender = messageSender;
    }

    public String getMessageDestination() {
        return messageDestination;
    }

    public void setMessageDestination(String messageDestination) {
        this.messageDestination = messageDestination;
    }

    public boolean isMessageDeliverable() {
        return isMessageDeliverable;
    }

    public void setMessageDeliverable(boolean messageDeliverable) {
        this.isMessageDeliverable = messageDeliverable;
    }

    public int getMessageID() {
        return messageID;
    }

    public void setMessageID(int messageID) {
        this.messageID = messageID;
    }

    public Message() {
    }

    public Message(String data, String messageSender, String messageDestination, boolean isMessageDeliverable, int messageID) {
        this.data = data;
        this.messageSender = messageSender;
        this.messageDestination = messageDestination;
        this.isMessageDeliverable = isMessageDeliverable;
        this.messageID = messageID;
    }

}

