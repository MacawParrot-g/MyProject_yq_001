<!-- 文件路径: src/main/frontend/src/components/NotificationCenter.vue -->
<script setup>import { ref, onMounted, onUnmounted } from 'vue'
import { fetchNotifications, fetchUnreadCount, markNotificationRead, markAllNotificationsRead } from '../api/index.js'

const emit = defineEmits(['error'])

const showPanel = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const loading = ref(false)
const currentUser = ref(localStorage.getItem('userName') || '')

async function loadUnread() {
  if (!currentUser.value) return
  try {
    const json = await fetchUnreadCount(currentUser.value)
    if (json.success) unreadCount.value = json.data || 0
  } catch (e) { /* silent */ }
}

async function loadNotifications() {
  if (!currentUser.value) return
  loading.value = true
  try {
    const json = await fetchNotifications(currentUser.value, 1, 50)
    if (json.success && json.data) {
      notifications.value = json.data.list || []
      unreadCount.value = json.data.unread || 0
    }
  } catch (e) { /* silent */ }
  finally { loading.value = false }
}

async function handleMarkRead(n) {
  if (n.isRead) return
  try {
    await markNotificationRead(currentUser.value, n.id)
    n.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (e) { /* silent */ }
}

async function handleMarkAllRead() {
  try {
    await markAllNotificationsRead(currentUser.value)
    notifications.value.forEach(n => n.isRead = true)
    unreadCount.value = 0
  } catch (e) { /* silent */ }
}

function togglePanel() {
  showPanel.value = !showPanel.value
  if (showPanel.value) loadNotifications()
}

function getTypeIcon(type) {
  switch (type) {
    case 'SYSTEM': return '📢'
    case 'EXPORT': return '📤'
    case 'ALERT': return '⚠️'
    default: return '🔔'
  }
}

function handleVisibilityChange() {
  if (!document.hidden) {
    loadUnread()
  }
}

onMounted(() => {
  loadUnread()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})
onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<template>
  <div class="notification-wrapper">
    <button class="notification-bell" @click="togglePanel" :class="{ 'bell-has-unread': unreadCount > 0 }">
      🔔
      <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </button>

    <div v-if="showPanel" class="notification-panel">
      <div class="notification-panel-header">
        <span class="notification-panel-title">🔔 通知中心</span>
        <div class="notification-panel-actions">
          <button v-if="unreadCount > 0" class="btn-mark-all" @click="handleMarkAllRead">全部已读</button>
          <button class="btn-close-panel" @click="showPanel = false">✕</button>
        </div>
      </div>

      <div v-if="loading" class="notification-loading">加载中...</div>

      <div v-if="!loading && notifications.length === 0" class="notification-empty">
        <div class="empty-icon">📭</div>
        <div>暂无通知</div>
      </div>

      <div v-if="notifications.length > 0" class="notification-list">
        <div
            v-for="n in notifications"
            :key="n.id"
            class="notification-item"
            :class="{ 'notification-unread': !n.isRead }"
            @click="handleMarkRead(n)"
        >
          <div class="notification-item-icon">{{ getTypeIcon(n.type) }}</div>
          <div class="notification-item-body">
            <div class="notification-item-title">{{ n.title }}</div>
            <div class="notification-item-content" v-if="n.content">{{ n.content }}</div>
            <div class="notification-item-time">{{ n.createdAt }}</div>
          </div>
          <div v-if="!n.isRead" class="notification-item-dot"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notification-wrapper { position: relative; }
.notification-bell { background: none; border: none; font-size: 20px; cursor: pointer; position: relative; padding: 4px 8px; border-radius: 8px; transition: background 0.2s; }
.notification-bell:hover { background: rgba(255,255,255,0.1); }
.bell-has-unread { animation: shake 0.5s ease-in-out; }
@keyframes shake { 0%,100% { transform: rotate(0); } 25% { transform: rotate(15deg); } 75% { transform: rotate(-15deg); } }
.bellbadge { position: absolute; top: -2px; right: -2px; background: #ef4444; color: #fff; font-size: 10px; font-weight: 700; min-width: 18px; height: 18px; border-radius: 9px; display: flex; align-items: center; justify-content: center; padding: 0 4px; }

.notification-panel { position: absolute; top: 40px; right: 0; width: 380px; max-height: 500px; background: #fff; border-radius: 14px; box-shadow: 0 8px 32px rgba(0,0,0,0.15); border: 1px solid #e8e8e8; z-index: 200; display: flex; flex-direction: column; overflow: hidden; }
.notification-panel-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid #f0f0f0; background: #fafbfc; }
.notification-panel-title { font-size: 14px; font-weight: 700; color: #1a1a2e; }
.notification-panel-actions { display: flex; gap: 8px; align-items: center; }
.btn-mark-all { background: none; border: none; color: #667eea; font-size: 12px; font-weight: 600; cursor: pointer; }
.btn-mark-all:hover { text-decoration: underline; }
.btn-close-panel { background: none; border: none; font-size: 16px; cursor: pointer; color: #999; padding: 2px 6px; border-radius: 6px; }
.btn-close-panel:hover { background: #f0f0f0; }

.notification-loading { padding: 30px; text-align: center; color: #aaa; font-size: 13px; }
.notification-empty { padding: 40px 20px; text-align: center; color: #aaa; }
.empty-icon { font-size: 36px; margin-bottom: 8px; }

.notification-list { overflow-y: auto; max-height: 420px; }
.notification-item { display: flex; align-items: flex-start; gap: 10px; padding: 12px 16px; border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: background 0.15s; }
.notification-item:hover { background: #f8f9ff; }
.notification-unread { background: #f0f4ff; }
.notification-unread:hover { background: #e8edff; }
.notification-item-icon { font-size: 18px; flex-shrink: 0; margin-top: 2px; }
.notification-item-body { flex: 1; min-width: 0; }
.notification-item-title { font-size: 13px; font-weight: 600; color: #1a1a2e; margin-bottom: 2px; }
.notification-item-content { font-size: 12px; color: #666; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notification-item-time { font-size: 10px; color: #bbb; }
.notification-item-dot { width: 8px; height: 8px; border-radius: 50%; background: #667eea; flex-shrink: 0; margin-top: 6px; }
</style>