package com.wk.ti.document.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "question_generation_section", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Immutable
public class SectionEntity {

    @Id
    private Long id;

    @Column(name ="document_id")
    private Long documentId;

    @Column(name ="section_number")
    private Integer sectionNumber;

    private String title;

    private String content;

    @Column(name ="start_page_number")
    private Integer startPageNumber;

    @Column(name ="end_page_number")
    private Integer endPageNumber;

    @Column(name ="token_count")
    private Integer tokenCount;

    @Column(name ="created_at")
    private OffsetDateTime createdAt;

}
