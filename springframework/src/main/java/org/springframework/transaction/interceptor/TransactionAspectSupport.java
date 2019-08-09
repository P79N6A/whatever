package org.springframework.transaction.interceptor;

import io.vavr.control.Try;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.transaction.*;
import org.springframework.transaction.reactive.TransactionContextManager;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

public abstract class TransactionAspectSupport implements BeanFactoryAware, InitializingBean {
    // NOTE: This class must not implement Serializable because it serves as base
    // class for AspectJ aspects (which are not allowed to implement Serializable)!

    private static final Object DEFAULT_TRANSACTION_MANAGER_KEY = new Object();

    private static final boolean vavrPresent = ClassUtils.isPresent("io.vavr.control.Try", TransactionAspectSupport.class.getClassLoader());

    private static final boolean reactiveStreamsPresent = ClassUtils.isPresent("org.reactivestreams.Publisher", TransactionAspectSupport.class.getClassLoader());

    private static final ThreadLocal<TransactionInfo> transactionInfoHolder = new NamedThreadLocal<>("Current aspect-driven transaction");

    @Nullable
    protected static TransactionInfo currentTransactionInfo() throws NoTransactionException {
        return transactionInfoHolder.get();
    }

    public static TransactionStatus currentTransactionStatus() throws NoTransactionException {
        TransactionInfo info = currentTransactionInfo();
        if (info == null || info.transactionStatus == null) {
            throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope");
        }
        return info.transactionStatus;
    }

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private final ReactiveAdapterRegistry reactiveAdapterRegistry;

    @Nullable
    private String transactionManagerBeanName;

    @Nullable
    private TransactionManager transactionManager;

    @Nullable
    private TransactionAttributeSource transactionAttributeSource;

    @Nullable
    private BeanFactory beanFactory;

    private final ConcurrentMap<Object, Object> transactionManagerCache = new ConcurrentReferenceHashMap<>(4);

    protected TransactionAspectSupport() {
        if (reactiveStreamsPresent) {
            this.reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
        } else {
            this.reactiveAdapterRegistry = null;
        }
    }

    public void setTransactionManagerBeanName(@Nullable String transactionManagerBeanName) {
        this.transactionManagerBeanName = transactionManagerBeanName;
    }

    @Nullable
    protected final String getTransactionManagerBeanName() {
        return this.transactionManagerBeanName;
    }

    public void setTransactionManager(@Nullable TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Nullable
    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public void setTransactionAttributes(Properties transactionAttributes) {
        NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
        tas.setProperties(transactionAttributes);
        this.transactionAttributeSource = tas;
    }

    public void setTransactionAttributeSources(TransactionAttributeSource... transactionAttributeSources) {
        this.transactionAttributeSource = new CompositeTransactionAttributeSource(transactionAttributeSources);
    }

    public void setTransactionAttributeSource(@Nullable TransactionAttributeSource transactionAttributeSource) {
        this.transactionAttributeSource = transactionAttributeSource;
    }

    @Nullable
    public TransactionAttributeSource getTransactionAttributeSource() {
        return this.transactionAttributeSource;
    }

    @Override
    public void setBeanFactory(@Nullable BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Nullable
    protected final BeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        if (getTransactionManager() == null && this.beanFactory == null) {
            throw new IllegalStateException("Set the 'transactionManager' property or make sure to run within a BeanFactory " + "containing a PlatformTransactionManager bean!");
        }
        if (getTransactionAttributeSource() == null) {
            throw new IllegalStateException("Either 'transactionAttributeSource' or 'transactionAttributes' is required: " + "If there are no transactional methods, then don't use a transaction aspect.");
        }
    }

    @Nullable
    protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass, final InvocationCallback invocation) throws Throwable {
        if (this.reactiveAdapterRegistry != null) {
            ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(method.getReturnType());
            if (adapter != null) {
                return new ReactiveTransactionSupport(adapter).invokeWithinTransaction(method, targetClass, invocation);
            }
        }
        /*
         * AnnotationTransactionAttributeSource
         * 对每个符合条件的方法，都有缓存该方法的事务属性TransactionAttribute，包括传播机制、隔离级别等
         */
        TransactionAttributeSource tas = getTransactionAttributeSource();
        // 获取该方法的事务属性
        final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
        // 查找容器中的PlatformTransactionManager，事务拦截器，这里是DataSourceTransactionManager
        final PlatformTransactionManager tm = determineTransactionManager(txAttr);
        // 获取被代理的类方法名称
        final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);
        // 声明式事务
        if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) {
            // 根据需要创建事务
            TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
            Object retVal;
            try {
                // do it
                retVal = invocation.proceedWithInvocation();
            } catch (Throwable ex) {
                // 回滚事务
                completeTransactionAfterThrowing(txInfo, ex);
                throw ex;
            } finally {
                // 清理事务状态
                cleanupTransactionInfo(txInfo);
            }
            if (vavrPresent && VavrDelegate.isVavrTry(retVal)) {
                // Set rollback-only in case of Vavr failure matching our rollback rules...
                TransactionStatus status = txInfo.getTransactionStatus();
                if (status != null && txAttr != null) {
                    retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);
                }
            }
            // 执行成功，提交事务
            commitTransactionAfterReturning(txInfo);
            return retVal;
        }
        // 编程式事务
        else {
            final ThrowableHolder throwableHolder = new ThrowableHolder();
            // It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
            try {
                Object result = ((CallbackPreferringPlatformTransactionManager) tm).execute(txAttr, status -> {
                    TransactionInfo txInfo = prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
                    try {
                        Object retVal = invocation.proceedWithInvocation();
                        if (vavrPresent && VavrDelegate.isVavrTry(retVal)) {
                            // Set rollback-only in case of Vavr failure matching our rollback rules...
                            retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);
                        }
                        return retVal;
                    } catch (Throwable ex) {
                        if (txAttr.rollbackOn(ex)) {
                            // A RuntimeException: will lead to a rollback.
                            if (ex instanceof RuntimeException) {
                                throw (RuntimeException) ex;
                            } else {
                                throw new ThrowableHolderException(ex);
                            }
                        } else {
                            // A normal return value: will lead to a commit.
                            throwableHolder.throwable = ex;
                            return null;
                        }
                    } finally {
                        cleanupTransactionInfo(txInfo);
                    }
                });
                // Check result state: It might indicate a Throwable to rethrow.
                if (throwableHolder.throwable != null) {
                    throw throwableHolder.throwable;
                }
                return result;
            } catch (ThrowableHolderException ex) {
                throw ex.getCause();
            } catch (TransactionSystemException ex2) {
                if (throwableHolder.throwable != null) {
                    logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
                    ex2.initApplicationException(throwableHolder.throwable);
                }
                throw ex2;
            } catch (Throwable ex2) {
                if (throwableHolder.throwable != null) {
                    logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
                }
                throw ex2;
            }
        }
    }

    protected void clearTransactionManagerCache() {
        this.transactionManagerCache.clear();
        this.beanFactory = null;
    }

    @Nullable
    protected PlatformTransactionManager determineTransactionManager(@Nullable TransactionAttribute txAttr) {
        // Do not attempt to lookup tx manager if no tx attributes are set
        if (txAttr == null || this.beanFactory == null) {
            return asPlatformTransactionManager(getTransactionManager());
        }
        String qualifier = txAttr.getQualifier();
        if (StringUtils.hasText(qualifier)) {
            return determineQualifiedTransactionManager(this.beanFactory, qualifier);
        } else if (StringUtils.hasText(this.transactionManagerBeanName)) {
            return determineQualifiedTransactionManager(this.beanFactory, this.transactionManagerBeanName);
        } else {
            PlatformTransactionManager defaultTransactionManager = asPlatformTransactionManager(getTransactionManager());
            if (defaultTransactionManager == null) {
                defaultTransactionManager = asPlatformTransactionManager(this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY));
                if (defaultTransactionManager == null) {
                    defaultTransactionManager = this.beanFactory.getBean(PlatformTransactionManager.class);
                    this.transactionManagerCache.putIfAbsent(DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
                }
            }
            return defaultTransactionManager;
        }
    }

    private PlatformTransactionManager determineQualifiedTransactionManager(BeanFactory beanFactory, String qualifier) {
        PlatformTransactionManager txManager = asPlatformTransactionManager(this.transactionManagerCache.get(qualifier));
        if (txManager == null) {
            txManager = BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, PlatformTransactionManager.class, qualifier);
            this.transactionManagerCache.putIfAbsent(qualifier, txManager);
        }
        return txManager;
    }

    @Nullable
    private PlatformTransactionManager asPlatformTransactionManager(@Nullable Object transactionManager) {
        if (transactionManager == null || transactionManager instanceof PlatformTransactionManager) {
            return (PlatformTransactionManager) transactionManager;
        } else {
            throw new IllegalStateException("Specified transaction manager is not a PlatformTransactionManager: " + transactionManager);
        }
    }

    private String methodIdentification(Method method, @Nullable Class<?> targetClass, @Nullable TransactionAttribute txAttr) {
        String methodIdentification = methodIdentification(method, targetClass);
        if (methodIdentification == null) {
            if (txAttr instanceof DefaultTransactionAttribute) {
                methodIdentification = ((DefaultTransactionAttribute) txAttr).getDescriptor();
            }
            if (methodIdentification == null) {
                methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
            }
        }
        return methodIdentification;
    }

    @Nullable
    protected String methodIdentification(Method method, @Nullable Class<?> targetClass) {
        return null;
    }

    @SuppressWarnings("serial")
    protected TransactionInfo createTransactionIfNecessary(@Nullable PlatformTransactionManager tm, @Nullable TransactionAttribute txAttr, final String joinpointIdentification) {
        // If no name specified, apply method identification as transaction name.
        if (txAttr != null && txAttr.getName() == null) {
            txAttr = new DelegatingTransactionAttribute(txAttr) {
                @Override
                public String getName() {
                    return joinpointIdentification;
                }
            };
        }
        TransactionStatus status = null;
        if (txAttr != null) {
            if (tm != null) {
                // AbstractPlatformTransactionManager#getTransaction
                // 创建Transaction
                status = tm.getTransaction(txAttr);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping transactional joinpoint [" + joinpointIdentification + "] because no transaction manager has been configured");
                }
            }
        }
        // 封装TransactionInfo
        return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
    }

    protected TransactionInfo prepareTransactionInfo(@Nullable PlatformTransactionManager tm, @Nullable TransactionAttribute txAttr, String joinpointIdentification, @Nullable TransactionStatus status) {
        TransactionInfo txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification);
        if (txAttr != null) {
            // We need a transaction for this method...
            if (logger.isTraceEnabled()) {
                logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
            }
            // The transaction manager will flag an error if an incompatible tx already exists.
            txInfo.newTransactionStatus(status);
        } else {
            // The TransactionInfo.hasTransaction() method will return false. We created it only
            // to preserve the integrity of the ThreadLocal stack maintained in this class.
            if (logger.isTraceEnabled()) {
                logger.trace("No need to create transaction for [" + joinpointIdentification + "]: This method is not transactional.");
            }
        }
        // We always bind the TransactionInfo to the thread, even if we didn't create
        // a new transaction here. This guarantees that the TransactionInfo stack
        // will be managed correctly even if no transaction was created by this aspect.
        txInfo.bindToThread();
        return txInfo;
    }

    protected void commitTransactionAfterReturning(@Nullable TransactionInfo txInfo) {
        if (txInfo != null && txInfo.getTransactionStatus() != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
            }
            txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
        }
    }

    protected void completeTransactionAfterThrowing(@Nullable TransactionInfo txInfo, Throwable ex) {
        if (txInfo != null && txInfo.getTransactionStatus() != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "] after exception: " + ex);
            }
            // 判断回滚的默认依据是 抛出的异常是否是RuntimeException或者ERROR类型
            if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
                try {
                    // 事务回滚
                    txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
                } catch (TransactionSystemException ex2) {
                    logger.error("Application exception overridden by rollback exception", ex);
                    ex2.initApplicationException(ex);
                    throw ex2;
                } catch (RuntimeException | Error ex2) {
                    logger.error("Application exception overridden by rollback exception", ex);
                    throw ex2;
                }
            }
            // 如果不是，则提交事务
            else {
                // We don't roll back on this exception.
                // Will still roll back if TransactionStatus.isRollbackOnly() is true.
                try {
                    txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
                } catch (TransactionSystemException ex2) {
                    logger.error("Application exception overridden by commit exception", ex);
                    ex2.initApplicationException(ex);
                    throw ex2;
                } catch (RuntimeException | Error ex2) {
                    logger.error("Application exception overridden by commit exception", ex);
                    throw ex2;
                }
            }
        }
    }

    protected void cleanupTransactionInfo(@Nullable TransactionInfo txInfo) {
        if (txInfo != null) {
            txInfo.restoreThreadLocalStatus();
        }
    }

    protected static final class TransactionInfo {

        @Nullable
        private final PlatformTransactionManager transactionManager;

        @Nullable
        private final TransactionAttribute transactionAttribute;

        private final String joinpointIdentification;

        @Nullable
        private TransactionStatus transactionStatus;

        @Nullable
        private TransactionInfo oldTransactionInfo;

        public TransactionInfo(@Nullable PlatformTransactionManager transactionManager, @Nullable TransactionAttribute transactionAttribute, String joinpointIdentification) {
            this.transactionManager = transactionManager;
            this.transactionAttribute = transactionAttribute;
            this.joinpointIdentification = joinpointIdentification;
        }

        public PlatformTransactionManager getTransactionManager() {
            Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
            return this.transactionManager;
        }

        @Nullable
        public TransactionAttribute getTransactionAttribute() {
            return this.transactionAttribute;
        }

        public String getJoinpointIdentification() {
            return this.joinpointIdentification;
        }

        public void newTransactionStatus(@Nullable TransactionStatus status) {
            this.transactionStatus = status;
        }

        @Nullable
        public TransactionStatus getTransactionStatus() {
            return this.transactionStatus;
        }

        public boolean hasTransaction() {
            return (this.transactionStatus != null);
        }

        private void bindToThread() {
            // Expose current TransactionStatus, preserving any existing TransactionStatus
            // for restoration after this transaction is complete.
            this.oldTransactionInfo = transactionInfoHolder.get();
            transactionInfoHolder.set(this);
        }

        private void restoreThreadLocalStatus() {
            // Use stack to restore old transaction TransactionInfo.
            // Will be null if none was set.
            transactionInfoHolder.set(this.oldTransactionInfo);
        }

        @Override
        public String toString() {
            return (this.transactionAttribute != null ? this.transactionAttribute.toString() : "No transaction");
        }

    }

    @FunctionalInterface
    protected interface InvocationCallback {

        Object proceedWithInvocation() throws Throwable;

    }

    private static class ThrowableHolder {

        @Nullable
        public Throwable throwable;

    }

    @SuppressWarnings("serial")
    private static class ThrowableHolderException extends RuntimeException {

        public ThrowableHolderException(Throwable throwable) {
            super(throwable);
        }

        @Override
        public String toString() {
            return getCause().toString();
        }

    }

    private static class VavrDelegate {

        public static boolean isVavrTry(Object retVal) {
            return (retVal instanceof Try);
        }

        public static Object evaluateTryFailure(Object retVal, TransactionAttribute txAttr, TransactionStatus status) {
            return ((Try<?>) retVal).onFailure(ex -> {
                if (txAttr.rollbackOn(ex)) {
                    status.setRollbackOnly();
                }
            });
        }

    }

    private class ReactiveTransactionSupport {

        private final ReactiveAdapter adapter;

        public ReactiveTransactionSupport(ReactiveAdapter adapter) {
            this.adapter = adapter;
        }

        public Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass, InvocationCallback invocation) {
            // If the transaction attribute is null, the method is non-transactional.
            TransactionAttributeSource tas = getTransactionAttributeSource();
            TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
            ReactiveTransactionManager tm = determineTransactionManager(txAttr);
            String joinpointIdentification = methodIdentification(method, targetClass, txAttr);
            // Optimize for Mono
            if (Mono.class.isAssignableFrom(method.getReturnType())) {
                return TransactionContextManager.currentContext().flatMap(context -> createTransactionIfNecessary(tm, txAttr, joinpointIdentification).flatMap(it -> {
                    try {
                        // This is an around advice: Invoke the next interceptor in the chain.
                        // This will normally result in a target object being invoked.
                        // Need re-wrapping of ReactiveTransaction until we get hold of the exception
                        // through usingWhen.
                        return Mono.<Object, ReactiveTransactionInfo>usingWhen(Mono.just(it), txInfo -> {
                            try {
                                return (Mono<?>) invocation.proceedWithInvocation();
                            } catch (Throwable throwable) {
                                return Mono.error(throwable);
                            }
                        }, this::commitTransactionAfterReturning, txInfo -> Mono.empty()).onErrorResume(ex -> completeTransactionAfterThrowing(it, ex).then(Mono.error(ex)));
                    } catch (Throwable ex) {
                        // target invocation exception
                        return completeTransactionAfterThrowing(it, ex).then(Mono.error(ex));
                    }
                })).subscriberContext(TransactionContextManager.getOrCreateContext()).subscriberContext(TransactionContextManager.getOrCreateContextHolder());
            }
            return TransactionContextManager.currentContext().flatMapMany(context -> createTransactionIfNecessary(tm, txAttr, joinpointIdentification).flatMapMany(it -> {
                try {
                    // This is an around advice: Invoke the next interceptor in the chain.
                    // This will normally result in a target object being invoked.
                    // Need re-wrapping of ReactiveTransaction until we get hold of the exception
                    // through usingWhen.
                    return Flux.usingWhen(Mono.just(it), txInfo -> {
                        try {
                            return this.adapter.toPublisher(invocation.proceedWithInvocation());
                        } catch (Throwable throwable) {
                            return Mono.error(throwable);
                        }
                    }, this::commitTransactionAfterReturning, txInfo -> Mono.empty()).onErrorResume(ex -> completeTransactionAfterThrowing(it, ex).then(Mono.error(ex)));
                } catch (Throwable ex) {
                    // target invocation exception
                    return completeTransactionAfterThrowing(it, ex).then(Mono.error(ex));
                }
            })).subscriberContext(TransactionContextManager.getOrCreateContext()).subscriberContext(TransactionContextManager.getOrCreateContextHolder());
        }

        @Nullable
        private ReactiveTransactionManager determineTransactionManager(@Nullable TransactionAttribute txAttr) {
            // Do not attempt to lookup tx manager if no tx attributes are set
            if (txAttr == null || beanFactory == null) {
                return asReactiveTransactionManager(getTransactionManager());
            }
            String qualifier = txAttr.getQualifier();
            if (StringUtils.hasText(qualifier)) {
                return determineQualifiedTransactionManager(beanFactory, qualifier);
            } else if (StringUtils.hasText(transactionManagerBeanName)) {
                return determineQualifiedTransactionManager(beanFactory, transactionManagerBeanName);
            } else {
                ReactiveTransactionManager defaultTransactionManager = asReactiveTransactionManager(getTransactionManager());
                if (defaultTransactionManager == null) {
                    defaultTransactionManager = asReactiveTransactionManager(transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY));
                    if (defaultTransactionManager == null) {
                        defaultTransactionManager = beanFactory.getBean(ReactiveTransactionManager.class);
                        transactionManagerCache.putIfAbsent(DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
                    }
                }
                return defaultTransactionManager;
            }
        }

        private ReactiveTransactionManager determineQualifiedTransactionManager(BeanFactory beanFactory, String qualifier) {
            ReactiveTransactionManager txManager = asReactiveTransactionManager(transactionManagerCache.get(qualifier));
            if (txManager == null) {
                txManager = BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, ReactiveTransactionManager.class, qualifier);
                transactionManagerCache.putIfAbsent(qualifier, txManager);
            }
            return txManager;
        }

        @Nullable
        private ReactiveTransactionManager asReactiveTransactionManager(@Nullable Object transactionManager) {
            if (transactionManager == null || transactionManager instanceof ReactiveTransactionManager) {
                return (ReactiveTransactionManager) transactionManager;
            } else {
                throw new IllegalStateException("Specified transaction manager is not a ReactiveTransactionManager: " + transactionManager);
            }
        }

        @SuppressWarnings("serial")
        private Mono<ReactiveTransactionInfo> createTransactionIfNecessary(@Nullable ReactiveTransactionManager tm, @Nullable TransactionAttribute txAttr, final String joinpointIdentification) {
            // If no name specified, apply method identification as transaction name.
            if (txAttr != null && txAttr.getName() == null) {
                txAttr = new DelegatingTransactionAttribute(txAttr) {
                    @Override
                    public String getName() {
                        return joinpointIdentification;
                    }
                };
            }
            TransactionAttribute attrToUse = txAttr;
            Mono<ReactiveTransaction> tx = Mono.empty();
            if (txAttr != null) {
                if (tm != null) {
                    tx = tm.getReactiveTransaction(txAttr);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipping transactional joinpoint [" + joinpointIdentification + "] because no transaction manager has been configured");
                    }
                }
            }
            return tx.map(it -> prepareTransactionInfo(tm, attrToUse, joinpointIdentification, it)).switchIfEmpty(Mono.defer(() -> Mono.just(prepareTransactionInfo(tm, attrToUse, joinpointIdentification, null))));
        }

        private ReactiveTransactionInfo prepareTransactionInfo(@Nullable ReactiveTransactionManager tm, @Nullable TransactionAttribute txAttr, String joinpointIdentification, @Nullable ReactiveTransaction transaction) {
            ReactiveTransactionInfo txInfo = new ReactiveTransactionInfo(tm, txAttr, joinpointIdentification);
            if (txAttr != null) {
                // We need a transaction for this method...
                if (logger.isTraceEnabled()) {
                    logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
                }
                // The transaction manager will flag an error if an incompatible tx already exists.
                txInfo.newReactiveTransaction(transaction);
            } else {
                // The TransactionInfo.hasTransaction() method will return false. We created it only
                // to preserve the integrity of the ThreadLocal stack maintained in this class.
                if (logger.isTraceEnabled()) {
                    logger.trace("Don't need to create transaction for [" + joinpointIdentification + "]: This method isn't transactional.");
                }
            }
            return txInfo;
        }

        private Mono<Void> commitTransactionAfterReturning(@Nullable ReactiveTransactionInfo txInfo) {
            if (txInfo != null && txInfo.getReactiveTransaction() != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
                }
                return txInfo.getTransactionManager().commit(txInfo.getReactiveTransaction());
            }
            return Mono.empty();
        }

        private Mono<Void> completeTransactionAfterThrowing(@Nullable ReactiveTransactionInfo txInfo, Throwable ex) {
            if (txInfo != null && txInfo.getReactiveTransaction() != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "] after exception: " + ex);
                }
                if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
                    return txInfo.getTransactionManager().rollback(txInfo.getReactiveTransaction()).onErrorMap(ex2 -> {
                        logger.error("Application exception overridden by rollback exception", ex);
                        if (ex2 instanceof TransactionSystemException) {
                            ((TransactionSystemException) ex2).initApplicationException(ex);
                        }
                        return ex2;
                    });
                } else {
                    // We don't roll back on this exception.
                    // Will still roll back if TransactionStatus.isRollbackOnly() is true.
                    return txInfo.getTransactionManager().commit(txInfo.getReactiveTransaction()).onErrorMap(ex2 -> {
                        logger.error("Application exception overridden by commit exception", ex);
                        if (ex2 instanceof TransactionSystemException) {
                            ((TransactionSystemException) ex2).initApplicationException(ex);
                        }
                        return ex2;
                    });
                }
            }
            return Mono.empty();
        }

    }

    private static final class ReactiveTransactionInfo {

        @Nullable
        private final ReactiveTransactionManager transactionManager;

        @Nullable
        private final TransactionAttribute transactionAttribute;

        private final String joinpointIdentification;

        @Nullable
        private ReactiveTransaction reactiveTransaction;

        public ReactiveTransactionInfo(@Nullable ReactiveTransactionManager transactionManager, @Nullable TransactionAttribute transactionAttribute, String joinpointIdentification) {
            this.transactionManager = transactionManager;
            this.transactionAttribute = transactionAttribute;
            this.joinpointIdentification = joinpointIdentification;
        }

        public ReactiveTransactionManager getTransactionManager() {
            Assert.state(this.transactionManager != null, "No ReactiveTransactionManager set");
            return this.transactionManager;
        }

        @Nullable
        public TransactionAttribute getTransactionAttribute() {
            return this.transactionAttribute;
        }

        public String getJoinpointIdentification() {
            return this.joinpointIdentification;
        }

        public void newReactiveTransaction(@Nullable ReactiveTransaction transaction) {
            this.reactiveTransaction = transaction;
        }

        @Nullable
        public ReactiveTransaction getReactiveTransaction() {
            return this.reactiveTransaction;
        }

        @Override
        public String toString() {
            return (this.transactionAttribute != null ? this.transactionAttribute.toString() : "No transaction");
        }

    }

}
