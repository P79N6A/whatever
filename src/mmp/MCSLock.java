package mmp;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;


// MCS Spinlock 基于链表 公平 自旋锁
// 申请线程只在本地变量上自旋，直接前驱负责通知其结束自旋，减少了不必要的处理器缓存同步的次数
public class MCSLock {

    // 独占，且不能重入

    // CLH是在前驱节点的属性上自旋，MCS是在本地属性变量上自旋
    // CLH的队列是隐式的，CLHNode并不实际持有下一个节点；MCS的队列是物理存在的
    // CLH锁释放时只需要改变自己的属性，MCS锁释放则需要改变后继节点的属性
    public static class MCSNode {
        MCSNode next;
        boolean isLocked = true; // 默认是在等待锁
    }

    volatile MCSNode queue; // 指向最后一个申请锁的MCSNode

    private static final AtomicReferenceFieldUpdater<MCSLock, MCSNode> UPDATER = AtomicReferenceFieldUpdater.newUpdater(MCSLock.class, MCSNode.class, "queue");

    public void lock(MCSNode currentThreadMcsNode) {
        MCSNode predecessor = UPDATER.getAndSet(this, currentThreadMcsNode); // 1

        if (predecessor != null) {
            // 添加到队尾
            predecessor.next = currentThreadMcsNode; // 2
            // 自旋
            while (currentThreadMcsNode.isLocked) {
            }
        }
    }

    public void unlock(MCSNode currentThreadMcsNode) {
        // 锁拥有者才能释放锁
        if (UPDATER.get(this) == currentThreadMcsNode) {
            // 检查是否自己是否是队尾
            if (currentThreadMcsNode.next == null) {
                // 自己是队尾，置空返回
                if (UPDATER.compareAndSet(this, currentThreadMcsNode, null)) {
                    return;
                }
                // 自己不是队尾
                else {
                    // 忙等是因为：step 1执行完后，step 2可能还没执行完
                    while (currentThreadMcsNode.next == null) {
                    }
                }
            }
            // 通知下一个节点
            currentThreadMcsNode.next.isLocked = false;
            currentThreadMcsNode.next = null; // for GC
        }
    }
}
