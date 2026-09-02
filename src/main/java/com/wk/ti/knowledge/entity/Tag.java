package com.wk.ti.knowledge.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;


@SuppressWarnings("JpaDataSourceORMInspection")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Immutable
@Entity
@Table(name = "tag", schema = "knowledge")
public class Tag {

    @Id
    private Long id;

    private String tag;

}
