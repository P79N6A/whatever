package org.springframework.validation;

import org.springframework.lang.Nullable;

public interface MessageCodeFormatter {

    String format(String errorCode, @Nullable String objectName, @Nullable String field);

}
