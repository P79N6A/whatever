package com.alipay.remoting.util;

import java.util.zip.CRC32;

public class CrcUtil {

    private static final ThreadLocal<CRC32> CRC_32_THREAD_LOCAL = ThreadLocal.withInitial(CRC32::new);

    public static final int crc32(byte[] array) {
        if (array != null) {
            return crc32(array, 0, array.length);
        }
        return 0;
    }

    public static final int crc32(byte[] array, int offset, int length) {
        CRC32 crc32 = CRC_32_THREAD_LOCAL.get();
        crc32.update(array, offset, length);
        int ret = (int) crc32.getValue();
        crc32.reset();
        return ret;
    }

}
