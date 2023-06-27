package bid.yuanlu.ifr_controller;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.IntStream;

public class DataPack {
    /**
     * 每个数据域的bit数
     */
    private static final int[] BITS = new int[]{2, 1, 1, 1, 3, 11, 11, 11, 11,};
    /**
     * 每个数据域的偏移bit数
     */
    private static final int[] OFFSETS = new int[BITS.length];
    /**
     * 总共的数据长度
     */
    private static final int DATA_LENGTH;

    static {
        int count = Arrays.stream(BITS).sum();
        DATA_LENGTH = ((count % 8 == 0) ? (count / 8) : (count / 8 + 1));
        IntStream.range(0, BITS.length).forEach(i -> OFFSETS[i] = Arrays.stream(BITS, 0, i).sum());
    }

    private final FieldInfo[] fieldInfos = new FieldInfo[BITS.length];
    private final byte[] bytes = new byte[DATA_LENGTH];

    public DataPack() {
        IntStream.range(0, BITS.length).forEach(i -> fieldInfos[i] = new FieldInfo(OFFSETS[i], BITS[i]));
    }

    @NonNull
    private static String toBinaryStr(byte b) {
        String str = Integer.toBinaryString(Byte.toUnsignedInt(b));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8 - str.length(); i++) sb.append('0');
        sb.append(str);
        return sb.toString();
    }

    /**
     * 设置一个通道数据
     *
     * @param id 通道ID
     * @param x  通道值[-1 ~ 1]
     */
    public void setCH(int id, float x) {
        synchronized (bytes) {
            fieldInfos[id].set((int) (x * 660 + 1024));
        }
    }

    /**
     * 设置两个个通道数据
     *
     * @param id 通道ID (id及id+1)
     * @param x  通道值[-1 ~ 1]
     */
    public void setCH(int id, float x, float y) {
        synchronized (bytes) {
            fieldInfos[id].set((int) (x * 660 + 1024));
            fieldInfos[id + 1].set((int) (y * 660 + 1024));
        }
    }

    /**
     * 设置一个拨杆数据
     *
     * @param id 拨杆ID
     * @param x  拨杆值[1, 2, 3]
     */
    public void setSW(int id, int x) {
        synchronized (bytes) {
            fieldInfos[id].set(x);
        }
    }

    /**
     * 设置一个按钮数据
     *
     * @param id 拨杆ID
     * @param x  拨杆值[1, 2, 3]
     */
    public void setBTN(int id, boolean x) {
        synchronized (bytes) {
            fieldInfos[id].set(x ? 1 : 0);
        }
    }

    /**
     * 设置一个原始值
     */
    public void setRAW(int id, int select) {
        synchronized (bytes) {
            fieldInfos[id].set(select);
        }
    }

    String getDataString() {
        synchronized (bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    byte[] getData(byte[] bs) {
        if (bs == null || bs.length != bytes.length) bs = new byte[bytes.length];
        synchronized (bytes) {
            System.arraycopy(bytes, 0, bs, 0, bytes.length);
        }
        return bs;
    }

    @SuppressLint("DefaultLocale")
    public String getFieldInfoString() {
        StringJoiner sj = new StringJoiner("\n");
        for (int i = 0; i < fieldInfos.length; i++)
            sj.add(String.format("%d: %s", i, fieldInfos[i]));
        return sj.toString();
    }

    private class FieldInfo {
        private final Part[] parts;

        FieldInfo(int offset, int bits) {
            ArrayList<Part> partArrayList = new ArrayList<>();
            int off = 0;
            while (bits > 0) {
                int index = (offset + off) / 8;
                int start = (offset + off) % 8;
                int length = Math.min(bits, 8 - start);
                int end = start + length - 1;
                partArrayList.add(new Part(index, start, end, off));
                off += length;
                bits -= length;
            }
            parts = partArrayList.toArray(new Part[0]);
        }

        public void set(int v) {
            for (final Part part : parts) part.set(v);
        }

        @NonNull
        @Override
        public String toString() {
            return Arrays.toString(parts);
        }

        private class Part {
            /**
             * 第几个字节位
             */
            private final int index;
            /**
             * 字节位上的偏移量
             */
            private final int offset;
            /**
             * 设为空的掩码
             */
            private final byte blank_mask;
            /**
             * 值掩码
             */
            private final byte value_mask;
            /**
             * 值偏移量
             */
            private final int value_offset;

            /**
             * 初始化
             *
             * @param index  属于第几个字节位
             * @param start  字节位上开始bit(含)
             * @param end    字节位上结束bit(含)
             * @param offset 值上开始bit(含)
             */
            Part(int index, int start, int end, int offset) {
                this.index = index;
                this.offset = start;
                this.value_offset = offset;
                byte bm = 0, vm = 0;
                for (int i = 0; i < 8; i++) if (i < start || i > end) bm |= 1 << i;//要赋值位置为0，其他位置为1
                for (int i = 0; i < end - start + 1; i++) vm |= 1 << i;
                this.blank_mask = bm;
                this.value_mask = vm;
            }

            @NonNull
            @SuppressLint("DefaultLocale")
            public String toString() {
                return String.format("[%d,%d,%s,%s,%d]", index, offset,
                        toBinaryStr(blank_mask),
                        toBinaryStr(value_mask),
                        value_offset);
            }

            void set(int x) {
                bytes[index] &= blank_mask;
                byte v = (byte) ((((byte) ((x >>> value_offset) & value_mask))) << offset);
                bytes[index] |= v;
            }
        }
    }
}
