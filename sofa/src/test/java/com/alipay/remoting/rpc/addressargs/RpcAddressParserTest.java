package com.alipay.remoting.rpc.addressargs;

import com.alipay.remoting.Url;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcAddressParser;
import com.alipay.remoting.rpc.RpcConfigs;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcAddressParserTest {
    static Logger logger = LoggerFactory.getLogger(RpcAddressParserTest.class);

    @Test
    public void testParserNonProtocol() throws RemotingException {
        String url = "127.0.0.1:1111?_TIMEOUT=3000&_SERIALIZETYPE=hessian2";
        RpcAddressParser parser = new RpcAddressParser();
        Url btUrl = parser.parse(url);
        Assert.assertEquals("127.0.0.1", btUrl.getIp());
        Assert.assertEquals(1111, btUrl.getPort());
        Assert.assertEquals(null, btUrl.getProperty(RpcConfigs.URL_PROTOCOL));
        url = "127.0.0.1:1111";
        btUrl = parser.parse(url);
        Assert.assertEquals("127.0.0.1", btUrl.getIp());
        Assert.assertEquals(1111, btUrl.getPort());
        Assert.assertEquals(null, btUrl.getProperty(RpcConfigs.URL_PROTOCOL));
    }

    @Test
    public void testParserWithProtocol() throws RemotingException {
        String url = "127.0.0.1:1111?_TIMEOUT=3000&_SERIALIZETYPE=hessian2&_PROTOCOL=1";
        RpcAddressParser parser = new RpcAddressParser();
        Url btUrl = parser.parse(url);
        Assert.assertEquals("127.0.0.1", btUrl.getIp());
        Assert.assertEquals(1111, btUrl.getPort());
        Assert.assertEquals("1", btUrl.getProperty(RpcConfigs.URL_PROTOCOL));
        url = "127.0.0.1:1111?protocol=1";
        Assert.assertEquals("127.0.0.1", btUrl.getIp());
        Assert.assertEquals(1111, btUrl.getPort());
        Assert.assertEquals("1", btUrl.getProperty(RpcConfigs.URL_PROTOCOL));
    }

    @Test
    public void testParserConnectTimeout() throws RemotingException {
        String url = "127.0.0.1:1111?_CONNECTTIMEOUT=2000&_TIMEOUT=3000&_SERIALIZETYPE=hessian2&_PROTOCOL=1";
        RpcAddressParser parser = new RpcAddressParser();
        Url btUrl = parser.parse(url);
        Assert.assertEquals("127.0.0.1", btUrl.getIp());
        Assert.assertEquals(1111, btUrl.getPort());
        Assert.assertEquals("1", btUrl.getProperty(RpcConfigs.URL_PROTOCOL));
        Assert.assertEquals("2000", btUrl.getProperty(RpcConfigs.CONNECT_TIMEOUT_KEY));
    }

    @Test
    public void testUrlIllegal() throws RemotingException {
        Url btUrl = null;
        try {
            String url = "127.0.0.1";
            RpcAddressParser parser = new RpcAddressParser();
            btUrl = parser.parse(url);
            Assert.fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
            logger.error("URL FORMAT ERROR!", e);
            Assert.assertNull(btUrl);
        }
        btUrl = null;
        try {
            String url = "127.0.0.1:";
            RpcAddressParser parser = new RpcAddressParser();
            btUrl = parser.parse(url);
            Assert.fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
            logger.error("URL FORMAT ERROR!", e);
            Assert.assertNull(btUrl);
        }
        btUrl = null;
        try {
            String url = "127.0.0.1:1111?_CONNECTTIMEOUT";
            RpcAddressParser parser = new RpcAddressParser();
            btUrl = parser.parse(url);
            Assert.fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
            logger.error("URL FORMAT ERROR!", e);
            Assert.assertNull(btUrl);
        }
        btUrl = null;
        try {
            String url = "127.0.0.1:100?";
            RpcAddressParser parser = new RpcAddressParser();
            btUrl = parser.parse(url);
            Assert.fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
            logger.error("URL FORMAT ERROR!", e);
            Assert.assertNull(btUrl);
        }
        btUrl = null;
        try {
            String url = "127.0.1:100?A=B&";
            RpcAddressParser parser = new RpcAddressParser();
            btUrl = parser.parse(url);
            Assert.fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
            logger.error("URL FORMAT ERROR!", e);
            Assert.assertNull(btUrl);
        }
        btUrl = null;
        try {
            String url = "127.0.1:100?A=B&C";
            RpcAddressParser parser = new RpcAddressParser();
            btUrl = parser.parse(url);
            Assert.fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
            logger.error("URL FORMAT ERROR!", e);
            Assert.assertNull(btUrl);
        }
        btUrl = null;
        try {
            String url = "127.0.1:100?A=B&C=";
            RpcAddressParser parser = new RpcAddressParser();
            btUrl = parser.parse(url);
            Assert.fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
            logger.error("URL FORMAT ERROR!", e);
            Assert.assertNull(btUrl);
        }
    }

    @Test
    public void testArgsIllegal() {
        Url btUrl = null;
        try {
            String url = "127.0.0.1:1111?_CONNECTTIMEOUT=-1&_TIMEOUT=3000&_SERIALIZETYPE=hessian2&_PROTOCOL=1";
            RpcAddressParser parser = new RpcAddressParser();
            btUrl = parser.parse(url);
            parser.initUrlArgs(btUrl);
            Assert.fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
            logger.error("URL ARGS ILLEAGL!", e);
        }
        btUrl = null;
        try {
            String url = "127.0.0.1:1111?_CONNECTTIMEOUT=1000&_TIMEOUT=3000&_SERIALIZETYPE=hessian2&_PROTOCOL=-11";
            RpcAddressParser parser = new RpcAddressParser();
            btUrl = parser.parse(url);
            parser.initUrlArgs(btUrl);
            Assert.fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
            logger.error("URL ARGS ILLEAGL!", e);
        }
        btUrl = null;
        try {
            String url = "127.0.0.1:1111?_CONNECTTIMEOUT=1000&_TIMEOUT=3000&_SERIALIZETYPE=hessian2&_CONNECTIONNUM=-11";
            RpcAddressParser parser = new RpcAddressParser();
            btUrl = parser.parse(url);
            parser.initUrlArgs(btUrl);
            Assert.fail("Should not reach here!");
        } catch (IllegalArgumentException e) {
            logger.error("URL ARGS ILLEAGL!", e);
        }
    }

}
