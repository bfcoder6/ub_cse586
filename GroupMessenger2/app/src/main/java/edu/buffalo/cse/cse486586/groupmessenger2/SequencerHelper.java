package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class SequencerHelper {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    int msgCount = 0;
    String sequencer = "11108";
    private BlockingQueue<String> hold_back = new ArrayBlockingQueue<String>(30);

    public void sequencerHelper(MyMessage inputObject,
                                Uri mUri,
                                String myPort,
                                ContentResolver mContentResolver) throws InterruptedException {
        String inputMsg = inputObject.msg;
        if(inputObject.type == MyMessage.Type.Common) {
            hold_back.put(inputMsg);
            if(myPort.equals(sequencer)) {
                send_seq(hold_back);
            }
            Log.v("recieved common msg: ", inputMsg + " " + inputObject.type +
                    " " + inputObject.ID);
        } else if(inputObject.type == MyMessage.Type.Sequence) {
            ContentValues mContentValue = new ContentValues();
            mContentValue.put("key", inputObject.ID);
            mContentValue.put("value", inputObject.msg);
            mContentResolver.insert(mUri, mContentValue);
            Log.v("recieved seq msg: ", inputMsg + " " + inputObject.type +
                    " " + inputObject.ID);
        } else if(inputObject.type == MyMessage.Type.Switch) {
            sequencer = "11112";
            if(myPort.equals(sequencer)) {
                send_seq(hold_back);
            }
            Log.d(TAG, "recieved Switch");
        }
    }

    public synchronized void send_seq(BlockingQueue<String> hold_back) {
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
                new GroupMessengerActivity.ClientTask().executeOnExecutor(
                        AsyncTask.SERIAL_EXECUTOR,
                        item,
                        REMOTE_PORTS[i],
                        Integer.toString(msgCount),
                        "Sequence");
                Log.d("send seq msg: ",  item + " " + REMOTE_PORTS[i] + " " +
                        msgCount);
            }
            msgCount ++;
        }
    }

    public void switch_sequencer(String sendPort,
                                 String myPort) {
        if(sendPort.equals("11108") && sequencer.equals("11108")) {
            sequencer = "11112";
            if(myPort.equals(sequencer)) {
                send_seq(hold_back);
            }
            for (int i = 0; i < REMOTE_PORTS.length; i++) {
                new GroupMessengerActivity.ClientTask().executeOnExecutor(
                        AsyncTask.SERIAL_EXECUTOR,
                        "Switch",
                        REMOTE_PORTS[i],
                        "-1",
                        "Switch");
                Log.d(TAG, "Send Switch msg: "  + REMOTE_PORTS[i]);
            }
        }
    }

    //                if (data == -1) {
//                    Log.e("SocketStatus", "Socket is closed (FIN received):"
//                            + msgs[1]);
//                    throw new IOException("Socket is closed: " + msgs[1]);
//                }
}
