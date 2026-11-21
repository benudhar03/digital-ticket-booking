package com.digital.booking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log =
            LoggerFactory.getLogger(RoutingDataSource.class);

    private final Map<DatabaseType, String> jdbcUrls;

    public RoutingDataSource(Map<DatabaseType, String> jdbcUrls) {
        this.jdbcUrls = jdbcUrls;
    }

    @Override
    protected Object determineCurrentLookupKey() {

        boolean readOnly =
                TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        DatabaseType databaseType =
                readOnly ? DatabaseType.REPLICA : DatabaseType.PRIMARY;
        log.info(
                "DATABASE ROUTING | Selected: {} | ReadOnly: {} | JDBC URL: {}",
                databaseType,
                readOnly,
                jdbcUrls.get(databaseType)
        );
        return databaseType;
    }
}