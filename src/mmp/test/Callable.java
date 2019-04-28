package mmp.test;

@FunctionalInterface
public interface Callable<V> {

    V call() throws Exception;
}
