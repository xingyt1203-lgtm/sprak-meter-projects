import axios from "axios"

const service = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 10000
})

// Axios 请求拦截器
service.interceptors.request.use(
  config => {
    // 每次发送请求时，自动从 localStorage 中读取 token
    const token = localStorage.getItem("spark-meter-token")
    if (token) {
      // 并拼接成 Bearer ${token} 放入请求头的 Authorization 中
      config.headers["Authorization"] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Axios 响应拦截器
service.interceptors.response.use(
  response => {
    return response
  },
  error => {
    // 🚨防坑警告：拦截 HTTP 401 错误时，严禁使用 window.location.reload() 以防止白屏死循环！
    if (error.response && error.response.status === 401) {
      // 温和地清除 localStorage 中的鉴权数据
      localStorage.removeItem("spark-meter-token")
      localStorage.removeItem("spark-meter-user")
      
      // 提示用户重新登录
      alert("登录已过期，请重新登录")
      
      // 将状态设置为未登录
      window.location.href = "/"
    }
    return Promise.reject(error)
  }
)

export default service