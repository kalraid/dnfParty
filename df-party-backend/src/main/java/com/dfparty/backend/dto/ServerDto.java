package com.dfparty.backend.dto;

public class ServerDto {
    private String serverId;
    private String serverName;

    public ServerDto() {}

    public ServerDto(String serverId, String serverName) {
        this.serverId = serverId;
        this.serverName = serverName;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
}
