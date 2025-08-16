import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import CharacterSearch from '../views/CharacterSearch.vue'
import CharacterDetail from '../views/CharacterDetail.vue'
import DungeonStatus from '../views/DungeonStatus.vue'
import PartyFormation from '../views/PartyFormation.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/character-search',
      name: 'character-search',
      component: CharacterSearch
    },
    {
      path: '/character/:characterId',
      name: 'character-detail',
      component: CharacterDetail
    },
    {
      path: '/dungeon-status',
      name: 'dungeon-status',
      component: DungeonStatus
    },
    {
      path: '/party-formation',
      name: 'party-formation',
      component: PartyFormation
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue')
    }
  ]
})

export default router
