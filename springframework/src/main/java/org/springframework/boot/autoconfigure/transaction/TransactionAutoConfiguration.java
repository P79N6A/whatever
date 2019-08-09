/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.transaction;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AbstractTransactionManagementConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
// import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
// import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
// import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for Spring transaction.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(PlatformTransactionManager.class)
@AutoConfigureAfter({
        // JtaAutoConfiguration.class,
        // HibernateJpaAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class, // 在DataSourceTransactionManager之后
        // Neo4jDataAutoConfiguration.class
})
@EnableConfigurationProperties(TransactionProperties.class)
public class TransactionAutoConfiguration {

    /**
     * 包装PlatformTransactionManagerCustomizer，对PlatformTransactionManager作自定义处理
     */
    @Bean
    @ConditionalOnMissingBean // 不存在时
    public TransactionManagerCustomizers platformTransactionManagerCustomizers(ObjectProvider<PlatformTransactionManagerCustomizer<?>> customizers) {
        return new TransactionManagerCustomizers(customizers.orderedStream().collect(Collectors.toList()));
    }


    @Configuration(proxyBeanMethods = false)
    @ConditionalOnSingleCandidate(PlatformTransactionManager.class) // 存在一个PlatformTransactionManager的Bean
    public static class TransactionTemplateConfiguration {
        /**
         * 注册TransactionTemplate：用于编程式事务
         */
        @Bean
        @ConditionalOnMissingBean
        public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(PlatformTransactionManager.class)
    @ConditionalOnMissingBean(AbstractTransactionManagementConfiguration.class)
    public static class EnableTransactionManagementConfiguration {

        @Configuration(proxyBeanMethods = false)
        @EnableTransactionManagement(proxyTargetClass = false) // @EnableTransactionManagement
        @ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "false", matchIfMissing = false)
        public static class JdkDynamicAutoProxyConfiguration {
            /*
             * 引入EnableTransactionManagement
             * EnableTransactionManagement引入TransactionManagementConfigurationSelector
             * TransactionManagementConfigurationSelector根据AdviceMode是PROXY还是ASPECTJ导入不同的配置
             * 默认PROXY模式，引入AutoProxyRegistrar和ProxyTransactionManagementConfiguration
             * AutoProxyRegistrar：
             *      注册InfrastructureAdvisorAutoProxyCreator
             *          InfrastructureAdvisorAutoProxyCreator实现了InstantiationAwareBeanPostProcessor接口，拦截Bean并寻找合适的Advisor，创建代理
             *          InfrastructureAdvisorAutoProxyCreator
             *          -> AbstractAdvisorAutoProxyCreator
             *          -> AbstractAutoProxyCreator
             *          -> SmartInstantiationAwareBeanPostProcessor
             *          -> InstantiationAwareBeanPostProcessor
             * ProxyTransactionManagementConfiguration：
             *      注册BeanFactoryTransactionAttributeSourceAdvisor：Advisor
             *      注册AnnotationTransactionAttributeSource：匹配规则，处理@Transactional注解
             *      注册TransactionInterceptor：事务拦截器
             */
        }

        @Configuration(proxyBeanMethods = false)
        @EnableTransactionManagement(proxyTargetClass = true)
        @ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "true", matchIfMissing = true)
        public static class CglibAutoProxyConfiguration {

        }

    }

}
