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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
    static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};

    static final int SERVER_PORT = 10000;
    String myPort;

    private ContentResolver mContentResolver;
    private ContentValues mContentValue;
    private Uri mUri;
    static int msgCount = 0;
    String sequencer = "11108";

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
                Log.d(TAG, "input msg is : " + msg);
                sendMessage.setText("");
                for (int i = 0; i < REMOTE_PORTS.length; i++) {
                    new ClientTask().executeOnExecutor(
                            AsyncTask.SERIAL_EXECUTOR,
                            msg,
                            REMOTE_PORTS[i],
                            myPort);
                }
            }
        });

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");

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
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(msgs[1]));
                socket.setSoTimeout(2500);
//                socket.setKeepAlive(true);

                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                MyMessage msg = new MyMessage(msgs[0], myPort, msgs[2], "Common");
                if(msgs.length == 4 && msgs[3].equals("Sequence")) {
                    msg.type = MyMessage.Type.Sequence;
                } else if (msgs.length == 4 && msgs[3].equals("Switch")) {
                    msg.type = MyMessage.Type.Switch;
                }
                String msgToSend = msg.toString();
                out.writeUTF(msgToSend);
                out.flush();
                Log.v("send msg: ", msgToSend + msgs[1]);
//                InputStream in = socket.getInputStream();
//                int data = in.read();
//                if (data == -1) {
//                    throw new IOException("Invalid message format: " + msgs[1]);
//                    // Log.d("SocketStatus", "对端已关闭 (FIN received):" + msgs[1]);
//                }
//                out.close();
//                socket.close();
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "ClientTask SocketTimeoutException on " + msgs[1]);
            } catch (StreamCorruptedException e) {
                Log.e(TAG, "ClientTask StreamCorruptedException on " + msgs[1]);
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException on " + msgs[1]);
                if(msgs[1].equals("11108")) {
                    sequencer = "11112";
                    if(myPort.equals(sequencer)) {
                        send_seq();
                    }
                    for (int i = 0; i < REMOTE_PORTS.length; i++) {
                        new ClientTask().executeOnExecutor(
                                AsyncTask.SERIAL_EXECUTOR,
                                "Switch",
                                REMOTE_PORTS[i],
                                "-1",
                                "Switch");
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "ClientTask Exception on " + msgs[1]);
            }
            return null;
        }

        private void send_seq() {
            /*
            *
                public String msg;
                public String from;
                public String ID;
                public Type type;

            * */
            while (hold_back.size() > 0) {
                String item = hold_back.poll();
                for (int i = 0; i < REMOTE_PORTS.length; i++) {
                    new ClientTask().executeOnExecutor(
                            AsyncTask.SERIAL_EXECUTOR,
                            item,
                            REMOTE_PORTS[i],
                            Integer.toString(msgCount),
                            "Sequence");
                }
                msgCount ++;
            }
        }
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {


        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    out.writeUTF("ACK");
                    out.flush();
                    DataInputStream in = new DataInputStream(client.getInputStream());
                    String msg = in.readUTF();
                    if(msg == null) {
                        publishProgress("");
                        return null;
                    }
                    MyMessage inputObject = MyMessage.parse(msg);
                    String inputMsg = inputObject.msg;
                    if(inputObject.type == MyMessage.Type.Common) {
                        hold_back.put(inputMsg);
                        publishProgress(inputMsg);
                        if(myPort.equals(sequencer)) {
                            send_seq();
                        }
                        Log.v("recieved common msg: ", inputMsg + " " + inputObject.type +
                                " " + Integer.toString(hold_back.size()));
                    } else if(inputObject.type == MyMessage.Type.Sequence) {
                        mContentValue = new ContentValues();
                        mContentValue.put("key", inputObject.ID);
                        mContentValue.put("value", inputObject.msg);
                        mContentResolver.insert(mUri, mContentValue);
                        Log.v("recieved seq msg: ", inputMsg + " " + inputObject.type +
                                " " + inputObject.ID);
                    } else if(inputObject.type == MyMessage.Type.Switch) {
                        sequencer = "11112";
                        if(myPort.equals(sequencer)) {
                            send_seq();
                        }
                        Log.v(TAG, "recieved Switch");
                    }
                    client.close();
                } catch (IOException e) {
                    Log.e(TAG, "ServerTask socket IOException" + e.getMessage());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // return null;
        }

        private void send_seq() {
            /*
            *
                public String msg;
                public String from;
                public String ID;
                public Type type;

            * */
            while (hold_back.size() > 0) {
                String item = hold_back.poll();
                for (int i = 0; i < REMOTE_PORTS.length; i++) {
                    new ClientTask().executeOnExecutor(
                            AsyncTask.SERIAL_EXECUTOR,
                            item,
                            REMOTE_PORTS[i],
                            Integer.toString(msgCount),
                            "Sequence");
                }
                msgCount ++;
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
    }


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}
