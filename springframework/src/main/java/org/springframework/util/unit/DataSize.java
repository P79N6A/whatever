package org.springframework.util.unit;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DataSize implements Comparable<DataSize> {

    private static final Pattern PATTERN = Pattern.compile("^([+\\-]?\\d+)([a-zA-Z]{0,2})$");

    private static long BYTES_PER_KB = 1024;

    private static long BYTES_PER_MB = BYTES_PER_KB * 1024;

    private static long BYTES_PER_GB = BYTES_PER_MB * 1024;

    private static long BYTES_PER_TB = BYTES_PER_GB * 1024;

    private final long bytes;

    private DataSize(long bytes) {
        this.bytes = bytes;
    }

    public static DataSize ofBytes(long bytes) {
        return new DataSize(bytes);
    }

    public static DataSize ofKilobytes(long kilobytes) {
        return new DataSize(Math.multiplyExact(kilobytes, BYTES_PER_KB));
    }

    public static DataSize ofMegabytes(long megabytes) {
        return new DataSize(Math.multiplyExact(megabytes, BYTES_PER_MB));
    }

    public static DataSize ofGigabytes(long gigabytes) {
        return new DataSize(Math.multiplyExact(gigabytes, BYTES_PER_GB));
    }

    public static DataSize ofTerabytes(long terabytes) {
        return new DataSize(Math.multiplyExact(terabytes, BYTES_PER_TB));
    }

    public static DataSize of(long amount, DataUnit unit) {
        Assert.notNull(unit, "Unit must not be null");
        return new DataSize(Math.multiplyExact(amount, unit.size().toBytes()));
    }

    public static DataSize parse(CharSequence text) {
        return parse(text, null);
    }

    public static DataSize parse(CharSequence text, @Nullable DataUnit defaultUnit) {
        Assert.notNull(text, "Text must not be null");
        try {
            Matcher matcher = PATTERN.matcher(text);
            Assert.state(matcher.matches(), "Does not match data size pattern");
            DataUnit unit = determineDataUnit(matcher.group(2), defaultUnit);
            long amount = Long.parseLong(matcher.group(1));
            return DataSize.of(amount, unit);
        } catch (Exception ex) {
            throw new IllegalArgumentException("'" + text + "' is not a valid data size", ex);
        }
    }

    private static DataUnit determineDataUnit(String suffix, @Nullable DataUnit defaultUnit) {
        DataUnit defaultUnitToUse = (defaultUnit != null ? defaultUnit : DataUnit.BYTES);
        return (StringUtils.hasLength(suffix) ? DataUnit.fromSuffix(suffix) : defaultUnitToUse);
    }

    public boolean isNegative() {
        return this.bytes < 0;
    }

    public long toBytes() {
        return this.bytes;
    }

    public long toKilobytes() {
        return this.bytes / BYTES_PER_KB;
    }

    public long toMegabytes() {
        return this.bytes / BYTES_PER_MB;
    }

    public long toGigabytes() {
        return this.bytes / BYTES_PER_GB;
    }

    public long toTerabytes() {
        return this.bytes / BYTES_PER_TB;
    }

    @Override
    public int compareTo(DataSize other) {
        return Long.compare(this.bytes, other.bytes);
    }

    @Override
    public String toString() {
        return String.format("%dB", this.bytes);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        DataSize otherSize = (DataSize) other;
        return (this.bytes == otherSize.bytes);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.bytes);
    }

}
