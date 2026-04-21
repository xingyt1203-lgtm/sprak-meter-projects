package com.example.demo.service;

import com.example.demo.mapper.SystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiForecastService {

    private final String PYTHON_API_URL = "http://127.0.0.1:8000/api/v1/predict/tomorrow";
    private static final int STARTUP_RETRY_TIMES = 10;
    private static final int STARTUP_RETRY_INTERVAL_MS = 1500;

    @Autowired  
    private SystemMapper systemMapper;

    // 每小时同步一次
    @Scheduled(initialDelay = 20000, fixedRate = 3600000)
    public void fetchPredictionData() {
        syncForecastNow(false);
    }

    public Map<String, Object> syncForecastNow() {
        return syncForecastNow(false);
    }

    public Map<String, Object> syncForecastNow(boolean retrain) {
        System.out.println("🔔 [Java] 正在尝试从 Python 获取最新的 AI 预测数据...");
        Map<String, Object> result = new HashMap<>();
        String requestUrl = retrain ? PYTHON_API_URL + "?retrain=1" : PYTHON_API_URL;
        Exception lastException = null;

        for (int attempt = 1; attempt <= STARTUP_RETRY_TIMES; attempt++) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("code") && (Integer) body.get("code") == 200) {

                    List<Double> loads = toDoubleList(body.get("hourly_loads"));
                    List<Double> alignedLoads = alignForecastScale(loads, systemMapper.getTrendStats());

                    // 数据库操作
                    systemMapper.deleteAllForecast();
                    System.out.println("🧹 [数据库] 已清空旧的预测记录...");

                    for (int i = 0; i < alignedLoads.size(); i++) {
                        String timePoint = String.format("%02d:00", i);
                        double val = Double.parseDouble(alignedLoads.get(i).toString());
                        systemMapper.insertForecast(timePoint, val);
                    }

                    System.out.println("🚀 [数据库] 24小时新预测数据已同步至 sys_load_forecast 表！");
                    result.put("code", 200);
                    result.put("msg", retrain ? "预测同步成功（已强制重训模型）" : "预测同步成功");
                    result.put("rows", alignedLoads.size());
                    result.put("retrain", retrain);
                    return result;
                }

                result.put("code", 500);
                result.put("msg", "Python 返回非成功状态");
                result.put("rows", 0);
                result.put("retrain", retrain);
                return result;
            } catch (Exception e) {
                lastException = e;
                System.err.println("⚠️ [Java] 第 " + attempt + " 次拉取预测失败: " + e.getMessage());
                if (attempt < STARTUP_RETRY_TIMES) {
                    try {
                        Thread.sleep(STARTUP_RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        String errMsg = (lastException == null) ? "未知错误" : lastException.getMessage();
        System.err.println("❌ [Java] 同步失败！原因: " + errMsg);
        result.put("code", 500);
        result.put("msg", "预测同步失败: " + errMsg);
        result.put("rows", 0);
        result.put("retrain", retrain);
        return result;
    }

    private List<Double> alignForecastScale(List<Double> rawForecasts, Map<String, Object> trendStats) {
        if (rawForecasts == null || rawForecasts.isEmpty() || trendStats == null) {
            return rawForecasts;
        }

        double trendAvg = toDouble(trendStats.get("avgLoad"));
        double trendMin = toDouble(trendStats.get("minLoad"));
        double trendMax = toDouble(trendStats.get("maxLoad"));

        double forecastAvg = rawForecasts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (trendAvg <= 0 || forecastAvg <= 0) {
            return rawForecasts;
        }

        // 同量级数据直接返回
        double ratio = forecastAvg / trendAvg;
        if (ratio >= 0.7 && ratio <= 1.3) {
            return rawForecasts;
        }

        double fMin = Collections.min(rawForecasts);
        double fMax = Collections.max(rawForecasts);
        if (Math.abs(fMax - fMin) < 1e-6) {
            List<Double> flat = new ArrayList<>();
            for (int i = 0; i < rawForecasts.size(); i++) {
                flat.add(round2(trendAvg));
            }
            return flat;
        }

        double targetRange = Math.max(1.0, trendMax - trendMin);
        List<Double> scaled = new ArrayList<>();
        for (Double v : rawForecasts) {
            double t = (v - fMin) / (fMax - fMin);
            scaled.add(round2(trendMin + t * targetRange));
        }
        return scaled;
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        return Double.parseDouble(value.toString());
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private List<Double> toDoubleList(Object value) {
        List<Double> result = new ArrayList<>();
        if (!(value instanceof List)) {
            return result;
        }
        for (Object item : (List<?>) value) {
            if (item != null) {
                result.add(Double.parseDouble(item.toString()));
            }
        }
        return result;
    }
}
