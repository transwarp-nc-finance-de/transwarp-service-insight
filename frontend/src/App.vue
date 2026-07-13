<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>
<template>
  <header>
    <div><span class="brand">TRANSWARP</span><strong>Service Insight</strong></div>
    <nav>
      <RouterLink to="/sandbox">Sandbox</RouterLink><RouterLink to="/embed">Embed</RouterLink>
    </nav>
  </header>
  <RouterView />
</template>
