package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
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
    static String myPort;
    private ContentResolver mContentResolver;
    private Uri mUri;
    static SequencerHelper helper = new SequencerHelper();

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
                Log.i(TAG, "input msg is : " + msg);
                sendMessage.setText("");
                for (int i = REMOTE_PORTS.length - 1; i >= 0 ; i--) {
                    new ClientTask().executeOnExecutor(
                            AsyncTask.SERIAL_EXECUTOR,
                            msg,
                            REMOTE_PORTS[i],
                            myPort,
                            "Common");
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

    public static class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(msgs[1]));
                socket.setSoTimeout(2500);
                socket.setKeepAlive(true);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                MyMessage msg = new MyMessage(msgs[0], myPort, msgs[2], msgs[3]);
                String msgToSend = msg.toString();
                out.writeUTF(msgToSend);
                out.flush();
                Log.v("General send msg: ", msgToSend + msgs[1]);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                String ack = in.readUTF();
                Log.d("SocketStatus", "Received: " + ack);
//                out.close();
//                socket.close();
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "ClientTask SocketTimeoutException on " + msgs[1]);
            } catch (StreamCorruptedException e) {
                Log.e(TAG, "ClientTask StreamCorruptedException on " + msgs[1]);
            } catch (EOFException e) {
                Log.e(TAG, "ClientTask closed by server on " + msgs[1]);
                helper.switch_sequencer(msgs[1], myPort);
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException on " + msgs[1] + " " +
                        e.getMessage());
                helper.switch_sequencer(msgs[1], myPort);
            } catch (Exception e) {
                Log.e(TAG, "ClientTask Exception on " + msgs[1]);
            }
            return null;
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
                    helper.sequencerHelper(inputObject, mUri, myPort, mContentResolver);
                    publishProgress(inputObject.msg);
                    client.close();
                } catch (IOException e) {
                    Log.e(TAG, "ServerTask socket IOException" + e.getMessage());
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
}
