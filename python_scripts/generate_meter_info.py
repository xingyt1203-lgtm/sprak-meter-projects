from pyspark.sql import SparkSession
# 🌟 注意这里：引入了 IntegerType
from pyspark.sql.types import StructType, StructField, StringType, DateType, IntegerType 
import random
from datetime import date

def main():
    spark = SparkSession.builder \
        .appName("GenerateMeterInfo") \
        .config("spark.jars", "mysql-connector-java-8.0.30.jar") \
        .getOrCreate()
    spark.sparkContext.setLogLevel("WARN")

    surnames = ["赵", "钱", "孙", "李", "周", "吴", "郑", "王", "陈", "林", "张"]
    names = ["伟", "芳", "娜", "敏", "静", "强", "磊", "洋", "艳", "勇", "军"]
    regions = ["朝阳区", "海淀区", "丰台区", "大兴区", "昌平区"]

    data = []
    for i in range(1, 51):
        meter_id = f"METER_{i:03d}"
        user_name = random.choice(surnames) + random.choice(names)
        address = random.choice(regions) + f"第{random.randint(1, 10)}街道{random.randint(10, 999)}号"
        meter_type = "三相智能电表" if random.random() > 0.8 else "单相智能电表"
        install_date = date(random.randint(2018, 2022), random.randint(1, 12), random.randint(1, 28))
        
        #  修复关键点：用 1 代表正常在线，0 代表离线 (95%的概率是在线)
        status = 1 if random.random() > 0.05 else 0
        
        data.append((meter_id, user_name, address, meter_type, install_date, status))

    #  修复关键点：把 status 的类型从 StringType 改成 IntegerType
    schema = StructType([
        StructField("meter_id", StringType(), True),
        StructField("user_name", StringType(), True),
        StructField("address", StringType(), True),
        StructField("meter_type", StringType(), True),
        StructField("install_date", DateType(), True),
        StructField("status", IntegerType(), True)
    ])

    df = spark.createDataFrame(data, schema)
    print("✅ 成功生成 50 条电表档案数据 (包含整数状态码)，预览如下：")
    df.show(5)

    db_url = "jdbc:mysql://localhost:3306/spark_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
    
    print("💾 正在写入 meter_info 表...")
    df.write.format("jdbc") \
        .option("url", db_url).option("dbtable", "meter_info") \
        .option("user", "root").option("password", "123456") \
        .option("driver", "com.mysql.cj.jdbc.Driver") \
        .option("truncate", "true").mode("overwrite").save()

    print("🎉 档案数据填充完毕！这次绝对不会报错了！")
    spark.stop()

if __name__ == "__main__":
    main()
