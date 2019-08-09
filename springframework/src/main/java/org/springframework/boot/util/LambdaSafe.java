package org.springframework.boot.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class LambdaSafe {

    private static final Method CLASS_GET_MODULE;

    private static final Method MODULE_GET_NAME;

    static {
        CLASS_GET_MODULE = ReflectionUtils.findMethod(Class.class, "getModule");
        MODULE_GET_NAME = (CLASS_GET_MODULE != null) ? ReflectionUtils.findMethod(CLASS_GET_MODULE.getReturnType(), "getName") : null;
    }

    private LambdaSafe() {
    }

    public static <C, A> Callback<C, A> callback(Class<C> callbackType, C callbackInstance, A argument, Object... additionalArguments) {
        Assert.notNull(callbackType, "CallbackType must not be null");
        Assert.notNull(callbackInstance, "CallbackInstance must not be null");
        return new Callback<>(callbackType, callbackInstance, argument, additionalArguments);
    }

    public static <C, A> Callbacks<C, A> callbacks(Class<C> callbackType, Collection<? extends C> callbackInstances, A argument, Object... additionalArguments) {
        Assert.notNull(callbackType, "CallbackType must not be null");
        Assert.notNull(callbackInstances, "CallbackInstances must not be null");
        return new Callbacks<>(callbackType, callbackInstances, argument, additionalArguments);
    }

    private abstract static class LambdaSafeCallback<C, A, SELF extends LambdaSafeCallback<C, A, SELF>> {

        private final Class<C> callbackType;

        private final A argument;

        private final Object[] additionalArguments;

        private Log logger;

        private Filter<C, A> filter = new GenericTypeFilter<>();

        protected LambdaSafeCallback(Class<C> callbackType, A argument, Object[] additionalArguments) {
            this.callbackType = callbackType;
            this.argument = argument;
            this.additionalArguments = additionalArguments;
            this.logger = LogFactory.getLog(callbackType);
        }

        public SELF withLogger(Class<?> loggerSource) {
            return withLogger(LogFactory.getLog(loggerSource));
        }

        public SELF withLogger(Log logger) {
            Assert.notNull(logger, "Logger must not be null");
            this.logger = logger;
            return self();
        }

        public SELF withFilter(Filter<C, A> filter) {
            Assert.notNull(filter, "Filter must not be null");
            this.filter = filter;
            return self();
        }

        protected final <R> InvocationResult<R> invoke(C callbackInstance, Supplier<R> supplier) {
            if (this.filter.match(this.callbackType, callbackInstance, this.argument, this.additionalArguments)) {
                try {
                    return InvocationResult.of(supplier.get());
                } catch (ClassCastException ex) {
                    if (!isLambdaGenericProblem(ex)) {
                        throw ex;
                    }
                    logNonMatchingType(callbackInstance, ex);
                }
            }
            return InvocationResult.noResult();
        }

        private boolean isLambdaGenericProblem(ClassCastException ex) {
            return (ex.getMessage() == null || startsWithArgumentClassName(ex.getMessage()));
        }

        private boolean startsWithArgumentClassName(String message) {
            Predicate<Object> startsWith = (argument) -> startsWithArgumentClassName(message, argument);
            return startsWith.test(this.argument) || Stream.of(this.additionalArguments).anyMatch(startsWith);
        }

        private boolean startsWithArgumentClassName(String message, Object argument) {
            if (argument == null) {
                return false;
            }
            Class<?> argumentType = argument.getClass();
            // On Java 8, the message starts with the class name: "java.lang.String cannot
            // be cast..."
            if (message.startsWith(argumentType.getName())) {
                return true;
            }
            // On Java 11, the message starts with "class ..." a.k.a. Class.toString()
            if (message.startsWith(argumentType.toString())) {
                return true;
            }
            // On Java 9, the message used to contain the module name:
            // "java.base/java.lang.String cannot be cast..."
            int moduleSeparatorIndex = message.indexOf('/');
            if (moduleSeparatorIndex != -1 && message.startsWith(argumentType.getName(), moduleSeparatorIndex + 1)) {
                return true;
            }
            if (CLASS_GET_MODULE != null) {
                Object module = ReflectionUtils.invokeMethod(CLASS_GET_MODULE, argumentType);
                Object moduleName = ReflectionUtils.invokeMethod(MODULE_GET_NAME, module);
                return message.startsWith(moduleName + "/" + argumentType.getName());
            }
            return false;
        }

        private void logNonMatchingType(C callback, ClassCastException ex) {
            if (this.logger.isDebugEnabled()) {
                Class<?> expectedType = ResolvableType.forClass(this.callbackType).resolveGeneric();
                String expectedTypeName = (expectedType != null) ? ClassUtils.getShortName(expectedType) + " type" : "type";
                String message = "Non-matching " + expectedTypeName + " for callback " + ClassUtils.getShortName(this.callbackType) + ": " + callback;
                this.logger.debug(message, ex);
            }
        }

        @SuppressWarnings("unchecked")
        private SELF self() {
            return (SELF) this;
        }

    }

    public static final class Callback<C, A> extends LambdaSafeCallback<C, A, Callback<C, A>> {

        private final C callbackInstance;

        private Callback(Class<C> callbackType, C callbackInstance, A argument, Object[] additionalArguments) {
            super(callbackType, argument, additionalArguments);
            this.callbackInstance = callbackInstance;
        }

        public void invoke(Consumer<C> invoker) {
            invoke(this.callbackInstance, () -> {
                invoker.accept(this.callbackInstance);
                return null;
            });
        }

        public <R> InvocationResult<R> invokeAnd(Function<C, R> invoker) {
            return invoke(this.callbackInstance, () -> invoker.apply(this.callbackInstance));
        }

    }

    public static final class Callbacks<C, A> extends LambdaSafeCallback<C, A, Callbacks<C, A>> {

        private final Collection<? extends C> callbackInstances;

        private Callbacks(Class<C> callbackType, Collection<? extends C> callbackInstances, A argument, Object[] additionalArguments) {
            super(callbackType, argument, additionalArguments);
            this.callbackInstances = callbackInstances;
        }

        public void invoke(Consumer<C> invoker) {
            this.callbackInstances.forEach((callbackInstance) -> {
                invoke(callbackInstance, () -> {
                    invoker.accept(callbackInstance);
                    return null;
                });
            });
        }

        public <R> Stream<R> invokeAnd(Function<C, R> invoker) {
            Function<C, InvocationResult<R>> mapper = (callbackInstance) -> invoke(callbackInstance, () -> invoker.apply(callbackInstance));
            return this.callbackInstances.stream().map(mapper).filter(InvocationResult::hasResult).map(InvocationResult::get);
        }

    }

    @FunctionalInterface
    interface Filter<C, A> {

        boolean match(Class<C> callbackType, C callbackInstance, A argument, Object[] additionalArguments);

        static <C, A> Filter<C, A> allowAll() {
            return (callbackType, callbackInstance, argument, additionalArguments) -> true;
        }

    }

    private static class GenericTypeFilter<C, A> implements Filter<C, A> {

        @Override
        public boolean match(Class<C> callbackType, C callbackInstance, A argument, Object[] additionalArguments) {
            ResolvableType type = ResolvableType.forClass(callbackType, callbackInstance.getClass());
            if (type.getGenerics().length == 1 && type.resolveGeneric() != null) {
                return type.resolveGeneric().isInstance(argument);
            }
            return true;
        }

    }

    public static final class InvocationResult<R> {

        private static final InvocationResult<?> NONE = new InvocationResult<>(null);

        private final R value;

        private InvocationResult(R value) {
            this.value = value;
        }

        public boolean hasResult() {
            return this != NONE;
        }

        public R get() {
            return this.value;
        }

        public R get(R fallback) {
            return (this != NONE) ? this.value : fallback;
        }

        public static <R> InvocationResult<R> of(R value) {
            return new InvocationResult<>(value);
        }

        @SuppressWarnings("unchecked")
        public static <R> InvocationResult<R> noResult() {
            return (InvocationResult<R>) NONE;
        }

    }

}
