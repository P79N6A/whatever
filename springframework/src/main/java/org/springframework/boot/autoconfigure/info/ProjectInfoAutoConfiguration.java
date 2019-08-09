package org.springframework.boot.autoconfigure.info;

import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProjectInfoProperties.class)
public class ProjectInfoAutoConfiguration {

    private final ProjectInfoProperties properties;

    public ProjectInfoAutoConfiguration(ProjectInfoProperties properties) {
        this.properties = properties;
    }

    @Conditional(GitResourceAvailableCondition.class)
    @ConditionalOnMissingBean
    @Bean
    public GitProperties gitProperties() throws Exception {
        return new GitProperties(loadFrom(this.properties.getGit().getLocation(), "git", this.properties.getGit().getEncoding()));
    }

    @ConditionalOnResource(resources = "${spring.info.build.location:classpath:META-INF/build-info.properties}")
    @ConditionalOnMissingBean
    @Bean
    public BuildProperties buildProperties() throws Exception {
        return new BuildProperties(loadFrom(this.properties.getBuild().getLocation(), "build", this.properties.getBuild().getEncoding()));
    }

    protected Properties loadFrom(Resource location, String prefix, Charset encoding) throws IOException {
        prefix = prefix.endsWith(".") ? prefix : prefix + ".";
        Properties source = loadSource(location, encoding);
        Properties target = new Properties();
        for (String key : source.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                target.put(key.substring(prefix.length()), source.get(key));
            }
        }
        return target;
    }

    private Properties loadSource(Resource location, Charset encoding) throws IOException {
        if (encoding != null) {
            return PropertiesLoaderUtils.loadProperties(new EncodedResource(location, encoding));
        }
        return PropertiesLoaderUtils.loadProperties(location);
    }

    static class GitResourceAvailableCondition extends SpringBootCondition {

        private final ResourceLoader defaultResourceLoader = new DefaultResourceLoader();

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ResourceLoader loader = context.getResourceLoader();
            loader = (loader != null) ? loader : this.defaultResourceLoader;
            Environment environment = context.getEnvironment();
            String location = environment.getProperty("spring.info.git.location");
            if (location == null) {
                location = "classpath:git.properties";
            }
            ConditionMessage.Builder message = ConditionMessage.forCondition("GitResource");
            if (loader.getResource(location).exists()) {
                return ConditionOutcome.match(message.found("git info at").items(location));
            }
            return ConditionOutcome.noMatch(message.didNotFind("git info at").items(location));
        }

    }

}
