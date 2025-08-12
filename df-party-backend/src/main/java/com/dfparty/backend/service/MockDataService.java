package com.dfparty.backend.service;

import com.dfparty.backend.config.MockConfig;
import com.dfparty.backend.model.MockApiData;
import com.dfparty.backend.repository.MockApiDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockDataService {
    
    private final MockApiDataRepository mockApiDataRepository;
    private final MockConfig mockConfig;
    private final ObjectMapper objectMapper;
    
    /**
     * Mock 데이터 저장 (실제 API 호출 시)
     */
    public void saveMockData(
            MockApiData.ApiType apiType,
            String endpoint,
            String requestMethod,
            Map<String, String> requestParams,
            String requestBody,
            ResponseEntity<?> response) {
        
        if (!mockConfig.isAutoSave()) {
            return;
        }
        
        try {
            MockApiData mockData = MockApiData.builder()
                .apiType(apiType)
                .endpoint(endpoint)
                .requestMethod(requestMethod)
                .requestParams(convertToJson(requestParams))
                .requestBody(requestBody)
                .responseStatus(response.getStatusCodeValue())
                .responseBody(convertToJson(response.getBody()))
                .responseHeaders(convertToJson(response.getHeaders()))
                .source("REAL_API")
                .build();
            
            mockApiDataRepository.save(mockData);
            log.info("Mock 데이터 저장 완료: {} - {}", apiType, endpoint);
            
        } catch (Exception e) {
            log.error("Mock 데이터 저장 실패", e);
        }
    }
    
    /**
     * Mock 데이터로 응답 생성
     */
    public Optional<ResponseEntity<?>> getMockResponse(
            MockApiData.ApiType apiType,
            String endpoint,
            String requestMethod) {
        
        try {
            Optional<MockApiData> mockData = mockApiDataRepository
                .findByApiTypeAndEndpointAndRequestMethodAndIsActiveTrue(apiType, endpoint, requestMethod);
            
            if (mockData.isPresent()) {
                MockApiData data = mockData.get();
                
                // 접근 횟수 업데이트
                data.setAccessCount(data.getAccessCount() + 1);
                mockApiDataRepository.save(data);
                
                // Mock 응답 생성
                Object responseBody = objectMapper.readValue(data.getResponseBody(), Object.class);
                HttpHeaders headers = objectMapper.readValue(data.getResponseHeaders(), HttpHeaders.class);
                
                ResponseEntity<?> response = ResponseEntity
                    .status(data.getResponseStatus())
                    .headers(headers)
                    .body(responseBody);
                
                log.info("Mock 응답 반환: {} - {} (접근 횟수: {})", apiType, endpoint, data.getAccessCount());
                return Optional.of(response);
            }
            
        } catch (Exception e) {
            log.error("Mock 응답 생성 실패", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * 모든 Mock 데이터 조회
     */
    public List<MockApiData> getAllMockData() {
        return mockApiDataRepository.findByIsActiveTrue();
    }
    
    /**
     * API 타입별 Mock 데이터 조회
     */
    public List<MockApiData> getMockDataByType(MockApiData.ApiType apiType) {
        return mockApiDataRepository.findByApiTypeAndIsActiveTrue(apiType);
    }
    
    /**
     * Mock 데이터 비활성화
     */
    public void deactivateMockData(Long id) {
        mockApiDataRepository.findById(id).ifPresent(data -> {
            data.setIsActive(false);
            mockApiDataRepository.save(data);
            log.info("Mock 데이터 비활성화: {}", id);
        });
    }
    
    /**
     * Mock 데이터 삭제
     */
    public void deleteMockData(Long id) {
        mockApiDataRepository.deleteById(id);
        log.info("Mock 데이터 삭제: {}", id);
    }
    
    /**
     * Mock 데이터 초기화
     */
    public void resetAllMockData() {
        List<MockApiData> allData = mockApiDataRepository.findAll();
        mockApiDataRepository.deleteAll(allData);
        log.info("모든 Mock 데이터 초기화 완료");
    }
    
    // 유틸리티 메서드들
    private String convertToJson(Object obj) {
        try {
            if (obj == null) return "{}";
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON 변환 실패", e);
            return "{}";
        }
    }
    
    private String convertToJson(Map<String, ?> map) {
        try {
            if (map == null) return "{}";
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("JSON 변환 실패", e);
            return "{}";
        }
    }
}
