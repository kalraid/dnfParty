package com.dfparty.backend.controller;

import com.dfparty.backend.service.DungeonClearService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dungeon-clear")
@CrossOrigin(origins = "http://localhost:5173")
public class DungeonClearController {

    @Autowired
    private DungeonClearService dungeonClearService;

    @GetMapping("/{serverId}/{characterId}")
    public ResponseEntity<Map<String, Object>> getDungeonClearStatus(
            @PathVariable String serverId,
            @PathVariable String characterId) {
        try {
            Map<String, Object> result = dungeonClearService.getDungeonClearStatus(serverId, characterId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{serverId}/bulk")
    public ResponseEntity<Map<String, Object>> getBulkDungeonClearStatus(
            @PathVariable String serverId,
            @RequestBody String[] characterIds) {
        try {
            Map<String, Object> result = dungeonClearService.getBulkDungeonClearStatus(serverId, characterIds);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
