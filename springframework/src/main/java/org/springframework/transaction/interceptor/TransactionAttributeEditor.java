package org.springframework.transaction.interceptor;

import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

public class TransactionAttributeEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasLength(text)) {
            // tokenize it with ","
            String[] tokens = StringUtils.commaDelimitedListToStringArray(text);
            RuleBasedTransactionAttribute attr = new RuleBasedTransactionAttribute();
            for (String token : tokens) {
                // Trim leading and trailing whitespace.
                String trimmedToken = StringUtils.trimWhitespace(token.trim());
                // Check whether token contains illegal whitespace within text.
                if (StringUtils.containsWhitespace(trimmedToken)) {
                    throw new IllegalArgumentException("Transaction attribute token contains illegal whitespace: [" + trimmedToken + "]");
                }
                // Check token type.
                if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_PROPAGATION)) {
                    attr.setPropagationBehaviorName(trimmedToken);
                } else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_ISOLATION)) {
                    attr.setIsolationLevelName(trimmedToken);
                } else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_TIMEOUT)) {
                    String value = trimmedToken.substring(DefaultTransactionAttribute.PREFIX_TIMEOUT.length());
                    attr.setTimeout(Integer.parseInt(value));
                } else if (trimmedToken.equals(RuleBasedTransactionAttribute.READ_ONLY_MARKER)) {
                    attr.setReadOnly(true);
                } else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_COMMIT_RULE)) {
                    attr.getRollbackRules().add(new NoRollbackRuleAttribute(trimmedToken.substring(1)));
                } else if (trimmedToken.startsWith(RuleBasedTransactionAttribute.PREFIX_ROLLBACK_RULE)) {
                    attr.getRollbackRules().add(new RollbackRuleAttribute(trimmedToken.substring(1)));
                } else {
                    throw new IllegalArgumentException("Invalid transaction attribute token: [" + trimmedToken + "]");
                }
            }
            setValue(attr);
        } else {
            setValue(null);
        }
    }

}
