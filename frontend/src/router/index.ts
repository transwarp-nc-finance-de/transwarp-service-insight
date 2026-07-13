import { createRouter, createWebHistory } from 'vue-router'
import SandboxPage from '../pages/SandboxPage.vue'
import EmbedPrecheckPage from '../pages/EmbedPrecheckPage.vue'
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/sandbox' },
    { path: '/sandbox', component: SandboxPage },
    { path: '/embed', component: EmbedPrecheckPage },
  ],
})
