package org.springframework.boot.autoconfigure.condition;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;

@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnResourceCondition extends SpringBootCondition {

    private final ResourceLoader defaultResourceLoader = new DefaultResourceLoader();

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(ConditionalOnResource.class.getName(), true);
        ResourceLoader loader = (context.getResourceLoader() != null) ? context.getResourceLoader() : this.defaultResourceLoader;
        List<String> locations = new ArrayList<>();
        collectValues(locations, attributes.get("resources"));
        Assert.isTrue(!locations.isEmpty(), "@ConditionalOnResource annotations must specify at " + "least one resource location");
        List<String> missing = new ArrayList<>();
        for (String location : locations) {
            String resource = context.getEnvironment().resolvePlaceholders(location);
            if (!loader.getResource(resource).exists()) {
                missing.add(location);
            }
        }
        if (!missing.isEmpty()) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnResource.class).didNotFind("resource", "resources").items(Style.QUOTE, missing));
        }
        return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnResource.class).found("location", "locations").items(locations));
    }

    private void collectValues(List<String> names, List<Object> values) {
        for (Object value : values) {
            for (Object item : (Object[]) value) {
                names.add((String) item);
            }
        }
    }

}
