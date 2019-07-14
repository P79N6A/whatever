package org.apache.dubbo.rpc.cluster.router.tag.model;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class TagRuleParser {

    public static TagRouterRule parse(String rawRule) {
        Constructor constructor = new Constructor(TagRouterRule.class);
        TypeDescription tagDescription = new TypeDescription(TagRouterRule.class);
        tagDescription.addPropertyParameters("tags", Tag.class);
        constructor.addTypeDescription(tagDescription);
        Yaml yaml = new Yaml(constructor);
        TagRouterRule rule = yaml.load(rawRule);
        rule.setRawRule(rawRule);
        if (CollectionUtils.isEmpty(rule.getTags())) {
            rule.setValid(false);
        }
        rule.init();
        return rule;
    }

}
