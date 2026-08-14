import axios from 'axios'
import { attachSiteAuthInterceptor } from '../stores/siteAuth'
import { attachColdStartIndicator } from '../stores/coldStart'

export const http = axios.create({
  baseURL: '/api',
})

attachSiteAuthInterceptor(http)
attachColdStartIndicator(http)
