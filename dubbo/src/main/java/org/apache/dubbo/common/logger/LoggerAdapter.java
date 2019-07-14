package org.apache.dubbo.common.logger;

import org.apache.dubbo.common.extension.SPI;

import java.io.File;

@SPI
public interface LoggerAdapter {

    Logger getLogger(Class<?> key);

    Logger getLogger(String key);

    Level getLevel();

    void setLevel(Level level);

    File getFile();

    void setFile(File file);

}