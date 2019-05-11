package mmp.nio;

// 字节顺序
public final class ByteOrder {

    private String name;

    private ByteOrder(String name) {
        this.name = name;
    }

    // 大端
    public static final ByteOrder BIG_ENDIAN = new ByteOrder("BIG_ENDIAN");

    // 小端
    public static final ByteOrder LITTLE_ENDIAN = new ByteOrder("LITTLE_ENDIAN");


    public static ByteOrder nativeOrder() {
        return Bits.byteOrder();
    }


    public String toString() {
        return name;
    }

}
