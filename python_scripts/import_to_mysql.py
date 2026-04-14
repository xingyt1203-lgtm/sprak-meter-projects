import pandas as pd
import pymysql
from sqlalchemy import create_engine

# ================= 1. 配置信息 =================
# 数据库连接配置 (注意修改密码)
DB_USER = 'root'
DB_PASS = '123456'  # <--- 改成你的密码
DB_HOST = 'localhost'
DB_NAME = 'spark_db' # 确保数据库名正确

# CSV文件路径
CSV_FILE = 'meter_data.csv'

# ================= 2. 开始导入 =================
try:
    print(f"正在读取 CSV 文件: {CSV_FILE} ...")
    # 读取CSV (如果文件很大，加上 chunksize)
    df = pd.read_csv(CSV_FILE)
    
    # 统一字段名，确保和数据库表 meter_data_raw 对应
    # 假设你的CSV列名是: meter_id, district, record_time, usage_kwh
    # 假设你的数据库列名是: meter_id, record_time, power_usage (请根据实际情况调整)
    df = df.rename(columns={
        'usage_kwh': 'power_usage'
    })
    
    # 如果CSV里有多余的列(比如district)，但数据库表里没有，需要删掉
    # df = df.drop(columns=['district']) 

    print(f"读取成功，共 {len(df)} 条记录。正在连接数据库...")
    
    # 使用 sqlalchemy 建立连接引擎，这种方式导入最快
    engine = create_engine(f"mysql+pymysql://{DB_USER}:{DB_PASS}@{DB_HOST}/{DB_NAME}?charset=utf8mb4")
    
    print("开始写入数据库 (meter_data_raw)，请稍候...")
    # if_exists='append' 表示追加在后面；如果想清空重来，用 'replace'
    df.to_sql('meter_data_raw', con=engine, if_exists='append', index=False)
    
    print("✅ 恭喜！全量原始数据已成功导入 meter_data_raw 表！")

except Exception as e:
    print(f"❌ 导入失败: {e}")