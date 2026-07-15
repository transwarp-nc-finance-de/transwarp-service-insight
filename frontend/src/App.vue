<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import AuthSessionPanel from './features/identity/components/AuthSessionPanel.vue'
</script>
<template>
  <header>
    <div><span class="brand">TRANSWARP</span><strong>Service Insight</strong></div>
    <nav>
      <RouterLink to="/sandbox">Sandbox</RouterLink><RouterLink to="/embed">Embed</RouterLink>
    </nav>
    <AuthSessionPanel />
  </header>
  <RouterView />
</template>
