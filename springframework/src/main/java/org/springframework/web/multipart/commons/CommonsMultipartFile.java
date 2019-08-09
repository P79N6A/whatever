package org.springframework.web.multipart.commons;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("serial")
public class CommonsMultipartFile implements MultipartFile, Serializable {

    protected static final Log logger = LogFactory.getLog(CommonsMultipartFile.class);

    private final FileItem fileItem;

    private final long size;

    private boolean preserveFilename = false;

    public CommonsMultipartFile(FileItem fileItem) {
        this.fileItem = fileItem;
        this.size = this.fileItem.getSize();
    }

    public final FileItem getFileItem() {
        return this.fileItem;
    }

    public void setPreserveFilename(boolean preserveFilename) {
        this.preserveFilename = preserveFilename;
    }

    @Override
    public String getName() {
        return this.fileItem.getFieldName();
    }

    @Override
    public String getOriginalFilename() {
        String filename = this.fileItem.getName();
        if (filename == null) {
            // Should never happen.
            return "";
        }
        if (this.preserveFilename) {
            // Do not try to strip off a path...
            return filename;
        }
        // Check for Unix-style path
        int unixSep = filename.lastIndexOf('/');
        // Check for Windows-style path
        int winSep = filename.lastIndexOf('\\');
        // Cut off at latest possible point
        int pos = Math.max(winSep, unixSep);
        if (pos != -1) {
            // Any sort of path separator found...
            return filename.substring(pos + 1);
        } else {
            // A plain name
            return filename;
        }
    }

    @Override
    public String getContentType() {
        return this.fileItem.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return (this.size == 0);
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public byte[] getBytes() {
        if (!isAvailable()) {
            throw new IllegalStateException("File has been moved - cannot be read again");
        }
        byte[] bytes = this.fileItem.get();
        return (bytes != null ? bytes : new byte[0]);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("File has been moved - cannot be read again");
        }
        InputStream inputStream = this.fileItem.getInputStream();
        return (inputStream != null ? inputStream : StreamUtils.emptyInput());
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        if (!isAvailable()) {
            throw new IllegalStateException("File has already been moved - cannot be transferred again");
        }
        if (dest.exists() && !dest.delete()) {
            throw new IOException("Destination file [" + dest.getAbsolutePath() + "] already exists and could not be deleted");
        }
        try {
            this.fileItem.write(dest);
            LogFormatUtils.traceDebug(logger, traceOn -> {
                String action = "transferred";
                if (!this.fileItem.isInMemory()) {
                    action = (isAvailable() ? "copied" : "moved");
                }
                return "Part '" + getName() + "',  filename '" + getOriginalFilename() + "'" + (traceOn ? ", stored " + getStorageDescription() : "") + ": " + action + " to [" + dest.getAbsolutePath() + "]";
            });
        } catch (FileUploadException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        } catch (IllegalStateException | IOException ex) {
            // Pass through IllegalStateException when coming from FileItem directly,
            // or propagate an exception from I/O operations within FileItem.write
            throw ex;
        } catch (Exception ex) {
            throw new IOException("File transfer failed", ex);
        }
    }

    @Override
    public void transferTo(Path dest) throws IOException, IllegalStateException {
        if (!isAvailable()) {
            throw new IllegalStateException("File has already been moved - cannot be transferred again");
        }
        FileCopyUtils.copy(this.fileItem.getInputStream(), Files.newOutputStream(dest));
    }

    protected boolean isAvailable() {
        // If in memory, it's available.
        if (this.fileItem.isInMemory()) {
            return true;
        }
        // Check actual existence of temporary file.
        if (this.fileItem instanceof DiskFileItem) {
            return ((DiskFileItem) this.fileItem).getStoreLocation().exists();
        }
        // Check whether current file size is different than original one.
        return (this.fileItem.getSize() == this.size);
    }

    public String getStorageDescription() {
        if (this.fileItem.isInMemory()) {
            return "in memory";
        } else if (this.fileItem instanceof DiskFileItem) {
            return "at [" + ((DiskFileItem) this.fileItem).getStoreLocation().getAbsolutePath() + "]";
        } else {
            return "on disk";
        }
    }

}
