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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

			// DFO API에서 직업 목록 조회 (최종 직업만, 眞 제거, 버퍼 지정 규칙 적용)
			List<JobType> jobTypes = fetchFinalJobTypesFromDfoApi();

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
	 * DFO API에서 전체 직업을 가져와 각 1차 전직별 체인의 최종 직업(眞 제거)을 1건씩 생성
	 * 버퍼 여부는 인챈트리스/크루세이더/패러메딕/뮤즈 계열만 true 처리
	 */
	private List<JobType> fetchFinalJobTypesFromDfoApi() throws Exception {
		List<JobType> jobTypes = new ArrayList<>();

		String url = UriComponentsBuilder
			.fromHttpUrl(baseUrl + "/jobs")
			.queryParam("apikey", apiKey)
			.build()
			.toUriString();

		log.info("DFO API 직업 목록 조회: {}", url);

		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
			JsonNode root = objectMapper.readTree(response.getBody());
			if (root.has("rows")) {
				JsonNode jobsNode = root.get("rows");

				// 버퍼 계열 1차 전직명
				Set<String> bufferRoots = new HashSet<>();
				bufferRoots.add("인챈트리스");
				bufferRoots.add("크루세이더");
				bufferRoots.add("패러메딕");
				bufferRoots.add("뮤즈");

				for (JsonNode jobNode : jobsNode) {
					if (!jobNode.has("rows")) continue;
					JsonNode firstGrowthRows = jobNode.get("rows");

					for (JsonNode growthNode : firstGrowthRows) {
						// 이 체인의 최종 직업명(眞 제거) 추출
						String canonicalFinalName = extractCanonicalFinalName(growthNode);

						// 버퍼 여부: 1차 전직명이 버퍼 루트인지로 판정
						String firstGrowthName = cleanName(growthNode.path("jobGrowName").asText());
						boolean isBuffer = bufferRoots.contains(firstGrowthName);
						boolean isDealer = !isBuffer;

						// 중복키 방지: jobName(UNIQUE)에 최종 직업명 저장. jobGrowName도 동일 값으로 저장
						JobType jobType = JobType.builder()
							.jobName(canonicalFinalName)
							.jobGrowName(canonicalFinalName)
							.isBuffer(isBuffer)
							.isDealer(isDealer)
							.build();

						jobTypes.add(jobType);

						log.info("직업 정보 생성: {} (버퍼: {}, 딜러: {})", canonicalFinalName, isBuffer, isDealer);
					}
				}
			}
		}

		return jobTypes;
	}

	/**
	 * 체인의 마지막 노드의 jobGrowName을 가져와 眞 제거 후 반환
	 */
	private String extractCanonicalFinalName(JsonNode growthNode) {
		JsonNode current = growthNode;
		String name = current.path("jobGrowName").asText();
		while (current.has("next")) {
			current = current.get("next");
			name = current.path("jobGrowName").asText();
		}
		return cleanName(name);
	}

	private String cleanName(String name) {
		if (name == null) return "";
		return name.replaceAll("眞\\s*", "").trim();
	}

	/**
	 * 특정 직업의 버퍼/딜러 여부 업데이트 (기존 로직 유지)
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
	 * 모든 직업의 버퍼/딜러 상태를 CharacterUtils 기준으로 재계산 (기존 로직 유지)
	 */
	public void recalculateAllJobTypeBufferStatus() {
		try {
			log.info("모든 직업의 버퍼/딜러 상태 재계산 시작");

			List<JobType> allJobTypes = jobTypeRepository.findAll();
			int updatedCount = 0;

			for (JobType jobType : allJobTypes) {
				boolean isBuffer = characterUtils.isBuffer(jobType.getJobName(), jobType.getJobGrowName());
				boolean isDealer = !isBuffer;

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