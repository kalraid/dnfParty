package com.dfparty.backend.controller;

import com.dfparty.backend.dto.ServerDto;
import com.dfparty.backend.dto.CharacterDto;
import com.dfparty.backend.dto.CharacterDetailDto;
import com.dfparty.backend.service.DfoApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dfo")
@CrossOrigin(origins = "http://localhost:5173")
public class DfoApiController {

    @Autowired
    private DfoApiService dfoApiService;

    @GetMapping("/servers")
    public ResponseEntity<List<ServerDto>> getServers() {
        try {
            List<ServerDto> servers = dfoApiService.getServers();
            return ResponseEntity.ok(servers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/characters/search")
    public ResponseEntity<List<CharacterDto>> searchCharacters(
            @RequestParam String serverId,
            @RequestParam String characterName,
            @RequestParam(required = false) String jobId,
            @RequestParam(required = false) String jobGrowId,
            @RequestParam(defaultValue = "true") boolean isAllJobGrow,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "match") String wordType) {
        try {
            List<CharacterDto> characters = dfoApiService.searchCharacters(
                serverId, characterName, jobId, jobGrowId, isAllJobGrow, limit, wordType);
            return ResponseEntity.ok(characters);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/characters/{serverId}/{characterId}")
    public ResponseEntity<CharacterDetailDto> getCharacterDetail(
            @PathVariable String serverId,
            @PathVariable String characterId) {
        try {
            CharacterDetailDto characterDetail = dfoApiService.getCharacterDetail(serverId, characterId);
            return ResponseEntity.ok(characterDetail);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/characters/{serverId}/{characterId}/timeline")
    public ResponseEntity<Object> getCharacterTimeline(
            @PathVariable String serverId,
            @PathVariable String characterId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String next) {
        try {
            Object timeline = dfoApiService.getCharacterTimeline(
                serverId, characterId, limit, code, startDate, endDate, next);
            return ResponseEntity.ok(timeline);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
