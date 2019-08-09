package org.springframework.context.annotation;

public interface ConfigurationCondition extends Condition {

    ConfigurationPhase getConfigurationPhase();

    /**
     * 在创建Configuration类的时候过滤还是在创建Bean的时候过滤
     */
    enum ConfigurationPhase {

        PARSE_CONFIGURATION,

        REGISTER_BEAN
    }

}
