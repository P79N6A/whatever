package mmp.test;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

/**
 * 参考 https://www.jianshu.com/p/6bac52527ca4
 */
public class CompletableFutureTest {

    /*
     * CompletableFuture提供了四个静态方法创建一个异步操作
     * Async结尾的方法都可以异步执行，如果指定了线程池，会在指定的线程池中执行，如果没有指定，默认在ForkJoinPool.commonPool()中执行
     * runAsync不支持返回值，supplyAsync支持返回值
     *
     * public static CompletableFuture<Void> runAsync(Runnable runnable)
     * public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor)
     * public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier)
     * public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor)
     */

    /**
     * 无返回值
     */
    public static void runAsync() throws Exception {

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            sleep(1);
            System.out.println("run end ...");
        });

        System.out.println(future.get());

    }

    /**
     * 有返回值
     */
    public static void supplyAsync() throws Exception {

        CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
            sleep(1);
            System.out.println("run end ...");
            return System.currentTimeMillis();
        });


        System.out.println(future.get());
    }

    /*
     * 当CompletableFuture的计算结果完成，或者抛出异常的时候，可以执行特定的Action
     * Action的类型是BiConsumer<? super T,? super Throwable>，它可以处理正常的计算结果，或者异常情况
     * whenComplete：当前线程执行whenComplete的任务
     * whenCompleteAsync：把任务提交给线程池执行
     *
     * public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action)
     * public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action)
     * public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor)
     * public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn)
     */
    public static void whenComplete() throws Exception {

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            sleep(1);
            int i = 12 / 0;
            System.out.println("run end ...");
        });

        future.whenComplete(new BiConsumer<Void, Throwable>() {
            @Override
            public void accept(Void t, Throwable action) {
                System.out.println("执行完成！");
            }

        });

        future.exceptionally(new Function<Throwable, Void>() {
            @Override
            public Void apply(Throwable t) {
                System.out.println("执行失败！" + t.getMessage());
                return null;
            }
        });

        sleep(2);
    }

    /*
     * 当一个线程依赖另一个线程时，可以使用thenApply把这两个线程串行化
     * Function<? super T,? extends U>
     * T：上一个任务返回结果的类型
     * U：当前任务的返回值类型
     *
     * public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn)
     * public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn)
     * public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor)
     */
    private static void thenApply() throws Exception {

        CompletableFuture<Long> future = CompletableFuture.supplyAsync(new Supplier<Long>() {
            @Override
            public Long get() {
                long result = new Random().nextInt(100);
                System.out.println("result1=" + result);
                return result;
            }
        })
                // 第二个任务依赖第一个任务的结果
                .thenApply(new Function<Long, Long>() {
                    @Override
                    public Long apply(Long t) {
                        long result = t * 5;
                        System.out.println("result2=" + result);
                        return result;
                    }
                });


        System.out.println(future.get());

    }

    /*
     * handle，执行任务完成时对结果的处理，和thenApply处理方式基本一样
     * 不同的是handle是在任务完成后再执行，还可以处理异常的任务
     * henApply只可以执行正常的任务，任务异常则不执行thenApply方法
     *
     * public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);
     * public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn);
     * public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor);
     */
    public static void handle() throws Exception {

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                throw new RuntimeException();
            }
        }).handle(new BiFunction<Integer, Throwable, Integer>() {
            @Override
            public Integer apply(Integer param, Throwable throwable) {
                return throwable == null ? param * 2 : null;
            }
        });

        System.out.println(future.get());
    }

    /*
     * thenAccept，接收任务的处理结果，并消费处理，无返回结果
     *
     * public CompletionStage<Void> thenAccept(Consumer<? super T> action);
     * public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action);
     * public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor);
     */
    public static void thenAccept() throws Exception {

        CompletableFuture<Void> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return 0;
            }
        }).thenAccept(System.out::println);

        System.out.println(future.get());
    }

    /*
     * thenRun，不关心任务的处理结果，只要上面的任务执行完成，就开始执行thenAccept
     *
     * public CompletionStage<Void> thenRun(Runnable action);
     * public CompletionStage<Void> thenRunAsync(Runnable action);
     * public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor);
     */

    public static void thenRun() throws Exception {

        CompletableFuture<Void> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return 1;
            }
        })
                // 上个任务处理完成后，不会把结果传给thenRun
                .thenRun(() -> {
                    System.out.println("thenRun ...");
                });

        System.out.println(future.get());
    }

    /*
     * thenCombine合并任务，把两个CompletionStage的任务都执行完成后，把两个任务的结果一块交给thenCombine
     *
     * public <U,V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);
     * public <U,V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);
     * public <U,V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor);
     */
    private static void thenCombine() throws Exception {

        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                return "hello";
            }
        });

        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                return "bitch";
            }
        });

        CompletableFuture<String> result = future1.thenCombine(future2, new BiFunction<String, String, String>() {
            @Override
            public String apply(String t, String u) {
                return t + " " + u;
            }
        });

        System.out.println(result.get());
    }

    /*
     * 当两个CompletionStage都执行完成后，把结果一块交给thenAcceptBoth来进行消耗
     *
     * public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action);
     * public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action);
     * public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor);
     */
    private static void thenAcceptBoth() throws Exception {

        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                sleep(1);
                int t = 1;
                System.out.println("f1=" + t);
                return t;
            }
        });

        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                sleep(1);
                int t = 2;
                System.out.println("f2=" + t);
                return t;
            }
        });

        f1.thenAcceptBoth(f2, new BiConsumer<Integer, Integer>() {
            @Override
            public void accept(Integer t, Integer u) {
                System.out.println("f1=" + t + ";f2=" + u + ";");
            }
        });
    }

    /*
     * applyToEither，两个CompletionStage，谁执行返回的结果快，就用那个CompletionStage的结果进行下一步的转化操作
     *
     * public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn);
     * public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn);
     * public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor);
     */

    private static void applyToEither() throws Exception {

        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                sleep(1);
                int t = 1;
                System.out.println("f1=" + t);
                return t;
            }
        });

        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                sleep(1);
                int t = 2;
                System.out.println("f2=" + t);
                return t;
            }
        });

        CompletableFuture<Integer> result = f1.applyToEither(f2, new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t) {
                System.out.println(t);
                return t * 2;
            }
        });

        System.out.println(result.get());
    }

    /*
     * acceptEither，两个CompletionStage，谁执行返回的结果快，就用那个CompletionStage的结果进行下一步的消耗操作
     *
     * public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action);
     * public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action);
     * public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor);
     */

    private static void acceptEither() throws Exception {

        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                sleep(1);
                int t = 1;
                System.out.println("f1=" + t);
                return t;
            }
        });

        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                sleep(1);
                int t = 2;
                System.out.println("f2=" + t);
                return t;
            }
        });

        f1.acceptEither(f2, new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                System.out.println(t);
            }
        });
    }

    /*
     * runAfterEither，两个CompletionStage，任何一个完成了都会执行下一步的操作（Runnable）
     *
     * public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action);
     * public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action);
     * public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor);
     */
    private static void runAfterEither() throws Exception {

        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                sleep(1);
                int t = 1;
                System.out.println("f1=" + t);
                return t;
            }
        });

        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                sleep(1);
                int t = 2;
                System.out.println("f2=" + t);
                return t;
            }
        });

        f1.runAfterEither(f2, new Runnable() {
            @Override
            public void run() {
                System.out.println("上面有一个已经完成了。");
            }
        });
    }

    /*
     * runAfterBoth，两个CompletionStage，都完成了计算才会执行下一步的操作（Runnable）
     *
     * public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action);
     * public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action);
     * public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor);
     */
    private static void runAfterBoth() throws Exception {

        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                sleep(1);
                int t = 1;
                System.out.println("f1=" + t);
                return t;
            }
        });

        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                sleep(1);
                int t = 2;
                System.out.println("f2=" + t);
                return t;
            }
        });

        f1.runAfterBoth(f2, new Runnable() {

            @Override
            public void run() {
                System.out.println("上面两个任务都执行完成了。");
            }
        });
    }

    /*
     * thenCompose，对两个CompletionStage进行流水线操作，第一个操作完成时，将其结果作为参数传递给第二个操作
     *
     * public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);
     * public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn);
     * public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor);
     */
    private static void thenCompose() throws Exception {

        CompletableFuture<Integer> f = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return 1;
            }
        }).thenCompose(new Function<Integer, CompletionStage<Integer>>() {
            @Override
            public CompletionStage<Integer> apply(Integer param) {
                return CompletableFuture.supplyAsync(new Supplier<Integer>() {
                    @Override
                    public Integer get() {
                        return param * 2;
                    }
                });
            }

        });

        System.out.println(f.get());
    }

    private static void sleep(Integer second) {
        try {
            TimeUnit.SECONDS.sleep(second);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
