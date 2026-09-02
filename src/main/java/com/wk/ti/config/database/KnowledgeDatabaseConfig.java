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

@Configuration
@EnableJpaRepositories(
        basePackages = "com.wk.ti.knowledge.repository",
        entityManagerFactoryRef = "knowledgeEntityManagerFactory",
        transactionManagerRef = "knowledgeTransactionManager"
)
public class KnowledgeDatabaseConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.knowledge")
    public DataSourceProperties knowledgeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource knowledgeDataSource(
            @Qualifier("knowledgeDataSourceProperties")
            DataSourceProperties properties) {

        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean knowledgeEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("knowledgeDataSource")
            DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.wk.ti.knowledge.entity")
                .persistenceUnit("knowledge")
                .properties(Map.of(
                        "hibernate.default_schema", "knowledge"
                ))
                .build();
    }

    @Bean
    public PlatformTransactionManager knowledgeTransactionManager(
            @Qualifier("knowledgeEntityManagerFactory")
            EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}