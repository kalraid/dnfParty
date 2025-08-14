package com.dfparty.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "mock_api_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockApiData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "api_type", nullable = false, columnDefinition = "varchar(255)")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private ApiType apiType;
    
    @Column(name = "endpoint", nullable = false)
    private String endpoint;
    
    @Column(name = "request_method", nullable = false)
    private String requestMethod;
    
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams; // JSON 형태로 저장
    
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody; // JSON 형태로 저장
    
    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody; // JSON 형태로 저장
    
    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders; // JSON 형태로 저장
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_accessed", nullable = false)
    private LocalDateTime lastAccessed;
    
    @Column(name = "access_count", nullable = false)
    private Integer accessCount;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @Column(name = "source", nullable = false)
    private String source; // "REAL_API" 또는 "MOCK"
    
    public enum ApiType {
        DFO_SERVERS,
        DFO_CHARACTER_SEARCH,
        DFO_CHARACTER_DETAIL,
        DFO_CHARACTER_TIMELINE,
        DUNDAM_CHARACTER_INFO,
        DUNDAM_CHARACTER_STATS
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastAccessed = LocalDateTime.now();
        accessCount = 1;
        isActive = true;
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastAccessed = LocalDateTime.now();
        accessCount++;
    }
}
