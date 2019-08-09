package org.springframework.transaction.aspectj;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.annotation.AbstractTransactionManagementConfiguration;
import org.springframework.transaction.config.TransactionManagementConfigUtils;

@Configuration
public class AspectJTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {

    @Bean(name = TransactionManagementConfigUtils.TRANSACTION_ASPECT_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public AnnotationTransactionAspect transactionAspect() {
        AnnotationTransactionAspect txAspect = AnnotationTransactionAspect.aspectOf();
        if (this.txManager != null) {
            txAspect.setTransactionManager(this.txManager);
        }
        return txAspect;
    }

}
