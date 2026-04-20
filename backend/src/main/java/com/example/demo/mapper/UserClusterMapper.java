package com.example.demo.mapper;

import com.example.demo.entity.User; // 确保你有这个导入
import com.example.demo.entity.UserCluster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserClusterMapper {

    // 1. 原有的聚类数据查�?    @Select("SELECT * FROM user_clusters")
    List<UserCluster> getAllClusters();

    @Select("SELECT cluster_name AS name, COUNT(*) AS value FROM meter_cluster_result GROUP BY cluster_name")
    List<Map<String, Object>> getClusterDistribution();

    // 2. 新增的登录查�?(把刚才报错的代码贴到这里�?
    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    User findByUsername(@Param("username") String username);
    // 1. �?UserClusterMapper 接口里增加这几行


    // 2. 顺便加一个修改状态的方法（可以对嫌疑人进行处理）
    @Update("UPDATE anomaly_records SET status = #{status} WHERE id = #{id}")
    int updateAnomalyStatus(@Param("id") Integer id, @Param("status") String status);

    // 🚨 查窃�?异常行为嫌疑名单
    // 注意：把 anomaly_records 换成你数据库里真实存�?30 个嫌疑人的表名！
    @Select("SELECT meter_id, detect_date, daily_usage, avg_usage, z_score " +
            "FROM anomaly_records ORDER BY detect_date DESC LIMIT 50")
    List<Map<String, Object>> getAnomalyList();
}
