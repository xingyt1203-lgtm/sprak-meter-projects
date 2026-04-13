package com.example.demo.service;

import com.example.demo.mapper.SystemMapper; // 👈 新增：引入你的 Mapper (请确认包名路径对不对)
import org.springframework.beans.factory.annotation.Autowired; // 👈 新增：引入自动装配注解
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.List; // 👈 新增：引入 List
import java.util.Map;


@Service
public class AiForecastService {

    private final String PYTHON_API_URL = "http://127.0.0.1:8000/api/v1/predict/tomorrow";

    // 🚀 新增 1：把操作数据库的 Mapper 请进来
    @Autowired
    private SystemMapper systemMapper;

    // 把 10000(10秒) 改成了 60000(1分钟)。一直刷库太耗性能，1分钟同步一次刚刚好。
    @Scheduled(fixedRate = 3600000) 
    public void fetchPredictionData() {
        System.out.println("⏳ [Java] 正在尝试从 Python 获取最新的 AI 预测数据...");

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(PYTHON_API_URL, Map.class);
            
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("code") && (Integer) body.get("code") == 200) {
                
                // 🚀 新增 2：从 Python 拿出来的数组，转换成 Java 的 List
                List<Double> loads = (List<Double>) body.get("hourly_loads");
                
                // 🚀 新增 3：开始数据库操作
                // 第一步：清空表里之前的数据
                systemMapper.deleteAllForecast();
                System.out.println("🧹 [数据库] 已清空旧的预测记录...");

                // 第二步：循环 24 次，把今天新算的 24 个数字塞进去
                for (int i = 0; i < loads.size(); i++) {
                    // %02d:00 会把 0 变成 "00:00"，1 变成 "01:00"
                    String timePoint = String.format("%02d:00", i); 
                    double val = loads.get(i);
                    
                    // 调用 Mapper 插入数据库
                    systemMapper.insertForecast(timePoint, val);
                }
                
                System.out.println("🚀 [数据库] 24小时新预测数据已同步至 sys_load_forecast 表！");
                
            }
        } catch (Exception e) {
            System.err.println("❌ [Java] 同步失败！原因: " + e.toString());
        }
    }
}