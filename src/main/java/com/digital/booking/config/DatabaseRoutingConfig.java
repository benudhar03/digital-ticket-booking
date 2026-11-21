package com.digital.booking.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseRoutingConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public HikariDataSource primaryDataSource() {
        return new HikariDataSource();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.replica")
    public HikariDataSource replicaDataSource() {
        return new HikariDataSource();
    }

    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("replicaDataSource") DataSource replicaDataSource) {

        Map<DatabaseType, String> jdbcUrls = Map.of(
                DatabaseType.PRIMARY, ((HikariDataSource) primaryDataSource).getJdbcUrl(),
                DatabaseType.REPLICA, ((HikariDataSource) replicaDataSource).getJdbcUrl()
        );

        RoutingDataSource routingDataSource =
                new RoutingDataSource(jdbcUrls);

        Map<Object, Object> dataSources = new HashMap<>();

        dataSources.put(DatabaseType.PRIMARY, primaryDataSource);
        dataSources.put(DatabaseType.REPLICA, replicaDataSource);

        routingDataSource.setTargetDataSources(dataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);

        routingDataSource.afterPropertiesSet();

        // Delay physical connection acquisition until transaction metadata
        // is available, allowing correct read/write routing.
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}