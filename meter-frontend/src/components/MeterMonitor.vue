<template>
  <div class="monitor-panel">
    <div class="panel-header">
      <div class="stats-box">
        <div class="stat-item total">
          <span class="label">接入总数</span>
          <span class="value">{{ meters.length }}</span>
        </div>
        <div class="stat-item warning">
          <span class="label">当前异常</span>
          <span class="value">{{ abnormalCount }}</span>
        </div>
        <div class="stat-item offline">
          <span class="label">设备离线</span>
          <span class="value">{{ offlineCount }}</span>
        </div>
      </div>

      <div class="filter-box">
        <button
          v-for="f in filters"
          :key="f.value"
          :class="['filter-btn', { active: currentFilter === f.value }]"
          @click="currentFilter = f.value"
        >
          {{ f.label }}
        </button>
      </div>
    </div>

    <div class="meter-grid">
      <div
        v-for="meter in filteredMeters"
        :key="meter.id"
        class="meter-card"
        :class="meter.status"
      >
        <div class="card-header">
          <h3 class="meter-id">⚡ {{ meter.id }}</h3>
          <div class="status-indicator">
            <span class="dot"></span>
            {{ statusText(meter.status) }}
          </div>
        </div>

        <div class="card-body">
          <div class="info-row">
            <span class="info-label">所属区域:</span>
            <span class="info-val">{{ meter.region }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">当前负荷:</span>
            <span class="info-val highlight" :class="meter.status">{{ meter.currentLoad }} MW</span>
          </div>
          <div class="info-row">
            <span class="info-label">今日用电:</span>
            <span class="info-val">{{ meter.dailyUsage }} kWh</span>
          </div>
        </div>

        <div class="card-footer" v-if="meter.status === 'abnormal'">
          <button
            class="action-btn"
            :class="{ dispatched: meter.dispatched }"
            @click="dispatchWorkOrder(meter)"
            :disabled="meter.dispatched"
          >
            {{ meter.dispatched ? '✅ 已生成排查工单' : '🚨 生成排查工单' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

// --- 状态变量 ---
const meters = ref([])
const currentFilter = ref('all')

const filters = [
  { label: '全部设备', value: 'all' },
  { label: '🔴 仅看异常', value: 'abnormal' },
  { label: '🟡 仅看离线', value: 'offline' }
]

// --- 统计与过滤逻辑 ---
const filteredMeters = computed(() => {
  if (currentFilter.value === 'all') return meters.value
  return meters.value.filter(m => m.status === currentFilter.value)
})

const abnormalCount = computed(() => meters.value.filter(m => m.status === 'abnormal').length)
const offlineCount = computed(() => meters.value.filter(m => m.status === 'offline').length)

const statusText = (status) => {
  const map = { normal: '正常运行', abnormal: '疑似窃电/过载', offline: '设备离线' }
  return map[status]
}

const dispatchWorkOrder = (meter) => {
  meter.dispatched = true
  alert(`[工单系统] 成功！已将电表 ${meter.id} 的排查任务下发给运维团队。`)
}

// --- 🌟 核心：唯一的数据来源通道，只跟 Java 要数据 ---
const fetchRealMeterData = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/meter/monitor-list')
    
    // 把 Java 传过来的数据库真实数据，塞给前端的 meters 数组
    meters.value = res.data.map(item => {
      // 将数据库里的状态码 (1,2,3) 翻译成前端的颜色样式
      let statusClass = 'normal'
      if (item.status == 2) statusClass = 'abnormal'
      else if (item.status == 3) statusClass = 'offline'

      return {
        id: item.id,
        region: item.region,
        currentLoad: item.currentLoad,
        dailyUsage: item.dailyUsage,
        status: statusClass,
        dispatched: false 
      }
    })
    console.log("✅ 成功拉取数据库真实电表数据:", meters.value)
  } catch (error) {
    console.error("❌ 数据拉取失败，请检查 Java 后端是否启动！", error)
  }
}

// --- 🌟 页面加载时，绝对只执行这一个函数！ ---
onMounted(() => {
  fetchRealMeterData()
})
</script>
// --- 顶部统计逻辑 ---
const abno

<style scoped>
/* 容器基础样式 */
.monitor-panel {
  padding: 20px;
  color: #333;
}

/* 顶部统计区 */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}
.stats-box {
  display: flex;
  gap: 30px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  padding: 15px 25px;
  border-radius: 8px;
  border-left: 4px solid #1890ff;
}
.stat-item.warning { border-left-color: #ff4d4f; }
.stat-item.offline { border-left-color: #faad14; }
.stat-item .label { font-size: 14px; color: #666; margin-bottom: 5px; }
.stat-item .value { font-size: 28px; font-weight: bold; color: #111; }

/* 筛选按钮 */
.filter-box {
  display: flex;
  gap: 10px;
}
.filter-btn {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
}
.filter-btn.active {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

/* 电表卡片网格布局 */
.meter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

/* 卡片通用样式 */
.meter-card {
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  border: 1px solid #eee;
  transition: all 0.3s ease;
}
.meter-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.1);
}

/* 各种状态的卡片 UI */
.meter-card.normal .dot { background: #52c41a; }
.meter-card.offline .dot { background: #faad14; }
.meter-card.offline { opacity: 0.7; }

/* 🚨 核心特效：异常电表呼吸灯 */
.meter-card.abnormal {
  border: 1px solid #ff4d4f;
  background: #fffafa;
  animation: breatheRed 2s infinite alternate;
}
.meter-card.abnormal .dot { background: #ff4d4f; box-shadow: 0 0 8px #ff4d4f; }
@keyframes breatheRed {
  0% { box-shadow: 0 0 5px rgba(255, 77, 79, 0.1); }
  100% { box-shadow: 0 0 20px rgba(255, 77, 79, 0.4), inset 0 0 10px rgba(255, 77, 79, 0.05); }
}

/* 卡片内部排版 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eee;
}
.meter-id { margin: 0; font-size: 18px; color: #222; }
.status-indicator { font-size: 13px; display: flex; align-items: center; gap: 6px; }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}
.info-label { color: #888; }
.info-val { font-weight: 500; color: #333; }
.info-val.highlight.abnormal { color: #ff4d4f; font-weight: bold; font-size: 16px; }

/* 动作按钮 */
.card-footer { margin-top: 15px; }
.action-btn {
  width: 100%;
  padding: 10px;
  border: none;
  background: #ff4d4f;
  color: white;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.3s;
}
.action-btn:hover { background: #d9363e; }
.action-btn.dispatched {
  background: #f5f5f5;
  color: #999;
  cursor: not-allowed;
  border: 1px solid #d9d9d9;
}
</style>