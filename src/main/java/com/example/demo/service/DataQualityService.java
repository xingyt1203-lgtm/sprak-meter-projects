package com.example.demo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DataQualityService {

    private final JdbcTemplate jdbcTemplate;

    public DataQualityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 每天 00:10 执行一次数据质量快照
    @Scheduled(cron = "0 10 0 * * ?")
    public void collectDailyQualitySnapshot() {
        try {
            ensureTable();
            jdbcTemplate.update("DELETE FROM data_quality_daily WHERE check_date = CURDATE()");

            insertMetric("trend_row_count", "SELECT COUNT(*) FROM sys_load_trend");
            insertMetric("trend_unique_time_point", "SELECT COUNT(DISTINCT time_point) FROM sys_load_trend");
            insertMetric("forecast_row_count", "SELECT COUNT(*) FROM sys_load_forecast");
            insertMetric("forecast_unique_time_point", "SELECT COUNT(DISTINCT time_point) FROM sys_load_forecast");
            insertMetric("forecast_trend_avg_ratio",
                    "SELECT CASE WHEN t.avg_v = 0 THEN NULL ELSE f.avg_v / t.avg_v END " +
                    "FROM (SELECT AVG(load_value) avg_v FROM sys_load_trend) t " +
                    "CROSS JOIN (SELECT AVG(forecast_value) avg_v FROM sys_load_forecast) f");
            insertMetric("anomaly_total_count", "SELECT COUNT(*) FROM anomaly_records");
            insertMetric("anomaly_suspect_count", "SELECT SUM(CASE WHEN IFNULL(is_suspect, 0) = 1 THEN 1 ELSE 0 END) FROM anomaly_records");
            insertMetric("anomaly_duplicate_meter_day",
                    "SELECT COALESCE(SUM(cnt - 1), 0) FROM (" +
                    "SELECT meter_id, detect_date, COUNT(*) cnt FROM anomaly_records GROUP BY meter_id, detect_date HAVING COUNT(*) > 1" +
                    ") x");
            insertMetric("meter_info_null_current_load", "SELECT SUM(CASE WHEN current_load IS NULL THEN 1 ELSE 0 END) FROM meter_info");
            insertMetric("meter_info_null_daily_usage", "SELECT SUM(CASE WHEN daily_usage IS NULL THEN 1 ELSE 0 END) FROM meter_info");

            System.out.println("✅ [DataQuality] 今日数据质量快照已写入 data_quality_daily");
        } catch (Exception e) {
            System.err.println("❌ [DataQuality] 数据质量快照失败: " + e.getMessage());
        }
    }

    private void ensureTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS data_quality_daily (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "check_date DATE NOT NULL," +
                        "metric_key VARCHAR(80) NOT NULL," +
                        "metric_value DECIMAL(18,4) NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "UNIQUE KEY uq_check_metric (check_date, metric_key)" +
                        ")"
        );
    }

    private void insertMetric(String key, String valueSql) {
        String sql = "INSERT INTO data_quality_daily (check_date, metric_key, metric_value) " +
                "SELECT CURDATE(), ?, (" + valueSql + ")";
        jdbcTemplate.update(sql, key);
    }
}
