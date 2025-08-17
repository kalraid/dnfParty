package com.dfparty.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "nabel_difficulty_selections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NabelDifficultySelection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "character_id", nullable = false)
    private String characterId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "selected_difficulty", nullable = false)
    private NabelDifficulty selectedDifficulty;
    
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
    
    public enum NabelDifficulty {
        HARD, NORMAL, MATCHING
    }
}
