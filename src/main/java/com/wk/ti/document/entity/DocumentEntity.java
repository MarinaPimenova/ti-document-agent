package com.wk.ti.document.entity;

import com.wk.ti.question.generation.model.DocumentExtension;
import com.wk.ti.question.generation.model.DocumentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "question_generation_document", schema = "public")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Immutable
public class DocumentEntity {
    @Id
    private Long id;

    private String filename;

    @Column(name = "file_extension")
    private DocumentExtension fileExtension;

    @Column(name = "file_size")
    private Long fileSize;

    private DocumentStatus status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
