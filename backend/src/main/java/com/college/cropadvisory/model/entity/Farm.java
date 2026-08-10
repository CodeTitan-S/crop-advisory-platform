package com.college.cropadvisory.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "farms")
@Getter
@Setter
@NoArgsConstructor
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Double size; // in acres or hectares

    @Column(nullable = false)
    private String soilType;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SoilReading> soilReadings = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeasonLog> seasonLogs = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "farm")
    private List<AdvisoryRequest> advisoryRequests = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "farm")
    private List<DiseaseReport> diseaseReports = new ArrayList<>();
}