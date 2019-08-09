package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.TypePatternClassFilter;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.ComposablePointcut;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

@SuppressWarnings("serial")
public class AspectMetadata implements Serializable {

    private final String aspectName;

    private final Class<?> aspectClass;

    private transient AjType<?> ajType;

    private final Pointcut perClausePointcut;

    public AspectMetadata(Class<?> aspectClass, String aspectName) {
        this.aspectName = aspectName;
        Class<?> currClass = aspectClass;
        AjType<?> ajType = null;
        while (currClass != Object.class) {
            AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
            if (ajTypeToCheck.isAspect()) {
                ajType = ajTypeToCheck;
                break;
            }
            // 递归父类
            currClass = currClass.getSuperclass();
        }
        if (ajType == null) {
            throw new IllegalArgumentException("Class '" + aspectClass.getName() + "' is not an @AspectJ aspect");
        }
        if (ajType.getDeclarePrecedence().length > 0) {
            throw new IllegalArgumentException("DeclarePrecendence not presently supported in Spring AOP");
        }
        this.aspectClass = ajType.getJavaClass();
        this.ajType = ajType;
        switch (this.ajType.getPerClause().getKind()) {
            // 一般都是这个
            case SINGLETON:
                this.perClausePointcut = Pointcut.TRUE;
                return;
            case PERTARGET:
            case PERTHIS:
                AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
                ajexp.setLocation(aspectClass.getName());
                ajexp.setExpression(findPerClause(aspectClass));
                ajexp.setPointcutDeclarationScope(aspectClass);
                this.perClausePointcut = ajexp;
                return;
            case PERTYPEWITHIN:
                this.perClausePointcut = new ComposablePointcut(new TypePatternClassFilter(findPerClause(aspectClass)));
                return;
            default:
                throw new AopConfigException("PerClause " + ajType.getPerClause().getKind() + " not supported by Spring AOP for " + aspectClass);
        }
    }

    private String findPerClause(Class<?> aspectClass) {
        String str = aspectClass.getAnnotation(Aspect.class).value();
        str = str.substring(str.indexOf('(') + 1);
        str = str.substring(0, str.length() - 1);
        return str;
    }

    public AjType<?> getAjType() {
        return this.ajType;
    }

    public Class<?> getAspectClass() {
        return this.aspectClass;
    }

    public String getAspectName() {
        return this.aspectName;
    }

    public Pointcut getPerClausePointcut() {
        return this.perClausePointcut;
    }

    public boolean isPerThisOrPerTarget() {
        PerClauseKind kind = getAjType().getPerClause().getKind();
        return (kind == PerClauseKind.PERTARGET || kind == PerClauseKind.PERTHIS);
    }

    public boolean isPerTypeWithin() {
        PerClauseKind kind = getAjType().getPerClause().getKind();
        return (kind == PerClauseKind.PERTYPEWITHIN);
    }

    public boolean isLazilyInstantiated() {
        return (isPerThisOrPerTarget() || isPerTypeWithin());
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        this.ajType = AjTypeSystem.getAjType(this.aspectClass);
    }

}
