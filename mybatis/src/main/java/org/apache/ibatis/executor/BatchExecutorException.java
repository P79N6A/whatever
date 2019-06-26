package org.apache.ibatis.executor;

import java.sql.BatchUpdateException;
import java.util.List;

public class BatchExecutorException extends ExecutorException {

    private static final long serialVersionUID = 154049229650533990L;
    private final List<BatchResult> successfulBatchResults;
    private final BatchUpdateException batchUpdateException;
    private final BatchResult batchResult;

    public BatchExecutorException(String message, BatchUpdateException cause, List<BatchResult> successfulBatchResults, BatchResult batchResult) {
        super(message + " Cause: " + cause, cause);
        this.batchUpdateException = cause;
        this.successfulBatchResults = successfulBatchResults;
        this.batchResult = batchResult;
    }

    public BatchUpdateException getBatchUpdateException() {
        return batchUpdateException;
    }

    public List<BatchResult> getSuccessfulBatchResults() {
        return successfulBatchResults;
    }

    public String getFailingSqlStatement() {
        return batchResult.getSql();
    }

    public String getFailingStatementId() {
        return batchResult.getMappedStatement().getId();
    }
}
