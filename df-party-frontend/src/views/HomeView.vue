<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="home">
    <div class="hero-section">
      <h1>던전앤파이터 파티 구성 사이트</h1>
      <p class="hero-subtitle">API를 활용한 자동 파티 구성 시스템</p>
    </div>

    <div class="features-section">
      <h2>주요 기능</h2>
      <div class="features-grid">
        <div class="feature-card">
          <h3>🔍 캐릭터 검색</h3>
          <p>서버별 캐릭터 검색 및 정보 수집</p>
          <RouterLink to="/character-search" class="feature-link">바로가기</RouterLink>
        </div>
        
        <div class="feature-card">
          <h3>📊 던전 클리어 현황</h3>
          <p>모험단별 캐릭터 관리 및 던전 클리어 현황</p>
          <RouterLink to="/dungeon-status" class="feature-link">바로가기</RouterLink>
        </div>
        
        <div class="feature-card">
          <h3>⚔️ 파티 짜기</h3>
          <p>자동/수동 파티 구성 및 최적화</p>
          <RouterLink to="/party-formation" class="feature-link">바로가기</RouterLink>
        </div>
        
        <div class="feature-card">
          <h3>📝 건의사항</h3>
          <p>수정이나 에러나 기타 건의사항 다는곳</p>
          <a href="https://docs.google.com/spreadsheets/d/1ehHEGMsI8v16-j7BvseDq_lTBLZm59U1FdksI3nQD_I/edit?usp=sharing" 
             target="_blank" 
             rel="noopener noreferrer" 
             class="feature-link">건의사항 작성</a>
        </div>
      </div>
    </div>


  </div>
</template>

<style scoped>
.home {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.hero-section {
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 12px;
  margin-bottom: 40px;
}

.hero-section h1 {
  font-size: 3rem;
  margin: 0 0 20px 0;
  font-weight: bold;
}

.hero-subtitle {
  font-size: 1.2rem;
  margin: 0;
  opacity: 0.9;
}

.features-section {
  margin-bottom: 40px;
}

.features-section h2 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 2rem;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.feature-card {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  text-align: center;
  transition: transform 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-5px);
}

.feature-card h3 {
  color: #333;
  margin: 0 0 15px 0;
  font-size: 1.3rem;
}

.feature-card p {
  color: #666;
  margin: 0 0 20px 0;
  line-height: 1.6;
}

.feature-link {
  display: inline-block;
  background: #007bff;
  color: white;
  text-decoration: none;
  padding: 10px 20px;
  border-radius: 6px;
  transition: background 0.3s ease;
}

.feature-link:hover {
  background: #0056b3;
}



/* ========================================
   세밀한 반응형 디자인 - 디바이스별 최적화
   ======================================== */

/* 대형 데스크탑 (1920px 이상) */
@media screen and (min-width: 1920px) {
  .home {
    max-width: 1600px;
    padding: 30px;
  }
  
  .hero-section h1 {
    font-size: 4rem;
  }
  
  .hero-subtitle {
    font-size: 1.5rem;
  }
  
  .features-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 30px;
  }
}

/* 데스크탑 (1600px ~ 1919px) */
@media screen and (min-width: 1600px) and (max-width: 1919px) {
  .home {
    max-width: 1400px;
    padding: 25px;
  }
  
  .hero-section h1 {
    font-size: 3.5rem;
  }
  
  .features-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 25px;
  }
}

/* 노트북 (1200px ~ 1599px) */
@media screen and (min-width: 1200px) and (max-width: 1599px) {
  .home {
    max-width: 1200px;
    padding: 20px;
  }
  
  .hero-section h1 {
    font-size: 3.2rem;
  }
  
  .features-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
  }
}

/* 태블릿 가로 (1024px ~ 1199px) */
@media screen and (min-width: 1024px) and (max-width: 1199px) {
  .home {
    padding: 18px;
  }
  
  .hero-section h1 {
    font-size: 3rem;
  }
  
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }
  
  .feature-card {
    padding: 25px;
  }
}

/* 태블릿 세로 (768px ~ 1023px) */
@media screen and (min-width: 768px) and (max-width: 1023px) {
  .home {
    padding: 15px;
  }
  
  .hero-section {
    padding: 40px 15px;
    margin-bottom: 30px;
  }
  
  .hero-section h1 {
    font-size: 2.8rem;
  }
  
  .hero-subtitle {
    font-size: 1.2rem;
  }
  
  .features-section h2 {
    font-size: 1.8rem;
    margin-bottom: 25px;
  }
  
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
  }
  
  .feature-card {
    padding: 22px 18px;
  }
  
  .feature-card h3 {
    font-size: 1.2rem;
  }
}

/* 중형 모바일 (600px ~ 767px) */
@media screen and (min-width: 600px) and (max-width: 767px) {
  .home {
    padding: 12px;
  }
  
  .hero-section {
    padding: 35px 15px;
    margin-bottom: 28px;
  }
  
  .hero-section h1 {
    font-size: 2.4rem;
  }
  
  .hero-subtitle {
    font-size: 1.1rem;
  }
  
  .features-section h2 {
    font-size: 1.7rem;
    margin-bottom: 22px;
  }
  
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  
  .feature-card {
    padding: 20px 16px;
  }
  
  .feature-card h3 {
    font-size: 1.1rem;
  }
}

/* 소형 모바일 (480px ~ 599px) */
@media screen and (min-width: 480px) and (max-width: 599px) {
  .home {
    padding: 10px;
  }
  
  .hero-section {
    padding: 30px 15px;
    margin-bottom: 25px;
  }
  
  .hero-section h1 {
    font-size: 2.2rem;
  }
  
  .hero-subtitle {
    font-size: 1rem;
  }
  
  .features-section h2 {
    font-size: 1.6rem;
    margin-bottom: 20px;
  }
  
  .features-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .feature-card {
    padding: 18px 15px;
  }
  
  .feature-card h3 {
    font-size: 1.1rem;
  }
  
  .feature-card p {
    font-size: 0.95rem;
  }
  
  .feature-link {
    padding: 9px 18px;
    font-size: 0.95rem;
  }
}

/* 초소형 모바일 (320px ~ 479px) */
@media screen and (min-width: 320px) and (max-width: 479px) {
  .home {
    padding: 8px;
  }
  
  .hero-section {
    padding: 25px 12px;
    margin-bottom: 20px;
  }
  
  .hero-section h1 {
    font-size: 1.8rem;
  }
  
  .hero-subtitle {
    font-size: 0.9rem;
  }
  
  .features-section h2 {
    font-size: 1.4rem;
    margin-bottom: 18px;
  }
  
  .features-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .feature-card {
    padding: 15px 12px;
  }
  
  .feature-card h3 {
    font-size: 1rem;
  }
  
  .feature-card p {
    font-size: 0.9rem;
  }
  
  .feature-link {
    padding: 8px 16px;
    font-size: 0.9rem;
  }
}

/* 터치 디바이스 최적화 */
@media (hover: none) and (pointer: coarse) {
  .feature-card:hover {
    transform: none;
  }
  
  .feature-link {
    min-height: 44px; /* 터치 최소 크기 */
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>

