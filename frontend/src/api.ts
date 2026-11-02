import axios from 'axios'; import {ElMessage} from 'element-plus';
export const api=axios.create({baseURL:'/api',timeout:10000});
api.interceptors.request.use(c=>{const t=localStorage.getItem('token');if(t)c.headers.Authorization=`Bearer ${t}`;return c});
api.interceptors.response.use(r=>r.data,e=>{const message=e.response?.data?.message||'请求失败，请检查服务是否启动';ElMessage.error(message);if(e.response?.status===401&&location.pathname!='/login'){localStorage.removeItem('token');location.href='/login'}return Promise.reject(e)});
export type Order={id:number;studentId:number;building:string;room:string;category:string;description:string;urgency:string;status:string;technicianId:number|null;result:string|null;createdAt:string};
export type Profile={id:number;username:string;role:string};
