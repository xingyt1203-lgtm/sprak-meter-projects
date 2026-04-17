<template>
  <div v-if="!isAuthenticated" class="login-wrapper">
    <Login @login-success="onLoginSuccess" />
  </div>

  <div v-else class="system-layout">
    <div class="sidebar">
      <div class="logo-box">
        <div class="logo-icon-bg">
          <span class="logo-icon">⚡</span>
        </div>
        <h2>智能电表分析</h2>
      </div>
      <ul class="nav-menu">
        <li @click="currentMenu = 'total'" :class="{ active: currentMenu === 'total' }">📊 总体数据大屏</li>
        <li @click="currentMenu = 'monitor'" :class="{ active: currentMenu === 'monitor' }">🖥️ 设备状态监控</li>
        <li @click="currentMenu = 'load'" :class="{ active: currentMenu === 'load' }">📈 用电负荷分析</li>
        <li @click="currentMenu = 'cluster'" :class="{ active: currentMenu === 'cluster' }">🧬 聚类与异常监控</li>
        <li @click="currentMenu = 'individual'" :class="{ active: currentMenu === 'individual' }">👤 个体画像检索</li>
      </ul>
      <div class="logout-container">
        <button class="logout-btn" @click="handleLogout">🚪 退出系统</button>
      </div>
    </div>
    <div class="main-content">
      <div v-if="currentMenu === 'total'" class="content-wrapper">
        <div class="stat-cards">
          <div class="card card-blue"><h3>{{ cardTotalMeters }}</h3><p>电表总数</p></div>
          <div class="card card-green"><h3>{{ cardTotalLoad }}</h3><p>今日负荷</p></div>
          <div class="card card-red"><h3>{{ cardAnomalyCount }}</h3><p>异常预警</p></div>
        </div>
        <div id="region-chart" style="height:300px; margin-top: 24px;"></div>
      </div>
      <div v-else-if="currentMenu === 'monitor'"><MeterMonitor /></div>
      <div v-else-if="currentMenu === 'load'"><div id="line-chart" style="height:450px;"></div></div>
      <div v-else-if="currentMenu === 'cluster'"><div id="pie-chart" style="height:350px;"></div></div>
      <div v-else-if="currentMenu === 'individual'">
        <input v-model="searchMeterId" @keyup.enter="handleSearch" /><button @click="handleSearch">搜索</button>
        <div v-if="showProfile"><h4>{{ currentUserName }}</h4><p>{{ currentAddress }}</p></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import request from './utils/request'
import Login from './components/Login.vue'
import MeterMonitor from './components/MeterMonitor.vue'

const isAuthenticated = ref(!!localStorage.getItem('token'))
const currentMenu = ref('total')
const searchMeterId = ref('')
const showProfile = ref(false)
const cardTotalMeters = ref(0)
const cardTotalLoad = ref(0)
const cardAnomalyCount = ref(0)
const currentUserName = ref('')
const currentAddress = ref('')
const chartsMap = new Map()

const onLoginSuccess = () => { isAuthenticated.value = true; nextTick(() => renderTotalCharts()); }
const handleLogout = () => { localStorage.removeItem('token'); isAuthenticated.value = false; }

const renderTotalCharts = async () => {
  const res = await request.get('/api/system/dashboard')
  cardTotalMeters.value = res.data.totalMeters
  cardTotalLoad.value = res.data.totalLoad
  cardAnomalyCount.value = res.data.anomalyCount
}

onMounted(() => { if (isAuthenticated.value) renderTotalCharts(); })
</script>

<style scoped>
.system-layout { display: flex; height: 100vh; }
.sidebar { width: 280px; background: #1e293b; color: white; display: flex; flex-direction: column; }
.logo-box { padding: 32px 24px; }
.nav-menu { list-style: none; padding: 0 16px; flex: 1; }
.nav-menu li { padding: 16px; cursor: pointer; border-radius: 8px; margin-bottom: 4px; }
.nav-menu li.active { background: #3b82f6; }
.main-content { flex: 1; padding: 32px; overflow-y: auto; background: #f4f7fe; }
.stat-cards { display: flex; gap: 24px; }
.card { flex: 1; padding: 24px; background: white; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
.card-blue { border-top: 4px solid #3b82f6; }
.card-green { border-top: 4px solid #10b981; }
.card-red { border-top: 4px solid #ef4444; }
</style>