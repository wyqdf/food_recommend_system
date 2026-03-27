import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/api",
  timeout: 15000,
});

const publicApis = [
  '/recipes',
  '/categories',
  '/tastes',
  '/techniques',
  '/time-costs',
  '/difficulties',
  '/ingredients',
  '/cookwares',
  '/search',
  '/analytics',
  '/scenes',
];

const isPublicApi = (url) => {
  if (!url) return false;
  if (url.startsWith('/admin')) return false;
  if (url.startsWith('/users/profile')) return false;
  if (url.startsWith('/favorites')) return false;
  if (url.startsWith('/comments') && !url.includes('/recipe/')) return false;
  return publicApis.some(api => url.startsWith(api));
};

const createBusinessError = (response, config) => {
  const message = response?.message || "请求失败";
  const error = new Error(message);
  error.name = "BusinessError";
  error.responseCode = Number(response?.code || 500);
  error.businessResponse = response;
  error.config = config;
  return error;
};

const handleUnauthorized = (url, message, silentError) => {
  const isAdminRoute = url.startsWith("/admin");
  const resolvedMessage = message || "登录已过期，请重新登录";

  if (isAdminRoute) {
    localStorage.removeItem("admin_token");
    localStorage.removeItem("admin_info");
    if (window.location.pathname !== '/admin/login') {
      if (!silentError) {
        ElMessage.warning(resolvedMessage);
      }
      setTimeout(() => {
        window.location.href = "/admin/login";
      }, 500);
    }
    return;
  }

  localStorage.removeItem("token");
  localStorage.removeItem("user_info");
  if (!isPublicApi(url) && window.location.pathname !== '/login') {
    if (!silentError) {
      ElMessage.warning(resolvedMessage);
    }
    setTimeout(() => {
      window.location.href = "/login";
    }, 500);
  }
};

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    const adminToken = localStorage.getItem("admin_token");

    if (config.url?.startsWith("/admin")) {
      if (adminToken) {
        config.headers.Authorization = `Bearer ${adminToken}`;
      }
    } else if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200) {
      const url = response.config?.url || '';
      const silentError = Boolean(response.config?.silentError);
      const businessError = createBusinessError(res, response.config);

      if (businessError.responseCode === 401) {
        handleUnauthorized(url, res.message, silentError);
      } else if (!silentError) {
        ElMessage.error(res.message || "请求失败");
      }
      return Promise.reject(businessError);
    }
    return res;
  },
  (error) => {
    const url = error.config?.url || '';
    const silentError = Boolean(error.config?.silentError);
    const errorMessage = error.response?.data?.message || error.message || "网络错误";
    const resolvedMessage = error.code === 'ECONNABORTED'
      ? "请求超时，请稍后重试"
      : errorMessage;
    
    if (error.response?.status === 401) {
      handleUnauthorized(url, resolvedMessage, silentError);
    } else if (!silentError) {
      ElMessage.error(resolvedMessage);
    }
    return Promise.reject(error);
  },
);

export default request;
