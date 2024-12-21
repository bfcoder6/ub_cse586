package edu.buffalo.cse.cse486586.groupmessenger1;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by yanxi on 2/14/18.
 */

public class OnSendClickListener implements OnClickListener {

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    final String remotePort = "11112";
    private final EditText sendMessage;
    static final String TAG = GroupMessengerActivity.class.getSimpleName();


    public OnSendClickListener(EditText sendMessage) {
        this.sendMessage = sendMessage;
    }

    @Override
    public void onClick(View v) {
        try {

            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(remotePort));


            String msg = sendMessage.getText().toString() + "\n";
            sendMessage.setText("");
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(msg);
            out.close();
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "ClientTask socket IOException");
        }


    }

    private void send() {

    }


}
