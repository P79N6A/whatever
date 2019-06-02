package mmp.nio.test;

import mmp.nio.ByteBuffer;
import mmp.nio.MappedByteBuffer;
import mmp.nio.channels.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.Set;

public class Test {

    public static void main(String[] args) {

    }

    private static void test_k() throws Exception {

        File fileSrc = new File("d:" + File.separator + "fileSrc.txt");
        FileInputStream fileInputStream = new FileInputStream(fileSrc);
        FileChannel fileInputStreamChannel = fileInputStream.getChannel();

        File fileDest = new File("d:" + File.separator + "fileDest.txt");
        FileOutputStream fileOutputStream = new FileOutputStream(fileDest);
        FileChannel fileOutputStreamChannel = fileOutputStream.getChannel();

        // 虽然通道是双向的，但输入流的通道只能读取数据到缓冲区，输出流的通道把缓冲区数据写入通道
        // 也可以使用RandomAccessFile来创建FileChannel，设为读写模式
        FileChannel fileSrcChannel = new RandomAccessFile(fileSrc, "rw").getChannel();
        FileChannel fileDestChannel = new RandomAccessFile(fileDest, "rw").getChannel();

        // Channel直接把通道中全部数据映射成ByteBuffer，使用内存映射可以大幅度提高文件拷贝性能
        MappedByteBuffer mapBuff = fileInputStreamChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSrc.length());

        // 也可以使用Buffer的allocate()来自由分配缓冲区容量
        // 分批次拷贝文件，避免一次性写入大量数据到内存中
        ByteBuffer buf = ByteBuffer.allocate(1024);

        int temp = 0;
        // 读取管道数据到缓冲区中，-1则结束
        while ((temp = fileInputStreamChannel.read(buf)) != -1) {
            buf.flip();
            fileOutputStreamChannel.write(buf);
            buf.clear();
        }

        // 独占锁
        FileLock lock = fileOutputStreamChannel.tryLock();
        if (lock != null) {
            System.out.println(fileDest.getName() + "文件锁定3秒");
            Thread.sleep(3 * 1000);
            // 释放
            lock.release();
            System.out.println(fileDest.getName() + "文件解除锁定");
        }

        fileInputStreamChannel.close();
        fileOutputStreamChannel.close();
        fileInputStream.close();
        fileOutputStream.close();

    }

    private static void test_a() throws Exception {

        final int BUF_SIZE = 1024;
        final int TIMEOUT = 3000;

        // 打开ServerSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        // 打开Selector
        Selector selector = Selector.open();

        // 配置为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        // ServerSocketChannel监听8080端口
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));

        // 只有非阻塞的Channel才能注册到Selector，FileChannel不能用选择器，因为是阻塞的

        // 将ServerSocketChannel注册到Selector中，指定关注OP_ACCEPT事件
        // 通常先注册OP_ACCEPT事件，然后在OP_ACCEPT时，再将这个Channel的OP_READ事件注册到Selector中
        SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 一个Channel仅可以被注册到一个Selector一次，重复注册，相当于更新SelectionKey的interestSet
        // 在注册的时候加一个附件
        key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, new Object());

        while (true) {
            // select方法阻塞地等待Channel就绪，返回值表示有多少个Channel就绪
            if (selector.select(TIMEOUT) == 0) {
                System.out.print("...");
                continue;
            }

            // 有通道就绪了，然后可以访问已关注选择键集合中的就绪通道
            Set<SelectionKey> selectedKeys = selector.selectedKeys();

            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                // 每次迭代将SelectionKey删除，表示已处理
                keyIterator.remove();

                SelectionKey selectionKey = keyIterator.next();

                // 获取对应的Channel，需要转型
                Channel channel = selectionKey.channel();
                // 获取对应的Selector
                Selector _selector = selectionKey.selector();

                // 加一个附件
                selectionKey.attach(new Object());
                // 获取附件
                Object attachedObj = selectionKey.attachment();

                // 获取关注事件集
                int interestSet = selectionKey.interestOps();

                int isInterestedInAccept = interestSet & SelectionKey.OP_ACCEPT;
                int isInterestedInConnect = interestSet & SelectionKey.OP_CONNECT;
                int isInterestedInRead = interestSet & SelectionKey.OP_READ;
                int isInterestedInWrite = interestSet & SelectionKey.OP_WRITE;

                // 也可以通过这个来判断就绪事件
                int readySet = selectionKey.readyOps();

                if (selectionKey.isAcceptable()) {
                    // 创建SocketChannel有两种：
                    // 打开SocketChannel并连接到主机
                    // 一个新连接到达ServerSocketChannel时，会创建一个SocketChannel
                    SocketChannel clientChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
                    clientChannel.configureBlocking(false);
                    // 如果没有设置OP_READ，即interestSet仍是OP_CONNECT的话，select方法会直接返回
                    clientChannel.register(selectionKey.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(BUF_SIZE));
                }

                if (selectionKey.isReadable()) {
                    SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
                    ByteBuffer buf = (ByteBuffer) selectionKey.attachment();
                    // 返回的int值表示从通道读了多少字节进缓冲里
                    // 如果返回-1，表示已经读到了流的末尾（连接关闭了）
                    long bytesRead = clientChannel.read(buf);
                    if (bytesRead == -1) {
                        clientChannel.close();
                    } else if (bytesRead > 0) {

                        // 或运算组合多个事件 更新
                        selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        System.out.println("Get data length: " + bytesRead);
                    }
                }

                if (selectionKey.isValid() && selectionKey.isWritable()) {
                    ByteBuffer buf = (ByteBuffer) key.attachment();
                    // 切换到读模式
                    buf.flip();
                    SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
                    // 从缓冲写入通道

                    while (buf.hasRemaining()) {
                        clientChannel.write(buf);
                    }

                    // 如果没有剩余
                    selectionKey.interestOps(SelectionKey.OP_READ);

                    buf.compact();
                }

                // SelectionKey.OP_ACCEPT
                // 连接建立完成
                if (selectionKey.isAcceptable()) {
                    // a connection was accepted by a ServerSocketChannel.
                }

                // SelectionKey.OP_CONNECT
                // TCP三次握手完成
                else if (selectionKey.isConnectable()) {
                    // a connection was established with a remote server.
                }

                // SelectionKey.OP_READ
                // TCP接收缓冲区可读
                else if (selectionKey.isReadable()) {
                    // a channel is ready for reading
                }
                // SelectionKey.OP_WRITE
                // TCP发送缓冲区可写
                else if (selectionKey.isWritable()) {
                    // a channel is ready for writing
                }

            }

            serverSocketChannel.close();

        }

    }

    private static void test_b() throws Exception {

        ByteBuffer buf = ByteBuffer.allocate(1024);

        SocketChannel socketChannel = SocketChannel.open();
        // 设置非阻塞
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("http://8.8.8.8", 80));

        // 非阻塞情况下，或许连接还没有建立，connect方法就返回了，因此需要检查当前是否是连接到了主机
        while (!socketChannel.finishConnect()) {

        }

        // -1表示连接中断了
        int bytesRead = socketChannel.read(buf);

        buf.clear();

        // 写入数据

        buf.put(String.valueOf(System.currentTimeMillis()).getBytes());

        buf.flip();
        // buf.get();
        while (buf.hasRemaining()) {
            socketChannel.write(buf);
        }

        // 关闭
        socketChannel.close();
    }

    private static void test_d() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(1024);

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));
        while (true) {
            // accept()方法会阻塞，直到有连接到来，当有连接时，返回一个SocketChannel对象
            // 非阻塞模式下，accept()是非阻塞的，如果此时没有连接到来，返回
            SocketChannel socketChannel = serverSocketChannel.accept();

            if (socketChannel != null) {
                // 非阻塞模式下，write()方法在尚未写出任何内容时可能就返回了
                while (buf.hasRemaining()) {
                    socketChannel.write(buf);
                }
                // 非阻塞模式下，read()方法在尚未读取到任何数据时可能就返回了
                long bytesRead = socketChannel.read(buf);

            }

        }
    }

    private static void test_e() throws Exception {

        ByteBuffer buf = ByteBuffer.allocate(1024);

        //
        DatagramChannel clientChannel = DatagramChannel.open();
        clientChannel.connect(new InetSocketAddress("localhost", 80));
        int bytesRead = clientChannel.read(buf);
        int bytesWritten = clientChannel.write(buf);

        //
        DatagramChannel serverChannel = DatagramChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(80));
        // receive()方法从DatagramChannel接收数据
        serverChannel.receive(buf);
        // send()方法从DatagramChannel发送数据
        int bytesSent = serverChannel.send(buf, new InetSocketAddress("localhost", 80));

    }

}
