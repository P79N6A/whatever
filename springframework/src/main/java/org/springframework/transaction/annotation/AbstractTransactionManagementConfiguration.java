package org.springframework.transaction.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.config.TransactionManagementConfigUtils;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

@Configuration
public abstract class AbstractTransactionManagementConfiguration implements ImportAware {

    @Nullable
    protected AnnotationAttributes enableTx;

    @Nullable
    protected TransactionManager txManager;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableTx = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(EnableTransactionManagement.class.getName(), false));
        if (this.enableTx == null) {
            throw new IllegalArgumentException("@EnableTransactionManagement is not present on importing class " + importMetadata.getClassName());
        }
    }

    @Autowired(required = false)
    void setConfigurers(Collection<TransactionManagementConfigurer> configurers) {
        if (CollectionUtils.isEmpty(configurers)) {
            return;
        }
        if (configurers.size() > 1) {
            throw new IllegalStateException("Only one TransactionManagementConfigurer may exist");
        }
        TransactionManagementConfigurer configurer = configurers.iterator().next();
        this.txManager = configurer.annotationDrivenTransactionManager();
    }

    @Bean(name = TransactionManagementConfigUtils.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static TransactionalEventListenerFactory transactionalEventListenerFactory() {
        return new TransactionalEventListenerFactory();
    }

}
