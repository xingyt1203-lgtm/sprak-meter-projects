import os
# 强制注入 Java 环境变量，防止底层报错
os.environ["JAVA_HOME"] = "/usr/lib/jvm/java-8-openjdk-amd64"

from pyspark.sql import SparkSession, Window
from pyspark.sql import functions as F

# 初始化 SparkSession
spark = SparkSession.builder.appName("SmartMeterAdvanced").master("local[*]").getOrCreate()
spark.sparkContext.setLogLevel("WARN")

print(" 正在加载原始电表数据...")
# 假设这是我们包含空值/异常值的原始数据
df = spark.read.csv("meter_data.csv", header=True, inferSchema=True)

# ==========================================
# 核心模块 1：缺失数据修复 (Data Imputation)
# ==========================================
print("\n[模块 1] 正在执行缺失数据修复 (基于前后时序滑动窗口均值)...")

# 定义一个时间窗口：按电表 ID 分组，按时间排序，取当前行的前1行到后1行
window_rolling = Window.partitionBy("meter_id").orderBy("timestamp").rowsBetween(-1, 1)

# 如果 active_power 是空值，则用前后相邻时间的平均值填充；否则保留原值
cleaned_df = df.withColumn(
    "power_cleaned",
    F.when(F.col("active_power").isNull(), F.round(F.avg("active_power").over(window_rolling), 2))
     .otherwise(F.col("active_power"))
)

# 展示修复前后的对比（假设有空值的话）
# cleaned_df.select("meter_id", "timestamp", "active_power", "power_cleaned").show(5)

# ==========================================
# 核心模块 2：窃电嫌疑户自动发现 (Anomaly Detection)
# ==========================================
print("\n [模块 2] 正在执行窃电嫌疑分析 (基于 3-Sigma 准则的 Z-Score 算法)...")

# 步骤 2.1：计算每个电表每天的日用电量
daily_df = cleaned_df.withColumn("date", F.to_date("timestamp")) \
    .groupBy("meter_id", "date") \
    .agg(F.round(F.sum("power_cleaned"), 2).alias("daily_usage"))

# 步骤 2.2：计算每个用户的“历史行为基线”（平均日用电量 & 标准差）
user_profile = daily_df.groupBy("meter_id").agg(
    F.round(F.avg("daily_usage"), 2).alias("avg_usage"),
    F.round(F.stddev("daily_usage"), 2).alias("std_usage")
)

# 步骤 2.3：计算每日 Z-Score，揪出异常波动
# 业务逻辑：如果某天的用电量出现极大幅度的异常下跌，Z-Score 会是一个很大的负数。
anomaly_df = daily_df.join(user_profile, "meter_id") \
    .withColumn("z_score", F.round((F.col("daily_usage") - F.col("avg_usage")) / F.col("std_usage"), 2)) \
    .withColumn(
        "is_suspect", 
        F.when(F.col("z_score") < -2.0, 1).otherwise(0)  # Z-Score 小于 -2.0 视为异常骤降（窃电嫌疑）
    )

# 提取并展示嫌疑用户名单
suspects = anomaly_df.filter(F.col("is_suspect") == 1).orderBy("z_score")

print("\n 发现以下窃电嫌疑行为记录 (用电量发生极其异常的骤降):")
suspects.select("meter_id", "date", "daily_usage", "avg_usage", "z_score").show(10)

# ==========================================
# 核心模块 3：同步结果到数据库
# ==========================================
print("\n🚀 正在尝试写入数据库...")
# 建议：如果还是卡住，把上面的 .show() 注释掉，直接跑下面这段
url = "jdbc:mysql://localhost:3306/spark_db?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf-8"
properties = {
    "user": "root",
    "password": "123456",
    "driver": "com.mysql.cj.jdbc.Driver"
}

try:
    # 写入前先打印一下，确认进入了逻辑
    print("⏳ 连接 MySQL 中...")
    suspects.select(
        F.col("meter_id"), 
        F.col("date").alias("detect_date"), 
        F.col("daily_usage"), 
        F.col("avg_usage"), 
        F.col("z_score")
    ).write.jdbc(url=url, table="anomaly_records", mode="append", properties=properties)
    print("✅ 同步完成！现在可以去 Web 界面查看了。")
except Exception as e:
    print(f"❌ 写入失败，错误信息: {e}")

spark.stop()
