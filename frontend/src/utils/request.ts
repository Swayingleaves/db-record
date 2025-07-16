import axios, { type AxiosResponse } from 'axios';

// 定义后端统一返回格式
interface ApiResult<T = any> {
  status: boolean;
  code: number;
  msg: string;
  data: T;
}

const instance = axios.create({
  baseURL: '/',
  timeout: 10000,
});

// 请求拦截器
instance.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
}, err => Promise.reject(err));

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const { data } = response;
    
    // 检查业务状态
    if (data.status === false) {
      // 业务失败，但HTTP状态码可能是200
      console.error('业务错误:', data.msg);
      
      // 如果是认证相关错误，清除token并跳转登录
      if (data.code === 401 || data.code === 403) {
        localStorage.removeItem('token');
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
      
      // 返回错误，让调用方处理
      return Promise.reject(new Error(data.msg || '请求失败'));
    }
    
    // 业务成功，返回完整的响应数据
    return response;
  },
  err => {
    // HTTP错误处理
    if (err.response) {
      const { status, data } = err.response;
      
      if (status === 401) {
      localStorage.removeItem('token');
        if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
        return Promise.reject(new Error('未授权，请重新登录'));
      }
      
      if (status === 403) {
        return Promise.reject(new Error('权限不足'));
      }
      
      if (status === 500) {
        return Promise.reject(new Error('服务器内部错误'));
      }
      
      // 如果后端返回了错误信息
      if (data && data.msg) {
        return Promise.reject(new Error(data.msg));
      }
    }
    
    // 网络错误或其他错误
    if (err.code === 'ECONNABORTED') {
      return Promise.reject(new Error('请求超时，请重试'));
    }
    
    return Promise.reject(new Error(err.message || '网络错误'));
  }
);

export default instance; 
export type { ApiResult }; 