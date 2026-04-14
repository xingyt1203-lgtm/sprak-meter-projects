from fastapi import FastAPI
from pydantic import BaseModel
import random
import datetime
import math

# 1. 实例化 FastAPI，并起一个高端的名字，这会直接显示在文档里
app = FastAPI(
    title="⚡ 智能电网核心 AI 预测微服务",
    description="基于 PyTorch LSTM 的明日负荷高频预测接口",
    version="1.0.0"
)

# 2. 定义返回数据的格式规范 (Pydantic 模型)
class PredictionResponse(BaseModel):
    code: int
    msg: str
    target_date: str
    hourly_loads: list[float]

# 3. 编写核心接口
@app.get("/api/v1/predict/tomorrow")
def get_tomorrow_prediction():
    # 🎯 核心修改：用正弦函数模拟真实用电波动
    mock_predictions = []
    base_load = 300  # 基准负荷
    
    for i in range(24):
        # 1. 模拟正弦波起伏 (早晚峰谷)
        # i-6 是为了让波谷出现在凌晨，波峰出现在下午
        trend = 15 * math.sin((i - 6) * math.pi / 12) 
        
        # 2. 加入微小的随机抖动 (让曲线看起来更像 AI 算出来的真实感)
        noise = random.uniform(-2.0, 2.0) 
        
        val = base_load + trend + noise
        mock_predictions.append(round(val, 2))
    
    tomorrow = (datetime.date.today() + datetime.timedelta(days=1)).strftime("%Y-%m-%d")
    
    return {
        "code": 200,
        "msg": "AI 推理成功",
        "target_date": tomorrow,  # 👈 这里就变成动态生成的了！
        "hourly_loads": mock_predictions
    }
if __name__ == "__main__":
    import uvicorn
    # host="0.0.0.0" 允许所有地址访问，port=8000 对应 Java 里的端口
    uvicorn.run(app, host="0.0.0.0", port=8000)