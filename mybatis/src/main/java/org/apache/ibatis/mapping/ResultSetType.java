package org.apache.ibatis.mapping;

import java.sql.ResultSet;

public enum ResultSetType {

    DEFAULT(-1), FORWARD_ONLY(ResultSet.TYPE_FORWARD_ONLY), SCROLL_INSENSITIVE(ResultSet.TYPE_SCROLL_INSENSITIVE), SCROLL_SENSITIVE(ResultSet.TYPE_SCROLL_SENSITIVE);

    private final int value;

    ResultSetType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
