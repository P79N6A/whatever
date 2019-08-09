package org.springframework.boot.autoconfigure.web;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

class OnEnabledResourceChainCondition extends SpringBootCondition {

    private static final String WEBJAR_ASSET_LOCATOR = "org.webjars.WebJarAssetLocator";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();
        boolean fixed = getEnabledProperty(environment, "strategy.fixed.", false);
        boolean content = getEnabledProperty(environment, "strategy.content.", false);
        Boolean chain = getEnabledProperty(environment, "", null);
        Boolean match = ResourceProperties.Chain.getEnabled(fixed, content, chain);
        ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnEnabledResourceChain.class);
        if (match == null) {
            if (ClassUtils.isPresent(WEBJAR_ASSET_LOCATOR, getClass().getClassLoader())) {
                return ConditionOutcome.match(message.found("class").items(WEBJAR_ASSET_LOCATOR));
            }
            return ConditionOutcome.noMatch(message.didNotFind("class").items(WEBJAR_ASSET_LOCATOR));
        }
        if (match) {
            return ConditionOutcome.match(message.because("enabled"));
        }
        return ConditionOutcome.noMatch(message.because("disabled"));
    }

    private Boolean getEnabledProperty(ConfigurableEnvironment environment, String key, Boolean defaultValue) {
        String name = "spring.resources.chain." + key + "enabled";
        return environment.getProperty(name, Boolean.class, defaultValue);
    }

}
