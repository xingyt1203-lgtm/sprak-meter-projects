package com.example.demo.service;

import com.example.demo.mapper.SystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AnomalyAlertService {

    @Autowired
    private SystemMapper systemMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AnomalyAlertWebSocketHandler webSocketHandler;

    private final Set<String> pushedAlertKeys = new HashSet<>();

    @PostConstruct
    public void init() {
        ensureWorkOrderTable();
    }

    @Scheduled(initialDelay = 12000, fixedRate = 30000)
    public void pushNewAnomalyAlerts() {
        try {
            List<Map<String, Object>> latest = systemMapper.getLatestSuspectAnomalies(20);
            if (latest == null || latest.isEmpty()) {
                return;
            }
            for (Map<String, Object> row : latest) {
                String meterId = String.valueOf(row.get("meter_id"));
                String detectDate = String.valueOf(row.get("detect_date"));
                String key = meterId + "@" + detectDate;
                if (pushedAlertKeys.contains(key)) {
                    continue;
                }
                pushedAlertKeys.add(key);

                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "ANOMALY_ALERT");
                payload.put("meterId", meterId);
                payload.put("detectDate", detectDate);
                payload.put("dailyUsage", row.get("daily_usage"));
                payload.put("avgUsage", row.get("avg_usage"));
                payload.put("zScore", row.get("z_score"));
                payload.put("message", "检测到异常用电，建议立即生成维修工单");
                webSocketHandler.broadcast(payload);
            }

            // 轻量控内存：仅保留最近窗口，避免服务长期运行时集合无限增长
            if (pushedAlertKeys.size() > 3000) {
                pushedAlertKeys.clear();
            }
        } catch (Exception e) {
            System.err.println("[Alert] anomaly push failed: " + e.getMessage());
        }
    }

    public Map<String, Object> createWorkOrder(String meterId, String detectDate, String reason, String source) {
        Map<String, Object> result = new HashMap<>();
        try {
            ensureWorkOrderTable();
            int existed = systemMapper.countWorkOrderByMeterAndDate(meterId, detectDate);
            if (existed > 0) {
                result.put("code", 409);
                result.put("msg", "该异常日期的工单已存在，请勿重复生成");
                return result;
            }

            String orderNo = buildOrderNo();
            String safeReason = (reason == null || reason.trim().isEmpty()) ? "异常用电自动预警" : reason.trim();
            String safeSource = (source == null || source.trim().isEmpty()) ? "web" : source.trim();

            systemMapper.insertWorkOrder(orderNo, meterId, detectDate, "PENDING", safeReason, safeSource);

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "WORK_ORDER_CREATED");
            payload.put("orderNo", orderNo);
            payload.put("meterId", meterId);
            payload.put("detectDate", detectDate);
            payload.put("status", "PENDING");
            payload.put("message", "维修工单已生成，等待派发");
            webSocketHandler.broadcast(payload);

            result.put("code", 200);
            result.put("msg", "工单创建成功");
            result.put("orderNo", orderNo);
            result.put("status", "PENDING");
            return result;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "工单创建失败: " + e.getMessage());
            return result;
        }
    }

    public Map<String, Object> createWorkOrderByMeter(String meterId, String reason, String source) {
        if (meterId == null || meterId.trim().isEmpty()) {
            Map<String, Object> bad = new HashMap<>();
            bad.put("code", 400);
            bad.put("msg", "meterId 不能为空");
            return bad;
        }

        String detectDate = systemMapper.getLatestSuspectDetectDateByMeter(meterId.trim());
        if (detectDate == null || detectDate.trim().isEmpty()) {
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("code", 404);
            notFound.put("msg", "该设备暂无可建单的异常记录");
            return notFound;
        }

        return createWorkOrder(meterId.trim(), detectDate.trim(), reason, source);
    }

    private void ensureWorkOrderTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS repair_work_order (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "order_no VARCHAR(40) NOT NULL," +
                        "meter_id VARCHAR(64) NOT NULL," +
                        "detect_date VARCHAR(32) NOT NULL," +
                        "order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
                        "reason VARCHAR(255) NULL," +
                        "source VARCHAR(32) NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "UNIQUE KEY uq_order_no (order_no)," +
                        "UNIQUE KEY uq_meter_day (meter_id, detect_date)" +
                        ")"
        );
    }

    private String buildOrderNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "WO-" + System.currentTimeMillis() + "-" + suffix;
    }
}
