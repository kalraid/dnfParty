package com.dfparty.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "shared_parties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedParty {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "share_code", nullable = false, unique = true)
    private String shareCode; // 공유 코드 (6자리)
    
    @Column(name = "title", nullable = false)
    private String title; // 파티 제목
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 파티 설명
    
    @Column(name = "dungeon_name", nullable = false)
    private String dungeonName; // 던전 이름
    
    @Column(name = "party_size", nullable = false)
    private Integer partySize; // 파티 크기 (4, 8)
    
    @Column(name = "party_data", columnDefinition = "TEXT")
    private String partyData; // 파티 구성 데이터 (JSON)
    
    @Column(name = "creator_name")
    private String creatorName; // 생성자 이름
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // 만료 시간 (기본 7일)
    
    @Column(name = "view_count", nullable = false)
    private Integer viewCount; // 조회 횟수
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // 태그 (JSON 배열)
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusDays(7); // 기본 7일
        viewCount = 0;
        isActive = true;
    }
    
    @PreUpdate
    protected void onUpdate() {
        viewCount++;
    }
}
