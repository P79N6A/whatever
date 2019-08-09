package org.springframework.web;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * ServletContainerInitializer接口用来给Web应用在启动时动态注册Servlet，Filter，Listener
 * SPI加载：接口实现必须要在被jar包声明，声明文件在META-INF/services目录下
 * 文件名javax.servlet.ServletContainerInitializer，内容org.springframework.web.SpringServletContainerInitializer
 */
@HandlesTypes(WebApplicationInitializer.class) // 应用启动的时候加载指定的类
public class SpringServletContainerInitializer implements ServletContainerInitializer {

    /**
     * 根据@HandlesTypes(WebApplicationInitializer.class)
     * 所有WebApplicationInitializer类都传入到onStartup方法的Set参数中
     */
    @Override
    public void onStartup(@Nullable Set<Class<?>> webAppInitializerClasses, ServletContext servletContext) throws ServletException {
        List<WebApplicationInitializer> initializers = new LinkedList<>();
        if (webAppInitializerClasses != null) {
            for (Class<?> waiClass : webAppInitializerClasses) {
                // Be defensive: Some servlet containers provide us with invalid classes,
                // no matter what @HandlesTypes says...
                if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) && WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
                    try {
                        /*
                         * 如果不使用内嵌的Tomcat，需要继承SpringBootServletInitializer，重写configure方法，然后这里会反射初始化
                         * 之后onStartup时候会创建并启动Spring容器
                         */
                        initializers.add((WebApplicationInitializer) ReflectionUtils.accessibleConstructor(waiClass).newInstance());
                    } catch (Throwable ex) {
                        throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
                    }
                }
            }
        }
        if (initializers.isEmpty()) {
            servletContext.log("No Spring WebApplicationInitializer types detected on classpath");
            return;
        }
        servletContext.log(initializers.size() + " Spring WebApplicationInitializers detected on classpath");
        AnnotationAwareOrderComparator.sort(initializers);
        for (WebApplicationInitializer initializer : initializers) {
            // onStartup
            initializer.onStartup(servletContext);
        }
    }

}
