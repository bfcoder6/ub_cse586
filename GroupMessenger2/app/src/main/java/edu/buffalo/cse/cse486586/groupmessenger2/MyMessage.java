package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yx on 12/14/24.
 */

public class MyMessage {
    private static final long serialVersionUID = 1L;

    public enum Type {
        Common,
        Sequence,
        Switch
    }

    public String msg;
    public String from;
    public String ID;

    public Type type;

    public MyMessage(String msg, String from, String ID, String type) {
        this.msg = msg;
        this.from = from;
        this.ID = ID;
        this.type = Type.valueOf(type);
    }

    @Override
    public String toString() {
        return "MyMessage{" +
                "msg='" + msg + '\'' +
                ", from='" + from + '\'' +
                ", ID='" + ID + '\'' +
                ", type=" + type +
                '}';
    }

    public static MyMessage parse(String input) {
        String content = input.substring(input.indexOf('{') + 1, input.lastIndexOf('}')).trim();
        String[] pairs = content.split(",\\s*");
        Map<String, String> map = new HashMap<String, String>();
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].replaceAll("^'|'$", "");
                map.put(key, value);
            }
        }
        String msg = map.get("msg")
                .replace("\\n", "\n")
                .replace("\\t", "\t");

        return new MyMessage(
                msg,
                map.get("from"),
                map.get("ID"),
                map.get("type")
        );
    }
}
