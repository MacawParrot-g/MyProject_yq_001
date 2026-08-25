<!--<template>-->
<!--  <div class="dev-mode">-->
<!--    <div class="dev-badge">🛠️ 开发者模式</div>-->

<!--    <div class="top-bar">-->
<!--      <div v-if="duplicateTip" class="duplicate-tip">⚠️ {{ duplicateTip }}</div>-->
<!--      <div v-if="polling" class="polling-section">-->
<!--        <span class="polling-status">🔄 正在每10秒自动获取下载任务...（第{{ pollCount }}次）</span>-->
<!--        <button class="btn-timer-cancel" @click="stopPolling">停止轮询</button>-->
<!--      </div>-->
<!--      <div class="top-actions">-->
<!--        <button class="btn-refresh" @click="confirmRefresh" :disabled="loading">-->
<!--          {{ loading ? '加载中...' : '🔄 刷新数据' }}-->
<!--        </button>-->
<!--      </div>-->
<!--    </div>-->

<!--    <div class="manual-section">-->
<!--      <div class="input-group">-->
<!--        <input type="text" v-model="appId" placeholder="输入 App ID 获取下载链接" @keyup.enter="fetchByAppId" />-->
<!--        <button class="btn-refresh" @click="fetchByAppId" :disabled="loading || !appId.trim()">-->
<!--          {{ loading ? '获取中...' : '生成' }}-->
<!--        </button>-->
<!--      </div>-->
<!--    </div>-->

<!--    <div v-if="loading && !downloadUrl" class="loading">正在获取数据...</div>-->

<!--    <div class="empty-placeholder" v-if="!downloadUrl && !loading">-->
<!--      <div class="empty-icon">🔧</div>-->
<!--      <div class="empty-text">点击刷新数据获取任务，或输入 App ID 生成下载链接</div>-->
<!--    </div>-->

<!--    <div class="qr-info-section" v-if="downloadUrl">-->
<!--      <div class="qr-card">-->
<!--        <div class="qr-card-header">-->
<!--          <span class="qr-card-icon">📱</span>-->
<!--          <span class="qr-card-title">扫描二维码下载APP进行测试</span>-->
<!--        </div>-->
<!--        <div class="qrcode-container">-->
<!--          <canvas ref="qrCanvas"></canvas>-->
<!--        </div>-->
<!--      </div>-->
<!--      <div class="side-info">-->
<!--        <div class="info-card">-->
<!--          <div class="info-card-label">Bundle ID</div>-->
<!--          <div class="info-card-value">{{ bundleId }}</div>-->
<!--        </div>-->
<!--        <div class="info-card">-->
<!--          <div class="info-card-label">原始CurrentTargetNum数</div>-->
<!--          <div class="info-card-value">{{ originalCurrentTargetNum }}</div>-->
<!--        </div>-->
<!--      </div>-->
<!--    </div>-->

<!--    <div class="action-row" v-if="bundleId">-->
<!--      <button class="btn-event" @click="queryEvent" :disabled="eventLoading">-->
<!--        {{ eventLoading ? '查询中...' : '查询事件' }}-->
<!--      </button>-->
<!--      <button class="btn-event" @click="queryEventWithFullMsg" :disabled="eventLoading">-->
<!--        查看JSON-->
<!--      </button>-->
<!--    </div>-->

<!--    <div class="timer-panel" v-if="bundleId">-->
<!--      <div class="timer-left">-->
<!--        <div class="timer-row">-->
<!--          <label class="timer-label">定时查询</label>-->
<!--          <input class="timer-input" type="number" v-model.number="timerSeconds" min="1" max="3600" placeholder="秒数" />-->
<!--          <button class="btn-timer" @click="startTimer" :disabled="timerCountdown > 0 || !timerSeconds || timerSeconds < 1">-->
<!--            {{ timerCountdown > 0 ? timerCountdown + 's' : '开始定时' }}-->
<!--          </button>-->
<!--          <button class="btn-timer-cancel" v-if="timerCountdown > 0" @click="cancelTimer">取消</button>-->
<!--        </div>-->
<!--        <div class="timer-status" v-if="timerMsg">{{ timerMsg }}</div>-->
<!--      </div>-->
<!--      <div class="timer-divider"></div>-->
<!--      <div class="timer-right">-->
<!--        <template v-if="eventResult">-->
<!--          <div class="event-result no-event" v-if="eventResult === 'no_event'">-->
<!--            <div class="event-label">查询结果</div>-->
<!--            <div class="event-value">✅ 无事件</div>-->
<!--            <button class="btn-frozen" @click="doFrozen" :disabled="frozenLoading">-->
<!--              {{ frozenLoading ? '冻结中...' : '冻结应用' }}-->
<!--            </button>-->
<!--            <div class="frozen-result" v-if="frozenMsg">-->
<!--              <div class="frozen-label">冻结结果</div>-->
<!--              <div class="frozen-value">{{ frozenMsg }}</div>-->
<!--            </div>-->
<!--          </div>-->
<!--          <div class="event-result" v-if="eventResult === 'has_event'">-->
<!--            <div class="event-label">最新 currentTargetNum</div>-->
<!--            <div class="event-value highlight">{{ newCurrentTargetNum }}</div>-->
<!--            <div class="event-compare">-->
<!--              原始值：<span class="diff">{{ originalCurrentTargetNum }}</span> → 最新值：<span class="diff">{{ newCurrentTargetNum }}</span>-->
<!--            </div>-->
<!--            <div class="attribution-tags" v-if="attributions.length > 0">-->
<!--              <span class="attr-tag" v-for="attr in attributions" :key="attr" :class="attr">{{ attr }}</span>-->
<!--            </div>-->
<!--            <div class="no-attribution" v-if="attributions.length === 0">⚠️ 无归因</div>-->
<!--          </div>-->
<!--        </template>-->
<!--        <div class="event-placeholder" v-else>-->
<!--          <span class="event-placeholder-icon">🔍</span>-->
<!--          <span>查询的事件会在这里显示</span>-->
<!--        </div>-->
<!--      </div>-->
<!--    </div>-->

<!--    &lt;!&ndash; Four Attribution Panels &ndash;&gt;-->
<!--    <div class="attr-section">-->
<!--      <h3 class="section-title">🔍 归因查询</h3>-->
<!--      <div class="attr-grid">-->
<!--        <div v-for="type in ATTR_TYPES" :key="type" class="attr-panel" :class="type">-->
<!--          <div class="attr-panel-header">-->
<!--            <span class="attr-panel-icon">{{ attrIcon(type) }}</span>-->
<!--            <span class="attr-panel-title">{{ type.toUpperCase() }}</span>-->
<!--            <span class="attr-count" v-if="attrStates[type].count !== null">({{ attrStates[type].count }}条)</span>-->
<!--          </div>-->
<!--          <div class="attr-input-row">-->
<!--            <input v-model="attrStates[type].bundleId" :placeholder="'Bundle ID'" @keyup.enter="querySingleAttr(type)" />-->
<!--            <button class="btn-attr-query" @click="querySingleAttr(type)" :disabled="attrStates[type].loading || !attrStates[type].bundleId">-->
<!--              {{ attrStates[type].loading ? '...' : '查询' }}-->
<!--            </button>-->
<!--          </div>-->
<!--          <div v-if="attrStates[type].data.length > 0" class="attr-results">-->
<!--            <div v-for="(record, rIdx) in attrStates[type].data" :key="rIdx" class="attr-record">-->
<!--              <div v-for="(val, key) in record" :key="key" class="attr-field">-->
<!--                <span class="attr-field-key">{{ key }}</span>-->
<!--                <span class="attr-field-val">{{ formatAttrValue(val) }}</span>-->
<!--              </div>-->
<!--            </div>-->
<!--          </div>-->
<!--          <div v-if="attrStates[type].queried && attrStates[type].data.length === 0 && !attrStates[type].loading" class="attr-empty">-->
<!--            无数据-->
<!--          </div>-->
<!--        </div>-->
<!--      </div>-->
<!--    </div>-->

<!--    &lt;!&ndash; Insert Form &ndash;&gt;-->
<!--    <div class="form-section" v-if="downloadUrl && eventResult">-->
<!--      <h4>填写入库信息（直接写入数据库）</h4>-->
<!--      <div class="form-group">-->
<!--        <label>异常类型</label>-->
<!--        <select v-model="form.exception_type">-->
<!--          <option value="" disabled>请选择异常类型</option>-->
<!--          <option v-for="opt in exceptionOptions" :key="opt" :value="opt">{{ opt }}</option>-->
<!--        </select>-->
<!--      </div>-->
<!--      <div class="form-group">-->
<!--        <label>备注</label>-->
<!--        <textarea v-model="form.remark" placeholder="手动输入(如果用了模板此处禁填)"></textarea>-->
<!--        <br/>-->
<!--        <select v-model="form.remark">-->
<!--          <option value="" disabled>模板(可选)</option>-->
<!--          <option v-for="opt in templateOptions" :key="opt" :value="opt">{{ opt }}</option>-->
<!--        </select>-->
<!--      </div>-->
<!--      <div style="display:flex; gap:12px;">-->
<!--        <div class="form-group" style="flex:1">-->
<!--          <label>记录人</label>-->
<!--          <input v-model="form.recorder" />-->
<!--        </div>-->
<!--        <div class="form-group" style="flex:1">-->
<!--          <label>记录日期</label>-->
<!--          <input v-model="form.record_data" />-->
<!--        </div>-->
<!--      </div>-->
<!--      <button class="btn-save" @click="saveToMySQL" :disabled="saving">-->
<!--        {{ saving ? '入库中...' : '直接入库' }}-->
<!--      </button>-->
<!--      <div class="save-success" v-if="saveMsg">{{ saveMsg }}</div>-->
<!--    </div>-->

<!--    &lt;!&ndash; History Section &ndash;&gt;-->
<!--    <div class="history-section">-->
<!--      <div class="history-header">-->
<!--        <h3 class="section-title">📋 今日测试历史</h3>-->
<!--        <div class="history-actions">-->
<!--          <span v-if="redisStatusMsg" class="redis-status" :class="redisOk ? 'ok' : 'warn'">{{ redisStatusMsg }}</span>-->
<!--          <button class="btn-history-action" @click="loadHistory">刷新</button>-->
<!--          <button class="btn-history-action btn-danger" @click="clearHistory" :disabled="historyList.length === 0">清空</button>-->
<!--        </div>-->
<!--      </div>-->
<!--      <div v-if="historyLoading" class="loading">加载历史记录...</div>-->
<!--      <div v-if="!historyLoading && historyList.length === 0" class="history-empty">暂无测试记录</div>-->
<!--      <div v-if="historyList.length > 0" class="history-table-wrapper">-->
<!--        <table class="history-table">-->
<!--          <thead>-->
<!--          <tr>-->
<!--            <th>时间</th>-->
<!--            <th>Bundle ID</th>-->
<!--            <th>事件数</th>-->
<!--            <th>归因</th>-->
<!--            <th>异常类型</th>-->
<!--            <th>备注</th>-->
<!--            <th>记录人</th>-->
<!--            <th>日期</th>-->
<!--            <th>操作</th>-->
<!--          </tr>-->
<!--          </thead>-->
<!--          <tbody>-->
<!--          <tr v-for="(item, idx) in historyList" :key="idx">-->
<!--            <td>{{ item.timestamp || '-' }}</td>-->
<!--            <td class="cell-mono">{{ item.bundleId || '-' }}</td>-->
<!--            <td>{{ item.eventNumber ?? '-' }}</td>-->
<!--            <td>-->
<!--              <span v-if="item.ascribe" class="attr-tag-inline">{{ item.ascribe }}</span>-->
<!--              <span v-else>-</span>-->
<!--            </td>-->
<!--            <td>{{ item.exceptionType || '-' }}</td>-->
<!--            <td class="cell-remark" :title="item.remark">{{ item.remark || '-' }}</td>-->
<!--            <td>{{ item.recorder || '-' }}</td>-->
<!--            <td>{{ item.recordData || '-' }}</td>-->
<!--            <td>-->
<!--              <button class="btn-del-row" @click="deleteHistoryRecord(item.timestamp)" title="删除">×</button>-->
<!--            </td>-->
<!--          </tr>-->
<!--          </tbody>-->
<!--        </table>-->
<!--      </div>-->
<!--    </div>-->

<!--    &lt;!&ndash; Retest Modal &ndash;&gt;-->
<!--    <div class="modal-overlay" v-if="showRetestModal" @click.self="showRetestModal = false">-->
<!--      <div class="modal-box">-->
<!--        <div class="modal-title">库存已耗尽</div>-->
<!--        <div class="modal-desc">当前没有可用的下载任务，请选择操作：</div>-->
<!--        <div class="modal-actions">-->
<!--          <button class="btn-modal btn-poll" @click="showRetestModal = false; startPolling()">继续轮询</button>-->
<!--          <button class="btn-modal btn-retest" @click="retestFlow" :disabled="retestLoading">-->
<!--            {{ retestLoading ? '获取中...' : '复测' }}-->
<!--          </button>-->
<!--          <button class="btn-modal btn-cancel" @click="showRetestModal = false">取消</button>-->
<!--        </div>-->
<!--      </div>-->
<!--    </div>-->
<!--  </div>-->
<!--</template>-->

<!--<script setup>-->
<!--import { ref, reactive, onMounted, watch, nextTick, onUnmounted } from 'vue'-->
<!--import QRCode from 'qrcode'-->
<!--import {-->
<!--  fetchTask, fetchObtain, fetchEvent, fetchAllAttributions, fetchAttribution,-->
<!--  fetchFrozen, insertRecord, fetchRandomForRetest,-->
<!--  fetchDevHistory, saveDevHistory, clearDevHistory, fetchDevRedisStatus, deleteDevHistoryRecord-->
<!--} from '../api/index.js'-->

<!--const emit = defineEmits(['error', 'record-saved'])-->

<!--const exceptionOptions = ['正常','iOS16闪退','iOS13/14/16均闪退','需要iOS18以上','地区不支持','硬件版本过低','超过10分钟0上报','越狱检测','其他','验证已解决','测试']-->
<!--const templateOptions = ['需要iOS17以上','需要登陆后使用，无法注册','卡死在加载页进不去','非英语汉语软件，看不懂','网络检测，无法进入','需要订阅后使用','禁止入库']-->
<!--const ATTR_TYPES = ['appflyer', 'adjust', 'singular', 'tenjin']-->
<!--const MAX_RETRIES = 5-->

<!--const downloadUrl = ref('')-->
<!--const bundleId = ref('')-->
<!--const originalCurrentTargetNum = ref(null)-->
<!--const eventResult = ref('')-->
<!--const newCurrentTargetNum = ref(null)-->
<!--const attributions = ref([])-->
<!--const eventId = ref(null)-->
<!--const frozenMsg = ref('')-->
<!--const duplicateTip = ref('')-->
<!--const loading = ref(false)-->
<!--const eventLoading = ref(false)-->
<!--const frozenLoading = ref(false)-->
<!--const saving = ref(false)-->
<!--const saveMsg = ref('')-->
<!--const appId = ref('')-->
<!--const showRetestModal = ref(false)-->
<!--const retestLoading = ref(false)-->
<!--const qrCanvas = ref(null)-->
<!--const timerSeconds = ref(60)-->
<!--const timerCountdown = ref(0)-->
<!--const timerMsg = ref('')-->
<!--const polling = ref(false)-->
<!--const pollCount = ref(0)-->
<!--let pollTimer = null-->
<!--let timerInterval = null-->
<!--let isFrozen = ref('')-->
<!--let isSubmit = true-->
<!--let retryCount = 0-->

<!--const attrStates = reactive({-->
<!--  appflyer: { bundleId: '', data: [], loading: false, count: null, queried: false },-->
<!--  adjust: { bundleId: '', data: [], loading: false, count: null, queried: false },-->
<!--  singular: { bundleId: '', data: [], loading: false, count: null, queried: false },-->
<!--  tenjin: { bundleId: '', data: [], loading: false, count: null, queried: false }-->
<!--})-->

<!--const form = reactive({-->
<!--  exception_type: '',-->
<!--  remark: '',-->
<!--  recorder: localStorage.getItem('userName') || '',-->
<!--  record_data: getTodayStr()-->
<!--})-->

<!--const historyList = ref([])-->
<!--const historyLoading = ref(false)-->
<!--const redisOk = ref(true)-->
<!--const redisStatusMsg = ref('')-->

<!--function getTodayStr() {-->
<!--  const d = new Date()-->
<!--  return d.getFullYear() + '/' + (d.getMonth() + 1) + '/' + d.getDate()-->
<!--}-->

<!--function attrIcon(type) {-->
<!--  return { appflyer: '🟢', adjust: '🟡', singular: '🔵', tenjin: '🟣' }[type] || '⚪'-->
<!--}-->

<!--function formatAttrValue(val) {-->
<!--  if (val === null || val === undefined) return '-'-->
<!--  if (typeof val === 'object') return JSON.stringify(val)-->
<!--  return String(val)-->
<!--}-->

<!--async function renderQR(url) {-->
<!--  await nextTick()-->
<!--  if (url && qrCanvas.value) {-->
<!--    try {-->
<!--      await QRCode.toCanvas(qrCanvas.value, url, { width: 256, margin: 2, color: { dark: '#000000', light: '#ffffff' } })-->
<!--    } catch (e) { console.error('QR码生成失败:', e) }-->
<!--  }-->
<!--}-->

<!--watch(downloadUrl, (val) => { renderQR(val) })-->

<!--function fillAttrBundleIds(bid) {-->
<!--  for (const type of ATTR_TYPES) {-->
<!--    attrStates[type].bundleId = bid-->
<!--  }-->
<!--}-->

<!--function confirmRefresh() {-->
<!--  if (!isSubmit && downloadUrl.value) {-->
<!--    if (confirm('请确认是否已入库该条测试数据，刷新会导致数据丢失！')) {-->
<!--      fetchData()-->
<!--    }-->
<!--  } else {-->
<!--    fetchData()-->
<!--  }-->
<!--}-->

<!--async function fetchData() {-->
<!--  loading.value = true-->
<!--  emit('error', '')-->
<!--  resetTaskState()-->
<!--  try {-->
<!--    const json = await fetchTask()-->
<!--    if (json.data) {-->
<!--      if (json.duplicate) {-->
<!--        retryCount++-->
<!--        if (retryCount > MAX_RETRIES) {-->
<!--          emit('error', '已连续 ' + MAX_RETRIES + ' 次检测到重复URL，请稍后再试')-->
<!--          duplicateTip.value = ''-->
<!--          return-->
<!--        }-->
<!--        duplicateTip.value = '检测到重复URL（第' + retryCount + '次），正在为您自动刷新...'-->
<!--        setTimeout(() => fetchData(), 1500)-->
<!--        return-->
<!--      }-->
<!--      retryCount = 0-->
<!--      duplicateTip.value = ''-->
<!--      downloadUrl.value = json.data.downloadUrl || ''-->
<!--      bundleId.value = json.data.bundleId || ''-->
<!--      originalCurrentTargetNum.value = json.data.currentTargetNum ?? null-->
<!--      isSubmit = false-->
<!--      fillAttrBundleIds(bundleId.value)-->
<!--      if (originalCurrentTargetNum.value <= 1 || originalCurrentTargetNum.value === 0) {-->
<!--        alert('初始originalCurrentTargetNum数小于等于1或等于0，后续测试期间可能会没有归因，请注意')-->
<!--      }-->
<!--    } else {-->
<!--      showRetestModal.value = true-->
<!--    }-->
<!--  } catch (e) {-->
<!--    if (e.name === 'AbortError') emit('error', '请求超时，远程服务器响应太慢')-->
<!--    else emit('error', '请求失败：' + e.message)-->
<!--  } finally {-->
<!--    loading.value = false-->
<!--  }-->
<!--}-->

<!--async function fetchByAppId() {-->
<!--  const id = appId.value.trim()-->
<!--  if (!id) return-->
<!--  loading.value = true-->
<!--  emit('error', '')-->
<!--  resetTaskState()-->
<!--  try {-->
<!--    const json = await fetchObtain(id)-->
<!--    if (json.success && json.data) {-->
<!--      if (json.duplicate) {-->
<!--        retryCount++-->
<!--        if (retryCount > MAX_RETRIES) {-->
<!--          emit('error', '已连续 ' + MAX_RETRIES + ' 次检测到重复URL，请稍后再试')-->
<!--          duplicateTip.value = ''-->
<!--          return-->
<!--        }-->
<!--        duplicateTip.value = '检测到重复URL（第' + retryCount + '次），正在为您自动刷新...'-->
<!--        setTimeout(() => fetchByAppId(), 1500)-->
<!--        return-->
<!--      }-->
<!--      retryCount = 0-->
<!--      duplicateTip.value = ''-->
<!--      downloadUrl.value = json.data.downloadUrl || ''-->
<!--      bundleId.value = json.data.bundleId || ''-->
<!--      originalCurrentTargetNum.value = json.data.currentTargetNum ?? null-->
<!--      isSubmit = false-->
<!--      fillAttrBundleIds(bundleId.value)-->
<!--      if (originalCurrentTargetNum.value <= 1 || originalCurrentTargetNum.value === 0) {-->
<!--        alert('初始originalCurrentTargetNum数小于等于1或等于0，后续测试期间可能会没有归因，请注意')-->
<!--      }-->
<!--    } else {-->
<!--      emit('error', '接口返回异常：' + (json.resultMsg || '未知错误'))-->
<!--    }-->
<!--  } catch (e) {-->
<!--    if (e.name === 'AbortError') emit('error', '请求超时')-->
<!--    else emit('error', '请求失败：' + e.message)-->
<!--  } finally {-->
<!--    loading.value = false-->
<!--  }-->
<!--}-->

<!--function startPolling() {-->
<!--  polling.value = true-->
<!--  pollCount.value = 0-->
<!--  pollTimer = setInterval(async () => {-->
<!--    pollCount.value++-->
<!--    try {-->
<!--      const json = await fetchTask()-->
<!--      if (json.data && !json.duplicate) {-->
<!--        clearInterval(pollTimer)-->
<!--        polling.value = false-->
<!--        pollCount.value = 0-->
<!--        downloadUrl.value = json.data.downloadUrl || ''-->
<!--        bundleId.value = json.data.bundleId || ''-->
<!--        originalCurrentTargetNum.value = json.data.currentTargetNum ?? null-->
<!--        isSubmit = false-->
<!--        fillAttrBundleIds(bundleId.value)-->
<!--        if (originalCurrentTargetNum.value <= 1 || originalCurrentTargetNum.value === 0) {-->
<!--          alert('初始originalCurrentTargetNum数小于等于1或等于0，后续测试期间可能会没有归因，请注意')-->
<!--        }-->
<!--      }-->
<!--    } catch (e) {-->
<!--      console.error('轮询请求失败:', e)-->
<!--    }-->
<!--  }, 10000)-->
<!--}-->

<!--function stopPolling() {-->
<!--  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }-->
<!--  polling.value = false-->
<!--  pollCount.value = 0-->
<!--}-->

<!--async function retestFlow() {-->
<!--  retestLoading.value = true-->
<!--  resetTaskState()-->
<!--  try {-->
<!--    const dates = getPast3DaysDates()-->
<!--    const json = await fetchRandomForRetest(dates)-->
<!--    if (json.success && json.data) {-->
<!--      showRetestModal.value = false-->
<!--      downloadUrl.value = json.data.downloadUrl || ''-->
<!--      bundleId.value = json.data.bundleId || ''-->
<!--      fillAttrBundleIds(bundleId.value)-->
<!--      try {-->
<!--        const jsons = await fetchEvent(bundleId.value)-->
<!--        if (jsons.success) {-->
<!--          originalCurrentTargetNum.value = jsons.data.currentTargetNum ?? null-->
<!--        }-->
<!--      } catch (e) {-->
<!--        alert('服务器无响应，请联系技术人员')-->
<!--      }-->
<!--      isSubmit = false-->
<!--    } else {-->
<!--      emit('error', json.message || '获取复测数据失败')-->
<!--    }-->
<!--  } catch (e) {-->
<!--    emit('error', '复测请求失败：' + e.message)-->
<!--  } finally {-->
<!--    retestLoading.value = false-->
<!--  }-->
<!--}-->

<!--function getPast3DaysDates() {-->
<!--  const dates = []-->
<!--  const d = new Date()-->
<!--  for (let i = 0; i < 3; i++) {-->
<!--    const t = new Date(d)-->
<!--    t.setDate(d.getDate() - i)-->
<!--    dates.push(t.getFullYear() + '/' + (t.getMonth() + 1) + '/' + t.getDate())-->
<!--  }-->
<!--  return dates-->
<!--}-->

<!--async function queryEvent() {-->
<!--  if (!bundleId.value) return-->
<!--  eventLoading.value = true-->
<!--  emit('error', '')-->
<!--  eventResult.value = ''; newCurrentTargetNum.value = null; attributions.value = []; eventId.value = null; frozenMsg.value = ''-->
<!--  try {-->
<!--    const [eventJson, attrResults] = await Promise.all([-->
<!--      fetchEvent(bundleId.value),-->
<!--      fetchAllAttributions(bundleId.value)-->
<!--    ])-->
<!--    if (eventJson.success && eventJson.data) {-->
<!--      const newCurrent = eventJson.data.currentTargetNum-->
<!--      eventId.value = eventJson.data.id ?? null-->
<!--      if (originalCurrentTargetNum.value !== null) {-->
<!--        if (newCurrent !== originalCurrentTargetNum.value) {-->
<!--          eventResult.value = 'has_event'-->
<!--          newCurrentTargetNum.value = newCurrent-->
<!--        } else {-->
<!--          eventResult.value = 'no_event'-->
<!--        }-->
<!--      } else {-->
<!--        newCurrentTargetNum.value = newCurrent-->
<!--        eventResult.value = newCurrent != null && newCurrent > 0 ? 'has_event' : 'no_event'-->
<!--      }-->
<!--    } else {-->
<!--      emit('error', '事件接口返回异常：' + (eventJson.resultMsg || '未知错误'))-->
<!--    }-->
<!--    const found = []-->
<!--    for (const { type, json } of attrResults) {-->
<!--      if (json.success && json.data) {-->
<!--        if (Array.isArray(json.data) && json.data.length > 0) found.push(type)-->
<!--        else if (!Array.isArray(json.data) && Object.keys(json.data).length > 0) found.push(type)-->
<!--      }-->
<!--    }-->
<!--    attributions.value = found-->
<!--  } catch (e) {-->
<!--    emit('error', '事件查询失败：' + e.message)-->
<!--  } finally {-->
<!--    eventLoading.value = false-->
<!--  }-->
<!--}-->

<!--async function queryEventWithFullMsg() {-->
<!--  try {-->
<!--    const data = await fetchEvent(bundleId.value)-->
<!--    if (data.success) alert(JSON.stringify(data, null, 2))-->
<!--  } catch (e) {-->
<!--    alert('服务器无响应，请联系技术人员')-->
<!--  }-->
<!--}-->

<!--async function querySingleAttr(type) {-->
<!--  const bid = attrStates[type].bundleId-->
<!--  if (!bid) return-->
<!--  attrStates[type].loading = true-->
<!--  attrStates[type].data = []-->
<!--  attrStates[type].count = null-->
<!--  attrStates[type].queried = false-->
<!--  try {-->
<!--    const json = await fetchAttribution(bid, type)-->
<!--    if (json.success) {-->
<!--      let records = []-->
<!--      if (Array.isArray(json.data)) {-->
<!--        records = json.data-->
<!--      } else if (json.data && typeof json.data === 'object') {-->
<!--        records = [json.data]-->
<!--      }-->
<!--      attrStates[type].data = records-->
<!--      attrStates[type].count = records.length-->
<!--    } else {-->
<!--      attrStates[type].data = []-->
<!--      attrStates[type].count = 0-->
<!--    }-->
<!--  } catch (e) {-->
<!--    attrStates[type].data = []-->
<!--    attrStates[type].count = 0-->
<!--  } finally {-->
<!--    attrStates[type].loading = false-->
<!--    attrStates[type].queried = true-->
<!--  }-->
<!--}-->

<!--async function doFrozen() {-->
<!--  if (!eventId.value) return-->
<!--  frozenLoading.value = true-->
<!--  emit('error', '')-->
<!--  try {-->
<!--    const json = await fetchFrozen(eventId.value)-->
<!--    if (json.success) {-->
<!--      frozenMsg.value = json.resultMsg || '操作完成'-->
<!--      isFrozen.value = ',已冻结'-->
<!--    } else {-->
<!--      emit('error', '冻结接口返回异常：' + (json.resultMsg || '未知错误'))-->
<!--    }-->
<!--  } catch (e) {-->
<!--    emit('error', '冻结请求失败：' + e.message)-->
<!--  } finally {-->
<!--    frozenLoading.value = false-->
<!--  }-->
<!--}-->

<!--async function saveToMySQL() {-->
<!--  emit('record-saved')-->
<!--  if (!form.exception_type.trim()) { emit('error', '请选择异常类型'); return }-->
<!--  saving.value = true-->
<!--  saveMsg.value = ''-->
<!--  if (newCurrentTargetNum.value === 0 || newCurrentTargetNum.value === null) {-->
<!--    attributions.value = []-->
<!--    alert('无新增事件token，即使原本的token有归因，也不会被设置在字段内')-->
<!--  }-->
<!--  let finalRemark = form.remark.trim()-->
<!--  if (isFrozen.value) finalRemark += isFrozen.value-->
<!--  if (newCurrentTargetNum.value > 0 && attributions.value.length === 0) finalRemark += ',无事件归因'-->
<!--  try {-->
<!--    const json = await insertRecord({-->
<!--      URL: downloadUrl.value,-->
<!--      bundleId: bundleId.value,-->
<!--      ascribe: (attributions.value || []).join(';'),-->
<!--      event_number: newCurrentTargetNum.value,-->
<!--      exception_type: form.exception_type.trim(),-->
<!--      record_data: form.record_data,-->
<!--      recorder: form.recorder,-->
<!--      remark: finalRemark,-->
<!--      isOutput: 0-->
<!--    })-->
<!--    if (json.success) {-->
<!--      saveMsg.value = '✅ ' + (json.resultMsg || '入库成功')-->
<!--      isSubmit = true-->
<!--      await saveToHistory(finalRemark)-->
<!--      await loadHistory()-->
<!--    } else {-->
<!--      emit('error', json.resultMsg || '入库失败')-->
<!--    }-->
<!--  } catch (e) {-->
<!--    emit('error', '入库请求失败：' + e.message)-->
<!--  } finally {-->
<!--    saving.value = false-->
<!--  }-->
<!--}-->

<!--async function saveToHistory(remark) {-->
<!--  try {-->
<!--    await saveDevHistory({-->
<!--      bundleId: bundleId.value,-->
<!--      ascribe: (attributions.value || []).join(';'),-->
<!--      eventNumber: newCurrentTargetNum.value,-->
<!--      exceptionType: form.exception_type.trim(),-->
<!--      recordData: form.record_data,-->
<!--      recorder: form.recorder,-->
<!--      remark: remark || form.remark.trim()-->
<!--    })-->
<!--  } catch (e) {-->
<!--    console.warn('保存历史失败:', e)-->
<!--  }-->
<!--}-->

<!--async function loadHistory() {-->
<!--  historyLoading.value = true-->
<!--  try {-->
<!--    const json = await fetchDevHistory()-->
<!--    if (json.success) {-->
<!--      historyList.value = json.data || []-->
<!--      if (json.message && json.message.includes('Redis不可用')) {-->
<!--        redisOk.value = false-->
<!--        redisStatusMsg.value = '⚠️ Redis不可用，使用本地缓存'-->
<!--      } else {-->
<!--        redisOk.value = true-->
<!--        redisStatusMsg.value = '✅ Redis正常'-->
<!--      }-->
<!--    }-->
<!--  } catch (e) {-->
<!--    console.warn('加载历史失败:', e)-->
<!--  } finally {-->
<!--    historyLoading.value = false-->
<!--  }-->
<!--}-->

<!--async function clearHistory() {-->
<!--  if (!confirm('确定清空所有测试历史记录？')) return-->
<!--  try {-->
<!--    await clearDevHistory()-->
<!--    historyList.value = []-->
<!--  } catch (e) {-->
<!--    console.warn('清空历史失败:', e)-->
<!--  }-->
<!--}-->

<!--async function deleteHistoryRecord(timestamp) {-->
<!--  if (!timestamp) return-->
<!--  try {-->
<!--    await deleteDevHistoryRecord(timestamp)-->
<!--    historyList.value = historyList.value.filter(h => h.timestamp !== timestamp)-->
<!--  } catch (e) {-->
<!--    console.warn('删除记录失败:', e)-->
<!--  }-->
<!--}-->

<!--function resetTaskState() {-->
<!--  downloadUrl.value = ''-->
<!--  bundleId.value = ''-->
<!--  originalCurrentTargetNum.value = null-->
<!--  eventResult.value = ''-->
<!--  newCurrentTargetNum.value = null-->
<!--  attributions.value = []-->
<!--  eventId.value = null-->
<!--  frozenMsg.value = ''-->
<!--  duplicateTip.value = ''-->
<!--  saveMsg.value = ''-->
<!--  isFrozen.value = ''-->
<!--  form.exception_type = ''-->
<!--  form.remark = ''-->
<!--  form.recorder = localStorage.getItem('userName') || ''-->
<!--  form.record_data = getTodayStr()-->
<!--  for (const type of ATTR_TYPES) {-->
<!--    attrStates[type].data = []-->
<!--    attrStates[type].count = null-->
<!--    attrStates[type].queried = false-->
<!--  }-->
<!--}-->

<!--function startTimer() {-->
<!--  if (!timerSeconds.value || timerSeconds.value < 1) return-->
<!--  timerCountdown.value = timerSeconds.value-->
<!--  timerMsg.value = '将在 ' + timerCountdown.value + ' 秒后自动查询事件'-->
<!--  timerInterval = setInterval(() => {-->
<!--    timerCountdown.value&#45;&#45;-->
<!--    if (timerCountdown.value <= 0) {-->
<!--      clearInterval(timerInterval)-->
<!--      timerInterval = null-->
<!--      timerCountdown.value = 0-->
<!--      timerMsg.value = '定时已到，正在自动查询事件...'-->
<!--      queryEvent().then(() => {-->
<!--        timerMsg.value = '✅ 定时查询已完成'-->
<!--        setTimeout(() => { timerMsg.value = '计时结束后内容会在这里显示' }, 5000)-->
<!--      })-->
<!--    }-->
<!--  }, 1000)-->
<!--}-->

<!--function cancelTimer() {-->
<!--  if (timerInterval) { clearInterval(timerInterval); timerInterval = null }-->
<!--  timerCountdown.value = 0-->
<!--  timerMsg.value = ''-->
<!--}-->

<!--onMounted(() => {-->
<!--  timerMsg.value = '计时结束后内容会在这里显示'-->
<!--  loadHistory()-->
<!--})-->

<!--onUnmounted(() => {-->
<!--  cancelTimer()-->
<!--  stopPolling()-->
<!--})-->
<!--</script>-->

<!--<style scoped>-->
<!--.dev-mode {-->
<!--  max-width: 1100px;-->
<!--  margin: 0 auto;-->
<!--}-->
<!--.dev-badge {-->
<!--  display: inline-block;-->
<!--  background: linear-gradient(135deg, #f59e0b, #f97316);-->
<!--  color: #fff;-->
<!--  padding: 4px 16px;-->
<!--  border-radius: 20px;-->
<!--  font-size: 12px;-->
<!--  font-weight: 700;-->
<!--  margin-bottom: 16px;-->
<!--  letter-spacing: 0.5px;-->
<!--}-->
<!--.top-bar { margin-bottom: 16px; }-->
<!--.top-actions { display: flex; justify-content: flex-end; margin-top: 10px; }-->
<!--.polling-section {-->
<!--  display: flex; align-items: center; gap: 12px; margin-bottom: 10px;-->
<!--  padding: 12px 18px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 12px;-->
<!--}-->
<!--.polling-status { color: #92400e; font-weight: 600; font-size: 13px; }-->
<!--.manual-section { margin-bottom: 16px; }-->
<!--.empty-placeholder {-->
<!--  display: flex; flex-direction: column; align-items: center; justify-content: center;-->
<!--  padding: 50px 20px; background: var(&#45;&#45;bg-card); border: 2px dashed var(&#45;&#45;border-color);-->
<!--  border-radius: 16px; margin-bottom: 20px;-->
<!--}-->
<!--.empty-icon { font-size: 48px; margin-bottom: 16px; }-->
<!--.empty-text { font-size: 14px; color: var(&#45;&#45;text-secondary); font-weight: 500; text-align: center; line-height: 1.6; }-->
<!--.qr-info-section {-->
<!--  display: grid; grid-template-columns: auto 1fr; gap: 20px; margin-bottom: 24px;-->
<!--  background: var(&#45;&#45;bg-card); border: 1px solid var(&#45;&#45;border-color); border-radius: 16px;-->
<!--  padding: 24px; box-shadow: var(&#45;&#45;shadow-sm);-->
<!--}-->
<!--.qr-card { text-align: center; }-->
<!--.qr-card-header { display: flex; align-items: center; justify-content: center; gap: 8px; margin-bottom: 16px; }-->
<!--.qr-card-icon { font-size: 20px; }-->
<!--.qr-card-title { font-size: 15px; font-weight: 700; color: var(&#45;&#45;text-primary); }-->
<!--.side-info { display: flex; flex-direction: column; gap: 12px; justify-content: center; }-->
<!--.info-card { border: 1px solid var(&#45;&#45;border-color); border-radius: 12px; padding: 16px 20px; background: var(&#45;&#45;bg-primary); }-->
<!--.info-card-label { font-size: 11px; font-weight: 700; color: var(&#45;&#45;text-secondary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }-->
<!--.info-card-value { font-size: 14px; font-weight: 600; color: var(&#45;&#45;text-primary); word-break: break-all; }-->
<!--.action-row { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }-->
<!--.timer-panel {-->
<!--  display: flex; align-items: center; gap: 0; background: #fdf2f8;-->
<!--  border: 1px solid #fbcfe8; border-radius: 14px; padding: 16px 20px; margin-bottom: 16px; min-height: 80px;-->
<!--}-->
<!--.timer-left { flex-shrink: 0; min-width: 200px; }-->
<!--.timer-divider { width: 1px; align-self: stretch; background: #f9a8d4; margin: 0 18px; flex-shrink: 0; }-->
<!--.timer-right { flex: 1; min-width: 0; }-->
<!--.event-placeholder { display: flex; align-items: center; gap: 8px; color: #9ca3af; font-size: 13px; font-weight: 500; }-->
<!--.event-placeholder-icon { font-size: 18px; }-->
<!--.timer-panel .event-result { padding: 10px 14px; margin: 0; }-->
<!--.timer-panel .event-label { margin-bottom: 2px; }-->
<!--.timer-panel .event-value { font-size: 14px; }-->
<!--.timer-panel .event-value.highlight { font-size: 16px; }-->
<!--.timer-panel .event-compare { margin-top: 6px; padding: 6px 10px; }-->
<!--.timer-panel .attribution-tags { margin-top: 6px; }-->
<!--.timer-panel .frozen-result { margin-top: 6px; padding: 8px 12px; }-->

<!--.section-title {-->
<!--  font-size: 16px; font-weight: 700; color: var(&#45;&#45;text-primary); margin-bottom: 14px;-->
<!--}-->
<!--.attr-section { margin-bottom: 24px; }-->
<!--.attr-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }-->
<!--.attr-panel {-->
<!--  border: 1px solid var(&#45;&#45;border-color); border-radius: 14px; padding: 16px;-->
<!--  background: var(&#45;&#45;bg-card); box-shadow: var(&#45;&#45;shadow-sm); display: flex; flex-direction: column;-->
<!--}-->
<!--.attr-panel.appflyer { border-top: 3px solid #22c55e; }-->
<!--.attr-panel.adjust { border-top: 3px solid #f59e0b; }-->
<!--.attr-panel.singular { border-top: 3px solid #3b82f6; }-->
<!--.attr-panel.tenjin { border-top: 3px solid #a855f7; }-->
<!--.attr-panel-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }-->
<!--.attr-panel-icon { font-size: 16px; }-->
<!--.attr-panel-title { font-size: 14px; font-weight: 700; color: var(&#45;&#45;text-primary); }-->
<!--.attr-count { font-size: 12px; color: var(&#45;&#45;text-secondary); font-weight: 600; }-->
<!--.attr-input-row { display: flex; gap: 8px; margin-bottom: 10px; }-->
<!--.attr-input-row input {-->
<!--  flex: 1; padding: 8px 12px; font-size: 13px; border: 2px solid var(&#45;&#45;border-color);-->
<!--  border-radius: 8px; outline: none; transition: var(&#45;&#45;transition); background: var(&#45;&#45;bg-card);-->
<!--}-->
<!--.attr-input-row input:focus { border-color: var(&#45;&#45;accent); box-shadow: 0 0 0 3px var(&#45;&#45;accent-glow); }-->
<!--.btn-attr-query {-->
<!--  background: linear-gradient(135deg, var(&#45;&#45;accent), #a855f7); color: #fff; border: none;-->
<!--  padding: 8px 16px; font-size: 13px; border-radius: 8px; cursor: pointer; font-weight: 600;-->
<!--  transition: var(&#45;&#45;transition); white-space: nowrap;-->
<!--}-->
<!--.btn-attr-query:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.3); }-->
<!--.btn-attr-query:disabled { opacity: 0.5; cursor: not-allowed; }-->
<!--.attr-results { max-height: 260px; overflow-y: auto; flex: 1; }-->
<!--.attr-record {-->
<!--  border: 1px solid var(&#45;&#45;border-color); border-radius: 10px; padding: 10px 12px; margin-bottom: 8px;-->
<!--  background: var(&#45;&#45;bg-primary);-->
<!--}-->
<!--.attr-record:last-child { margin-bottom: 0; }-->
<!--.attr-field { display: flex; gap: 8px; padding: 3px 0; border-bottom: 1px solid #f0f0f5; font-size: 12px; }-->
<!--.attr-field:last-child { border-bottom: none; }-->
<!--.attr-field-key { font-weight: 700; color: var(&#45;&#45;text-secondary); min-width: 110px; flex-shrink: 0; }-->
<!--.attr-field-val { color: var(&#45;&#45;text-primary); word-break: break-all; line-height: 1.5; }-->
<!--.attr-empty { text-align: center; color: var(&#45;&#45;text-secondary); font-size: 13px; padding: 16px; }-->

<!--.history-section { margin-top: 32px; }-->
<!--.history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 8px; }-->
<!--.history-actions { display: flex; align-items: center; gap: 10px; }-->
<!--.redis-status { font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 8px; }-->
<!--.redis-status.ok { background: #f0fdf4; color: #166534; }-->
<!--.redis-status.warn { background: #fef3c7; color: #92400e; }-->
<!--.btn-history-action {-->
<!--  background: var(&#45;&#45;bg-primary); color: var(&#45;&#45;text-secondary); border: 1px solid var(&#45;&#45;border-color);-->
<!--  padding: 6px 14px; font-size: 12px; border-radius: 8px; cursor: pointer; font-weight: 600;-->
<!--  transition: var(&#45;&#45;transition);-->
<!--}-->
<!--.btn-history-action:hover { background: var(&#45;&#45;accent-glow); color: var(&#45;&#45;accent); border-color: var(&#45;&#45;accent); }-->
<!--.btn-history-action.btn-danger:hover { background: #fef2f2; color: var(&#45;&#45;danger); border-color: var(&#45;&#45;danger); }-->
<!--.btn-history-action:disabled { opacity: 0.4; cursor: not-allowed; }-->
<!--.history-empty { text-align: center; padding: 40px 20px; color: var(&#45;&#45;text-secondary); font-size: 14px; }-->
<!--.history-table-wrapper { overflow-x: auto; border-radius: 12px; border: 1px solid var(&#45;&#45;border-color); }-->
<!--.history-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; min-width: 800px; }-->
<!--.history-table th {-->
<!--  background: linear-gradient(135deg, #f59e0b, #f97316); color: #fff; padding: 10px 12px;-->
<!--  white-space: nowrap; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.3px;-->
<!--}-->
<!--.history-table td {-->
<!--  padding: 9px 12px; border-bottom: 1px solid var(&#45;&#45;border-color); color: var(&#45;&#45;text-primary);-->
<!--  max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;-->
<!--}-->
<!--.history-table tr:hover td { background: #fffbeb; }-->
<!--.history-table tr:last-child td { border-bottom: none; }-->
<!--.cell-mono { font-family: 'Courier New', monospace; font-size: 11px; }-->
<!--.cell-remark { max-width: 120px; }-->
<!--.attr-tag-inline { font-size: 11px; font-weight: 600; color: var(&#45;&#45;accent); }-->
<!--.btn-del-row {-->
<!--  background: none; border: none; color: var(&#45;&#45;danger); font-size: 16px; cursor: pointer;-->
<!--  padding: 2px 6px; border-radius: 4px; transition: var(&#45;&#45;transition); font-weight: 700;-->
<!--}-->
<!--.btn-del-row:hover { background: #fef2f2; }-->

<!--@media (max-width: 700px) {-->
<!--  .qr-info-section { grid-template-columns: 1fr; }-->
<!--  .qr-card { justify-self: center; }-->
<!--  .timer-panel { flex-direction: column; align-items: stretch; }-->
<!--  .timer-divider { width: 100%; height: 1px; margin: 12px 0; }-->
<!--  .timer-left { min-width: auto; }-->
<!--  .attr-grid { grid-template-columns: 1fr; }-->
<!--}-->
<!--</style>-->

<template>
  <div class="dev-mode">
    <div class="dev-badge">🛠️ 开发者模式</div>

    <div class="dev-tabs">
      <button class="dev-tab" :class="{ active: devActiveTab === 'test' }" @click="devActiveTab = 'test'">
        <span class="dev-tab-icon">🧪</span><span>专业测试</span>
      </button>
      <button class="dev-tab" :class="{ active: devActiveTab === 'governance' }" @click="devActiveTab = 'governance'; loadTableList()">
        <span class="dev-tab-icon">🗄️</span><span>数据建表和治理</span>
      </button>
    </div>

    <!-- ==================== 专业测试 Tab ==================== -->
    <div v-if="devActiveTab === 'test'">
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

    <!-- ==================== 数据建表和治理 Tab ==================== -->
    <div v-if="devActiveTab === 'governance'" class="gov-section">

      <!-- 表列表 -->
      <div class="gov-card">
        <div class="gov-card-hd">
          <div class="gov-card-hd-left">
            <span class="gov-icon">📋</span><h3>数据库表列表</h3>
            <span class="gov-badge" v-if="govTables.length">{{ govTables.length }} 张表</span>
          </div>
          <button class="gov-btn-ghost" @click="loadTableList" :disabled="govTableLoading">
            {{ govTableLoading ? '加载中...' : '🔄 刷新' }}
          </button>
        </div>
        <div class="gov-card-bd">
          <div v-if="govTableLoading && !govTables.length" class="gov-loading">正在加载...</div>
          <div v-else-if="!govTables.length" class="gov-empty">暂无数据表</div>
          <div v-else class="gov-tbl-list">
            <div v-for="t in govTables" :key="t" class="gov-tbl-item"
                 :class="{ 'gov-tbl-active': govSelectedTable === t }" @click="selectTable(t)">
              <span class="gov-tbl-icon">📄</span>
              <span class="gov-tbl-name">{{ t }}</span>
              <button class="gov-btn-icon-del" @click.stop="handleDropTable(t)" title="删除表">🗑</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 建表 -->
      <div class="gov-card">
        <div class="gov-card-hd"><span class="gov-icon">➕</span><h3>新建数据表</h3></div>
        <div class="gov-card-bd">
          <div class="gov-field">
            <label>表名</label>
            <input v-model="govCreateTableName" class="gov-input" placeholder="例如: my_new_table" />
          </div>
          <div class="gov-field">
            <label>字段定义（MySQL DDL语法）</label>
            <textarea v-model="govCreateColumns" class="gov-textarea" rows="4"
                      placeholder="示例：&#10;id INT AUTO_INCREMENT PRIMARY KEY,&#10;name VARCHAR(100) NOT NULL,&#10;created_at DATETIME DEFAULT CURRENT_TIMESTAMP"></textarea>
          </div>
          <button class="gov-btn-primary" @click="handleCreateTable" :disabled="govCreating">
            {{ govCreating ? '创建中...' : '创建表' }}
          </button>
          <div v-if="govCreateMsg" class="gov-fb" :class="govCreateMsg.startsWith('✅') ? 'gov-fb-ok' : 'gov-fb-err'">{{ govCreateMsg }}</div>
        </div>
      </div>

      <!-- 表结构 & 字段管理 -->
      <div class="gov-card" v-if="govSelectedTable">
        <div class="gov-card-hd">
          <div class="gov-card-hd-left">
            <span class="gov-icon">🔧</span><h3>表结构：{{ govSelectedTable }}</h3>
            <span class="gov-badge" v-if="govColumns.length">{{ govColumns.length }} 个字段</span>
          </div>
          <button class="gov-btn-ghost" @click="describeTable(govSelectedTable)" :disabled="govDescLoading">
            {{ govDescLoading ? '加载中...' : '🔄 刷新' }}
          </button>
        </div>
        <div class="gov-card-bd">
          <div v-if="govDescLoading" class="gov-loading">正在加载表结构...</div>
          <div v-if="govColumns.length > 0" class="gov-tbl-wrap">
            <table class="gov-data-tbl">
              <thead><tr><th>字段名</th><th>类型</th><th>可空</th><th>键</th><th>默认值</th><th>额外</th><th>注释</th><th>操作</th></tr></thead>
              <tbody>
              <tr v-for="col in govColumns" :key="col.field">
                <td class="gov-mono">{{ col.field }}</td>
                <td class="gov-mono">{{ col.type }}</td>
                <td>{{ col.null }}</td>
                <td>{{ col.key || '-' }}</td>
                <td>{{ col.default || '-' }}</td>
                <td>{{ col.extra || '-' }}</td>
                <td>{{ col.comment || '-' }}</td>
                <td><button class="gov-btn-sm gov-btn-danger" @click="handleDropColumn(col.field)">删除</button></td>
              </tr>
              </tbody>
            </table>
          </div>

          <div class="gov-sub">
            <h4 class="gov-sub-title">➕ 新增字段</h4>
            <div class="gov-inline-row">
              <input v-model="govAddColDef" class="gov-input gov-input-flex" placeholder="字段定义，例如: age INT NOT NULL DEFAULT 0" />
              <button class="gov-btn-primary gov-btn-sm" @click="handleAddColumn" :disabled="govAddColLoading">{{ govAddColLoading ? '...' : '添加' }}</button>
            </div>
            <div v-if="govAddColMsg" class="gov-fb-sm" :class="govAddColMsg.startsWith('✅') ? 'gov-fb-ok' : 'gov-fb-err'">{{ govAddColMsg }}</div>
          </div>

          <div class="gov-sub">
            <h4 class="gov-sub-title">✏️ 修改字段</h4>
            <div class="gov-inline-row">
              <input v-model="govModifyColDef" class="gov-input gov-input-flex" placeholder="完整字段定义，例如: name VARCHAR(200) NOT NULL COMMENT '名称'" />
              <button class="gov-btn-primary gov-btn-sm" @click="handleModifyColumn" :disabled="govModifyColLoading">{{ govModifyColLoading ? '...' : '修改' }}</button>
            </div>
            <div v-if="govModifyColMsg" class="gov-fb-sm" :class="govModifyColMsg.startsWith('✅') ? 'gov-fb-ok' : 'gov-fb-err'">{{ govModifyColMsg }}</div>
          </div>
        </div>
      </div>

      <!-- SQL 执行器 -->
      <div class="gov-card">
        <div class="gov-card-hd"><span class="gov-icon">⚡</span><h3>SQL 执行器</h3></div>
        <div class="gov-card-bd">
          <div class="gov-field">
            <label>目标表（可选，仅用于快捷填充）</label>
            <select v-model="govSqlTable" class="gov-input">
              <option value="">全局执行（不限表）</option>
              <option v-for="t in govTables" :key="t" :value="t">{{ t }}</option>
            </select>
          </div>
          <div class="gov-field">
            <label>SQL 语句</label>
            <textarea v-model="govSqlText" class="gov-textarea gov-sql-ta" rows="5"
                      placeholder="输入SQL语句，例如：&#10;SELECT * FROM test_static LIMIT 10&#10;UPDATE test_static SET remark='test' WHERE URL='xxx'"></textarea>
          </div>
          <div class="gov-btn-row">
            <button class="gov-btn-primary" @click="handleExecuteSQL" :disabled="govSqlLoading">{{ govSqlLoading ? '执行中...' : '▶ 执行SQL' }}</button>
            <button class="gov-btn-ghost" @click="fillSqlTemplate('select')">SELECT模板</button>
            <button class="gov-btn-ghost" @click="fillSqlTemplate('update')">UPDATE模板</button>
            <button class="gov-btn-ghost" @click="fillSqlTemplate('insert')">INSERT模板</button>
          </div>
          <div v-if="govSqlMsg" class="gov-fb" :class="govSqlMsg.startsWith('✅') ? 'gov-fb-ok' : 'gov-fb-err'">{{ govSqlMsg }}</div>
          <div v-if="govSqlResult" class="gov-sql-result">
            <div class="gov-sql-result-hd">
              <span>查询结果</span>
              <span class="gov-sql-rows">{{ govSqlResult.rowCount ?? govSqlResult.affectedRows ?? 0 }} {{ govSqlResult.rowCount !== undefined ? '行' : '行受影响' }}</span>
            </div>
            <div v-if="govSqlResult.rows && govSqlResult.rows.length > 0" class="gov-tbl-wrap">
              <table class="gov-data-tbl gov-sql-tbl">
                <thead><tr><th v-for="key in Object.keys(govSqlResult.rows[0])" :key="key">{{ key }}</th></tr></thead>
                <tbody>
                <tr v-for="(row, idx) in govSqlResult.rows" :key="idx">
                  <td v-for="key in Object.keys(row)" :key="key" class="gov-mono">{{ formatSqlVal(row[key]) }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- 批量导入 -->
      <div class="gov-card">
        <div class="gov-card-hd"><span class="gov-icon">📥</span><h3>从其他数据库批量导入</h3></div>
        <div class="gov-card-bd">
          <div class="gov-field">
            <label>源数据库 JDBC URL</label>
            <input v-model="govImport.sourceUrl" class="gov-input"
                   placeholder="例如: jdbc:mysql://10.1.14.107:3306/test_data?useSSL=false&serverTimezone=Asia/Shanghai" />
          </div>
          <div class="gov-grid-2">
            <div class="gov-field">
              <label>用户名</label>
              <input v-model="govImport.sourceUsername" class="gov-input" placeholder="root" />
            </div>
            <div class="gov-field">
              <label>密码</label>
              <input v-model="govImport.sourcePassword" type="password" class="gov-input" placeholder="密码" />
            </div>
          </div>
          <button class="gov-btn-ghost" @click="fetchSourceTables" :disabled="govImport.fetching || !govImport.sourceUrl">
            {{ govImport.fetching ? '连接中...' : '🔗 连接并获取表列表' }}
          </button>
          <div v-if="govImport.sourceTables.length > 0" class="gov-import-section">
            <label class="gov-import-label">选择要导入的表：</label>
            <div class="gov-cb-grid">
              <label v-for="t in govImport.sourceTables" :key="t" class="gov-cb-item">
                <input type="checkbox" :value="t" v-model="govImport.selectedTables" />
                <span>{{ t }}</span>
              </label>
            </div>
            <label class="gov-cb-item" style="margin-top:12px">
              <input type="checkbox" v-model="govImport.truncateBefore" />
              <span>导入前清空本地同名表数据</span>
            </label>
            <button class="gov-btn-primary gov-btn-import" @click="handleBatchImport"
                    :disabled="govImport.importing || govImport.selectedTables.length === 0">
              {{ govImport.importing ? '导入中...' : '📥 开始批量导入' }}
            </button>
          </div>
          <div v-if="govImport.msg" class="gov-fb" :class="govImport.msg.startsWith('✅') ? 'gov-fb-ok' : 'gov-fb-err'">{{ govImport.msg }}</div>
          <div v-if="govImport.details.length > 0" class="gov-import-details">
            <div v-for="(d, idx) in govImport.details" :key="idx" class="gov-import-detail" :class="'gov-detail-' + d.status">
              <span class="gov-detail-tbl">{{ d.table }}</span>
              <span>{{ d.message }}</span>
            </div>
          </div>
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
  fetchDevHistory, saveDevHistory, clearDevHistory, fetchDevRedisStatus, deleteDevHistoryRecord,
  fetchTableList, fetchTableDescribe, createTable, dropTable,
  addColumn, modifyColumn, dropColumn, executeSQL, batchImport
} from '../api/index.js'

const emit = defineEmits(['error', 'record-saved'])

const devActiveTab = ref('test')

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

// ========== 数据治理 Tab 状态 ==========
const govTables = ref([])
const govTableLoading = ref(false)
const govSelectedTable = ref('')
const govColumns = ref([])
const govDescLoading = ref(false)
const govCreateTableName = ref('')
const govCreateColumns = ref('')
const govCreating = ref(false)
const govCreateMsg = ref('')
const govAddColDef = ref('')
const govAddColLoading = ref(false)
const govAddColMsg = ref('')
const govModifyColDef = ref('')
const govModifyColLoading = ref(false)
const govModifyColMsg = ref('')
const govSqlTable = ref('')
const govSqlText = ref('')
const govSqlLoading = ref(false)
const govSqlMsg = ref('')
const govSqlResult = ref(null)
const govImport = reactive({
  sourceUrl: '', sourceUsername: 'root', sourcePassword: '',
  sourceTables: [], selectedTables: [], truncateBefore: false,
  fetching: false, importing: false, msg: '', details: []
})

// ========== 原有测试函数（完全不动） ==========

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

// ========== 数据治理 Tab 函数 ==========

function formatSqlVal(val) {
  if (val === null || val === undefined) return 'NULL'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}

async function loadTableList() {
  govTableLoading.value = true
  try {
    const json = await fetchTableList()
    if (json.success) {
      govTables.value = json.data?.tables || []
    } else {
      emit('error', json.message || '获取表列表失败')
    }
  } catch (e) {
    emit('error', '获取表列表失败：' + e.message)
  } finally {
    govTableLoading.value = false
  }
}

async function selectTable(tableName) {
  govSelectedTable.value = tableName
  govAddColMsg.value = ''
  govModifyColMsg.value = ''
  govAddColDef.value = ''
  govModifyColDef.value = ''
  await describeTable(tableName)
}

async function describeTable(tableName) {
  govDescLoading.value = true
  govColumns.value = []
  try {
    const json = await fetchTableDescribe(tableName)
    if (json.success) {
      govColumns.value = json.data?.columns || []
    } else {
      emit('error', json.message || '获取表结构失败')
    }
  } catch (e) {
    emit('error', '获取表结构失败：' + e.message)
  } finally {
    govDescLoading.value = false
  }
}

async function handleCreateTable() {
  if (!govCreateTableName.value.trim()) { emit('error', '请输入表名'); return }
  if (!govCreateColumns.value.trim()) { emit('error', '请输入字段定义'); return }
  govCreating.value = true
  govCreateMsg.value = ''
  try {
    const json = await createTable(govCreateTableName.value.trim(), govCreateColumns.value.trim())
    govCreateMsg.value = json.success ? '✅ ' + json.message : '❌ ' + json.message
    if (json.success) { govCreateTableName.value = ''; govCreateColumns.value = ''; await loadTableList() }
  } catch (e) {
    govCreateMsg.value = '❌ 建表请求失败：' + e.message
  } finally {
    govCreating.value = false
  }
}

async function handleDropTable(tableName) {
  if (!confirm(`确定要删除表 "${tableName}" 吗？此操作不可撤销！`)) return
  try {
    const json = await dropTable(tableName)
    if (json.success) {
      if (govSelectedTable.value === tableName) { govSelectedTable.value = ''; govColumns.value = [] }
      await loadTableList()
    } else { emit('error', json.message || '删表失败') }
  } catch (e) {
    emit('error', '删表请求失败：' + e.message)
  }
}

async function handleAddColumn() {
  if (!govSelectedTable.value) return
  if (!govAddColDef.value.trim()) { emit('error', '请输入字段定义'); return }
  govAddColLoading.value = true
  govAddColMsg.value = ''
  try {
    const json = await addColumn(govSelectedTable.value, govAddColDef.value.trim())
    govAddColMsg.value = json.success ? '✅ ' + json.message : '❌ ' + json.message
    if (json.success) { govAddColDef.value = ''; await describeTable(govSelectedTable.value) }
  } catch (e) {
    govAddColMsg.value = '❌ 新增字段失败：' + e.message
  } finally {
    govAddColLoading.value = false
  }
}

async function handleModifyColumn() {
  if (!govSelectedTable.value) return
  if (!govModifyColDef.value.trim()) { emit('error', '请输入字段定义'); return }
  govModifyColLoading.value = true
  govModifyColMsg.value = ''
  try {
    const json = await modifyColumn(govSelectedTable.value, govModifyColDef.value.trim())
    govModifyColMsg.value = json.success ? '✅ ' + json.message : '❌ ' + json.message
    if (json.success) { govModifyColDef.value = ''; await describeTable(govSelectedTable.value) }
  } catch (e) {
    govModifyColMsg.value = '❌ 修改字段失败：' + e.message
  } finally {
    govModifyColLoading.value = false
  }
}

async function handleDropColumn(colName) {
  if (!govSelectedTable.value) return
  if (!confirm(`确定要删除字段 "${colName}" 吗？`)) return
  try {
    const json = await dropColumn(govSelectedTable.value, colName)
    if (json.success) { await describeTable(govSelectedTable.value) }
    else { emit('error', json.message || '删除字段失败') }
  } catch (e) {
    emit('error', '删除字段请求失败：' + e.message)
  }
}

function fillSqlTemplate(type) {
  const t = govSqlTable.value || 'test_static'
  if (type === 'select') govSqlText.value = `SELECT * FROM ${t} LIMIT 20`
  else if (type === 'update') govSqlText.value = `UPDATE ${t} SET 字段名 = '新值' WHERE 条件字段 = '条件值'`
  else if (type === 'insert') govSqlText.value = `INSERT INTO ${t} (字段1, 字段2) VALUES ('值1', '值2')`
}

async function handleExecuteSQL() {
  if (!govSqlText.value.trim()) { emit('error', '请输入SQL语句'); return }
  govSqlLoading.value = true
  govSqlMsg.value = ''
  govSqlResult.value = null
  try {
    const json = await executeSQL(govSqlText.value.trim(), govSqlTable.value)
    govSqlMsg.value = json.success ? '✅ ' + json.message : '❌ ' + json.message
    if (json.success && json.data) { govSqlResult.value = json.data }
  } catch (e) {
    govSqlMsg.value = '❌ 执行SQL失败：' + e.message
  } finally {
    govSqlLoading.value = false
  }
}

async function fetchSourceTables() {
  govImport.fetching = true
  govImport.sourceTables = []
  govImport.selectedTables = []
  govImport.msg = ''
  govImport.details = []
  try {
    const json = await executeSQL('SHOW TABLES', '', govImport.sourceUrl, govImport.sourceUsername, govImport.sourcePassword)
    if (json.success && json.data?.rows) {
      govImport.sourceTables = json.data.rows.map(r => Object.values(r)[0])
    } else {
      govImport.msg = '❌ ' + (json.message || '无法获取源数据库表列表')
    }
  } catch (e) {
    govImport.msg = '❌ 连接源数据库失败：' + e.message
  } finally {
    govImport.fetching = false
  }
}

async function handleBatchImport() {
  if (govImport.selectedTables.length === 0) return
  govImport.importing = true
  govImport.msg = ''
  govImport.details = []
  try {
    const json = await batchImport({
      sourceUrl: govImport.sourceUrl,
      sourceUsername: govImport.sourceUsername,
      sourcePassword: govImport.sourcePassword,
      tables: govImport.selectedTables,
      truncateBefore: govImport.truncateBefore
    })
    govImport.msg = json.success ? '✅ ' + json.message : '❌ ' + json.message
    if (json.success && json.data?.details) { govImport.details = json.data.details }
    if (json.success) { await loadTableList() }
  } catch (e) {
    govImport.msg = '❌ 批量导入请求失败：' + e.message
  } finally {
    govImport.importing = false
  }
}
</script>

<style scoped>.dev-mode {
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

/* ========== Tab 栏 ========== */
.dev-tabs { display: flex; gap: 4px; background: #f4f5f7; border-radius: 12px; padding: 4px; margin-bottom: 24px; }
.dev-tab { flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 10px 0; font-size: 14px; border: none; border-radius: 10px; cursor: pointer; background: transparent; color: #888; font-weight: 600; transition: all 0.25s ease; }
.dev-tab.active { background: #fff; color: #1a1a2e; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.dev-tab:not(.active):hover { color: #555; background: rgba(255,255,255,0.5); }
.dev-tab-icon { font-size: 16px; }

/* ========== 专业测试 Tab 基础样式 ========== */
.top-bar { margin-bottom: 16px; }
.top-actions { display: flex; justify-content: flex-end; margin-top: 10px; }
.duplicate-tip {
  padding: 10px 16px; background: #fef3c7; border: 1px solid #fde68a; border-radius: 10px;
  color: #92400e; font-size: 13px; font-weight: 600; margin-bottom: 10px;
}
.polling-section {
  display: flex; align-items: center; gap: 12px; margin-bottom: 10px;
  padding: 12px 18px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 12px;
}
.polling-status { color: #92400e; font-weight: 600; font-size: 13px; }
.manual-section { margin-bottom: 16px; }
.input-group { display: flex; gap: 8px; }
.input-group input {
  flex: 1; padding: 10px 14px; font-size: 14px; border: 2px solid var(--border-color, #e5e7eb);
  border-radius: 10px; outline: none; transition: border-color 0.2s; background: var(--bg-card, #fff);
}
.input-group input:focus { border-color: var(--accent, #6366f1); box-shadow: 0 0 0 3px rgba(99,102,241,0.15); }
.btn-refresh {
  background: linear-gradient(135deg, var(--accent, #6366f1), #a855f7); color: #fff; border: none;
  padding: 10px 20px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600;
  transition: all 0.2s; white-space: nowrap;
}
.btn-refresh:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.3); }
.btn-refresh:disabled { opacity: 0.5; cursor: not-allowed; }
.loading { text-align: center; padding: 32px 20px; color: var(--text-secondary, #9ca3af); font-size: 14px; font-weight: 500; }
.empty-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 50px 20px; background: var(--bg-card, #fff); border: 2px dashed var(--border-color, #e5e7eb);
  border-radius: 16px; margin-bottom: 20px;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-text { font-size: 14px; color: var(--text-secondary, #9ca3af); font-weight: 500; text-align: center; line-height: 1.6; }
.qr-info-section {
  display: grid; grid-template-columns: auto 1fr; gap: 20px; margin-bottom: 24px;
  background: var(--bg-card, #fff); border: 1px solid var(--border-color, #e5e7eb); border-radius: 16px;
  padding: 24px; box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.qr-card { text-align: center; }
.qr-card-header { display: flex; align-items: center; justify-content: center; gap: 8px; margin-bottom: 16px; }
.qr-card-icon { font-size: 20px; }
.qr-card-title { font-size: 15px; font-weight: 700; color: var(--text-primary, #1a1a2e); }
.qrcode-container { display: flex; justify-content: center; }
.side-info { display: flex; flex-direction: column; gap: 12px; justify-content: center; }
.info-card { border: 1px solid var(--border-color, #e5e7eb); border-radius: 12px; padding: 16px 20px; background: var(--bg-primary, #fafafa); }
.info-card-label { font-size: 11px; font-weight: 700; color: var(--text-secondary, #9ca3af); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }
.info-card-value { font-size: 14px; font-weight: 600; color: var(--text-primary, #1a1a2e); word-break: break-all; }
.action-row { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.btn-event {
  background: linear-gradient(135deg, #3b82f6, #6366f1); color: #fff; border: none;
  padding: 10px 20px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600;
  transition: all 0.2s;
}
.btn-event:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(59,130,246,0.3); }
.btn-event:disabled { opacity: 0.5; cursor: not-allowed; }
.timer-panel {
  display: flex; align-items: center; gap: 0; background: #fdf2f8;
  border: 1px solid #fbcfe8; border-radius: 14px; padding: 16px 20px; margin-bottom: 16px; min-height: 80px;
}
.timer-left { flex-shrink: 0; min-width: 200px; }
.timer-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.timer-label { font-size: 13px; font-weight: 600; color: #831843; }
.timer-input {
  width: 70px; padding: 6px 8px; font-size: 13px; border: 2px solid #f9a8d4; border-radius: 8px;
  outline: none; text-align: center; background: #fff;
}
.timer-input:focus { border-color: #ec4899; }
.btn-timer {
  background: #ec4899; color: #fff; border: none; padding: 6px 14px; font-size: 12px;
  border-radius: 8px; cursor: pointer; font-weight: 600; transition: all 0.2s;
}
.btn-timer:hover:not(:disabled) { background: #db2777; }
.btn-timer:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-timer-cancel {
  background: #fff; color: #be185d; border: 1px solid #f9a8d4; padding: 6px 14px;
  font-size: 12px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: all 0.2s;
}
.btn-timer-cancel:hover { background: #fdf2f8; }
.timer-status { margin-top: 6px; font-size: 12px; color: #9d174d; font-weight: 500; }
.timer-divider { width: 1px; align-self: stretch; background: #f9a8d4; margin: 0 18px; flex-shrink: 0; }
.timer-right { flex: 1; min-width: 0; }
.event-result { padding: 10px 14px; background: #fff; border-radius: 10px; border: 1px solid #f9a8d4; }
.event-result.no-event { text-align: center; }
.event-label { font-size: 11px; font-weight: 700; color: var(--text-secondary, #9ca3af); text-transform: uppercase; letter-spacing: 0.3px; margin-bottom: 2px; }
.event-value { font-size: 14px; font-weight: 700; color: var(--text-primary, #1a1a2e); }
.event-value.highlight { font-size: 16px; color: #059669; }
.btn-frozen {
  margin-top: 8px; background: #0ea5e9; color: #fff; border: none; padding: 8px 18px;
  font-size: 12px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: all 0.2s;
}
.btn-frozen:hover:not(:disabled) { background: #0284c7; }
.btn-frozen:disabled { opacity: 0.5; cursor: not-allowed; }
.frozen-result { margin-top: 6px; padding: 8px 12px; background: #f0f9ff; border-radius: 8px; }
.frozen-label { font-size: 11px; font-weight: 700; color: #0369a1; margin-bottom: 2px; }
.frozen-value { font-size: 13px; color: #0c4a6e; font-weight: 500; }
.event-compare {
  margin-top: 6px; padding: 6px 10px; background: #f0fdf4; border-radius: 8px;
  font-size: 12px; color: #166534; font-weight: 500;
}
.diff { font-weight: 700; color: #059669; }
.attribution-tags { margin-top: 6px; display: flex; gap: 6px; flex-wrap: wrap; }
.attr-tag {
  font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 6px;
  background: #eef2ff; color: #4f46e5;
}
.attr-tag.appflyer { background: #f0fdf4; color: #166534; }
.attr-tag.adjust { background: #fffbeb; color: #92400e; }
.attr-tag.singular { background: #eff6ff; color: #1e40af; }
.attr-tag.tenjin { background: #faf5ff; color: #7e22ce; }
.no-attribution { margin-top: 6px; font-size: 12px; color: #d97706; font-weight: 600; }
.event-placeholder { display: flex; align-items: center; gap: 8px; color: #9ca3af; font-size: 13px; font-weight: 500; }
.event-placeholder-icon { font-size: 18px; }

/* ========== 归因查询 ========== */
.section-title { font-size: 16px; font-weight: 700; color: var(--text-primary, #1a1a2e); margin-bottom: 14px; }
.attr-section { margin-bottom: 24px; }
.attr-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.attr-panel {
  border: 1px solid var(--border-color, #e5e7eb); border-radius: 14px; padding: 16px;
  background: var(--bg-card, #fff); box-shadow: 0 1px 4px rgba(0,0,0,0.04); display: flex; flex-direction: column;
}
.attr-panel.appflyer { border-top: 3px solid #22c55e; }
.attr-panel.adjust { border-top: 3px solid #f59e0b; }
.attr-panel.singular { border-top: 3px solid #3b82f6; }
.attr-panel.tenjin { border-top: 3px solid #a855f7; }
.attr-panel-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.attr-panel-icon { font-size: 16px; }
.attr-panel-title { font-size: 14px; font-weight: 700; color: var(--text-primary, #1a1a2e); }
.attr-count { font-size: 12px; color: var(--text-secondary, #9ca3af); font-weight: 600; }
.attr-input-row { display: flex; gap: 8px; margin-bottom: 10px; }
.attr-input-row input {
  flex: 1; padding: 8px 12px; font-size: 13px; border: 2px solid var(--border-color, #e5e7eb);
  border-radius: 8px; outline: none; transition: border-color 0.2s; background: var(--bg-card, #fff);
}
.attr-input-row input:focus { border-color: var(--accent, #6366f1); box-shadow: 0 0 0 3px rgba(99,102,241,0.15); }
.btn-attr-query {
  background: linear-gradient(135deg, var(--accent, #6366f1), #a855f7); color: #fff; border: none;
  padding: 8px 16px; font-size: 13px; border-radius: 8px; cursor: pointer; font-weight: 600;
  transition: all 0.2s; white-space: nowrap;
}
.btn-attr-query:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.3); }
.btn-attr-query:disabled { opacity: 0.5; cursor: not-allowed; }
.attr-results { max-height: 260px; overflow-y: auto; flex: 1; }
.attr-record {
  border: 1px solid var(--border-color, #e5e7eb); border-radius: 10px; padding: 10px 12px; margin-bottom: 8px;
  background: var(--bg-primary, #fafafa);
}
.attr-record:last-child { margin-bottom: 0; }
.attr-field { display: flex; gap: 8px; padding: 3px 0; border-bottom: 1px solid #f0f0f5; font-size: 12px; }
.attr-field:last-child { border-bottom: none; }
.attr-field-key { font-weight: 700; color: var(--text-secondary, #9ca3af); min-width: 110px; flex-shrink: 0; }
.attr-field-val { color: var(--text-primary, #1a1a2e); word-break: break-all; line-height: 1.5; }
.attr-empty { text-align: center; color: var(--text-secondary, #9ca3af); font-size: 13px; padding: 16px; }

/* ========== 入库表单 ========== */
.form-section {
  background: var(--bg-card, #fff); border: 1px solid var(--border-color, #e5e7eb); border-radius: 14px;
  padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.form-section h4 { font-size: 15px; font-weight: 700; color: var(--text-primary, #1a1a2e); margin-bottom: 16px; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 12px; font-weight: 700; color: var(--text-secondary, #9ca3af); margin-bottom: 4px; text-transform: uppercase; letter-spacing: 0.3px; }
.form-group input, .form-group select, .form-group textarea {
  width: 100%; padding: 9px 12px; font-size: 13px; border: 2px solid var(--border-color, #e5e7eb);
  border-radius: 8px; outline: none; transition: border-color 0.2s; background: var(--bg-card, #fff);
  box-sizing: border-box; font-family: inherit;
}
.form-group input:focus, .form-group select:focus, .form-group textarea:focus {
  border-color: var(--accent, #6366f1); box-shadow: 0 0 0 3px rgba(99,102,241,0.15);
}
.form-group textarea { resize: vertical; min-height: 60px; }
.btn-save {
  background: linear-gradient(135deg, #22c55e, #16a34a); color: #fff; border: none;
  padding: 10px 28px; font-size: 14px; border-radius: 10px; cursor: pointer; font-weight: 700;
  transition: all 0.2s; margin-top: 4px;
}
.btn-save:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(34,197,94,0.3); }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }
.save-success { margin-top: 8px; font-size: 13px; font-weight: 600; color: #166534; }

/* ========== 历史记录 ========== */
.history-section { margin-top: 32px; }
.history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 8px; }
.history-actions { display: flex; align-items: center; gap: 10px; }
.redis-status { font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 8px; }
.redis-status.ok { background: #f0fdf4; color: #166534; }
.redis-status.warn { background: #fef3c7; color: #92400e; }
.btn-history-action {
  background: var(--bg-primary, #fafafa); color: var(--text-secondary, #6b7280); border: 1px solid var(--border-color, #e5e7eb);
  padding: 6px 14px; font-size: 12px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: all 0.2s;
}
.btn-history-action:hover { background: rgba(99,102,241,0.08); color: var(--accent, #6366f1); border-color: var(--accent, #6366f1); }
.btn-history-action.btn-danger:hover { background: #fef2f2; color: #dc2626; border-color: #dc2626; }
.btn-history-action:disabled { opacity: 0.4; cursor: not-allowed; }
.history-empty { text-align: center; padding: 40px 20px; color: var(--text-secondary, #9ca3af); font-size: 14px; }
.history-table-wrapper { overflow-x: auto; border-radius: 12px; border: 1px solid var(--border-color, #e5e7eb); }
.history-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; min-width: 800px; }
.history-table th {
  background: linear-gradient(135deg, #f59e0b, #f97316); color: #fff; padding: 10px 12px;
  white-space: nowrap; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.3px;
}
.history-table td {
  padding: 9px 12px; border-bottom: 1px solid var(--border-color, #e5e7eb); color: var(--text-primary, #1a1a2e);
  max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.history-table tr:hover td { background: #fffbeb; }
.history-table tr:last-child td { border-bottom: none; }
.cell-mono { font-family: 'Courier New', monospace; font-size: 11px; }
.cell-remark { max-width: 120px; }
.attr-tag-inline { font-size: 11px; font-weight: 600; color: var(--accent, #6366f1); }
.btn-del-row {
  background: none; border: none; color: #dc2626; font-size: 16px; cursor: pointer;
  padding: 2px 6px; border-radius: 4px; transition: all 0.2s; font-weight: 700;
}
.btn-del-row:hover { background: #fef2f2; }

/* ========== 复测弹窗 ========== */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center; z-index: 999;
}
.modal-box {
  background: #fff; border-radius: 20px; padding: 28px 32px; max-width: 420px; width: 90%;
  box-shadow: 0 24px 80px rgba(0,0,0,0.2); text-align: center;
}
.modal-title { font-size: 18px; font-weight: 700; color: #1a1a2e; margin-bottom: 8px; }
.modal-desc { font-size: 14px; color: #6b7280; margin-bottom: 20px; }
.modal-actions { display: flex; gap: 10px; justify-content: center; }
.btn-modal {
  padding: 10px 22px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600;
  border: none; transition: all 0.2s;
}
.btn-modal.btn-poll { background: #6366f1; color: #fff; }
.btn-modal.btn-poll:hover { background: #4f46e5; }
.btn-modal.btn-retest { background: #22c55e; color: #fff; }
.btn-modal.btn-retest:hover:not(:disabled) { background: #16a34a; }
.btn-modal.btn-retest:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-modal.btn-cancel { background: #f3f4f6; color: #6b7280; }
.btn-modal.btn-cancel:hover { background: #e5e7eb; }

/* ================================================================
   数据建表和治理 Tab 样式
   ================================================================ */
.gov-section { animation: govFadeUp 0.3s ease; }
@keyframes govFadeUp { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.gov-card {
  background: #fff; border-radius: 14px; border: 1px solid #eaeaea;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); margin-bottom: 20px; overflow: hidden;
}
.gov-card-hd {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-bottom: 1px solid #f0f0f0;
}
.gov-card-hd-left { display: flex; align-items: center; gap: 8px; }
.gov-card-hd h3 { margin: 0; font-size: 15px; color: #333; font-weight: 700; }
.gov-icon { font-size: 18px; }
.gov-badge {
  font-size: 11px; font-weight: 600; color: #6366f1; background: #eef2ff;
  padding: 2px 10px; border-radius: 8px;
}
.gov-card-bd { padding: 20px; }
.gov-loading { text-align: center; color: #999; padding: 24px; font-size: 13px; }
.gov-empty { text-align: center; color: #999; padding: 32px; font-size: 13px; }

/* 表列表 */
.gov-tbl-list { display: flex; flex-wrap: wrap; gap: 8px; }
.gov-tbl-item {
  display: flex; align-items: center; gap: 6px; padding: 8px 14px;
  border: 1px solid #e5e7eb; border-radius: 10px; cursor: pointer;
  font-size: 13px; font-weight: 500; transition: all 0.2s; background: #fafafa;
}
.gov-tbl-item:hover { border-color: #a5b4fc; background: #eef2ff; }
.gov-tbl-active { border-color: #6366f1 !important; background: #eef2ff !important; color: #4338ca; font-weight: 600; }
.gov-tbl-icon { font-size: 14px; }
.gov-tbl-name { flex: 1; }
.gov-btn-icon-del {
  background: none; border: none; cursor: pointer; font-size: 14px; padding: 2px 4px;
  opacity: 0.4; transition: all 0.2s; border-radius: 4px;
}
.gov-btn-icon-del:hover { opacity: 1; background: #fef2f2; }

/* 表单元素 */
.gov-field { margin-bottom: 14px; }
.gov-field label {
  display: block; font-size: 12px; font-weight: 700; color: #6b7280;
  margin-bottom: 4px; text-transform: uppercase; letter-spacing: 0.3px;
}
.gov-input {
  width: 100%; padding: 9px 12px; font-size: 13px; border: 2px solid #e5e7eb;
  border-radius: 8px; outline: none; transition: border-color 0.2s; background: #fff;
  box-sizing: border-box; font-family: inherit;
}
.gov-input:focus { border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,0.12); }
.gov-textarea {
  width: 100%; padding: 9px 12px; font-size: 13px; border: 2px solid #e5e7eb;
  border-radius: 8px; outline: none; transition: border-color 0.2s; background: #fff;
  box-sizing: border-box; font-family: 'Courier New', monospace; resize: vertical;
}
.gov-textarea:focus { border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,0.12); }
.gov-sql-ta { min-height: 100px; }
.gov-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

/* 按钮 */
.gov-btn-primary {
  background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; border: none;
  padding: 9px 22px; font-size: 13px; border-radius: 8px; cursor: pointer;
  font-weight: 600; transition: all 0.2s;
}
.gov-btn-primary:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.3); }
.gov-btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.gov-btn-ghost {
  background: #fff; color: #6b7280; border: 1px solid #e5e7eb;
  padding: 8px 16px; font-size: 12px; border-radius: 8px; cursor: pointer;
  font-weight: 600; transition: all 0.2s;
}
.gov-btn-ghost:hover:not(:disabled) { background: #f9fafb; color: #6366f1; border-color: #a5b4fc; }
.gov-btn-ghost:disabled { opacity: 0.4; cursor: not-allowed; }
.gov-btn-sm { padding: 6px 14px; font-size: 12px; }
.gov-btn-danger {
  background: #fef2f2; color: #dc2626; border: 1px solid #fecaca;
}
.gov-btn-danger:hover { background: #fee2e2; }
.gov-btn-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.gov-btn-sql { min-width: 120px; }
.gov-btn-import { margin-top: 14px; width: 100%; padding: 12px; font-size: 14px; }

/* 反馈消息 */
.gov-fb {
  margin-top: 10px; padding: 10px 14px; border-radius: 8px; font-size: 13px; font-weight: 600;
}
.gov-fb-ok { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
.gov-fb-err { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
.gov-fb-sm {
  margin-top: 6px; padding: 6px 10px; border-radius: 6px; font-size: 12px; font-weight: 600;
}

/* 数据表格 */
.gov-tbl-wrap { overflow-x: auto; border-radius: 10px; border: 1px solid #e5e7eb; margin-bottom: 16px; }
.gov-data-tbl { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; }
.gov-data-tbl th {
  background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff;
  padding: 9px 12px; white-space: nowrap; font-size: 11px; font-weight: 700;
  text-transform: uppercase; letter-spacing: 0.3px;
}
.gov-data-tbl td {
  padding: 8px 12px; border-bottom: 1px solid #f0f0f5; color: #374151;
  max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.gov-data-tbl tr:hover td { background: #f9fafb; }
.gov-data-tbl tr:last-child td { border-bottom: none; }
.gov-mono { font-family: 'Courier New', monospace; font-size: 11px; }
.gov-sql-tbl { min-width: 600px; }

/* 子区块（新增字段/修改字段） */
.gov-sub {
  margin-top: 18px; padding-top: 16px; border-top: 1px solid #f0f0f0;
}
.gov-sub-title {
  font-size: 14px; font-weight: 700; color: #374151; margin-bottom: 10px;
}
.gov-inline-row { display: flex; gap: 8px; align-items: center; }
.gov-input-flex { flex: 1; }

/* SQL 结果 */
.gov-sql-result { margin-top: 16px; }
.gov-sql-result-hd {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; background: #f9fafb; border: 1px solid #e5e7eb;
  border-radius: 10px 10px 0 0; font-size: 13px; font-weight: 700; color: #374151;
}
.gov-sql-rows { font-size: 12px; font-weight: 600; color: #6366f1; }
.gov-sql-result .gov-tbl-wrap { border-radius: 0 0 10px 10px; border-top: none; }

/* 批量导入 */
.gov-import-section { margin-top: 16px; }
.gov-import-label { font-size: 13px; font-weight: 700; color: #374151; margin-bottom: 8px; display: block; }
.gov-cb-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 6px; }
.gov-cb-item {
  display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 500;
  color: #374151; cursor: pointer; padding: 6px 10px; border-radius: 8px;
  transition: background 0.15s;
}
.gov-cb-item:hover { background: #f3f4f6; }
.gov-cb-item input[type="checkbox"] { width: 16px; height: 16px; accent-color: #6366f1; cursor: pointer; }
.gov-import-details { margin-top: 14px; }
.gov-import-detail {
  display: flex; align-items: center; gap: 10px; padding: 8px 14px;
  border-radius: 8px; font-size: 13px; margin-bottom: 4px;
}
.gov-detail-success { background: #f0fdf4; color: #166534; }
.gov-detail-error { background: #fef2f2; color: #991b1b; }
.gov-detail-skipped { background: #fef3c7; color: #92400e; }
.gov-detail-empty { background: #f9fafb; color: #6b7280; }
.gov-detail-tbl { font-weight: 700; font-family: 'Courier New', monospace; min-width: 120px; }

/* ========== 响应式 ========== */
@media (max-width: 700px) {
  .qr-info-section { grid-template-columns: 1fr; }
  .qr-card { justify-self: center; }
  .timer-panel { flex-direction: column; align-items: stretch; }
  .timer-divider { width: 100%; height: 1px; margin: 12px 0; }
  .timer-left { min-width: auto; }
  .attr-grid { grid-template-columns: 1fr; }
  .gov-grid-2 { grid-template-columns: 1fr; }
  .gov-cb-grid { grid-template-columns: 1fr; }
  .gov-inline-row { flex-direction: column; }
  .gov-btn-row { flex-direction: column; }
}
</style>
