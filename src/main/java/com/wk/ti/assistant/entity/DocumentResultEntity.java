package com.wk.ti.assistant.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document_result", schema = "assistant")
public class DocumentResultEntity extends AgentGeneralEntity {
    @Id
    @GeneratedValue(generator = "document_result_id_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "document_result_id_seq", sequenceName = "document_result_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "sql_text")
    private String sqlText;

}
