from flask import Flask, jsonify
import random

app = Flask(__name__)

@app.route('/api/v1/predict/tomorrow', methods=['GET'])
def predict_tomorrow():
    # 模拟 24 小时的预测数据 (单位: MW)
    # 正常用电曲线：凌晨低，白天逐渐升高，傍晚达到峰值，深夜回落
    hourly_loads = []
    for hour in range(24):
        if 0 <= hour <= 5:
            base = 1500
        elif 6 <= hour <= 10:
            base = 2500
        elif 11 <= hour <= 16:
            base = 3500
        elif 17 <= hour <= 21:
            base = 4000
        else:
            base = 2000
        
        # 添加一点随机波动
        load = base + random.uniform(-100, 100)
        hourly_loads.append(round(load, 2))
    
    return jsonify({
        "code": 200,
        "message": "success",
        "hourly_loads": hourly_loads
    })

if __name__ == '__main__':
    # 运行在 8000 端口，对应 Java 后端的 AiForecastService
    app.run(host='0.0.0.0', port=8000)
