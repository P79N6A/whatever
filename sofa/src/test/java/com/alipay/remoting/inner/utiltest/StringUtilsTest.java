package com.alipay.remoting.inner.utiltest;

import com.alipay.remoting.util.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StringUtilsTest {

    @Before
    public void init() {
    }

    @After
    public void stop() {
    }

    @Test
    public void testIsEmpty() {
        Assert.assertTrue(StringUtils.isEmpty(null));
        Assert.assertTrue(StringUtils.isEmpty(""));
        Assert.assertFalse(StringUtils.isEmpty(" "));
        Assert.assertFalse(StringUtils.isEmpty("bob"));
        Assert.assertFalse(StringUtils.isEmpty("  bob  "));
        Assert.assertFalse(StringUtils.isNotEmpty(null));
        Assert.assertFalse(StringUtils.isNotEmpty(""));
        Assert.assertTrue(StringUtils.isNotEmpty(" "));
        Assert.assertTrue(StringUtils.isNotEmpty("bob"));
        Assert.assertTrue(StringUtils.isNotEmpty("  bob  "));
    }

    @Test
    public void testIsBlank() {
        Assert.assertTrue(StringUtils.isBlank(null));
        Assert.assertTrue(StringUtils.isBlank(""));
        Assert.assertTrue(StringUtils.isBlank(" "));
        Assert.assertFalse(StringUtils.isBlank("bob"));
        Assert.assertFalse(StringUtils.isBlank("  bob  "));
        Assert.assertFalse(StringUtils.isNotBlank(null));
        Assert.assertFalse(StringUtils.isNotBlank(""));
        Assert.assertFalse(StringUtils.isNotBlank(" "));
        Assert.assertTrue(StringUtils.isNotBlank("bob"));
        Assert.assertTrue(StringUtils.isNotBlank("  bob  "));
    }

    @Test
    public void testIsNumeric() {
        Assert.assertTrue(StringUtils.isNumeric("11"));
        Assert.assertFalse(StringUtils.isNumeric("1a1"));
        Assert.assertFalse(StringUtils.isNumeric("aa"));
        Assert.assertTrue(StringUtils.isNumeric(""));
        Assert.assertFalse(StringUtils.isNumeric("  "));
        Assert.assertFalse(StringUtils.isNumeric(" a "));
        Assert.assertFalse(StringUtils.isNumeric("  123  "));
    }

    @Test
    public void testSplit() {
        String test = "127.0.0.1:12200?key1=value1&";
        String[] splitted = StringUtils.split(test, '?');
        Assert.assertEquals(splitted[0], "127.0.0.1:12200");
        Assert.assertEquals(splitted[1], "key1=value1&");
    }

    @Test
    public void testEquals() {
        String t = "hehe";
        String t1 = "hehe";
        String b = "hehehe";
        Assert.assertTrue(StringUtils.equals(t, t1));
        Assert.assertFalse(StringUtils.equals(t, b));
    }

}