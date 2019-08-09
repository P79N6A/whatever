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

package org.springframework.boot.autoconfigure.jdbc;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DataSourceTransactionManager}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({JdbcTemplate.class, PlatformTransactionManager.class})
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(DataSourceProperties.class) // spring.datasource
public class DataSourceTransactionManagerAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnSingleCandidate(DataSource.class) // 容器中只有一个DataSource的Bean，或者这个DataSource的Bean是首选Bean
    static class DataSourceTransactionManagerConfiguration {

        /**
         * 注册DataSourceTransactionManager
         * DataSourceTransactionManager实现了PlatformTransactionManager接口
         */
        @Bean
        @ConditionalOnMissingBean(PlatformTransactionManager.class) // 不存在PlatformTransactionManager的Bean时（避免覆盖）
        public DataSourceTransactionManager transactionManager(DataSource dataSource, ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
            // 注入DataSource
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            // 注入TransactionManagerCustomizers，进行自定义
            transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));
            return transactionManager;
        }

    }

}
