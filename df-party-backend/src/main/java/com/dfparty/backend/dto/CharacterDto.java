package com.dfparty.backend.dto;

public class CharacterDto {
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

    public CharacterDto() {}

    public CharacterDto(String serverId, String characterId, String characterName, 
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
}
