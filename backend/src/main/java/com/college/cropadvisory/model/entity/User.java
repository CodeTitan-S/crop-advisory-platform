package com.college.cropadvisory.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Relationships
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Farm> farms = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "farmer")
    private List<AdvisoryRequest> submittedRequests = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "officer")
    private List<AdvisoryRequest> assignedRequests = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "farmer")
    private List<DiseaseReport> submittedReports = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "officer")
    private List<DiseaseReport> reviewedReports = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "createdByAdmin")
    private List<KnowledgeBaseEntry> knowledgeBaseEntries = new ArrayList<>();
}