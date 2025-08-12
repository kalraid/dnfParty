package com.dfparty.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedPartyDto {
    
    private Long id;
    private String shareCode;
    private String title;
    private String description;
    private String dungeonName;
    private Integer partySize;
    private Map<String, Object> partyData; // 파티 구성 데이터
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Integer viewCount;
    private Boolean isActive;
    private List<String> tags;
    
    // 공유 URL 생성
    public String getShareUrl() {
        return "/shared-party/" + shareCode;
    }
    
    // 만료 여부 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    // 남은 시간 계산 (일 단위)
    public long getDaysUntilExpiry() {
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
    }
}
