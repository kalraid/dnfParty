package com.dfparty.backend.utils;

import org.springframework.stereotype.Component;
import com.dfparty.backend.entity.JobType;
import com.dfparty.backend.repository.JobTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * 캐릭터 관련 유틸리티 클래스
 * isBuffer 로직을 중앙화하여 관리
 */
@Component
public class CharacterUtils {

    @Autowired
    private JobTypeRepository jobTypeRepository;

    /**
     * 캐릭터가 버퍼인지 확인하는 통합 메소드
     * @param jobName 직업명
     * @param jobGrowName 직업 성장명
     * @return 버퍼 여부
     */
    public boolean isBuffer(String jobName, String jobGrowName) {
        // 1. DB에서 JobType 조회 시도
        if (jobName != null && jobGrowName != null) {
            Optional<JobType> jobType = jobTypeRepository.findByJobNameAndJobGrowName(jobName, jobGrowName);
            if (jobType.isPresent()) {
                return jobType.get().getIsBuffer();
            }
        }

        // 2. jobName만으로 조회 시도
        if (jobName != null) {
            Optional<JobType> jobType = jobTypeRepository.findByJobName(jobName);
            if (jobType.isPresent()) {
                return jobType.get().getIsBuffer();
            }
        }

        // 3. 기본값으로 판단 (DB에 없는 경우)
        return isBufferByDefault(jobGrowName != null ? jobGrowName : jobName);
    }

    /**
     * 기본 버퍼 판단 로직 (DB에 없는 경우 사용)
     * @param jobName 직업명
     * @return 버퍼 여부
     */
    private boolean isBufferByDefault(String jobName) {
        if (jobName == null) return false;
        
        String cleanJobName = cleanJobName(jobName);
        
        String[] bufferJobs = {
            "크루세이더", "뮤즈", "패러메딕", "인챈트리스", "헤카테"
        };
        
        for (String bufferJob : bufferJobs) {
            if (cleanJobName.contains(bufferJob)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 직업명 정리 (眞 문자 제거)
     * @param jobName 원본 직업명
     * @return 정리된 직업명
     */
    private String cleanJobName(String jobName) {
        if (jobName == null) return "";
        return jobName.replaceAll("眞\\s*", "").trim();
    }

    /**
     * 캐릭터가 딜러인지 확인
     * @param jobName 직업명
     * @param jobGrowName 직업 성장명
     * @return 딜러 여부
     */
    public boolean isDealer(String jobName, String jobGrowName) {
        return !isBuffer(jobName, jobGrowName);
    }
}