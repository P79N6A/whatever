package org.springframework.boot.cloud;

import org.springframework.core.env.*;

public enum CloudPlatform {

    CLOUD_FOUNDRY {
        @Override
        public boolean isActive(Environment environment) {
            return environment.containsProperty("VCAP_APPLICATION") || environment.containsProperty("VCAP_SERVICES");
        }

    },

    HEROKU {
        @Override
        public boolean isActive(Environment environment) {
            return environment.containsProperty("DYNO");
        }

    },

    SAP {
        @Override
        public boolean isActive(Environment environment) {
            return environment.containsProperty("HC_LANDSCAPE");
        }

    },

    KUBERNETES {
        private static final String SERVICE_HOST_SUFFIX = "_SERVICE_HOST";
        private static final String SERVICE_PORT_SUFFIX = "_SERVICE_PORT";

        @Override
        public boolean isActive(Environment environment) {
            if (environment instanceof ConfigurableEnvironment) {
                return isActive((ConfigurableEnvironment) environment);
            }
            return false;
        }

        private boolean isActive(ConfigurableEnvironment environment) {
            PropertySource<?> environmentPropertySource = environment.getPropertySources().get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            if (environmentPropertySource instanceof EnumerablePropertySource) {
                return isActive((EnumerablePropertySource<?>) environmentPropertySource);
            }
            return false;
        }

        private boolean isActive(EnumerablePropertySource<?> environmentPropertySource) {
            for (String propertyName : environmentPropertySource.getPropertyNames()) {
                if (propertyName.endsWith(SERVICE_HOST_SUFFIX)) {
                    String serviceName = propertyName.substring(0, propertyName.length() - SERVICE_HOST_SUFFIX.length());
                    if (environmentPropertySource.getProperty(serviceName + SERVICE_PORT_SUFFIX) != null) {
                        return true;
                    }
                }
            }
            return false;
        }

    };

    public abstract boolean isActive(Environment environment);

    public boolean isUsingForwardHeaders() {
        return true;
    }

    public static CloudPlatform getActive(Environment environment) {
        if (environment != null) {
            for (CloudPlatform cloudPlatform : values()) {
                if (cloudPlatform.isActive(environment)) {
                    return cloudPlatform;
                }
            }
        }
        return null;
    }

}
