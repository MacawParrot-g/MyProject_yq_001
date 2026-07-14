<template>
  <div>
    <div class="floating-text">
    <h4>请注意!该模式下自动去重不生效</h4>
      </div>
    <div class="input-group">
      <input type="text" v-model="appId" placeholder="请输入 App ID" @keyup.enter="fetchData" />
      <button class="btn-refresh" @click="fetchData" :disabled="loading || !appId.trim()">
        {{ loading ? '加载中...' : '生成' }}
      </button>
    </div>
    <div class="preview-url" v-if="appId.trim()">
      <strong>请求地址：</strong>https://d-reporter.de123.net/ad/play/task?appId={{ appId.trim() }}
    </div>

    <div v-if="duplicateTip" class="duplicate-tip">⚠️ {{ duplicateTip }}</div>
    <div v-if="loading && !downloadUrl" class="loading">正在获取数据...</div>

    <div class="qrcode-container" v-if="downloadUrl">
      <canvas ref="qrCanvas"></canvas>
    </div>

    <div class="info-section" v-if="downloadUrl">
      <div class="info-item">
        <div class="info-label">Download URL</div>
        <div class="info-value"><a :href="downloadUrl" target="_blank">{{ downloadUrl }}</a></div>
      </div>
      <div class="info-item">
        <div class="info-label">Bundle ID</div>
        <div class="info-value">{{ bundleId }}</div>
      </div>
    </div>

    <button class="btn-event" v-if="bundleId" @click="queryEvent" :disabled="eventLoading">
      {{ eventLoading ? '查询中...' : '查询事件' }}
    </button>
    <div class="timer-section" v-if="bundleId">
      <div class="timer-row">
        <label class="timer-label">⏱ 定时查询</label>
        <input class="timer-input" type="number" v-model.number="timerSeconds" min="1" max="3600" placeholder="秒数" />
        <button class="btn-timer" @click="startTimer" :disabled="timerCountdown > 0 || !timerSeconds || timerSeconds < 1">
          {{ timerCountdown > 0 ? timerCountdown + 's' : '开始定时' }}
        </button>
        <button class="btn-timer-cancel" v-if="timerCountdown > 0" @click="cancelTimer">取消</button>
      </div>
      <div class="timer-status" v-if="timerMsg">{{ timerMsg }}</div>
    </div>
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
    </div>

    <div class="form-section" v-if="downloadUrl && eventResult">
      <h4>填写入库信息（直接写入MySQL）</h4>
      <div class="form-group">
        <label>异常类型</label>
        <select v-model="form.exception_type">
          <option value="" disabled>请选择异常类型</option>
          <option v-for="opt in exceptionOptions" :key="opt" :value="opt">{{ opt }}</option>
        </select>
      </div>
      <div class="form-group">
        <label>备注</label>
        <textarea v-model="form.remark" placeholder="请输入备注信息"></textarea>
      </div>
      <div style="display:flex; gap:12px;">
        <div class="form-group" style="flex:1"><label>记录人</label><input v-model="form.recorder" /></div>
        <div class="form-group" style="flex:1"><label>记录日期</label><input v-model="form.record_data" /></div>
      </div>
      <button class="btn-save" @click="saveToMySQL" :disabled="saving">
        {{ saving ? '入库中...' : '直接入库MySQL' }}
      </button>
      <div class="save-success" v-if="saveMsg">{{ saveMsg }}</div>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive, watch, nextTick, onUnmounted, onMounted} from 'vue'
import QRCode from 'qrcode'
import { fetchObtain, fetchEvent, fetchAllAttributions, fetchFrozen, insertRecord } from '../api/index.js'

const emit = defineEmits(['error'])
const exceptionOptions = ['正常','iOS16闪退','iOS13/14/16均闪退','需要iOS18以上','地区不支持','硬件版本过低','超过10分钟0上报','越狱检测','其他','验证已解决']
const softwareType=ref('')
const appId = ref('')
const downloadUrl = ref('')
const bundleId = ref('')
const originalCurrentTargetNum = ref(null)
const eventResult = ref('')
const newCurrentTargetNum = ref(null)
let attributions = ref([])
const eventId = ref(null)
const frozenMsg = ref('')
const duplicateTip = ref('')
const loading = ref(false)
const eventLoading = ref(false)
const frozenLoading = ref(false)
const saving = ref(false)
const saveMsg = ref('')
const qrCanvas = ref(null)
const timerSeconds = ref(30)
const timerCountdown = ref(0)
let timerMsg = ref('')
let timerInterval = null
let isFrozen=ref('')
const MAX_RETRIES = 5
let retryCount = 0

const form = reactive(
    {
      exception_type: '',
      remark: '',
      recorder: '冯俊杰',
      record_data: getTodayStr()
    })

function getTodayStr() {
  const d = new Date();
  return d.getFullYear() + '/' + (d.getMonth() + 1) + '/' + d.getDate()
}

async function renderQR(url) {
  await nextTick()
  if (url && qrCanvas.value) {
    try {
      await QRCode.toCanvas(qrCanvas.value, url, {
        width: 256,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#ffffff'
        }
      })
    } catch (e) { console.error('QR码生成失败:', e) }
  }
}

watch(downloadUrl, (val) => { renderQR(val) })

async function fetchData() {
  const id = appId.value.trim(); if (!id) return
  loading.value = true; emit('error', ''); resetState()
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
        setTimeout(() => fetchData(), 1500)
        return
      }
      retryCount = 0; duplicateTip.value = ''
      downloadUrl.value = json.data.downloadUrl || ''; bundleId.value = json.data.bundleId || ''
      originalCurrentTargetNum.value = json.data.currentTargetNum ?? null
    } else {
      emit('error', '接口返回异常：' + (json.resultMsg || '未知错误'))
    }
  } catch (e) {
    if (e.name === 'AbortError') {
      emit('error', '请求超时')
    }
    else {
      emit('error', '请求失败：' + e.message)
    }
  } finally { loading.value = false }
}

async function queryEvent() {
  if (!bundleId.value) return
  eventLoading.value = true; emit('error', '')
  eventResult.value = ''; newCurrentTargetNum.value = null; attributions.value = []; eventId.value = null; frozenMsg.value = ''
  try {
    const [eventJson, attrResults] = await Promise.all([fetchEvent(bundleId.value), fetchAllAttributions(bundleId.value)])
    if (eventJson.success && eventJson.data) {
      const newCurrent = eventJson.data.currentTargetNum; eventId.value = eventJson.data.id ?? null
      if (newCurrent !== originalCurrentTargetNum.value) {
        eventResult.value = 'has_event'; newCurrentTargetNum.value = newCurrent
      }
      else {
        eventResult.value = 'no_event'
      }
    } else {
      emit('error', '事件接口返回异常：' + (eventJson.resultMsg || '未知错误'))
    }
    const found = []
    for (const { type, json } of attrResults) {
      if (json.success && Array.isArray(json.data) && json.data.length > 0) found.push(type) }
    attributions.value = found
  } catch (e) { emit('error', '事件查询失败：' + e.message) }
  finally { eventLoading.value = false }
}

async function doFrozen() {
  if (!eventId.value) return
  frozenLoading.value = true; emit('error', '')
  try {
    const json = await fetchFrozen(eventId.value);
      if (json.success) {
        frozenMsg.value = json.resultMsg || '操作完成'
        isFrozen.value=',已冻结'
      } else {
        emit('error', '冻结接口返回异常：' + (json.resultMsg || '未知错误'))
      }
  }
  catch (e) {
    emit('error', '冻结请求失败：' + e.message)
  }
  finally {
    frozenLoading.value = false
  }
}

async function saveToMySQL() {
  if (!form.exception_type.trim()) { emit('error', '请选择异常类型'); return }
  saving.value = true; saveMsg.value = ''
  if(newCurrentTargetNum.value===0||newCurrentTargetNum.value===null){
    attributions=[]
    alert('无新增事件token，即使原本的token有归因，也不会被设置在字段内')
  }
  try {
    const json = await insertRecord({
      URL: downloadUrl.value, bundleId: bundleId.value,
      ascribe: (attributions.value || []).join(';'),
      event_number: newCurrentTargetNum.value,
      exception_type: form.exception_type.trim(),
      record_data: form.record_data,
      recorder: form.recorder,
      remark: form.remark.trim()+isFrozen.value,
      isOutput: 0
    })
    if (json.success) { saveMsg.value = '✅ ' + (json.resultMsg || '入库成功') }
    else { emit('error', json.resultMsg || '入库失败') }
  } catch (e) { emit('error', '入库请求失败：' + e.message) }
  finally { saving.value = false }
}

function resetState() {
  downloadUrl.value = '';
  bundleId.value = '';
  originalCurrentTargetNum.value = null
  eventResult.value = '';
  newCurrentTargetNum.value = null;
  attributions.value = []
  eventId.value = null;
  frozenMsg.value = '';
  duplicateTip.value = '';
  saveMsg.value = ''
  retryCount = 0
  form.exception_type = '';
  form.remark = '';
  form.recorder = '冯俊杰';
  form.record_data = getTodayStr()
  isFrozen.value=''
}
// function startTimer() {
//   if (!timerSeconds.value || timerSeconds.value < 1) return
//   timerCountdown.value = timerSeconds.value
//   timerMsg.value = '将在 ' + timerCountdown.value + ' 秒后自动查询事件'
//   timerInterval = setInterval(() => {
//     timerCountdown.value--
//     if (timerCountdown.value <= 0) {
//       clearInterval(timerInterval)
//       timerInterval = null
//       timerCountdown.value = 0
//       timerMsg.value = '⏰ 定时已到，正在自动查询事件...'
//       queryEvent().then(() => {
//         timerMsg.value = '✅ 定时查询已完成'
//       })
//     }
//   }, 1000)
// }
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
        setTimeout(() => {
          timerMsg.value = '计时结束后内容会在这里显示'
        }, 5000);
      })
    }
  }, 1000)
}

function cancelTimer() {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
  timerCountdown.value = 0
  timerMsg.value = ''
}

onUnmounted(() => { cancelTimer() })
onMounted(() => {
  timerMsg.value='计时结束后内容会在这里显示' }
)
</script>
