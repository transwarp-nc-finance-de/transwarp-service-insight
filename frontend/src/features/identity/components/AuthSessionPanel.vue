<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAuthSession } from '../useAuthSession'

const identities = [
  { code: 'mock-precheck-tdh', label: 'TDH 预诊用户（模拟数据）' },
  { code: 'mock-knowledge-editor', label: '知识编辑人员（模拟数据）' },
  { code: 'mock-knowledge-reviewer', label: '知识审核人员（模拟数据）' },
  { code: 'mock-admin', label: '本地管理员（模拟数据）' },
]
const selectedUserCode = ref('')
const state = useAuthSession()

watch(state.session, (session) => {
  selectedUserCode.value = session?.userCode ?? ''
})

function submit() {
  if (selectedUserCode.value) void state.login(selectedUserCode.value)
}

function logout() {
  void state.logout()
}
</script>

<template>
  <section class="auth-session" aria-label="本地模拟身份">
    <form @submit.prevent="submit">
      <label>
        <span>本地模拟身份（模拟数据）</span>
        <select v-model="selectedUserCode" :disabled="state.loading.value">
          <option value="">请选择模拟身份</option>
          <option v-for="identity in identities" :key="identity.code" :value="identity.code">
            {{ identity.label }}
          </option>
        </select>
      </label>
      <button class="identity-action" :disabled="!selectedUserCode || state.loading.value">
        {{ state.session.value ? '切换身份' : '登录' }}
      </button>
    </form>
    <div v-if="state.session.value" class="identity-summary">
      <strong>{{ state.session.value.displayName }}</strong>
      <span>角色：{{ state.session.value.roles.join('、') }}</span>
      <span>产品线：{{ state.session.value.productLineCodes.join('、') }}</span>
      <button data-test="logout" :disabled="state.loading.value" @click="logout">退出</button>
    </div>
    <p v-else class="identity-empty">请选择模拟身份</p>
    <p v-if="state.error.value" class="identity-error">{{ state.error.value }}</p>
  </section>
</template>
