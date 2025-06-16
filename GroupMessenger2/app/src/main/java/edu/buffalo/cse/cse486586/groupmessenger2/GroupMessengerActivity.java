package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    static final int SERVER_PORT = 10000;
    String myPort;

    private ContentResolver mContentResolver;
    private ContentValues mContentValue;
    private Uri mUri;
    static int msgCount = 0;

    private BlockingQueue<String> hold_back = new ArrayBlockingQueue<String>(30);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        mContentResolver = getContentResolver();
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        Log.v("myport", myPort);

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
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText sendMessage = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = sendMessage.getText().toString() + "\n";
                sendMessage.setText(""); // This is one way to reset the input box.
                // tv.append("\t" + msg); // This is one way to display a string.

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT0,
                        myPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT1,
                        myPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT2,
                        myPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT3,
                        myPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT4,
                        myPort);
            }
        });

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... msgs) {
            /*
            * item
            *
              REMOTE_PORT0
              Integer.toString(ID)
              "Sequence"
            *
            *
            * */
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(msgs[1]));

                String msgToSend = msgs[0];
                Log.v("send msg: ", msgToSend);
                MyMessage msg = new MyMessage(msgToSend, myPort, msgs[2]);
                if(msgs.length == 4 && msgs[3].equals("Sequence")) {
                    msg.type = MyMessage.Type.Sequence;
                }
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(msg);
                out.flush();

                // socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket socket = null;

            while (true) {
                try {
                    socket = serverSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    MyMessage inputObject = (MyMessage) in.readObject( );
                    String inputMsg = inputObject.msg;
                    if(inputMsg == null) {
                        publishProgress("");
                        return null;
                    }
                    if(inputObject.type == MyMessage.Type.Common) {
                        hold_back.put(inputMsg);
                        publishProgress(inputMsg);
                        if(myPort.equals("11112")) {
                            send_seq(inputMsg);
                        }
                        Log.v("recieved common msg: ", inputMsg + " " + inputObject.type);
                    } else if(inputObject.type == MyMessage.Type.Sequence) {
                        mContentValue = new ContentValues();
                        mContentValue.put("key", inputObject.ID);
                        mContentValue.put("value", inputObject.msg);
                        mContentResolver.insert(mUri, mContentValue);
                        Log.v("recieved seq msg: ", inputMsg + " " + inputObject.type +
                                " " + inputObject.ID);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "ServerTask socket Exception");
                }
            }
        }

        @Override
        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            if(strReceived.equals("")) {
                Context context = getApplicationContext();
                CharSequence text = "the input is null!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append(strReceived + "\n");
            localTextView.append("\n");
            return ;
        }

        private void send_seq(String inputMsg) {
            /*
            *
                public String msg;
                public String from;
                public String ID;
                public Type type;

            * */

            while (hold_back.size() > 0) {
                String item = hold_back.poll();
                new ClientTask().executeOnExecutor(
                        AsyncTask.SERIAL_EXECUTOR, item,
                        REMOTE_PORT0, Integer.toString(msgCount), "Sequence");
                new ClientTask().executeOnExecutor(
                        AsyncTask.SERIAL_EXECUTOR, item,
                        REMOTE_PORT1, Integer.toString(msgCount), "Sequence");
                new ClientTask().executeOnExecutor(
                        AsyncTask.SERIAL_EXECUTOR, item,
                        REMOTE_PORT2, Integer.toString(msgCount), "Sequence");
                new ClientTask().executeOnExecutor(
                        AsyncTask.SERIAL_EXECUTOR, item,
                        REMOTE_PORT3, Integer.toString(msgCount), "Sequence");
                new ClientTask().executeOnExecutor(
                        AsyncTask.SERIAL_EXECUTOR, item,
                        REMOTE_PORT4, Integer.toString(msgCount), "Sequence");
                msgCount ++;
            }

        }


    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}
