package com.dfparty.backend.repository;

import com.dfparty.backend.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {

    // 기본 조회 메서드
    Optional<Character> findByCharacterId(String characterId);
    
    List<Character> findByServerId(String serverId);
    
    List<Character> findByAdventure_AdventureName(String adventureName);
    
    List<Character> findByServerIdAndAdventure_AdventureName(String serverId, String adventureName);
    
    // 던전 클리어 현황 기반 조회
    List<Character> findByDungeonClearNabelFalse();
    
    List<Character> findByDungeonClearVenusFalse();
    
    List<Character> findByDungeonClearFogFalse();
    
    // 명성 기준 조회
    List<Character> findByFameGreaterThanEqual(Long minFame);
    
    // 업데이트 시간 기준 조회
    List<Character> findByLastStatsUpdateBefore(LocalDateTime before);
    
    List<Character> findByLastDungeonCheckBefore(LocalDateTime before);
    
    // 제외된 캐릭터 조회
    List<Character> findByIsExcludedTrue();
    
    List<Character> findByIsExcludedFalse();
    
    // 던전별 즐겨찾기 캐릭터 조회
    List<Character> findByIsFavoriteNabelTrue();
    List<Character> findByIsFavoriteVenusTrue();
    List<Character> findByIsFavoriteFogTrue();
    List<Character> findByIsFavoriteTwilightTrue();
    
    // 복합 조건 조회
    @Query("SELECT c FROM Character c WHERE c.serverId = :serverId AND c.fame >= :minFame AND c.isExcluded = false")
    List<Character> findAvailableCharactersByServerAndFame(@Param("serverId") String serverId, @Param("minFame") Long minFame);
    
    // 특정 던전을 클리어하지 않은 캐릭터 조회
    @Query("SELECT c FROM Character c WHERE c.serverId = :serverId AND c.dungeonClearNabel = false AND c.isExcluded = false")
    List<Character> findCharactersNotClearedNabel(@Param("serverId") String serverId);
    
    @Query("SELECT c FROM Character c WHERE c.serverId = :serverId AND c.dungeonClearVenus = false AND c.isExcluded = false")
    List<Character> findCharactersNotClearedVenus(@Param("serverId") String serverId);
    
    @Query("SELECT c FROM Character c WHERE c.serverId = :serverId AND c.dungeonClearFog = false AND c.isExcluded = false")
    List<Character> findCharactersNotClearedFog(@Param("serverId") String serverId);
    
    // 모험단별 통계 조회
    @Query("SELECT c.adventure.adventureName, COUNT(c), AVG(c.fame), AVG(c.level) FROM Character c WHERE c.serverId = :serverId GROUP BY c.adventure.adventureName")
    List<Object[]> getAdventureStatsByServer(@Param("serverId") String serverId);
    
    // 업데이트가 필요한 캐릭터 조회 (1시간 이상 업데이트되지 않은 명성 정보)
    @Query("SELECT c FROM Character c WHERE c.lastStatsUpdate IS NULL OR c.lastStatsUpdate < :threshold")
    List<Character> findCharactersNeedingFameUpdate(@Param("threshold") LocalDateTime threshold);
    
    // 던전 클리어 현황 업데이트가 필요한 캐릭터 조회 (1주일 이상 업데이트되지 않은 던전 정보)
    @Query("SELECT c FROM Character c WHERE c.lastDungeonCheck IS NULL OR c.lastDungeonCheck < :threshold")
    List<Character> findCharactersNeedingDungeonUpdate(@Param("threshold") LocalDateTime threshold);
    
    // 특정 조건을 만족하는 캐릭터 수 조회
    long countByServerIdAndIsExcludedFalse(String serverId);
    
    long countByAdventure_AdventureNameAndIsExcludedFalse(String adventureName);
    
    // 존재 여부 확인
    boolean existsByCharacterId(String characterId);
    
    boolean existsByServerIdAndCharacterName(String serverId, String characterName);
    
    // ID 목록으로 캐릭터 조회
    List<Character> findAllByCharacterIdIn(List<String> characterIds);
    
    // 모험단명 목록 조회
    @Query("SELECT DISTINCT c.adventure.adventureName FROM Character c WHERE c.adventure.adventureName IS NOT NULL AND c.adventure.adventureName != 'N/A' ORDER BY c.adventure.adventureName")
    List<String> findDistinctAdventureNames();
    
    // 동기화를 위한 캐릭터 조회 (최근 업데이트 순)
    @Query("SELECT c FROM Character c ORDER BY COALESCE(c.lastStatsUpdate, c.createdAt) ASC")
    List<Character> findAllByOrderByLastStatsUpdateAsc();
}
