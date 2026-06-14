package edu.buffalo.cse.cse486586.simpledht;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    private HashSet<String> insert_keys = new HashSet();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        Log.v("insert", values.toString());
        String key = values.getAsString("key");
        String value = values.getAsString("value");
        FileOutputStream fos;
        Context context = getContext();
        try {
            fos = context.openFileOutput(key, Context.MODE_PRIVATE);
            fos.write(value.getBytes());
            fos.close();
            insert_keys.add(key);
        } catch (IOException e) {
            Log.e("insert", "IOException" + e.getMessage());
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        Log.v("query", selection);
        Context context = getContext();
        String[] COLUMN_NAME = { "key", "value" };
        MatrixCursor resultCursor=new MatrixCursor(COLUMN_NAME);
        if (selection.equals("*")) {
            queryAll(context, resultCursor);
        } else if (selection.equals("@")) {
            queryAll(context, resultCursor);
        } else {
            queryOne(context, resultCursor, selection);
        }
        return resultCursor;
    }

    private void queryOne(Context context, MatrixCursor resultCursor, String key) {
        Log.v("query_one: ", key);
        StringBuilder value = new StringBuilder();
        try {
            int c;
            FileInputStream fis = context.openFileInput(key);
            value = new StringBuilder();
            while( (c = fis.read() ) != -1 ) {
                value.append((char) c);
            }
            fis.close();
        } catch (FileNotFoundException e) {
            Log.e("query", "FileNotFoundException" + e.getMessage());
        } catch (IOException e) {
            Log.e("query", "IOException" + e.getMessage());
        }
        Log.e("key: " + key, "value: " + value);
        resultCursor.addRow(new Object[] {key, value});
    }

    private void queryAll(Context context, MatrixCursor resultCursor) {
        Log.v("query_all", "");
        for (String key : insert_keys) {
            queryOne(context, resultCursor, key);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
