package org.apache.dubbo.rpc.cluster.router.condition.config.model;

import org.apache.dubbo.rpc.cluster.router.AbstractRouterRule;

import java.util.List;

public class ConditionRouterRule extends AbstractRouterRule {
    public ConditionRouterRule() {
    }

    private List<String> conditions;

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

}
