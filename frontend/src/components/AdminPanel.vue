<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import {
  fetchRecordList,
  fetchUserList,
  createUser,
  deleteUser,
  fetchSystemInfo
} from '../api/index.js'

const emit = defineEmits(['error'])

const ATTR_OPTIONS = ['appflyer', 'adjust', 'singular', 'tenjin']

const activeTab = ref('data')

const selectedAscribe = ref('')
const frozenOnly = ref(false)
const list = ref([])
const loading = ref(false)
const queried = ref(false)
const viewType = ref('ALL')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)
const recorderSearch = ref('')
const dateSearch = ref('')

const userList = ref([])
const userLoading = ref(false)
const createForm = reactive({ name: '', pwd: '', type: 'USER' })
const creating = ref(false)
const createMsg = ref('')

const sysInfo = ref(null)
const sysLoading = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const viewTypeLabel = {
  ALL: '全部数据', APPFLYER: 'appflyer', ADJUST: 'adjust',
  SINGULAR: 'singular', TENJIN: 'tenjin', FROZEN: '已冻结数据',
  APPFLYER_FROZEN: 'appflyer · 已冻结', ADJUST_FROZEN: 'adjust · 已冻结',
  SINGULAR_FROZEN: 'singular · 已冻结', TENJIN_FROZEN: 'tenjin · 已冻结'
}

async function fetchData(resetPage = false) {
  if (resetPage) currentPage.value = 1
  loading.value = true
  queried.value = true
  list.value = []
  try {
    const params = new URLSearchParams()
    params.append('page', currentPage.value)
    params.append('size', pageSize.value)
    if (selectedAscribe.value) params.append('ascribe', selectedAscribe.value)
    if (frozenOnly.value) params.append('frozen', 'true')
    if (recorderSearch.value.trim()) params.append('recorder', recorderSearch.value.trim())
    if (dateSearch.value) params.append('recordData', dateSearch.value)

    const url = '/api/record/list?' + params.toString()
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 30000)
    const r = await fetch(url, { signal: controller.signal, credentials: 'include',
      headers: { 'X-User-Name': encodeURIComponent(localStorage.getItem('userName') || 'anonymous') }
    }).finally(() => clearTimeout(timeoutId))
    const text = await r.text()
    const json = text ? JSON.parse(text) : { success: false }

    if (json.success) {
      list.value = json.data || []
      viewType.value = json.viewType || 'ALL'
      total.value = json.total || 0
    } else {
      emit('error', json.message || '查询失败')
    }
  } catch (e) {
    emit('error', '查询请求失败：' + e.message)
  } finally {
    loading.value = false
  }
}

function prevPage() { if (currentPage.value > 1) { currentPage.value--; fetchData() } }
function nextPage() { if (currentPage.value < totalPages.value) { currentPage.value++; fetchData() } }

async function loadUsers() {
  userLoading.value = true
  try {
    const json = await fetchUserList()
    if (json.success) {
      userList.value = json.data || []
    } else {
      emit('error', json.message || '获取用户列表失败')
    }
  } catch (e) {
    emit('error', '获取用户列表失败：' + e.message)
  } finally {
    userLoading.value = false
  }
}

async function handleCreateUser() {
  if (!createForm.name.trim() || !createForm.pwd.trim()) {
    emit('error', '姓名和密码不能为空')
    return
  }
  creating.value = true
  createMsg.value = ''
  try {
    const json = await createUser(createForm.name.trim(), createForm.pwd.trim(), createForm.type)
    if (json.success) {
      createMsg.value = '✅ ' + json.message + '，账号UID：' + (json.data?.uid || '')
      createForm.name = ''
      createForm.pwd = ''
      createForm.type = 'USER'
      await loadUsers()
    } else {
      createMsg.value = '❌ ' + (json.message || '创建失败')
    }
  } catch (e) {
    createMsg.value = '❌ 创建请求失败：' + e.message
  } finally {
    creating.value = false
  }
}

async function handleDeleteUser(uid) {
  if (!confirm('确定要删除用户 ' + uid + ' 吗？此操作不可撤销！')) return
  try {
    const json = await deleteUser(uid)
    if (json.success) {
      await loadUsers()
    } else {
      emit('error', json.message || '删除失败')
    }
  } catch (e) {
    emit('error', '删除请求失败：' + e.message)
  }
}

async function loadSystemInfo() {
  sysLoading.value = true
  try {
    const json = await fetchSystemInfo()
    if (json.success) sysInfo.value = json.data
  } catch (e) { /* silent */ }
  finally { sysLoading.value = false }
}

onMounted(() => {
  fetchData()
  loadUsers()
  loadSystemInfo()
})
</script>

<template>
  <div class="admin-page">
    <h2>🔧 后台管理系统</h2>

    <div class="admin-tabs">
      <button class="admin-tab" :class="{ active: activeTab === 'data' }" @click="activeTab = 'data'">📊 数据管理</button>
      <button class="admin-tab" :class="{ active: activeTab === 'users' }" @click="activeTab = 'users'">👥 用户管理</button>
      <button class="admin-tab" :class="{ active: activeTab === 'system' }" @click="activeTab = 'system'; loadSystemInfo()">🖥 系统状态</button>
    </div>

    <!-- 数据管理 Tab -->
    <div v-if="activeTab === 'data'" class="admin-section">
      <div class="filter-card">
        <div class="filter-row">
          <span class="filter-label">归因筛选：</span>
          <div class="radio-group">
            <label class="radio-tag radio-all" :class="{ active: selectedAscribe === '' }">
              <input type="radio" name="adminAscribe" value="" :checked="selectedAscribe === ''" @change="selectedAscribe = ''" />
              <span>全部</span>
            </label>
            <label v-for="opt in ATTR_OPTIONS" :key="opt" class="radio-tag" :class="[opt, { active: selectedAscribe === opt }]">
              <input type="radio" name="adminAscribe" :value="opt" :checked="selectedAscribe === opt" @change="selectedAscribe = opt" />
              <span>{{ opt }}</span>
            </label>
          </div>
        </div>
        <div class="filter-row">
          <label class="checkbox-tag" :class="{ active: frozenOnly }">
            <input type="checkbox" v-model="frozenOnly" />
            <span>仅查看已冻结数据</span>
          </label>
        </div>
        <div class="filter-row">
          <span class="filter-label">测试人：</span>
          <input v-model="recorderSearch" class="filter-input" placeholder="输入测试人姓名" @keyup.enter="fetchData(true)" />
          <span class="filter-label" style="margin-left:12px;">日期：</span>
          <input v-model="dateSearch" class="filter-input" placeholder="如 2026/8/19" @keyup.enter="fetchData(true)" />
          <button class="btn-query" @click="fetchData(true)" :disabled="loading">
            {{ loading ? '查询中...' : '查询' }}
          </button>
        </div>
      </div>

      <div v-if="queried && viewType" class="view-type-tag">
        当前视图：<strong>{{ viewTypeLabel[viewType] || viewType }}</strong>
        <span v-if="total > 0"> — 共 {{ total }} 条，第 {{ currentPage }} / {{ totalPages }} 页</span>
      </div>

      <div v-if="loading && !queried" class="loading-text">正在查询数据...</div>
      <div v-if="!loading && queried && list.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <div>没有符合条件的数据</div>
      </div>

      <div v-if="list.length > 0" class="table-wrapper">
        <table class="data-table">
          <thead>
          <tr>
            <th>#</th><th>URL</th><th>Bundle ID</th><th>归因</th><th>事件数</th>
            <th>异常类型</th><th>记录日期</th><th>记录人</th><th>备注</th><th>导出</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(item, index) in list" :key="item.URL">
            <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
            <td :title="item.URL" class="cell-url">{{ item.URL }}</td>
            <td>{{ item.bundleId }}</td>
            <td>{{ item.ascribe || '-' }}</td>
            <td>{{ item.event_number }}</td>
            <td>{{ item.exception_type || '-' }}</td>
            <td>{{ item.record_data || '-' }}</td>
            <td>{{ item.recorder || '-' }}</td>
            <td :title="item.remark" class="cell-remark">{{ item.remark || '-' }}</td>
            <td><span :class="item.isOutput === 1 ? 'tag-exported' : 'tag-unexported'">{{ item.isOutput === 1 ? '已导出' : '未导出' }}</span></td>
          </tr>
          </tbody>
        </table>
      </div>

      <div v-if="total > 0" class="pagination">
        <button class="page-btn" :disabled="currentPage <= 1" @click="prevPage">‹ 上一页</button>
        <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条</span>
        <button class="page-btn" :disabled="currentPage >= totalPages" @click="nextPage">下一页 ›</button>
      </div>
    </div>

    <!-- 用户管理 Tab -->
    <div v-if="activeTab === 'users'" class="admin-section">
      <div class="create-user-card">
        <h3>创建新用户</h3>
        <div class="create-form">
          <div class="form-row">
            <div class="form-group">
              <label>姓名</label>
              <input v-model="createForm.name" placeholder="用户姓名" class="form-input" />
            </div>
            <div class="form-group">
              <label>密码</label>
              <input v-model="createForm.pwd" type="password" placeholder="登录密码" class="form-input" />
            </div>
            <div class="form-group">
              <label>账户类型</label>
              <select v-model="createForm.type" class="form-input">
                <option value="USER">普通用户 (USER)</option>
                <option value="ADMIN">管理员 (ADMIN)</option>
              </select>
            </div>
          </div>
          <button class="btn-create" @click="handleCreateUser" :disabled="creating">
            {{ creating ? '创建中...' : '创建用户' }}
          </button>
          <div v-if="createMsg" class="create-msg" :class="{ 'msg-success': createMsg.startsWith('✅'), 'msg-fail': createMsg.startsWith('❌') }">
            {{ createMsg }}
          </div>
        </div>
      </div>

      <div class="user-list-card">
        <h3>用户列表</h3>
        <div v-if="userLoading" class="loading-text">加载中...</div>
        <div v-if="!userLoading && userList.length === 0" class="empty-state">暂无用户</div>
        <div v-if="userList.length > 0" class="table-wrapper">
          <table class="data-table user-table">
            <thead>
            <tr><th>UID</th><th>姓名</th><th>类型</th><th>操作</th></tr>
            </thead>
            <tbody>
            <tr v-for="u in userList" :key="u.uid">
              <td class="uid-cell">{{ u.uid }}</td>
              <td>{{ u.name }}</td>
              <td>
                <span class="type-tag" :class="u.type === 'ADMIN' ? 'type-admin' : 'type-user'">
                  {{ u.type }}
                </span>
              </td>
              <td>
                <button class="btn-del-user" @click="handleDeleteUser(u.uid)">删除</button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 系统状态 Tab -->
    <div v-if="activeTab === 'system'" class="admin-section">
      <div class="sys-card">
        <div class="sys-header">
          <span class="sys-title">🖥 服务器运行状态</span>
          <button class="sys-refresh-btn" @click="loadSystemInfo" :disabled="sysLoading">
            {{ sysLoading ? '加载中...' : '刷新' }}
          </button>
        </div>
        <div v-if="sysInfo" class="sys-body">
          <div class="sys-grid">
            <div class="sys-item"><span class="sys-label">操作系统</span><span class="sys-val">{{ sysInfo.osName }}</span></div>
            <div class="sys-item"><span class="sys-label">系统架构</span><span class="sys-val">{{ sysInfo.osArch }}</span></div>
            <div class="sys-item"><span class="sys-label">CPU</span><span class="sys-val">{{ sysInfo.cpuName }} ({{ sysInfo.cpuCores }}核)</span></div>
            <div class="sys-item"><span class="sys-label">CPU占用</span><span class="sys-val" :class="{ 'sys-warn': sysInfo.cpuUsagePercent > 80 }">{{ sysInfo.cpuUsagePercent }}%</span></div>
            <div class="sys-item"><span class="sys-label">内存总量</span><span class="sys-val">{{ sysInfo.totalMemoryGB }} GB</span></div>
            <div class="sys-item"><span class="sys-label">已用内存</span><span class="sys-val" :class="{ 'sys-warn': sysInfo.memoryUsagePercent > 85 }">{{ sysInfo.usedMemoryGB }} GB ({{ sysInfo.memoryUsagePercent }}%)</span></div>
            <div class="sys-item"><span class="sys-label">可用内存</span><span class="sys-val">{{ sysInfo.availableMemoryGB }} GB</span></div>
            <div class="sys-item"><span class="sys-label">服务运行时长</span><span class="sys-val">{{ sysInfo.javaUptime }}</span></div>
          </div>
          <div class="progress-bar">
            <div class="progress-label">内存使用率</div>
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: sysInfo.memoryUsagePercent + '%' }" :class="{ 'progress-danger': sysInfo.memoryUsagePercent > 85 }"></div>
            </div>
            <span class="progress-text">{{ sysInfo.memoryUsagePercent }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-label">CPU使用率</div>
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: sysInfo.cpuUsagePercent + '%' }" :class="{ 'progress-danger': sysInfo.cpuUsagePercent > 80 }"></div>
            </div>
            <span class="progress-text">{{ sysInfo.cpuUsagePercent }}%</span>
          </div>
        </div>
        <div v-else-if="sysLoading" class="loading-text">正在获取系统信息...</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-page { width: 100%; max-width: 1600px; margin: 0 auto; padding: 0 16px; box-sizing: border-box; }
.admin-page h2 { margin-bottom: 18px; color: #333; }

.admin-tabs { display: flex; background: #f0f0f0; border-radius: 10px; padding: 4px; margin-bottom: 24px; }
.admin-tab { flex: 1; padding: 10px 0; font-size: 14px; border: none; border-radius: 8px; cursor: pointer; background: transparent; color: #888; font-weight: 600; transition: all 0.25s ease; }
.admin-tab.active { background: #fff; color: #333; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.admin-tab:not(.active):hover { color: #555; }

.admin-section { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }

.filter-card { text-align: left; background: #f8f9fa; border-radius: 10px; padding: 16px 18px; margin-bottom: 16px; border: 1px solid #e0e0e0; }
.filter-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; flex-wrap: wrap; }
.filter-row:last-child { margin-bottom: 0; }
.filter-label { font-weight: 600; color: #555; font-size: 13px; white-space: nowrap; }
.filter-input { padding: 6px 14px; font-size: 13px; border: 2px solid #ddd; border-radius: 8px; outline: none; width: 180px; transition: border-color 0.2s; }
.filter-input:focus { border-color: #667eea; }

.radio-group { display: flex; gap: 10px; flex-wrap: wrap; }
.radio-tag { display: flex; align-items: center; gap: 4px; padding: 5px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; cursor: pointer; color: #fff; transition: all 0.2s; opacity: 0.4; border: 2px solid transparent; }
.radio-tag input { display: none; }
.radio-tag.active { opacity: 1; box-shadow: 0 2px 8px rgba(0,0,0,0.2); }
.radio-tag.radio-all { background: #607D8B; }
.radio-tag.appflyer { background: #4CAF50; }
.radio-tag.adjust { background: #FF9800; }
.radio-tag.singular { background: #2196F3; }
.radio-tag.tenjin { background: #9C27B0; }

.checkbox-tag { background: #e3f2fd; color: #1976d2; padding: 6px 14px; border-radius: 8px; font-size: 13px; opacity: 0.5; transition: all 0.2s; border: 2px solid transparent; display: flex; align-items: center; gap: 6px; }
.checkbox-tag.active { opacity: 1; border-color: #1976d2; }
.checkbox-tag input { margin-right: 4px; }

.btn-query { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; padding: 7px 20px; font-size: 13px; border-radius: 8px; cursor: pointer; font-weight: 600; }
.btn-query:disabled { opacity: 0.6; cursor: not-allowed; }

.view-type-tag { text-align: left; font-size: 13px; color: #667eea; margin-bottom: 12px; padding: 8px 14px; background: #f0f4ff; border-radius: 8px; }
.loading-text { text-align: center; color: #888; font-size: 13px; padding: 20px 0; }
.empty-state { text-align: center; padding: 50px 20px; color: #aaa; font-size: 15px; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }

.table-wrapper { overflow-x: auto; border-radius: 8px; border: 1px solid #e0e0e0; margin-bottom: 16px; }
.data-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; min-width: 1000px; }
.data-table th { background: #667eea; color: #fff; padding: 10px 12px; white-space: nowrap; }
.data-table td { padding: 8px 12px; border-bottom: 1px solid #eee; color: #333; vertical-align: middle; }
.data-table tr:hover td { background: #f5f7ff; }
.data-table tr:last-child td { border-bottom: none; }
.cell-url { max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cell-remark { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tag-exported { background: #e8f5e9; color: #2e7d32; padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; }
.tag-unexported { background: #fff3e0; color: #e65100; padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 12px 0; }
.page-btn { background: #667eea; color: #fff; border: none; padding: 8px 20px; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 13px; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 13px; color: #555; font-weight: 600; }

.create-user-card, .user-list-card { background: #f8f9fa; border-radius: 10px; padding: 20px; margin-bottom: 18px; border: 1px solid #e0e0e0; text-align: left; }
.create-user-card h3, .user-list-card h3 { font-size: 15px; color: #333; margin-bottom: 16px; }
.create-form { display: flex; flex-direction: column; gap: 12px; }
.form-row { display: flex; gap: 12px; flex-wrap: wrap; }
.form-group { flex: 1; min-width: 150px; }
.form-group label { display: block; font-weight: 600; color: #555; font-size: 12px; margin-bottom: 4px; }
.form-input { width: 100%; padding: 9px 12px; font-size: 13px; border: 2px solid #ddd; border-radius: 8px; outline: none; transition: border-color 0.2s; box-sizing: border-box; }
.form-input:focus { border-color: #667eea; }

.btn-create { background: linear-gradient(135deg, #43e97b, #38f9d7); color: #fff; border: none; padding: 10px 24px; font-size: 14px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: transform 0.2s, box-shadow 0.2s; align-self: flex-start; }
.btn-create:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(67,233,123,0.4); }
.btn-create:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.create-msg { font-size: 13px; font-weight: 600; padding: 8px 14px; border-radius: 8px; }
.msg-success { background: #e8f5e9; color: #2e7d32; }
.msg-fail { background: #fdecea; color: #c62828; }

.user-table { min-width: 500px; }
.uid-cell { font-family: monospace; font-weight: 600; color: #667eea; letter-spacing: 0.5px; }
.type-tag { padding: 3px 12px; border-radius: 12px; font-size: 11px; font-weight: 700; }
.type-admin { background: #fff3e0; color: #e65100; }
.type-user { background: #e3f2fd; color: #1565c0; }
.btn-del-user { background: linear-gradient(135deg, #ff6b6b, #ee5a24); color: #fff; border: none; padding: 5px 16px; font-size: 12px; border-radius: 6px; cursor: pointer; font-weight: 600; }
.btn-del-user:hover { transform: translateY(-1px); }

.sys-card { background: #f8f9fa; border-radius: 10px; padding: 20px; border: 1px solid #e0e0e0; text-align: left; }
.sys-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.sys-title { font-size: 15px; font-weight: 700; color: #333; }
.sys-refresh-btn { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; padding: 5px 14px; font-size: 12px; border-radius: 6px; cursor: pointer; font-weight: 600; }
.sys-refresh-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.sys-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 14px; }
.sys-item { display: flex; flex-direction: column; gap: 2px; }
.sys-label { font-size: 11px; color: #888; font-weight: 600; }
.sys-val { font-size: 13px; color: #333; font-weight: 500; }
.sys-warn { color: #e74c3c; font-weight: 700; }
.progress-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.progress-label { font-size: 12px; color: #555; font-weight: 600; white-space: nowrap; width: 70px; }
.progress-track { flex: 1; height: 10px; background: #e0e0e0; border-radius: 5px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #43e97b, #38f9d7); border-radius: 5px; transition: width 0.5s ease; }
.progress-fill.progress-danger { background: linear-gradient(90deg, #ff6b6b, #ee5a24); }
.progress-text { font-size: 12px; font-weight: 700; color: #333; width: 50px; text-align: right; }
</style>