import os
os.environ["JAVA_HOME"] = "/usr/lib/jvm/java-8-openjdk-amd64"

from pyspark.sql import SparkSession
from pyspark.sql import functions as F
from pyspark.ml.feature import VectorAssembler, StandardScaler
from pyspark.ml.clustering import KMeans

def main():
    # ================= 1. 初始化 Spark =================
    spark = SparkSession.builder \
        .appName("SmartMeterBehavior") \
        .master("local[*]") \
        .config("spark.jars", "mysql-connector-java-8.0.30.jar") \
        .getOrCreate()
    spark.sparkContext.setLogLevel("WARN")

    print("🚀 正在加载本地电表数据...")
    # 假设你的原始数据叫 meter_data.csv，如果有变化请修改这里
    df = spark.read.csv("meter_data.csv", header=True, inferSchema=True)

    # ================= 2. 核心步骤：特征工程 =================
    print("🧠 正在提取用户行为特征 (包含白天峰期占比)...")
    behavior_df = df.withColumn("hour", F.hour("timestamp")) \
        .withColumn("is_peak", F.when((F.col("hour") >= 8) & (F.col("hour") <= 22), 1).otherwise(0)) \
        .groupBy("meter_id").agg(
            F.round(F.sum("active_power"), 2).alias("total_usage"),
            F.round(F.max("active_power"), 2).alias("peak_load"),
            F.round(F.sum(F.col("active_power") * F.col("is_peak")) / F.sum("active_power"), 2).alias("peak_ratio")
        )

    # ================= 3. 机器学习流水线 =================
    print("🤖 正在执行 K-Means 聚类模型训练...")
    assembler = VectorAssembler(inputCols=["total_usage", "peak_load", "peak_ratio"], outputCol="features_vec")
    scaler = StandardScaler(inputCol="features_vec", outputCol="features")
    kmeans = KMeans(featuresCol="features", k=3, seed=42)

    pipeline_df = assembler.transform(behavior_df)
    scaled_data = scaler.fit(pipeline_df).transform(pipeline_df)
    model = kmeans.fit(scaled_data)
    results = model.transform(scaled_data)

    # ================= 4. 结果展示与翻译 (修改重点) =================
    print("\n✅ 聚类完成！正在为大屏生成业务画像...")
    
    # 将 0, 1, 2 翻译成前端需要的中文标签，并加上分析日期
    final_results = results.withColumn("cluster_name",
            F.when(F.col("prediction") == 0, "常规平稳型")
             .when(F.col("prediction") == 1, "持续高耗能型")
             .otherwise("极度波动型")
        ) \
        .withColumn("analyze_date", F.current_date()) \
        .withColumnRenamed("prediction", "cluster_label")

    # 打印前 15 条看看效果
    final_results.select("meter_id", "total_usage", "cluster_label", "cluster_name", "analyze_date").show(15)

    # ================= 5. 结果持久化：写入 MySQL (修改重点) =================
    print("\n💾 正在将用户画像结果同步至 MySQL 数据库...")

    # 配置数据库连接信息 (统一配置，防错)
    db_url = "jdbc:mysql://localhost:3306/spark_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
    db_user = "root"
    db_pass = "123456" 
    db_driver = "com.mysql.cj.jdbc.Driver"
    
    # ⚠️ 关键：这里改成你上一张截图里真实的表名！
    # 之前截图里有 id, meter_id, cluster_label, cluster_name, analyze_date 这些字段
    target_table = "user_clusters" 

    # 提取最终需要的列，并使用 overwrite 覆盖写入，防止出现 Duplicate entry 报错！
    final_results.select(
        "meter_id",
        "cluster_label",
        "cluster_name",
        "analyze_date"
    ).write \
        .format("jdbc") \
        .option("url", db_url) \
        .option("dbtable", target_table) \
        .option("user", db_user) \
        .option("password", db_pass) \
        .option("driver", db_driver) \
        .option("truncate", "true") \
        .mode("overwrite") \
        .save()

    print("🎉 成功覆写数据库！现在你可以去刷新前端网页，查看 50 个真实画像了！")
    spark.stop()

if __name__ == "__main__":
    main()