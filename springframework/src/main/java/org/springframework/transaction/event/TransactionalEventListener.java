package org.springframework.transaction.event;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EventListener
public @interface TransactionalEventListener {

    TransactionPhase phase() default TransactionPhase.AFTER_COMMIT;

    boolean fallbackExecution() default false;

    @AliasFor(annotation = EventListener.class, attribute = "classes") Class<?>[] value() default {};

    @AliasFor(annotation = EventListener.class, attribute = "classes") Class<?>[] classes() default {};

    String condition() default "";

}
