package com.example.demo.controller;

import com.example.demo.entity.UserCluster;
import com.example.demo.mapper.UserClusterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import com.example.demo.entity.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.HashMap;


@RestController
@RequestMapping("/api/cluster")
@CrossOrigin // 允许前端跨域请求
public class ClusterController {
    @PostMapping("/login")
public Object login(@RequestBody User loginUser) {
    User user = userClusterMapper.login(loginUser.getUsername(), loginUser.getPassword());
    HashMap<String, Object> result = new HashMap<>();
    if (user != null) {
        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("data", user);
    } else {
        result.put("code", 400);
        result.put("msg", "用户名或密码错误");
    }
    return result;
}
// 获取窃电嫌疑名单
    @GetMapping("/anomaly")
    public List<Map<String, Object>> getAnomaly() {
        return userClusterMapper.getAnomalyList();
    }

    @Autowired
    private UserClusterMapper userClusterMapper;

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
