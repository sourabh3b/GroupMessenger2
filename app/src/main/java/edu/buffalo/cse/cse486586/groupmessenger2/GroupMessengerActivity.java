package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;
import android.telephony.TelephonyManager;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.io.*;

import android.content.ContentResolver;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {

    //Static variables ( Referenced from PA1)
    static final String TAG = GroupMessengerActivity.class.getSimpleName();

    LinkedList<MessageList> messageMessageList = new LinkedList<MessageList>();

    int sequenceNumberGlobal = 0;
    int messaageID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

         /*
         * Calculate the port number that this AVD listens on. (Referenced from PA1)
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        //Creating Server Socket (Referenced from PA1)
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             * AsyncTask is a simplified thread construct that Android provides.
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());


        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(new OnPTestClickListener(tv, getContentResolver()));


        /* Note : Below code is referenced from PA2a
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

         /* Retrieve a pointer to the input box (EditText) defined in the layout
         * XML file (res/layout/main.xml).
         *
         * This is another example of R class variables. R.id.edit_text refers to the EditText UI
         * element declared in res/layout/main.xml. The id of "edit_text" is given in that file by
         * the use of "android:id="@+id/edit_text""
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);

        final Button sendButton = (Button) findViewById(R.id.button4);

        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                //This displays message in textView (just for debugging easy)
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                editText.setTextColor(Color.RED);
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t >>>>" + ">>>" + msg); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                remoteTextView.setTextColor(Color.BLUE);
                remoteTextView.append("\n>>>>>");

                /*
                 * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                 * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                 * the difference, please take a look at
                 * http://developer.android.com/reference/android/os/AsyncTask.html
                 */

                //create client Async task, which will accept messages from other clients
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);


            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        //Referred from PA1
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
            * Note : Below code is referenced from PA2 with change for multicast and failure node
            Algorithm :
            * 0. In order to continue accepting more connections, use infinite while loop
            * 1. Listen for a connection to be made to the socket coming  as a param in AsyncTask and accepts it. [ Reference : https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html]
            * 2. Create InputStream form incoming socket
            * 3. To send message to UI thread, call onProgressUpdate with bufferReader.readLine() string value (which runs on UI thread as a result of calling this function)
            * */
            try {

                //this is done to keep reading multiple messages
                //at least one time send & receive message
                do {
                    //server is ready to accept data starting
                    Socket socket = serverSocket.accept();

                    //Basic Stream flow in Java : InputStream -> InputStreamReader -> BufferReader -> Java Program [ Reference : https://www.youtube.com/watch?v=mq-f7zPZ7b8  ; https://www.youtube.com/watch?v=BSyTJSbNPdc]
                    //taking input from socket as a stream
                    //Note: Buffered Reader approach was not working properly
                    DataInputStream inputStreamFromSocket = new DataInputStream(socket.getInputStream());

                    //getting string from inputStreamFromSocket
                    String messageFromSocket = inputStreamFromSocket.readUTF();


                    //Splitting message by regex value
                    String[] messageSerializedObject = messageFromSocket.split(Constants.messageDelimiter);

                    //1st index of serialized message contains the categoy of message
                    int category = Integer.parseInt(messageSerializedObject[0]);

                    /*
                    * If category == Multicast then check for messages in hold back queue,
                    * Since, category is not failes, pipeline messages are marked deliverables  (using setMessageDeliverable)
                    * A Sanity check for failed AVD is done, when AVD fails in between, in this case messages corresponding to failed AVD are removd from hols back queue
                    * */


                    //category for multicast
                    Constants.Category varMc = Constants.Category.Multicast;
                    int ValueOfEnumMc = varMc.getValue();


                    //checking for multicast message type
                    if (category == ValueOfEnumMc) {

                        //getting sequence number, source, destination from serialized message object
                        int currentSequenceNumber = Integer.parseInt(messageSerializedObject[3]);
                        String source = messageSerializedObject[2];
                        String currentMessageID = messageSerializedObject[1];
                        String destination = messageSerializedObject[4];


                        //comparing current seq. no. for higher priority global sequence number
                        if (currentSequenceNumber > sequenceNumberGlobal)
                            sequenceNumberGlobal = currentSequenceNumber;


                        Iterator<Message> holdBackIterator = Constants.holdBackQueue.iterator();

                        //check is holdback queue has message correspondin to thi sender
                        while (holdBackIterator.hasNext()) {
                            Message pipelineMessage = holdBackIterator.next();
                            String pipelineMessageSender = pipelineMessage.getMessageSender();

                            //check for current message ID with pipeline
                            if (Integer.parseInt(pipelineMessageSender) != Integer.parseInt(source)) {
                                //
                            }

                            //sender ID should be same as pipeline message senderID
                            if (pipelineMessage.messageID == Integer.parseInt(currentMessageID)) {
                                Constants.holdBackQueue.remove(pipelineMessage);


                                //modifying pipeline message
                                pipelineMessage.setMessageDeliverable(true);
                                pipelineMessage.setMessageSequenceNumber(currentSequenceNumber);
                                pipelineMessage.setMessageDestination(destination);

                                //adding pipeline message to holdBackQueue
                                Constants.holdBackQueue.add(pipelineMessage);
                            }


                        }

                        //check for get failure port if exist
                        //if failure port exist then remove messages corresponding to failure ADV port
                        Constants.failureAvdID = messageSerializedObject[6];
                        if (Integer.parseInt(Constants.failureAvdID) != 0) {
                            Iterator<Message> it2 = Constants.holdBackQueue.iterator();
                            while (it2.hasNext()) {
                                Message current = it2.next();
                                if (Integer.parseInt(current.messageSender) == Integer.parseInt(Constants.failureAvdID)) {
                                    Constants.holdBackQueue.remove(current);
                                }
                            }
                        }

                    }


                    Constants.Category varF = Constants.Category.Failed;
                    int ValueOfEnumF = varF.getValue();

                    /*
                    * If category == Failure then check for messages in hold back queue,
                    * Since, category is not failes, pipeline messages are marked deliverables  (using setMessageDeliverable)
                    * A Sanity check for failed AVD is done, when AVD fails in between, in this case messages corresponding to failed AVD are removd from hols back queue
                    * */

                    if (category == ValueOfEnumF) {
                        sequenceNumberGlobal = sequenceNumberGlobal + 1;
                        OutputStream outToServer = socket.getOutputStream();

                        DataOutputStream out = new DataOutputStream(outToServer);
                        String messageDestination = messageSerializedObject[4];

                        String messageID = messageSerializedObject[2];


                        Constants.Category varFF = Constants.Category.FromFailed;
                        int ValueOfEnumFF = varFF.getValue();
                        String FromFail = String.valueOf(ValueOfEnumFF);


                        //creating message object for this category
                        String messageObject = FromFail + Constants.messageDelimiter +
                                messageID + Constants.messageDelimiter +
                                Integer.toString(sequenceNumberGlobal) + Constants.messageDelimiter +
                                messageDestination;


                        out.writeUTF(messageObject);
                        String currentMessageData = messageSerializedObject[1];
                        String source = messageSerializedObject[3];

                        //creating message object
                        Message failureMessageObject = new Message(currentMessageData, source, messageDestination, false, Integer.parseInt(messageID));

                        //add failureMessageObject to holdback ququq
                        Constants.holdBackQueue.add(failureMessageObject);


                        //check for failed AVD
                        String potentialFailedAVD = Constants.getPotentialFailedAVD(category, messageSerializedObject);

                        //cleanup if there exist a failed port
                        Constants.cleanUP(potentialFailedAVD);
                    }


                    //check of there are  deliverable messages in holdBackQueue
                    //if Yes, deliver those messages and push to getContentResolver

                    //checking for deliverable messages in the holdBackQueue
                    while (Constants.holdBackQueue.peek() != null) {

                        //if there are no deliverable message break;
                        if (Constants.holdBackQueue.peek().isMessageDeliverable != true) {
                            break;


                        } else {
                            //else add messages to the content provider
                            Message currentMessage = Constants.holdBackQueue.poll();


                            //Initialize new content value
                            ContentValues keyValueToInsert = new ContentValues();

                            //Put messageID as key into content value
                            keyValueToInsert.put(Constants.KEY, messaageID);

                            //Put content value as received string from step 1
                            keyValueToInsert.put(Constants.VALUE, currentMessage.data);


                            //Initialize content resolver helps to Inserts a row into a table at the given Uri (which is also fetched from Constants class)
                            ContentResolver contentResolver = getContentResolver();

                            //Insert values to content resolver
                            contentResolver.insert(Constants.CONTENT_URL, keyValueToInsert);

                            //This is invoked in doBackground() to send message to UI thread to call onProgressUpdate (which runs on UI thread as a result of this function calling)
                            //publishing progress with bufferReader.readline() - which returns a line of String which has been read by bufferReader
                            publishProgress(String.valueOf(messaageID), currentMessage.data);

                            //Increment sequence number for next message to receive
                            messaageID++;
                            //break;


                        }

                    }


                    //push data coming from socket
                    OutputStream socketOutputStream = socket.getOutputStream();
                    DataOutputStream dataOutputStream = new DataOutputStream(socketOutputStream);

                    //writing Done! to outputStream
                    dataOutputStream.writeUTF("Done!");


                } while (true);
            } catch (IOException e) {
                Log.e(TAG, "Message receive exception");
            }
            return null;
        }


        protected void onProgressUpdate(String... strings) {
            /*
            * This runs on UI thread as a result of publish progress
             * Reference :
             * [0] : https://developer.android.com/training/data-storage/files.html#WriteInternalStorage
             * [1] : https://developer.android.com/reference/android/content/Context.html
             * [2] : https://developer.android.com/reference/android/content/Context.html#openFileOutput(java.lang.String, int)
             * [3] : https://www.mkyong.com/java/how-to-write-to-file-in-java-fileoutputstream-example/
             * [4] : http://www.java2s.com/Code/Android/Core-Class/ContextopenFileOutput.htm
             */
            //Receive string from argument's first index (i.e strings[0]) and trim it
            String strReceived = strings[0].trim();


            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");



            /*
             * The following code creates a file in the AVD'sequenceNumberGlobal internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            //storing data in to a file, where key will be filename and value will be message.
            String fileName = "fileName";
            String data = strReceived;

            FileOutputStream outputStream = null;
            try {

                //create the file with file name as "key" (the default mode is used, where the created file can only be accessed by the calling application)
                //Reference : [https://stackoverflow.com/questions/25591066/openfileoutput-method-vs-fileoutputstream-constructor]
                //Using openFileOutput because it is specifically used for writing file into internal storage
                outputStream = getBaseContext().openFileOutput(fileName, Context.MODE_PRIVATE);

                //write to file
                outputStream.write(data.getBytes()); //writing the data into the file
                outputStream.flush();
                outputStream.close();


            } catch (FileNotFoundException e) {
                Log.v(TAG, "FileNotFoundException in insert");
            } catch (IOException e) {
                Log.v(TAG, "IOException in insert");
            } finally {
                try {
                    if (outputStream != null) {
                        //closing output stream incase no exception occurs
                        outputStream.close();
                    }
                } catch (IOException e) {
                    Log.v(TAG, "IOException in insert");
                }
            }


            //return;
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {


        //linked list maintained to keep track of messages in client side
        LinkedList<MessageList> messageMessageList = new LinkedList<MessageList>();


        @Override
        protected Void doInBackground(String... msgs) {


            //fetching message data
            String messageData = msgs[1];


            //Current Message List
            MessageList currentMessageList = new MessageList();

            int currentAvdID = 0; //todo: constant
            currentAvdID = Constants.startAvdID;

            try {
                //loop through all AVDs
                /*
                *Algorithm :
                * 1. Iterate through all AVDs from 1 to 5
                * 2. If current AVD is failed AVD, ignore
                * 3. Else, create client socket
                * 4. Send customized message to the server
                *
                * ///// Failure detection
                * 5. Fetch  message (heartbeat message) from server socket dataInputStream
                * 6. If there is IOException upon receipient of heartbeat message, then mark current AvdID as failureAvdID
                * 7. Else, current AVD is not failed AVD, decode message details from heart beat message received in step 6.
                * 8. After forming message from step 7, push message to the Linked List of message
                * 9. Message needs to be total order for Other AVDs (FIFO), the is done with the help of sorting message as per their sequence number
                * 10. Sleep the current thread by 100ms (To make sure all message are arrived due to failed AVD)
                *
                * //Multicast
                * 11. Multicast message after identification of failure node (call multicast() function)
                * */
                do {

                    //check for failed AVD, if exist ignore it
                    if (Integer.parseInt(Constants.PortList[currentAvdID]) == Integer.parseInt(Constants.failureAvdID)) {

                        //ignore the failed AVD ID
                        Log.d(TAG, "Client " + currentAvdID + " failed");

                    } else {
                        //check if this port is not failed AVDs port

                        //creating socket corresponding to current AVD port
                        Socket currentSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(Constants.PortList[currentAvdID]));


                        //Reference : https://stackoverflow.com/questions/7996335/how-to-match-int-to-enum/7996473#7996473
                        Constants.Category varF = Constants.Category.Failed;
                        int ValueOfEnum = varF.getValue();
                        String messageCategory = Integer.toString(ValueOfEnum);


                        //generating clientMessageSequenceNumber with appending global counter
                        String clientMessageSequenceNumber = Integer.toString(Constants.clientMessageSequenceNumber) + msgs[1];

                        //Creating customize message which needs to be decoded by sequencer and push as per total ordering
                        String failedMessageID = msgs[0];
                        String destinationID = msgs[1]; //Note : This order is different for multicast
                        String currentSenderID = Constants.PortList[currentAvdID]; //source ID information


                        //creating customize message for failed AVD case (category)
                        //messageCategory = failure, from failure ,multicast
                        //Ref : Idea taken from [https://www.codementor.io/java/tutorial/serialization-and-deserialization-in-java]
                        String customizeMessageObject = messageCategory + Constants.messageDelimiter +
                                failedMessageID + Constants.messageDelimiter +
                                clientMessageSequenceNumber + Constants.messageDelimiter +
                                destinationID + Constants.messageDelimiter + currentSenderID +
                                Constants.messageDelimiter + Constants.failureAvdID;


                        //Note : In order to properly detect failure node, it is important to catch every exception
                        //Ref : https://www.programcreek.com/java-api-examples/java.net.SocketException
                        OutputStream outputStream = null;
                        try {
                            outputStream = currentSocket.getOutputStream();
                        } catch (IOException e) {
                            Log.d(TAG, "Client : IOException");
                        }


                        DataOutputStream out = new DataOutputStream(outputStream);
                        try {
                            out.writeUTF(customizeMessageObject);
                        } catch (IOException e) {
                            Log.d(TAG, "Client : IOException");
                        }

                        DataInputStream dataInputStream = null;
                        try {
                            dataInputStream = new DataInputStream(currentSocket.getInputStream());
                        } catch (IOException e) {
                            Log.d(TAG, "Client IOException");
                        }


                        /*
                        *This is a heartbeat like approach, on receive of  message from socket, IOExceprtion is checked
                        * This is the reason client is failed, therefore, by catching this exception and storing it in failureAvdID variable gives failed node
                        * If exception doesn't occur = > message is pushed to the linked list
                        * */
                        String heartBeatMessage = null;
                        try {
                            if (dataInputStream != null) {
                                heartBeatMessage = dataInputStream.readUTF();
                            }
                        } catch (IOException e) {
                            Log.d(TAG, "Client IOException");
                            //getting failed ADV port
                            Constants.failureAvdID = Constants.PortList[currentAvdID];
                        } finally {
                            ///Checking for sequencue number and message sender ID
                            try {
                                //Ref : http://javatutorialhq.com/java/util/scanner-class-tutorial/usedelimiter-string-pattern-method-example/
                                String[] messageObject = heartBeatMessage.split(Constants.messageDelimiter);
                                currentMessageList.messageGivenSequenceNumber = Integer.parseInt(messageObject[2]);
                                currentMessageList.messageSenderID = messageObject[3];
                            } catch (RuntimeException e) {
                                Log.d(TAG, "Client RuntimeException");
                            }


                            //add currentMessage to the linkedlist of messages
                            messageMessageList.add(currentMessageList);

                            //sorting the message linkedlist based on sequence number for total ordering
                            Collections.sort(messageMessageList, new CompareMessageLists());

                        }

                        //stabilizing the system for failed AVD
                        //Ref : [https://stackoverflow.com/questions/26703324/why-do-i-need-to-handle-an-exception-for-thread-sleep]
                        try {
                            Thread.sleep(Constants.sleepTime);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Client Can't sleep : InterruptedException");
                        }

                        //closing socket
                        try {
                            currentSocket.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Client Can't sleep : InterruptedException");
                        }
                    }

                    //incrementing for next AVD
                    currentAvdID++;
                } while (currentAvdID < Constants.endAvdID); //base condition
            } catch (IOException e) {
                e.printStackTrace();
            }

            //multicast messages to all AVDs
            multicast(Integer.toString(Constants.clientMessageSequenceNumber) + msgs[1], messageData, currentMessageList);

            //incrementing client message counter
            Constants.clientMessageSequenceNumber++;

            return null;
        }


        //multicast - function to multicast message to all AVDs including self
        void multicast(String messageId, String messageData, MessageList currentMessageList) {

            //base AVD ID //todo: take this from constants
            int currentAvdID = 0;

            try {

                /* Note : Below code is referenced from PA2a, with small modification
                 * Objective : multicast customize message  to other clients including self
                 * Algorithm :
                 * 1. Iterate from starting Port until ending Port
                 * 2. Create socket for each port
                 * 3. Create a output stream from the socket coming as a param in AsyncTask
                 * 4. Create message object with addition of delimiters (this need to be parsed)
                 * 5. Write incoming socketStream from 1 to a bufferedWriter (Intermediate step of moving outputStream to bufferedWriter is done in BufferedWriter constructor)
                 * 6. Flush and close Buffered writer
                 * 7. Close socket
                 * Reference : [https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html]
                 *           : [https://www.youtube.com/watch?v=mq-f7zPZ7b8]
                 */

                do {

                    //creating socket corresponding to current AVD port corresponding to currentAvdID
                    Socket clientSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(Constants.PortList[currentAvdID]));


                    //getting int value of enum value for multicast category
                    //Reference : https://stackoverflow.com/questions/7996335/how-to-match-int-to-enum/7996473#7996473
                    Constants.Category var = Constants.Category.Multicast;
                    int ValueOfEnum = var.getValue();
                    String messageCategory = Integer.toString(ValueOfEnum);


                    //forming customize message for multicast catagory
                    String customizeMessageObject = messageCategory + Constants.messageDelimiter +
                            messageId + Constants.messageDelimiter +
                            messageData + Constants.messageDelimiter +
                            currentMessageList.messageGivenSequenceNumber + Constants.messageDelimiter +
                            currentMessageList.messageSenderID + Constants.messageDelimiter +
                            Constants.PortList[currentAvdID] + Constants.messageDelimiter +
                            Constants.failureAvdID;


                    //sending serialized message from socket's outstream to the server
                    DataOutputStream out = new DataOutputStream(new DataOutputStream(clientSocket.getOutputStream()));

                    //sending serialized message
                    out.writeUTF(customizeMessageObject);


                    //closing socket
                    clientSocket.close();

                    //incrementing AvdID for next device
                    currentAvdID++;
                } while (currentAvdID < Constants.endAvdID);
            } catch (UnknownHostException e) {
                Log.d(TAG, "UnknownHostException");
            } catch (IOException e) {
                Log.d(TAG, "IOException");
            } finally {
                //
            }
        }

        //comparator of two message linked lists
        class CompareMessageLists implements Comparator<MessageList> {
            public int compare(MessageList messageList1, MessageList messageList2) {
                if (messageList1.messageGivenSequenceNumber < messageList2.messageGivenSequenceNumber)
                    return -1;
                else return 1;
            }
        }
    }
}