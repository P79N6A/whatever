package org.springframework.aop.aspectj;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.weaver.patterns.NamePattern;
import org.aspectj.weaver.reflect.ReflectionWorld.ReflectionWorldException;
import org.aspectj.weaver.reflect.ShadowMatchImpl;
import org.aspectj.weaver.tools.*;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.autoproxy.ProxyCreationContext;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AbstractExpressionPointcut;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("serial")
public class AspectJExpressionPointcut extends AbstractExpressionPointcut implements ClassFilter, IntroductionAwareMethodMatcher, BeanFactoryAware {

    private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = new HashSet<>();

    static {
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.EXECUTION);
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.ARGS);
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.REFERENCE);
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.THIS);
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.TARGET);
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.WITHIN);
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_ANNOTATION);
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_WITHIN);
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_ARGS);
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_TARGET);
    }

    private static final Log logger = LogFactory.getLog(AspectJExpressionPointcut.class);

    @Nullable
    private Class<?> pointcutDeclarationScope;

    private String[] pointcutParameterNames = new String[0];

    private Class<?>[] pointcutParameterTypes = new Class<?>[0];

    @Nullable
    private BeanFactory beanFactory;

    @Nullable
    private transient ClassLoader pointcutClassLoader;

    @Nullable
    private transient PointcutExpression pointcutExpression;

    private transient Map<Method, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(32);

    public AspectJExpressionPointcut() {
    }

    public AspectJExpressionPointcut(Class<?> declarationScope, String[] paramNames, Class<?>[] paramTypes) {
        this.pointcutDeclarationScope = declarationScope;
        if (paramNames.length != paramTypes.length) {
            throw new IllegalStateException("Number of pointcut parameter names must match number of pointcut parameter types");
        }
        this.pointcutParameterNames = paramNames;
        this.pointcutParameterTypes = paramTypes;
    }

    public void setPointcutDeclarationScope(Class<?> pointcutDeclarationScope) {
        this.pointcutDeclarationScope = pointcutDeclarationScope;
    }

    public void setParameterNames(String... names) {
        this.pointcutParameterNames = names;
    }

    public void setParameterTypes(Class<?>... types) {
        this.pointcutParameterTypes = types;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public ClassFilter getClassFilter() {
        obtainPointcutExpression();
        return this;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        obtainPointcutExpression();
        return this;
    }

    private PointcutExpression obtainPointcutExpression() {
        if (getExpression() == null) {
            throw new IllegalStateException("Must set property 'expression' before attempting to match");
        }
        if (this.pointcutExpression == null) {
            this.pointcutClassLoader = determinePointcutClassLoader();
            this.pointcutExpression = buildPointcutExpression(this.pointcutClassLoader);
        }
        return this.pointcutExpression;
    }

    @Nullable
    private ClassLoader determinePointcutClassLoader() {
        if (this.beanFactory instanceof ConfigurableBeanFactory) {
            return ((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader();
        }
        if (this.pointcutDeclarationScope != null) {
            return this.pointcutDeclarationScope.getClassLoader();
        }
        return ClassUtils.getDefaultClassLoader();
    }

    private PointcutExpression buildPointcutExpression(@Nullable ClassLoader classLoader) {
        PointcutParser parser = initializePointcutParser(classLoader);
        PointcutParameter[] pointcutParameters = new PointcutParameter[this.pointcutParameterNames.length];
        for (int i = 0; i < pointcutParameters.length; i++) {
            pointcutParameters[i] = parser.createPointcutParameter(this.pointcutParameterNames[i], this.pointcutParameterTypes[i]);
        }
        return parser.parsePointcutExpression(replaceBooleanOperators(resolveExpression()), this.pointcutDeclarationScope, pointcutParameters);
    }

    private String resolveExpression() {
        String expression = getExpression();
        Assert.state(expression != null, "No expression set");
        return expression;
    }

    private PointcutParser initializePointcutParser(@Nullable ClassLoader classLoader) {
        PointcutParser parser = PointcutParser.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(SUPPORTED_PRIMITIVES, classLoader);
        parser.registerPointcutDesignatorHandler(new BeanPointcutDesignatorHandler());
        return parser;
    }

    private String replaceBooleanOperators(String pcExpr) {
        String result = StringUtils.replace(pcExpr, " and ", " && ");
        result = StringUtils.replace(result, " or ", " || ");
        result = StringUtils.replace(result, " not ", " ! ");
        return result;
    }

    public PointcutExpression getPointcutExpression() {
        return obtainPointcutExpression();
    }

    @Override
    public boolean matches(Class<?> targetClass) {
        PointcutExpression pointcutExpression = obtainPointcutExpression();
        try {
            try {
                return pointcutExpression.couldMatchJoinPointsInType(targetClass);
            } catch (ReflectionWorldException ex) {
                logger.debug("PointcutExpression matching rejected target class - trying fallback expression", ex);
                PointcutExpression fallbackExpression = getFallbackPointcutExpression(targetClass);
                if (fallbackExpression != null) {
                    return fallbackExpression.couldMatchJoinPointsInType(targetClass);
                }
            }
        } catch (Throwable ex) {
            logger.debug("PointcutExpression matching rejected target class", ex);
        }
        return false;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
        obtainPointcutExpression();
        ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);
        if (shadowMatch.alwaysMatches()) {
            return true;
        } else if (shadowMatch.neverMatches()) {
            return false;
        } else {
            if (hasIntroductions) {
                return true;
            }
            RuntimeTestWalker walker = getRuntimeTestWalker(shadowMatch);
            return (!walker.testsSubtypeSensitiveVars() || walker.testTargetInstanceOfResidue(targetClass));
        }
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return matches(method, targetClass, false);
    }

    @Override
    public boolean isRuntime() {
        return obtainPointcutExpression().mayNeedDynamicTest();
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass, Object... args) {
        obtainPointcutExpression();
        ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);
        ProxyMethodInvocation pmi = null;
        Object targetObject = null;
        Object thisObject = null;
        try {
            MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
            targetObject = mi.getThis();
            if (!(mi instanceof ProxyMethodInvocation)) {
                throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
            }
            pmi = (ProxyMethodInvocation) mi;
            thisObject = pmi.getProxy();
        } catch (IllegalStateException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not access current invocation - matching with limited context: " + ex);
            }
        }
        try {
            JoinPointMatch joinPointMatch = shadowMatch.matchesJoinPoint(thisObject, targetObject, args);
            if (pmi != null && thisObject != null) {
                RuntimeTestWalker originalMethodResidueTest = getRuntimeTestWalker(getShadowMatch(method, method));
                if (!originalMethodResidueTest.testThisInstanceOfResidue(thisObject.getClass())) {
                    return false;
                }
                if (joinPointMatch.matches()) {
                    bindParameters(pmi, joinPointMatch);
                }
            }
            return joinPointMatch.matches();
        } catch (Throwable ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to evaluate join point for arguments " + Arrays.asList(args) + " - falling back to non-match", ex);
            }
            return false;
        }
    }

    @Nullable
    protected String getCurrentProxiedBeanName() {
        return ProxyCreationContext.getCurrentProxiedBeanName();
    }

    @Nullable
    private PointcutExpression getFallbackPointcutExpression(Class<?> targetClass) {
        try {
            ClassLoader classLoader = targetClass.getClassLoader();
            if (classLoader != null && classLoader != this.pointcutClassLoader) {
                return buildPointcutExpression(classLoader);
            }
        } catch (Throwable ex) {
            logger.debug("Failed to create fallback PointcutExpression", ex);
        }
        return null;
    }

    private RuntimeTestWalker getRuntimeTestWalker(ShadowMatch shadowMatch) {
        if (shadowMatch instanceof DefensiveShadowMatch) {
            return new RuntimeTestWalker(((DefensiveShadowMatch) shadowMatch).primary);
        }
        return new RuntimeTestWalker(shadowMatch);
    }

    private void bindParameters(ProxyMethodInvocation invocation, JoinPointMatch jpm) {
        invocation.setUserAttribute(resolveExpression(), jpm);
    }

    private ShadowMatch getTargetShadowMatch(Method method, Class<?> targetClass) {
        Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        if (targetMethod.getDeclaringClass().isInterface()) {
            Set<Class<?>> ifcs = ClassUtils.getAllInterfacesForClassAsSet(targetClass);
            if (ifcs.size() > 1) {
                try {
                    Class<?> compositeInterface = ClassUtils.createCompositeInterface(ClassUtils.toClassArray(ifcs), targetClass.getClassLoader());
                    targetMethod = ClassUtils.getMostSpecificMethod(targetMethod, compositeInterface);
                } catch (IllegalArgumentException ex) {
                }
            }
        }
        return getShadowMatch(targetMethod, method);
    }

    private ShadowMatch getShadowMatch(Method targetMethod, Method originalMethod) {
        ShadowMatch shadowMatch = this.shadowMatchCache.get(targetMethod);
        if (shadowMatch == null) {
            synchronized (this.shadowMatchCache) {
                PointcutExpression fallbackExpression = null;
                shadowMatch = this.shadowMatchCache.get(targetMethod);
                if (shadowMatch == null) {
                    Method methodToMatch = targetMethod;
                    try {
                        try {
                            shadowMatch = obtainPointcutExpression().matchesMethodExecution(methodToMatch);
                        } catch (ReflectionWorldException ex) {
                            try {
                                fallbackExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
                                if (fallbackExpression != null) {
                                    shadowMatch = fallbackExpression.matchesMethodExecution(methodToMatch);
                                }
                            } catch (ReflectionWorldException ex2) {
                                fallbackExpression = null;
                            }
                        }
                        if (targetMethod != originalMethod && (shadowMatch == null || (shadowMatch.neverMatches() && Proxy.isProxyClass(targetMethod.getDeclaringClass())))) {
                            methodToMatch = originalMethod;
                            try {
                                shadowMatch = obtainPointcutExpression().matchesMethodExecution(methodToMatch);
                            } catch (ReflectionWorldException ex) {
                                try {
                                    fallbackExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
                                    if (fallbackExpression != null) {
                                        shadowMatch = fallbackExpression.matchesMethodExecution(methodToMatch);
                                    }
                                } catch (ReflectionWorldException ex2) {
                                    fallbackExpression = null;
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        logger.debug("PointcutExpression matching rejected target method", ex);
                        fallbackExpression = null;
                    }
                    if (shadowMatch == null) {
                        shadowMatch = new ShadowMatchImpl(org.aspectj.util.FuzzyBoolean.NO, null, null, null);
                    } else if (shadowMatch.maybeMatches() && fallbackExpression != null) {
                        shadowMatch = new DefensiveShadowMatch(shadowMatch, fallbackExpression.matchesMethodExecution(methodToMatch));
                    }
                    this.shadowMatchCache.put(targetMethod, shadowMatch);
                }
            }
        }
        return shadowMatch;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AspectJExpressionPointcut)) {
            return false;
        }
        AspectJExpressionPointcut otherPc = (AspectJExpressionPointcut) other;
        return ObjectUtils.nullSafeEquals(this.getExpression(), otherPc.getExpression()) && ObjectUtils.nullSafeEquals(this.pointcutDeclarationScope, otherPc.pointcutDeclarationScope) && ObjectUtils.nullSafeEquals(this.pointcutParameterNames, otherPc.pointcutParameterNames) && ObjectUtils.nullSafeEquals(this.pointcutParameterTypes, otherPc.pointcutParameterTypes);
    }

    @Override
    public int hashCode() {
        int hashCode = ObjectUtils.nullSafeHashCode(this.getExpression());
        hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.pointcutDeclarationScope);
        hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.pointcutParameterNames);
        hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.pointcutParameterTypes);
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AspectJExpressionPointcut: ");
        sb.append("(");
        for (int i = 0; i < this.pointcutParameterTypes.length; i++) {
            sb.append(this.pointcutParameterTypes[i].getName());
            sb.append(" ");
            sb.append(this.pointcutParameterNames[i]);
            if ((i + 1) < this.pointcutParameterTypes.length) {
                sb.append(", ");
            }
        }
        sb.append(")");
        sb.append(" ");
        if (getExpression() != null) {
            sb.append(getExpression());
        } else {
            sb.append("<pointcut expression not set>");
        }
        return sb.toString();
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.shadowMatchCache = new ConcurrentHashMap<>(32);
    }

    private class BeanPointcutDesignatorHandler implements PointcutDesignatorHandler {

        private static final String BEAN_DESIGNATOR_NAME = "bean";

        @Override
        public String getDesignatorName() {
            return BEAN_DESIGNATOR_NAME;
        }

        @Override
        public ContextBasedMatcher parse(String expression) {
            return new BeanContextMatcher(expression);
        }

    }

    private class BeanContextMatcher implements ContextBasedMatcher {

        private final NamePattern expressionPattern;

        public BeanContextMatcher(String expression) {
            this.expressionPattern = new NamePattern(expression);
        }

        @Override
        @SuppressWarnings("rawtypes")
        @Deprecated
        public boolean couldMatchJoinPointsInType(Class someClass) {
            return (contextMatch(someClass) == FuzzyBoolean.YES);
        }

        @Override
        @SuppressWarnings("rawtypes")
        @Deprecated
        public boolean couldMatchJoinPointsInType(Class someClass, MatchingContext context) {
            return (contextMatch(someClass) == FuzzyBoolean.YES);
        }

        @Override
        public boolean matchesDynamically(MatchingContext context) {
            return true;
        }

        @Override
        public FuzzyBoolean matchesStatically(MatchingContext context) {
            return contextMatch(null);
        }

        @Override
        public boolean mayNeedDynamicTest() {
            return false;
        }

        private FuzzyBoolean contextMatch(@Nullable Class<?> targetType) {
            String advisedBeanName = getCurrentProxiedBeanName();
            if (advisedBeanName == null) {
                return FuzzyBoolean.MAYBE;
            }
            if (BeanFactoryUtils.isGeneratedBeanName(advisedBeanName)) {
                return FuzzyBoolean.NO;
            }
            if (targetType != null) {
                boolean isFactory = FactoryBean.class.isAssignableFrom(targetType);
                return FuzzyBoolean.fromBoolean(matchesBean(isFactory ? BeanFactory.FACTORY_BEAN_PREFIX + advisedBeanName : advisedBeanName));
            } else {
                return FuzzyBoolean.fromBoolean(matchesBean(advisedBeanName) || matchesBean(BeanFactory.FACTORY_BEAN_PREFIX + advisedBeanName));
            }
        }

        private boolean matchesBean(String advisedBeanName) {
            return BeanFactoryAnnotationUtils.isQualifierMatch(this.expressionPattern::matches, advisedBeanName, beanFactory);
        }

    }

    private static class DefensiveShadowMatch implements ShadowMatch {

        private final ShadowMatch primary;

        private final ShadowMatch other;

        public DefensiveShadowMatch(ShadowMatch primary, ShadowMatch other) {
            this.primary = primary;
            this.other = other;
        }

        @Override
        public boolean alwaysMatches() {
            return this.primary.alwaysMatches();
        }

        @Override
        public boolean maybeMatches() {
            return this.primary.maybeMatches();
        }

        @Override
        public boolean neverMatches() {
            return this.primary.neverMatches();
        }

        @Override
        public JoinPointMatch matchesJoinPoint(Object thisObject, Object targetObject, Object[] args) {
            try {
                return this.primary.matchesJoinPoint(thisObject, targetObject, args);
            } catch (ReflectionWorldException ex) {
                return this.other.matchesJoinPoint(thisObject, targetObject, args);
            }
        }

        @Override
        public void setMatchingContext(MatchingContext aMatchContext) {
            this.primary.setMatchingContext(aMatchContext);
            this.other.setMatchingContext(aMatchContext);
        }

    }

}
