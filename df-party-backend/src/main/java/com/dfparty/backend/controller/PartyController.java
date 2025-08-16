package com.dfparty.backend.controller;

import com.dfparty.backend.service.PartyOptimizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/party")
@CrossOrigin(origins = "*")
public class PartyController {

    @Autowired
    private PartyOptimizationService partyOptimizationService;

    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeParty(@RequestBody Map<String, Object> request) {
        try {
            // 프론트엔드 호환 메서드 사용
            Map<String, Object> result = partyOptimizationService.optimizePartyForFrontend(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "파티 최적화 중 서버 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
