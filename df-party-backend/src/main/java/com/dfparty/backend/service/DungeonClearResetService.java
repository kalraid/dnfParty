package com.dfparty.backend.service;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DungeonClearResetService {

    private final CharacterRepository characterRepository;

    /**
     * 한국 표준시(KST) 기준 현재 시간 반환
     */
    private LocalDateTime getCurrentTimeKST() {
        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        return LocalDateTime.now(kstZone);
    }

    /**
     * 매주 목요일 오전 8시 모든 캐릭터의 던전 클리어 상태를 초기화 (KST 기준)
     * cron: 매주 목요일(4) 오전 8시 
     */
    @Scheduled(cron = "0 0 8 * * 4")
    @Transactional
    public void resetDungeonClearStatus() {
        try {
            log.info("목요일 오전 8시 (KST) - 던전 클리어 상태 초기화 시작");
            
            // 모든 캐릭터 조회
            List<Character> characters = characterRepository.findAll();
            int resetCount = 0;
            
            for (Character character : characters) {
                // 던전 클리어 상태 초기화
                character.setDungeonClearNabel(false);
                character.setDungeonClearVenus(false);
                character.setDungeonClearFog(false);
                character.setDungeonClearTwilight(false);
                
                // 마지막 던전 체크 시간 업데이트 (KST 기준)
                character.setLastDungeonCheck(getCurrentTimeKST());
                
                // 변경사항 저장
                characterRepository.save(character);
                resetCount++;


            }
            
            log.info("던전 클리어 상태 초기화 완료: {}개 캐릭터", resetCount);
            
        } catch (Exception e) {
            log.error("던전 클리어 상태 초기화 중 오류 발생", e);
        }
    }

    /**
     * 목요일인지 확인 (KST 기준)
     */
    public boolean isThursday() {
        return getCurrentTimeKST().getDayOfWeek() == DayOfWeek.THURSDAY;
    }

    /**
     * 목요일 오전 8시 전후인지 확인 (7시55분 ~ 8시05분, KST 기준)
     */
    public boolean isNearThursdayResetTime() {
        LocalDateTime now = getCurrentTimeKST();
        if (now.getDayOfWeek() != DayOfWeek.THURSDAY) {
            return false;
        }
        
        LocalTime currentTime = now.toLocalTime();
        LocalTime resetTime = LocalTime.of(8, 0);
        
        // 7시55분 ~ 8시05분 범위 내인지 확인 (KST 기준)
        return currentTime.isAfter(LocalTime.of(7, 55)) && 
               currentTime.isBefore(LocalTime.of(8, 5));
    }

    /**
     * 수동으로 던전 클리어 상태 초기화 (테스트용)
     */
    @Transactional
    public void manualResetDungeonClearStatus() {
        log.info("수동 던전 클리어 상태 초기화 시작 (KST 기준)");
        resetDungeonClearStatus();
    }
}
