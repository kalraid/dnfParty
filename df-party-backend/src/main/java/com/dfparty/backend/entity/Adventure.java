package com.dfparty.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "adventures")
public class Adventure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "adventure_name", unique = true, nullable = false)
    private String adventureName;

    @Column(name = "server_id", nullable = false)
    private String serverId;

    @Column(name = "character_count")
    private Integer characterCount = 0;

    @Column(name = "total_fame")
    private Long totalFame = 0L;

    @Column(name = "average_level")
    private Double averageLevel = 0.0;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 생성자
    public Adventure() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Adventure(String adventureName, String serverId) {
        this();
        this.adventureName = adventureName;
        this.serverId = serverId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAdventureName() { return adventureName; }
    public void setAdventureName(String adventureName) { this.adventureName = adventureName; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public Integer getCharacterCount() { return characterCount; }
    public void setCharacterCount(Integer characterCount) { this.characterCount = characterCount; }

    public Long getTotalFame() { return totalFame; }
    public void setTotalFame(Long totalFame) { this.totalFame = totalFame; }

    public Double getAverageLevel() { return averageLevel; }
    public void setAverageLevel(Double averageLevel) { this.averageLevel = averageLevel; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // 업데이트 시간 자동 설정
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 모험단 통계 업데이트
    public void updateStats(Integer characterCount, Long totalFame, Double averageLevel) {
        this.characterCount = characterCount;
        this.totalFame = totalFame;
        this.averageLevel = averageLevel;
        this.lastActivity = LocalDateTime.now();
    }
}
