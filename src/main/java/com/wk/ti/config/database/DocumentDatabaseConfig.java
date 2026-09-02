package com.wk.ti.config.database;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.wk.ti.document.repository",
        entityManagerFactoryRef = "documentEntityManagerFactory",
        transactionManagerRef = "documentTransactionManager"
)
public class DocumentDatabaseConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.document")
    public DataSourceProperties documentDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource documentDataSource(
            @Qualifier("documentDataSourceProperties")
            DataSourceProperties properties) {

        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean documentEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("documentDataSource")
            DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.wk.ti.document.entity")
                .persistenceUnit("document")
                .properties(Map.of(
                        "hibernate.default_schema", "public"
                ))
                .build();
    }

    @Bean
    public PlatformTransactionManager documentTransactionManager(
            @Qualifier("documentEntityManagerFactory")
            EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}