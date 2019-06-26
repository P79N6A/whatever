package org.apache.ibatis.io;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import java.io.*;
import java.util.Properties;

@Deprecated
public class ExternalResources {

    private static final Log log = LogFactory.getLog(ExternalResources.class);

    private ExternalResources() {

    }

    public static void copyExternalResource(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileInputStream source = new FileInputStream(sourceFile); FileOutputStream destination = new FileOutputStream(destFile)) {
            destination.getChannel().transferFrom(source.getChannel(), 0, source.getChannel().size());
        }

    }

    public static String getConfiguredTemplate(String templatePath, String templateProperty) throws FileNotFoundException {
        String templateName = "";
        Properties migrationProperties = new Properties();

        try {
            migrationProperties.load(new FileInputStream(templatePath));
            templateName = migrationProperties.getProperty(templateProperty);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("", e);
        }

        return templateName;
    }

}
