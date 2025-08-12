package com.dfparty.backend.service;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.entity.Adventure;
import com.dfparty.backend.repository.CharacterRepository;
import com.dfparty.backend.repository.AdventureRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DatabaseMigrationService {

    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private AdventureRepository adventureRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 기존 Local Storage 데이터를 데이터베이스로 마이그레이션
     * 프론트엔드에서 Local Storage 데이터를 JSON 형태로 전송받아 처리
     */
    public Map<String, Object> migrateFromLocalStorage(List<Map<String, Object>> localCharacters) {
        Map<String, Object> result = Map.of(
            "success", true,
            "message", "마이그레이션이 시작되었습니다.",
            "totalCharacters", localCharacters.size()
        );

        try {
            int migratedCount = 0;
            int errorCount = 0;

            for (Map<String, Object> localChar : localCharacters) {
                try {
                    if (migrateCharacter(localChar)) {
                        migratedCount++;
                    } else {
                        errorCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    // 로그 기록
                    System.err.println("캐릭터 마이그레이션 실패: " + e.getMessage());
                }
            }

            // 모험단 통계 업데이트
            updateAdventureStats();

            return Map.of(
                "success", true,
                "message", "마이그레이션이 완료되었습니다.",
                "totalCharacters", localCharacters.size(),
                "migratedCount", migratedCount,
                "errorCount", errorCount
            );

        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "마이그레이션 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 개별 캐릭터 마이그레이션
     */
    private boolean migrateCharacter(Map<String, Object> localChar) {
        try {
            String characterId = (String) localChar.get("characterId");
            String characterName = (String) localChar.get("characterName");
            String serverId = (String) localChar.get("serverId");
            String adventureName = (String) localChar.get("adventureName");

            // 필수 필드 검증
            if (characterId == null || characterName == null || serverId == null) {
                return false;
            }

            // 이미 존재하는지 확인
            Optional<Character> existingChar = characterRepository.findByCharacterId(characterId);
            if (existingChar.isPresent()) {
                // 기존 데이터 업데이트
                updateExistingCharacter(existingChar.get(), localChar);
                return true;
            }

            // 새 캐릭터 생성
            Character newCharacter = createNewCharacter(localChar);
            characterRepository.save(newCharacter);

            // 모험단 정보 생성/업데이트
            ensureAdventureExists(serverId, adventureName);

            return true;

        } catch (Exception e) {
            System.err.println("캐릭터 마이그레이션 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 기존 캐릭터 정보 업데이트
     */
    private void updateExistingCharacter(Character existingChar, Map<String, Object> localChar) {
        // Local Storage의 최신 정보로 업데이트
        if (localChar.containsKey("fame")) {
            existingChar.setFame(((Number) localChar.get("fame")).longValue());
        }
        
        if (localChar.containsKey("buffPower")) {
            existingChar.setBuffPower(((Number) localChar.get("buffPower")).longValue());
        }
        
        if (localChar.containsKey("totalDamage")) {
            existingChar.setTotalDamage(((Number) localChar.get("totalDamage")).longValue());
        }
        
        if (localChar.containsKey("level")) {
            existingChar.setLevel(((Number) localChar.get("level")).intValue());
        }

        existingChar.setUpdatedAt(LocalDateTime.now());
        characterRepository.save(existingChar);
    }

    /**
     * 새 캐릭터 엔티티 생성
     */
    private Character createNewCharacter(Map<String, Object> localChar) {
        Character character = new Character(
            (String) localChar.get("characterId"),
            (String) localChar.get("characterName"),
            (String) localChar.get("serverId")
        );

        // 기본 정보 설정
        if (localChar.containsKey("adventureName")) {
            character.setAdventureName((String) localChar.get("adventureName"));
        }
        
        if (localChar.containsKey("fame")) {
            character.setFame(((Number) localChar.get("fame")).longValue());
        }
        
        if (localChar.containsKey("buffPower")) {
            character.setBuffPower(((Number) localChar.get("buffPower")).longValue());
        }
        
        if (localChar.containsKey("totalDamage")) {
            character.setTotalDamage(((Number) localChar.get("totalDamage")).longValue());
        }
        
        if (localChar.containsKey("level")) {
            character.setLevel(((Number) localChar.get("level")).intValue());
        }

        // 저장 시간 설정
        if (localChar.containsKey("savedAt")) {
            try {
                LocalDateTime savedAt = LocalDateTime.parse((String) localChar.get("savedAt"));
                character.setCreatedAt(savedAt);
                character.setUpdatedAt(savedAt);
            } catch (Exception e) {
                // 파싱 실패 시 현재 시간 사용
                character.setCreatedAt(LocalDateTime.now());
                character.setUpdatedAt(LocalDateTime.now());
            }
        }

        return character;
    }

    /**
     * 모험단 정보가 존재하는지 확인하고 없으면 생성
     */
    private void ensureAdventureExists(String serverId, String adventureName) {
        if (adventureName == null || adventureName.trim().isEmpty()) {
            return;
        }

        Optional<Adventure> existingAdventure = adventureRepository.findByServerIdAndAdventureName(serverId, adventureName);
        if (existingAdventure.isEmpty()) {
            Adventure newAdventure = new Adventure(adventureName, serverId);
            adventureRepository.save(newAdventure);
        }
    }

    /**
     * 모험단 통계 업데이트
     */
    private void updateAdventureStats() {
        List<Adventure> allAdventures = adventureRepository.findAll();
        
        for (Adventure adventure : allAdventures) {
            try {
                // 해당 모험단의 캐릭터 수와 통계 계산
                List<Character> characters = characterRepository.findByServerIdAndAdventureName(
                    adventure.getServerId(), 
                    adventure.getAdventureName()
                );

                int characterCount = characters.size();
                long totalFame = characters.stream()
                    .mapToLong(c -> c.getFame() != null ? c.getFame() : 0L)
                    .sum();
                double averageLevel = characters.stream()
                    .mapToDouble(c -> c.getLevel() != null ? c.getLevel() : 0.0)
                    .average()
                    .orElse(0.0);

                adventure.updateStats(characterCount, totalFame, averageLevel);
                adventureRepository.save(adventure);

            } catch (Exception e) {
                System.err.println("모험단 통계 업데이트 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 마이그레이션 상태 확인
     */
    public Map<String, Object> getMigrationStatus() {
        long totalCharacters = characterRepository.count();
        long totalAdventures = adventureRepository.count();
        
        return Map.of(
            "success", true,
            "totalCharacters", totalCharacters,
            "totalAdventures", totalAdventures,
            "lastUpdated", LocalDateTime.now()
        );
    }
}
