package com.wk.ti.document.repository;

import com.wk.ti.document.entity.DocumentSection;
import com.wk.ti.document.entity.SectionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionGenerationSectionRepository
        extends Repository<SectionEntity, Long> {

    @Query(value = """
            select
            qgd.id as documentId,
            qgd.filename,
            qgs.id as sectionId,
            qgs.section_number as sectionNumber,
            qgs.content,
            qgs.start_page_number as startPage,
            qgs.end_page_number as endPage
            from public.question_generation_section qgs
            inner join public.question_generation_document qgd on qgd.id = qgs.document_id
            where qgd.id = :documentId
            order by qgs.section_number
            """, nativeQuery = true)
    List<DocumentSection> findByDocumentId(@Param("documentId") Long documentId);
}
