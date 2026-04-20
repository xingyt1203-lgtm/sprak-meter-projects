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

    // 2. 查 24 小时负荷折线数据
    @Select("SELECT time_point, load_value FROM sys_load_trend ORDER BY time_point ASC")
    List<Map<String, Object>> getLoadTrendList();

    // 3. 查辖区负荷排行数据
    @Select("SELECT region_name, load_value FROM sys_region_load ORDER BY load_value DESC")
    List<Map<String, Object>> getRegionLoads();

    @Select("SELECT COUNT(*) FROM meter_info")
    int countTotalMeters();

    @Select("SELECT COUNT(*) FROM meter_info WHERE status = 2")
    int countAbnormalMeters();

    @Select("SELECT COUNT(*) FROM meter_info WHERE status = 3")
    int countOfflineMeters();

    @Select("SELECT cluster_name as name, COUNT(*) as value FROM cluster_result GROUP BY cluster_name")
    List<Map<String, Object>> getUserClusterDistribution();

    @Select("SELECT meter_id, detect_date, daily_usage, avg_usage, z_score " +
            "FROM anomaly_records ORDER BY detect_date DESC LIMIT 50")      
    List<Map<String, Object>> getAnomalyList();

    @Select("SELECT forecast_value FROM sys_load_forecast ORDER BY time_point ASC")
    List<Double> getLoadForecastList();

    @Delete("DELETE FROM sys_load_forecast")
    void deleteAllForecast();

    @Insert("INSERT INTO sys_load_forecast (time_point, forecast_value) VALUES (#{timePoint}, #{value})")
    void insertForecast(@Param("timePoint") String timePoint, @Param("value") double value);

    @Select("SELECT " +
            "meter_id AS id, " +
            "address AS region, " +
            "current_load AS currentLoad, " +
            "daily_usage AS dailyUsage, " +
            "status AS status " +
            "FROM meter_info LIMIT 50")
    List<Map<String, Object>> getMeterMonitorList();

    @Select("SELECT user_name, address, meter_type FROM meter_info WHERE meter_id = #{id} LIMIT 1")
    Map<String, Object> getMeterBaseInfo(@Param("id") String id);

    @Select("SELECT cluster_name FROM cluster_result WHERE meter_id = #{id} LIMIT 1")
    String getClusterNameByMeterId(@Param("id") String id);

    @Select("SELECT COUNT(*) FROM anomaly_records WHERE meter_id = #{id}")  
    int checkIsAnomaly(@Param("id") String id);

    @Select("SELECT DATE_FORMAT(stat_date, '%m-%d') as recordDate, total_usage as dailyUsage " +
            "FROM daily_usage_stat " +
            "WHERE meter_id = #{id} ORDER BY stat_date DESC LIMIT 30")      
    List<Map<String, Object>> getMeter30DaysUsage(@Param("id") String id);  

    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username} AND password = #{password} AND status = 1")
    Integer checkUserLogin(@Param("username") String username, @Param("password") String password);

    // 检查账号是否已存在
    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
    Integer checkUserExists(@Param("username") String username);

    // 注册新用户入库 (默认状态设为 1 正常)
    @Insert("INSERT INTO sys_user (username, password, real_name, status, create_time) VALUES (#{username}, #{password}, #{realName}, 1, NOW())")
    void insertNewUser(@Param("username") String username, @Param("password") String password, @Param("realName") String realName);
}