package com.wk.ti.assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.io.Serializable;
import java.time.Instant;

@Data
@MappedSuperclass
public abstract class BaseEntity implements Serializable {
    @JsonIgnore
    @CreatedDate
    @Column(name = "created_date", columnDefinition = "TIMESTAMP")
    protected Instant createdDate;

    @JsonIgnore
    @Column(name = "created_by", columnDefinition = "VARCHAR(256) DEFAULT='service-account'")
    @CreatedBy
    protected String createdBy;

    @PrePersist
    public void prePersist() {
        if (createdDate == null) {
            createdDate = Instant.now();
        }
        if (createdBy == null) {
            createdBy = "npa";
        }
    }
}
