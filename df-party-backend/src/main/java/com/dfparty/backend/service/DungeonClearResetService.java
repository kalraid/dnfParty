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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DungeonClearResetService {

    private final CharacterRepository characterRepository;

    /**
     * 매주 목요일 오전 8시50분에 모든 캐릭터의 던전 클리어 상태를 초기화
     * cron: 매주 목요일(4) 오전 8시 50분
     */
    @Scheduled(cron = "0 50 8 * * 4")
    @Transactional
    public void resetDungeonClearStatus() {
        try {
            log.info("목요일 오전 8시50분 - 던전 클리어 상태 초기화 시작");
            
            // 모든 캐릭터 조회
            List<Character> characters = characterRepository.findAll();
            int resetCount = 0;
            
            for (Character character : characters) {
                // 던전 클리어 상태 초기화
                character.setDungeonClearNabel(false);
                character.setDungeonClearVenus(false);
                character.setDungeonClearFog(false);
                character.setDungeonClearNightmare(false);
                character.setDungeonClearTemple(false);
                character.setDungeonClearAzure(false);
                character.setDungeonClearStorm(false);
                
                // 업데이트 시간 설정
                character.setUpdatedAt(LocalDateTime.now());
                
                resetCount++;
            }
            
            // DB에 일괄 저장
            characterRepository.saveAll(characters);
            
            log.info("던전 클리어 상태 초기화 완료: {}개 캐릭터", resetCount);
            
        } catch (Exception e) {
            log.error("던전 클리어 상태 초기화 중 오류 발생", e);
        }
    }

    /**
     * 목요일인지 확인
     */
    public boolean isThursday() {
        return LocalDateTime.now().getDayOfWeek() == DayOfWeek.THURSDAY;
    }

    /**
     * 목요일 오전 8시50분 전후인지 확인 (8시45분 ~ 8시55분)
     */
    public boolean isNearThursdayResetTime() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getDayOfWeek() != DayOfWeek.THURSDAY) {
            return false;
        }
        
        LocalTime currentTime = now.toLocalTime();
        LocalTime resetTime = LocalTime.of(8, 50);
        
        // 8시45분 ~ 8시55분 범위 내인지 확인
        return currentTime.isAfter(LocalTime.of(8, 45)) && 
               currentTime.isBefore(LocalTime.of(8, 55));
    }

    /**
     * 수동으로 던전 클리어 상태 초기화 (테스트용)
     */
    @Transactional
    public void manualResetDungeonClearStatus() {
        log.info("수동 던전 클리어 상태 초기화 시작");
        resetDungeonClearStatus();
    }
}
