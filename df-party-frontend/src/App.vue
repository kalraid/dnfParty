<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div id="app">
    <header>
      <div class="header-content">
        <h1 class="site-title">던전앤파이터 파티 구성</h1>
        
        <nav class="main-nav">
          <RouterLink to="/" class="nav-link">홈</RouterLink>
          <RouterLink to="/character-search" class="nav-link">캐릭터 검색</RouterLink>
          <RouterLink to="/character-list" class="nav-link">캐릭터 리스트</RouterLink>
          <RouterLink to="/party-formation" class="nav-link">파티 짜기</RouterLink>
        </nav>
      </div>
    </header>

    <main class="main-content">
      <RouterView />
    </main>

    <footer class="footer">
      <p>&copy; 2024 던전앤파이터 파티 구성 사이트</p>
    </footer>
  </div>
</template>

<style scoped>
header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 1rem 0;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.site-title {
  margin: 0 0 1rem 0;
  font-size: 2rem;
  font-weight: bold;
  text-align: center;
}

.main-nav {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center;
}

.nav-link {
  color: white;
  text-decoration: none;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.nav-link:hover {
  background: rgba(255,255,255,0.2);
  transform: translateY(-2px);
}

.nav-link.router-link-active {
  background: rgba(255,255,255,0.3);
  font-weight: bold;
}

.main-content {
  min-height: calc(100vh - 200px);
  padding: 20px;
}

.footer {
  background: #333;
  color: white;
  text-align: center;
  padding: 1rem 0;
  margin-top: auto;
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 10px;
  }
  
  .site-title {
    font-size: 1.5rem;
  }
  
  .main-nav {
    gap: 0.5rem;
  }
  
  .nav-link {
    padding: 0.4rem 0.8rem;
    font-size: 0.9rem;
  }
}
</style>

<style>
body {
  margin: 0;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: #f8f9fa;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
