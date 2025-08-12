package com.dfparty.backend.service;

import com.dfparty.backend.dto.SharedPartyDto;
import com.dfparty.backend.model.SharedParty;
import com.dfparty.backend.repository.SharedPartyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedPartyService {
    
    private final SharedPartyRepository sharedPartyRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 공유 파티 생성
     */
    public SharedPartyDto createSharedParty(SharedPartyDto requestDto) {
        try {
            // 고유한 공유 코드 생성
            String shareCode = generateUniqueShareCode();
            
            SharedParty sharedParty = SharedParty.builder()
                .shareCode(shareCode)
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .dungeonName(requestDto.getDungeonName())
                .partySize(requestDto.getPartySize())
                .partyData(convertToJson(requestDto.getPartyData()))
                .creatorName(requestDto.getCreatorName())
                .tags(convertToJson(requestDto.getTags()))
                .build();
            
            SharedParty saved = sharedPartyRepository.save(sharedParty);
            log.info("공유 파티 생성 완료: {} - {}", shareCode, requestDto.getTitle());
            
            return convertToDto(saved);
            
        } catch (Exception e) {
            log.error("공유 파티 생성 실패", e);
            throw new RuntimeException("공유 파티 생성에 실패했습니다.", e);
        }
    }
    
    /**
     * 공유 코드로 파티 조회
     */
    public Optional<SharedPartyDto> getSharedPartyByCode(String shareCode) {
        try {
            Optional<SharedParty> sharedParty = sharedPartyRepository.findByShareCodeAndIsActiveTrue(shareCode);
            
            if (sharedParty.isPresent()) {
                SharedParty party = sharedParty.get();
                
                // 만료 체크
                if (party.getExpiresAt().isBefore(LocalDateTime.now())) {
                    log.info("만료된 공유 파티: {}", shareCode);
                    return Optional.empty();
                }
                
                // 조회 횟수 증가
                party.setViewCount(party.getViewCount() + 1);
                sharedPartyRepository.save(party);
                
                return Optional.of(convertToDto(party));
            }
            
        } catch (Exception e) {
            log.error("공유 파티 조회 실패: {}", shareCode, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * 던전별 공유 파티 목록 조회
     */
    public List<SharedPartyDto> getSharedPartiesByDungeon(String dungeonName) {
        try {
            List<SharedParty> parties = sharedPartyRepository.findByDungeonNameAndIsActiveTrueOrderByCreatedAtDesc(dungeonName);
            return parties.stream()
                .filter(party -> !party.getExpiresAt().isBefore(LocalDateTime.now()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("던전별 공유 파티 조회 실패: {}", dungeonName, e);
            return List.of();
        }
    }
    
    /**
     * 파티 크기별 공유 파티 목록 조회
     */
    public List<SharedPartyDto> getSharedPartiesBySize(Integer partySize) {
        try {
            List<SharedParty> parties = sharedPartyRepository.findByPartySizeAndIsActiveTrueOrderByCreatedAtDesc(partySize);
            return parties.stream()
                .filter(party -> !party.getExpiresAt().isBefore(LocalDateTime.now()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("파티 크기별 공유 파티 조회 실패: {}", partySize, e);
            return List.of();
        }
    }
    
    /**
     * 인기 공유 파티 목록 조회
     */
    public List<SharedPartyDto> getPopularSharedParties() {
        try {
            List<SharedParty> parties = sharedPartyRepository.findByIsActiveTrueOrderByViewCountDesc();
            return parties.stream()
                .filter(party -> !party.getExpiresAt().isBefore(LocalDateTime.now()))
                .limit(10) // 상위 10개
                .map(this::convertToDto)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("인기 공유 파티 조회 실패", e);
            return List.of();
        }
    }
    
    /**
     * 최근 공유 파티 목록 조회
     */
    public List<SharedPartyDto> getRecentSharedParties() {
        try {
            List<SharedParty> parties = sharedPartyRepository.findByIsActiveTrueOrderByCreatedAtDesc();
            return parties.stream()
                .filter(party -> !party.getExpiresAt().isBefore(LocalDateTime.now()))
                .limit(20) // 최근 20개
                .map(this::convertToDto)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("최근 공유 파티 조회 실패", e);
            return List.of();
        }
    }
    
    /**
     * 키워드로 공유 파티 검색
     */
    public List<SharedPartyDto> searchSharedParties(String keyword) {
        try {
            List<SharedParty> parties = sharedPartyRepository.searchByKeyword(keyword);
            return parties.stream()
                .filter(party -> !party.getExpiresAt().isBefore(LocalDateTime.now()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("공유 파티 검색 실패: {}", keyword, e);
            return List.of();
        }
    }
    
    /**
     * 공유 파티 삭제 (비활성화)
     */
    public boolean deactivateSharedParty(String shareCode) {
        try {
            Optional<SharedParty> sharedParty = sharedPartyRepository.findByShareCodeAndIsActiveTrue(shareCode);
            
            if (sharedParty.isPresent()) {
                SharedParty party = sharedParty.get();
                party.setIsActive(false);
                sharedPartyRepository.save(party);
                
                log.info("공유 파티 비활성화: {}", shareCode);
                return true;
            }
            
        } catch (Exception e) {
            log.error("공유 파티 비활성화 실패: {}", shareCode, e);
        }
        
        return false;
    }
    
    /**
     * 만료된 공유 파티 정리
     */
    public void cleanupExpiredParties() {
        try {
            List<SharedParty> expiredParties = sharedPartyRepository.findAll().stream()
                .filter(party -> party.getExpiresAt().isBefore(LocalDateTime.now()))
                .collect(Collectors.toList());
            
            for (SharedParty party : expiredParties) {
                party.setIsActive(false);
                sharedPartyRepository.save(party);
            }
            
            log.info("만료된 공유 파티 {}개 정리 완료", expiredParties.size());
            
        } catch (Exception e) {
            log.error("만료된 공유 파티 정리 실패", e);
        }
    }
    
    // 유틸리티 메서드들
    private String generateUniqueShareCode() {
        Random random = new Random();
        String shareCode;
        
        do {
            // 6자리 숫자 코드 생성
            shareCode = String.format("%06d", random.nextInt(1000000));
        } while (sharedPartyRepository.findByShareCodeAndIsActiveTrue(shareCode).isPresent());
        
        return shareCode;
    }
    
    private SharedPartyDto convertToDto(SharedParty sharedParty) {
        try {
            return SharedPartyDto.builder()
                .id(sharedParty.getId())
                .shareCode(sharedParty.getShareCode())
                .title(sharedParty.getTitle())
                .description(sharedParty.getDescription())
                .dungeonName(sharedParty.getDungeonName())
                .partySize(sharedParty.getPartySize())
                .partyData(parseJson(sharedParty.getPartyData()))
                .creatorName(sharedParty.getCreatorName())
                .createdAt(sharedParty.getCreatedAt())
                .expiresAt(sharedParty.getExpiresAt())
                .viewCount(sharedParty.getViewCount())
                .isActive(sharedParty.getIsActive())
                .tags(parseJsonList(sharedParty.getTags()))
                .build();
                
        } catch (Exception e) {
            log.error("SharedParty를 DTO로 변환 실패", e);
            return null;
        }
    }
    
    private String convertToJson(Object obj) {
        try {
            if (obj == null) return "{}";
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON 변환 실패", e);
            return "{}";
        }
    }
    
    private Map<String, Object> parseJson(String json) {
        try {
            if (json == null || json.isEmpty()) return Map.of();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("JSON 파싱 실패", e);
            return Map.of();
        }
    }
    
    private List<String> parseJsonList(String json) {
        try {
            if (json == null || json.isEmpty()) return List.of();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("JSON 리스트 파싱 실패", e);
            return List.of();
        }
    }
}
