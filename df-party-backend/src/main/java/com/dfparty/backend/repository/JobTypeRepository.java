package com.dfparty.backend.repository;

import com.dfparty.backend.entity.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobTypeRepository extends JpaRepository<JobType, Long> {
    
    /**
     * 직업명으로 직업 타입 조회
     */
    Optional<JobType> findByJobName(String jobName);
    
    /**
     * 직업명과 직업성장명으로 직업 타입 조회
     */
    Optional<JobType> findByJobNameAndJobGrowName(String jobName, String jobGrowName);
    
    /**
     * 버퍼 직업들 조회
     */
    List<JobType> findByIsBufferTrue();
    
    /**
     * 딜러 직업들 조회
     */
    List<JobType> findByIsDealerTrue();
    
    /**
     * 직업명으로 버퍼 여부 확인
     */
    @Query("SELECT jt.isBuffer FROM JobType jt WHERE jt.jobName = :jobName")
    Optional<Boolean> findIsBufferByJobName(@Param("jobName") String jobName);
    
    /**
     * 직업명과 직업성장명으로 버퍼 여부 확인
     */
    @Query("SELECT jt.isBuffer FROM JobType jt WHERE jt.jobName = :jobName AND jt.jobGrowName = :jobGrowName")
    Optional<Boolean> findIsBufferByJobNameAndJobGrowName(@Param("jobName") String jobName, @Param("jobGrowName") String jobGrowName);
}
