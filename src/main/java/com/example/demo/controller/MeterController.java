package com.example.demo.controller;

import com.example.demo.mapper.SystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/meter")
public class MeterController {

    @Autowired
    private SystemMapper systemMapper;

    @GetMapping("/detail/{id}")
    public Map<String, Object> getMeterDetail(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();

        // 1. 获取基本信息
        Map<String, Object> baseInfo = systemMapper.getMeterBaseInfo(id);
        if (baseInfo != null) {
            response.put("name", baseInfo.get("user_name"));
            response.put("address", baseInfo.get("address"));
            response.put("type", baseInfo.get("meter_type"));
        } else {
            response.put("name", "未知用户");
            response.put("address", "未知地址");
            response.put("type", "普通表");
        }

        // 2. 获取聚类标签
        String dbClusterName = systemMapper.getClusterNameByMeterId(id);
        response.put("clusterLabel", dbClusterName != null ? dbClusterName : "未参与本次 AI 聚类");

        // 3. 检查异常状态 (根据 anomaly_records 表判定)
        int anomalyCount = 0;
        try {
            anomalyCount = systemMapper.checkIsAnomaly(id);
        } catch (Exception e) {
            System.err.println("⚠️ 警告：查询异常表失败，可能表不存在。内容: " + e.getMessage());
        }
        boolean isRealAnomaly = anomalyCount > 0;
        response.put("isAnomaly", isRealAnomaly);
        response.put("anomalyLabel", isRealAnomaly ? "⚠️ 3-Sigma 预警：用电骤降" : "✅ 近期体检正常");

        // 4. 从数据库获取真实的 30 天用电历史流水
        List<String> dates = new ArrayList<>();
        List<Double> usages = new ArrayList<>();

        List<Map<String, Object>> historyRecords = systemMapper.getMeter30DaysUsage(id);  

        if (historyRecords != null && !historyRecords.isEmpty()) {
            Collections.reverse(historyRecords);
            for (Map<String, Object> record : historyRecords) {
                dates.add((String) record.get("recordDate"));
                usages.add(Double.valueOf(record.get("dailyUsage").toString()));
            }
        }
        
        response.put("dates", dates);
        response.put("usages", usages);

        return response;
    }
}
