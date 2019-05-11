package mmp.test;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadLocal<T> {

    // 每个实例都唯一
    private final int threadLocalHashCode = nextHashCode();


    private static AtomicInteger nextHashCode = new AtomicInteger();

    // Hash增量
    private static final int HASH_INCREMENT = 0x61c88647;


    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    // 重写
    protected T initialValue() {
        return null;
    }


    public ThreadLocal() {
    }


    public T get() {
        // 获取当前线程
        Thread t = Thread.currentThread();
        // 获取当前线程的ThreadLocalMap对象
        ThreadLocalMap map = getMap(t);
        // 如果map不是null
        if (map != null) {
            // ThreadLocal对象作为key获取对应的值
            ThreadLocalMap.Entry e = map.getEntry(this);
            // 如果该值存在，则返回该值
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T) e.value;
                return result;
            }
        }
        return setInitialValue();
    }


    private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) map.set(this, value);
        else createMap(t, value);
        return value;
    }


    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) map.set(this, value);
        else createMap(t, value);
    }


    /*
     * Java有线程池，线程基本不会退出，如果在线程中放置了一个大大的对象，没有清除（调用 remove），会一直留在线程中，造成内存泄漏
     * 所以有了弱引用Entry，GC自动清除，通过判断key是否为null清除无用数据
     * 如果ThreadLocal 是静态变量，并且使用结束后没有设置为null，ThreadLocal对象是无法自动删除的
     * 因此需要调用remove方法，或者使用完毕后置为null
     *
     * JDK建议将ThreadLocal变量定义成private static，这样ThreadLocal的生命周期就更长
     * 由于一直存在ThreadLocal的强引用，所以ThreadLocal不会被回收
     * 所以任何时候都能根据ThreadLocal的弱引用访问到Entry的value值，然后remove它，防止内存泄露
     *
     * ThreadLocal自动回收的时刻：
     * remove
     * get && hash冲突了
     * set && hash冲突了
     * set && 正常插入（覆盖不会清理）
     * */

    public void remove() {
        ThreadLocalMap m = getMap(Thread.currentThread());
        if (m != null) m.remove(this);
    }


    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }


    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }


    static class ThreadLocalMap {

        // ThreadLocal是被弱引用的对象
        // 当一个对象仅仅被弱引用指向，而没有任何其他强引用指向时
        // 如果这时GC，这个对象就会被回收，不论当前空间是否足够
        static class Entry extends WeakReference<ThreadLocal<?>> {
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                // key是弱引用
                super(k);
                value = v;
            }
        }

        private static final int INITIAL_CAPACITY = 16;

        // table大小必须是2的N次幂
        private Entry[] table;

        private int size = 0;

        // 扩容阈值
        private int threshold; // Default to 0

        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            // 创建一个16长度的Entry数组
            table = new Entry[INITIAL_CAPACITY];
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }


        private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            // 有元素 && hash没有冲突，直接返回
            if (e != null && e.get() == key) return e;
                // 冲突，向下一个位置查询
            else return getEntryAfterMiss(key, i, e);
        }

        // 循环所有的元素，直到找到key对应的entry
        // 如果发现了某个元素的key是null，顺手expungeStaleEntry清理所有key为null的entry
        // Entry内的value也就没有强引用链，自然会被回收
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal<?> k = e.get();
                // 正中
                if (k == key) return e;
                // 顺手清理key为null的entry
                if (k == null) expungeStaleEntry(i);
                    // 下一个位置
                else i = nextIndex(i, len);
                e = tab[i];
                /*
                 * HashMap的冲突方法是拉链法，即用链表来处理
                 * ThreadLocalMap处理冲突采用的是线性探测法，即这个槽不行，就换下一个槽，直到插入为止
                 * */
            }
            return null;
        }


        private void set(ThreadLocal<?> key, Object value) {

            Entry[] tab = table;
            int len = tab.length;
            // 根据ThreadLocal的HashCode得到对应的下标
            int i = key.threadLocalHashCode & (len - 1);
            // 首先通过下标找对应的entry对象，如果没有，则创建一个新的entry对象
            // 如果找到了，但key冲突了或者key是null，则将下标加一（加一后如果小于数组长度则使用该值，否则使用0）
            // 再次尝试获取对应的entry，如果不为null，则在循环中继续判断key，是否重复或者k是否是null
            for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();
                // key相同，覆盖value
                if (k == key) {
                    e.value = value;
                    return;
                }
                // 如果key被GC了（因为是软引用），则创建一个新的entry对象填充该槽
                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }
            // 创建一个新的entry对象
            tab[i] = new Entry(key, value);
            // 长度加一
            int sz = ++size;
            // 如果没有清除多余的entry，并且数组长度达到了阀值，则扩容
            if (!cleanSomeSlots(i, sz) && sz >= threshold) rehash();
        }

        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len - 1);
            // 线性探测法找到key对应的entry，调用clear，将ThreadLocal设置为null
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
                    // 顺便会清理所有的key为null的entry
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        // 删除陈旧的entry（ThreadLocal为null），将entry key为null的对象设置为null
        private void replaceStaleEntry(ThreadLocal<?> key, Object value, int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len); (e = tab[i]) != null; i = prevIndex(i, len))
                if (e.get() == null) slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            for (int i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();

                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    if (slotToExpunge == staleSlot) slotToExpunge = i;
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                if (k == null && slotToExpunge == staleSlot) slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
            if (slotToExpunge != staleSlot) cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        // 通过线性探测法，找到每个槽位，如果该槽位的key为相同，就替换这个value
        // 如果这个key是null，则将原来的entry设置为null，并重新创建一个entry
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            // 清空键值对
            // expunge entry at staleSlot
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            //
            size--;

            // Rehash until we encounter null
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null) h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        // 清除无用的entry，也就是key为null的节点
        // 遍历所有的entry，并判断他们的key，如果key是null，则调用expungeStaleEntry方法，也就是清除entry
        // 最后返回true，如果返回了false ，说明没有清除，并且size>=10 ，就需要rehash
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ((n >>>= 1) != 0);
            return removed;
        }

        private void rehash() {
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            if (size >= threshold - threshold / 4) resize();
        }

        // 扩容分两步，当长度达到了容量的2/3，就会清理无用的数据
        // 如果清理完之后，长度还大于等于阀值的3/4，那么就真正的扩容
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            // 直接扩容为原来的2倍
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;
            // 将老数组的数据都移动到新数组
            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null) h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null) expungeStaleEntry(j);
            }
        }
    }
}
