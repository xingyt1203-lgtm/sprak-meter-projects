package com.example.demo.controller;

import com.example.demo.mapper.SystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@CrossOrigin // 允许前端跨域访问
@RestController
@RequestMapping("/api/meter") // 统一前缀
public class MeterController {

    @Autowired
    private SystemMapper systemMapper;

    // ================= 接口 1: 设备状态监控 (查 50 户列表) =================
    @GetMapping("/monitor-list")
    public List<Map<String, Object>> getMonitorList() {
        System.out.println("⚡ [Java] 正在从数据库提取 50 户电表真实数据...");
        return systemMapper.getMeterMonitorList();
    }

    // ================= 接口 2: 个体画像检索 (查单户 30 天详情与真实档案) =================
    @GetMapping("/detail")
    public Map<String, Object> getMeterDetail(@RequestParam("id") String id) {
        Map<String, Object> response = new HashMap<>();
        
        // 1. 获取户主真实姓名、地址、电表类型 (从数据库档案表读取)
        Map<String, Object> baseInfo = systemMapper.getMeterBaseInfo(id);
        if (baseInfo != null) {
            response.put("userName", baseInfo.get("user_name"));
            response.put("address", baseInfo.get("address"));
            response.put("meterType", baseInfo.get("meter_type"));
        } else {
            response.put("userName", "未知用户");
            response.put("address", "未知地址");
            response.put("meterType", "未知类型");
        }

        // 2. 获取 AI 聚类画像名称
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

        // 4. 从数据库获取真实的 30 天用电历史流水 (彻底移除 Random 逻辑)
        List<String> dates = new ArrayList<>();
        List<Double> usages = new ArrayList<>();
        
        // 调用 Mapper 访问你截图中的 daily_usage_stat 表
        List<Map<String, Object>> historyRecords = systemMapper.getMeter30DaysUsage(id);
        
        if (historyRecords != null && !historyRecords.isEmpty()) {
            // 🌟 重点：如果 SQL 使用 ORDER BY stat_date DESC，这里需要反转顺序以符合图表从左到右的时间轴
            Collections.reverse(historyRecords);
            
            for (Map<String, Object> record : historyRecords) {
                // recordDate 对应 SQL 里的 DATE_FORMAT 结果
                dates.add((String) record.get("recordDate"));
                // dailyUsage 对应 SQL 里的 total_usage 字段
                usages.add(Double.valueOf(record.get("dailyUsage").toString()));
            }
        } else {
            // 兜底空数据处理
            dates.add("暂无数据");
            usages.add(0.0);
        }

        // 5. 封装最终返回结果
        response.put("dates", dates);
        response.put("usages", usages);
        response.put("meterId", id);

        return response;
    }
}