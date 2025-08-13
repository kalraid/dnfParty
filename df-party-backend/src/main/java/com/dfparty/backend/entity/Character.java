package com.dfparty.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "characters")
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 정적 정보 (한 번 저장 후 재사용)
    @Column(name = "character_id", unique = true, nullable = false)
    private String characterId;

    @Column(name = "character_name", nullable = false)
    private String characterName;

    @Column(name = "server_id", nullable = false)
    private String serverId;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "job_grow_id")
    private String jobGrowId;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "job_grow_name")
    private String jobGrowName;

    @Column(name = "level")
    private Integer level;

    @Column(name = "adventure_name")
    private String adventureName;

    @Column(name = "guild_name")
    private String guildName;

    // 동적 정보 (주기적 업데이트 필요)
    @Column(name = "fame")
    private Long fame;

    @Column(name = "dungeon_clear_nabel")
    private Boolean dungeonClearNabel = false;

    @Column(name = "dungeon_clear_venus")
    private Boolean dungeonClearVenus = false;

    @Column(name = "dungeon_clear_fog")
    private Boolean dungeonClearFog = false;

    @Column(name = "dungeon_clear_azure")
    private Boolean dungeonClearAzure = false;

    @Column(name = "dungeon_clear_storm")
    private Boolean dungeonClearStorm = false;

    @Column(name = "dungeon_clear_nightmare")
    private Boolean dungeonClearNightmare = false;

    @Column(name = "dungeon_clear_temple")
    private Boolean dungeonClearTemple = false;

    @Column(name = "last_dungeon_check")
    private LocalDateTime lastDungeonCheck;

    // 실시간 정보 (매번 새로 조회)
    @Column(name = "buff_power")
    private Long buffPower;

    @Column(name = "total_damage")
    private Long totalDamage;

    @Column(name = "dundam_source")
    private String dundamSource;

    @Column(name = "last_stats_update")
    private LocalDateTime lastStatsUpdate;

    // 메타 정보
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_excluded")
    private Boolean isExcluded = false;

    @Column(name = "excluded_dungeons")
    private String excludedDungeons; // JSON 형태로 저장 (예: ["navel", "venus"])

    @Column(name = "is_favorite")
    private Boolean isFavorite = false;

    // 생성자
    public Character() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Character(String characterId, String characterName, String serverId) {
        this();
        this.characterId = characterId;
        this.characterName = characterName;
        this.serverId = serverId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCharacterId() { return characterId; }
    public void setCharacterId(String characterId) { this.characterId = characterId; }

    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getJobGrowId() { return jobGrowId; }
    public void setJobGrowId(String jobGrowId) { this.jobGrowId = jobGrowId; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getJobGrowName() { return jobGrowName; }
    public void setJobGrowName(String jobGrowName) { this.jobGrowName = jobGrowName; }

    // 직업명 반환 (getJob() 메서드 추가)
    public String getJob() { return jobName; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getAdventureName() { return adventureName; }
    public void setAdventureName(String adventureName) { this.adventureName = adventureName; }

    public String getGuildName() { return guildName; }
    public void setGuildName(String guildName) { this.guildName = guildName; }

    public Long getFame() { return fame; }
    public void setFame(Long fame) { this.fame = fame; }

    public Boolean getDungeonClearNabel() { return dungeonClearNabel; }
    public void setDungeonClearNabel(Boolean dungeonClearNabel) { this.dungeonClearNabel = dungeonClearNabel; }

    public Boolean getDungeonClearVenus() { return dungeonClearVenus; }
    public void setDungeonClearVenus(Boolean dungeonClearVenus) { this.dungeonClearVenus = dungeonClearVenus; }

    public Boolean getDungeonClearFog() { return dungeonClearFog; }
    public void setDungeonClearFog(Boolean dungeonClearFog) { this.dungeonClearFog = dungeonClearFog; }

    public Boolean getDungeonClearAzure() { return dungeonClearAzure; }
    public void setDungeonClearAzure(Boolean dungeonClearAzure) { this.dungeonClearAzure = dungeonClearAzure; }

    public Boolean getDungeonClearStorm() { return dungeonClearStorm; }
    public void setDungeonClearStorm(Boolean dungeonClearStorm) { this.dungeonClearStorm = dungeonClearStorm; }

    public Boolean getDungeonClearNightmare() { return dungeonClearNightmare; }
    public void setDungeonClearNightmare(Boolean dungeonClearNightmare) { this.dungeonClearNightmare = dungeonClearNightmare; }

    public Boolean getDungeonClearTemple() { return dungeonClearTemple; }
    public void setDungeonClearTemple(Boolean dungeonClearTemple) { this.dungeonClearTemple = dungeonClearTemple; }

    public LocalDateTime getLastDungeonCheck() { return lastDungeonCheck; }
    public void setLastDungeonCheck(LocalDateTime lastDungeonCheck) { this.lastDungeonCheck = lastDungeonCheck; }

    public Long getBuffPower() { return buffPower; }
    public void setBuffPower(Long buffPower) { this.buffPower = buffPower; }

    public Long getTotalDamage() { return totalDamage; }
    public void setTotalDamage(Long totalDamage) { this.totalDamage = totalDamage; }

    public String getDundamSource() { return dundamSource; }
    public void setDundamSource(String dundamSource) { this.dundamSource = dundamSource; }

    public LocalDateTime getLastStatsUpdate() { return lastStatsUpdate; }
    public void setLastStatsUpdate(LocalDateTime lastStatsUpdate) { this.lastStatsUpdate = lastStatsUpdate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getIsExcluded() { return isExcluded; }
    public void setIsExcluded(Boolean isExcluded) { this.isExcluded = isExcluded; }

    public String getExcludedDungeons() { return excludedDungeons; }
    public void setExcludedDungeons(String excludedDungeons) { this.excludedDungeons = excludedDungeons; }

    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }

    // 전투력 계산 메서드 (버프력 + 총딜의 합계)
    public Long getCombatPower() {
        if (buffPower == null && totalDamage == null) {
            return 0L;
        }
        return (buffPower != null ? buffPower : 0L) + (totalDamage != null ? totalDamage : 0L);
    }

    // 업데이트 시간 자동 설정
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    // 스펙 정보 업데이트
    public void updateStats(Long buffPower, Long totalDamage, String source) {
        this.buffPower = buffPower;
        this.totalDamage = totalDamage;
        this.dundamSource = source;
        this.lastStatsUpdate = LocalDateTime.now();
    }

    // 제외 던전 설정
    public void setExcludedDungeon(String dungeon, boolean excluded) {
        // JSON 형태로 제외 던전 관리
        // 실제 구현 시 JSON 파싱/생성 로직 필요
    }
}
