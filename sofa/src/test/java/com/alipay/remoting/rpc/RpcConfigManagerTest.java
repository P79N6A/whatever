package com.alipay.remoting.rpc;

import org.junit.*;

public class RpcConfigManagerTest {
    @BeforeClass
    public static void initClass() {
    }

    @Before
    public void init() {
    }

    @After
    public void stop() {
    }

    @AfterClass
    public static void afterClass() {
    }

    @Test
    public void testSystemSettings() {
        Assert.assertTrue(RpcConfigManager.dispatch_msg_list_in_default_executor());
    }

}
