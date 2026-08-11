import { http } from './http'

export interface TableUsage {
  tableName: string
  bytes: number
  rowEstimate: number
}

export interface DbUsageResponse {
  totalBytes: number
  limitBytes: number
  tables: TableUsage[]
}

export function fetchDbUsage() {
  return http.get<DbUsageResponse>('/admin/db-usage').then((res) => res.data)
}

export function clearSummaryCache() {
  return http.post<{ deletedCount: number }>('/admin/db-usage/clear-summary-cache').then((res) => res.data)
}

export function purgeTicketTranscripts(olderThanDays: number) {
  return http
    .post<{ clearedCount: number }>('/admin/db-usage/purge-ticket-transcripts', { olderThanDays })
    .then((res) => res.data)
}
