package com.example.demo.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface SystemMapper {

    // 1. 查大屏核心指标
    @Select("SELECT total_meters as totalMeters, total_load as totalLoad, anomaly_count as anomalyCount, spark_time as sparkTime FROM sys_dashboard ORDER BY id DESC LIMIT 1")
    Map<String, Object> getDashboardCoreStats();

    // 2. 查24小时负荷折线数据
    @Select("SELECT time_point, load_value FROM sys_load_trend ORDER BY time_point ASC")
    List<Map<String, Object>> getLoadTrendList();

    // 3. 查辖区负荷排行数据
    @Select("SELECT region_name, load_value FROM sys_region_load ORDER BY load_value DESC")
    List<Map<String, Object>> getRegionLoads();

    // ================= 👇 修复后的三大实时统计（解决饼图和卡片数据不对的问题） 👇 =================
    
    @Select("SELECT COUNT(*) FROM meter_info")
    int countTotalMeters();

    // 🌟 已修复：使用真实状态码 2 代表异常
    @Select("SELECT COUNT(*) FROM meter_info WHERE status = 2")
    int countAbnormalMeters();

    // 🌟 已修复：使用真实状态码 3 代表离线
    @Select("SELECT COUNT(*) FROM meter_info WHERE status = 3")
    int countOfflineMeters();

    // 🌟 已修复：饼图专用 SQL，精准切分正常(1)、异常(2)、离线(3)
    @Select("SELECT CASE " +
            "WHEN status = 1 THEN '正常在线' " +
            "WHEN status = 2 THEN '异常' " +
            "ELSE '离线' END as name, " +
            "COUNT(*) as value FROM meter_info GROUP BY status")
    List<Map<String, Object>> getDeviceStatus();
    
    // ================= 👆 修复结束 👆 =================

    // 5. 查用户聚类分布饼图
    @Select("SELECT cluster_name as name, COUNT(*) as value FROM cluster_result GROUP BY cluster_name")
    List<Map<String, Object>> getUserClusterDistribution();

    // 查异常嫌疑名单
    @Select("SELECT meter_id, detect_date, daily_usage, avg_usage, z_score " +
            "FROM anomaly_records ORDER BY detect_date DESC LIMIT 50")
    List<Map<String, Object>> getAnomalyList();

    // 查明日预测负荷折线数据
    @Select("SELECT forecast_value FROM sys_load_forecast ORDER BY time_point ASC")
    List<Double> getLoadForecastList();

    // 清空旧数据
    @Delete("DELETE FROM sys_load_forecast")
    void deleteAllForecast();

    // 插入新数据
    @Insert("INSERT INTO sys_load_forecast (time_point, forecast_value) VALUES (#{timePoint}, #{value})")
    void insertForecast(@Param("timePoint") String timePoint, @Param("value") double value);

    // ================= 👇 模块一：设备状态监控 (完全真实版) 👇 =================
    // 已经彻底撕掉假数据！直接查询数据库里真实的 current_load 和 daily_usage
    @Select("SELECT " +
            "meter_id AS id, " +
            "address AS region, " +      
            "current_load AS currentLoad, " + 
            "daily_usage AS dailyUsage, " +    
            "status AS status " +
            "FROM meter_info LIMIT 50")
    List<Map<String, Object>> getMeterMonitorList();


    // ================= 👇 模块二：个体画像检索 (完全真实版) 👇 =================
    
    // 🌟 根据电表ID查询该用户的真实姓名、地址和电表类型
    @Select("SELECT user_name, address, meter_type FROM meter_info WHERE meter_id = #{id} LIMIT 1")
    Map<String, Object> getMeterBaseInfo(@Param("id") String id);

    // 1. 根据电表 ID 查询它真实的聚类画像名称
    @Select("SELECT cluster_name FROM cluster_result WHERE meter_id = #{id} LIMIT 1")
    String getClusterNameByMeterId(@Param("id") String id);

    // 2. 检查这个电表在不在异常名单里 
    @Select("SELECT COUNT(*) FROM anomaly_records WHERE meter_id = #{id}")
    int checkIsAnomaly(@Param("id") String id);
    // ================= 👇 个体画像：直接查询 Spark 跑出来的真实日用电统计 👇 =================
    @Select("SELECT DATE_FORMAT(stat_date, '%m-%d') as recordDate, total_usage as dailyUsage " +
            "FROM daily_usage_stat " +
            "WHERE meter_id = #{id} ORDER BY stat_date DESC LIMIT 30")
    List<Map<String, Object>> getMeter30DaysUsage(@Param("id") String id);
}