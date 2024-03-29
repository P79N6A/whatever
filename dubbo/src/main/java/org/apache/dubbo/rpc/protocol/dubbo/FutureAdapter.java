package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.remoting.exchange.ResponseCallback;
import org.apache.dubbo.remoting.exchange.ResponseFuture;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureAdapter<V> extends CompletableFuture<V> {

    private final ResponseFuture future;

    private CompletableFuture<Result> resultFuture;

    public FutureAdapter(ResponseFuture future) {
        this.future = future;
        this.resultFuture = new CompletableFuture<>();
        future.setCallback(new ResponseCallback() {
            @Override
            public void done(Object response) {
                Result result = (Result) response;
                FutureAdapter.this.resultFuture.complete(result);
                V value = null;
                try {
                    value = (V) result.recreate();
                } catch (Throwable t) {
                    FutureAdapter.this.completeExceptionally(t);
                }
                FutureAdapter.this.complete(value);
            }

            @Override
            public void caught(Throwable exception) {
                FutureAdapter.this.completeExceptionally(exception);
            }
        });
    }

    public ResponseFuture getFuture() {
        return future;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return super.isDone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } catch (ExecutionException | InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RpcException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RpcException(e);
        }
    }

    public CompletableFuture<Result> getResultFuture() {
        return resultFuture;
    }

}
