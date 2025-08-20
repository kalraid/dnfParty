package com.dfparty.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "adventures", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"adventure_name", "server_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adventure {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "adventure_name", nullable = false)
    private String adventureName;
    
    @Column(name = "server_id", nullable = false)
    private String serverId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 양방향 관계 설정 (선택사항)
    @OneToMany(mappedBy = "adventure", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Character> characters;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 기본 생성자만 유지
}
