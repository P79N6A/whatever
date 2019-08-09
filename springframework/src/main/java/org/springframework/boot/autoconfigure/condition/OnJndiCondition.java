package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.util.StringUtils;

import javax.naming.NamingException;

@Order(Ordered.LOWEST_PRECEDENCE - 20)
class OnJndiCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(ConditionalOnJndi.class.getName()));
        String[] locations = annotationAttributes.getStringArray("value");
        try {
            return getMatchOutcome(locations);
        } catch (NoClassDefFoundError ex) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnJndi.class).because("JNDI class not found"));
        }
    }

    private ConditionOutcome getMatchOutcome(String[] locations) {
        if (!isJndiAvailable()) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnJndi.class).notAvailable("JNDI environment"));
        }
        if (locations.length == 0) {
            return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnJndi.class).available("JNDI environment"));
        }
        JndiLocator locator = getJndiLocator(locations);
        String location = locator.lookupFirstLocation();
        String details = "(" + StringUtils.arrayToCommaDelimitedString(locations) + ")";
        if (location != null) {
            return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnJndi.class, details).foundExactly("\"" + location + "\""));
        }
        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnJndi.class, details).didNotFind("any matching JNDI location").atAll());
    }

    protected boolean isJndiAvailable() {
        return JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable();
    }

    protected JndiLocator getJndiLocator(String[] locations) {
        return new JndiLocator(locations);
    }

    protected static class JndiLocator extends JndiLocatorSupport {

        private String[] locations;

        public JndiLocator(String[] locations) {
            this.locations = locations;
        }

        public String lookupFirstLocation() {
            for (String location : this.locations) {
                try {
                    lookup(location);
                    return location;
                } catch (NamingException ex) {
                    // Swallow and continue
                }
            }
            return null;
        }

    }

}
