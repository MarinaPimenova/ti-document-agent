package com.wk.ti.config.database;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
        basePackages = "com.wk.ti.assistant.repository",
        entityManagerFactoryRef = "assistantEntityManagerFactory",
        transactionManagerRef = "assistantTransactionManager"
)
public class AssistantDatabaseConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.assistant")
    public DataSourceProperties assistantDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource assistantDataSource(
            @Qualifier("assistantDataSourceProperties")
            DataSourceProperties properties) {

        return properties
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean assistantEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("assistantDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.wk.ti.assistant.entity")
                .persistenceUnit("assistant")
                .properties(Map.of(
                        "hibernate.default_schema", "assistant"
                ))
                .build();
    }

    @Bean
    public PlatformTransactionManager assistantTransactionManager(
            @Qualifier("assistantEntityManagerFactory")
            EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}