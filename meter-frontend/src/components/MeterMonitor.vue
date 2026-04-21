<template>
  <div class="monitor-wrapper animated-fade-up">
    <div class="top-bar premium-shadow">
      <div class="stats-group">
        <div class="stat-item border-blue">
          <span class="stat-label">接入总数</span>
          <span class="stat-value">{{ totalCount }}</span>
        </div>
        <div class="stat-item border-red">
          <span class="stat-label">当前异常</span>
          <span class="stat-value">{{ abnormalCount }}</span>
        </div>
        <div class="stat-item border-yellow">
          <span class="stat-label">设备离线</span>
          <span class="stat-value">{{ offlineCount }}</span>
        </div>
      </div>

      <div class="filter-group">
        <button class="filter-btn" :class="{ 'active-blue': currentFilter === 'all' }" @click="currentFilter = 'all'">全部设备</button>
        <button class="filter-btn" :class="{ 'active-text': currentFilter === 'abnormal' }" @click="currentFilter = 'abnormal'">
          <span class="dot dot-red"></span> 仅看异常
        </button>
        <button class="filter-btn" :class="{ 'active-text': currentFilter === 'offline' }" @click="currentFilter = 'offline'">
          <span class="dot dot-yellow"></span> 仅看离线
        </button>
      </div>
    </div>

    <div class="grid-container premium-shadow">
      <div class="scroll-list custom-scrollbar">
        <div v-if="filteredList.length === 0" class="empty-tip">📭 暂无符合条件的设备数据</div>

        <div v-else class="meter-grid">
          <article v-for="item in filteredList" :key="item.id" class="meter-card">
            <div class="card-head">
              <span class="meter-id">{{ item.id }}</span>
              <span v-if="item.status === 1" class="status-tag tag-normal">正常</span>
              <span v-else-if="item.status === 2" class="status-tag tag-abnormal">异常</span>
              <span v-else class="status-tag tag-offline">离线</span>
            </div>

            <div class="card-line">
              <span class="line-label">所属辖区</span>
              <span class="line-value">📍 {{ item.region || '未知辖区' }}</span>
            </div>

            <div class="card-kpi-row">
              <div class="kpi-cell">
                <div class="kpi-label">当前负荷</div>
                <div class="kpi-value">{{ item.currentLoad || 0 }} <span>kW</span></div>
              </div>
              <div class="kpi-cell">
                <div class="kpi-label">日累计用电</div>
                <div class="kpi-value energy">{{ item.dailyUsage || 0 }} <span>kWh</span></div>
              </div>
            </div>

            <div v-if="item.status === 2" class="card-action">
              <button
                class="workorder-btn"
                :disabled="creatingMeterId === item.id || hasCreated(item.id)"
                @click="createWorkOrder(item.id)"
              >
                {{ hasCreated(item.id) ? '已建单' : (creatingMeterId === item.id ? '建单中...' : '生成工单') }}
              </button>
            </div>
          </article>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../utils/request'

const meterList = ref([])
const currentFilter = ref('all') 
const creatingMeterId = ref('')
const createdMeters = ref(new Set())

const totalCount = computed(() => meterList.value.length)
const abnormalCount = computed(() => meterList.value.filter(item => item.status === 2).length)
const offlineCount = computed(() => meterList.value.filter(item => item.status === 3).length)

const filteredList = computed(() => {
  if (currentFilter.value === 'abnormal') return meterList.value.filter(item => item.status === 2)
  if (currentFilter.value === 'offline') return meterList.value.filter(item => item.status === 3)
  return meterList.value
})

const fetchMonitorData = async () => {
  try {
    const res = await api.get('/api/meter/monitor-list')
    if (res.data) meterList.value = res.data
  } catch (error) {
    console.error("获取设备监控列表失败:", error)
  }
}

const hasCreated = (meterId) => createdMeters.value.has(meterId)

const createWorkOrder = async (meterId) => {
  if (!meterId || hasCreated(meterId)) return
  creatingMeterId.value = meterId
  try {
    const res = await api.post('/api/alert/work-order/by-meter', {
      meterId,
      reason: '设备状态监控页手动建单',
      source: 'monitor'
    })
    const data = res.data || {}
    if (data.code === 200 || data.code === 409) {
      createdMeters.value = new Set([...createdMeters.value, meterId])
      alert(data.msg || '工单创建成功')
    } else {
      alert(data.msg || '工单创建失败')
    }
  } catch (error) {
    const msg = error?.response?.data?.msg || error?.message || '请求失败'
    alert(`工单创建失败：${msg}`)
  } finally {
    creatingMeterId.value = ''
  }
}

onMounted(() => { fetchMonitorData() })
</script>

<style scoped>
.monitor-wrapper { display: flex; flex-direction: column; gap: 24px; height: 100%; width: 100%; }
.top-bar { display: flex; justify-content: space-between; align-items: center; background: #fff; border-radius: 20px; padding: 20px 32px; }
.stats-group { display: flex; gap: 40px; }
.stat-item { display: flex; flex-direction: column; gap: 8px; padding-left: 16px; position: relative; }
.stat-item::before { content: ''; position: absolute; left: 0; top: 2px; bottom: 2px; width: 4px; border-radius: 4px; }
.border-blue::before { background: #3b82f6; }
.border-red::before { background: #ef4444; }
.border-yellow::before { background: #f59e0b; }
.stat-label { font-size: 14px; color: #64748b; font-weight: 600; }
.stat-value { font-size: 28px; font-weight: 800; color: #0f172a; line-height: 1; }
.filter-group { display: flex; gap: 12px; align-items: center; background: #f8fafc; padding: 6px; border-radius: 99px; }
.filter-btn { border: none; background: transparent; padding: 10px 20px; border-radius: 99px; font-size: 14px; font-weight: 700; color: #64748b; cursor: pointer; transition: all 0.3s; display: flex; align-items: center; gap: 8px; }
.filter-btn:hover { color: #0f172a; }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot-red { background: #ef4444; box-shadow: 0 0 8px rgba(239, 68, 68, 0.5); }
.dot-yellow { background: #f59e0b; box-shadow: 0 0 8px rgba(245, 158, 11, 0.5); }
.active-blue { background: #3b82f6; color: #fff !important; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3); }
.active-text { background: #fff; color: #0f172a !important; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); }
.grid-container { flex: 1; background: #fff; border-radius: 20px; padding: 24px; display: flex; flex-direction: column; overflow: hidden; }
.scroll-list { flex: 1; overflow-y: auto; padding-right: 8px; }
.meter-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 14px; }
.meter-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  transition: all 0.2s;
}
.meter-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08); border-color: #bfdbfe; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.meter-id { font-family: monospace; font-size: 15px; font-weight: 700; color: #1e293b; }
.card-line { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 12px; }
.line-label { color: #64748b; font-size: 13px; }
.line-value { color: #334155; font-weight: 600; text-align: right; font-size: 13px; }
.card-kpi-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.kpi-cell { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 10px; }
.kpi-label { color: #64748b; font-size: 12px; margin-bottom: 6px; }
.kpi-value { color: #0f172a; font-size: 18px; font-weight: 800; letter-spacing: -0.3px; }
.kpi-value.energy { color: #2563eb; }
.kpi-value span { font-size: 12px; color: #64748b; font-weight: 600; margin-left: 2px; }
.card-action { margin-top: 12px; display: flex; justify-content: flex-end; }
.workorder-btn {
  border: none;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-radius: 8px;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.workorder-btn:disabled { background: #94a3b8; cursor: not-allowed; }
.status-tag { padding: 6px 14px; border-radius: 99px; font-size: 13px; font-weight: 800; }
.tag-normal { background: #dcfce3; color: #16a34a; }
.tag-abnormal { background: #fee2e2; color: #dc2626; }
.tag-offline { background: #fef3c7; color: #d97706; }
.empty-tip { text-align: center; padding: 60px 0; color: #94a3b8; font-size: 16px; font-weight: 600; }
.premium-shadow { box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08); border: 1px solid rgba(226, 232, 240, 0.65); }
.animated-fade-up { animation: fadeInUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #94a3b8; }

@media (max-width: 900px) {
  .top-bar { flex-direction: column; align-items: flex-start; gap: 14px; }
  .stats-group { gap: 24px; flex-wrap: wrap; }
}
</style>