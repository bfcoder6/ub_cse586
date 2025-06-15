package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by yx on 12/14/24.
 */

public class MyMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        Common,
        Sequence
    }

    public String msg;
    public String from;
    public String ID;

    public Type type;

    public MyMessage(String msg, String from, String ID) {
        this.msg = msg;
        this.from = from;
        this.ID = ID;
        this.type = Type.Common;
    }
}
