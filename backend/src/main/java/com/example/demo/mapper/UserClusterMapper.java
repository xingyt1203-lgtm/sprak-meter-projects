package com.example.demo.mapper;

import com.example.demo.entity.User;
import com.example.demo.entity.UserCluster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserClusterMapper {

    // 原有的聚类数据查询
    @Select("SELECT * FROM user_clusters")
    List<UserCluster> getAllClusters();

    @Select("SELECT cluster_name AS name, COUNT(*) AS value FROM meter_cluster_result GROUP BY cluster_name")
    List<Map<String, Object>> getClusterDistribution();

    // 新增的登录查询
    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    User findByUsername(@Param("username") String username);

    // 修改状态的方法（可以对嫌疑人进行处理）
    @Update("UPDATE anomaly_records SET status = #{status} WHERE id = #{id}")
    int updateAnomalyStatus(@Param("id") Integer id, @Param("status") String status);

    // 查窃电异常行为嫌疑名单
    @Select("SELECT meter_id, detect_date, daily_usage, avg_usage, z_score " +
            "FROM anomaly_records ORDER BY detect_date DESC LIMIT 50")
    List<Map<String, Object>> getAnomalyList();
}
