import os
# 指定 Java 路径，绕过 Linux 环境变量
os.environ["JAVA_HOME"] = "/usr/lib/jvm/java-8-openjdk-amd64"

from pyspark.sql import SparkSession
from pyspark.sql.functions import col, sum, round

# 1. 初始化 SparkSession
spark = SparkSession.builder \
    .appName("SmartMeterAnalysis") \
    .master("local[*]") \
    .getOrCreate()

# 设置日志级别为 WARN
spark.sparkContext.setLogLevel("WARN")

print(" Spark 引擎启动成功！正在加载数据...")

# 2. 读取 CSV 数据
df = spark.read.csv("meter_data.csv", header=True, inferSchema=True)

print("\n--- 数据结构 (Schema) ---")
df.printSchema()

print("\n--- 前 5 行数据预览 ---")
df.show(5)

# 3. 核心计算：按电表ID分组计算总耗电量
print("\n---  数据统计：每个电表的总耗电指标 ---")
summary_df = df.groupBy("meter_id") \
    .agg(round(sum("active_power"), 2).alias("total_active_power")) \
    .orderBy("meter_id")

summary_df.show(10)

spark.stop()
