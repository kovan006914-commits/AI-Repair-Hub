import {defineStore} from 'pinia';import {api,type Profile} from './api';
export const useSession=defineStore('session',{state:()=>({user:null as Profile|null}),actions:{async load(){this.user=(await api.get('/users/me') as any).data},logout(){localStorage.removeItem('token');this.user=null;location.href='/login'}}});
