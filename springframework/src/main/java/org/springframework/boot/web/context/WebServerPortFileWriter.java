package org.springframework.boot.web.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.system.SystemProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Locale;

public class WebServerPortFileWriter implements ApplicationListener<WebServerInitializedEvent> {

    private static final String DEFAULT_FILE_NAME = "application.port";

    private static final String[] PROPERTY_VARIABLES = {"PORTFILE", "portfile"};

    private static final Log logger = LogFactory.getLog(WebServerPortFileWriter.class);

    private final File file;

    public WebServerPortFileWriter() {
        this(new File(DEFAULT_FILE_NAME));
    }

    public WebServerPortFileWriter(String filename) {
        this(new File(filename));
    }

    public WebServerPortFileWriter(File file) {
        Assert.notNull(file, "File must not be null");
        String override = SystemProperties.get(PROPERTY_VARIABLES);
        if (override != null) {
            this.file = new File(override);
        } else {
            this.file = file;
        }
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        File portFile = getPortFile(event.getApplicationContext());
        try {
            String port = String.valueOf(event.getWebServer().getPort());
            createParentFolder(portFile);
            FileCopyUtils.copy(port.getBytes(), portFile);
            portFile.deleteOnExit();
        } catch (Exception ex) {
            logger.warn(String.format("Cannot create port file %s", this.file));
        }
    }

    protected File getPortFile(ApplicationContext applicationContext) {
        String namespace = getServerNamespace(applicationContext);
        if (StringUtils.isEmpty(namespace)) {
            return this.file;
        }
        String name = this.file.getName();
        String extension = StringUtils.getFilenameExtension(this.file.getName());
        name = name.substring(0, name.length() - extension.length() - 1);
        if (isUpperCase(name)) {
            name = name + "-" + namespace.toUpperCase(Locale.ENGLISH);
        } else {
            name = name + "-" + namespace.toLowerCase(Locale.ENGLISH);
        }
        if (StringUtils.hasLength(extension)) {
            name = name + "." + extension;
        }
        return new File(this.file.getParentFile(), name);
    }

    private String getServerNamespace(ApplicationContext applicationContext) {
        if (applicationContext instanceof WebServerApplicationContext) {
            return ((WebServerApplicationContext) applicationContext).getServerNamespace();
        }
        return null;
    }

    private boolean isUpperCase(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i)) && !Character.isUpperCase(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void createParentFolder(File file) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
    }

}
