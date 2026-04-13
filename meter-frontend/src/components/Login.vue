<template>
  <div class="login-container">
    <div class="glass-panel">
      <div class="brand">
        <div class="icon-pulse">⚡</div>
        <h2>智能电表行为分析系统</h2>
        <p class="subtitle">基于 Apache Spark 的分布式流计算平台</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="input-wrapper">
          <span class="input-icon">👤</span>
          <input type="text" v-model="username" placeholder="管理员账号 " required />
        </div>
        
        <div class="input-wrapper">
          <span class="input-icon">🔒</span>
          <input type="password" v-model="password" placeholder="系统密码 " required />
        </div>

        <button type="submit" class="login-btn" :disabled="isLoggingIn">
          <span v-if="!isLoggingIn">登 录 系 统</span>
          <span v-else class="loading-text">正在初始化 Spark 节点...</span>
        </button>
      </form>

      <div class="footer-text">
        <p>数据接入状态: <span class="status-green">正常</span> | 集群节点: <span class="status-green">4 Active</span></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['login-success'])

const username = ref('')
const password = ref('')
const isLoggingIn = ref(false)

const handleLogin = () => {
  // 模拟一个高大上的加载过程
  isLoggingIn.value = true
  
  setTimeout(() => {
    // 简单的本地验证演示
    if (username.value.trim() === 'admin' && password.value.trim() === '123456') {
      emit('login-success') // 告诉大屏：密码正确，开门！
    } else {
      alert('⚠️ 账号或密码错误，请重新输入！')
      isLoggingIn.value = false
    }
  }, 1500) // 假装后台在验证 1.5 秒
}
</script>

<style scoped>
/* 充满科技感的暗黑玻璃态背景 */
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #0f172a;
  background-image: 
    radial-gradient(at 0% 0%, hsla(253,16%,7%,1) 0, transparent 50%), 
    radial-gradient(at 50% 0%, hsla(225,39%,30%,1) 0, transparent 50%), 
    radial-gradient(at 100% 0%, hsla(339,49%,30%,1) 0, transparent 50%);
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.glass-panel {
  background: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 50px 40px;
  border-radius: 20px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  width: 100%;
  max-width: 420px;
  text-align: center;
}

.icon-pulse {
  font-size: 48px;
  margin-bottom: 10px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.1); opacity: 1; text-shadow: 0 0 20px #60a5fa; }
  100% { transform: scale(1); opacity: 0.8; }
}

h2 { color: #f8fafc; font-size: 24px; margin-bottom: 5px; font-weight: 600; }
.subtitle { color: #94a3b8; font-size: 14px; margin-bottom: 30px; letter-spacing: 1px; }

.input-wrapper {
  position: relative;
  margin-bottom: 20px;
}

.input-icon {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
}

input {
  width: 100%;
  padding: 14px 14px 14px 45px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid #334155;
  border-radius: 10px;
  color: #f8fafc;
  font-size: 15px;
  box-sizing: border-box;
  transition: all 0.3s ease;
}

input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 15px rgba(59, 130, 246, 0.3);
}

.login-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  margin-top: 10px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -10px #3b82f6;
}

.login-btn:disabled { background: #475569; cursor: not-allowed; }
.loading-text { animation: blink 1.5s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

.footer-text { margin-top: 30px; font-size: 12px; color: #64748b; }
.status-green { color: #10b981; font-weight: bold; }
</style>