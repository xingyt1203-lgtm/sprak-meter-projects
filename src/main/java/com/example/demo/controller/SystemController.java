package com.example.demo.controller; 

import com.example.demo.mapper.SystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@CrossOrigin
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private SystemMapper systemMapper;

    // 📊 提供首页大屏所有宏观数据
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> response = new HashMap<>();
        
        // ================= 1. 核心指标 =================
        // 先把原来假表里的数据拿出来 (为了保留 totalLoad 和 sparkTime)
        Map<String, Object> coreStats = systemMapper.getDashboardCoreStats();
        if (coreStats != null) {
            response.putAll(coreStats); 
        }

        // 🌟🌟 核心修复：从真实的 meter_info 表里统计数量！🌟🌟
        int total = systemMapper.countTotalMeters();
        int abnormal = systemMapper.countAbnormalMeters();
        int offline = systemMapper.countOfflineMeters();
        int normal = total - abnormal - offline; // 剩下的就是正常的

        // 强行覆盖掉 coreStats 里的假数据，让顶部卡片显示真实数字
        response.put("totalMeters", total);
        response.put("anomalyCount", abnormal);


        // ================= 2. 辖区排行 =================
        List<Map<String, Object>> regionList = systemMapper.getRegionLoads();
        List<String> regions = new ArrayList<>();
        List<Double> regionLoads = new ArrayList<>();
        
        if (regionList != null) {
            for (Map<String, Object> row : regionList) {
                regions.add((String) row.get("region_name"));
                regionLoads.add(Double.valueOf(row.get("load_value").toString()));
            }
        }
        response.put("regions", regions);
        response.put("regionLoads", regionLoads);

        // ================= 3. 终端设备在线率 (右侧上饼图) =================
        // 🌟🌟 核心修复：用真实的统计数字构造饼图数据，确保大屏数据绝对一致！🌟🌟
        List<Map<String, Object>> devices = new ArrayList<>();
        
        Map<String, Object> normalNode = new HashMap<>();
        normalNode.put("name", "正常在线");
        normalNode.put("value", normal);
        
        Map<String, Object> abnormalNode = new HashMap<>();
        abnormalNode.put("name", "异常");
        abnormalNode.put("value", abnormal);
        
        Map<String, Object> offlineNode = new HashMap<>();
        offlineNode.put("name", "离线");
        offlineNode.put("value", offline);
        
        devices.add(normalNode);
        devices.add(abnormalNode);
        devices.add(offlineNode);
        
        // 把重新拼装好的真实饼图数据发给前端
        response.put("devices", devices);


        // ================= 4. AI 聚类画像分布 (右侧下饼图) =================
        List<Map<String, Object>> clusters = systemMapper.getUserClusterDistribution();
        response.put("clusters", clusters);

        return response;
    }

    // 📈 提供 24 小时全网用电负荷趋势数据 + LSTM AI 预测数据
    @GetMapping("/load")
    public Map<String, Object> getLoadTrend() {
        Map<String, Object> response = new HashMap<>();
        
        // 1. 从 MySQL 查询今日真实的折线图数据
        List<Map<String, Object>> trendList = systemMapper.getLoadTrendList();
        
        List<String> hours = new ArrayList<>();
        List<Double> loads = new ArrayList<>();
        
        if (trendList != null) {
            for (Map<String, Object> row : trendList) {
                hours.add((String) row.get("time_point"));
                loads.add(Double.valueOf(row.get("load_value").toString())); 
            }
        }

        // 2. 查明日预测负荷 
        List<Double> forecasts = systemMapper.getLoadForecastList();

        // 3. 将真实数据和预测数据一起打包发给 Vue 前端
        response.put("hours", hours);
        response.put("loads", loads);           // 实线：今日真实
        response.put("forecasts", forecasts);   // 虚线：明日预测

        return response;
    }
}