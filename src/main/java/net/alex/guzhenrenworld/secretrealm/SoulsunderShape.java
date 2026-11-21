package net.alex.guzhenrenworld.secretrealm;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * 荡魂山[Soulsunder Mountain]的山形：160×160 列、每列若干段相对山底的实心区间 [y0, y1]（列跨度），只读、全局一份。
 * <ul>
 *   <li>数据来自 {@link #RESOURCE}，由主仓库 {@code tools/soulsunder_shape.py} 生成（格式与生成方法见该脚本文档）；改山
 *       形只重跑脚本，不手改文件。格式 1：魔数 {@code GZSQ}、版本、宽（x）、深（z）、高（y），随后按 z 优先逐列写段数与
 *       各段的 {@code (y0, y1)}，大端序。</li>
 *   <li>{@link #get} 在首次调用时从类路径读一次并常驻（约 65 KB），世界生成线程并发读取；文件不随数据包重载，因为放置只
 *       依赖 jar 内这一份。</li>
 *   <li>列序号 {@code z * width + x}；第 {@link #getCentre} 列对准山心方块，山底为 y = 0。</li>
 *   <li>⚠ {@link #read} 拒收段倒序、相接或越过高度的文件：结构片按段写方块且不再检查，坏数据会写出浮空块或越界。</li>
 * </ul>
 */

public final class SoulsunderShape {

    public static final String RESOURCE = "/data/guworld/secret_realm/soulsunder_mountain.spans";
    private static final int MAGIC = 0x475A5351;
    private static final int VERSION = 1;

    private final int width;
    private final int depth;
    private final int height;
    private final int[] firstSpan;
    private final byte[] spans;
    private final int volume;

    private SoulsunderShape(int width, int depth, int height, int[] firstSpan, byte[] spans) {
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.firstSpan = firstSpan;
        this.spans = spans;
        int blocks = 0;
        for (int i = 0; i < spans.length; i += 2) blocks += (spans[i + 1] & 0xFF) - (spans[i] & 0xFF) + 1;
        this.volume = blocks;
    }

    public static SoulsunderShape get() { return Holder.SHAPE; }

    public static SoulsunderShape read(InputStream input) throws IOException {
        DataInputStream data = new DataInputStream(new BufferedInputStream(input));
        if (data.readInt() != MAGIC) throw new IOException("not a soulsunder shape file");
        int version = data.readUnsignedByte();
        if (version != VERSION) throw new IOException("shape format " + version + ", expected " + VERSION);
        int width = data.readUnsignedShort();
        int depth = data.readUnsignedShort();
        int height = data.readUnsignedShort();
        int[] firstSpan = new int[width * depth + 1];
        ByteArrayOutputStream spans = new ByteArrayOutputStream();
        for (int column = 0; column < width * depth; column++) {
            firstSpan[column] = spans.size() / 2;
            int count = data.readUnsignedByte();
            int last = -2;
            for (int i = 0; i < count; i++) {
                int bottom = data.readUnsignedByte();
                int top = data.readUnsignedByte();
                if (bottom > top || bottom <= last + 1 || top >= height) {
                    throw new IOException("bad span (" + bottom + ", " + top + ") in column " + column);
                }
                spans.write(bottom);
                spans.write(top);
                last = top;
            }
        }
        firstSpan[width * depth] = spans.size() / 2;
        if (data.read() >= 0) throw new IOException("bytes trail the column table");
        return new SoulsunderShape(width, depth, height, firstSpan, spans.toByteArray());
    }

    public int getWidth() { return width; }

    public int getDepth() { return depth; }

    public int getHeight() { return height; }

    public int getCentre() { return width / 2; }

    public int getVolume() { return volume; }

    public int getSpanCount(int column) { return firstSpan[column + 1] - firstSpan[column]; }

    public int getBottom(int column, int span) { return spans[2 * (firstSpan[column] + span)] & 0xFF; }

    public int getTop(int column, int span) { return spans[2 * (firstSpan[column] + span) + 1] & 0xFF; }

    private static final class Holder {

        private static final SoulsunderShape SHAPE = load();

        private static SoulsunderShape load() {
            try (InputStream input = SoulsunderShape.class.getResourceAsStream(RESOURCE)) {
                if (input == null) throw new EOFException("missing " + RESOURCE);
                return read(input);
            } catch (IOException exception) {
                throw new UncheckedIOException("cannot read " + RESOURCE, exception);
            }
        }
    }
}
