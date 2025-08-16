package com.dfparty.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_name", nullable = false, unique = true)
    private String jobName;
    
    @Column(name = "job_grow_name")
    private String jobGrowName;
    
    @Column(name = "is_buffer", nullable = false)
    private Boolean isBuffer;
    
    @Column(name = "is_dealer", nullable = false)
    private Boolean isDealer;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
