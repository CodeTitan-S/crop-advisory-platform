package com.college.cropadvisory.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "season_logs")
@Getter
@Setter
@NoArgsConstructor
public class SeasonLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cropPlanted;

    @Column(nullable = false)
    private LocalDate sowingDate;

    @Column(columnDefinition = "TEXT")
    private String outcomeNotes;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /**
     * Exposes just the farm id. The {@code farm} association itself is {@code @JsonIgnore}d,
     * so clients would otherwise have no way to reference the farm a log belongs to.
     */
    @JsonProperty("farmId")
    public Long getFarmId() {
        return farm != null ? farm.getId() : null;
    }
}
