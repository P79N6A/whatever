package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Set;

public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        boolean candidateFound = false;
        // 获取注解
        Set<String> annTypes = importingClassMetadata.getAnnotationTypes();
        for (String annType : annTypes) {
            AnnotationAttributes candidate = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
            if (candidate == null) {
                continue;
            }
            Object mode = candidate.get("mode");
            Object proxyTargetClass = candidate.get("proxyTargetClass");

            if (mode != null && proxyTargetClass != null && AdviceMode.class == mode.getClass() && Boolean.class == proxyTargetClass.getClass()) {
                candidateFound = true;
                if (mode == AdviceMode.PROXY) {
                    // 注册InfrastructureAdvisorAutoProxyCreator
                    AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
                    // proxyTargetClass == true
                    if ((Boolean) proxyTargetClass) {
                        AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
                        return;
                    }
                }
            }
        }
        if (!candidateFound && logger.isInfoEnabled()) {
            String name = getClass().getSimpleName();
            logger.info(String.format("%s was imported but no annotations were found " + "having both 'mode' and 'proxyTargetClass' attributes of type " + "AdviceMode and boolean respectively. This means that auto proxy " + "creator registration and configuration may not have occurred as " + "intended, and components may not be proxied as expected. Check to " + "ensure that %s has been @Import'ed on the same class where these " + "annotations are declared; otherwise remove the import of %s " + "altogether.", name, name, name));
        }
    }

}
