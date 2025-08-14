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
            
            Map<String, Object> result = partyOptimizationService.optimizeParty(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
