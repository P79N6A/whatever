package org.springframework.transaction.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings("serial")
public class NameMatchTransactionAttributeSource implements TransactionAttributeSource, Serializable {

    protected static final Log logger = LogFactory.getLog(NameMatchTransactionAttributeSource.class);

    private Map<String, TransactionAttribute> nameMap = new HashMap<>();

    public void setNameMap(Map<String, TransactionAttribute> nameMap) {
        nameMap.forEach(this::addTransactionalMethod);
    }

    public void setProperties(Properties transactionAttributes) {
        TransactionAttributeEditor tae = new TransactionAttributeEditor();
        Enumeration<?> propNames = transactionAttributes.propertyNames();
        while (propNames.hasMoreElements()) {
            String methodName = (String) propNames.nextElement();
            String value = transactionAttributes.getProperty(methodName);
            tae.setAsText(value);
            TransactionAttribute attr = (TransactionAttribute) tae.getValue();
            addTransactionalMethod(methodName, attr);
        }
    }

    public void addTransactionalMethod(String methodName, TransactionAttribute attr) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding transactional method [" + methodName + "] with attribute [" + attr + "]");
        }
        this.nameMap.put(methodName, attr);
    }

    @Override
    @Nullable
    public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
        if (!ClassUtils.isUserLevelMethod(method)) {
            return null;
        }
        // Look for direct name match.
        String methodName = method.getName();
        TransactionAttribute attr = this.nameMap.get(methodName);
        if (attr == null) {
            // Look for most specific name match.
            String bestNameMatch = null;
            for (String mappedName : this.nameMap.keySet()) {
                if (isMatch(methodName, mappedName) && (bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
                    attr = this.nameMap.get(mappedName);
                    bestNameMatch = mappedName;
                }
            }
        }
        return attr;
    }

    protected boolean isMatch(String methodName, String mappedName) {
        return PatternMatchUtils.simpleMatch(mappedName, methodName);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NameMatchTransactionAttributeSource)) {
            return false;
        }
        NameMatchTransactionAttributeSource otherTas = (NameMatchTransactionAttributeSource) other;
        return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
    }

    @Override
    public int hashCode() {
        return NameMatchTransactionAttributeSource.class.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + this.nameMap;
    }

}
