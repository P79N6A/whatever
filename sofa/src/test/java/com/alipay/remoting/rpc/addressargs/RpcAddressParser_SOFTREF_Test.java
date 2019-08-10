package com.alipay.remoting.rpc.addressargs;

import com.alipay.remoting.Url;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcAddressParser;
import org.junit.Assert;

public class RpcAddressParser_SOFTREF_Test {

    public void testParserNonProtocol() throws RemotingException {
        String url = "127.0.0.1:1111?_TIMEOUT=3000&_SERIALIZETYPE=hessian2";
        RpcAddressParser parser = new RpcAddressParser();
        int MAX = 1000000;
        printMemory();
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < MAX; ++i) {
            Url btUrl = parser.parse(url);
            Assert.assertEquals(btUrl.getUniqueKey(), "127.0.0.1:1111");
        }
        long end1 = System.currentTimeMillis();
        long time1 = end1 - start1;
        System.out.println("time1:" + time1);
        printMemory();
    }

    private void printMemory() {
        int mb = 1024 * 1024;
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long max = rt.maxMemory();
        long free = rt.freeMemory();
        System.out.print("total[" + total / mb + "mb] ");
        System.out.print("max[" + max / mb + "mb] ");
        System.out.println("free[" + free / mb + "mb]");
    }

}
