import axios from "axios"

const service = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 10000
})

// Axios 请求拦截器
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem("token")
    if (token) {
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
    if (error.response && error.response.status === 401) {
      // 如果是登录接口报 401，不要弹“登录过期”，让 Login.vue 自己处理
      if (error.config.url.includes("/auth/login")) {
        return Promise.reject(error)
      }

      localStorage.removeItem("token")
      localStorage.removeItem("user")
      alert("登录已过期，请重新登录")
      window.location.href = "/"
    }
    return Promise.reject(error)
  }
)

export default service