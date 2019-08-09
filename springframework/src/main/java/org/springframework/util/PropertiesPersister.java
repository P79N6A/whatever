package org.springframework.util;

import java.io.*;
import java.util.Properties;

public interface PropertiesPersister {

    void load(Properties props, InputStream is) throws IOException;

    void load(Properties props, Reader reader) throws IOException;

    void store(Properties props, OutputStream os, String header) throws IOException;

    void store(Properties props, Writer writer, String header) throws IOException;

    void loadFromXml(Properties props, InputStream is) throws IOException;

    void storeToXml(Properties props, OutputStream os, String header) throws IOException;

    void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException;

}
