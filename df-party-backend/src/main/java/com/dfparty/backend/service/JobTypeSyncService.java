package com.dfparty.backend.service;

import com.dfparty.backend.entity.JobType;
import com.dfparty.backend.repository.JobTypeRepository;
import com.dfparty.backend.utils.CharacterUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class JobTypeSyncService {

    @Value("${df.api.base-url:https://api.neople.co.kr/df}")
    private String baseUrl;

    @Value("${df.api.key:}")
    private String apiKey;

    @Autowired
    private JobTypeRepository jobTypeRepository;

    @Autowired
    private CharacterUtils characterUtils;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * DFO API에서 직업 목록을 가져와서 job_types 테이블에 동기화
     */
    public void syncJobTypesFromDfoApi() {
        try {
            log.info("DFO API에서 직업 정보 동기화 시작");
            
            if (apiKey == null || apiKey.isEmpty()) {
                log.error("DFO API 키가 설정되지 않음");
                return;
            }

            // DFO API에서 직업 목록 조회
            List<JobType> jobTypes = fetchJobTypesFromDfoApi();
            
            if (jobTypes.isEmpty()) {
                log.warn("DFO API에서 직업 정보를 가져올 수 없음");
                return;
            }

            // 기존 데이터 삭제
            jobTypeRepository.deleteAll();
            log.info("기존 job_types 데이터 삭제 완료");

            // 새로운 데이터 저장
            for (JobType jobType : jobTypes) {
                jobTypeRepository.save(jobType);
            }

            log.info("직업 정보 동기화 완료: {}개 직업", jobTypes.size());

        } catch (Exception e) {
            log.error("직업 정보 동기화 중 오류 발생", e);
        }
    }

    /**
     * DFO API에서 직업 목록 조회
     */
    private List<JobType> fetchJobTypesFromDfoApi() throws Exception {
        List<JobType> jobTypes = new ArrayList<>();

        // DFO API 직업 목록 엔드포인트
        String url = UriComponentsBuilder
            .fromHttpUrl(baseUrl + "/jobs")
            .queryParam("apikey", apiKey)
            .build()
            .toUriString();

        log.info("DFO API 직업 목록 조회: {}", url);

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            
            if (root.has("jobs")) {
                JsonNode jobsNode = root.get("jobs");
                for (JsonNode jobNode : jobsNode) {
                    String jobId = jobNode.path("jobId").asText();
                    String jobName = jobNode.path("jobName").asText();
                    
                    // 각 직업의 성장 직업들 조회
                    List<JobType> growthJobs = fetchJobGrowthFromDfoApi(jobId, jobName);
                    jobTypes.addAll(growthJobs);
                }
            }
        }

        return jobTypes;
    }

    /**
     * DFO API에서 특정 직업의 성장 직업들 조회
     */
    private List<JobType> fetchJobGrowthFromDfoApi(String jobId, String jobName) throws Exception {
        List<JobType> growthJobs = new ArrayList<>();

        String url = UriComponentsBuilder
            .fromHttpUrl(baseUrl + "/jobs/" + jobId + "/jobGrows")
            .queryParam("apikey", apiKey)
            .build()
            .toUriString();

        log.info("DFO API 성장 직업 조회: {}", url);

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            
            if (root.has("jobGrows")) {
                JsonNode jobGrowsNode = root.get("jobGrows");
                for (JsonNode growthNode : jobGrowsNode) {
                    String jobGrowId = growthNode.path("jobGrowId").asText();
                    String jobGrowName = growthNode.path("jobGrowName").asText();
                    
                    // CharacterUtils를 사용하여 버퍼/딜러 판별
                    boolean isBuffer = characterUtils.isBuffer(jobName, jobGrowName);
                    boolean isDealer = !isBuffer;
                    
                    JobType jobType = JobType.builder()
                        .jobName(jobName)
                        .jobGrowName(jobGrowName)
                        .isBuffer(isBuffer)
                        .isDealer(isDealer)
                        .build();
                    
                    growthJobs.add(jobType);
                    
                    log.info("직업 정보 생성: {} - {} (버퍼: {}, 딜러: {})", 
                        jobName, jobGrowName, isBuffer, isDealer);
                }
            }
        }

        return growthJobs;
    }

    /**
     * 특정 직업의 버퍼/딜러 여부 업데이트
     */
    public void updateJobTypeBufferStatus(String jobName, String jobGrowName) {
        try {
            Optional<JobType> existingJobType = jobTypeRepository.findByJobNameAndJobGrowName(jobName, jobGrowName);
            
            if (existingJobType.isPresent()) {
                JobType jobType = existingJobType.get();
                boolean isBuffer = characterUtils.isBuffer(jobName, jobGrowName);
                boolean isDealer = !isBuffer;
                
                jobType.setIsBuffer(isBuffer);
                jobType.setIsDealer(isDealer);
                
                jobTypeRepository.save(jobType);
                
                log.info("직업 버퍼 상태 업데이트: {} - {} (버퍼: {}, 딜러: {})", 
                    jobName, jobGrowName, isBuffer, isDealer);
            } else {
                // 새로운 직업 타입 생성
                boolean isBuffer = characterUtils.isBuffer(jobName, jobGrowName);
                boolean isDealer = !isBuffer;
                
                JobType newJobType = JobType.builder()
                    .jobName(jobName)
                    .jobGrowName(jobGrowName)
                    .isBuffer(isBuffer)
                    .isDealer(isDealer)
                    .build();
                
                jobTypeRepository.save(newJobType);
                
                log.info("새 직업 타입 생성: {} - {} (버퍼: {}, 딜러: {})", 
                    jobName, jobGrowName, isBuffer, isDealer);
            }
        } catch (Exception e) {
            log.error("직업 버퍼 상태 업데이트 중 오류 발생: {} - {}", jobName, jobGrowName, e);
        }
    }

    /**
     * 모든 직업의 버퍼/딜러 상태를 CharacterUtils 기준으로 재계산
     */
    public void recalculateAllJobTypeBufferStatus() {
        try {
            log.info("모든 직업의 버퍼/딜러 상태 재계산 시작");
            
            List<JobType> allJobTypes = jobTypeRepository.findAll();
            int updatedCount = 0;
            
            for (JobType jobType : allJobTypes) {
                boolean isBuffer = characterUtils.isBuffer(jobType.getJobName(), jobType.getJobGrowName());
                boolean isDealer = !isBuffer;
                
                // 상태가 변경된 경우에만 업데이트
                if (jobType.getIsBuffer() != isBuffer || jobType.getIsDealer() != isDealer) {
                    jobType.setIsBuffer(isBuffer);
                    jobType.setIsDealer(isDealer);
                    jobTypeRepository.save(jobType);
                    updatedCount++;
                    
                    log.info("직업 상태 업데이트: {} - {} (버퍼: {}, 딜러: {})", 
                        jobType.getJobName(), jobType.getJobGrowName(), isBuffer, isDealer);
                }
            }
            
            log.info("직업 상태 재계산 완료: {}개 업데이트", updatedCount);
            
        } catch (Exception e) {
            log.error("직업 상태 재계산 중 오류 발생", e);
        }
    }
}