package com.wk.ti.assistant.repository;

import com.wk.ti.assistant.entity.DocumentResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface DocumentResultRepository extends JpaRepository<DocumentResultEntity, Long> {
}
