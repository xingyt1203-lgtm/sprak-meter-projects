from pyspark.sql import SparkSession
from pyspark.sql.functions import sum, avg, stddev, col, lit, current_date, udf
from pyspark.sql.types import StringType
from pyspark.ml.feature import VectorAssembler, StandardScaler
from pyspark.ml.clustering import KMeans

# ==========================================
# 1. 初始化 Spark 环境与数据库配置
# =========================================
print(" 正在启动 Spark 聚类分析引擎...")
spark = SparkSession.builder \
    .appName("SmartMeter_KMeans_Clustering") \
    .config("spark.jars.packages", "mysql:mysql-connector-java:8.0.32") \
    .getOrCreate() # 请确保你的 mysql jar 包路径正确

# MySQL 数据库连接配置
jdbc_url = "jdbc:mysql://localhost:3306/spark_db?useSSL=false&characterEncoding=utf8"
connection_properties = {
    "user": "root",      
    "password": "123456",  
    "driver": "com.mysql.cj.jdbc.Driver"
}

# ==========================================
# 2. 读取 MySQL 中的原始电表数据
# ==========================================
print(" 正在从 MySQL 读取电表原始数据...")
raw_df = spark.read.jdbc(url=jdbc_url, table="meter_data_raw", properties=connection_properties)

# ==========================================
# 3. 特征工程 (Feature Engineering) - 降维与特征提取
# ==========================================
print(" 正在进行特征提取 (总用电量, 平均用电量, 波动率)...")
feature_df = raw_df.groupBy("meter_id").agg(
    sum("power_usage").alias("total_usage"),
    avg("power_usage").alias("avg_usage"),
    stddev("power_usage").alias("usage_stddev")
).na.fill(0) # 填充空值为0，防止计算报错

# 将三个特征组合成一个向量列 'features'
assembler = VectorAssembler(
    inputCols=["total_usage", "avg_usage", "usage_stddev"],
    outputCol="raw_features"
)
vector_df = assembler.transform(feature_df)

# 数据标准化 (StandardScaler) - K-Means 对数据量级很敏感，必须做标准化！
scaler = StandardScaler(inputCol="raw_features", outputCol="features", withStd=True, withMean=True)
scaler_model = scaler.fit(vector_df)
scaled_df = scaler_model.transform(vector_df)

# ==========================================
# 4. K-Means 聚类模型训练
# ==========================================
print(" 正在训练 K-Means 模型 (K=3)...")
k = 3 # 我们假设把用户分为 3 大类
kmeans = KMeans(featuresCol="features", predictionCol="cluster_label", k=k, seed=42)
model = kmeans.fit(scaled_df)

# 进行预测，打上类别标签
predictions = model.transform(scaled_df)

# ==========================================
# 5. 结果后处理与回写 MySQL
# ==========================================
print(" 正在解析聚类结果并写入数据库...")

# 定义一个 UDF (用户自定义函数)，给冷冰冰的数字打上人类能看懂的标签
def assign_cluster_name(label):
    if label == 0:
        return "常规平稳型"
    elif label == 1:
        return "高耗能型"
    else:
        return "极度波动型"

cluster_name_udf = udf(assign_cluster_name, StringType())

# 提取最终需要存入数据库的列，并加上当天日期
final_result_df = predictions.select(
    col("meter_id"),
    col("cluster_label"),
    cluster_name_udf(col("cluster_label")).alias("cluster_name")
).withColumn("analyze_date", current_date())

# 打印一下结果预览
final_result_df.show(10, truncate=False)

# 将结果以追加模式 (append) 写入 MySQL 的 cluster_result 表中
final_result_df.write.jdbc(
    url=jdbc_url, 
    table="cluster_result", 
    mode="append", 
    properties=connection_properties
)

print("聚类分析任务完成！数据已成功存入 MySQL 的 cluster_result 表！")
spark.stop()