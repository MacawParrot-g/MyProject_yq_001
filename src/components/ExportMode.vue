<template>
  <div>
    <div style="margin-bottom: 20px;">
      <button class="btn-refresh" @click="fetchList" :disabled="loading">{{ loading ? '查询中...' : '刷新未导出数据' }}</button>
    </div>

    <div v-if="loading && !queried" class="loading">正在查询数据库...</div>

    <div v-if="!loading && queried && list.length === 0" class="export-empty">
      <div class="empty-icon">📭</div>
      <div>所有数据均已导出，暂无未导出记录</div>
    </div>

    <div v-if="list.length > 0">
      <div class="export-header">
        <div class="export-count">共 <span>{{ list.length }}</span> 条未导出数据</div>
        <button class="btn-export-exec" @click="doExport" :disabled="executing">
          {{ executing ? '执行中...' : '执行导出脚本' }}
        </button>
      </div>
      <div v-if="resultMsg" class="export-result-box" :class="resultSuccess ? 'success' : 'fail'">
        <div class="result-title">{{ resultSuccess ? '✅ 导出成功' : '❌ 导出失败' }}</div>
        <div>{{ resultMsg }}</div>
        <div v-if="resultData" style="white-space: pre-wrap; font-size: 12px; margin-top: 6px; padding-top: 6px; border-top: 1px dashed #ccc;">
          <strong>Python输出：</strong>{{ resultData }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchUnexported, executeExport } from '../api/index.js'

const emit = defineEmits(['error'])
const list = ref([])
const loading = ref(false)
const queried = ref(false)
const executing = ref(false)
const resultMsg = ref('')
const resultSuccess = ref(false)
const resultData = ref('')

async function fetchList() {
  loading.value = true; queried.value = false; resultMsg.value = ''; resultData.value = ''
  try {
    const json = await fetchUnexported()
    if (json.success) {
      list.value = json.data || []
    }
    else {
      emit('error', json.resultMsg || '查询失败'); list.value = []
    }
  } catch (e) {
    emit('error', '查询请求失败：' + e.message); list.value = []
  }
  finally {
    loading.value = false; queried.value = true
  }
}

async function doExport() {
  executing.value = true; resultMsg.value = ''; resultData.value = ''; resultSuccess.value = false
  try {
    const json = await executeExport()
    resultSuccess.value = json.success; resultMsg.value = json.message || ''; resultData.value = json.data || ''
    if (json.success) {
      await fetchList()
    }
  } catch (e) {
    resultSuccess.value = false; resultMsg.value = '请求失败：' + e.message
  }
  finally {
    executing.value = false
  }
}

onMounted(() => { fetchList() })
</script>
