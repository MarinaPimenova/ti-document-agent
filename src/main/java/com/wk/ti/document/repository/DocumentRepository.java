package com.wk.ti.document.repository;

import com.wk.ti.document.entity.DocumentEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository
        extends JpaRepository<DocumentEntity, Long> {

    @NonNull List<DocumentEntity> findAll();
}
