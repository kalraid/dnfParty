package com.dfparty.backend.service;

import com.dfparty.backend.entity.Adventure;
import com.dfparty.backend.repository.AdventureRepository;
import com.dfparty.backend.service.DfoApiService;
import com.dfparty.backend.service.CachingService;
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
    private CachingService cachingService;

    private boolean isInitialized = false;

    /**
     * 애플리케이션 시작 시 서버 목록 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeServersOnStartup() {
        if (!isInitialized) {
            initializeServers();
            isInitialized = true;
        }
    }

    /**
     * 서버 목록 초기화 (수동 호출 가능)
     */
    public boolean initializeServers() {
        try {
            // DFO API에서 서버 목록 조회
            Object serverList = dfoApiService.getServers();
            
            if (serverList != null) {
                // 서버 목록을 DB에 저장
                saveServersToDatabase(serverList);
                
                // 캐시에 저장 (무제한)
                cachingService.put(
                    CachingService.getServerListKey(), 
                    serverList, 
                    CachingService.CacheType.SERVER_LIST
                );
                
                System.out.println("서버 목록 초기화 완료");
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("서버 목록 초기화 실패: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * 서버 목록을 데이터베이스에 저장
     */
    private void saveServersToDatabase(Object serverList) {
        try {
            // DFO API 응답을 파싱하여 서버 정보 추출
            // 실제 구현에서는 JSON 파싱 로직 필요
            List<Map<String, Object>> servers = parseServerList(serverList);
            
            for (Map<String, Object> server : servers) {
                String serverId = (String) server.get("serverId");
                String serverName = (String) server.get("serverName");
                
                if (serverId != null && serverName != null) {
                    // 이미 존재하는지 확인
                    List<Adventure> existingServers = adventureRepository.findByServerId(serverId);
                    
                    if (existingServers.isEmpty()) {
                        // 새 서버 정보 생성
                        Adventure newServer = new Adventure(serverName, serverId);
                        adventureRepository.save(newServer);
                        System.out.println("새 서버 추가: " + serverName + " (" + serverId + ")");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("서버 정보 DB 저장 실패: " + e.getMessage());
        }
    }

    /**
     * DFO API 응답에서 서버 목록 파싱
     * 실제 구현에서는 JSON 파싱 로직 필요
     */
    private List<Map<String, Object>> parseServerList(Object serverList) {
        // TODO: 실제 DFO API 응답 구조에 맞게 JSON 파싱 구현
        // 현재는 Mock 데이터 반환
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
     * 서버 목록 조회 (캐시 또는 DB에서)
     */
    public Object getServerList() {
        // 먼저 캐시에서 확인
        Object cachedServers = cachingService.get(CachingService.getServerListKey());
        if (cachedServers != null) {
            return cachedServers;
        }
        
        // 캐시에 없으면 DB에서 조회
        List<Adventure> servers = adventureRepository.findAll();
        if (!servers.isEmpty()) {
            // DB 데이터를 캐시에 저장
            Object serverList = convertToServerList(servers);
            cachingService.put(
                CachingService.getServerListKey(), 
                serverList, 
                CachingService.CacheType.SERVER_LIST
            );
            return serverList;
        }
        
        // DB에도 없으면 초기화 실행
        if (initializeServers()) {
            return cachingService.get(CachingService.getServerListKey());
        }
        
        return null;
    }

    /**
     * DB 엔티티를 서버 목록 형태로 변환
     */
    private Object convertToServerList(List<Adventure> adventures) {
        // TODO: 실제 DFO API 응답 형태로 변환
        return adventures.stream()
            .map(adventure -> Map.of(
                "serverId", adventure.getServerId(),
                "serverName", adventure.getAdventureName()
            ))
            .toList();
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
