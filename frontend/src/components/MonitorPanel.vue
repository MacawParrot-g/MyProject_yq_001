<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { fetchSystemInfo, fetchRedisInfo, fetchRedisKeys, fetchLogTail, terminalExec } from '../api/index.js'

const emit = defineEmits(['error'])

const activeTab = ref('server')

const sysInfo = ref(null)
const sysLoading = ref(false)

const logLines = ref([])
const logLoading = ref(false)
const logLevel = ref('ALL')
const logAutoScroll = ref(true)
const logPaused = ref(false)
const logTailCount = ref(300)
let logTimer = null
const logContainer = ref(null)

const redisInfo = ref(null)
const redisLoading = ref(false)
const selectedDb = ref(-1)
const redisKeys = ref([])
const redisKeysLoading = ref(false)

const termCommand = ref('')
const termOutput = ref('')
const termExecuting = ref(false)
const termHistory = ref([])
const termHistoryIndex = ref(-1)
const termExitCode = ref(null)
const termElapsed = ref(null)
const termContainer = ref(null)
const termInput = ref(null)

async function loadSystemInfo() {
  sysLoading.value = true
  try {
    const json = await fetchSystemInfo()
    if (json.success) sysInfo.value = json.data
  } catch (e) { /* silent */ }
  finally { sysLoading.value = false }
}

async function loadRedisInfo() {
  redisLoading.value = true
  try {
    const json = await fetchRedisInfo()
    if (json.success) redisInfo.value = json.data
  } catch (e) { emit('error', 'Redis信息获取失败: ' + e.message) }
  finally { redisLoading.value = false }
}

async function loadRedisKeys(db) {
  selectedDb.value = db
  redisKeysLoading.value = true
  redisKeys.value = []
  try {
    const json = await fetchRedisKeys(db, 200)
    if (json.success) redisKeys.value = json.data?.keys || []
  } catch (e) { emit('error', 'Redis键查询失败: ' + e.message) }
  finally { redisKeysLoading.value = false }
}

function closeKeyViewer() {
  selectedDb.value = -1
  redisKeys.value = []
}

async function loadLogs() {
  if (logPaused.value) return
  logLoading.value = true
  try {
    const json = await fetchLogTail(logTailCount.value, logLevel.value)
    if (json.success) {
      logLines.value = json.data || []
      if (logAutoScroll.value) {
        nextTick(() => {
          if (logContainer.value) {
            logContainer.value.scrollTop = logContainer.value.scrollHeight
          }
        })
      }
    }
  } catch (e) { /* silent */ }
  finally { logLoading.value = false }
}

function startLogPolling() {
  stopLogPolling()
  loadLogs()
  logTimer = setInterval(loadLogs, 3000)
}

function stopLogPolling() {
  if (logTimer) { clearInterval(logTimer); logTimer = null }
}

function togglePause() {
  logPaused.value = !logPaused.value
}

function getLogLineClass(line) {
  if (line.includes('ERROR') || line.includes('Exception')) return 'log-error'
  if (line.includes('WARN')) return 'log-warn'
  if (line.includes('DEBUG')) return 'log-debug'
  if (line.includes('INFO')) return 'log-info'
  return ''
}

function formatUptime(seconds) {
  const s = parseInt(seconds)
  if (isNaN(s)) return 'N/A'
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  return d + '天 ' + h + '小时 ' + m + '分钟'
}

function getDbKeyCount(dbIndex) {
  if (!redisInfo.value?.databases) return 0
  return redisInfo.value.databases[dbIndex]?.keys || 0
}

async function executeTerminalCommand() {
  const cmd = termCommand.value.trim()
  if (!cmd || termExecuting.value) return
  termExecuting.value = true
  termExitCode.value = null
  termElapsed.value = null
  termOutput.value = ''

  termHistory.value.unshift(cmd)
  if (termHistory.value.length > 50) termHistory.value.pop()
  termHistoryIndex.value = -1

  try {
    const json = await terminalExec(cmd)
    if (json.success && json.data) {
      const d = json.data
      let result = ''
      if (d.output) result += d.output
      if (d.error) result += (result ? '\n' : '') + '[STDERR] ' + d.error
      if (!result) result = '(无输出)'
      termOutput.value = result
      termExitCode.value = d.exitCode
      termElapsed.value = d.elapsed
    } else {
      termOutput.value = '[ERROR] ' + (json.message || '命令执行失败')
      termExitCode.value = -1
    }
  } catch (e) {
    termOutput.value = '[ERROR] 请求失败: ' + e.message
    termExitCode.value = -1
  } finally {
    termExecuting.value = false
    termCommand.value = ''
    nextTick(() => {
      if (termContainer.value) {
        termContainer.value.scrollTop = termContainer.value.scrollHeight
      }
      if (termInput.value) termInput.value.focus()
    })
  }
}

function handleTermKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    executeTerminalCommand()
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    if (termHistory.value.length > 0) {
      if (termHistoryIndex.value < termHistory.value.length - 1) {
        termHistoryIndex.value++
      }
      termCommand.value = termHistory.value[termHistoryIndex.value]
    }
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    if (termHistoryIndex.value > 0) {
      termHistoryIndex.value--
      termCommand.value = termHistory.value[termHistoryIndex.value]
    } else {
      termHistoryIndex.value = -1
      termCommand.value = ''
    }
  }
}

function clearTerminal() {
  termOutput.value = ''
  termExitCode.value = null
  termElapsed.value = null
}

onMounted(() => {
  loadSystemInfo()
  loadRedisInfo()
  startLogPolling()
})

onUnmounted(() => {
  stopLogPolling()
})
</script>

<template>
  <div class="monitor-page">
    <div class="page-header">
      <h2>🖥️ 系统监控面板</h2>
      <div class="page-header-sub">实时日志 · Redis监控 · 服务器状态 · 远程终端</div>
    </div>

    <div class="monitor-tabs">
      <button class="monitor-tab" :class="{ active: activeTab === 'server' }" @click="activeTab = 'server'; loadSystemInfo()">
        <span class="tab-icon">📊</span><span>服务器状态</span>
      </button>
      <button class="monitor-tab" :class="{ active: activeTab === 'logs' }" @click="activeTab = 'logs'">
        <span class="tab-icon">📜</span><span>实时日志</span>
      </button>
      <button class="monitor-tab" :class="{ active: activeTab === 'redis' }" @click="activeTab = 'redis'; loadRedisInfo()">
        <span class="tab-icon">🗄️</span><span>Redis监控</span>
      </button>
      <button class="monitor-tab" :class="{ active: activeTab === 'terminal' }" @click="activeTab = 'terminal'">
        <span class="tab-icon">💻</span><span>远程终端</span>
      </button>
    </div>

    <!-- ==================== 服务器状态 ==================== -->
    <div v-if="activeTab === 'server'" class="monitor-section">
      <div class="card">
        <div class="card-header card-header-between">
          <div class="card-header-left">
            <span class="card-icon">📊</span>
            <h3>服务器运行状态</h3>
          </div>
          <button class="btn-action btn-refresh" @click="loadSystemInfo" :disabled="sysLoading">
            {{ sysLoading ? '加载中...' : '🔄 刷新' }}
          </button>
        </div>
        <div class="card-body">
          <div v-if="sysInfo" class="sys-body">
            <div class="sys-grid">
              <div class="sys-item">
                <span class="sys-label">操作系统</span>
                <span class="sys-val">{{ sysInfo.osName }}</span>
              </div>
              <div class="sys-item">
                <span class="sys-label">系统架构</span>
                <span class="sys-val">{{ sysInfo.osArch }}</span>
              </div>
              <div class="sys-item">
                <span class="sys-label">CPU</span>
                <span class="sys-val">{{ sysInfo.cpuName }} ({{ sysInfo.cpuCores }}核)</span>
              </div>
              <div class="sys-item">
                <span class="sys-label">CPU占用</span>
                <span class="sys-val" :class="{ 'val-warn': sysInfo.cpuUsagePercent > 80 }">{{ sysInfo.cpuUsagePercent }}%</span>
              </div>
              <div class="sys-item">
                <span class="sys-label">内存总量</span>
                <span class="sys-val">{{ sysInfo.totalMemoryGB }} GB</span>
              </div>
              <div class="sys-item">
                <span class="sys-label">已用内存</span>
                <span class="sys-val" :class="{ 'val-warn': sysInfo.memoryUsagePercent > 85 }">{{ sysInfo.usedMemoryGB }} GB ({{ sysInfo.memoryUsagePercent }}%)</span>
              </div>
              <div class="sys-item">
                <span class="sys-label">可用内存</span>
                <span class="sys-val">{{ sysInfo.availableMemoryGB }} GB</span>
              </div>
              <div class="sys-item">
                <span class="sys-label">服务运行时长</span>
                <span class="sys-val">{{ sysInfo.javaUptime }}</span>
              </div>
            </div>
            <div class="meter">
              <div class="meter-header">
                <span class="meter-label">内存使用率</span>
                <span class="meter-value">{{ sysInfo.memoryUsagePercent }}%</span>
              </div>
              <div class="meter-track">
                <div class="meter-fill" :style="{ width: sysInfo.memoryUsagePercent + '%' }" :class="{ 'meter-danger': sysInfo.memoryUsagePercent > 85 }"></div>
              </div>
            </div>
            <div class="meter">
              <div class="meter-header">
                <span class="meter-label">CPU使用率</span>
                <span class="meter-value">{{ sysInfo.cpuUsagePercent }}%</span>
              </div>
              <div class="meter-track">
                <div class="meter-fill" :style="{ width: sysInfo.cpuUsagePercent + '%' }" :class="{ 'meter-danger': sysInfo.cpuUsagePercent > 80 }"></div>
              </div>
            </div>
          </div>
          <div v-else-if="sysLoading" class="state-block">
            <div class="state-spinner"></div>
            <div class="state-text">正在获取系统信息...</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 实时日志 ==================== -->
    <div v-if="activeTab === 'logs'" class="monitor-section">
      <div class="card">
        <div class="card-header card-header-between">
          <div class="card-header-left">
            <span class="card-icon">📜</span>
            <h3>实时日志</h3>
            <span class="log-status-dot" :class="{ 'log-paused': logPaused }"></span>
            <span class="log-status-text">{{ logPaused ? '已暂停' : '实时监控中' }}</span>
          </div>
          <div class="card-header-right">
            <select v-model="logLevel" class="log-level-select" @change="loadLogs()">
              <option value="ALL">全部级别</option>
              <option value="INFO">INFO</option>
              <option value="WARN">WARN</option>
              <option value="ERROR">ERROR</option>
              <option value="DEBUG">DEBUG</option>
            </select>
            <button class="btn-action btn-sm-action" @click="togglePause">
              {{ logPaused ? '▶ 恢复' : '⏸ 暂停' }}
            </button>
            <button class="btn-action btn-refresh" @click="loadLogs" :disabled="logLoading">
              {{ logLoading ? '刷新中...' : '🔄 刷新' }}
            </button>
          </div>
        </div>
        <div class="card-body log-card-body">
          <div class="log-viewer" ref="logContainer">
            <div v-if="logLines.length === 0 && !logLoading" class="log-empty">暂无日志数据...</div>
            <div v-for="(line, i) in logLines" :key="i" class="log-line" :class="getLogLineClass(line)">{{ line }}</div>
            <div v-if="logLoading && logLines.length === 0" class="log-empty">
              <div class="state-spinner"></div>
              <div>正在加载日志...</div>
            </div>
          </div>
          <div class="log-footer">
            <label class="log-auto-scroll-label">
              <input type="checkbox" v-model="logAutoScroll" />
              <span>自动滚动</span>
            </label>
            <span class="log-count">共 {{ logLines.length }} 行</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== Redis监控 ==================== -->
    <div v-if="activeTab === 'redis'" class="monitor-section">
      <div class="card">
        <div class="card-header card-header-between">
          <div class="card-header-left">
            <span class="card-icon">🗄️</span>
            <h3>Redis 数据库监控</h3>
            <span v-if="redisInfo" class="redis-host-badge">{{ redisInfo.host }}</span>
          </div>
          <button class="btn-action btn-refresh" @click="loadRedisInfo" :disabled="redisLoading">
            {{ redisLoading ? '加载中...' : '🔄 刷新' }}
          </button>
        </div>
        <div class="card-body">
          <div v-if="redisInfo" class="redis-body">
            <div class="redis-server-info">
              <div class="redis-si-item">
                <span class="redis-si-label">版本</span>
                <span class="redis-si-val">{{ redisInfo.redisVersion }}</span>
              </div>
              <div class="redis-si-item">
                <span class="redis-si-label">已用内存</span>
                <span class="redis-si-val">{{ redisInfo.usedMemory }}</span>
              </div>
              <div class="redis-si-item">
                <span class="redis-si-label">峰值内存</span>
                <span class="redis-si-val">{{ redisInfo.usedMemoryPeak }}</span>
              </div>
              <div class="redis-si-item">
                <span class="redis-si-label">连接数</span>
                <span class="redis-si-val">{{ redisInfo.connectedClients }}</span>
              </div>
              <div class="redis-si-item">
                <span class="redis-si-label">运行时长</span>
                <span class="redis-si-val">{{ formatUptime(redisInfo.uptimeSeconds) }}</span>
              </div>
            </div>

            <div class="redis-db-grid">
              <div v-for="dbInfo in redisInfo.databases" :key="dbInfo.db"
                   class="redis-db-card"
                   :class="{ 'redis-db-card-active': selectedDb === dbInfo.db, 'redis-db-empty': dbInfo.keys === 0 }"
                   @click="loadRedisKeys(dbInfo.db)">
                <div class="redis-db-num">DB {{ dbInfo.db }}</div>
                <div class="redis-db-keys">{{ dbInfo.keys }}</div>
                <div class="redis-db-keys-label">keys</div>
              </div>
            </div>

            <div v-if="selectedDb >= 0" class="redis-key-viewer">
              <div class="rkv-header">
                <h4>🔑 DB{{ selectedDb }} 的键</h4>
                <button class="btn-sm-action" @click="closeKeyViewer">✕ 关闭</button>
              </div>
              <div v-if="redisKeysLoading" class="rkv-loading">
                <div class="state-spinner"></div>
                <span>正在查询...</span>
              </div>
              <div v-else-if="redisKeys.length === 0" class="rkv-empty">该数据库暂无数据</div>
              <div v-else class="rkv-table-wrap">
                <table class="rkv-table">
                  <thead>
                  <tr>
                    <th>Key</th>
                    <th>类型</th>
                    <th>TTL(秒)</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr v-for="k in redisKeys" :key="k.key">
                    <td class="rkv-key" :title="k.key">{{ k.key }}</td>
                    <td><span class="rkv-type" :class="'rkv-type-' + k.type">{{ k.type }}</span></td>
                    <td class="rkv-ttl">{{ k.ttl === -1 ? '永久' : k.ttl }}</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
          <div v-else-if="redisLoading" class="state-block">
            <div class="state-spinner"></div>
            <div class="state-text">正在获取Redis信息...</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 远程终端 ==================== -->
    <div v-if="activeTab === 'terminal'" class="monitor-section">
      <div class="card">
        <div class="card-header card-header-between">
          <div class="card-header-left">
            <span class="card-icon">💻</span>
            <h3>远程终端</h3>
            <span class="term-warning-badge">⚠️ 谨慎操作</span>
          </div>
          <div class="card-header-right">
            <button class="btn-action btn-sm-action" @click="clearTerminal">🗑 清屏</button>
          </div>
        </div>
        <div class="card-body term-card-body">
          <div class="term-output-area" ref="termContainer">
            <div v-if="!termOutput && !termExecuting" class="term-placeholder">
              <div class="term-placeholder-icon">💻</div>
              <div class="term-placeholder-text">在下方输入命令，按 Enter 执行</div>
              <div class="term-placeholder-hint">支持 ↑↓ 键浏览历史命令 · Ctrl+Enter 换行</div>
            </div>
            <div v-if="termOutput" class="term-output-content">
              <pre class="term-pre">{{ termOutput }}</pre>
            </div>
            <div v-if="termExecuting" class="term-executing">
              <div class="state-spinner" style="width:18px;height:18px;border-width:2px;margin:0;display:inline-block;vertical-align:middle;margin-right:8px;"></div>
              正在执行命令...
            </div>
          </div>
          <div class="term-status-bar" v-if="termExitCode !== null">
            <span class="term-exit-code" :class="{ 'term-exit-ok': termExitCode === 0, 'term-exit-err': termExitCode !== 0 }">
              Exit Code: {{ termExitCode }}
            </span>
            <span class="term-elapsed" v-if="termElapsed !== null">耗时: {{ termElapsed }}ms</span>
          </div>
          <div class="term-input-area">
            <span class="term-prompt">$</span>
            <textarea
                ref="termInput"
                v-model="termCommand"
                class="term-input"
                placeholder="输入命令... (Enter 执行, ↑↓ 历史)"
                rows="1"
                @keydown="handleTermKeydown"
                :disabled="termExecuting"
            ></textarea>
            <button class="btn-action term-exec-btn" @click="executeTerminalCommand" :disabled="termExecuting || !termCommand.trim()">
              {{ termExecuting ? '执行中...' : '▶ 执行' }}
            </button>
          </div>
          <div class="term-history-bar" v-if="termHistory.length > 0">
            <span class="term-history-label">最近命令:</span>
            <div class="term-history-items">
              <span v-for="(cmd, i) in termHistory.slice(0, 8)" :key="i" class="term-history-item" @click="termCommand = cmd" :title="cmd">
                {{ cmd.length > 40 ? cmd.substring(0, 40) + '...' : cmd }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.monitor-page { width: 100%; max-width: 1600px; margin: 0 auto; padding: 0 20px 40px; box-sizing: border-box; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 2px solid #f0f0f0; }
.page-header h2 { margin: 0; font-size: 22px; color: #1a1a2e; font-weight: 700; }
.page-header-sub { font-size: 13px; color: #999; }

.monitor-tabs { display: flex; gap: 4px; background: #f4f5f7; border-radius: 12px; padding: 4px; margin-bottom: 24px; }
.monitor-tab { flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 10px 0; font-size: 14px; border: none; border-radius: 10px; cursor: pointer; background: transparent; color: #888; font-weight: 600; transition: all 0.25s ease; }
.monitor-tab.active { background: #fff; color: #1a1a2e; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.monitor-tab:not(.active):hover { color: #555; background: rgba(255,255,255,0.5); }
.tab-icon { font-size: 16px; }

.monitor-section { animation: fadeUp 0.3s ease; }
@keyframes fadeUp { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.card { background: #fff; border-radius: 14px; padding: 0; margin-bottom: 20px; border: 1px solid #eaeaea; box-shadow: 0 1px 4px rgba(0,0,0,0.04); overflow: hidden; }
.card-header { display: flex; align-items: center; gap: 8px; padding: 16px 20px; border-bottom: 1px solid #f0f0f0; }
.card-header h3 { margin: 0; font-size: 15px; color: #333; font-weight: 700; }
.card-header-between { justify-content: space-between; }
.card-header-left { display: flex; align-items: center; gap: 8px; }
.card-header-right { display: flex; align-items: center; gap: 8px; }
.card-icon { font-size: 18px; }
.card-body { padding: 20px; }

.sys-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
@media (max-width: 900px) { .sys-grid { grid-template-columns: 1fr 1fr; } }
.sys-item { display: flex; flex-direction: column; gap: 4px; padding: 12px 16px; background: #f8f9fc; border-radius: 10px; }
.sys-label { font-size: 11px; color: #999; font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px; }
.sys-val { font-size: 14px; color: #333; font-weight: 600; }
.val-warn { color: #ef4444; }

.meter { margin-bottom: 12px; }
.meter-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.meter-label { font-size: 12px; color: #666; font-weight: 600; }
.meter-value { font-size: 13px; font-weight: 700; color: #333; }
.meter-track { height: 10px; background: #f0f0f0; border-radius: 5px; overflow: hidden; }
.meter-fill { height: 100%; background: linear-gradient(90deg, #43e97b, #38f9d7); border-radius: 5px; transition: width 0.5s ease; }
.meter-fill.meter-danger { background: linear-gradient(90deg, #f87171, #ef4444); }

.state-block { text-align: center; padding: 50px 20px; }
.state-text { color: #aaa; font-size: 14px; }
.state-spinner { width: 32px; height: 32px; border: 3px solid #e0e0e0; border-top-color: #667eea; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 12px; }
@keyframes spin { to { transform: rotate(360deg); } }

.btn-action { border: none; padding: 10px 24px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600; transition: transform 0.15s, box-shadow 0.15s; }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }
.btn-refresh { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 6px 18px; font-size: 12px; }
.btn-refresh:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(102,126,234,0.35); }
.btn-sm-action { background: #f4f5f7; color: #666; border: 1pxpx solid #e0e0e0; padding: 6px 16px; font-size: 12px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: all 0.2s; }
.btn-sm-action:hover { background: #e8e8e8; color: #333; }

/* ===== 日志查看器 ===== */
.log-card-body { padding: 0 !important; }
.log-status-dot { width: 10px; height: 10px; border-radius: 50%; background: #22c55e; display: inline-block; animation: pulse-dot 2s ease-in-out infinite; }
.log-status-dot.log-paused { background: #f59e0b; animation: none; }
@keyframes pulse-dot { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
.log-status-text { font-size: 12px; color: #888; font-weight: 500; }

.log-level-select { padding: 6px 12px; font-size: 12px; border: 2px solid #e8e8e8; border-radius: 8px; outline: none; cursor: pointer; background: #fff; }
.log-level-select:focus { border-color: #667eea; }

.log-viewer { background: #1a1a2e; color: #d4d4d8; font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace; font-size: 12px; line-height: 1.7; padding: 16px 20px; min-height: 400px; max-height: 600px; overflow-y: auto; }
.log-line { padding: 1px 0; white-space: pre-wrap; word-break: break-all; border-bottom: 1px solid rgba(255,255,255,0.03); }
.log-line:hover { background: rgba(255,255,255,0.05); }
.log-error { color: #f87171; font-weight: 600; }
.log-warn { color: #fbbf24; }
.log-info { color: #60a5fa; }
.log-debug { color: #a78bfa; }
.log-empty { color: #666; text-align: center; padding: 60px 20px; }

.log-footer { display: flex; justify-content: space-between; align-items: center; padding: 10px 20px; background: #f8f9fc; border-top: 1px solid #f0f0f0; }
.log-auto-scroll-label { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #666; cursor: pointer; }
.log-count { font-size: 12px; color: #999; }

/* ===== Redis监控 ===== */
.redis-server-info { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 24px; }
@media (max-width: 900px) { .redis-server-info { grid-template-columns: repeat(3, 1fr); } }
.redis-si-item { display: flex; flex-direction: column; gap: 4px; padding: 12px 16px; background: linear-gradient(135deg, #f8f9fc, #eef2ff); border-radius: 10px; border: 1px solid #e8eaef; }
.redis-si-label { font-size: 11px; color: #999; font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px; }
.redis-si-val { font-size: 14px; color: #333; font-weight: 700; }
.redis-host-badge { font-size: 11px; font-weight: 600; color: #667eea; background: #eef2ff; padding: 3px 12px; border-radius: 20px; }

.redis-db-grid { display: grid; grid-template-columns: repeat(8, 1fr); gap: 12px; }
@media (max-width: 1100px) { .redis-db-grid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 600px) { .redis-db-grid { grid-template-columns: repeat(4, 1fr); } }

.redis-db-card { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 16px 8px; background: #f8f9fc; border-radius: 12px; border: 2px solid #e8eaef; cursor: pointer; transition: all 0.2s; }
.redis-db-card:hover { border-color: #667eea; transform: translateY(-2px); box-shadow: 0 4px 16px rgba(102,126,234,0.15); }
.redis-db-card-active { border-color: #667eea; background: #eef2ff; box-shadow: 0 4px 16px rgba(102,126,234,0.2); }
.redis-db-empty { opacity: 0.5; }
.redis-db-empty:hover { opacity: 0.8; }
.redis-db-num { font-size: 11px; font-weight: 700; color: #999; text-transform: uppercase; margin-bottom: 4px; }
.redis-db-keys { font-size: 24px; font-weight: 800; color: #1a1a2e; }
.redis-db-keys-label { font-size: 10px; color: #bbb; font-weight: 600; text-transform: uppercase; }

.redis-key-viewer { margin-top: 20px; border: 1px solid #e8eaef; border-radius: 12px; overflow: hidden; }
.rkv-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; background: #f8f9fc; border-bottom: 1px solid #f0f0f0; }
.rkv-header h4 { margin: 0; font-size: 14px; font-weight: 700; color: #333; }
.rkv-loading { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 40px 20px; color: #999; font-size: 14px; }
.rkv-empty { text-align: center; padding: 40px 20px; color: #aaa; font-size: 14px; }
.rkv-table-wrap { overflow-x: auto; max-height: 400px; overflow-y: auto; }
.rkv-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; }
.rkv-table th { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 10px 14px; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.3px; position: sticky; top: 0; z-index: 1; }
.rkv-table td { padding: 9px 14px; border-bottom: 1px solid #f0f0f0; color: #333; }
.rkv-table tbody tr:hover td { background: #f8f9ff; }
.rkv-key { max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-family: 'SF Mono', 'Fira Code', monospace; font-size: 11px; color: #555; }
.rkv-type { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; }
.rkv-type-string { background: #dcfce7; color: #166534; }
.rkv-type-list { background: #e0e7ff; color: #3730a3; }
.rkv-type-set { background: #fef3c7; color: #92400e; }
.rkv-type-zset { background: #fce7f3; color: #9d174d; }
.rkv-type-hash { background: #e0f2fe; color: #075985; }
.rkv-ttl { font-family: 'SF Mono', monospace; font-weight: 600; color: #666; }

/* ===== 远程终端 ===== */
.term-card-body { padding: 0 !important; }
.term-warning-badge { font-size: 11px; font-weight: 700; color: #dc2626; background: #fef2f2; padding: 3px 10px; border-radius: 20px; border: 1px solid #fecaca; }
.term-output-area { background: #0d1117; color: #c9d1d9; font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace; font-size: 13px; line-height: 1.6; min-height: 350px; max-height: 550px; overflow-y: auto; padding: 16px 20px; }
.term-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 300px; color: #484f58; }
.term-placeholder-icon { font-size: 48px; margin-bottom: 12px; opacity: 0.5; }
.term-placeholder-text { font-size: 14px; margin-bottom: 6px; }
.term-placeholder-hint { font-size: 12px; opacity: 0.6; }
.term-output-content { margin-bottom: 8px; }
.term-pre { margin: 0; white-space: pre-wrap; word-break: break-all; font-family: inherit; font-size: inherit; color: inherit; }
.term-executing { color: #58a6ff; padding: 8px 0; display: flex; align-items: center; font-size: 13px; }
.term-status-bar { display: flex; align-items: center; gap: 16px; padding: 8px 20px; background: #161b22; border-top: 1px solid #21262d; font-size: 12px; }
.term-exit-code { font-weight: 700; font-family: 'SF Mono', monospace; }
.term-exit-ok { color: #3fb950; }
.term-exit-err { color: #f85149; }
.term-elapsed { color: #8b949e; font-family: 'SF Mono', monospace; }
.term-input-area { display: flex; align-items: flex-start; gap: 8px; padding: 12px 20px; background: #161b22; border-top: 1px solid #21262d; }
.term-prompt { color: #3fb950; font-family: 'SF Mono', monospace; font-size: 14px; font-weight: 700; line-height: 32px; flex-shrink: 0; }
.term-input { flex: 1; background: #0d1117; color: #c9d1d9; border: 1px solid #30363d; border-radius: 6px; padding: 6px 12px; font-family: 'SF Mono', 'Fira Code', monospace; font-size: 13px; resize: none; outline: none; min-height: 32px; line-height: 1.5; }
.term-input:focus { border-color: #58a6ff; box-shadow: 0 0 0 2px rgba(88,166,255,0.15); }
.term-input:disabled { opacity: 0.5; }
.term-exec-btn { background: linear-gradient(135deg, #238636, #2ea043) !important; color: #fff !important; padding: 6px 20px !important; font-size: 12px !important; border-radius: 6px !important; flex-shrink: 0; }
.term-exec-btn:hover:not(:disabled) { box-shadow: 0 2px 10px rgba(46,160,67,0.4) !important; }
.term-history-bar { display: flex; align-items: flex-start; gap: 8px; padding: 10px 20px; background: #f8f9fc; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
.term-history-label { font-size: 11px; color: #999; font-weight: 600; flex-shrink: 0; line-height: 24px; }
.term-history-items { display: flex; gap: 6px; flex-wrap: wrap; }
.term-history-item { font-size: 11px; font-family: 'SF Mono', monospace; color: #555; background: #e8e8e8; padding: 3px 10px; border-radius: 6px; cursor: pointer; transition: all 0.15s; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.term-history-item:hover { background: #667eea; color: #fff; }
</style>