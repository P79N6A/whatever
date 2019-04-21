package mmp.container;


import mmp.lock.ReentrantLock;

import java.util.Arrays;
import java.util.Collection;

// CopyOnWrite 写时复制
// 在写入新元素时不直接操作原容器，而是先复制一个快照，对这个快照操作，在操作结束后再将原容器的引用指向新引用
public class CopyOnWriteArrayList<E> {

    final transient ReentrantLock lock = new ReentrantLock();

    // volatile 可见性、有序性  更新之后立即就能被其他线程看到
    // 但不保证原子性
    private transient volatile Object[] array;

    final Object[] getArray() {
        return array;
    }

    final void setArray(Object[] a) {
        array = a;
    }

    // 三种构造函数 分别创建了空的数组、使用指定集合和数组来创建新数组
    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }


    public CopyOnWriteArrayList(Collection<? extends E> c) {
        Object[] elements;
        elements = c.toArray();
        if (elements.getClass() != Object[].class) elements = Arrays.copyOf(elements, elements.length, Object[].class);
        setArray(elements);
    }

    public CopyOnWriteArrayList(E[] toCopyIn) {
        setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
    }

    public int size() {
        return getArray().length;
    }


    // Positional Access Operations

    @SuppressWarnings("unchecked")
    private E get(Object[] a, int index) {
        return (E) a[index];
    }

    public E get(int index) {
        return get(getArray(), index);
    }

    public E set(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            E oldValue = get(elements, index);
            if (oldValue != element) {
                int len = elements.length;
                Object[] newElements = Arrays.copyOf(elements, len);
                newElements[index] = element;
                setArray(newElements);
            } else {
                // Not quite a no-op; ensures volatile write semantics
                setArray(elements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }


    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 拷贝一个新数组
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            // 添加进元素
            newElements[len] = e;
            // 将原数组的引用修改为拷贝数组
            // 这步前，其他线程访问的就是旧数据
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }


    public void add(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0) throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + len);
            Object[] newElements;
            int numMoved = len - index;
            if (numMoved == 0) newElements = Arrays.copyOf(elements, len + 1);
            else {
                newElements = new Object[len + 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index, newElements, index + 1, numMoved);
            }
            newElements[index] = element;
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }


    public E remove(int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            E oldValue = get(elements, index);
            int numMoved = len - index - 1;
            if (numMoved == 0) setArray(Arrays.copyOf(elements, len - 1));
            else {
                Object[] newElements = new Object[len - 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index + 1, newElements, index, numMoved);
                setArray(newElements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }


    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            setArray(new Object[0]);
        } finally {
            lock.unlock();
        }
    }


    // 迭代器不支持 remove ，set， add，抛出 UnsupportedOperationException 异常
    // 迭代器永远不会抛出 ConcurrentModificationException异常
    // 可以在 for 循环中做删除操作，删除之后也都是拷贝重写赋值，缺点就是可能读取的不是最新的值

    // ArrayList 用 for 循环删除实际用迭代器的 next 方法，会比较 expectedModCount 和 modCount 是否相等
    // 不相等就会抛出 ConcurrentModificationException ，也就是 fail-fast

    // Git 多人协作流程：
    // 修改代码时，拉取服务端代码到本地，先修改本地的代码，再提交到服务器，避免直接修改远端代码对别人造成影响

}
