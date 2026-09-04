package dev.agiro.fanel.shared.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@MappedSuperclass
public abstract class UuidEntity {
    @Id
    @GeneratedValue
    @Column(length = 36, nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @PrePersist
    void assignId() {
        if (id == null) id = UuidCreator.getTimeOrderedEpoch();
    }

    public UUID getId() { return id; }
}
