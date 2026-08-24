<template>
  <div class="dev-mode">
    <div class="dev-badge">🛠️ 开发者模式</div>

    <div class="top-bar">
      <div v-if="duplicateTip" class="duplicate-tip">⚠️ {{ duplicateTip }}</div>
      <div v-if="polling" class="polling-section">
        <span class="polling-status">🔄 正在每10秒自动获取下载任务...（第{{ pollCount }}次）</span>
        <button class="btn-timer-cancel" @click="stopPolling">停止轮询</button>
      </div>
      <div class="top-actions">
        <button class="btn-refresh" @click="confirmRefresh" :disabled="loading">
          {{ loading ? '加载中...' : '🔄 刷新数据' }}
        </button>
      </div>
    </div>

    <div class="manual-section">
      <div class="input-group">
        <input type="text" v-model="appId" placeholder="输入 App ID 获取下载链接" @keyup.enter="fetchByAppId" />
        <button class="btn-refresh" @click="fetchByAppId" :disabled="loading || !appId.trim()">
          {{ loading ? '获取中...' : '生成' }}
        </button>
      </div>
    </div>

    <div v-if="loading && !downloadUrl" class="loading">正在获取数据...</div>

    <div class="empty-placeholder" v-if="!downloadUrl && !loading">
      <div class="empty-icon">🔧</div>
      <div class="empty-text">点击刷新数据获取任务，或输入 App ID 生成下载链接</div>
    </div>

    <div class="qr-info-section" v-if="downloadUrl">
      <div class="qr-card">
        <div class="qr-card-header">
          <span class="qr-card-icon">📱</span>
          <span class="qr-card-title">扫描二维码下载APP进行测试</span>
        </div>
        <div class="qrcode-container">
          <canvas ref="qrCanvas"></canvas>
        </div>
      </div>
      <div class="side-info">
        <div class="info-card">
          <div class="info-card-label">Bundle ID</div>
          <div class="info-card-value">{{ bundleId }}</div>
        </div>
        <div class="info-card">
          <div class="info-card-label">原始CurrentTargetNum数</div>
          <div class="info-card-value">{{ originalCurrentTargetNum }}</div>
        </div>
      </div>
    </div>

    <div class="action-row" v-if="bundleId">
      <button class="btn-event" @click="queryEvent" :disabled="eventLoading">
        {{ eventLoading ? '查询中...' : '查询事件' }}
      </button>
      <button class="btn-event" @click="queryEventWithFullMsg" :disabled="eventLoading">
        查看JSON
      </button>
    </div>

    <div class="timer-panel" v-if="bundleId">
      <div class="timer-left">
        <div class="timer-row">
          <label class="timer-label">定时查询</label>
          <input class="timer-input" type="number" v-model.number="timerSeconds" min="1" max="3600" placeholder="秒数" />
          <button class="btn-timer" @click="startTimer" :disabled="timerCountdown > 0 || !timerSeconds || timerSeconds < 1">
            {{ timerCountdown > 0 ? timerCountdown + 's' : '开始定时' }}
          </button>
          <button class="btn-timer-cancel" v-if="timerCountdown > 0" @click="cancelTimer">取消</button>
        </div>
        <div class="timer-status" v-if="timerMsg">{{ timerMsg }}</div>
      </div>
      <div class="timer-divider"></div>
      <div class="timer-right">
        <template v-if="eventResult">
          <div class="event-result no-event" v-if="eventResult === 'no_event'">
            <div class="event-label">查询结果</div>
            <div class="event-value">✅ 无事件</div>
            <button class="btn-frozen" @click="doFrozen" :disabled="frozenLoading">
              {{ frozenLoading ? '冻结中...' : '冻结应用' }}
            </button>
            <div class="frozen-result" v-if="frozenMsg">
              <div class="frozen-label">冻结结果</div>
              <div class="frozen-value">{{ frozenMsg }}</div>
            </div>
          </div>
          <div class="event-result" v-if="eventResult === 'has_event'">
            <div class="event-label">最新 currentTargetNum</div>
            <div class="event-value highlight">{{ newCurrentTargetNum }}</div>
            <div class="event-compare">
              原始值：<span class="diff">{{ originalCurrentTargetNum }}</span> → 最新值：<span class="diff">{{ newCurrentTargetNum }}</span>
            </div>
            <div class="attribution-tags" v-if="attributions.length > 0">
              <span class="attr-tag" v-for="attr in attributions" :key="attr" :class="attr">{{ attr }}</span>
            </div>
            <div class="no-attribution" v-if="attributions.length === 0">⚠️ 无归因</div>
          </div>
        </template>
        <div class="event-placeholder" v-else>
          <span class="event-placeholder-icon">🔍</span>
          <span>查询的事件会在这里显示</span>
        </div>
      </div>
    </div>

    <!-- Four Attribution Panels -->
    <div class="attr-section">
      <h3 class="section-title">🔍 归因查询</h3>
      <div class="attr-grid">
        <div v-for="type in ATTR_TYPES" :key="type" class="attr-panel" :class="type">
          <div class="attr-panel-header">
            <span class="attr-panel-icon">{{ attrIcon(type) }}</span>
            <span class="attr-panel-title">{{ type.toUpperCase() }}</span>
            <span class="attr-count" v-if="attrStates[type].count !== null">({{ attrStates[type].count }}条)</span>
          </div>
          <div class="attr-input-row">
            <input v-model="attrStates[type].bundleId" :placeholder="'Bundle ID'" @keyup.enter="querySingleAttr(type)" />
            <button class="btn-attr-query" @click="querySingleAttr(type)" :disabled="attrStates[type].loading || !attrStates[type].bundleId">
              {{ attrStates[type].loading ? '...' : '查询' }}
            </button>
          </div>
          <div v-if="attrStates[type].data.length > 0" class="attr-results">
            <div v-for="(record, rIdx) in attrStates[type].data" :key="rIdx" class="attr-record">
              <div v-for="(val, key) in record" :key="key" class="attr-field">
                <span class="attr-field-key">{{ key }}</span>
                <span class="attr-field-val">{{ formatAttrValue(val) }}</span>
              </div>
            </div>
          </div>
          <div v-if="attrStates[type].queried && attrStates[type].data.length === 0 && !attrStates[type].loading" class="attr-empty">
            无数据
          </div>
        </div>
      </div>
    </div>

    <!-- Insert Form -->
    <div class="form-section" v-if="downloadUrl && eventResult">
      <h4>填写入库信息（直接写入数据库）</h4>
      <div class="form-group">
        <label>异常类型</label>
        <select v-model="form.exception_type">
          <option value="" disabled>请选择异常类型</option>
          <option v-for="opt in exceptionOptions" :key="opt" :value="opt">{{ opt }}</option>
        </select>
      </div>
      <div class="form-group">
        <label>备注</label>
        <textarea v-model="form.remark" placeholder="手动输入(如果用了模板此处禁填)"></textarea>
        <br/>
        <select v-model="form.remark">
          <option value="" disabled>模板(可选)</option>
          <option v-for="opt in templateOptions" :key="opt" :value="opt">{{ opt }}</option>
        </select>
      </div>
      <div style="display:flex; gap:12px;">
        <div class="form-group" style="flex:1">
          <label>记录人</label>
          <input v-model="form.recorder" />
        </div>
        <div class="form-group" style="flex:1">
          <label>记录日期</label>
          <input v-model="form.record_data" />
        </div>
      </div>
      <button class="btn-save" @click="saveToMySQL" :disabled="saving">
        {{ saving ? '入库中...' : '直接入库' }}
      </button>
      <div class="save-success" v-if="saveMsg">{{ saveMsg }}</div>
    </div>

    <!-- History Section -->
    <div class="history-section">
      <div class="history-header">
        <h3 class="section-title">📋 今日测试历史</h3>
        <div class="history-actions">
          <span v-if="redisStatusMsg" class="redis-status" :class="redisOk ? 'ok' : 'warn'">{{ redisStatusMsg }}</span>
          <button class="btn-history-action" @click="loadHistory">刷新</button>
          <button class="btn-history-action btn-danger" @click="clearHistory" :disabled="historyList.length === 0">清空</button>
        </div>
      </div>
      <div v-if="historyLoading" class="loading">加载历史记录...</div>
      <div v-if="!historyLoading && historyList.length === 0" class="history-empty">暂无测试记录</div>
      <div v-if="historyList.length > 0" class="history-table-wrapper">
        <table class="history-table">
          <thead>
          <tr>
            <th>时间</th>
            <th>Bundle ID</th>
            <th>事件数</th>
            <th>归因</th>
            <th>异常类型</th>
            <th>备注</th>
            <th>记录人</th>
            <th>日期</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(item, idx) in historyList" :key="idx">
            <td>{{ item.timestamp || '-' }}</td>
            <td class="cell-mono">{{ item.bundleId || '-' }}</td>
            <td>{{ item.eventNumber ?? '-' }}</td>
            <td>
              <span v-if="item.ascribe" class="attr-tag-inline">{{ item.ascribe }}</span>
              <span v-else>-</span>
            </td>
            <td>{{ item.exceptionType || '-' }}</td>
            <td class="cell-remark" :title="item.remark">{{ item.remark || '-' }}</td>
            <td>{{ item.recorder || '-' }}</td>
            <td>{{ item.recordData || '-' }}</td>
            <td>
              <button class="btn-del-row" @click="deleteHistoryRecord(item.timestamp)" title="删除">×</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Retest Modal -->
    <div class="modal-overlay" v-if="showRetestModal" @click.self="showRetestModal = false">
      <div class="modal-box">
        <div class="modal-title">库存已耗尽</div>
        <div class="modal-desc">当前没有可用的下载任务，请选择操作：</div>
        <div class="modal-actions">
          <button class="btn-modal btn-poll" @click="showRetestModal = false; startPolling()">继续轮询</button>
          <button class="btn-modal btn-retest" @click="retestFlow" :disabled="retestLoading">
            {{ retestLoading ? '获取中...' : '复测' }}
          </button>
          <button class="btn-modal btn-cancel" @click="showRetestModal = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick, onUnmounted } from 'vue'
import QRCode from 'qrcode'
import {
  fetchTask, fetchObtain, fetchEvent, fetchAllAttributions, fetchAttribution,
  fetchFrozen, insertRecord, fetchRandomForRetest,
  fetchDevHistory, saveDevHistory, clearDevHistory, fetchDevRedisStatus, deleteDevHistoryRecord
} from '../api/index.js'

const emit = defineEmits(['error', 'record-saved'])

const exceptionOptions = ['正常','iOS16闪退','iOS13/14/16均闪退','需要iOS18以上','地区不支持','硬件版本过低','超过10分钟0上报','越狱检测','其他','验证已解决','测试']
const templateOptions = ['需要iOS17以上','需要登陆后使用，无法注册','卡死在加载页进不去','非英语汉语软件，看不懂','网络检测，无法进入','需要订阅后使用','禁止入库']
const ATTR_TYPES = ['appflyer', 'adjust', 'singular', 'tenjin']
const MAX_RETRIES = 5

const downloadUrl = ref('')
const bundleId = ref('')
const originalCurrentTargetNum = ref(null)
const eventResult = ref('')
const newCurrentTargetNum = ref(null)
const attributions = ref([])
const eventId = ref(null)
const frozenMsg = ref('')
const duplicateTip = ref('')
const loading = ref(false)
const eventLoading = ref(false)
const frozenLoading = ref(false)
const saving = ref(false)
const saveMsg = ref('')
const appId = ref('')
const showRetestModal = ref(false)
const retestLoading = ref(false)
const qrCanvas = ref(null)
const timerSeconds = ref(60)
const timerCountdown = ref(0)
const timerMsg = ref('')
const polling = ref(false)
const pollCount = ref(0)
let pollTimer = null
let timerInterval = null
let isFrozen = ref('')
let isSubmit = true
let retryCount = 0

const attrStates = reactive({
  appflyer: { bundleId: '', data: [], loading: false, count: null, queried: false },
  adjust: { bundleId: '', data: [], loading: false, count: null, queried: false },
  singular: { bundleId: '', data: [], loading: false, count: null, queried: false },
  tenjin: { bundleId: '', data: [], loading: false, count: null, queried: false }
})

const form = reactive({
  exception_type: '',
  remark: '',
  recorder: localStorage.getItem('userName') || '',
  record_data: getTodayStr()
})

const historyList = ref([])
const historyLoading = ref(false)
const redisOk = ref(true)
const redisStatusMsg = ref('')

function getTodayStr() {
  const d = new Date()
  return d.getFullYear() + '/' + (d.getMonth() + 1) + '/' + d.getDate()
}

function attrIcon(type) {
  return { appflyer: '🟢', adjust: '🟡', singular: '🔵', tenjin: '🟣' }[type] || '⚪'
}

function formatAttrValue(val) {
  if (val === null || val === undefined) return '-'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}

async function renderQR(url) {
  await nextTick()
  if (url && qrCanvas.value) {
    try {
      await QRCode.toCanvas(qrCanvas.value, url, { width: 256, margin: 2, color: { dark: '#000000', light: '#ffffff' } })
    } catch (e) { console.error('QR码生成失败:', e) }
  }
}

watch(downloadUrl, (val) => { renderQR(val) })

function fillAttrBundleIds(bid) {
  for (const type of ATTR_TYPES) {
    attrStates[type].bundleId = bid
  }
}

function confirmRefresh() {
  if (!isSubmit && downloadUrl.value) {
    if (confirm('请确认是否已入库该条测试数据，刷新会导致数据丢失！')) {
      fetchData()
    }
  } else {
    fetchData()
  }
}

async function fetchData() {
  loading.value = true
  emit('error', '')
  resetTaskState()
  try {
    const json = await fetchTask()
    if (json.data) {
      if (json.duplicate) {
        retryCount++
        if (retryCount > MAX_RETRIES) {
          emit('error', '已连续 ' + MAX_RETRIES + ' 次检测到重复URL，请稍后再试')
          duplicateTip.value = ''
          return
        }
        duplicateTip.value = '检测到重复URL（第' + retryCount + '次），正在为您自动刷新...'
        setTimeout(() => fetchData(), 1500)
        return
      }
      retryCount = 0
      duplicateTip.value = ''
      downloadUrl.value = json.data.downloadUrl || ''
      bundleId.value = json.data.bundleId || ''
      originalCurrentTargetNum.value = json.data.currentTargetNum ?? null
      isSubmit = false
      fillAttrBundleIds(bundleId.value)
      if (originalCurrentTargetNum.value <= 1 || originalCurrentTargetNum.value === 0) {
        alert('初始originalCurrentTargetNum数小于等于1或等于0，后续测试期间可能会没有归因，请注意')
      }
    } else {
      showRetestModal.value = true
    }
  } catch (e) {
    if (e.name === 'AbortError') emit('error', '请求超时，远程服务器响应太慢')
    else emit('error', '请求失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function fetchByAppId() {
  const id = appId.value.trim()
  if (!id) return
  loading.value = true
  emit('error', '')
  resetTaskState()
  try {
    const json = await fetchObtain(id)
    if (json.success && json.data) {
      if (json.duplicate) {
        retryCount++
        if (retryCount > MAX_RETRIES) {
          emit('error', '已连续 ' + MAX_RETRIES + ' 次检测到重复URL，请稍后再试')
          duplicateTip.value = ''
          return
        }
        duplicateTip.value = '检测到重复URL（第' + retryCount + '次），正在为您自动刷新...'
        setTimeout(() => fetchByAppId(), 1500)
        return
      }
      retryCount = 0
      duplicateTip.value = ''
      downloadUrl.value = json.data.downloadUrl || ''
      bundleId.value = json.data.bundleId || ''
      originalCurrentTargetNum.value = json.data.currentTargetNum ?? null
      isSubmit = false
      fillAttrBundleIds(bundleId.value)
      if (originalCurrentTargetNum.value <= 1 || originalCurrentTargetNum.value === 0) {
        alert('初始originalCurrentTargetNum数小于等于1或等于0，后续测试期间可能会没有归因，请注意')
      }
    } else {
      emit('error', '接口返回异常：' + (json.resultMsg || '未知错误'))
    }
  } catch (e) {
    if (e.name === 'AbortError') emit('error', '请求超时')
    else emit('error', '请求失败：' + e.message)
  } finally {
    loading.value = false
  }
}

function startPolling() {
  polling.value = true
  pollCount.value = 0
  pollTimer = setInterval(async () => {
    pollCount.value++
    try {
      const json = await fetchTask()
      if (json.data && !json.duplicate) {
        clearInterval(pollTimer)
        polling.value = false
        pollCount.value = 0
        downloadUrl.value = json.data.downloadUrl || ''
        bundleId.value = json.data.bundleId || ''
        originalCurrentTargetNum.value = json.data.currentTargetNum ?? null
        isSubmit = false
        fillAttrBundleIds(bundleId.value)
        if (originalCurrentTargetNum.value <= 1 || originalCurrentTargetNum.value === 0) {
          alert('初始originalCurrentTargetNum数小于等于1或等于0，后续测试期间可能会没有归因，请注意')
        }
      }
    } catch (e) {
      console.error('轮询请求失败:', e)
    }
  }, 10000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  polling.value = false
  pollCount.value = 0
}

async function retestFlow() {
  retestLoading.value = true
  resetTaskState()
  try {
    const dates = getPast3DaysDates()
    const json = await fetchRandomForRetest(dates)
    if (json.success && json.data) {
      showRetestModal.value = false
      downloadUrl.value = json.data.downloadUrl || ''
      bundleId.value = json.data.bundleId || ''
      fillAttrBundleIds(bundleId.value)
      try {
        const jsons = await fetchEvent(bundleId.value)
        if (jsons.success) {
          originalCurrentTargetNum.value = jsons.data.currentTargetNum ?? null
        }
      } catch (e) {
        alert('服务器无响应，请联系技术人员')
      }
      isSubmit = false
    } else {
      emit('error', json.message || '获取复测数据失败')
    }
  } catch (e) {
    emit('error', '复测请求失败：' + e.message)
  } finally {
    retestLoading.value = false
  }
}

function getPast3DaysDates() {
  const dates = []
  const d = new Date()
  for (let i = 0; i < 3; i++) {
    const t = new Date(d)
    t.setDate(d.getDate() - i)
    dates.push(t.getFullYear() + '/' + (t.getMonth() + 1) + '/' + t.getDate())
  }
  return dates
}

async function queryEvent() {
  if (!bundleId.value) return
  eventLoading.value = true
  emit('error', '')
  eventResult.value = ''; newCurrentTargetNum.value = null; attributions.value = []; eventId.value = null; frozenMsg.value = ''
  try {
    const [eventJson, attrResults] = await Promise.all([
      fetchEvent(bundleId.value),
      fetchAllAttributions(bundleId.value)
    ])
    if (eventJson.success && eventJson.data) {
      const newCurrent = eventJson.data.currentTargetNum
      eventId.value = eventJson.data.id ?? null
      if (originalCurrentTargetNum.value !== null) {
        if (newCurrent !== originalCurrentTargetNum.value) {
          eventResult.value = 'has_event'
          newCurrentTargetNum.value = newCurrent
        } else {
          eventResult.value = 'no_event'
        }
      } else {
        newCurrentTargetNum.value = newCurrent
        eventResult.value = newCurrent != null && newCurrent > 0 ? 'has_event' : 'no_event'
      }
    } else {
      emit('error', '事件接口返回异常：' + (eventJson.resultMsg || '未知错误'))
    }
    const found = []
    for (const { type, json } of attrResults) {
      if (json.success && json.data) {
        if (Array.isArray(json.data) && json.data.length > 0) found.push(type)
        else if (!Array.isArray(json.data) && Object.keys(json.data).length > 0) found.push(type)
      }
    }
    attributions.value = found
  } catch (e) {
    emit('error', '事件查询失败：' + e.message)
  } finally {
    eventLoading.value = false
  }
}

async function queryEventWithFullMsg() {
  try {
    const data = await fetchEvent(bundleId.value)
    if (data.success) alert(JSON.stringify(data, null, 2))
  } catch (e) {
    alert('服务器无响应，请联系技术人员')
  }
}

async function querySingleAttr(type) {
  const bid = attrStates[type].bundleId
  if (!bid) return
  attrStates[type].loading = true
  attrStates[type].data = []
  attrStates[type].count = null
  attrStates[type].queried = false
  try {
    const json = await fetchAttribution(bid, type)
    if (json.success) {
      let records = []
      if (Array.isArray(json.data)) {
        records = json.data
      } else if (json.data && typeof json.data === 'object') {
        records = [json.data]
      }
      attrStates[type].data = records
      attrStates[type].count = records.length
    } else {
      attrStates[type].data = []
      attrStates[type].count = 0
    }
  } catch (e) {
    attrStates[type].data = []
    attrStates[type].count = 0
  } finally {
    attrStates[type].loading = false
    attrStates[type].queried = true
  }
}

async function doFrozen() {
  if (!eventId.value) return
  frozenLoading.value = true
  emit('error', '')
  try {
    const json = await fetchFrozen(eventId.value)
    if (json.success) {
      frozenMsg.value = json.resultMsg || '操作完成'
      isFrozen.value = ',已冻结'
    } else {
      emit('error', '冻结接口返回异常：' + (json.resultMsg || '未知错误'))
    }
  } catch (e) {
    emit('error', '冻结请求失败：' + e.message)
  } finally {
    frozenLoading.value = false
  }
}

async function saveToMySQL() {
  emit('record-saved')
  if (!form.exception_type.trim()) { emit('error', '请选择异常类型'); return }
  saving.value = true
  saveMsg.value = ''
  if (newCurrentTargetNum.value === 0 || newCurrentTargetNum.value === null) {
    attributions.value = []
    alert('无新增事件token，即使原本的token有归因，也不会被设置在字段内')
  }
  let finalRemark = form.remark.trim()
  if (isFrozen.value) finalRemark += isFrozen.value
  if (newCurrentTargetNum.value > 0 && attributions.value.length === 0) finalRemark += ',无事件归因'
  try {
    const json = await insertRecord({
      URL: downloadUrl.value,
      bundleId: bundleId.value,
      ascribe: (attributions.value || []).join(';'),
      event_number: newCurrentTargetNum.value,
      exception_type: form.exception_type.trim(),
      record_data: form.record_data,
      recorder: form.recorder,
      remark: finalRemark,
      isOutput: 0
    })
    if (json.success) {
      saveMsg.value = '✅ ' + (json.resultMsg || '入库成功')
      isSubmit = true
      await saveToHistory(finalRemark)
      await loadHistory()
    } else {
      emit('error', json.resultMsg || '入库失败')
    }
  } catch (e) {
    emit('error', '入库请求失败：' + e.message)
  } finally {
    saving.value = false
  }
}

async function saveToHistory(remark) {
  try {
    await saveDevHistory({
      bundleId: bundleId.value,
      ascribe: (attributions.value || []).join(';'),
      eventNumber: newCurrentTargetNum.value,
      exceptionType: form.exception_type.trim(),
      recordData: form.record_data,
      recorder: form.recorder,
      remark: remark || form.remark.trim()
    })
  } catch (e) {
    console.warn('保存历史失败:', e)
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const json = await fetchDevHistory()
    if (json.success) {
      historyList.value = json.data || []
      if (json.message && json.message.includes('Redis不可用')) {
        redisOk.value = false
        redisStatusMsg.value = '⚠️ Redis不可用，使用本地缓存'
      } else {
        redisOk.value = true
        redisStatusMsg.value = '✅ Redis正常'
      }
    }
  } catch (e) {
    console.warn('加载历史失败:', e)
  } finally {
    historyLoading.value = false
  }
}

async function clearHistory() {
  if (!confirm('确定清空所有测试历史记录？')) return
  try {
    await clearDevHistory()
    historyList.value = []
  } catch (e) {
    console.warn('清空历史失败:', e)
  }
}

async function deleteHistoryRecord(timestamp) {
  if (!timestamp) return
  try {
    await deleteDevHistoryRecord(timestamp)
    historyList.value = historyList.value.filter(h => h.timestamp !== timestamp)
  } catch (e) {
    console.warn('删除记录失败:', e)
  }
}

function resetTaskState() {
  downloadUrl.value = ''
  bundleId.value = ''
  originalCurrentTargetNum.value = null
  eventResult.value = ''
  newCurrentTargetNum.value = null
  attributions.value = []
  eventId.value = null
  frozenMsg.value = ''
  duplicateTip.value = ''
  saveMsg.value = ''
  isFrozen.value = ''
  form.exception_type = ''
  form.remark = ''
  form.recorder = localStorage.getItem('userName') || ''
  form.record_data = getTodayStr()
  for (const type of ATTR_TYPES) {
    attrStates[type].data = []
    attrStates[type].count = null
    attrStates[type].queried = false
  }
}

function startTimer() {
  if (!timerSeconds.value || timerSeconds.value < 1) return
  timerCountdown.value = timerSeconds.value
  timerMsg.value = '将在 ' + timerCountdown.value + ' 秒后自动查询事件'
  timerInterval = setInterval(() => {
    timerCountdown.value--
    if (timerCountdown.value <= 0) {
      clearInterval(timerInterval)
      timerInterval = null
      timerCountdown.value = 0
      timerMsg.value = '定时已到，正在自动查询事件...'
      queryEvent().then(() => {
        timerMsg.value = '✅ 定时查询已完成'
        setTimeout(() => { timerMsg.value = '计时结束后内容会在这里显示' }, 5000)
      })
    }
  }, 1000)
}

function cancelTimer() {
  if (timerInterval) { clearInterval(timerInterval); timerInterval = null }
  timerCountdown.value = 0
  timerMsg.value = ''
}

onMounted(() => {
  timerMsg.value = '计时结束后内容会在这里显示'
  loadHistory()
})

onUnmounted(() => {
  cancelTimer()
  stopPolling()
})
</script>

<style scoped>
.dev-mode {
  max-width: 1100px;
  margin: 0 auto;
}
.dev-badge {
  display: inline-block;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #fff;
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 16px;
  letter-spacing: 0.5px;
}
.top-bar { margin-bottom: 16px; }
.top-actions { display: flex; justify-content: flex-end; margin-top: 10px; }
.polling-section {
  display: flex; align-items: center; gap: 12px; margin-bottom: 10px;
  padding: 12px 18px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 12px;
}
.polling-status { color: #92400e; font-weight: 600; font-size: 13px; }
.manual-section { margin-bottom: 16px; }
.empty-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 50px 20px; background: var(--bg-card); border: 2px dashed var(--border-color);
  border-radius: 16px; margin-bottom: 20px;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-text { font-size: 14px; color: var(--text-secondary); font-weight: 500; text-align: center; line-height: 1.6; }
.qr-info-section {
  display: grid; grid-template-columns: auto 1fr; gap: 20px; margin-bottom: 24px;
  background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 16px;
  padding: 24px; box-shadow: var(--shadow-sm);
}
.qr-card { text-align: center; }
.qr-card-header { display: flex; align-items: center; justify-content: center; gap: 8px; margin-bottom: 16px; }
.qr-card-icon { font-size: 20px; }
.qr-card-title { font-size: 15px; font-weight: 700; color: var(--text-primary); }
.side-info { display: flex; flex-direction: column; gap: 12px; justify-content: center; }
.info-card { border: 1px solid var(--border-color); border-radius: 12px; padding: 16px 20px; background: var(--bg-primary); }
.info-card-label { font-size: 11px; font-weight: 700; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }
.info-card-value { font-size: 14px; font-weight: 600; color: var(--text-primary); word-break: break-all; }
.action-row { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.timer-panel {
  display: flex; align-items: center; gap: 0; background: #fdf2f8;
  border: 1px solid #fbcfe8; border-radius: 14px; padding: 16px 20px; margin-bottom: 16px; min-height: 80px;
}
.timer-left { flex-shrink: 0; min-width: 200px; }
.timer-divider { width: 1px; align-self: stretch; background: #f9a8d4; margin: 0 18px; flex-shrink: 0; }
.timer-right { flex: 1; min-width: 0; }
.event-placeholder { display: flex; align-items: center; gap: 8px; color: #9ca3af; font-size: 13px; font-weight: 500; }
.event-placeholder-icon { font-size: 18px; }
.timer-panel .event-result { padding: 10px 14px; margin: 0; }
.timer-panel .event-label { margin-bottom: 2px; }
.timer-panel .event-value { font-size: 14px; }
.timer-panel .event-value.highlight { font-size: 16px; }
.timer-panel .event-compare { margin-top: 6px; padding: 6px 10px; }
.timer-panel .attribution-tags { margin-top: 6px; }
.timer-panel .frozen-result { margin-top: 6px; padding: 8px 12px; }

.section-title {
  font-size: 16px; font-weight: 700; color: var(--text-primary); margin-bottom: 14px;
}
.attr-section { margin-bottom: 24px; }
.attr-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.attr-panel {
  border: 1px solid var(--border-color); border-radius: 14px; padding: 16px;
  background: var(--bg-card); box-shadow: var(--shadow-sm); display: flex; flex-direction: column;
}
.attr-panel.appflyer { border-top: 3px solid #22c55e; }
.attr-panel.adjust { border-top: 3px solid #f59e0b; }
.attr-panel.singular { border-top: 3px solid #3b82f6; }
.attr-panel.tenjin { border-top: 3px solid #a855f7; }
.attr-panel-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.attr-panel-icon { font-size: 16px; }
.attr-panel-title { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.attr-count { font-size: 12px; color: var(--text-secondary); font-weight: 600; }
.attr-input-row { display: flex; gap: 8px; margin-bottom: 10px; }
.attr-input-row input {
  flex: 1; padding: 8px 12px; font-size: 13px; border: 2px solid var(--border-color);
  border-radius: 8px; outline: none; transition: var(--transition); background: var(--bg-card);
}
.attr-input-row input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-glow); }
.btn-attr-query {
  background: linear-gradient(135deg, var(--accent), #a855f7); color: #fff; border: none;
  padding: 8px 16px; font-size: 13px; border-radius: 8px; cursor: pointer; font-weight: 600;
  transition: var(--transition); white-space: nowrap;
}
.btn-attr-query:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.3); }
.btn-attr-query:disabled { opacity: 0.5; cursor: not-allowed; }
.attr-results { max-height: 260px; overflow-y: auto; flex: 1; }
.attr-record {
  border: 1px solid var(--border-color); border-radius: 10px; padding: 10px 12px; margin-bottom: 8px;
  background: var(--bg-primary);
}
.attr-record:last-child { margin-bottom: 0; }
.attr-field { display: flex; gap: 8px; padding: 3px 0; border-bottom: 1px solid #f0f0f5; font-size: 12px; }
.attr-field:last-child { border-bottom: none; }
.attr-field-key { font-weight: 700; color: var(--text-secondary); min-width: 110px; flex-shrink: 0; }
.attr-field-val { color: var(--text-primary); word-break: break-all; line-height: 1.5; }
.attr-empty { text-align: center; color: var(--text-secondary); font-size: 13px; padding: 16px; }

.history-section { margin-top: 32px; }
.history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 8px; }
.history-actions { display: flex; align-items: center; gap: 10px; }
.redis-status { font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 8px; }
.redis-status.ok { background: #f0fdf4; color: #166534; }
.redis-status.warn { background: #fef3c7; color: #92400e; }
.btn-history-action {
  background: var(--bg-primary); color: var(--text-secondary); border: 1px solid var(--border-color);
  padding: 6px 14px; font-size: 12px; border-radius: 8px; cursor: pointer; font-weight: 600;
  transition: var(--transition);
}
.btn-history-action:hover { background: var(--accent-glow); color: var(--accent); border-color: var(--accent); }
.btn-history-action.btn-danger:hover { background: #fef2f2; color: var(--danger); border-color: var(--danger); }
.btn-history-action:disabled { opacity: 0.4; cursor: not-allowed; }
.history-empty { text-align: center; padding: 40px 20px; color: var(--text-secondary); font-size: 14px; }
.history-table-wrapper { overflow-x: auto; border-radius: 12px; border: 1px solid var(--border-color); }
.history-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; min-width: 800px; }
.history-table th {
  background: linear-gradient(135deg, #f59e0b, #f97316); color: #fff; padding: 10px 12px;
  white-space: nowrap; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.3px;
}
.history-table td {
  padding: 9px 12px; border-bottom: 1px solid var(--border-color); color: var(--text-primary);
  max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.history-table tr:hover td { background: #fffbeb; }
.history-table tr:last-child td { border-bottom: none; }
.cell-mono { font-family: 'Courier New', monospace; font-size: 11px; }
.cell-remark { max-width: 120px; }
.attr-tag-inline { font-size: 11px; font-weight: 600; color: var(--accent); }
.btn-del-row {
  background: none; border: none; color: var(--danger); font-size: 16px; cursor: pointer;
  padding: 2px 6px; border-radius: 4px; transition: var(--transition); font-weight: 700;
}
.btn-del-row:hover { background: #fef2f2; }

@media (max-width: 700px) {
  .qr-info-section { grid-template-columns: 1fr; }
  .qr-card { justify-self: center; }
  .timer-panel { flex-direction: column; align-items: stretch; }
  .timer-divider { width: 100%; height: 1px; margin: 12px 0; }
  .timer-left { min-width: auto; }
  .attr-grid { grid-template-columns: 1fr; }
}
</style>