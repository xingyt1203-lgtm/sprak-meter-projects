import pymysql
import random
from datetime import datetime, timedelta

print("智能电表模拟数据发生器启动...")

# ==========================================
# 1. 数据库连接配置 (请确认密码是否正确)
# ==========================================
db_config = {
    'host': 'localhost',
    'user': 'root',       # 替换为你的 MySQL 账号
    'password': '123456',   # 替换为你的 MySQL 密码
    'database': 'spark_db',
    'charset': 'utf8mb4'
}

# ==========================================
# 2. 模拟参数设置
# ==========================================
start_date = datetime(2023, 1, 1)  # 从1月1日开始模拟
days_to_simulate = 30              # 模拟30天的数据

# 定义 3 种类型的电表
meters = [
    {"id": f"METER_{str(i).zfill(3)}", "type": "正常平稳型", "base": 10, "fluctuation": 2} for i in range(1, 6)
] + [
    {"id": f"METER_{str(i).zfill(3)}", "type": "持续高耗能型", "base": 45, "fluctuation": 5} for i in range(6, 9)
] + [
    {"id": f"METER_{str(i).zfill(3)}", "type": "极度波动异常型", "base": 15, "fluctuation": 20} for i in range(9, 11)
]

# ==========================================
# 3. 生成并插入数据
# ==========================================
try:
    conn = pymysql.connect(**db_config)
    cursor = conn.cursor()
    
    # 先清空旧数据（防止重复运行导致数据堆积）
    cursor.execute("TRUNCATE TABLE meter_data_raw;")
    conn.commit()
    print("已清空旧的原始数据表...")

    insert_sql = """
        INSERT INTO meter_data_raw (meter_id, record_time, power_usage, voltage, current)
        VALUES (%s, %s, %s, %s, %s)
    """
    
    total_records = 0
    print("正在生成并写入 30 天的流水数据，请稍候...")
    
    # 遍历每一天
    for day_offset in range(days_to_simulate):
        current_date = start_date + timedelta(days=day_offset)
        
        # 遍历每一个电表
        for meter in meters:
            # 每天生成 24 条记录（每小时一条）
            for hour in range(24):
                record_time = current_date + timedelta(hours=hour)
                
                # 根据不同类型的电表，生成不同的用电量特征
                # max(0, xxx) 是为了保证用电量不会出现负数
                if meter["type"] == "极度波动异常型" and random.random() < 0.3:
                    # 30% 的概率用电量骤降为 0 或者极低（模拟异常）
                    usage = max(0, random.gauss(2, 1)) 
                else:
                    # 正常按正态分布波动
                    usage = max(0, random.gauss(meter["base"] / 24, meter["fluctuation"] / 24))
                
                # 模拟电压和电流（稍微波动即可）
                voltage = random.gauss(220, 2)
                current_val = usage * 1000 / voltage if usage > 0 else 0
                
                cursor.execute(insert_sql, (meter["id"], record_time, round(usage, 4), round(voltage, 2), round(current_val, 2)))
                total_records += 1

    conn.commit()
    print(f" 大功告成！成功生成了 {len(meters)} 个电表，共计 {total_records} 条高质量模拟数据！")
    print("💡 你现在可以去运行 Spark K-Means 聚类代码了！")

except Exception as e:
    print(f" 数据库操作失败: {e}")
finally:
    if 'conn' in locals() and conn.open:
        cursor.close()
        conn.close()