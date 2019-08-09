package org.springframework.transaction.config;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

public class JtaTransactionManagerFactoryBean implements FactoryBean<JtaTransactionManager>, InitializingBean {

    private static final String WEBLOGIC_JTA_TRANSACTION_MANAGER_CLASS_NAME = "org.springframework.transaction.jta.WebLogicJtaTransactionManager";

    private static final String WEBSPHERE_TRANSACTION_MANAGER_CLASS_NAME = "org.springframework.transaction.jta.WebSphereUowTransactionManager";

    private static final String JTA_TRANSACTION_MANAGER_CLASS_NAME = "org.springframework.transaction.jta.JtaTransactionManager";

    private static final boolean weblogicPresent;

    private static final boolean webspherePresent;

    static {
        ClassLoader classLoader = JtaTransactionManagerFactoryBean.class.getClassLoader();
        weblogicPresent = ClassUtils.isPresent("weblogic.transaction.UserTransaction", classLoader);
        webspherePresent = ClassUtils.isPresent("com.ibm.wsspi.uow.UOWManager", classLoader);
    }

    private final JtaTransactionManager transactionManager;

    @SuppressWarnings("unchecked")
    public JtaTransactionManagerFactoryBean() {
        String className = resolveJtaTransactionManagerClassName();
        try {
            Class<? extends JtaTransactionManager> clazz = (Class<? extends JtaTransactionManager>) ClassUtils.forName(className, JtaTransactionManagerFactoryBean.class.getClassLoader());
            this.transactionManager = BeanUtils.instantiateClass(clazz);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to load JtaTransactionManager class: " + className, ex);
        }
    }

    @Override
    public void afterPropertiesSet() throws TransactionSystemException {
        this.transactionManager.afterPropertiesSet();
    }

    @Override
    @Nullable
    public JtaTransactionManager getObject() {
        return this.transactionManager;
    }

    @Override
    public Class<?> getObjectType() {
        return this.transactionManager.getClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    static String resolveJtaTransactionManagerClassName() {
        if (weblogicPresent) {
            return WEBLOGIC_JTA_TRANSACTION_MANAGER_CLASS_NAME;
        } else if (webspherePresent) {
            return WEBSPHERE_TRANSACTION_MANAGER_CLASS_NAME;
        } else {
            return JTA_TRANSACTION_MANAGER_CLASS_NAME;
        }
    }

}
