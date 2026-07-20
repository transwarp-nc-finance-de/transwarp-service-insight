<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import AuthSessionPanel from './features/identity/components/AuthSessionPanel.vue'
</script>
<template>
  <header>
    <div><span class="brand">TRANSWARP</span><strong>Service Insight</strong></div>
    <nav>
      <RouterLink to="/sandbox">Sandbox v1</RouterLink
      ><RouterLink to="/precheck-v2">Precheck v2</RouterLink
      ><RouterLink to="/embed">Embed</RouterLink><RouterLink to="/knowledge">Knowledge</RouterLink>
      <RouterLink to="/audit">Audit</RouterLink>
    </nav>
    <AuthSessionPanel />
  </header>
  <RouterView />
</template>
