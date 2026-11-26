import axios from "axios";
import { ElMessage } from "element-plus";

const TOKEN_KEY = "portal_token";
const REFRESH_TOKEN_KEY = "portal_refresh_token";
const USER_KEY = "portal_user";

const http = axios.create({
  baseURL: "/api/v1",
  timeout: 15000,
});

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && body.code !== undefined) {
      if (body.code === "00000") {
        return body.data;
      }
      // silent：后台轮询类请求（如购物车角标）失败时不打扰用户
      if (!response.config?.silent) {
        ElMessage.error(body.msg || "请求失败");
      }
      return Promise.reject(new Error(body.msg || "请求失败"));
    }
    return body;
  },
  (error) => {
    const data = error.response?.data;
    if (error.response?.status === 401) {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(REFRESH_TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
      if (!window.location.hash.startsWith("#/login")) {
        window.location.hash = "#/login";
      }
      ElMessage.error(data?.msg || "登录已过期，请重新登录");
    } else {
      if (!error.config?.silent) {
        ElMessage.error(data?.msg || error.message || "网络异常，请稍后重试");
      }
    }
    return Promise.reject(error);
  }
);

export { TOKEN_KEY, REFRESH_TOKEN_KEY, USER_KEY };
export default http;
