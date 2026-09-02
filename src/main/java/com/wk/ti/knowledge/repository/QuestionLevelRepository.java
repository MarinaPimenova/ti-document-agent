package com.wk.ti.knowledge.repository;

import com.wk.ti.knowledge.entity.QuestionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@SuppressWarnings("SqlResolve")
@Repository
public interface QuestionLevelRepository extends JpaRepository<QuestionLevel, Long> {
    @Query(value = """
            select * from knowledge.question_level where code = :codeLevel
            """, nativeQuery = true)
    Optional<QuestionLevel> findByCode(@Param("codeLevel") String codeLevel);
}
