package com.dfparty.backend.service;

import com.dfparty.backend.entity.Adventure;
import com.dfparty.backend.entity.Server;
import com.dfparty.backend.repository.AdventureRepository;
import com.dfparty.backend.repository.ServerRepository;
import com.dfparty.backend.service.DfoApiService;
import com.dfparty.backend.service.CachingService;
import com.dfparty.backend.dto.ServerDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ServerInitializationService {

    @Autowired
    private DfoApiService dfoApiService;
    
    @Autowired
    private AdventureRepository adventureRepository;
    
    @Autowired
    private ServerRepository serverRepository;
    
    @Autowired
    private CachingService cachingService;

    private boolean isInitialized = false;

    /**
     * 애플리케이션 시작 시 서버 목록 초기화
     * DB에 서버가 없을 때만 DFO API 호출
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeServersOnStartup() {
        if (!isInitialized) {
            System.out.println("=== 서버 초기화 시작 ===");
            
            // DB에서 서버 목록 확인
            List<Server> existingServers = serverRepository.findAll();
            
            if (existingServers.isEmpty()) {
                System.out.println("DB에 서버 정보가 없습니다. DFO API에서 서버 목록을 가져옵니다.");
                initializeServers();
            } else {
                System.out.println("DB에 서버 정보가 " + existingServers.size() + "개 있습니다. API 호출을 건너뜁니다.");
                // 기존 서버 정보를 캐시에 로드
                cachingService.put(
                    CachingService.getServerListKey(), 
                    existingServers, 
                    CachingService.CacheType.SERVER_LIST
                );
            }
            isInitialized = true;
            System.out.println("=== 서버 초기화 완료 ===");
        }
    }

    /**
     * 서버 목록 초기화 (수동 호출 가능)
     */
    public boolean initializeServers() {
        try {
            System.out.println("DFO API에서 서버 목록 조회 시작...");
            
            // DFO API에서 서버 목록 조회
            List<ServerDto> serverList = dfoApiService.getServers();
            
            if (serverList != null && !serverList.isEmpty()) {
                System.out.println("DFO API에서 " + serverList.size() + "개 서버 정보 수신");
                
                // 서버 목록을 DB에 저장
                saveServersToDB(serverList);
                
                // 캐시에 저장
                cachingService.put(
                    CachingService.getServerListKey(), 
                    serverList, 
                    CachingService.CacheType.SERVER_LIST
                );
                
                System.out.println("서버 목록 초기화 완료: " + serverList.size() + "개 서버");
                return true;
            } else {
                System.err.println("DFO API에서 서버 목록을 가져올 수 없습니다.");
            }
            
        } catch (Exception e) {
            System.err.println("서버 목록 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * 서버 목록을 데이터베이스에 저장
     */
    private void saveServersToDB(List<ServerDto> serverList) {
        try {
            System.out.println("서버 정보 DB 저장 시작...");
            
            for (ServerDto serverDto : serverList) {
                String serverId = serverDto.getServerId();
                String serverName = serverDto.getServerName();
                
                if (serverId != null && serverName != null) {
                    // 이미 존재하는지 확인
                    if (serverRepository.findByServerId(serverId).isEmpty()) {
                        // 새로운 서버 생성 및 저장
                        Server server = Server.builder()
                            .serverId(serverId)
                            .serverName(serverName)
                            .isActive(true)
                            .build();
                        
                        serverRepository.save(server);
                        System.out.println("서버 저장 완료: " + serverName + " (" + serverId + ")");
                    } else {
                        System.out.println("서버 이미 존재: " + serverName + " (" + serverId + ")");
                    }
                }
            }
            
            System.out.println("서버 정보 DB 저장 완료");
            
        } catch (Exception e) {
            System.err.println("서버 정보 DB 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * DFO API 응답에서 서버 목록 파싱
     * 실제 구현에서는 JSON 파싱 로직 필요
     */
    private List<Map<String, Object>> parseServerList(Object serverList) {
        // TODO: 실제 DFO API 응답 구조에 맞게 JSON 파싱 구현
        // 기본 서버 목록 반환
        return List.of(
            Map.of("serverId", "cain", "serverName", "카인"),
            Map.of("serverId", "siroco", "serverName", "시로코"),
            Map.of("serverId", "prey", "serverName", "프레이"),
            Map.of("serverId", "casillas", "serverName", "카시야스"),
            Map.of("serverId", "hilder", "serverName", "힐더"),
            Map.of("serverId", "anton", "serverName", "안톤"),
            Map.of("serverId", "bakal", "serverName", "바칼")
        );
    }

    /**
     * 서버 목록 조회 (캐시에서만)
     */
    public Object getServerList() {
        // 먼저 캐시에서 확인
        Object cachedServers = cachingService.get(CachingService.getServerListKey());
        if (cachedServers != null) {
            return cachedServers;
        }
        
        // 캐시에 없으면 초기화 실행하여 캐시에 저장
        if (initializeServers()) {
            return cachingService.get(CachingService.getServerListKey());
        }
        
        return null;
    }

    /**
     * DB 엔티티를 서버 목록 형태로 변환
     */
    private Object convertToServerList(List<Adventure> adventures) {
        // 새로운 스키마에서는 모험단 정보를 서버 정보로 변환하지 않음
        // 서버 정보는 별도로 관리
        return List.of(
            Map.of("serverId", "cain", "serverName", "카인"),
            Map.of("serverId", "siroco", "serverName", "시로코"),
            Map.of("serverId", "prey", "serverName", "프레이"),
            Map.of("serverId", "casillas", "serverName", "카시야스"),
            Map.of("serverId", "hilder", "serverName", "힐더"),
            Map.of("serverId", "anton", "serverName", "안톤"),
            Map.of("serverId", "bakal", "serverName", "바칼")
        );
    }

    /**
     * 초기화 상태 확인
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 강제 재초기화
     */
    public boolean reinitializeServers() {
        isInitialized = false;
        cachingService.remove(CachingService.getServerListKey());
        return initializeServers();
    }
}
