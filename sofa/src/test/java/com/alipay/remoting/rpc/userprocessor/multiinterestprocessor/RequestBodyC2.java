package com.alipay.remoting.rpc.userprocessor.multiinterestprocessor;

import java.util.Random;

public class RequestBodyC2 implements MultiInterestBaseRequestBody {

    private static final long serialVersionUID = -7512540836127308096L;

    public static final String DEFAULT_CLIENT_STR = "HELLO WORLD! I'm from client--C2";

    public static final String DEFAULT_SERVER_STR = "HELLO WORLD! I'm from server--C2";

    public static final String DEFAULT_SERVER_RETURN_STR = "HELLO WORLD! I'm server return--C2";

    public static final String DEFAULT_CLIENT_RETURN_STR = "HELLO WORLD! I'm client return--C2";

    public static final String DEFAULT_ONEWAY_STR = "HELLO WORLD! I'm oneway req--C2";

    public static final String DEFAULT_SYNC_STR = "HELLO WORLD! I'm sync req--C2";

    public static final String DEFAULT_FUTURE_STR = "HELLO WORLD! I'm future req--C2";

    public static final String DEFAULT_CALLBACK_STR = "HELLO WORLD! I'm call back req--C2";

    private int id;

    private String msg;

    private byte[] body;

    private Random r = new Random();

    public RequestBodyC2() {
    }

    public RequestBodyC2(int id, String msg) {
        this.id = id;
        this.msg = msg;
    }

    public RequestBodyC2(int id, int size) {
        this.id = id;
        this.msg = "";
        this.body = new byte[size];
        r.nextBytes(this.body);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "Body[this.id = " + this.id + ", this.msg = " + this.msg + "]";
    }

}
