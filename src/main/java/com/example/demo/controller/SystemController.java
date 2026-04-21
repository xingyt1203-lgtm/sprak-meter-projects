package com.example.demo.controller;

import com.example.demo.mapper.SystemMapper;
import com.example.demo.service.AiForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private SystemMapper systemMapper;

    @Autowired
    private AiForecastService aiForecastService;

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> response = new HashMap<>();

        // 1. 核心指标
        Map<String, Object> coreStats = systemMapper.getDashboardCoreStats();
        if (coreStats != null) {
            response.putAll(coreStats);
        }

        // 核心修复：从真实的 meter_info 表里统计数量
        int total = systemMapper.countTotalMeters();
        int abnormal = systemMapper.countAbnormalMeters();
        int offline = systemMapper.countOfflineMeters();
        int normal = total - abnormal - offline;

        response.put("totalMeters", total);
        response.put("anomalyCount", abnormal);

        // 2. 辖区排行
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

        // 3. 终端设备在线率
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

        response.put("devices", devices);

        // 4. AI 聚类画像分布
        List<Map<String, Object>> clusters = systemMapper.getUserClusterDistribution();
        response.put("clusters", clusters);

        return response;
    }

    @GetMapping("/load")
    public Map<String, Object> getLoadTrend() {
        Map<String, Object> response = new HashMap<>();

        // 1. 查询今日真实的折线图数据
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

        // 预测数据可能来自不同量纲（例如 Python 侧聚合口径变化），这里做一次标尺对齐，避免前端曲线失真。
        forecasts = calibrateForecastScale(loads, forecasts);

        // 3. 将真实数据和预测数据一起打包发送
        response.put("hours", hours);
        response.put("loads", loads);
        response.put("forecasts", forecasts);
        return response;
    }

    private List<Double> calibrateForecastScale(List<Double> loads, List<Double> forecasts) {
        if (loads == null || forecasts == null || loads.isEmpty() || forecasts.isEmpty()) {
            return forecasts;
        }

        double lMin = Collections.min(loads);
        double lMax = Collections.max(loads);
        double fMin = Collections.min(forecasts);
        double fMax = Collections.max(forecasts);

        // 已经在同一量级时直接返回，避免重复变换。
        double avgLoad = loads.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgFc = forecasts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (avgLoad > 0 && avgFc > 0) {
            double ratio = avgFc / avgLoad;
            if (ratio >= 0.7 && ratio <= 1.3) {
                return forecasts;
            }
        }

        List<Double> normalized = new ArrayList<>(forecasts.size());
        if (Math.abs(fMax - fMin) < 1e-6) {
            for (int i = 0; i < forecasts.size(); i++) {
                normalized.add(round2(avgLoad));
            }
            return normalized;
        }

        double targetRange = Math.max(1.0, lMax - lMin);
        for (Double v : forecasts) {
            double t = (v - fMin) / (fMax - fMin);
            double mapped = lMin + t * targetRange;
            normalized.add(round2(mapped));
        }
        return normalized;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @PostMapping("/forecast/sync")
    public Map<String, Object> syncForecastManually(
            @RequestParam(value = "retrain", required = false, defaultValue = "false") boolean retrain) {
        return aiForecastService.syncForecastNow(retrain);
    }
}
