package com.dfparty.backend.controller;

import com.dfparty.backend.dto.SharedPartyDto;
import com.dfparty.backend.service.SharedPartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/shared-party")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SharedPartyController {
    
    private final SharedPartyService sharedPartyService;
    
    /**
     * 공유 파티 생성
     */
    @PostMapping
    public ResponseEntity<SharedPartyDto> createSharedParty(@RequestBody SharedPartyDto requestDto) {
        log.info("공유 파티 생성 요청: {}", requestDto.getTitle());
        
        try {
            SharedPartyDto created = sharedPartyService.createSharedParty(requestDto);
            return ResponseEntity.ok(created);
            
        } catch (Exception e) {
            log.error("공유 파티 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 공유 코드로 파티 조회
     */
    @GetMapping("/{shareCode}")
    public ResponseEntity<SharedPartyDto> getSharedParty(@PathVariable String shareCode) {
        log.info("공유 파티 조회: {}", shareCode);
        
        try {
            var sharedParty = sharedPartyService.getSharedPartyByCode(shareCode);
            
            if (sharedParty.isPresent()) {
                return ResponseEntity.ok(sharedParty.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("공유 파티 조회 실패: {}", shareCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 던전별 공유 파티 목록 조회
     */
    @GetMapping("/dungeon/{dungeonName}")
    public ResponseEntity<List<SharedPartyDto>> getSharedPartiesByDungeon(@PathVariable String dungeonName) {
        log.info("던전별 공유 파티 조회: {}", dungeonName);
        
        try {
            List<SharedPartyDto> parties = sharedPartyService.getSharedPartiesByDungeon(dungeonName);
            return ResponseEntity.ok(parties);
            
        } catch (Exception e) {
            log.error("던전별 공유 파티 조회 실패: {}", dungeonName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 파티 크기별 공유 파티 목록 조회
     */
    @GetMapping("/size/{partySize}")
    public ResponseEntity<List<SharedPartyDto>> getSharedPartiesBySize(@PathVariable Integer partySize) {
        log.info("파티 크기별 공유 파티 조회: {}", partySize);
        
        try {
            List<SharedPartyDto> parties = sharedPartyService.getSharedPartiesBySize(partySize);
            return ResponseEntity.ok(parties);
            
        } catch (Exception e) {
            log.error("파티 크기별 공유 파티 조회 실패: {}", partySize, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 인기 공유 파티 목록 조회
     */
    @GetMapping("/popular")
    public ResponseEntity<List<SharedPartyDto>> getPopularSharedParties() {
        log.info("인기 공유 파티 조회");
        
        try {
            List<SharedPartyDto> parties = sharedPartyService.getPopularSharedParties();
            return ResponseEntity.ok(parties);
            
        } catch (Exception e) {
            log.error("인기 공유 파티 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 최근 공유 파티 목록 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<List<SharedPartyDto>> getRecentSharedParties() {
        log.info("최근 공유 파티 조회");
        
        try {
            List<SharedPartyDto> parties = sharedPartyService.getRecentSharedParties();
            return ResponseEntity.ok(parties);
            
        } catch (Exception e) {
            log.error("최근 공유 파티 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 키워드로 공유 파티 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<SharedPartyDto>> searchSharedParties(@RequestParam String keyword) {
        log.info("공유 파티 검색: {}", keyword);
        
        try {
            List<SharedPartyDto> parties = sharedPartyService.searchSharedParties(keyword);
            return ResponseEntity.ok(parties);
            
        } catch (Exception e) {
            log.error("공유 파티 검색 실패: {}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 공유 파티 삭제 (비활성화)
     */
    @DeleteMapping("/{shareCode}")
    public ResponseEntity<Map<String, String>> deleteSharedParty(@PathVariable String shareCode) {
        log.info("공유 파티 삭제: {}", shareCode);
        
        try {
            boolean deleted = sharedPartyService.deactivateSharedParty(shareCode);
            
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "공유 파티가 삭제되었습니다."));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("공유 파티 삭제 실패: {}", shareCode, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "삭제 실패"));
        }
    }
    
    /**
     * 공유 파티 통계 정보
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSharedPartyStats() {
        log.info("공유 파티 통계 조회");
        
        try {
            // TODO: 통계 정보 구현
            Map<String, Object> stats = Map.of(
                "totalParties", 0,
                "activeParties", 0,
                "totalViews", 0,
                "popularDungeon", "나벨"
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("공유 파티 통계 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
