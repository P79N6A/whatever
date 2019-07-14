package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.threadlocal.NamedInternalThreadFactory;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.DEFAULT_FORKS;
import static org.apache.dubbo.rpc.cluster.Constants.FORKS_KEY;

/**
 * 在运行时通过线程池创建多个线程，并发调用多个服务提供者
 * 只要有一个服务提供者成功返回了结果，doInvoke方法就会立即结束运行
 * 应用场景是在一些对实时性要求比较高读操作（注意是读操作，并行写操作可能不安全）下使用，但这将会耗费更多的资源
 */
public class ForkingClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private final ExecutorService executor = Executors.newCachedThreadPool(new NamedInternalThreadFactory("forking-cluster-timer", true));

    public ForkingClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result doInvoke(final Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        try {
            checkInvokers(invokers, invocation);
            final List<Invoker<T>> selected;
            // 获取forks配置
            final int forks = getUrl().getParameter(FORKS_KEY, DEFAULT_FORKS);
            // 获取超时配置
            final int timeout = getUrl().getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
            // 如果forks配置不合理，则直接将invokers赋值给selected

            /*
             * 选出forks个Invoker，为接下来的并发调用提供输入
             */
            if (forks <= 0 || forks >= invokers.size()) {
                selected = invokers;
            } else {
                selected = new ArrayList<>();
                // 循环选出forks个Invoker，并添加到selected
                for (int i = 0; i < forks; i++) {
                    // 选择Invoker
                    Invoker<T> invoker = select(loadbalance, invocation, invokers, selected);
                    if (!selected.contains(invoker)) {
                        selected.add(invoker);
                    }
                }
            }
            // ----------------------✨ 分割线1 ✨---------------------- //
            /*
             * 通过线程池并发调用多个Invoker，并将结果存储在阻塞队列中
             */
            RpcContext.getContext().setInvokers((List) selected);
            final AtomicInteger count = new AtomicInteger();
            final BlockingQueue<Object> ref = new LinkedBlockingQueue<>();
            // 遍历selected列表
            for (final Invoker<T> invoker : selected) {
                // 为每个Invoker创建一个执行线程
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 进行远程调用
                            Result result = invoker.invoke(invocation);
                            // 将结果存到阻塞队列中
                            ref.offer(result);
                        } catch (Throwable e) {
                            int value = count.incrementAndGet();
                            // 仅在value>=selected.size()时，才将异常对象放入阻塞队列中
                            /*
                             * 在并行调用多个服务提供者的情况下，只要有一个服务提供者能够成功返回结果，而其他全部失败
                             * 此时ForkingClusterInvoker仍应该返回成功的结果，而非抛出异常
                             * 在value >= selected.size()时将异常对象放入阻塞队列中，可以保证异常对象不会出现在正常结果的前面，这样可从阻塞队列中优先取出正常的结果
                             */
                            if (value >= selected.size()) {
                                // 将异常对象存入到阻塞队列中
                                ref.offer(e);
                            }
                        }
                    }
                });
            }
            // ----------------------✨ 分割线2 ✨---------------------- //
            /*
             * 从阻塞队列中获取返回结果，并对返回结果类型进行判断
             * 如果为异常类型，则直接抛出，否则返回
             */
            try {
                // 从阻塞队列中取出远程调用结果
                Object ret = ref.poll(timeout, TimeUnit.MILLISECONDS);
                // 如果结果类型为Throwable，则抛出异常
                if (ret instanceof Throwable) {
                    Throwable e = (Throwable) ret;
                    throw new RpcException(e instanceof RpcException ? ((RpcException) e).getCode() : 0, "Failed to forking invoke provider " + selected + ", but no luck to perform the invocation. Last error is: " + e.getMessage(), e.getCause() != null ? e.getCause() : e);
                }
                // 返回结果
                return (Result) ret;
            } catch (InterruptedException e) {
                throw new RpcException("Failed to forking invoke provider " + selected + ", but no luck to perform the invocation. Last error is: " + e.getMessage(), e);
            }
        } finally {
            RpcContext.getContext().clearAttachments();
        }
    }

}
