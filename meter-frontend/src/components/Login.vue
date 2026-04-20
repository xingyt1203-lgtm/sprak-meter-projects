<template>
  <div class="login-container">
    <div class="glass-panel">
      <h2>{{ isRegister ? '创建新账号' : '系统登录' }}</h2>
      <form @submit.prevent="handleSubmit" class="login-form">
        
        <div v-if="isRegister" class="input-wrapper">
          <input type="text" v-model="realName" placeholder="真实姓名 (例: 张三)" required />
        </div>
        
        <div class="input-wrapper">
          <input type="text" v-model="username" placeholder="管理员账号" required />
        </div>
        
        <div class="input-wrapper">
          <input type="password" v-model="password" placeholder="系统密码" required />
        </div>
        
        <button type="submit" class="login-btn" :disabled="isLoading">
          {{ isLoading ? '处理中...' : (isRegister ? '立 即 注 册' : '登 录 系 统') }}
        </button>
        
        <p class="toggle-text" @click="isRegister = !isRegister">
          {{ isRegister ? '已有账号？点击去登录' : '没有账号？点击去注册' }}
        </p>

      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue"
import api from "../utils/request"

const emit = defineEmits(["login-success"])

// 状态变量
const isRegister = ref(false) // 默认是登录状态
const username = ref("")
const password = ref("")
const realName = ref("")
const isLoading = ref(false)

const handleSubmit = async () => {
  isLoading.value = true
  try {
    if (isRegister.value) {
      // ========= 走注册流程 =========
      await api.post("/auth/register", { 
        username: username.value, 
        password: password.value, 
        realName: realName.value 
      })
      alert("✅ 注册成功！请使用新账号登录。")
      // 注册成功后，自动清空密码并切回登录页
      password.value = ""
      isRegister.value = false 
    } else {
      // ========= 走登录流程 =========
      const res = await api.post("/auth/login", { 
        username: username.value, 
        password: password.value 
      })
      if (res.data && res.data.token) {
        localStorage.setItem("token", res.data.token)
        localStorage.setItem("user", res.data.user || username.value)
        emit("login-success")
      }
    }
  } catch (error) {
    // 错误提示
    if (error.response && error.response.data) {
      alert("❌ 失败: " + JSON.stringify(error.response.data))
    } else {
      alert("⚠️ 请求失败，请检查后端服务是否启动。")
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-container { min-height: 100vh; display: flex; justify-content: center; align-items: center; background: radial-gradient(circle at top right, #3b82f6 0%, #1e293b 40%, #0f172a 100%); }
.glass-panel { background: rgba(30, 41, 59, 0.7); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); padding: 48px; border-radius: 20px; border: 1px solid rgba(255,255,255,0.1); width: 100%; max-width: 420px; text-align: center; color: white; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7); transform: translateY(0); animation: floatUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
@keyframes floatUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }
h2 { margin-bottom: 30px; font-size: 28px; font-weight: 800; letter-spacing: 0.5px; background: linear-gradient(to right, #60a5fa, #a78bfa); -webkit-background-clip: text; -webkit-text-fill-color: transparent;}
.input-wrapper { margin-bottom: 20px; }
input { width: 100%; padding: 16px; background: rgba(15, 23, 42, 0.5); border: 1px solid rgba(255,255,255,0.15); border-radius: 12px; color: white; box-sizing: border-box; font-size: 15px; outline: none; transition: 0.3s; }
input:focus { border-color: #3b82f6; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.2); background: rgba(15, 23, 42, 0.8); }
.login-btn { width: 100%; padding: 16px; margin-top: 10px; background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); color: white; border: none; border-radius: 12px; cursor: pointer; font-weight: 800; font-size: 16px; letter-spacing: 1px; transition: 0.3s; box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4); }
.login-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(59, 130, 246, 0.6); }
.login-btn:disabled { background: #475569; box-shadow: none; cursor: not-allowed; }
.toggle-text { margin-top: 24px; color: #94a3b8; cursor: pointer; font-size: 14px; transition: 0.3s; font-weight: 500; }
.toggle-text:hover { color: #60a5fa; }
</style>