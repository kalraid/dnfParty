package com.dfparty.backend.repository;

import com.dfparty.backend.model.SharedParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SharedPartyRepository extends JpaRepository<SharedParty, Long> {
    
    /**
     * 공유 코드로 파티 조회
     */
    Optional<SharedParty> findByShareCodeAndIsActiveTrue(String shareCode);
    
    /**
     * 던전별 공유 파티 조회
     */
    List<SharedParty> findByDungeonNameAndIsActiveTrueOrderByCreatedAtDesc(String dungeonName);
    
    /**
     * 파티 크기별 공유 파티 조회
     */
    List<SharedParty> findByPartySizeAndIsActiveTrueOrderByCreatedAtDesc(Integer partySize);
    
    /**
     * 활성화된 모든 공유 파티 조회
     */
    List<SharedParty> findByIsActiveTrueOrderByCreatedAtDesc();
    
    /**
     * 만료되지 않은 공유 파티 조회
     */
    @Query("SELECT sp FROM SharedParty sp WHERE sp.isActive = true AND sp.expiresAt > :now ORDER BY sp.createdAt DESC")
    List<SharedParty> findActiveAndNotExpired(@Param("now") LocalDateTime now);
    
    /**
     * 제목 또는 설명으로 검색
     */
    @Query("SELECT sp FROM SharedParty sp WHERE sp.isActive = true AND (sp.title LIKE %:keyword% OR sp.description LIKE %:keyword%) ORDER BY sp.createdAt DESC")
    List<SharedParty> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 태그로 검색
     */
    @Query("SELECT sp FROM SharedParty sp WHERE sp.isActive = true AND sp.tags LIKE %:tag% ORDER BY sp.createdAt DESC")
    List<SharedParty> findByTag(@Param("tag") String tag);
    
    /**
     * 인기 공유 파티 조회 (조회 횟수 기준)
     */
    List<SharedParty> findByIsActiveTrueOrderByViewCountDesc();
    
    /**
     * 최근 공유 파티 조회
     */
    List<SharedParty> findByIsActiveTrueOrderByCreatedAtDesc();
}
