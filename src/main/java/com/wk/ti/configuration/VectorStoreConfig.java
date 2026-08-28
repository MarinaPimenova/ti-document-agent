package com.wk.ti.configuration;

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
    public JdbcTemplate pgvectorJdbcTemplate(
            DataSource pgvectorDataSource) {
        return new JdbcTemplate(pgvectorDataSource);
    }

    @Bean
    public VectorStore vectorStore(
            @Qualifier("pgvectorJdbcTemplate") JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(pgvectorConfig.getDimensions())                    // Optional: defaults to model dimensions or 1536
                .distanceType(PgVectorStore.PgDistanceType.valueOf(pgvectorConfig.getDistanceType()))       // Optional: defaults to COSINE_DISTANCE
                .indexType(PgVectorStore.PgIndexType.valueOf(pgvectorConfig.getIndexType())/*HNSW*/)                     // Optional: defaults to HNSW
                .initializeSchema(pgvectorConfig.getInitializeSchema())              // Optional: defaults to false
                .schemaName(pgvectorConfig.getSchemaName())                // Optional: defaults to "public"
                .vectorTableName(pgvectorConfig.getTableName())     // Optional: defaults to "vector_store"
                .maxDocumentBatchSize(pgvectorConfig.getMaxDocumentBatchSize())         // Optional: defaults to 10000
                .build();
    }

}
