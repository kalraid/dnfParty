package com.dfparty.backend.service;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.repository.CharacterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

@Slf4j
@Service
public class CharacterSyncSchedulerService {

    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private DundamService dundamService;
    
    @Value("${dundam.scheduler.enabled:false}")
    private boolean schedulerEnabled;
    
    @Value("${dundam.sync.interval:60000}")
    private long syncInterval;

    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private List<Character> charactersToSync = null;
    private LocalDateTime lastFullSync = null;

    /**
     * 1시간마다 전체 캐릭터 목록을 새로 로드
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 1시간마다
    public void reloadCharacterList() {
        if (!schedulerEnabled) {
            log.debug("던담 스케줄러가 비활성화되어 있습니다.");
            return;
        }
        try {
            log.info("캐릭터 동기화 목록 새로 로드 시작");
            
            // DB에서 모든 캐릭터 조회 (최근 업데이트 순으로 정렬)
            charactersToSync = characterRepository.findAllByOrderByLastStatsUpdateAsc();
            currentIndex.set(0);
            lastFullSync = LocalDateTime.now();
            
            log.info("캐릭터 동기화 목록 로드 완료: {}개 캐릭터", charactersToSync.size());
            
        } catch (Exception e) {
            log.error("캐릭터 동기화 목록 로드 실패", e);
        }
    }

    /**
     * 설정된 간격으로 한 캐릭터씩 동기화
     */
    @Scheduled(fixedRateString = "${dundam.sync.interval:60000}") // 설정된 간격 (기본값: 1분)
    public void syncNextCharacter() {
        if (!schedulerEnabled) {
            log.debug("던담 스케줄러가 비활성화되어 있습니다.");
            return;
        }
        try {
            if (charactersToSync == null || charactersToSync.isEmpty()) {
                log.debug("동기화할 캐릭터가 없습니다.");
                return;
            }

            // 현재 인덱스의 캐릭터 동기화
            int index = currentIndex.getAndIncrement() % charactersToSync.size();
            Character character = charactersToSync.get(index);
            
            log.info("캐릭터 동기화 시작: {} ({}/{})", 
                character.getCharacterName(), index + 1, charactersToSync.size());

            // Dundam에서 정보 동기화
            try {
                Map<String, Object> dundamInfo = dundamService.getCharacterInfo(
                    character.getServerId(),
                    character.getCharacterId()
                );

                if (dundamInfo != null && !dundamInfo.isEmpty() && dundamInfo.get("success") == Boolean.TRUE) {
                    // 성공한 경우에만 동기화된 정보로 업데이트
                    updateCharacterWithDundamInfo(character, dundamInfo);
                    characterRepository.save(character);
                    
                    log.info("캐릭터 동기화 완료: {} - 버프력: {}, 총딜: {}", 
                        character.getCharacterName(),
                        dundamInfo.get("buffPower"),
                        dundamInfo.get("totalDamage"));
                } else {
                    String message = dundamInfo != null ? (String) dundamInfo.get("message") : "Dundam 정보 없음";
                    log.warn("캐릭터 동기화 실패: {} - {}", character.getCharacterName(), message);
                }

            } catch (Exception e) {
                log.error("캐릭터 동기화 중 오류 발생: {} - {}", character.getCharacterName(), e.getMessage());
            }

            // 다음 동기화까지 대기 (1분)
            log.debug("다음 캐릭터 동기화까지 대기 중...");

        } catch (Exception e) {
            log.error("캐릭터 동기화 스케줄러 오류", e);
        }
    }

    /**
     * Dundam 정보로 캐릭터 업데이트
     */
    private void updateCharacterWithDundamInfo(Character character, Map<String, Object> dundamInfo) {
        try {
            // 기본 정보 업데이트
            if (dundamInfo.containsKey("buffPower")) {
                character.setBuffPower((Long) dundamInfo.get("buffPower"));
            }
            if (dundamInfo.containsKey("totalDamage")) {
                character.setTotalDamage((Long) dundamInfo.get("totalDamage"));
            }



            // 소스 및 업데이트 시간 설정
            character.setDundamSource("dundam_sync");
            character.setLastStatsUpdate(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Dundam 정보로 캐릭터 업데이트 실패: {}", e.getMessage());
        }
    }

    /**
     * 동기화 상태 조회
     */
    public Map<String, Object> getSyncStatus() {
        return Map.of(
            "schedulerEnabled", schedulerEnabled,
            "isRunning", schedulerEnabled && charactersToSync != null && !charactersToSync.isEmpty(),
            "totalCharacters", charactersToSync != null ? charactersToSync.size() : 0,
            "currentIndex", currentIndex.get(),
            "lastFullSync", lastFullSync,
            "nextSyncIn", schedulerEnabled ? (syncInterval / 1000) + "초 후" : "비활성화됨",
            "syncInterval", syncInterval
        );
    }

    /**
     * 수동으로 동기화 시작
     */
    public void startManualSync() {
        if (!schedulerEnabled) {
            log.warn("던담 스케줄러가 비활성화되어 있어 수동 동기화를 시작할 수 없습니다.");
            return;
        }
        log.info("수동 동기화 시작");
        reloadCharacterList();
    }
}
