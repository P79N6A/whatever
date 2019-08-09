package org.springframework.transaction.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public class RuleBasedTransactionAttribute extends DefaultTransactionAttribute implements Serializable {

    public static final String PREFIX_ROLLBACK_RULE = "-";

    public static final String PREFIX_COMMIT_RULE = "+";

    private static final Log logger = LogFactory.getLog(RuleBasedTransactionAttribute.class);

    @Nullable
    private List<RollbackRuleAttribute> rollbackRules;

    public RuleBasedTransactionAttribute() {
        super();
    }

    public RuleBasedTransactionAttribute(RuleBasedTransactionAttribute other) {
        super(other);
        this.rollbackRules = (other.rollbackRules != null ? new ArrayList<>(other.rollbackRules) : null);
    }

    public RuleBasedTransactionAttribute(int propagationBehavior, List<RollbackRuleAttribute> rollbackRules) {
        super(propagationBehavior);
        this.rollbackRules = rollbackRules;
    }

    public void setRollbackRules(List<RollbackRuleAttribute> rollbackRules) {
        this.rollbackRules = rollbackRules;
    }

    public List<RollbackRuleAttribute> getRollbackRules() {
        if (this.rollbackRules == null) {
            this.rollbackRules = new LinkedList<>();
        }
        return this.rollbackRules;
    }

    @Override
    public boolean rollbackOn(Throwable ex) {
        if (logger.isTraceEnabled()) {
            logger.trace("Applying rules to determine whether transaction should rollback on " + ex);
        }
        RollbackRuleAttribute winner = null;
        int deepest = Integer.MAX_VALUE;
        if (this.rollbackRules != null) {
            for (RollbackRuleAttribute rule : this.rollbackRules) {
                int depth = rule.getDepth(ex);
                if (depth >= 0 && depth < deepest) {
                    deepest = depth;
                    winner = rule;
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Winning rollback rule is: " + winner);
        }
        // User superclass behavior (rollback on unchecked) if no rule matches.
        if (winner == null) {
            logger.trace("No relevant rollback rule found: applying default rules");
            return super.rollbackOn(ex);
        }
        return !(winner instanceof NoRollbackRuleAttribute);
    }

    @Override
    public String toString() {
        StringBuilder result = getAttributeDescription();
        if (this.rollbackRules != null) {
            for (RollbackRuleAttribute rule : this.rollbackRules) {
                String sign = (rule instanceof NoRollbackRuleAttribute ? PREFIX_COMMIT_RULE : PREFIX_ROLLBACK_RULE);
                result.append(',').append(sign).append(rule.getExceptionName());
            }
        }
        return result.toString();
    }

}
