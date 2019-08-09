package org.springframework.aop.aspectj.annotation;

import org.aopalliance.aop.Advice;
import org.aspectj.lang.annotation.*;
import org.springframework.aop.Advisor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.*;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConvertingComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.comparator.InstanceComparator;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("serial")
public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory implements Serializable {

    private static final Comparator<Method> METHOD_COMPARATOR;

    static {
        Comparator<Method> adviceKindComparator = new ConvertingComparator<>(new InstanceComparator<>(Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class), (Converter<Method, Annotation>) method -> {
            AspectJAnnotation<?> annotation = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
            return (annotation != null ? annotation.getAnnotation() : null);
        });
        Comparator<Method> methodNameComparator = new ConvertingComparator<>(Method::getName);
        METHOD_COMPARATOR = adviceKindComparator.thenComparing(methodNameComparator);
    }

    @Nullable
    private final BeanFactory beanFactory;

    public ReflectiveAspectJAdvisorFactory() {
        this(null);
    }

    public ReflectiveAspectJAdvisorFactory(@Nullable BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
        // 被@AspectJ注解的顶级类
        Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
        // 被@AspectJ注解的类名
        String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
        // 验证
        validate(aspectClass);
        MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory = new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);
        List<Advisor> advisors = new ArrayList<>();
        // 用户声明的没有被被@Pointcut注解的方法
        for (Method method : getAdvisorMethods(aspectClass)) {
            // 转为Advisor，这里advisors.size()会给Advisor一个顺序
            Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, advisors.size(), aspectName);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }
        if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
            Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
            advisors.add(0, instantiationAdvisor);
        }
        // 遍历域变量
        for (Field field : aspectClass.getDeclaredFields()) {
            // 如果有@DeclareParents注解就转为Advisor
            Advisor advisor = getDeclareParentsAdvisor(field);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }
        return advisors;
    }

    private List<Method> getAdvisorMethods(Class<?> aspectClass) {
        final List<Method> methods = new ArrayList<>();
        ReflectionUtils.doWithMethods(aspectClass, method -> {
            if (AnnotationUtils.getAnnotation(method, Pointcut.class) == null) {
                methods.add(method);
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);
        methods.sort(METHOD_COMPARATOR);
        return methods;
    }

    @Nullable
    private Advisor getDeclareParentsAdvisor(Field introductionField) {
        DeclareParents declareParents = introductionField.getAnnotation(DeclareParents.class);
        if (declareParents == null) {
            return null;
        }
        if (DeclareParents.class == declareParents.defaultImpl()) {
            throw new IllegalStateException("'defaultImpl' attribute must be set on DeclareParents");
        }
        return new DeclareParentsAdvisor(introductionField.getType(), declareParents.value(), declareParents.defaultImpl());
    }

    @Override
    @Nullable
    public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrderInAspect, String aspectName) {
        validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());
        // 获得Pointcut表达式，包含了方法上面的@Before，@Around之类的注解
        AspectJExpressionPointcut expressionPointcut = getPointcut(candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
        if (expressionPointcut == null) {
            return null;
        }
        // 返回Advisor封装，实例化过程会实例化通知Advice
        return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod, this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
    }

    @Nullable
    private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
        // 返回方法上面的@Before，@Around之类的注解
        AspectJAnnotation<?> aspectJAnnotation = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
        if (aspectJAnnotation == null) {
            return null;
        }
        AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
        ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
        if (this.beanFactory != null) {
            ajexp.setBeanFactory(this.beanFactory);
        }
        return ajexp;
    }

    /**
     * 实例化Advice
     */
    @Override
    @Nullable
    public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut, MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {
        // 被@AspectJ注解的顶级类
        Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
        // 验证
        validate(candidateAspectClass);
        // 返回方法上面的@Before，@Around之类的注解
        AspectJAnnotation<?> aspectJAnnotation = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
        // 没有就返回
        if (aspectJAnnotation == null) {
            return null;
        }
        // 类必须被@AspectJ注解
        if (!isAspect(candidateAspectClass)) {
            throw new AopConfigException("Advice must be declared inside an aspect type: " + "Offending method '" + candidateAdviceMethod + "' in class [" + candidateAspectClass.getName() + "]");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Found AspectJ method: " + candidateAdviceMethod);
        }
        AbstractAspectJAdvice springAdvice;
        // switch
        switch (aspectJAnnotation.getAnnotationType()) {
            case AtPointcut:
                if (logger.isDebugEnabled()) {
                    logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
                }
                return null;
            case AtAround:
                springAdvice = new AspectJAroundAdvice(candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                break;
            case AtBefore:
                springAdvice = new AspectJMethodBeforeAdvice(candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                break;
            case AtAfter:
                springAdvice = new AspectJAfterAdvice(candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                break;
            case AtAfterReturning:
                springAdvice = new AspectJAfterReturningAdvice(candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
                if (StringUtils.hasText(afterReturningAnnotation.returning())) {
                    springAdvice.setReturningName(afterReturningAnnotation.returning());
                }
                break;
            case AtAfterThrowing:
                springAdvice = new AspectJAfterThrowingAdvice(candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
                if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
                    springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported advice type on method: " + candidateAdviceMethod);
        }
        springAdvice.setAspectName(aspectName);
        // 设置顺序
        springAdvice.setDeclarationOrder(declarationOrder);
        String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
        if (argNames != null) {
            springAdvice.setArgumentNamesFromStringArray(argNames);
        }

        // 绑定参数
        springAdvice.calculateArgumentBindings();
        return springAdvice;
    }

    @SuppressWarnings("serial")
    protected static class SyntheticInstantiationAdvisor extends DefaultPointcutAdvisor {

        public SyntheticInstantiationAdvisor(final MetadataAwareAspectInstanceFactory aif) {
            super(aif.getAspectMetadata().getPerClausePointcut(), (MethodBeforeAdvice) (method, args, target) -> aif.getAspectInstance());
        }

    }

}
