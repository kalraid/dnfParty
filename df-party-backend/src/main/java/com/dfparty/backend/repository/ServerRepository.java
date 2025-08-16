package com.dfparty.backend.repository;

import com.dfparty.backend.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, String> {

    // 서버 ID로 조회
    Optional<Server> findByServerId(String serverId);
    
    // 서버 이름으로 조회
    Optional<Server> findByServerName(String serverName);
    
    // 활성 서버 목록 조회
    List<Server> findByIsActiveTrue();
    
    // 서버 존재 여부 확인
    boolean existsByServerId(String serverId);
    boolean existsByServerName(String serverName);
}
