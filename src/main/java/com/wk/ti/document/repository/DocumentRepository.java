package com.wk.ti.document.repository;

import com.wk.ti.document.entity.DocumentEntity;
import com.wk.ti.document.entity.DocumentProjection;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository
        extends JpaRepository<DocumentEntity, Long> {

    @NonNull List<DocumentEntity> findAll();

    @Query(value = """
            select
                id,
                filename
            from public.question_generation_document qgd
            """, nativeQuery = true)
    List<DocumentProjection> findDocumentProjection();
}
