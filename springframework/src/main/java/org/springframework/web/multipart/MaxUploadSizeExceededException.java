package org.springframework.web.multipart;

import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class MaxUploadSizeExceededException extends MultipartException {

    private final long maxUploadSize;

    public MaxUploadSizeExceededException(long maxUploadSize) {
        this(maxUploadSize, null);
    }

    public MaxUploadSizeExceededException(long maxUploadSize, @Nullable Throwable ex) {
        super("Maximum upload size " + (maxUploadSize >= 0 ? "of " + maxUploadSize + " bytes " : "") + "exceeded", ex);
        this.maxUploadSize = maxUploadSize;
    }

    public long getMaxUploadSize() {
        return this.maxUploadSize;
    }

}
