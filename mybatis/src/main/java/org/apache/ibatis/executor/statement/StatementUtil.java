package org.apache.ibatis.executor.statement;

import java.sql.SQLException;
import java.sql.Statement;

public class StatementUtil {

    private StatementUtil() {

    }

    public static void applyTransactionTimeout(Statement statement, Integer queryTimeout, Integer transactionTimeout) throws SQLException {
        if (transactionTimeout == null) {
            return;
        }
        Integer timeToLiveOfQuery = null;
        if (queryTimeout == null || queryTimeout == 0) {
            timeToLiveOfQuery = transactionTimeout;
        } else if (transactionTimeout < queryTimeout) {
            timeToLiveOfQuery = transactionTimeout;
        }
        if (timeToLiveOfQuery != null) {
            statement.setQueryTimeout(timeToLiveOfQuery);
        }
    }

}
