package com.wk.ti.rag.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {

    private final PgvectorConfig pgvectorConfig;

    @Bean
    @ConfigurationProperties("datasource.pgvector")
    public DataSource pgvectorDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate pgvectorJdbcTemplate(@Qualifier("pgvectorDataSource") DataSource pgvectorDataSource) {
        return new JdbcTemplate(pgvectorDataSource);
    }

    @Bean
    public VectorStore documentVectorStore(
            @Qualifier("pgvectorJdbcTemplate") JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(pgvectorConfig.getDimensions())
                .distanceType(PgVectorStore.PgDistanceType.valueOf(pgvectorConfig.getDistanceType()))
                .indexType(PgVectorStore.PgIndexType.valueOf(pgvectorConfig.getIndexType()))
                .initializeSchema(pgvectorConfig.getInitializeSchema() != null ? pgvectorConfig.getInitializeSchema() : false)
                .schemaName(pgvectorConfig.getSchemaName() != null ? pgvectorConfig.getSchemaName() : "public")
                .vectorTableName(pgvectorConfig.getTableName() != null ? pgvectorConfig.getTableName() : "vector_store")
                .maxDocumentBatchSize(pgvectorConfig.getMaxDocumentBatchSize() != null ? pgvectorConfig.getMaxDocumentBatchSize() : 10000)
                .build();
    }
}