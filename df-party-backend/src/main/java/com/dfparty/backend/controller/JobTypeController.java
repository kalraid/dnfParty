package com.dfparty.backend.controller;

import com.dfparty.backend.service.JobTypeSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/job-types")
public class JobTypeController {

    @Autowired
    private JobTypeSyncService jobTypeSyncService;

    /**
     * DFO API에서 직업 정보를 가져와서 job_types 테이블에 동기화
     */
    @PostMapping("/sync-from-dfo")
    public ResponseEntity<Map<String, Object>> syncJobTypesFromDfo() {
        try {
            log.info("DFO API에서 직업 정보 동기화 요청");
            
            jobTypeSyncService.syncJobTypesFromDfoApi();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "DFO API에서 직업 정보 동기화가 완료되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("직업 정보 동기화 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "직업 정보 동기화 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 직업의 버퍼/딜러 상태 업데이트
     */
    @PostMapping("/update-buffer-status")
    public ResponseEntity<Map<String, Object>> updateJobTypeBufferStatus(
            @RequestParam String jobName,
            @RequestParam String jobGrowName) {
        try {
            log.info("직업 버퍼 상태 업데이트 요청: {} - {}", jobName, jobGrowName);
            
            jobTypeSyncService.updateJobTypeBufferStatus(jobName, jobGrowName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "직업 버퍼 상태가 업데이트되었습니다: " + jobName + " - " + jobGrowName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("직업 버퍼 상태 업데이트 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "직업 버퍼 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 모든 직업의 버퍼/딜러 상태를 CharacterUtils 기준으로 재계산
     */
    @PostMapping("/recalculate-buffer-status")
    public ResponseEntity<Map<String, Object>> recalculateAllJobTypeBufferStatus() {
        try {
            log.info("모든 직업의 버퍼/딜러 상태 재계산 요청");
            
            jobTypeSyncService.recalculateAllJobTypeBufferStatus();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모든 직업의 버퍼/딜러 상태가 재계산되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("직업 버퍼 상태 재계산 중 오류 발생", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "직업 버퍼 상태 재계산 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}