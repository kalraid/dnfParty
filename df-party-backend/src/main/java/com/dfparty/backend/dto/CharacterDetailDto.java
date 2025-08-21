package com.dfparty.backend.dto;

import java.util.List;
import java.util.Map;

public class CharacterDetailDto {
    private String serverId;
    private String characterId;
    private String characterName;
    private String jobId;
    private String jobGrowId;
    private String jobName;
    private String jobGrowName;
    private int level;
    private String adventureName;
    private long fame;
    private Long buffPower;
    private Long totalDamage;
    private Boolean dungeonClearNabel;
    private Boolean dungeonClearVenus;
    private Boolean dungeonClearFog;
    private Map<String, Object> status;
    private List<Map<String, Object>> equipment;
    private Map<String, Object> buffSkill;
    private String characterImageUrl;
    private String avatarImageUrl;
    private Boolean isHardNabelEligible;
    private Boolean isNormalNabelEligible;
    private Boolean isTwilightEligible;



    public CharacterDetailDto() {}

    public CharacterDetailDto(String serverId, String characterId, String characterName, 
                             String jobId, String jobGrowId, String jobName, String jobGrowName, 
                             int level, String adventureName, long fame) {
        this.serverId = serverId;
        this.characterId = characterId;
        this.characterName = characterName;
        this.jobId = jobId;
        this.jobGrowId = jobGrowId;
        this.jobName = jobName;
        this.jobGrowName = jobGrowName;
        this.level = level;
        this.adventureName = adventureName;
        this.fame = fame;
    }

    // Getters and Setters
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getCharacterId() { return characterId; }
    public void setCharacterId(String characterId) { this.characterId = characterId; }

    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getJobGrowId() { return jobGrowId; }
    public void setJobGrowId(String jobGrowId) { this.jobGrowId = jobGrowId; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getJobGrowName() { return jobGrowName; }
    public void setJobGrowName(String jobGrowName) { this.jobGrowName = jobGrowName; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getAdventureName() { return adventureName; }
    public void setAdventureName(String adventureName) { this.adventureName = adventureName; }

    public long getFame() { return fame; }
    public void setFame(long fame) { this.fame = fame; }

    public Map<String, Object> getStatus() { return status; }
    public void setStatus(Map<String, Object> status) { this.status = status; }

    public List<Map<String, Object>> getEquipment() { return equipment; }
    public void setEquipment(List<Map<String, Object>> equipment) { this.equipment = equipment; }

    public Map<String, Object> getBuffSkill() { return buffSkill; }
    public void setBuffSkill(Map<String, Object> buffSkill) { this.buffSkill = buffSkill; }

    public String getCharacterImageUrl() { return characterImageUrl; }
    public void setCharacterImageUrl(String characterImageUrl) { this.characterImageUrl = characterImageUrl; }

    public String getAvatarImageUrl() { return avatarImageUrl; }
    public void setAvatarImageUrl(String avatarImageUrl) { this.avatarImageUrl = avatarImageUrl; }

    public Long getBuffPower() { return buffPower; }
    public void setBuffPower(Long buffPower) { this.buffPower = buffPower; }

    public Long getTotalDamage() { return totalDamage; }
    public void setTotalDamage(Long totalDamage) { this.totalDamage = totalDamage; }

    public Boolean getDungeonClearNabel() { return dungeonClearNabel; }
    public void setDungeonClearNabel(Boolean dungeonClearNabel) { this.dungeonClearNabel = dungeonClearNabel; }

    public Boolean getDungeonClearVenus() { return dungeonClearVenus; }
    public void setDungeonClearVenus(Boolean dungeonClearVenus) { this.dungeonClearVenus = dungeonClearVenus; }

    public Boolean getDungeonClearFog() { return dungeonClearFog; }
    public void setDungeonClearFog(Boolean dungeonClearFog) { this.dungeonClearFog = dungeonClearFog; }
    
    public Boolean getIsHardNabelEligible() { return isHardNabelEligible; }
    public void setIsHardNabelEligible(Boolean isHardNabelEligible) { this.isHardNabelEligible = isHardNabelEligible; }
    
    public Boolean getIsNormalNabelEligible() { return isNormalNabelEligible; }
    public void setIsNormalNabelEligible(Boolean isNormalNabelEligible) { this.isNormalNabelEligible = isNormalNabelEligible; }

    public Boolean getIsTwilightEligible() { return isTwilightEligible; }
    public void setIsTwilightEligible(Boolean isTwilightEligible) { this.isTwilightEligible = isTwilightEligible; }
}
