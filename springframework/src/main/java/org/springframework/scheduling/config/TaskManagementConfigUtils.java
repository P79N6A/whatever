package org.springframework.scheduling.config;

public abstract class TaskManagementConfigUtils {

    public static final String SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME = "org.springframework.context.annotation.internalScheduledAnnotationProcessor";

    public static final String ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME = "org.springframework.context.annotation.internalAsyncAnnotationProcessor";

    public static final String ASYNC_EXECUTION_ASPECT_BEAN_NAME = "org.springframework.scheduling.config.internalAsyncExecutionAspect";

}
