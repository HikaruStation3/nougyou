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
  const token = localStorage.getItem(TOKEN_KEY);
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
      ElMessage.error(body.msg || "请求失败");
      return Promise.reject(new Error(body.msg || "请求失败"));
    }
    return body;
  },
  (error) => {
    const data = error.response?.data;
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      if (!window.location.hash.startsWith("#/login")) {
        window.location.hash = "#/login";
      }
      ElMessage.error(data?.msg || "登录已过期，请重新登录");
    } else {
      ElMessage.error(data?.msg || error.message || "网络异常，请稍后重试");
    }
    return Promise.reject(error);
  }
);

export { TOKEN_KEY, REFRESH_TOKEN_KEY, USER_KEY };
export default http;
