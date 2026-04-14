from pyspark.sql import SparkSession
from pyspark.sql.functions import col, sum as _sum, max as _max, min as _min, to_date, date_format

def main():
    # ================= 1. 初始化 Spark =================
    spark = SparkSession.builder \
        .appName("MeterDataAggregation") \
        .config("spark.driver.extraClassPath", "mysql-connector-java-8.0.30.jar") \
        .getOrCreate()
    
    # 屏蔽多余的 INFO 日志，只看关键输出
    spark.sparkContext.setLogLevel("WARN")

    # ================= 2. 全局数据库配置 =================
    # 统一使用这一个配置，防止前后密码、URL不一致
    db_url = "jdbc:mysql://localhost:3306/spark_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
    db_user = "root"
    db_password = "123456" # 确保这是你的真实密码
    db_driver = "com.mysql.cj.jdbc.Driver"

    db_properties = {
        "user": db_user,
        "password": db_password,
        "driver": db_driver
    }

    # ================= 3. 读取原始数据 =================
    print(" 正在读取 meter_data_raw 表...")
    raw_df = spark.read.jdbc(url=db_url, table="meter_data_raw", properties=db_properties)

    record_count = raw_df.count()
    print(f" Spark 成功读取到原始记录数: {record_count} 条")

    if record_count == 0:
        print("❌ 警告：原始表里没有数据，Spark 停止计算！请先检查 MySQL。")
        spark.stop()
        return

    # ================= 4. 计算一：日总电量与峰谷差 =================
    print(" 正在计算：日总电量、峰值、峰谷差...")
    
    # 按电表ID和日期分组，计算总用电量、最大负荷、峰谷差 (最大值 - 最小值)
    daily_stat_df = raw_df.withColumn("stat_date", to_date(col("record_time"))) \
        .groupBy("meter_id", "stat_date") \
        .agg(
            _sum("power_usage").alias("total_usage"),
            _max("power_usage").alias("max_load"),
            (_max("power_usage") - _min("power_usage")).alias("peak_usage")
        )

    # 严格对齐数据库字段顺序
    final_daily_df = daily_stat_df.select(
        col("meter_id"),
        col("stat_date"),
        col("total_usage"),
        col("peak_usage"),
        col("max_load")
    )

    print(" 正在将每日统计写入 daily_usage_stat 表...")
    final_daily_df.write \
        .format("jdbc") \
        .option("url", db_url) \
        .option("dbtable", "daily_usage_stat") \
        .option("user", db_user) \
        .option("password", db_password) \
        .option("driver", db_driver) \
        .option("truncate", "true") \
        .mode("overwrite") \
        .save()

    # ================= 5. 计算二：24小时全网趋势 =================
    print(" 正在按小时聚合 24 小时全网负荷趋势...")

    # 从 record_time 中提取小时 (例如 "08:00")，并对全网的 power_usage 进行求和
    trend_df = raw_df.withColumn("time_point", date_format(col("record_time"), "HH:00")) \
        .groupBy("time_point") \
        .agg(_sum("power_usage").alias("load_value")) \
        .orderBy("time_point")

    print("24小时趋势计算完成，预览如下：")
    trend_df.show(24)

    print(" 正在将折线图数据写入 sys_load_trend 表...")
    trend_df.write \
        .format("jdbc") \
        .option("url", db_url) \
        .option("dbtable", "sys_load_trend") \
        .option("user", db_user) \
        .option("password", db_password) \
        .option("driver", db_driver) \
        .option("truncate", "true") \
        .mode("overwrite") \
        .save()

    print("🎉 完美！所有数据聚合完毕并成功入库！可以去刷新前端网页了。")
    spark.stop()

if __name__ == "__main__":
    main()