package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    final String remotePort = "11112";
    static final int SERVER_PORT = 10000;
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private ContentResolver mContentResolver;
    private Uri mUri;
    private ContentValues mContentValue;
    static int msgCount = 0;
    //final String myPort = "11108";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        final EditText sendMessage = (EditText) findViewById(R.id.editText1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        mContentResolver = getContentResolver();
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");

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

        findViewById(R.id.button4).setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        String msg = sendMessage.getText().toString() + "\n";
                        sendMessage.setText("");
                        for (int i = 0; i < REMOTE_PORTS.length; i++) {
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORTS[i]);
                        }
                    }
                });

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            //new ServerTask().execute(serverSocket);

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



    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            while(true) {
                try {
                    Socket client = serverSocket.accept();
                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String input;

                    input = in.readLine();
                    if(input == null) {
                        publishProgress("");
                        return null;
                    }
                    publishProgress(input);

                    String key = Integer.toString(msgCount);
                    String val = new String(input);

                    mContentValue = new ContentValues();
                    mContentValue.put(KEY_FIELD, Integer.toString(msgCount));
                    mContentValue.put(VALUE_FIELD, input);
                    mContentResolver.insert(mUri, mContentValue);
//                    Cursor resultCursor =
//                            mContentResolver.query(mUri, null, Integer.toString(msgCount), null, null);
//                    if (resultCursor == null) {
//                        Log.e(TAG, "Result null");
//                        return null;
//                    }
//                    int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
//                    int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
//                    if (keyIndex == -1 || valueIndex == -1) {
//                        Log.e(TAG, "Wrong columns");
//                        resultCursor.close();
//                        return null;
//                    }
//                    resultCursor.moveToFirst();
//                    if (!(resultCursor.isFirst() && resultCursor.isLast())) {
//                        Log.e(TAG, "Wrong number of rows");
//                        resultCursor.close();
//                        return null;
//                    }
//                    String returnKey = resultCursor.getString(keyIndex);
//                    String returnValue = resultCursor.getString(valueIndex);
//                    if (!(returnKey.equals(key) && returnValue.equals(val))) {
//                        Log.e(TAG, "(key, value) pairs don't match\n");
//                        resultCursor.close();
//                        return null;
//                    }

                    //resultCursor.close();
                    msgCount++;
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

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
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(msgs[1]));

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(msgs[0]);
               // out.flush();
               // out.close();
               // socket.close();
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }
    }
}
