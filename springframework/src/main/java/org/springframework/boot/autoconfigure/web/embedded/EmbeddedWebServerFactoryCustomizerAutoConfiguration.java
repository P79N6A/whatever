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

package org.springframework.boot.autoconfigure.web.embedded;

import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for embedded servlet and reactive
 * web servers customizations.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@EnableConfigurationProperties(ServerProperties.class) // 导入ServerProperties配置类，读取上下文的server为开头的属性
public class EmbeddedWebServerFactoryCustomizerAutoConfiguration {

    /**
     * Nested configuration if Tomcat is being used.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({Tomcat.class, UpgradeProtocol.class})
    public static class TomcatWebServerFactoryCustomizerConfiguration {

        /**
         * 从ServerProperties配置Tomcat：BaseDir，MaxThreads，UriEncoding，ConnectionTimeout，MaxConnections等等
         */
        @Bean
        public TomcatWebServerFactoryCustomizer tomcatWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
            return new TomcatWebServerFactoryCustomizer(environment, serverProperties);
        }

    }
    /**
     * Nested configuration if Jetty is being used.
     */
    /**
     * Nested configuration if Undertow is being used.
     */
    /**
     * Nested configuration if Netty is being used.
     */

}
