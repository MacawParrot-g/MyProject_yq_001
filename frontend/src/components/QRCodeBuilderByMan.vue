<script setup>
import { ref, watch, nextTick } from 'vue'
import QRCode from 'qrcode'

const inputText = ref('')
const qrCanvas = ref(null)
const qrSize = ref(256)

async function renderQR() {
  await nextTick()
  if (!qrCanvas.value) return
  if (!inputText.value.trim()) {
    const ctx = qrCanvas.value.getContext('2d')
    ctx.clearRect(0, 0, qrCanvas.value.width, qrCanvas.value.height)
    return
  }
  try {
    await QRCode.toCanvas(qrCanvas.value, inputText.value, {
      width: Number(qrSize.value) || 256,
      margin: 2,
      color: { dark: '#000000', light: '#ffffff' }
    })
  } catch (e) {
    console.error('QR码生成失败:', e)
  }
}

watch([inputText, qrSize], () => { renderQR() })
</script>

<template>
  <div class="qr-builder">
    <h2>手动二维码生成器</h2>

    <div class="input-section">
      <label>输入内容</label>
      <textarea
          v-model="inputText"
          placeholder="请输入要转换为二维码的内容（如网址、文本等）"
          rows="4"
      ></textarea>
    </div>

    <div class="size-section">
      <label>二维码尺寸</label>
      <input type="number" v-model.number="qrSize" min="128" max="512" step="32" />
      <span>px</span>
    </div>

    <div class="qr-preview" v-if="inputText.trim()">
      <canvas ref="qrCanvas"></canvas>
    </div>
    <div class="qr-placeholder" v-else>
      <p>输入内容后自动生成二维码</p>
    </div>
  </div>
</template>

<style scoped>
.qr-builder {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px;
}

h2 {
  text-align: center;
  margin-bottom: 24px;
}

.input-section,
.size-section {
  margin-bottom: 16px;
}

.input-section label,
.size-section label {
  display: block;
  font-weight: bold;
  margin-bottom: 6px;
}

textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
}

.size-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.size-section input {
  width: 80px;
  padding: 6px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 14px;
}

.qr-preview {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.qr-placeholder {
  text-align: center;
  color: #999;
  margin-top: 20px;
  padding: 40px;
  border: 2px dashed #ddd;
  border-radius: 8px;
}
</style>
