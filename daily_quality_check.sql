CREATE TABLE IF NOT EXISTS data_quality_daily (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  check_date DATE NOT NULL,
  metric_key VARCHAR(80) NOT NULL,
  metric_value DECIMAL(18,4) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_check_metric (check_date, metric_key)
);

DELETE FROM data_quality_daily WHERE check_date = CURDATE();

INSERT INTO data_quality_daily (check_date, metric_key, metric_value)
SELECT CURDATE(), 'trend_row_count', COUNT(*) FROM sys_load_trend;

INSERT INTO data_quality_daily (check_date, metric_key, metric_value)
SELECT CURDATE(), 'trend_unique_time_point', COUNT(DISTINCT time_point) FROM sys_load_trend;

INSERT INTO data_quality_daily (check_date, metric_key, metric_value)
SELECT CURDATE(), 'forecast_row_count', COUNT(*) FROM sys_load_forecast;

INSERT INTO data_quality_daily (check_date, metric_key, metric_value)
SELECT CURDATE(), 'forecast_unique_time_point', COUNT(DISTINCT time_point) FROM sys_load_forecast;

INSERT INTO data_quality_daily (check_date, metric_key, metric_value)
SELECT CURDATE(), 'forecast_trend_avg_ratio',
       CASE WHEN t.avg_v = 0 THEN NULL ELSE f.avg_v / t.avg_v END
FROM (SELECT AVG(load_value) avg_v FROM sys_load_trend) t
CROSS JOIN (SELECT AVG(forecast_value) avg_v FROM sys_load_forecast) f;

INSERT INTO data_quality_daily (check_date, metric_key, metric_value)
SELECT CURDATE(), 'anomaly_total_count', COUNT(*) FROM anomaly_records;

INSERT INTO data_quality_daily (check_date, metric_key, metric_value)
SELECT CURDATE(), 'anomaly_suspect_count', SUM(CASE WHEN IFNULL(is_suspect, 0) = 1 THEN 1 ELSE 0 END)
FROM anomaly_records;

INSERT INTO data_quality_daily (check_date, metric_key, metric_value)
SELECT CURDATE(), 'anomaly_duplicate_meter_day', COALESCE(SUM(cnt - 1), 0)
FROM (
  SELECT meter_id, detect_date, COUNT(*) cnt
  FROM anomaly_records
  GROUP BY meter_id, detect_date
  HAVING COUNT(*) > 1
) x;

INSERT INTO data_quality_daily (check_date, metric_key, metric_value)
SELECT CURDATE(), 'meter_info_null_current_load', SUM(CASE WHEN current_load IS NULL THEN 1 ELSE 0 END)
FROM meter_info;

INSERT INTO data_quality_daily (check_date, metric_key, metric_value)
SELECT CURDATE(), 'meter_info_null_daily_usage', SUM(CASE WHEN daily_usage IS NULL THEN 1 ELSE 0 END)
FROM meter_info;
