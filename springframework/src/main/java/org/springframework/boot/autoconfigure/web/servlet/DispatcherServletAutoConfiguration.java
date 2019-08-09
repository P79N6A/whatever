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

package org.springframework.boot.autoconfigure.web.servlet;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.http.HttpProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import java.util.Arrays;
import java.util.List;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Spring
 * {@link DispatcherServlet}. Should work for a standalone application where an embedded
 * web server is already present and also for a deployable application using
 * {@link SpringBootServletInitializer}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Brian Clozel
 */
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@AutoConfigureAfter(ServletWebServerFactoryAutoConfiguration.class) // Tomcat之后
public class DispatcherServletAutoConfiguration {

    /*
     * The bean name for a DispatcherServlet that will be mapped to the root URL "/"
     */
    public static final String DEFAULT_DISPATCHER_SERVLET_BEAN_NAME = "dispatcherServlet";

    /*
     * The bean name for a ServletRegistrationBean for the DispatcherServlet "/"
     */
    public static final String DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME = "dispatcherServletRegistration";

    @Configuration(proxyBeanMethods = false) // 配置类
    @Conditional(DefaultDispatcherServletCondition.class)
    // 通过DefaultDispatcherServletCondition判断是否加载：容器不存在这个默认的DispatcherServlet
    @ConditionalOnClass(ServletRegistration.class) // ServletRegistration.class存在才加载
    @EnableConfigurationProperties({ // 把HttpProperties和WebMvcProperties导入容器
            HttpProperties.class, // spring.http
            WebMvcProperties.class // spring.mvc
    })
    // 静态内部类
    protected static class DispatcherServletConfiguration {

        /**
         * 注册DispatcherServlet
         */
        @Bean(name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
        public DispatcherServlet dispatcherServlet(HttpProperties httpProperties, WebMvcProperties webMvcProperties) {
            // 初始化DispatcherServlet
            DispatcherServlet dispatcherServlet = new DispatcherServlet();
            // 分发Option请求
            dispatcherServlet.setDispatchOptionsRequest(webMvcProperties.isDispatchOptionsRequest());
            dispatcherServlet.setDispatchTraceRequest(webMvcProperties.isDispatchTraceRequest());
            dispatcherServlet.setThrowExceptionIfNoHandlerFound(webMvcProperties.isThrowExceptionIfNoHandlerFound());
            dispatcherServlet.setEnableLoggingRequestDetails(httpProperties.isLogRequestDetails());
            return dispatcherServlet;
        }

        /**
         * 文件上传
         */
        @Bean
        @ConditionalOnBean(MultipartResolver.class)
        @ConditionalOnMissingBean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
        public MultipartResolver multipartResolver(MultipartResolver resolver) {
            // Detect if the user has created a MultipartResolver but named it incorrectly
            return resolver;
        }

    }

    @Configuration(proxyBeanMethods = false)
    @Conditional(DispatcherServletRegistrationCondition.class) // 条件判断
    @ConditionalOnClass(ServletRegistration.class)
    @EnableConfigurationProperties(WebMvcProperties.class) // spring.mvc
    @Import(DispatcherServletConfiguration.class) // 导入上面的静态内部类DispatcherServletConfiguration
    // 静态内部类
    protected static class DispatcherServletRegistrationConfiguration {

        @Bean(name = DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
        @ConditionalOnBean(value = DispatcherServlet.class, name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
        // 容器中存在该Bean实例才行
        public DispatcherServletRegistrationBean dispatcherServletRegistration(DispatcherServlet dispatcherServlet, WebMvcProperties webMvcProperties, ObjectProvider<MultipartConfigElement> multipartConfig) {
            // ServletRegistrationBean添加DispatcherServlet到ServletContext，注册为Servlet
            // 实现了ServletContextInitializer，内置的Tomcat容器启动时会被实例化并调用onStartup方法
            DispatcherServletRegistrationBean registration = new DispatcherServletRegistrationBean(dispatcherServlet, webMvcProperties.getServlet().getPath());
            registration.setName(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
            // Servlet
            registration.setLoadOnStartup(webMvcProperties.getServlet().getLoadOnStartup());
            multipartConfig.ifAvailable(registration::setMultipartConfig);
            return registration;
        }

    }

    /**
     * 条件
     */
    @Order(Ordered.LOWEST_PRECEDENCE - 10)
    private static class DefaultDispatcherServletCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConditionMessage.Builder message = ConditionMessage.forCondition("Default DispatcherServlet");
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            // 容器存在DispatcherServlet类型的Bean
            List<String> dispatchServletBeans = Arrays.asList(beanFactory.getBeanNamesForType(DispatcherServlet.class, false, false));
            // 名称一样
            if (dispatchServletBeans.contains(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
                // 不匹配
                return ConditionOutcome.noMatch(message.found("dispatcher servlet bean").items(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME));
            }
            // 类型不对，名字一样
            if (beanFactory.containsBean(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
                // 不匹配
                return ConditionOutcome.noMatch(message.found("non dispatcher servlet bean").items(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME));
            }
            // 不存在DispatcherServlet类型的Bean
            if (dispatchServletBeans.isEmpty()) {
                // 匹配
                return ConditionOutcome.match(message.didNotFind("dispatcher servlet beans").atAll());
            }
            // 匹配
            return ConditionOutcome.match(message.found("dispatcher servlet bean", "dispatcher servlet beans").items(Style.QUOTE, dispatchServletBeans).append("and none is named " + DEFAULT_DISPATCHER_SERVLET_BEAN_NAME));
        }

    }

    /**
     * 条件
     */
    @Order(Ordered.LOWEST_PRECEDENCE - 10)
    private static class DispatcherServletRegistrationCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            ConditionOutcome outcome = checkDefaultDispatcherName(beanFactory);
            // 不匹配直接返回
            if (!outcome.isMatch()) {
                return outcome;
            }
            // 进一步查询DispatcherServletRegistration是否存在
            return checkServletRegistration(beanFactory);
        }

        private ConditionOutcome checkDefaultDispatcherName(ConfigurableListableBeanFactory beanFactory) {
            // 容器存在DispatcherServlet类型的Bean
            List<String> servlets = Arrays.asList(beanFactory.getBeanNamesForType(DispatcherServlet.class, false, false));
            // 存在该名字的Bean
            boolean containsDispatcherBean = beanFactory.containsBean(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
            // 存在该名字的Bean && 容器不存在DispatcherServlet类型的Bean
            if (containsDispatcherBean && !servlets.contains(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
                // 不匹配
                return ConditionOutcome.noMatch(startMessage().found("non dispatcher servlet").items(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME));
            }
            // 匹配
            return ConditionOutcome.match();
        }

        private ConditionOutcome checkServletRegistration(ConfigurableListableBeanFactory beanFactory) {
            ConditionMessage.Builder message = startMessage();
            // 容器存在ServletRegistrationBean类型的Bean
            List<String> registrations = Arrays.asList(beanFactory.getBeanNamesForType(ServletRegistrationBean.class, false, false));
            // 存在该名字的Bean
            boolean containsDispatcherRegistrationBean = beanFactory.containsBean(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME);
            // 不存在对应类型
            if (registrations.isEmpty()) {
                // 名字存在
                if (containsDispatcherRegistrationBean) {
                    // 不匹配
                    return ConditionOutcome.noMatch(message.found("non servlet registration bean").items(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME));
                }
                // 名字不存在，匹配
                return ConditionOutcome.match(message.didNotFind("servlet registration bean").atAll());
            }
            // 类型有，名字有
            if (registrations.contains(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)) {
                // 不匹配
                return ConditionOutcome.noMatch(message.found("servlet registration bean").items(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME));
            }
            //
            if (containsDispatcherRegistrationBean) {
                // 不匹配
                return ConditionOutcome.noMatch(message.found("non servlet registration bean").items(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME));
            }
            // 匹配
            return ConditionOutcome.match(message.found("servlet registration beans").items(Style.QUOTE, registrations).append("and none is named " + DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME));
        }

        private ConditionMessage.Builder startMessage() {
            return ConditionMessage.forCondition("DispatcherServlet Registration");
        }

    }

}
