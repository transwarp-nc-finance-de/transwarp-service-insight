import { createRouter, createWebHistory } from 'vue-router'
import SandboxPage from '../pages/SandboxPage.vue'
import EmbedPrecheckPage from '../pages/EmbedPrecheckPage.vue'
import KnowledgeIngestionPage from '../pages/KnowledgeIngestionPage.vue'
import PersistentPrecheckPage from '../pages/PersistentPrecheckPage.vue'
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/sandbox' },
    { path: '/sandbox', component: SandboxPage },
    { path: '/precheck-v2', component: PersistentPrecheckPage },
    { path: '/embed', component: EmbedPrecheckPage },
    { path: '/knowledge', component: KnowledgeIngestionPage },
  ],
})
