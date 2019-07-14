package org.apache.dubbo.remoting.transport.netty4.logging;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ArrayUtils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

final class MessageFormatter {
    private static final Logger logger = LoggerFactory.getLogger(MessageFormatter.class);

    static final char DELIM_START = '{';

    static final char DELIM_STOP = '}';

    static final String DELIM_STR = "{}";

    private static final char ESCAPE_CHAR = '\\';

    static FormattingTuple format(String messagePattern, Object arg) {
        return arrayFormat(messagePattern, new Object[]{arg});
    }

    static FormattingTuple format(final String messagePattern, Object argA, Object argB) {
        return arrayFormat(messagePattern, new Object[]{argA, argB});
    }

    static Throwable getThrowableCandidate(Object[] argArray) {
        if (ArrayUtils.isEmpty(argArray)) {
            return null;
        }
        final Object lastEntry = argArray[argArray.length - 1];
        if (lastEntry instanceof Throwable) {
            return (Throwable) lastEntry;
        }
        return null;
    }

    static FormattingTuple arrayFormat(final String messagePattern, final Object[] argArray) {
        Throwable throwableCandidate = getThrowableCandidate(argArray);
        if (messagePattern == null) {
            return new FormattingTuple(null, argArray, throwableCandidate);
        }
        if (argArray == null) {
            return new FormattingTuple(messagePattern);
        }
        int i = 0;
        int j;
        StringBuffer sbuf = new StringBuffer(messagePattern.length() + 50);
        int l;
        for (l = 0; l < argArray.length; l++) {
            j = messagePattern.indexOf(DELIM_STR, i);
            if (j == -1) {
                if (i == 0) {
                    return new FormattingTuple(messagePattern, argArray, throwableCandidate);
                } else {
                    sbuf.append(messagePattern.substring(i, messagePattern.length()));
                    return new FormattingTuple(sbuf.toString(), argArray, throwableCandidate);
                }
            } else {
                if (isEscapedDelimeter(messagePattern, j)) {
                    if (!isDoubleEscaped(messagePattern, j)) {
                        l--;
                        sbuf.append(messagePattern.substring(i, j - 1));
                        sbuf.append(DELIM_START);
                        i = j + 1;
                    } else {
                        sbuf.append(messagePattern.substring(i, j - 1));
                        deeplyAppendParameter(sbuf, argArray[l], new HashMap<Object[], Void>());
                        i = j + 2;
                    }
                } else {
                    sbuf.append(messagePattern.substring(i, j));
                    deeplyAppendParameter(sbuf, argArray[l], new HashMap<Object[], Void>());
                    i = j + 2;
                }
            }
        }
        sbuf.append(messagePattern.substring(i, messagePattern.length()));
        if (l < argArray.length - 1) {
            return new FormattingTuple(sbuf.toString(), argArray, throwableCandidate);
        } else {
            return new FormattingTuple(sbuf.toString(), argArray, null);
        }
    }

    static boolean isEscapedDelimeter(String messagePattern, int delimeterStartIndex) {
        if (delimeterStartIndex == 0) {
            return false;
        }
        return messagePattern.charAt(delimeterStartIndex - 1) == ESCAPE_CHAR;
    }

    static boolean isDoubleEscaped(String messagePattern, int delimeterStartIndex) {
        return delimeterStartIndex >= 2 && messagePattern.charAt(delimeterStartIndex - 2) == ESCAPE_CHAR;
    }

    private static void deeplyAppendParameter(StringBuffer sbuf, Object o, Map<Object[], Void> seenMap) {
        if (o == null) {
            sbuf.append("null");
            return;
        }
        if (!o.getClass().isArray()) {
            safeObjectAppend(sbuf, o);
        } else {
            if (o instanceof boolean[]) {
                booleanArrayAppend(sbuf, (boolean[]) o);
            } else if (o instanceof byte[]) {
                byteArrayAppend(sbuf, (byte[]) o);
            } else if (o instanceof char[]) {
                charArrayAppend(sbuf, (char[]) o);
            } else if (o instanceof short[]) {
                shortArrayAppend(sbuf, (short[]) o);
            } else if (o instanceof int[]) {
                intArrayAppend(sbuf, (int[]) o);
            } else if (o instanceof long[]) {
                longArrayAppend(sbuf, (long[]) o);
            } else if (o instanceof float[]) {
                floatArrayAppend(sbuf, (float[]) o);
            } else if (o instanceof double[]) {
                doubleArrayAppend(sbuf, (double[]) o);
            } else {
                objectArrayAppend(sbuf, (Object[]) o, seenMap);
            }
        }
    }

    private static void safeObjectAppend(StringBuffer sbuf, Object o) {
        try {
            String oAsString = o.toString();
            sbuf.append(oAsString);
        } catch (Throwable t) {
            System.err.println("SLF4J: Failed toString() invocation on an object of type [" + o.getClass().getName() + ']');
            logger.error(t.getMessage(), t);
            sbuf.append("[FAILED toString()]");
        }
    }

    private static void objectArrayAppend(StringBuffer sbuf, Object[] a, Map<Object[], Void> seenMap) {
        sbuf.append('[');
        if (!seenMap.containsKey(a)) {
            seenMap.put(a, null);
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                deeplyAppendParameter(sbuf, a[i], seenMap);
                if (i != len - 1) {
                    sbuf.append(", ");
                }
            }
            seenMap.remove(a);
        } else {
            sbuf.append("...");
        }
        sbuf.append(']');
    }

    private static void booleanArrayAppend(StringBuffer sbuf, boolean[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void byteArrayAppend(StringBuffer sbuf, byte[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void charArrayAppend(StringBuffer sbuf, char[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void shortArrayAppend(StringBuffer sbuf, short[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void intArrayAppend(StringBuffer sbuf, int[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void longArrayAppend(StringBuffer sbuf, long[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void floatArrayAppend(StringBuffer sbuf, float[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private static void doubleArrayAppend(StringBuffer sbuf, double[] a) {
        sbuf.append('[');
        final int len = a.length;
        for (int i = 0; i < len; i++) {
            sbuf.append(a[i]);
            if (i != len - 1) {
                sbuf.append(", ");
            }
        }
        sbuf.append(']');
    }

    private MessageFormatter() {
    }

}
