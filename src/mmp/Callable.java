package mmp;

@FunctionalInterface
public interface Callable<V> {

    V call() throws Exception;
}
