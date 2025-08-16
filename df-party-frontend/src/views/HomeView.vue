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



@media (max-width: 768px) {
  .hero-section h1 {
    font-size: 2rem;
  }
  
  .hero-subtitle {
    font-size: 1rem;
  }
  
  .features-grid {
    grid-template-columns: 1fr;
  }
  

}
</style>
