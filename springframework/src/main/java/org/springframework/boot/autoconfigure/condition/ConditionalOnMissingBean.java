package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD}) // 类和方法
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class) // @Conditional，条件类是OnBeanCondition
public @interface ConditionalOnMissingBean {

    /**
     * 需要检查的Bean的Class类型
     * 当容器不包含每一个被指定的Class时条件匹配
     */
    Class<?>[] value() default {};

    /**
     * 需要检查的Bean的Class类型名称(全限定名)
     */
    String[] type() default {};

    /**
     * 识别匹配Bean时，可以被忽略的Bean的Class类型
     */
    Class<?>[] ignored() default {};

    /**
     * 识别匹配Bean时，可以被忽略的Bean的Class类型名称(全限定名)
     */
    String[] ignoredType() default {};

    /**
     * 装饰需要检查的Bean的注解
     * 当容器不包含带有这些注解的Bean时条件匹配
     */
    Class<? extends Annotation>[] annotation() default {};

    /**
     * 需要检查的Bean的name
     */
    String[] name() default {};

    /**
     * 搜索容器策略：当前，祖先，全部
     */
    SearchStrategy search() default SearchStrategy.ALL;

    Class<?>[] parameterizedContainer() default {};

}
