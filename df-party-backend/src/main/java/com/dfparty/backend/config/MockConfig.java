package com.dfparty.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.mock")
public class MockConfig {
    
    /**
     * Mock 모드 활성화 여부
     */
    private boolean enabled = false;
    
    /**
     * Mock 서버 URL
     */
    private String serverUrl = "http://localhost:8081";
    
    /**
     * API 호출 시 응답 자동 저장 여부
     */
    private boolean autoSave = true;
    
    /**
     * Mock 데이터 저장 경로
     */
    private String dataPath = "./mock-data";
    
    /**
     * Mock 모드에서 사용할 API 타입들
     */
    private ApiTypes apiTypes = new ApiTypes();
    
    @Data
    public static class ApiTypes {
        private boolean dfo = true;
        private boolean dundam = true;
    }
}
