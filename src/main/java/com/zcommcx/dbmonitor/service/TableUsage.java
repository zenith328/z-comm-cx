package com.zcommcx.dbmonitor.service;

/** pg_stat_user_tables 조회 결과 한 줄 (테이블별 용량/행수 추정치). */
public record TableUsage(String tableName, long bytes, long rowEstimate) {
}
