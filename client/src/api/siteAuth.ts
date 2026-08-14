import axios from 'axios'
import { attachColdStartIndicator } from '../stores/coldStart'

const client = axios.create({ baseURL: '/api' })
attachColdStartIndicator(client)

export interface SiteAuthResponse {
  email: string
}

export interface SiteAuthConfigResponse {
  googleClientId: string
}

export function fetchSiteAuthConfig(): Promise<SiteAuthConfigResponse> {
  return client.get<SiteAuthConfigResponse>('/auth/config').then((res) => res.data)
}

export function fetchCurrentSiteAuth(): Promise<SiteAuthResponse> {
  return client.get<SiteAuthResponse>('/auth/me').then((res) => res.data)
}

export function loginWithGoogle(idToken: string): Promise<SiteAuthResponse> {
  return client.post<SiteAuthResponse>('/auth/google', { idToken }).then((res) => res.data)
}

export function logoutSiteAuth(): Promise<void> {
  return client.post('/auth/logout').then(() => undefined)
}
