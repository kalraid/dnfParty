package com.dfparty.backend.repository;

import com.dfparty.backend.entity.Adventure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdventureRepository extends JpaRepository<Adventure, Long> {

    // 기본 조회 메서드
    Optional<Adventure> findByAdventureName(String adventureName);
    
    List<Adventure> findByServerId(String serverId);
    
    Optional<Adventure> findByServerIdAndAdventureName(String serverId, String adventureName);
    
    // 활성 모험단 조회
    List<Adventure> findByIsActiveTrue();
    
    List<Adventure> findByServerIdAndIsActiveTrue(String serverId);
    
    // 활동 기준 조회
    List<Adventure> findByLastActivityAfter(LocalDateTime after);
    
    List<Adventure> findByLastActivityBefore(LocalDateTime before);
    
    // 통계 기반 조회
    List<Adventure> findByCharacterCountGreaterThan(Integer minCount);
    
    List<Adventure> findByTotalFameGreaterThanEqual(Long minFame);
    
    List<Adventure> findByAverageLevelGreaterThanEqual(Double minLevel);
    
    // 복합 조건 조회
    @Query("SELECT a FROM Adventure a WHERE a.serverId = :serverId AND a.isActive = true AND a.characterCount >= :minCount")
    List<Adventure> findActiveAdventuresByServerAndMinCount(@Param("serverId") String serverId, @Param("minCount") Integer minCount);
    
    // 모험단별 캐릭터 수 통계
    @Query("SELECT a.adventureName, a.characterCount, a.totalFame, a.averageLevel FROM Adventure a WHERE a.serverId = :serverId ORDER BY a.characterCount DESC")
    List<Object[]> getAdventureRankingsByServer(@Param("serverId") String serverId);
    
    // 서버별 모험단 통계
    @Query("SELECT a.serverId, COUNT(a), SUM(a.characterCount), AVG(a.totalFame) FROM Adventure a WHERE a.isActive = true GROUP BY a.serverId")
    List<Object[]> getServerAdventureStats();
    
    // 활동이 없는 모험단 조회 (30일 이상 활동 없음)
    @Query("SELECT a FROM Adventure a WHERE a.lastActivity IS NULL OR a.lastActivity < :threshold")
    List<Adventure> findInactiveAdventures(@Param("threshold") LocalDateTime threshold);
    
    // 존재 여부 확인
    boolean existsByAdventureName(String adventureName);
    
    boolean existsByServerIdAndAdventureName(String serverId, String adventureName);
    
    // 모험단 수 조회
    long countByServerId(String serverId);
    
    long countByServerIdAndIsActiveTrue(String serverId);
}
