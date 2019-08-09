package org.springframework.beans.factory.parsing;

import org.springframework.util.StringUtils;

public class QualifierEntry implements ParseState.Entry {

    private String typeName;

    public QualifierEntry(String typeName) {
        if (!StringUtils.hasText(typeName)) {
            throw new IllegalArgumentException("Invalid qualifier type '" + typeName + "'.");
        }
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return "Qualifier '" + this.typeName + "'";
    }

}
