package org.apache.dubbo.common.config;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
import static org.apache.dubbo.common.constants.ConfigConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.ConfigConstants.SHUTDOWN_WAIT_SECONDS_KEY;

public class ConfigurationUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtils.class);

    @SuppressWarnings("deprecation")
    public static int getServerShutdownTimeout() {
        int timeout = DEFAULT_SERVER_SHUTDOWN_TIMEOUT;
        Configuration configuration = Environment.getInstance().getConfiguration();
        String value = StringUtils.trim(configuration.getString(SHUTDOWN_WAIT_KEY));
        if (value != null && value.length() > 0) {
            try {
                timeout = Integer.parseInt(value);
            } catch (Exception e) {
            }
        } else {
            value = StringUtils.trim(configuration.getString(SHUTDOWN_WAIT_SECONDS_KEY));
            if (value != null && value.length() > 0) {
                try {
                    timeout = Integer.parseInt(value) * 1000;
                } catch (Exception e) {
                }
            }
        }
        return timeout;
    }

    public static String getProperty(String property) {
        return getProperty(property, null);
    }

    public static String getProperty(String property, String defaultValue) {
        return StringUtils.trim(Environment.getInstance().getConfiguration().getString(property, defaultValue));
    }

    public static Map<String, String> parseProperties(String content) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isEmpty(content)) {
            logger.warn("You specified the config centre, but there's not even one single config item in it.");
        } else {
            Properties properties = new Properties();
            properties.load(new StringReader(content));
            properties.stringPropertyNames().forEach(k -> map.put(k, properties.getProperty(k)));
        }
        return map;
    }

}
