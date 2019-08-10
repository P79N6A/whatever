package com.alipay.remoting.config.configs;

public interface NettyConfigure {

    void initWriteBufferWaterMark(int low, int high);

    int netty_buffer_low_watermark();

    int netty_buffer_high_watermark();

}