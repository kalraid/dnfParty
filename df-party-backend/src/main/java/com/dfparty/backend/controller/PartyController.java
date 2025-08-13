package com.dfparty.backend.controller;

import com.dfparty.backend.service.PartyOptimizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/party")
@CrossOrigin(origins = "http://localhost:5173")
public class PartyController {

    @Autowired
    private PartyOptimizationService partyOptimizationService;

    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeParty(@RequestBody Map<String, Object> request) {
        try {
            // TODO: PartyOptimizationService에 optimizeParty 메서드 구현 필요
            return ResponseEntity.ok(Map.of("message", "파티 최적화 기능은 아직 구현되지 않았습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
