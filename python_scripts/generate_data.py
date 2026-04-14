import csv
import random
from datetime import datetime, timedelta

# --- 配置参数 ---
NUM_USERS = 50          # 模拟生成50个用户的电表
DAYS = 30               # 模拟30天的用电数据
START_DATE = datetime(2023, 1, 1) # 数据起始时间

def generate_data():
    # 创建并打开一个 CSV 文件用于写入
    with open('meter_data.csv', 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        # 写入表头
        writer.writerow(['meter_id', 'timestamp', 'voltage', 'current', 'active_power', 'total_energy'])

        for user_id in range(1, NUM_USERS + 1):
            # 关键步骤：为每个用户随机分配一种“用电行为画像”
            # resident: 普通居民 (早晚高峰)
            # commercial: 商业用户 (白天持续高负荷)
            # night_owl: 夜间活跃用户 (深夜负荷高)
            user_type = random.choice(['resident', 'commercial', 'night_owl'])
            
            # 初始化该电表的初始读数（总用电量）
            current_total_energy = random.uniform(1000.0, 5000.0) 

            for day in range(DAYS):
                for hour in range(24): # 每小时采集一次数据
                    timestamp = START_DATE + timedelta(days=day, hours=hour)
                    
                    # 模拟电压 (市电通常在 220V 上下波动)
                    voltage = random.uniform(215.0, 225.0)

                    # 根据不同的用户画像，模拟当前小时的有功功率 (kW)
                    if user_type == 'resident':
                        if 7 <= hour <= 9 or 18 <= hour <= 22:
                            active_power = random.uniform(1.5, 4.0) # 做饭、洗浴、看电视高峰
                        else:
                            active_power = random.uniform(0.1, 0.5) # 待机耗电
                    
                    elif user_type == 'commercial':
                        if 9 <= hour <= 21:
                            active_power = random.uniform(3.0, 10.0) # 营业时间，空调设备全开
                        else:
                            active_power = random.uniform(0.2, 0.8) # 关门歇业
                    
                    else: # night_owl (夜猫子/特殊作业)
                        if 0 <= hour <= 6 or 20 <= hour <= 23:
                            active_power = random.uniform(2.0, 5.0) # 深夜高耗电
                        else:
                            active_power = random.uniform(0.1, 0.5)

                    # 根据 P = UI 计算电流 (I = P*1000 / U)
                    current = (active_power * 1000) / voltage
                    
                    # 累加总用电量 (功率 * 1小时 = 用电度数)
                    current_total_energy += active_power

                    # 写入一行数据
                    writer.writerow([
                        f'METER_{user_id:03d}',
                        timestamp.strftime('%Y-%m-%d %H:%M:%S'),
                        round(voltage, 2),
                        round(current, 2),
                        round(active_power, 2),
                        round(current_total_energy, 2)
                    ])

if __name__ == '__main__':
    print(" 开始模拟生成智能电表数据...")
    generate_data()
    print(" 数据生成完毕！已在当前目录保存为 meter_data.csv")