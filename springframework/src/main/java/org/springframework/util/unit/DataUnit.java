package org.springframework.util.unit;

import java.util.Objects;

public enum DataUnit {

    BYTES("B", DataSize.ofBytes(1)),

    KILOBYTES("KB", DataSize.ofKilobytes(1)),

    MEGABYTES("MB", DataSize.ofMegabytes(1)),

    GIGABYTES("GB", DataSize.ofGigabytes(1)),

    TERABYTES("TB", DataSize.ofTerabytes(1));

    private final String suffix;

    private final DataSize size;

    DataUnit(String suffix, DataSize size) {
        this.suffix = suffix;
        this.size = size;
    }

    DataSize size() {
        return this.size;
    }

    public static DataUnit fromSuffix(String suffix) {
        for (DataUnit candidate : values()) {
            if (Objects.equals(candidate.suffix, suffix)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown unit '" + suffix + "'");
    }

}
