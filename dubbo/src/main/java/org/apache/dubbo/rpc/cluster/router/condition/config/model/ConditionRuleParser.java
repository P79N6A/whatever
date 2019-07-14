package org.apache.dubbo.rpc.cluster.router.condition.config.model;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class ConditionRuleParser {

    public static ConditionRouterRule parse(String rawRule) {
        Constructor constructor = new Constructor(ConditionRouterRule.class);
        Yaml yaml = new Yaml(constructor);
        ConditionRouterRule rule = yaml.load(rawRule);
        rule.setRawRule(rawRule);
        if (CollectionUtils.isEmpty(rule.getConditions())) {
            rule.setValid(false);
        }
        return rule;
    }

}
