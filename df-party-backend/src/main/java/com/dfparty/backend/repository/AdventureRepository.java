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
    
    // 서버별 모험단 조회 메서드
    Optional<Adventure> findByAdventureNameAndServerId(String adventureName, String serverId);
    
    // 존재 여부 확인
    boolean existsByAdventureName(String adventureName);
    
    // 서버별 모험단 존재 여부 확인
    boolean existsByAdventureNameAndServerId(String adventureName, String serverId);
}
