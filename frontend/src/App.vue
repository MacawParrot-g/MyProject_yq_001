<template>
  <div class="app-layout" v-if="loggedIn">
    <aside class="sidebar" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="sidebar-logo" v-if="!sidebarCollapsed">
<!--          <span class="logo-icon">🎮</span>-->
          <span class="logo-text">欢迎回来，请选择操作模式</span>
        </div>
        <button class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          {{ sidebarCollapsed ? '☰' : '✕' }}
        </button>
      </div>

      <nav class="sidebar-nav">
        <button
            v-for="tab in filteredTabs"
            :key="tab.key"
            class="nav-item"
            :class="{ active: mode === tab.key }"
            @click="switchMode(tab.key)"
        >
          <span class="nav-icon">{{ tabIcons[tab.key] }}</span>
          <span class="nav-label" v-if="!sidebarCollapsed">{{ tab.label }}</span>
        </button>
      </nav>

      <div class="sidebar-stats" v-if="!sidebarCollapsed">
        <div class="stat-card">
          <div class="stat-icon">📊</div>
          <div class="stat-body">
            <div class="stat-label">今日入库</div>
            <div class="stat-value">{{ todayCount }} <span class="stat-unit">条</span></div>
          </div>
        </div>
      </div>

      <div class="sidebar-footer" v-if="!sidebarCollapsed">
        <div class="sidebar-user">
          <div class="user-avatar">{{ displayName ? displayName.charAt(0) : 'U' }}</div>
          <div class="user-info">
            <div class="user-name">{{ displayName }}</div>
            <div class="user-role">{{ accType }}</div>
          </div>
        </div>
        <div class="sidebar-actions">
          <span class="action-link" @click="showGuide = true">新人指南</span>
          <span class="action-link logout-link" @click="comfirmLogout()">{{ loggingOut ? '退出中...' : '退出登录' }}</span>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <header class="main-header">
        <div class="header-left">
          <button class="mobile-menu-btn" @click="sidebarCollapsed = !sidebarCollapsed">☰</button>
          <h2 class="header-title">{{ currentTabLabel }}</h2>
        </div>
        <div class="header-right">
          <span class="header-badge" v-if="errorMsg">⚠️ {{ errorMsg }}</span>
        </div>
      </header>

      <div class="content-area">
        <keep-alive>
          <component :is="currentComponent" @error="setError" @record-saved="fetchTodayCount" />
        </keep-alive>
      </div>
    </main>
  </div>

  <NewbieGuide v-if="showGuide" @close="showGuide = false" />

  <div class="login-overlay" v-if="!loggedIn">
    <div class="login-card">
      <img src="/icon.png" alt="公司Logo" class="login-logo" />
      <h1>"我看行"游戏测试数据管理系统</h1>
      <div class="login-subtitle">请登录后使用</div>
      <div class="login-form">
        <div class="login-field">
          <label>账号</label>
          <input v-model="loginUid" placeholder="请输入账号" @keyup.enter="doLogin" class="login-input" />
        </div>
        <div class="login-field">
          <label>密码</label>
          <input v-model="loginPwd" type="password" placeholder="请输入密码" @keyup.enter="doLogin" class="login-input" />
        </div>
        <button class="login-btn" @click="doLogin" :disabled="!loginUid.trim() || !loginPwd.trim() || loginLoading">
          {{ loginLoading ? '登录中...' : '进入系统' }}
        </button>
        <div v-if="loginError" class="login-error">{{ loginError }}</div>
      </div>
      <div class="login-footer">由深圳市慧动创想科技有限公司 · 蓝黄金刚鹦鹉开发</div>
    </div>
  </div>
</template>

<script setup>import {ref, onMounted, computed, onUnmounted,watch} from 'vue'
import { authLogin, authLogout, authStatus, fetchCountByRecorder } from './api/index.js'
import AutoMode from './components/AutoMode.vue'
import ManualMode from './components/ManualMode.vue'
import DataVisitable from "./components/DataVisitable.vue";
import QRCodeBuilderByMan from "./components/QRCodeBuilderByMan.vue";
import NewbieGuide from "./components/NewbieGuide.vue";
import AdminPanel from "./components/AdminPanel.vue";
import DevMode from "./components/DevMode.vue";
import MonitorPanel from "./components/MonitorPanel.vue";
import GradeManage from "./components/GradeManage.vue";


const mode = ref('auto')
const errorMsg = ref('')
const loggedIn = ref(false)
const loginUid = ref('')
const loginPwd = ref('')
const loginLoading = ref(false)
const loginError = ref('')
const loggingOut = ref(false)
const showGuide = ref(false)
const accType = ref('USER')
const displayName = ref('')
const sidebarCollapsed = ref(false)
const todayCount = ref(0)
// let todayCountTimer = null
const tabIcons = {
  auto: 'A',
  manual: 'M',
  develop: '🛠',
  qrcode: 'Q',
  data: 'D',
  mon: 'M',
  grade: 'G'
}

const componentMap = computed(() => {
  const map = {
    auto: AutoMode,
    manual: ManualMode,
    qrcode: QRCodeBuilderByMan,
  }
  if (accType.value === 'ADMIN') {
    map.data = AdminPanel
  } else if (accType.value === 'DEVELOPER') {
    map.data = AdminPanel
    map.develop = DevMode
    map.mon=MonitorPanel
    map.grade = GradeManage

  } else {
    map.data = DataVisitable
  }
  return map
})


const currentComponent = computed(() => componentMap.value[mode.value])

const tabs = [
  { key: 'auto', label: '自动模式' },
  { key: 'manual', label: '手动模式' },
  { key: 'develop', label: '开发者模式' },
  { key: 'qrcode', label: '二维码生成' },
  { key: 'data', label: '数据看板&导出' },
  { key: 'mon', label: '监控面板' },
  { key: 'grade', label: '评级管理' },
]

const filteredTabs = computed(() => {
  let result = [...tabs]
  if (accType.value !== 'DEVELOPER') {
    result = result.filter(t => t.key !== 'develop' && t.key !== 'mon' && t.key !== 'grade')
  }
  if (accType.value === 'ADMIN') {
    result = result.map(t => {
      if (t.key === 'data') return { ...t, label: '管理员看板' }
      return t
    })
  } else if (accType.value === 'DEVELOPER') {
    result = result.map(t => {
      if (t.key === 'data') return { ...t, label: '管理员看板' }
      return t
    })
  }
  return result
})

const currentTabLabel = computed(() => {
  const found = filteredTabs.value.find(t => t.key === mode.value)
  return found ? found.label : ''
})

onMounted(async () => {
  try {
    const json = await authStatus()
    if (json.success && json.data && json.data.loggedIn) {
      loggedIn.value = true
      accType.value = json.data.data.type || 'USER'
      displayName.value = json.data.data.name || ''
      localStorage.setItem('userName', json.data.data.name || '')
      localStorage.setItem('accType', json.data.data.type || 'USER')
    }
  } catch (e) {
    console.warn('检查登录状态失败:', e)
  }
})

onMounted(() => {
  fetchTodayCount()
})


watch(sidebarCollapsed, (newVal) => {
  if (!newVal) {
    fetchTodayCount()
  }
})

// onUnmounted(() => {
//   if (todayCountTimer) clearInterval(todayCountTimer)
// })

async function doLogin() {
  if (!loginUid.value.trim() || !loginPwd.value.trim()) return
  loginLoading.value = true
  loginError.value = ''
  try {
    const json = await authLogin(loginUid.value.trim(), loginPwd.value.trim())
    if (json.success) {
      const data = json.data
      localStorage.setItem('userName', data.name)
      localStorage.setItem('accType', data.type)
      accType.value = data.type
      displayName.value = data.name
      loggedIn.value = true
      mode.value = 'auto'
    } else {
      loginError.value = json.message || '登录失败'
    }
  } catch (e) {
    loginError.value = '登录请求失败：' + e.message
  } finally {
    loginLoading.value = false
  }
}

function comfirmLogout(){
  let logout=confirm("确定要退出登录吗")
  if(logout){
    doLogout()
  }else{
    return
  }
}

async function doLogout() {
  loggingOut.value = true
  try {
    await authLogout()
  } catch (e) { /* silent */ }
  finally {
    localStorage.removeItem('userName')
    localStorage.removeItem('accType')
    loggedIn.value = false
    accType.value = 'USER'
    displayName.value = ''
    loginUid.value = ''
    loginPwd.value = ''
    loginError.value = ''
    mode.value = 'auto'
    loggingOut.value = false
  }
}

function setError(msg) { errorMsg.value = msg }

function switchMode(newMode) {
  mode.value = newMode
  errorMsg.value = ''
}
function getTodayStrLocal() {
  const d = new Date()
  return d.getFullYear() + '/' + (d.getMonth() + 1) + '/' + d.getDate()
}

async function fetchTodayCount() {
  try {
    const params = new URLSearchParams({
      recorder: localStorage.getItem('userName') || '',
      recordData: getTodayStrLocal()
    })
    const r = await fetch('/api/record/querybyname?' + params.toString())
    const json = await r.json()
    if (json.success) {
      todayCount.value = json.data || 0
    }
  } catch (e) { /* silent */ }
}
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

* { margin: 0; padding: 0; box-sizing: border-box; }

:root {
  --sidebar-bg: #0f0f1a;
  --sidebar-hover: #1a1a2e;
  --sidebar-active: #252540;
  --sidebar-text: #a0a0b8;
  --sidebar-text-active: #ffffff;
  --sidebar-width: 260px;
  --sidebar-collapsed-width: 64px;
  --accent: #6366f1;
  --accent-light: #818cf8;
  --accent-glow: rgba(99, 102, 241, 0.15);
  --bg-primary: #f8f9fc;
  --bg-card: #ffffff;
  --text-primary: #1a1a2e;
  --text-secondary: #64748b;
  --border-color: #e8eaef;
  --success: #22c55e;
  --warning: #f59e0b;
  --danger: #ef4444;
  --radius: 12px;
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.04);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.06);
  --shadow-lg: 0 8px 32px rgba(0,0,0,0.08);
  --transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: var(--bg-primary);
  min-height: 100vh;
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
}

.app-layout {
  display: flex;
  min-height: 100vh;
}

/* ===== Sidebar ===== */
.sidebar {
  width: var(--sidebar-width);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  transition: var(--transition);
  overflow: hidden;
}
.sidebar-collapsed {
  width: var(--sidebar-collapsed-width);
}
.nav-label {
  overflow: hidden;
}

.sidebar-stats {
  padding: 8px 12px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.15);
  border-radius: 12px;
  padding: 12px 14px;
}
.stat-icon {
  font-size: 20px;
  flex-shrink: 0;
}
.stat-body {
  overflow: hidden;
}
.stat-label {
  font-size: 11px;
  color: var(--sidebar-text);
  font-weight: 500;
  letter-spacing: 0.3px;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}
.stat-unit {
  font-size: 12px;
  font-weight: 500;
  color: var(--sidebar-text);
  margin-left: 2px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo-icon {
  font-size: 24px;
}
.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.3px;
}
.sidebar-toggle {
  background: none;
  border: none;
  color: var(--sidebar-text);
  font-size: 18px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  transition: var(--transition);
}
.sidebar-toggle:hover {
  background: var(--sidebar-hover);
  color: #fff;
}

.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--sidebar-text);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  text-align: left;
  white-space: nowrap;
}
.nav-item:hover {
  background: var(--sidebar-hover);
  color: var(--sidebar-text-active);
}
.nav-item.active {
  background: var(--sidebar-active);
  color: var(--sidebar-text-active);
  box-shadow: inset 3px 0 0 var(--accent);
}
.nav-icon {
  font-size: 18px;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}
.nav-label {
  overflow: hidden;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255,255,255,0.06);
}
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--accent), #a855f7);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}
.user-info {
  overflow: hidden;
}
.user-name {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user-role {
  font-size: 11px;
  color: var(--sidebar-text);
}
.sidebar-actions {
  display: flex;
  gap: 12px;
}
.action-link {
  font-size: 12px;
  color: var(--sidebar-text);
  cursor: pointer;
  transition: var(--transition);
}
.action-link:hover {
  color: var(--accent-light);
}
.logout-link:hover {
  color: var(--danger);
}

/* ===== Main Content ===== */
.main-content {
  flex: 1;
  margin-left: var(--sidebar-width);
  transition: var(--transition);
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.sidebar-collapsed ~ .main-content,
.sidebar-collapsed + .main-content {
  margin-left: var(--sidebar-collapsed-width);
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 50;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-badge {
  font-size: 12px;
  color: var(--danger);
  background: #fef2f2;
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 600;
}
.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  font-size: 22px;
  cursor: pointer;
  color: var(--text-primary);
  padding: 4px 8px;
  border-radius: 8px;
}

.content-area {
  flex: 1;
  padding: 28px 32px;
  overflow-y: auto;
}

/* ===== Login ===== */
.login-overlay {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0f1a 0%, #1a1a3e 50%, #0f0f1a 100%);
}
.login-card {
  background: var(--bg-card);
  border-radius: 20px;
  padding: 48px 40px;
  text-align: center;
  box-shadow: 0 32px 100px rgba(0,0,0,0.3);
  max-width: 420px;
  width: 90%;
}
.login-logo {
  width: 180px;
  height: 90px;
  object-fit: contain;
  margin: 0 auto 12px auto;
  display: block;
  border-radius: 16px;
}
.login-card h1 {
  font-size: 24px;
  color: var(--text-primary);
  margin-bottom: 6px;
  font-weight: 700;
}
.login-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}
.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.login-field {
  text-align: left;
}
.login-field label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.login-input {
  width: 100%;
  padding: 12px 16px;
  font-size: 15px;
  border: 2px solid var(--border-color);
  border-radius: 10px;
  outline: none;
  transition: var(--transition);
  font-family: inherit;
}
.login-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px var(--accent-glow);
}
.login-btn {
  width: 100%;
  background: linear-gradient(135deg, var(--accent), #a855f7);
  color: #fff;
  border: none;
  padding: 14px;
  font-size: 16px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  transition: var(--transition);
  margin-top: 4px;
}
.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(99,102,241,0.4);
}
.login-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.login-error {
  color: var(--danger);
  font-size: 13px;
  font-weight: 600;
  background: #fef2f2;
  padding: 10px 14px;
  border-radius: 10px;
}
.login-footer {
  font-size: 11px;
  color: #bbb;
  margin-top: 28px;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    width: var(--sidebar-width);
  }
  .sidebar:not(.sidebar-collapsed) {
    transform: translateX(0);
    box-shadow: 0 0 60px rgba(0,0,0,0.5);
  }
  .sidebar-collapsed {
    transform: translateX(-100%);
    width: var(--sidebar-width);
  }
  .main-content {
    margin-left: 0 !important;
  }
  .mobile-menu-btn {
    display: block;
  }
  .content-area {
    padding: 20px 16px;
  }
  .main-header {
    padding: 12px 16px;
  }
}

/* ===== Shared component styles (global) ===== */
.error-msg {
  color: var(--danger);
  background: #fef2f2;
  padding: 10px 14px;
  border-radius: 10px;
  margin-bottom: 14px;
  font-size: 13px;
  font-weight: 500;
  border-left: 3px solid var(--danger);
}
.duplicate-tip {
  color: #92400e;
  background: #fef3c7;
  border-left: 3px solid var(--warning);
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 16px;
  font-size: 13px;
  font-weight: 600;
}
.loading {
  color: var(--text-secondary);
  font-size: 13px;
  padding: 16px 0;
}

.btn-refresh {
  background: linear-gradient(135deg, var(--accent), #a855f7);
  color: #fff;
  border: none;
  padding: 11px 28px;
  font-size: 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: var(--transition);
  font-weight: 600;
}
.btn-refresh:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99,102,241,0.35);
}
.btn-refresh:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-today-count {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  border: none;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 10px;
  cursor: pointer;
  transition: var(--transition);
}
.btn-today-count:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99,102,241,0.35);
}

.btn-event {
  background: linear-gradient(135deg, #ec4899, #f43f5e);
  color: #fff;
  border: none;
  padding: 11px 28px;
  font-size: 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: var(--transition);
  font-weight: 600;
}
.btn-event:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(236,72,153,0.35);
}
.btn-event:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-frozen {
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: #fff;
  border: none;
  padding: 11px 28px;
  font-size: 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: var(--transition);
  font-weight: 600;
  margin-top: 10px;
}
.btn-frozen:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59,130,246,0.35);
}
.btn-frozen:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-save {
  background: linear-gradient(135deg, #22c55e, #10b981);
  color: #fff;
  border: none;
  padding: 11px 28px;
  font-size: 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: var(--transition);
  font-weight: 600;
}
.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(34,197,94,0.35);
}
.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-timer {
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #fff;
  border: none;
  padding: 8px 18px;
  font-size: 13px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  transition: var(--transition);
}
.btn-timer:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(245,158,11,0.35);
}
.btn-timer:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-timer-cancel {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  border: none;
  padding: 8px 14px;
  font-size: 13px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  transition: var(--transition);
}
.btn-timer-cancel:hover {
  transform: translateY(-1px);
}

.timer-section {
  margin-bottom: 14px;
}
.timer-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.timer-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  white-space: nowrap;
}
.timer-input {
  width: 80px;
  padding: 8px 10px;
  font-size: 14px;
  border: 2px solid var(--border-color);
  border-radius: 10px;
  outline: none;
  text-align: center;
  transition: var(--transition);
  background: var(--bg-card);
}
.timer-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}
.timer-status {
  margin-top: 8px;
  font-size: 13px;
  color: var(--accent);
  font-weight: 600;
}

.info-section {
  text-align: left;
  background: var(--bg-card);
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-bottom: 16px;
  border: 1px solid var(--border-color);
}
.info-item {
  margin-bottom: 10px;
  word-break: break-all;
}
.info-item:last-child {
  margin-bottom: 0;
}
.info-label {
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 3px;
}
.info-value {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.6;
}

.input-group {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.input-group input {
  flex: 1;
  padding: 11px 14px;
  font-size: 15px;
  border: 2px solid var(--border-color);
  border-radius: 10px;
  outline: none;
  transition: var(--transition);
  background: var(--bg-card);
  font-family: inherit;
}
.input-group input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.preview-url {
  text-align: left;
  background: var(--accent-glow);
  border-radius: var(--radius);
  padding: 10px 14px;
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--text-secondary);
  word-break: break-all;
  border: 1px solid rgba(99,102,241,0.15);
}
.preview-url strong {
  color: var(--accent);
}

.qrcode-container {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}
.qrcode-container canvas {
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
}

.event-result {
  text-align: left;
  background: #f0fdf4;
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-top: 14px;
  border: 1px solid #bbf7d0;
}
.event-result.no-event {
  background: #f0fdf4;
  border-color: #bbf7d0;
}
.event-result .event-label {
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}
.event-result .event-value {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.6;
}
.event-result .event-value.highlight {
  color: var(--accent);
  font-weight: 700;
  font-size: 20px;
}
.event-compare {
  text-align: left;
  background: #f8fafc;
  border-radius: 10px;
  padding: 10px 14px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-secondary);
}
.event-compare .diff {
  color: var(--danger);
  font-weight: 600;
}

.attribution-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.attr-tag {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}
.attr-tag.appflyer { background: #22c55e; }
.attr-tag.adjust { background: #f59e0b; }
.attr-tag.singular { background: #3b82f6; }
.attr-tag.tenjin { background: #a855f7; }
.no-attribution {
  color: #ea580c;
  font-weight: 600;
  font-size: 13px;
  margin-top: 8px;
  background: #fff7ed;
  padding: 6px 14px;
  border-radius: 10px;
  display: inline-block;
}

.frozen-result {
  text-align: left;
  background: #eff6ff;
  border-radius: var(--radius);
  padding: 14px 18px;
  margin-top: 10px;
  border: 1px solid #bfdbfe;
}
.frozen-result .frozen-label {
  font-weight: 600;
  color: #2563eb;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 3px;
}
.frozen-result .frozen-value {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.6;
}

.save-success {
  text-align: left;
  background: #f0fdf4;
  border-radius: var(--radius);
  padding: 10px 14px;
  margin-top: 10px;
  color: #166534;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid #bbf7d0;
}

.form-section {
  text-align: left;
  background: var(--bg-card);
  border-radius: var(--radius);
  padding: 20px 24px;
  margin-top: 16px;
  border: 1px solid var(--border-color);
}
.form-section h4 {
  font-size: 15px;
  color: var(--text-primary);
  margin-bottom: 16px;
  font-weight: 700;
}
.form-group {
  margin-bottom: 14px;
}
.form-group label {
  display: block;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}
.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 10px 14px;
  font-size: 13px;
  border: 2px solid var(--border-color);
  border-radius: 10px;
  outline: none;
  transition: var(--transition);
  font-family: inherit;
  background: var(--bg-card);
}
.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}
.form-group textarea {
  resize: vertical;
  min-height: 60px;
}

.export-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.export-count {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 600;
}
.export-count span {
  color: var(--accent);
  font-size: 20px;
  font-weight: 700;
}
.btn-export-exec {
  background: linear-gradient(135deg, #ec4899, #f43f5e);
  color: #fff;
  border: none;
  padding: 11px 28px;
  font-size: 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: var(--transition);
  font-weight: 600;
}
.btn-export-exec:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(236,72,153,0.35);
}
.btn-export-exec:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.export-table-wrapper {
  overflow-x: auto;
  margin-bottom: 20px;
  border-radius: var(--radius);
  border: 1px solid var(--border-color);
}
.export-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  text-align: left;
  min-width: 700px;
}
.export-table th {
  background: linear-gradient(135deg, var(--accent), #a855f7);
  color: #fff;
  padding: 11px 14px;
  white-space: nowrap;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.export-table td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.export-table tr:hover td {
  background: #f8f9ff;
}
.export-table tr:last-child td {
  border-bottom: none;
}
.export-empty {
  text-align: center;
  padding: 50px 20px;
  color: #aaa;
  font-size: 15px;
}
.export-empty .empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.export-result-box {
  text-align: left;
  border-radius: var(--radius);
  padding: 14px 18px;
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-primary);
}
.export-result-box.success {
  background: #f0fdf4;
  border-left: 3px solid var(--success);
}
.export-result-box.fail {
  background: #fef2f2;
  border-left: 3px solid var(--danger);
}

.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.modal-box {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 28px 32px;
  min-width: 340px;
  box-shadow: var(--shadow-lg);
  text-align: center;
}
.modal-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--text-primary);
}
.modal-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 22px;
}
.modal-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.btn-modal {
  padding: 10px 0;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}
.btn-modal:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-poll {
  background: #fbbf24;
  color: #78350f;
}
.btn-retest {
  background: var(--success);
  color: #fff;
}
.btn-cancel {
  background: #f1f5f9;
  color: var(--text-secondary);
}

.floating-text {
  padding-bottom: 17px;
}
</style>
