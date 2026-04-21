<template>
  <div v-if="!isAuthenticated" class="login-wrapper">
    <Login @login-success="onLoginSuccess" />
  </div>

  <div v-else class="system-layout">
    
    <aside class="sidebar">
      <div class="logo-box">
        <div class="logo-icon-bg">
          <span class="logo-icon">⚡</span>
        </div>
        <h2>智能电表分析</h2>
      </div>
      
      <ul class="nav-menu">
        <li @click="currentMenu = 'total'" :class="{ active: currentMenu === 'total' }">
          <span class="menu-icon">📊</span> 总体数据大屏
        </li>
        <li @click="currentMenu = 'monitor'" :class="{ active: currentMenu === 'monitor' }">
          <span class="menu-icon">🖥️</span> 设备状态监控
        </li>
        <li @click="currentMenu = 'load'" :class="{ active: currentMenu === 'load' }">
          <span class="menu-icon">📈</span> 用电负荷分析
        </li>
        <li @click="currentMenu = 'cluster'" :class="{ active: currentMenu === 'cluster' }">
          <span class="menu-icon">🧬</span> 聚类与异常监控
        </li>
        <li @click="currentMenu = 'individual'" :class="{ active: currentMenu === 'individual' }">
          <span class="menu-icon">👤</span> 个体画像检索
        </li>
      </ul>
      
      <div class="logout-container">
        <button class="logout-btn" @click="handleLogout">
          🚪 退出系统
        </button>
      </div>
    </aside>

    <main class="main-content">
      
      <div v-if="currentMenu === 'total'" class="content-wrapper animated-fade-up" style="flex-direction: column;">
        <div class="stat-cards">
          <div class="card card-blue">
            <div class="card-icon">🔌</div>
            <div class="card-info">
              <p>累计接入电表</p>
              <h3>{{ cardTotalMeters }} <span class="unit">台</span></h3>
            </div>
          </div>
          <div class="card card-green">
            <div class="card-icon" style="background: #ecfdf5; color: #10b981;">⚡</div>
            <div class="card-info">
              <p>今日全网总负荷</p>
              <h3>{{ cardTotalLoad }} <span class="unit">kWh</span></h3>
            </div>
          </div>
          <div class="card card-red">
            <div class="card-icon" style="background: #fef2f2; color: #ef4444;">🚨</div>
            <div class="card-info">
              <p>今日发现窃电嫌疑</p>
              <h3 style="color: #ef4444;">{{ cardAnomalyCount }} <span class="unit">户</span></h3>
            </div>
          </div>
          <div class="card card-teal">
            <div class="card-icon" style="background: #f0fdfa; color: #14b8a6;">⏱️</div>
            <div class="card-info">
              <p>Spark 计算耗时</p>
              <h3 style="color: #14b8a6;">{{ cardSparkTime }} <span class="unit">s</span></h3>
            </div>
          </div>
        </div>

        <div style="display: flex; gap: 24px; min-height: 350px; margin-bottom: 24px;">
          <div class="chart-box premium-shadow" style="flex: 2;">
            <div class="box-header">
              <h3>📍 各辖区用电负荷排行</h3>
              <span class="badge">实时计算</span>
            </div>
            <div id="region-chart" style="width: 100%; height: 280px;"></div>
          </div>
          <div class="chart-box premium-shadow" style="flex: 1;">
            <div class="box-header">
              <h3>📡 终端设备在线率</h3>
            </div>
            <div id="device-chart" style="width: 100%; height: 280px;"></div>
          </div>
        </div>

        <div class="chart-box premium-shadow map-container" style="width: 100%; min-height: 550px;">
          <div class="box-header map-header">
            <h3>🗺️ 北京市各辖区负荷实时热力分布</h3>
            <span class="badge" style="background: rgba(67, 56, 202, 0.2); color: #818cf8; border: 1px solid #4338ca;">GIS 动态监控</span>
          </div>
          <div id="map-chart" style="width: 100%; height: 480px;"></div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'monitor'" class="content-wrapper animated-fade-up">
        <MeterMonitor style="width: 100%;" />
      </div>

      <div v-else-if="currentMenu === 'load'" class="content-wrapper animated-fade-up" style="flex-direction: column;">
        <div class="stat-cards">
          <div class="card kpi-card">
            <div class="kpi-title">最大负荷 (Peak Load)</div>
            <div class="kpi-value highlight-red">{{ computedMaxLoad }} <span class="unit">kW</span></div>
          </div>
          <div class="card kpi-card">
            <div class="kpi-title">平均负荷 (Avg Load)</div>
            <div class="kpi-value highlight-blue">{{ computedAvgLoad }} <span class="unit">kW</span></div>
          </div>
          <div class="card kpi-card">
            <div class="kpi-title">日负荷率 (Load Factor)</div>
            <div class="kpi-value highlight-green">{{ computedLoadRate }} <span class="unit">%</span></div>
          </div>
        </div>
        <div class="chart-box premium-shadow" style="flex: 1;">
          <div class="box-header" style="margin-bottom: 12px;">
            <h3>🔄 AI 预测重算控制台</h3>
            <button class="sync-btn" :disabled="isSyncingForecast" @click="handleManualForecastSync">
              {{ isSyncingForecast ? '重算中...' : '立即重算预测' }}
            </button>
          </div>
          <div id="line-chart" style="width: 100%; height: 600px;"></div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'cluster'" class="content-wrapper animated-fade-up">
        <div class="chart-box premium-shadow" style="flex: 4;">
          <div class="box-header">
            <h3>📊 K-Means 用电画像分布</h3>
          </div>
          <div id="pie-chart" style="width: 100%; height: 500px;"></div>
        </div>
        <div class="table-box premium-shadow" style="flex: 5;">
          <div class="box-header" style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
            <h3>🚨 3-Sigma 异常行为嫌疑名单</h3>
            <span class="badge badge-danger">⚡ {{ anomalyData.length }} 条高危预警</span>
            <span class="badge" :class="wsConnected ? 'badge-online' : 'badge-offline'">
              {{ wsConnected ? '预警通道在线' : '预警通道断开' }}
            </span>
            <button class="export-btn" @click="exportToCSV">📥 导出 CSV 报表</button>
          </div>

          <div v-if="liveAlert" class="live-alert-strip">
            <div>
              <strong>实时预警：</strong>
              终端 {{ liveAlert.meterId }} 在 {{ liveAlert.detectDate }} 出现异常，请前往“设备状态监控”页面生成工单。
            </div>
          </div>
          
          <div class="scroll-table custom-scrollbar">
            <table class="anomaly-table">
              <thead>
                <tr>
                  <th>终端编号</th>
                  <th>诊断日期</th>
                  <th>日耗电(kWh)</th>
                  <th>历史均值(kWh)</th>
                  <th>Z-Score</th>
                  <th>智能研判</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in anomalyData" :key="`${item.meter_id}@${item.detect_date}`">
                  <td class="cell-id">{{ item.meter_id }}</td>
                  <td>{{ item.detect_date }}</td>
                  <td>{{ item.daily_usage }}</td>
                  <td>{{ item.avg_usage }}</td>
                  <td>{{ item.z_score }}</td>
                  <td>
                    <span class="tag" :class="item.daily_usage > item.avg_usage ? 'tag-up' : 'tag-down'">
                      {{ item.daily_usage > item.avg_usage ? '激增' : '骤降' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div v-else-if="currentMenu === 'individual'" class="content-wrapper animated-fade-up">
        <div class="chart-box premium-shadow" style="flex: 1; flex-direction: column;">
          <div class="box-header" style="justify-content: center; margin-bottom: 30px; border-bottom: none;">
            <h3 style="font-size: 24px; color: #0f172a; font-weight: 800;">🔍 个体用电画像与异常追踪系统</h3>
          </div>
          
          <div class="search-bar-container">
            <div class="search-input-wrapper">
              <span class="search-icon">🔎</span>
              <input type="text" v-model="searchMeterId" placeholder="请输入终端编号 (例如: METER_024)..." @keyup.enter="handleSearch" />
            </div>
            <button class="search-btn" @click="handleSearch">全网检索</button>
          </div>
          
          <div v-if="showProfile" class="profile-dashboard animated-fade-up">
            <div class="profile-sidebar">
              <div class="profile-avatar">
                <div class="avatar-circle">👤</div>
                <h4 style="font-size: 24px;">{{ currentUserName }}</h4>
                <div style="color: #64748b; font-size: 14px; margin-bottom: 12px; font-family: monospace;">{{ displayedMeterId }}</div>
                <span class="status-dot" :class="isAnomalyUser ? 'status-dot-danger' : ''">
                  {{ isAnomalyUser ? '异常管控中' : '正常接入' }}
                </span>
              </div>
              
              <div class="profile-info-group">
                <p class="info-label">📍 安装地址</p>
                <div class="info-value" style="font-size: 14px; font-weight: 600; padding: 4px 0;">{{ currentAddress }}</div>
              </div>
              <div class="profile-info-group">
                <p class="info-label">📟 电表类型</p>
                <div class="info-value" style="font-size: 14px; font-weight: 600; padding: 4px 0;">{{ currentMeterType }}</div>
              </div>
              
              <hr style="border: none; border-top: 1px dashed #cbd5e1; margin: 10px 0;">
              
              <div class="profile-info-group">
                <p class="info-label">K-Means 画像定性</p>
                <div class="info-value label-blue">{{ currentClusterLabel }}</div>
              </div>
              <div class="profile-info-group">
                <p class="info-label">近期安全体检</p>
                <div class="info-value" :class="isAnomalyUser ? 'label-red' : 'label-green'">
                  {{ currentAnomalyLabel }}
                </div>
              </div>
              
              <div v-if="isAnomalyUser" class="warning-box">
                <strong>⚠️ 稽查预警系统：</strong><br>
                发现该户存在异常耗电，已触发二级预警，请安排网格员现场核实。
              </div>
            </div>
            
            <div class="profile-chart-area">
              <div id="bar-chart" style="width: 100%; height: 100%; min-height: 400px;"></div>
            </div>
          </div>
          
          <div v-else class="empty-state">
            <div class="empty-icon">💡</div>
            <h2>等待检索指令</h2>
            <p>请输入终端编号以调取其近 30 天用电脉冲及多维分析画像</p>
          </div>
        </div>
      </div>

    </main>
  </div>
</template>

<script setup>
import { ref, nextTick, watch, onMounted, onUnmounted, computed } from 'vue'
import api from './utils/request'
import axios from 'axios'
import * as echarts from 'echarts'
import Login from './components/Login.vue' 
import MeterMonitor from './components/MeterMonitor.vue'

// ==================== 系统全局状态 ====================
const isAuthenticated = ref(!!localStorage.getItem('token'))
const currentMenu = ref('total')
const chartsMap = new Map()

// ==================== 数据状态定义 ====================
const anomalyData = ref([])
const searchMeterId = ref('')
const displayedMeterId = ref('')
const showProfile = ref(false) 
const currentClusterLabel = ref('')
const currentAnomalyLabel = ref('')
const isAnomalyUser = ref(false) 
const currentUserName = ref('')
const currentAddress = ref('')
const currentMeterType = ref('')

const cardTotalMeters = ref(0)
const cardTotalLoad = ref(0)
const cardAnomalyCount = ref(0)
const cardSparkTime = ref(0)
const isSyncingForecast = ref(false)
const wsConnected = ref(false)
const liveAlert = ref(null)
const alertSocket = ref(null)

const loadDataRaw = ref([])
const computedMaxLoad = computed(() => loadDataRaw.value.length ? Math.max(...loadDataRaw.value).toFixed(1) : 0)
const computedAvgLoad = computed(() => loadDataRaw.value.length ? (loadDataRaw.value.reduce((a, b) => a + b, 0) / loadDataRaw.value.length).toFixed(1) : 0)
const computedLoadRate = computed(() => computedMaxLoad.value > 0 ? ((computedAvgLoad.value / computedMaxLoad.value) * 100).toFixed(1) : 0)

// ==================== 登录与登出逻辑 ====================
const onLoginSuccess = async () => {
  isAuthenticated.value = true
  currentMenu.value = 'total'
  await nextTick() 
  renderTotalCharts()
  connectAlertSocket()
}

const handleLogout = () => {
  if (confirm("系统提示：确认要安全退出大屏分析中心吗？")) {
    if (alertSocket.value) {
      alertSocket.value.close()
      alertSocket.value = null
    }
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    isAuthenticated.value = false
    showProfile.value = false
  }
}

// ==================== 图表通用工具 ====================
const initChart = (domId) => {
  const dom = document.getElementById(domId)
  if (!dom) return null
  let chart = echarts.getInstanceByDom(dom)
  if (chart) chart.dispose()
  chart = echarts.init(dom)
  chartsMap.set(domId, chart)
  return chart
}

const premiumTooltip = {
  backgroundColor: 'rgba(255, 255, 255, 0.9)',
  borderColor: '#e2e8f0', borderWidth: 1, padding: [12, 16],
  textStyle: { color: '#1e293b', fontSize: 14, fontFamily: 'PingFang SC' },
  extraCssText: 'box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1); border-radius: 8px; backdrop-filter: blur(8px);'
}

// ==================== 核心功能：纯前端导出 CSV ====================
const exportToCSV = () => {
  if (anomalyData.value.length === 0) return alert('当前没有异常数据可供导出！')
  
  // 1. 定义中文表头
  let csvContent = "终端编号,诊断日期,日均耗电(kWh),历史平均(kWh),智能研判结果\n"
  
  // 2. 拼接数据行
  anomalyData.value.forEach(item => {
    let result = item.daily_usage > item.avg_usage ? '激增异常' : '骤降异常'
    csvContent += `${item.meter_id},${item.detect_date},${item.daily_usage},${item.avg_usage},${result}\n`
  })

  // 3. 构造 Blob 并通过浏览器隐藏 a 标签触发下载 (加上 BOM 头防止中文乱码)
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement("a")
  link.href = URL.createObjectURL(blob)
  link.download = `异常排查工单报表_${new Date().toISOString().slice(0,10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// ==================== 菜单渲染逻辑：1. 总体数据大屏 ====================
const renderTotalCharts = async () => {
  try {
    const res = await api.get('/api/system/dashboard')
    const data = res.data
    cardTotalMeters.value = data.totalMeters || 0
    cardTotalLoad.value = data.totalLoad || 0
    cardAnomalyCount.value = data.anomalyCount || 0
    cardSparkTime.value = data.sparkTime || 0

    // 1. 辖区负荷排行柱状图
    const regionChart = initChart('region-chart')
    if (regionChart) {
      regionChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...premiumTooltip },
        grid: { left: '3%', right: '4%', bottom: '5%', top: '15%', containLabel: true },
        xAxis: { type: 'category', data: data.regions || [], axisLine: { lineStyle: { color: '#cbd5e1' } }, axisLabel: { color: '#64748b' } },
        yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } }, axisLabel: { color: '#64748b' } },
        series: [{
          data: data.regionLoads || [], type: 'bar', barWidth: '30%', 
          showBackground: true, backgroundStyle: { color: '#f8fafc', borderRadius: [6, 6, 0, 0] },
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#60a5fa' }, { offset: 1, color: '#3b82f6' }]),
            borderRadius: [6, 6, 0, 0]
          }
        }]
      })
    }

    // 2. 终端设备在线率饼图
    const deviceChart = initChart('device-chart')
    if (deviceChart) {
      deviceChart.setOption({
        tooltip: { trigger: 'item', ...premiumTooltip },
        legend: { bottom: '0%', icon: 'circle', textStyle: { color: '#64748b' }, itemGap: 20 },
        color: ['#10b981', '#f59e0b', '#ef4444'], 
        series: [{
          type: 'pie', radius: ['55%', '75%'], center: ['50%', '42%'], 
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
          label: { show: false }, 
          data: data.devices || [] 
        }]
      })
    }

    // 3. 北京市热力分布大地图
    const mapChartDom = document.getElementById('map-chart')
    if (mapChartDom) {
      // 解决地图跨域问题：直接从前端 public 文件夹请求 JSON
      const mapGeoJson = await axios.get('/beijing.json').catch(() => null)
      if (mapGeoJson && mapGeoJson.data) {
        echarts.registerMap('Beijing', mapGeoJson.data)
        const mapChart = initChart('map-chart')

        const regionNames = Array.isArray(data.regions) ? data.regions : []
        const regionLoads = Array.isArray(data.regionLoads) ? data.regionLoads : []
        const mapData = regionNames.map((name, idx) => {
          const n = String(name || '')
          const fullName = n.endsWith('区') ? n : `${n}区`
          return { name: fullName, value: Number(regionLoads[idx] ?? 0) }
        })
        const maxValue = mapData.length ? Math.max(...mapData.map(i => i.value || 0)) : 1000

        mapChart.setOption({
          tooltip: {
            trigger: 'item', ...premiumTooltip, backgroundColor: 'rgba(15, 23, 42, 0.9)', textStyle: { color: '#f8fafc' },
            formatter: (params) => `${params.name}<br/>实时负荷: <b>${params.value ?? 0}</b> MW`
          },
          visualMap: {
            min: 0, max: maxValue || 1000, left: 20, bottom: 20, calculable: true, orient: 'horizontal',
            textStyle: { color: '#cbd5e1' }, inRange: { color: ['#1e293b', '#1d4ed8', '#3b82f6', '#93c5fd'] }
          },
          series: [{
            name: '区域负荷', type: 'map', map: 'Beijing', roam: true, zoom: 1.1,
            label: { show: true, color: '#e2e8f0', fontSize: 11 },
            itemStyle: { borderColor: '#3b82f6', borderWidth: 1 },
            emphasis: { label: { color: '#ffffff' }, itemStyle: { areaColor: '#2563eb' } },
            data: mapData
          }]
        })
      }
    }
  } catch (error) { console.error("加载大屏数据失败:", error) }
}

// ==================== 菜单渲染逻辑：3. 用电负荷分析 ====================
const renderLineChart = async () => {
  try {
    const res = await api.get('/api/system/load')
    const data = res.data
    loadDataRaw.value = data.loads || []
    const myChart = initChart('line-chart')
    if (myChart) {
      myChart.setOption({
        title: { 
          text: '全网用电负荷趋势与 AI 预测', 
          subtext: 'Powered by PyTorch LSTM Neural Network', 
          left: '2%', top: '2%', 
          textStyle: { color: '#0f172a', fontSize: 20, fontWeight: 800 },
          subtextStyle: { color: '#8b5cf6', fontWeight: 'bold' } 
        },
        legend: { data: ['今日实际负荷', '明日 LSTM 预测'], top: '2%', right: '5%', icon: 'roundRect', textStyle: { color: '#64748b' } },
        tooltip: { trigger: 'axis', ...premiumTooltip },
        grid: { left: '3%', right: '4%', bottom: '5%', top: '20%', containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: data.hours || [], axisLabel: { color: '#64748b' }, axisLine: { lineStyle: { color: '#cbd5e1' } } },
        yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } }, axisLabel: { color: '#64748b' } },
        series: [
          {
            name: '今日实际负荷', type: 'line', smooth: true, showSymbol: false,
            lineStyle: { width: 4, color: '#3b82f6', shadowColor: 'rgba(59, 130, 246, 0.3)', shadowBlur: 8 },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(59, 130, 246, 0.2)' }, { offset: 1, color: 'rgba(59, 130, 246, 0.01)' }]) },
            data: data.loads || []
          },
          {
            name: '明日 LSTM 预测', type: 'line', smooth: true, showSymbol: false,
            lineStyle: { width: 3, type: 'dashed', color: '#f59e0b', shadowColor: 'rgba(245, 158, 11, 0.3)', shadowBlur: 8 },
            data: data.forecasts || []
          }
        ]
      })
    }
  } catch (error) { console.error("加载负荷数据失败:", error) }
}

const handleManualForecastSync = async () => {
  if (isSyncingForecast.value) return
  isSyncingForecast.value = true
  try {
    const res = await api.post('/api/system/forecast/sync?retrain=true', null, { timeout: 180000 })
    const data = res.data || {}
    if (data.code === 200) {
      await renderLineChart()
      alert(`预测同步成功，本次写入 ${data.rows ?? 0} 条记录。`)
    } else {
      alert(`预测同步失败：${data.msg || '未知错误'}`)
    }
  } catch (error) {
    const msg = error?.response?.data?.msg || error?.message || '请求失败'
    alert(`预测同步失败：${msg}`)
  } finally {
    isSyncingForecast.value = false
  }
}

const connectAlertSocket = () => {
  if (!isAuthenticated.value) return
  if (alertSocket.value) {
    alertSocket.value.close()
  }
  const socket = new WebSocket('ws://localhost:8080/ws/anomaly-alert')
  alertSocket.value = socket

  socket.onopen = () => {
    wsConnected.value = true
  }

  socket.onmessage = (evt) => {
    try {
      const payload = JSON.parse(evt.data || '{}')
      if (payload.type === 'ANOMALY_ALERT') {
        liveAlert.value = payload
        const exists = anomalyData.value.some(
          i => i.meter_id === payload.meterId && i.detect_date === payload.detectDate
        )
        if (!exists) {
          anomalyData.value.unshift({
            meter_id: payload.meterId,
            detect_date: payload.detectDate,
            daily_usage: payload.dailyUsage,
            avg_usage: payload.avgUsage,
            z_score: payload.zScore
          })
        }
      }
    } catch (e) {
      console.error('解析实时预警失败', e)
    }
  }

  socket.onclose = () => {
    wsConnected.value = false
    if (isAuthenticated.value) {
      setTimeout(() => connectAlertSocket(), 3000)
    }
  }

  socket.onerror = () => {
    wsConnected.value = false
  }
}

// ==================== 菜单渲染逻辑：4. 聚类与异常监控 ====================
const renderClusterCharts = async () => {
  try {
    const resTable = await api.get('/api/cluster/anomaly')
    anomalyData.value = resTable.data || []
    const resDist = await api.get('/api/cluster/distribution')
    const myChart = initChart('pie-chart')
    if (myChart) {
      myChart.setOption({
        tooltip: { trigger: 'item', ...premiumTooltip },
        legend: { bottom: '5%', icon: 'roundRect', textStyle: { color: '#64748b' }, itemGap: 20 },
        color: ['#8b5cf6', '#ec4899', '#f59e0b', '#3b82f6'],
        series: [{
          type: 'pie', radius: ['45%', '70%'], center: ['50%', '42%'], 
          itemStyle: { borderRadius: 12, borderColor: '#fff', borderWidth: 4, shadowColor: 'rgba(0,0,0,0.05)', shadowBlur: 10 },
          label: { show: false },
          data: resDist.data || []
        }]
      })
    }
  } catch (error) { console.error("加载聚类数据失败:", error) }
}

// ==================== 菜单渲染逻辑：5. 个体画像检索 ====================
const handleSearch = async () => {
  if (!searchMeterId.value) return alert('请输入需要检索的终端编号！')
  try {
    const res = await api.get(`/api/meter/detail?id=${searchMeterId.value}`)
    const realData = res.data || {}
    
    displayedMeterId.value = searchMeterId.value
    currentUserName.value = realData.userName || '未知'
    currentAddress.value = realData.address || '未知地址'
    currentMeterType.value = realData.meterType || '普通智能表'
    currentClusterLabel.value = realData.clusterLabel || '暂无画像数据'
    currentAnomalyLabel.value = realData.anomalyLabel || '状态正常'
    isAnomalyUser.value = !!realData.isAnomaly
    
    showProfile.value = true
    await nextTick()
    
    setTimeout(() => {
      const myChart = initChart('bar-chart')
      if (myChart) {
        myChart.setOption({
          title: { text: '近 30 天耗电走势 (kWh)', textStyle: { fontSize: 16, color: '#0f172a', fontWeight: 700 }, padding: [10, 0, 20, 10] },
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...premiumTooltip },
          grid: { left: '2%', right: '4%', bottom: '5%', top: '20%', containLabel: true },
          xAxis: { type: 'category', data: realData.dates || [], axisLine: { lineStyle: { color: '#cbd5e1' } }, axisLabel: { color: '#64748b' } },
          yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } }, axisLabel: { color: '#64748b' } },
          series: [{
            data: realData.usages || [], type: 'bar', barWidth: '40%',
            itemStyle: {
              color: (params) => params.value < 20 ? '#fca5a5' : new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#818cf8' }, { offset: 1, color: '#4f46e5' }]),
              borderRadius: [4, 4, 0, 0]
            }
          }]
        })
      }
    }, 150)
  } catch (error) { 
    console.error("检索个体数据失败:", error)
  }
}

// ==================== 路由与监听 ====================
onMounted(async () => {
  if (isAuthenticated.value) {
    await nextTick()
    renderTotalCharts()
    connectAlertSocket()
  }
  // 监听窗口大小变化，图表自动重新渲染防重叠
  window.addEventListener('resize', () => {
    chartsMap.forEach(chart => { if (chart) chart.resize() })
  })
})

onUnmounted(() => {
  if (alertSocket.value) {
    alertSocket.value.close()
    alertSocket.value = null
  }
})

watch(currentMenu, async (newMenu) => {
  await nextTick()
  if (newMenu === 'total') renderTotalCharts()
  else if (newMenu === 'load') renderLineChart()
  else if (newMenu === 'cluster') renderClusterCharts()
  else if (newMenu === 'individual' && showProfile.value) handleSearch()
})
</script>

<style scoped>
/* ================= 全局与重置 ================= */
:global(html), :global(body), :global(#app) {
  margin: 0; padding: 0; width: 100vw; min-height: 100vh; overflow: hidden; box-sizing: border-box;
}
:global(*) {
  box-sizing: border-box;
}
:global(body) {
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: radial-gradient(circle at top, rgba(59,130,246,0.15), transparent 28%), #eef4ff;
  color: #0f172a;
}

/* 渐入动画 */
.animated-fade-up { animation: fadeInUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 自定义优美滚动条 */
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #94a3b8; }

/* ================= 主布局 ================= */
.login-wrapper {
  display: flex; justify-content: center; align-items: center; min-height: 100vh;
  background: linear-gradient(180deg, #e2e8ff 0%, #eff6ff 100%);
}
.system-layout {
  display: flex; height: 100vh; width: 100vw; overflow: hidden;
}

/* ================= 侧边栏 ================= */
.sidebar {
  width: 280px; min-width: 280px; height: 100vh;
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%); 
  color: #fff; display: flex; flex-direction: column;
  box-shadow: 4px 0 34px rgba(0,0,0,0.06); z-index: 10;
}
.logo-box {
  padding: 32px 24px; display: flex; align-items: center; gap: 16px; margin-bottom: 10px;
}
.logo-icon-bg {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); padding: 10px; 
  border-radius: 12px; display: flex; align-items: center; justify-content: center; 
  box-shadow: 0 4px 12px rgba(59,130,246,0.3);
}
.logo-icon { font-size: 20px; color: #fff; } 
.logo-box h2 {
  margin: 0; font-size: 20px; font-weight: 800; color: #f8fafc; letter-spacing: 0.5px;
}

.nav-menu {
  list-style: none; padding: 0 16px; margin: 0; flex-grow: 1; display: flex; flex-direction: column; gap: 6px;
}
.nav-menu li { 
  padding: 16px 20px; font-size: 15px; cursor: pointer; 
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); border-radius: 12px; color: #94a3b8;
  display: flex; align-items: center; gap: 14px; font-weight: 600;
}
.nav-menu li:hover {
  color: #fff; background: rgba(255,255,255,0.06); transform: translateX(4px);
}
.menu-icon { font-size: 20px; }
.nav-menu li.active { 
  background: linear-gradient(90deg, #3b82f6, #2563eb); 
  color: #fff; box-shadow: 0 8px 20px -4px rgba(59, 130, 246, 0.4);
}

.logout-container { padding: 24px; }
.logout-btn { 
  width: 100%; padding: 14px; background: rgba(255,255,255,0.05); color: #cbd5e1; 
  border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; font-size: 15px; 
  cursor: pointer; transition: all 0.3s; font-weight: 600;
}
.logout-btn:hover {
  background: #ef4444; color: white; border-color: #ef4444; 
  box-shadow: 0 8px 16px -4px rgba(239, 68, 68, 0.3);
}

/* ================= 右侧主内容区 ================= */
.main-content {
  flex: 1; height: 100vh; padding: 32px 40px; overflow-y: auto; background: #f4f7fe; 
}
.content-wrapper { display: flex; gap: 24px; min-height: calc(100vh - 64px); }

/* 通用白板盒子模型 */
.chart-box, .card, .table-box {
  background: #ffffff; border-radius: 20px; padding: 24px;
}
.premium-shadow {
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.05); border: 1px solid rgba(226, 232, 240, 0.6);
  display: flex; flex-direction: column; position: relative; transition: box-shadow 0.3s ease;
}
.premium-shadow:hover { box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.08); }

/* ================= 顶部 KPI 数据卡片 ================= */
.stat-cards { display: flex; gap: 24px; margin-bottom: 24px; width: 100%;}
.card { 
  flex: 1; display: flex; align-items: center; gap: 20px; min-width: 200px;
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.05); border: 1px solid rgba(226, 232, 240, 0.6); 
  position: relative; overflow: hidden; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.card:hover { transform: translateY(-4px); box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.1); }
.card::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 4px; }
.card-blue::before { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
.card-green::before { background: linear-gradient(90deg, #10b981, #34d399); }
.card-red::before { background: linear-gradient(90deg, #ef4444, #f87171); }
.card-teal::before { background: linear-gradient(90deg, #14b8a6, #2dd4bf); }

.card-icon {
  width: 68px; height: 68px; border-radius: 18px; background: #eff6ff; 
  color: #3b82f6; font-size: 30px; display: flex; justify-content: center; align-items: center;
}
.card-info p { margin: 0 0 8px 0; color: #64748b; font-size: 14px; font-weight: 600;}
.card-info h3 { margin: 0; color: #0f172a; font-size: 30px; font-weight: 800; letter-spacing: -0.5px;}
.unit { font-size: 15px; color: #94a3b8; font-weight: 600; margin-left: 4px;}

/* 负荷分析专用大数字卡片 */
.kpi-card { flex-direction: column; justify-content: center; align-items: flex-start; gap: 12px; padding: 28px 24px; }
.kpi-card::before { width: 6px; height: 100%; border-radius: 6px 0 0 6px;}
.kpi-card:nth-child(1)::before { background: #ef4444; }
.kpi-card:nth-child(2)::before { background: #3b82f6; }
.kpi-card:nth-child(3)::before { background: #10b981; }
.kpi-title { font-size: 16px; color: #64748b; font-weight: 700; padding-left: 12px;}
.kpi-value { font-size: 40px; font-weight: 900; padding-left: 12px; letter-spacing: -1px;}
.highlight-red { color: #ef4444; } .highlight-blue { color: #3b82f6; } .highlight-green { color: #10b981; }

/* ================= 盒子 Header 样式 ================= */
.box-header {
  display: flex; justify-content: space-between; align-items: center; 
  margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}
.box-header h3 { margin: 0; color: #0f172a; font-size: 18px; font-weight: 800; }
.badge { font-size: 12px; padding: 6px 12px; background: #f1f5f9; color: #475569; border-radius: 20px; font-weight: 700;}
.badge-danger { background: #fef2f2; color: #ef4444; border: 1px solid #fecaca;}
.badge-online { background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; }
.badge-offline { background: #fff7ed; color: #c2410c; border: 1px solid #fed7aa; }

/* Map 专用深色 Header 适配 */
.map-container { background: #0f172a; border: none; box-shadow: 0 20px 40px rgba(0,0,0,0.1); }
.map-header h3 { color: #f8fafc; }
.map-header { border-bottom: 1px solid rgba(255,255,255,0.1); }

/* ================= 导出按钮样式 ================= */
.export-btn {
  background: #10b981; color: white; border: none; padding: 8px 16px; 
  border-radius: 8px; font-weight: bold; cursor: pointer; font-size: 13px; 
  transition: all 0.3s; margin-left: 10px; box-shadow: 0 4px 6px rgba(16, 185, 129, 0.2);
}
.export-btn:hover { background: #059669; transform: translateY(-2px); box-shadow: 0 6px 12px rgba(16, 185, 129, 0.3); }

.sync-btn {
  background: #3b82f6;
  color: #fff;
  border: none;
  padding: 8px 14px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3);
}

.sync-btn:hover:not(:disabled) {
  background: #2563eb;
  transform: translateY(-1px);
}

.sync-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
  box-shadow: none;
}

/* ================= 表格美化 ================= */
.scroll-table { flex-grow: 1; overflow-y: auto; padding-right: 8px; }
.live-alert-strip {
  margin-bottom: 12px;
  border-radius: 12px;
  background: #fff7ed;
  border: 1px solid #fdba74;
  color: #9a3412;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
}
.anomaly-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 14px;
  color: #334155;
}
.anomaly-table thead th {
  position: sticky;
  top: 0;
  z-index: 2;
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
  text-align: left;
  padding: 12px 14px;
  border-bottom: 1px solid #e2e8f0;
}
.anomaly-table tbody td {
  padding: 12px 14px;
  border-bottom: 1px solid #f1f5f9;
}
.anomaly-table tbody tr:hover {
  background: #f8fafc;
}
.cell-id {
  font-family: monospace;
  font-weight: 700;
  color: #0f172a;
}
.tag {
  padding: 8px 14px; border-radius: 8px; font-size: 13px; font-weight: 800; 
  display: inline-flex; align-items: center; justify-content: center; min-width: 100px;
}
.tag-down { background: #fef9c3; color: #a16207; }
.tag-up { background: #fee2e2; color: #b91c1c; }
.workorder-btn {
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.workorder-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

/* ================= 检索系统 ================= */
.search-bar-container { display: flex; justify-content: center; gap: 16px; margin-bottom: 40px; }
.search-input-wrapper { position: relative; width: 480px; }
.search-icon { position: absolute; left: 20px; top: 50%; transform: translateY(-50%); font-size: 20px; color: #94a3b8; }
.search-input-wrapper input {
  width: 100%; padding: 18px 24px 18px 54px; border: 2px solid #e2e8f0; border-radius: 16px; 
  font-size: 16px; background: #f8fafc; outline: none; transition: all 0.3s; color: #0f172a; font-weight: 500;
}
.search-input-wrapper input:focus { border-color: #3b82f6; background: #fff; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15); }
.search-btn {
  padding: 0 36px; background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); 
  color: #fff; border: none; border-radius: 16px; font-size: 16px; font-weight: 700; 
  cursor: pointer; box-shadow: 0 8px 16px -4px rgba(37, 99, 235, 0.3); transition: all 0.3s;
}
.search-btn:hover { transform: translateY(-2px); box-shadow: 0 12px 20px -4px rgba(37, 99, 235, 0.4); }

/* ================= 检索画像卡片 ================= */
.profile-dashboard { display: flex; gap: 24px; min-height: 480px; }
.profile-sidebar {
  width: 320px; background: #f8fafc; border-radius: 20px; padding: 32px; 
  border: 1px solid rgba(226, 232, 240, 0.8); display: flex; flex-direction: column; 
  gap: 24px; box-shadow: inset 0 2px 4px rgba(255,255,255,0.5);
}
.profile-avatar { text-align: center; padding-bottom: 24px; border-bottom: 1px dashed #cbd5e1; }
.avatar-circle {
  width: 80px; height: 80px; background: linear-gradient(135deg, #e0e7ff, #c7d2fe); 
  border-radius: 50%; margin: 0 auto 16px; display: flex; justify-content: center; align-items: center; 
  font-size: 36px; border: 4px solid #fff; box-shadow: 0 8px 16px rgba(0,0,0,0.05);
}
.profile-avatar h4 { margin: 0 0 10px 0; font-size: 22px; color: #0f172a; font-weight: 800;}
.status-dot { font-size: 13px; padding: 6px 14px; background: #dcfce3; color: #166534; border-radius: 20px; font-weight: 700; border: 1px solid #bbf7d0;}
.status-dot-danger { background: #fee2e2; color: #b91c1c; border-color: #fecaca; }

.profile-info-group { display: flex; flex-direction: column; gap: 10px; }
.info-label { margin: 0; font-size: 14px; color: #64748b; font-weight: 700;}
.info-value { padding: 10px 16px; border-radius: 10px; font-size: 15px; font-weight: 800; width: fit-content; background: #fff; border: 1px solid #e2e8f0; }
.label-blue { background: #eff6ff; color: #2563eb; border-color: #bfdbfe; }
.label-green { background: #f0fdf4; color: #16a34a; border-color: #bbf7d0; }
.label-red { background: #fef2f2; color: #dc2626; border-color: #fecaca; }

.warning-box {
  margin-top: auto; background: #fff1f2; border: 1px solid #fecdd3; padding: 20px; 
  border-radius: 16px; font-size: 14px; color: #be123c; line-height: 1.6; font-weight: 500; 
  box-shadow: 0 4px 12px rgba(225, 29, 72, 0.1);
}
.profile-chart-area { flex: 1; background: #fff; border-radius: 20px; border: 1px solid rgba(226, 232, 240, 0.8); padding: 24px; }

/* 检索前的空状态 */
.empty-state { text-align: center; padding: 120px 0; color: #94a3b8; }
.empty-icon { font-size: 70px; margin-bottom: 24px; opacity: 0.8; filter: grayscale(100%); transition: all 0.3s;}
.empty-state:hover .empty-icon { filter: grayscale(0%); transform: scale(1.1);}
.empty-state h2 { color: #334155; font-weight: 800; margin-bottom: 12px;}
</style>