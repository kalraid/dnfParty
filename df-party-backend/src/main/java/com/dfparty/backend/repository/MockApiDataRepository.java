package com.dfparty.backend.repository;

import com.dfparty.backend.model.MockApiData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MockApiDataRepository extends JpaRepository<MockApiData, Long> {
    
    /**
     * API 타입, 엔드포인트, 메서드로 Mock 데이터 조회
     */
    Optional<MockApiData> findByApiTypeAndEndpointAndRequestMethodAndIsActiveTrue(
            MockApiData.ApiType apiType, 
            String endpoint, 
            String requestMethod);
    
    /**
     * API 타입별 Mock 데이터 조회
     */
    List<MockApiData> findByApiTypeAndIsActiveTrue(MockApiData.ApiType apiType);
    
    /**
     * 활성화된 모든 Mock 데이터 조회
     */
    List<MockApiData> findByIsActiveTrue();
    
    /**
     * 소스별 Mock 데이터 조회
     */
    List<MockApiData> findBySourceAndIsActiveTrue(String source);
    
    /**
     * 특정 엔드포인트의 Mock 데이터 조회
     */
    @Query("SELECT m FROM MockApiData m WHERE m.endpoint = :endpoint AND m.isActive = true")
    List<MockApiData> findByEndpoint(@Param("endpoint") String endpoint);
    
    /**
     * API 타입과 요청 파라미터로 Mock 데이터 조회 (유사도 기반)
     */
    @Query("SELECT m FROM MockApiData m WHERE m.apiType = :apiType AND m.isActive = true ORDER BY m.lastAccessed DESC")
    List<MockApiData> findRecentByApiType(@Param("apiType") MockApiData.ApiType apiType);
}
