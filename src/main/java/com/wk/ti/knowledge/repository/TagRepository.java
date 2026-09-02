package com.wk.ti.knowledge.repository;

import com.wk.ti.knowledge.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@SuppressWarnings("SqlResolve")
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    @Query(value = """
            select *
            from knowledge.tag
            where tag in (:tags)
            """, nativeQuery = true)
    List<Tag> findByTagIn(@Param("tags") List<String> tags);
}
