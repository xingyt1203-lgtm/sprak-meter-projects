package com.example.demo.service;

import com.example.demo.mapper.SystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@Service
public class AiForecastService {

    private final String PYTHON_API_URL = "http://127.0.0.1:8000/api/v1/predict/tomorrow";

    @Autowired  
    private SystemMapper systemMapper;

    // 每小时同步一次
    @Scheduled(fixedRate = 3600000)
    public void fetchPredictionData() {
        System.out.println("🔔 [Java] 正在尝试从 Python 获取最新的 AI 预测数据...");

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(PYTHON_API_URL, Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("code") && (Integer) body.get("code") == 200) {

                List<Double> loads = (List<Double>) body.get("hourly_loads");

                // 数据库操作
                systemMapper.deleteAllForecast();
                System.out.println("🧹 [数据库] 已清空旧的预测记录...");

                for (int i = 0; i < loads.size(); i++) {
                    String timePoint = String.format("%02d:00", i);
                    double val = Double.parseDouble(loads.get(i).toString());
                    systemMapper.insertForecast(timePoint, val);
                }

                System.out.println("🚀 [数据库] 24小时新预测数据已同步至 sys_load_forecast 表！");
            }
        } catch (Exception e) {
            System.err.println("❌ [Java] 同步失败！原因: " + e.getMessage());
        }
    }
}
