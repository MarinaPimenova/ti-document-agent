package com.wk.ti.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.ai.vectorstore.pgvector")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PgvectorConfig {
    private Integer dimensions;
    private String distanceType;
    private String indexType;
    private Boolean initializeSchema;
    private String schemaName;
    private String tableName;
    private Integer maxDocumentBatchSize;
}
