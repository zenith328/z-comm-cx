import axios from 'axios'
import { attachSiteAuthInterceptor } from '../stores/siteAuth'

export const http = axios.create({
  baseURL: '/api',
})

attachSiteAuthInterceptor(http)
