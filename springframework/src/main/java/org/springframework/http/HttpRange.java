package org.springframework.http;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

public abstract class HttpRange {

    private static final int MAX_RANGES = 100;

    private static final String BYTE_RANGE_PREFIX = "bytes=";

    public ResourceRegion toResourceRegion(Resource resource) {
        // Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
        // Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
        Assert.isTrue(resource.getClass() != InputStreamResource.class, "Cannot convert an InputStreamResource to a ResourceRegion");
        long contentLength = getLengthFor(resource);
        long start = getRangeStart(contentLength);
        long end = getRangeEnd(contentLength);
        return new ResourceRegion(resource, start, end - start + 1);
    }

    public abstract long getRangeStart(long length);

    public abstract long getRangeEnd(long length);

    public static HttpRange createByteRange(long firstBytePos) {
        return new ByteRange(firstBytePos, null);
    }

    public static HttpRange createByteRange(long firstBytePos, long lastBytePos) {
        return new ByteRange(firstBytePos, lastBytePos);
    }

    public static HttpRange createSuffixRange(long suffixLength) {
        return new SuffixByteRange(suffixLength);
    }

    public static List<HttpRange> parseRanges(@Nullable String ranges) {
        if (!StringUtils.hasLength(ranges)) {
            return Collections.emptyList();
        }
        if (!ranges.startsWith(BYTE_RANGE_PREFIX)) {
            throw new IllegalArgumentException("Range '" + ranges + "' does not start with 'bytes='");
        }
        ranges = ranges.substring(BYTE_RANGE_PREFIX.length());
        String[] tokens = StringUtils.tokenizeToStringArray(ranges, ",");
        if (tokens.length > MAX_RANGES) {
            throw new IllegalArgumentException("Too many ranges: " + tokens.length);
        }
        List<HttpRange> result = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            result.add(parseRange(token));
        }
        return result;
    }

    private static HttpRange parseRange(String range) {
        Assert.hasLength(range, "Range String must not be empty");
        int dashIdx = range.indexOf('-');
        if (dashIdx > 0) {
            long firstPos = Long.parseLong(range.substring(0, dashIdx));
            if (dashIdx < range.length() - 1) {
                Long lastPos = Long.parseLong(range.substring(dashIdx + 1));
                return new ByteRange(firstPos, lastPos);
            } else {
                return new ByteRange(firstPos, null);
            }
        } else if (dashIdx == 0) {
            long suffixLength = Long.parseLong(range.substring(1));
            return new SuffixByteRange(suffixLength);
        } else {
            throw new IllegalArgumentException("Range '" + range + "' does not contain \"-\"");
        }
    }

    public static List<ResourceRegion> toResourceRegions(List<HttpRange> ranges, Resource resource) {
        if (CollectionUtils.isEmpty(ranges)) {
            return Collections.emptyList();
        }
        List<ResourceRegion> regions = new ArrayList<>(ranges.size());
        for (HttpRange range : ranges) {
            regions.add(range.toResourceRegion(resource));
        }
        if (ranges.size() > 1) {
            long length = getLengthFor(resource);
            long total = 0;
            for (ResourceRegion region : regions) {
                total += region.getCount();
            }
            if (total >= length) {
                throw new IllegalArgumentException("The sum of all ranges (" + total + ") should be less than the resource length (" + length + ")");
            }
        }
        return regions;
    }

    private static long getLengthFor(Resource resource) {
        try {
            long contentLength = resource.contentLength();
            Assert.isTrue(contentLength > 0, "Resource content length should be > 0");
            return contentLength;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to obtain Resource content length", ex);
        }
    }

    public static String toString(Collection<HttpRange> ranges) {
        Assert.notEmpty(ranges, "Ranges Collection must not be empty");
        StringJoiner builder = new StringJoiner(", ", BYTE_RANGE_PREFIX, "");
        for (HttpRange range : ranges) {
            builder.add(range.toString());
        }
        return builder.toString();
    }

    private static class ByteRange extends HttpRange {

        private final long firstPos;

        @Nullable
        private final Long lastPos;

        public ByteRange(long firstPos, @Nullable Long lastPos) {
            assertPositions(firstPos, lastPos);
            this.firstPos = firstPos;
            this.lastPos = lastPos;
        }

        private void assertPositions(long firstBytePos, @Nullable Long lastBytePos) {
            if (firstBytePos < 0) {
                throw new IllegalArgumentException("Invalid first byte position: " + firstBytePos);
            }
            if (lastBytePos != null && lastBytePos < firstBytePos) {
                throw new IllegalArgumentException("firstBytePosition=" + firstBytePos + " should be less then or equal to lastBytePosition=" + lastBytePos);
            }
        }

        @Override
        public long getRangeStart(long length) {
            return this.firstPos;
        }

        @Override
        public long getRangeEnd(long length) {
            if (this.lastPos != null && this.lastPos < length) {
                return this.lastPos;
            } else {
                return length - 1;
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ByteRange)) {
                return false;
            }
            ByteRange otherRange = (ByteRange) other;
            return (this.firstPos == otherRange.firstPos && ObjectUtils.nullSafeEquals(this.lastPos, otherRange.lastPos));
        }

        @Override
        public int hashCode() {
            return (ObjectUtils.nullSafeHashCode(this.firstPos) * 31 + ObjectUtils.nullSafeHashCode(this.lastPos));
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(this.firstPos);
            builder.append('-');
            if (this.lastPos != null) {
                builder.append(this.lastPos);
            }
            return builder.toString();
        }

    }

    private static class SuffixByteRange extends HttpRange {

        private final long suffixLength;

        public SuffixByteRange(long suffixLength) {
            if (suffixLength < 0) {
                throw new IllegalArgumentException("Invalid suffix length: " + suffixLength);
            }
            this.suffixLength = suffixLength;
        }

        @Override
        public long getRangeStart(long length) {
            if (this.suffixLength < length) {
                return length - this.suffixLength;
            } else {
                return 0;
            }
        }

        @Override
        public long getRangeEnd(long length) {
            return length - 1;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SuffixByteRange)) {
                return false;
            }
            SuffixByteRange otherRange = (SuffixByteRange) other;
            return (this.suffixLength == otherRange.suffixLength);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(this.suffixLength);
        }

        @Override
        public String toString() {
            return "-" + this.suffixLength;
        }

    }

}
