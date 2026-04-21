package com.example.demo.controller;

import com.example.demo.entity.UserCluster;
import com.example.demo.mapper.SystemMapper;
import com.example.demo.mapper.UserClusterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    @Autowired
    private UserClusterMapper userClusterMapper;

    @Autowired
    private SystemMapper systemMapper;

    // 获取窃电嫌疑名单
    @GetMapping("/anomaly")
    public List<Map<String, Object>> getAnomaly() {
        return systemMapper.getAnomalyList();
    }

    // 访问路径: http://localhost:8080/api/cluster/list
    @GetMapping("/list")
    public List<UserCluster> getClusterList() {
        return userClusterMapper.getAllClusters();
    }

    // 访问路径: http://localhost:8080/api/cluster/distribution
    @GetMapping("/distribution")
    public List<Map<String, Object>> getDistribution() {
        return userClusterMapper.getClusterDistribution();
    }
}