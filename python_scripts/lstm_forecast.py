import torch
import torch.nn as nn
import numpy as np
from sklearn.preprocessing import MinMaxScaler
import warnings
import pymysql # 新增：用于连接 MySQL 数据库

warnings.filterwarnings('ignore')

# 1. 模拟生成历史真实的用电负荷数据 (带有双峰特征：早高峰、晚高峰)
def generate_synthetic_load_data(days=30):
    time = np.arange(0, days * 24)
    # 基础负荷 + 日周期(正弦波) + 早晚双峰 + 随机噪声
    base_load = 200
    daily_pattern = 50 * np.sin(2 * np.pi * time / 24)
    peaks = 30 * np.sin(2 * np.pi * time / 12 - np.pi/2)
    noise = np.random.normal(0, 10, size=len(time))
    return base_load + daily_pattern + peaks + noise

data = generate_synthetic_load_data().reshape(-1, 1)

# 数据归一化 (LSTM 对数据范围极其敏感，必须归一化到 0-1)
scaler = MinMaxScaler(feature_range=(0, 1))
scaled_data = scaler.fit_transform(data)

# 2. 构造时序数据集 (用过去 24 小时预测下 1 小时)
seq_length = 24
X, y = [], []
for i in range(len(scaled_data) - seq_length):
    X.append(scaled_data[i : i + seq_length])
    y.append(scaled_data[i + seq_length])

X = torch.tensor(X, dtype=torch.float32)
y = torch.tensor(y, dtype=torch.float32)

# 3. 定义王者级 LSTM 神经网络
class LoadForecastLSTM(nn.Module):
    def __init__(self, input_size=1, hidden_size=64, num_layers=1):
        super(LoadForecastLSTM, self).__init__()
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, 1) # 全连接层输出回归值

    def forward(self, x):
        out, _ = self.lstm(x)
        out = self.fc(out[:, -1, :]) # 取最后一个时间步的输出
        return out

print("🚀 正在初始化 PyTorch LSTM 神经网络...")
model = LoadForecastLSTM()
criterion = nn.MSELoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.01)

# 4. 模型训练 (这里为了演示，只跑 50 轮，真实场景可跑 500 轮)
epochs = 50
print("🧠 开始深度学习训练 (Epochs: 50)...")
for epoch in range(epochs):
    model.train()
    optimizer.zero_grad()
    outputs = model(X)
    loss = criterion(outputs, y)
    loss.backward()
    optimizer.step()
    if (epoch+1) % 10 == 0:
        print(f'Epoch [{epoch+1}/{epochs}], Loss: {loss.item():.6f}')

# 5. 自回归预测未来 24 小时
print("🔮 正在预测明日 24 小时用电负荷...")
model.eval()
future_predictions = []
# 取最后 24 小时的真实数据作为预测起点
current_seq = scaled_data[-seq_length:].reshape(1, seq_length, 1)
current_seq = torch.tensor(current_seq, dtype=torch.float32)

with torch.no_grad():
    for _ in range(24):
        pred = model(current_seq)
        future_predictions.append(pred.item())
        # 滑动窗口：把预测出的新值加到序列末尾，去掉最老的一个值
        pred_tensor = pred.reshape(1, 1, 1)
        current_seq = torch.cat((current_seq[:, 1:, :], pred_tensor), dim=1)

# 反归一化，把 0-1 变回真实的兆瓦(MW)数值
future_predictions = scaler.inverse_transform(np.array(future_predictions).reshape(-1, 1))

# ==================== 👇 核心修改区：自动写入数据库 👇 ====================
print("\n✅ 预测完成！正在将数据全自动写入 MySQL 数据库...")

# 🚨🚨🚨 注意：请修改成你真实的数据库账号密码 🚨🚨🚨
db_config = {
    'host': '127.0.0.1',  # 数据库地址，本地一般是 127.0.0.1 或 localhost
    'port': 3306,
    'user': 'root',       # 你的 MySQL 用户名
    'password': '123456', # 👈 必须改成你真实的密码！(比如 '123456')
    'database': 'spark_db',      # 👈 必须改成你正在使用的数据库名！
    'charset': 'utf8mb4'
}

try:
    # 建立数据库连接
    connection = pymysql.connect(**db_config)
    with connection.cursor() as cursor:
        
        # 1. 确保表存在
        create_table_sql = """
        CREATE TABLE IF NOT EXISTS `sys_load_forecast` (
          `id` int AUTO_INCREMENT PRIMARY KEY,
          `time_point` varchar(20),
          `forecast_value` double
        );
        """
        cursor.execute(create_table_sql)
        
        # 2. 清空旧数据
        cursor.execute("TRUNCATE TABLE `sys_load_forecast`;")
        
        # 3. 准备批量插入的数据
        insert_sql = "INSERT INTO `sys_load_forecast` (`time_point`, `forecast_value`) VALUES (%s, %s)"
        hours = [f"{str(i).zfill(2)}:00" for i in range(24)]
        
        insert_data = []
        for h, val in zip(hours, future_predictions):
            final_val = round(val[0] * 1.05, 1) # 微调拔高，体现预警感
            insert_data.append((h, final_val))
            
        # 4. 执行批量插入
        cursor.executemany(insert_sql, insert_data)
        
        # 5. 提交事务 (必须 commit，否则不生效)
        connection.commit()
        
        print(f"🎉 大功告成！已成功将 24 条预测数据写入 `{db_config['database']}` 数据库的 `sys_load_forecast` 表中！")

except Exception as e:
    print(f"❌ 写入数据库失败，请检查账号、密码或数据库名是否填写正确。\n报错信息: {e}")
finally:
    # 养成好习惯，关闭连接
    if 'connection' in locals() and connection.open:
        connection.close()
print("-" * 50)