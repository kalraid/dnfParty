package com.dfparty.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "characters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Character {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "character_id", unique = true, nullable = false)
    private String characterId;
    
    @Column(name = "character_name", nullable = false)
    private String characterName;
    
    @Column(name = "server_id", nullable = false)
    private String serverId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", insertable = false, updatable = false)
    private Server server;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adventure_id")
    private Adventure adventure;
    
    @Column(name = "level")
    private Integer level;
    
    @Column(name = "fame")
    private Long fame;
    
    @Column(name = "job_id")
    private String jobId;
    
    @Column(name = "job_name")
    private String jobName;
    
    @Column(name = "job_grow_id")
    private String jobGrowId;
    
    @Column(name = "job_grow_name")
    private String jobGrowName;
    
    // job_name 대신 job_grow_name을 사용하도록 별칭 추가
    @Transient
    public String getJobName() {
        return jobGrowName != null ? jobGrowName : "직업 없음";
    }
    
    @Transient
    public void setJobName(String jobName) {
        this.jobGrowName = jobName;
    }
    
    @Column(name = "guild_name")
    private String guildName;
    
    @Column(name = "character_image_url", columnDefinition = "TEXT")
    private String characterImageUrl;
    
    @Column(name = "avatar_image_url", columnDefinition = "TEXT")
    private String avatarImageUrl;
    
    // 던전 클리어 상태
    @Column(name = "dungeon_clear_nabel")
    @Builder.Default
    private Boolean dungeonClearNabel = false;
    
    @Column(name = "dungeon_clear_venus")
    @Builder.Default
    private Boolean dungeonClearVenus = false;
    
    @Column(name = "dungeon_clear_fog")
    @Builder.Default
    private Boolean dungeonClearFog = false;
    
    @Column(name = "dungeon_clear_twilight")
    @Builder.Default
    private Boolean dungeonClearTwilight = false;
    
    @Column(name = "dungeon_clear_azure")
    @Builder.Default
    private Boolean dungeonClearAzure = false;
    
    @Column(name = "dungeon_clear_storm")
    @Builder.Default
    private Boolean dungeonClearStorm = false;
    
    @Column(name = "dungeon_clear_temple")
    @Builder.Default
    private Boolean dungeonClearTemple = false;
    
    @Column(name = "dungeon_clear_nightmare")
    @Builder.Default
    private Boolean dungeonClearNightmare = false;
    
    // 던전별 안감/업둥 상태
    @Column(name = "is_excluded_nabel")
    @Builder.Default
    private Boolean isExcludedNabel = false;
    
    @Column(name = "is_excluded_venus")
    @Builder.Default
    private Boolean isExcludedVenus = false;
    
    @Column(name = "is_excluded_fog")
    @Builder.Default
    private Boolean isExcludedFog = false;
    
    @Column(name = "is_skip_nabel")
    @Builder.Default
    private Boolean isSkipNabel = false;
    
    @Column(name = "is_skip_venus")
    @Builder.Default
    private Boolean isSkipVenus = false;
    
    @Column(name = "is_skip_fog")
    @Builder.Default
    private Boolean isSkipFog = false;
    
    // 던전별 즐겨찾기
    @Column(name = "is_favorite_nabel")
    @Builder.Default
    private Boolean isFavoriteNabel = false;
    
    @Column(name = "is_favorite_venus")
    @Builder.Default
    private Boolean isFavoriteVenus = false;
    
    @Column(name = "is_favorite_fog")
    @Builder.Default
    private Boolean isFavoriteFog = false;
    
    @Column(name = "is_favorite_twilight")
    @Builder.Default
    private Boolean isFavoriteTwilight = false;
    
    // 스탯 정보
    @Column(name = "buff_power")
    private Long buffPower;
    
    @Column(name = "total_damage")
    private Long totalDamage;
    
    // 던담에서 가져온 두 개의 총딜 값 (작은 값, 큰 값)
    @Column(name = "dundam_total_damage_small")
    private Long dundamTotalDamageSmall;
    
    @Column(name = "dundam_total_damage_large")
    private Long dundamTotalDamageLarge;
    

    
    // 수동 입력 스탯
    @Column(name = "manual_buff_power")
    private Long manualBuffPower;
    
    @Column(name = "manual_total_damage")
    private Long manualTotalDamage;
    

    
    @Column(name = "manual_updated_at")
    private LocalDateTime manualUpdatedAt;
    
    @Column(name = "manual_updated_by")
    private String manualUpdatedBy;
    
    // 기타 정보
    @Column(name = "dundam_source")
    private String dundamSource;
    
    @Column(name = "excluded_dungeons", columnDefinition = "TEXT")
    private String excludedDungeons;
    
    @Column(name = "is_excluded")
    @Builder.Default
    private Boolean isExcluded = false;
    
    @Column(name = "is_hard_nabel_eligible")
    @Builder.Default
    private Boolean isHardNabelEligible = false;
    
    @Column(name = "is_normal_nabel_eligible")
    @Builder.Default
    private Boolean isNormalNabelEligible = false;
    
    @Column(name = "last_dungeon_check")
    private LocalDateTime lastDungeonCheck;
    
    @Column(name = "last_stats_update")
    private LocalDateTime lastStatsUpdate;
    
    // 타임스탬프
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 편의 메서드
    public String getAdventureName() {
        return adventure != null ? adventure.getAdventureName() : null;
    }
    
    public void setAdventureName(String adventureName) {
        if (this.adventure == null) {
            this.adventure = new Adventure();
        }
        this.adventure.setAdventureName(adventureName);
    }
    
    // 하위 호환성을 위한 메서드들
    public Boolean getIsFavorite() {
        return isFavoriteNabel; // 기본값으로 나벨 즐겨찾기 사용
    }
    
    public void setIsFavorite(Boolean isFavorite) {
        this.isFavoriteNabel = isFavorite;
    }
    
    // 직업명 반환 (getJob() 메서드 추가)
    public String getJob() { 
        return jobName; 
    }
    

    
    // 전투력 계산 메서드
    public Long getCombatPower() {
        if (buffPower == null && totalDamage == null) {
            return 0L;
        }
        return (buffPower != null ? buffPower : 0L) + (totalDamage != null ? totalDamage : 0L);
    }
    
    // 던전 클리어 현황 업데이트
    public void updateDungeonClearStatus(Boolean nabel, Boolean venus, Boolean fog, Boolean azure, Boolean storm) {
        this.dungeonClearNabel = nabel;
        this.dungeonClearVenus = venus;
        this.dungeonClearFog = fog;
        this.dungeonClearAzure = azure;
        this.dungeonClearStorm = storm;
        this.lastDungeonCheck = LocalDateTime.now();
    }
    
    // 스탯 정보 업데이트
    public void updateStats(Long buffPower, Long totalDamage, String source) {
        this.buffPower = buffPower;
        this.totalDamage = totalDamage;
        this.dundamSource = source;
        this.lastStatsUpdate = LocalDateTime.now();
        
        // 하드 나벨 대상자 여부 업데이트
        updateHardNabelEligibility();
    }
    

    
    // 수동 스탯 업데이트 메서드
    public void updateManualStats(Long buffPower, Long totalDamage, String updatedBy) {
        this.manualBuffPower = buffPower;
        this.manualTotalDamage = totalDamage;
        this.manualUpdatedAt = LocalDateTime.now();
        this.manualUpdatedBy = updatedBy;
        
        // 하드 나벨 대상자 여부 업데이트
        updateHardNabelEligibility();
    }
    
    // 하드 나벨 대상자 여부 업데이트
    private void updateHardNabelEligibility() {
        if (isBuffer()) {
            // 버퍼: 버프력 500만 이상
            this.isHardNabelEligible = (getEffectiveBuffPower() != null && getEffectiveBuffPower() >= 50000000L);
        } else {
            // 딜러: 총딜 100억 이상
            this.isHardNabelEligible = (getEffectiveTotalDamage() != null && getEffectiveTotalDamage() >= 10000000000L);
        }
        
        // 일반 나벨 대상자 여부도 함께 업데이트
        updateNormalNabelEligibility();
    }
    
    // 일반 나벨 대상자 여부 업데이트
    private void updateNormalNabelEligibility() {
        if (isBuffer()) {
            // 버퍼: 버프력 400만 이상
            this.isNormalNabelEligible = (getEffectiveBuffPower() != null && getEffectiveBuffPower() >= 40000000L);
        } else {
            // 딜러: 총딜 30억 이상
            this.isNormalNabelEligible = (getEffectiveTotalDamage() != null && getEffectiveTotalDamage() >= 3000000000L);
        }
    }
    
    // 버퍼 여부 판단
    private boolean isBuffer() {
        if (jobGrowName == null) return false;
        // 사용자 요청 기준: 크루세이더, 뮤즈, 패러메딕, 인챈트리스만 버퍼
        return jobGrowName.contains("크루세이더") || jobGrowName.contains("뮤즈") || 
               jobGrowName.contains("패러메딕") || jobGrowName.contains("인챈트리스");
    }
    

    
    // 효과적인 스탯 반환 (수동 > 자동)
    public Long getEffectiveBuffPower() {
        return manualBuffPower != null ? manualBuffPower : buffPower;
    }
    
    public Long getEffectiveTotalDamage() {
        return manualTotalDamage != null ? manualTotalDamage : totalDamage;
    }
    
    // 던담에서 가져온 두 개의 총딜 값 중 큰 값 반환
    public Long getDundamTotalDamageMax() {
        if (dundamTotalDamageLarge != null && dundamTotalDamageSmall != null) {
            return Math.max(dundamTotalDamageLarge, dundamTotalDamageSmall);
        } else if (dundamTotalDamageLarge != null) {
            return dundamTotalDamageLarge;
        } else if (dundamTotalDamageSmall != null) {
            return dundamTotalDamageSmall;
        }
        return null;
    }
    
    // 던담에서 가져온 두 개의 총딜 값 중 작은 값 반환
    public Long getDundamTotalDamageMin() {
        if (dundamTotalDamageLarge != null && dundamTotalDamageSmall != null) {
            return Math.min(dundamTotalDamageLarge, dundamTotalDamageSmall);
        } else if (dundamTotalDamageLarge != null) {
            return dundamTotalDamageLarge;
        } else if (dundamTotalDamageSmall != null) {
            return dundamTotalDamageSmall;
        }
        return null;
    }
    
    // 던담 총딜 값 업데이트 (두 개의 값 저장)
    public void updateDundamTotalDamage(Long damage1, Long damage2) {
        if (damage1 != null && damage2 != null) {
            if (damage1 >= damage2) {
                this.dundamTotalDamageLarge = damage1;
                this.dundamTotalDamageSmall = damage2;
            } else {
                this.dundamTotalDamageLarge = damage2;
                this.dundamTotalDamageSmall = damage1;
            }
        } else if (damage1 != null) {
            this.dundamTotalDamageLarge = damage1;
            this.dundamTotalDamageSmall = null;
        } else if (damage2 != null) {
            this.dundamTotalDamageLarge = damage2;
            this.dundamTotalDamageSmall = null;
        }
    }
    

    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 직업명 표시용 메서드
    public String getDisplayJobName() {
        if (jobGrowName != null && !jobGrowName.trim().isEmpty()) {
            // job_grow_name에서 眞 문자만 제거하고 공백 정리
            String cleanJobName = jobGrowName
                .replaceAll("眞\\s*", "")   // 眞 제거
                .trim();

            if (!cleanJobName.isEmpty()) {
                return cleanJobName;
            }
        }

        return "직업 없음";
    }

    // 하위 호환성을 위한 생성자
    public Character(String characterId, String characterName, String serverId) {
        this.characterId = characterId;
        this.characterName = characterName;
        this.serverId = serverId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
