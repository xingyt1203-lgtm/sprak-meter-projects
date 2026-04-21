import os
import subprocess

# 强制注入 Java 环境变量，防止底层报错
os.environ["JAVA_HOME"] = "/usr/lib/jvm/java-8-openjdk-amd64"

from pyspark.sql import SparkSession, Window
from pyspark.sql import functions as F
from pyspark.ml.clustering import KMeans
from pyspark.ml.feature import VectorAssembler, StandardScaler

# 本地 Maven 缓存中的 MySQL JDBC 驱动（与后端 pom 版本保持一致）
MYSQL_JDBC_JAR = "/root/.m2/repository/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar"

# 初始化 SparkSession
spark_builder = SparkSession.builder.appName("SmartMeterAdvanced").master("local[*]")
if os.path.exists(MYSQL_JDBC_JAR):
    spark_builder = spark_builder.config("spark.jars", MYSQL_JDBC_JAR)
spark = spark_builder.getOrCreate()
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
).fillna({"std_usage": 0.0})

# 步骤 2.3：计算每日 Z-Score，揪出异常波动
# 业务逻辑：如果某天的用电量出现极大幅度的异常下跌，Z-Score 会是一个很大的负数。
anomaly_df = daily_df.join(user_profile, "meter_id") \
    .withColumn(
        "z_score",
        F.when(F.col("std_usage") <= 0, F.lit(0.0))
         .otherwise(F.round((F.col("daily_usage") - F.col("avg_usage")) / F.col("std_usage"), 2))
    ) \
    .withColumn(
        "is_suspect", 
        F.when(F.col("z_score") < -2.0, 1).otherwise(0)  # Z-Score 小于 -2.0 视为异常骤降（窃电嫌疑）
    )

# 提取并展示嫌疑用户名单
suspects = anomaly_df.filter(F.col("is_suspect") == 1).orderBy("z_score")

full_anomaly_df = anomaly_df.select(
    F.col("meter_id"),
    F.col("date").alias("detect_date"),
    F.col("daily_usage"),
    F.col("avg_usage"),
    F.col("z_score"),
    F.col("is_suspect").cast("int"),
)

print("\n 发现以下窃电嫌疑行为记录 (用电量发生极其异常的骤降):")
suspects.select("meter_id", "date", "daily_usage", "avg_usage", "z_score").show(10)


def assign_cluster_names(cluster_stats_rows):
    """Map numeric cluster labels to business-readable profile names."""
    if not cluster_stats_rows:
        return {}

    stats = [
        {
            "cluster_label": int(r["cluster_label"]),
            "avg_daily_usage": float(r["avg_daily_usage"] or 0.0),
            "night_day_ratio": float(r["night_day_ratio"] or 0.0),
            "power_volatility": float(r["power_volatility"] or 0.0),
        }
        for r in cluster_stats_rows
    ]

    name_map = {}
    remaining = set(s["cluster_label"] for s in stats)

    def pick_max(metric):
        candidates = [s for s in stats if s["cluster_label"] in remaining]
        if not candidates:
            return None
        return max(candidates, key=lambda x: x[metric])["cluster_label"]

    high_label = pick_max("avg_daily_usage")
    if high_label is not None:
        name_map[high_label] = "持续高耗能型"
        remaining.discard(high_label)

    night_label = pick_max("night_day_ratio")
    if night_label is not None:
        name_map[night_label] = "夜间高耗能型"
        remaining.discard(night_label)

    volatile_label = pick_max("power_volatility")
    if volatile_label is not None:
        name_map[volatile_label] = "波动敏感型"
        remaining.discard(volatile_label)

    for label in remaining:
        name_map[label] = "常规平稳型"

    return name_map


def ensure_is_suspect_column():
    check_cmd = (
        "mysql -N -uroot -p123456 spark_db -e "
        "\"SELECT COUNT(*) FROM information_schema.COLUMNS "
        "WHERE TABLE_SCHEMA='spark_db' AND TABLE_NAME='anomaly_records' AND COLUMN_NAME='is_suspect'\""
    )
    try:
        count = subprocess.check_output(check_cmd, shell=True, text=True).strip()
        if count == "0":
            subprocess.run(
                "mysql -uroot -p123456 spark_db -e \"ALTER TABLE anomaly_records ADD COLUMN is_suspect TINYINT DEFAULT 0\"",
                shell=True,
                check=False,
            )
    except Exception:
        # 结构修复失败时，后续写入会抛错并在主流程日志里可见
        pass


# ==========================================
# 核心模块 3：用户用电行为画像聚类 (KMeans)
# ==========================================
print("\n [模块 3] 正在执行用户行为聚类 (Spark ML KMeans)...")

time_df = cleaned_df.withColumn("event_time", F.to_timestamp("timestamp")) \
    .withColumn("hour", F.hour("event_time"))

meter_daily_usage = time_df.withColumn("date", F.to_date("event_time")) \
    .groupBy("meter_id", "date") \
    .agg(F.round(F.sum("power_cleaned"), 4).alias("daily_usage"))

meter_base_features = meter_daily_usage.groupBy("meter_id").agg(
    F.round(F.avg("daily_usage"), 4).alias("avg_daily_usage"),
    F.round(F.stddev("daily_usage"), 4).alias("daily_usage_std")
)

meter_hour_features = time_df.groupBy("meter_id").agg(
    F.round(F.avg(F.when(F.col("hour").between(0, 5), F.col("power_cleaned"))), 4).alias("night_avg_power"),
    F.round(F.avg(F.when(F.col("hour").between(9, 17), F.col("power_cleaned"))), 4).alias("day_avg_power"),
    F.round(F.avg(F.when(F.col("hour").between(18, 22), F.col("power_cleaned"))), 4).alias("peak_avg_power"),
    F.round(F.stddev("power_cleaned"), 4).alias("power_volatility")
)

feature_df = meter_base_features.join(meter_hour_features, "meter_id", "inner").fillna(0.0)

feature_df = feature_df.withColumn(
    "night_day_ratio",
    F.round((F.col("night_avg_power") + F.lit(0.01)) / (F.col("day_avg_power") + F.lit(0.01)), 4)
).withColumn(
    "peak_valley_ratio",
    F.round((F.col("peak_avg_power") + F.lit(0.01)) / (F.col("night_avg_power") + F.lit(0.01)), 4)
)

feature_cols = [
    "avg_daily_usage",
    "daily_usage_std",
    "night_day_ratio",
    "peak_valley_ratio",
    "power_volatility",
]

assembler = VectorAssembler(inputCols=feature_cols, outputCol="features")
assembled_df = assembler.transform(feature_df)

scaler = StandardScaler(inputCol="features", outputCol="scaled_features", withStd=True, withMean=True)
scaler_model = scaler.fit(assembled_df)
scaled_df = scaler_model.transform(assembled_df)

kmeans = KMeans(k=3, seed=42, featuresCol="scaled_features", predictionCol="cluster_label")
km_model = kmeans.fit(scaled_df)
clustered_df = km_model.transform(scaled_df)

cluster_stats = clustered_df.groupBy("cluster_label").agg(
    F.avg("avg_daily_usage").alias("avg_daily_usage"),
    F.avg("night_day_ratio").alias("night_day_ratio"),
    F.avg("power_volatility").alias("power_volatility")
).collect()

label_name_map = assign_cluster_names(cluster_stats)
map_expr = F.create_map([F.lit(x) for kv in label_name_map.items() for x in kv])

cluster_result_df = clustered_df.select("meter_id", F.col("cluster_label").cast("int")) \
    .withColumn("cluster_name", F.coalesce(map_expr[F.col("cluster_label")], F.lit("常规平稳型"))) \
    .withColumn("analyze_date", F.current_date())

print("\n 用户聚类画像示例:")
cluster_result_df.show(10, truncate=False)

# ==========================================
# 核心模块 4：同步结果到数据库
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

    # 异常表改为“全量样本 + 标签”，便于后续质检和阈值调优
    ensure_is_suspect_column()
    subprocess.run(
        "mysql -uroot -p123456 spark_db -e \"DELETE FROM anomaly_records\"",
        shell=True,
        check=False,
    )

    full_anomaly_df.write.jdbc(url=url, table="anomaly_records", mode="append", properties=properties)

    # 聚类结果一般按批次全量替换，避免重复堆积
    subprocess.run(
        "mysql -uroot -p123456 spark_db -e \"DELETE FROM cluster_result\"",
        shell=True,
        check=False,
    )

    cluster_result_df.write.jdbc(url=url, table="cluster_result", mode="append", properties=properties)
    print("✅ 同步完成！全量异常样本（含标签）和聚类画像已写入数据库。")
except Exception as e:
    print(f"❌ 写入失败，错误信息: {e}")

spark.stop()
