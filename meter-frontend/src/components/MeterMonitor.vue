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
        <button v-for="f in filters" :key="f.value" :class="['filter-btn', { active: currentFilter === f.value }]" @click="currentFilter = f.value">
          {{ f.label }}
        </button>
      </div>
    </div>
    <div class="meter-grid">
      <div v-for="meter in filteredMeters" :key="meter.id" class="meter-card" :class="meter.status">
        <div class="card-header">
          <h3 class="meter-id">📟 {{ meter.id }}</h3>
          <div class="status-indicator"><span class="dot"></span>{{ statusText(meter.status) }}</div>
        </div>
        <div class="card-body">
          <div class="info-row"><span class="info-label">所属区域:</span><span class="info-val">{{ meter.region }}</span></div>
          <div class="info-row"><span class="info-label">当前负荷:</span><span class="info-val highlight" :class="meter.status">{{ meter.currentLoad }} MW</span></div>
          <div class="info-row"><span class="info-label">今日用电:</span><span class="info-val">{{ meter.dailyUsage }} kWh</span></div>
        </div>
        <div class="card-footer" v-if="meter.status === 'abnormal'">
          <button class="action-btn" :class="{ dispatched: meter.dispatched }" @click="dispatchWorkOrder(meter)" :disabled="meter.dispatched">
            {{ meter.dispatched ? '✅ 已生成排查工单' : '🚨 生成排查工单' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '../utils/request'
const meters = ref([])
const currentFilter = ref('all')
const filters = [
  { label: '全部设备', value: 'all' },
  { label: '🔴 仅看异常', value: 'abnormal' },
  { label: '🟡 仅看离线', value: 'offline' }
]
const filteredMeters = computed(() => {
  if (currentFilter.value === 'all') return meters.value
  return meters.value.filter(m => m.status === currentFilter.value)
})
const abnormalCount = computed(() => meters.value.filter(m => m.status === 'abnormal').length)
const offlineCount = computed(() => meters.value.filter(m => m.status === 'offline').length)
const statusText = (status) => {
  const map = { normal: '正常运行', abnormal: '疑似窃电/过载', offline: '设备离线' }
  return map[status] || '未知状态'
}
const dispatchWorkOrder = (meter) => {
  meter.dispatched = true
  alert(`[工单系统] 成功！已将电表 ${meter.id} 的排查任务下发给运维团队。`)
}
const fetchRealMeterData = async () => {
  try {
    const res = await request.get('/api/meter/monitor-list')
    meters.value = res.data.map(item => {
      let statusClass = 'normal'
      if (item.status == 2) statusClass = 'abnormal'
      else if (item.status == 3) statusClass = 'offline'
      return { id: item.id, region: item.region, currentLoad: item.currentLoad, dailyUsage: item.dailyUsage, status: statusClass, dispatched: false }
    })
  } catch (error) { console.error(error); }
}
onMounted(() => { fetchRealMeterData(); })
</script>
<style scoped>
.monitor-panel { padding: 20px; color: #333; }
.panel-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px; }
.stats-box { display: flex; gap: 30px; }
.stat-item { display: flex; flex-direction: column; background: #f8f9fa; padding: 15px 25px; border-radius: 8px; border-left: 4px solid #3b82f6; }
.stat-item.warning { border-left-color: #ef4444; }
.stat-item.offline { border-left-color: #f59e0b; }
.stat-item .label { font-size: 14px; color: #64748b; margin-bottom: 5px; }
.stat-item .value { font-size: 28px; font-weight: bold; color: #1e293b; }
.filter-box { display: flex; gap: 10px; }
.filter-btn { padding: 8px 16px; border: 1px solid #e2e8f0; background: white; border-radius: 20px; cursor: pointer; transition: all 0.3s; }
.filter-btn.active { background: #3b82f6; color: white; border-color: #3b82f6; }
.meter-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.meter-card { background: white; border-radius: 10px; padding: 20px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); border: 1px solid #f1f5f9; transition: all 0.3s ease; }
.meter-card.normal .dot { background: #10b981; }
.meter-card.offline .dot { background: #f59e0b; }
.meter-card.abnormal { border: 1px solid #ef4444; background: #fffafb; animation: breatheRed 2s infinite alternate; }
.meter-card.abnormal .dot { background: #ef4444; box-shadow: 0 0 8px #ef4444; }
@keyframes breatheRed { 0% { box-shadow: 0 0 5px rgba(239, 68, 68, 0.1); } 100% { box-shadow: 0 0 15px rgba(239, 68, 68, 0.3); } }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px dashed #e2e8f0; }
.meter-id { margin: 0; font-size: 18px; color: #1e293b; }
.status-indicator { font-size: 13px; display: flex; align-items: center; gap: 6px; color: #64748b; }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.info-row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px; }
.info-label { color: #64748b; }
.info-val { font-weight: 500; color: #1e293b; }
.info-val.highlight.abnormal { color: #ef4444; font-weight: bold; }
.card-footer { margin-top: 15px; }
.action-btn { width: 100%; padding: 10px; border: none; background: #ef4444; color: white; border-radius: 6px; cursor: pointer; font-weight: bold; transition: 0.3s; }
.action-btn:hover { background: #dc2626; }
.action-btn.dispatched { background: #f1f5f9; color: #94a3b8; cursor: not-allowed; border: 1px solid #e2e8f0; }
</style>