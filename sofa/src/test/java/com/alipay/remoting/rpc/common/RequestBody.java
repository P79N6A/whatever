package com.alipay.remoting.rpc.common;

import java.io.Serializable;
import java.util.Random;

public class RequestBody implements Serializable {

    private static final long serialVersionUID = -1288207208017808618L;

    public static final String DEFAULT_CLIENT_STR = "HELLO WORLD! I'm from client";

    public static final String DEFAULT_SERVER_STR = "HELLO WORLD! I'm from server";

    public static final String DEFAULT_SERVER_RETURN_STR = "HELLO WORLD! I'm server return";

    public static final String DEFAULT_CLIENT_RETURN_STR = "HELLO WORLD! I'm client return";

    public static final String DEFAULT_ONEWAY_STR = "HELLO WORLD! I'm oneway req";

    public static final String DEFAULT_SYNC_STR = "HELLO WORLD! I'm sync req";

    public static final String DEFAULT_FUTURE_STR = "HELLO WORLD! I'm future req";

    public static final String DEFAULT_CALLBACK_STR = "HELLO WORLD! I'm call back req";

    private int id;

    private String msg;

    private byte[] body;

    private Random r = new Random();

    public RequestBody() {
    }

    public RequestBody(int id, String msg) {
        this.id = id;
        this.msg = msg;
    }

    public RequestBody(int id, int size) {
        this.id = id;
        this.msg = "";
        this.body = new byte[size];
        r.nextBytes(this.body);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "Body[this.id = " + id + ", this.msg = " + msg + "]";
    }

    static public enum InvokeType {
        ONEWAY, SYNC, FUTURE, CALLBACK;
    }

}