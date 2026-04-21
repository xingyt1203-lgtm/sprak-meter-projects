from __future__ import annotations

import csv
import os
import statistics
import subprocess
from collections import defaultdict
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Tuple

from flask import Flask, jsonify, request

try:
    import torch
    import torch.nn as nn
except ImportError:
    torch = None
    nn = None

app = Flask(__name__)


BASE_DIR = Path(__file__).resolve().parent
DATA_CSV_PATH = BASE_DIR / "meter_data.csv"
MODEL_DIR = BASE_DIR / "models"
MODEL_PATH = MODEL_DIR / "load_lstm.pt"
WINDOW_SIZE = 24
FORECAST_STEPS = 24
MYSQL_DB = "spark_db"
MYSQL_USER = "root"
MYSQL_PASSWORD = "123456"


def _safe_float(text: str, default: float = 0.0) -> float:
    try:
        return float(text)
    except (TypeError, ValueError):
        return default


def _run_mysql(sql: str) -> str:
    cmd = [
        "mysql",
        f"-u{MYSQL_USER}",
        f"-p{MYSQL_PASSWORD}",
        MYSQL_DB,
        "-N",
        "-e",
        sql,
    ]
    out = subprocess.check_output(cmd, text=True)
    return out.strip()


def _iter_csv_rows(csv_path: Path):
    with csv_path.open("r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            yield row


def load_hourly_series(csv_path: Path) -> List[float]:
    """Aggregate hourly total load from historical meter_data.csv."""
    if not csv_path.exists():
        raise FileNotFoundError(f"未找到数据文件: {csv_path}")

    hourly_buckets = defaultdict(float)

    for row in _iter_csv_rows(csv_path):
        ts_raw = row.get("timestamp", "")
        if not ts_raw:
            continue
        try:
            ts = datetime.strptime(ts_raw, "%Y-%m-%d %H:%M:%S")
        except ValueError:
            continue

        # 使用 active_power，若缺失则回退为 voltage * current / 1000
        active_power = _safe_float(row.get("active_power"))
        if active_power <= 0:
            voltage = _safe_float(row.get("voltage"))
            current = _safe_float(row.get("current"))
            active_power = voltage * current / 1000.0

        key = ts.strftime("%Y-%m-%d %H:00")
        hourly_buckets[key] += active_power

    ordered_keys = sorted(hourly_buckets.keys())
    series = [hourly_buckets[k] for k in ordered_keys]
    if len(series) < WINDOW_SIZE + FORECAST_STEPS:
        raise ValueError("历史数据点太少，无法训练 LSTM 模型")
    return series


def build_supervised_dataset(series: List[float], window_size: int) -> Tuple[List[List[float]], List[float]]:
    xs: List[List[float]] = []
    ys: List[float] = []
    for i in range(window_size, len(series)):
        xs.append(series[i - window_size:i])
        ys.append(series[i])
    return xs, ys


def build_hourly_trend_points(csv_path: Path) -> List[Tuple[str, float]]:
    """Build 24 hourly trend points in kW from CSV by averaging daily total load at each hour."""
    per_timestamp_total: Dict[str, float] = defaultdict(float)
    for row in _iter_csv_rows(csv_path):
        ts = row.get("timestamp", "")
        if not ts:
            continue
        active_power = _safe_float(row.get("active_power"))
        if active_power <= 0:
            v = _safe_float(row.get("voltage"))
            c = _safe_float(row.get("current"))
            active_power = v * c / 1000.0
        per_timestamp_total[ts] += active_power

    hour_sum = defaultdict(float)
    hour_count = defaultdict(int)
    for ts, total_kw in per_timestamp_total.items():
        hour = ts[11:13]
        hour_sum[hour] += total_kw
        hour_count[hour] += 1

    points: List[Tuple[str, float]] = []
    for h in range(24):
        hh = f"{h:02d}"
        avg_kw = hour_sum[hh] / hour_count[hh] if hour_count[hh] else 0.0
        points.append((f"{hh}:00", round(avg_kw, 2)))
    return points


def sync_sys_load_trend(points: List[Tuple[str, float]]) -> None:
    if not points:
        raise ValueError("趋势点为空，无法回写 sys_load_trend")

    statements = ["DELETE FROM sys_load_trend"]
    for time_point, load_value in points:
        statements.append(
            f"INSERT INTO sys_load_trend(time_point, load_value) VALUES ('{time_point}', {load_value})"
        )
    _run_mysql("; ".join(statements) + ";")


def inspect_data_quality(csv_path: Path) -> Dict[str, float]:
    rows = 0
    missing = 0
    rel_errors: List[float] = []
    energy_diffs: List[float] = []
    active_power_values: List[float] = []
    prev_energy_by_meter: Dict[str, float] = {}

    for row in _iter_csv_rows(csv_path):
        rows += 1
        meter_id = row.get("meter_id", "")
        ts = row.get("timestamp", "")
        v_raw = row.get("voltage", "")
        c_raw = row.get("current", "")
        ap_raw = row.get("active_power", "")
        e_raw = row.get("total_energy", "")
        if not (meter_id and ts and v_raw and c_raw and ap_raw and e_raw):
            missing += 1

        v = _safe_float(v_raw)
        c = _safe_float(c_raw)
        ap = _safe_float(ap_raw)
        e = _safe_float(e_raw)
        if ap > 0:
            active_power_values.append(ap)

        calc_kw = (v * c / 1000.0) if (v > 0 and c > 0) else 0.0
        if calc_kw > 0:
            rel_errors.append(abs(ap - calc_kw) / calc_kw)

        prev = prev_energy_by_meter.get(meter_id)
        if prev is not None:
            energy_diffs.append(abs((e - prev) - ap))
        prev_energy_by_meter[meter_id] = e

    # 3-sigma 异常比率（仅用于体检日志）
    outlier_ratio = 0.0
    if len(active_power_values) > 2:
        mu = statistics.mean(active_power_values)
        sigma = statistics.pstdev(active_power_values)
        if sigma > 1e-9:
            outliers = [x for x in active_power_values if abs((x - mu) / sigma) > 3.0]
            outlier_ratio = len(outliers) / len(active_power_values)

    return {
        "rows": float(rows),
        "missing_ratio": (missing / rows) if rows else 0.0,
        "unit_rel_err_avg": statistics.mean(rel_errors) if rel_errors else 0.0,
        "unit_rel_err_max": max(rel_errors) if rel_errors else 0.0,
        "energy_step_err_avg": statistics.mean(energy_diffs) if energy_diffs else 0.0,
        "energy_step_err_max": max(energy_diffs) if energy_diffs else 0.0,
        "outlier_ratio": outlier_ratio,
    }


def print_startup_quality_report(csv_path: Path) -> None:
    metrics = inspect_data_quality(csv_path)
    print("\n========== [Data Quality & Unit Check] ==========")
    print(f"rows={int(metrics['rows'])}, missing_ratio={metrics['missing_ratio']:.4%}")
    print(
        "unit_check(active_power vs voltage*current/1000): "
        f"avg_rel_err={metrics['unit_rel_err_avg']:.4%}, "
        f"max_rel_err={metrics['unit_rel_err_max']:.4%}"
    )
    print(
        "energy_consistency(total_energy step - active_power): "
        f"avg_abs_err={metrics['energy_step_err_avg']:.4f}, "
        f"max_abs_err={metrics['energy_step_err_max']:.4f}"
    )
    print(f"outlier_ratio(3-sigma on active_power)={metrics['outlier_ratio']:.4%}")
    print("=================================================\n")


def sync_trend_from_csv_on_startup(csv_path: Path) -> None:
    points = build_hourly_trend_points(csv_path)
    sync_sys_load_trend(points)
    values = [p[1] for p in points]
    print(
        "[Startup Sync] sys_load_trend 已按 meter_data.csv 重算并回写（口径: kW），"
        f"min={min(values):.2f}, max={max(values):.2f}, avg={statistics.mean(values):.2f}"
    )


class LoadLSTM(nn.Module):
    def __init__(self, hidden_size: int = 64, num_layers: int = 2):
        super().__init__()
        self.lstm = nn.LSTM(
            input_size=1,
            hidden_size=hidden_size,
            num_layers=num_layers,
            batch_first=True,
            dropout=0.1,
        )
        self.fc = nn.Linear(hidden_size, 1)

    def forward(self, x):
        out, _ = self.lstm(x)
        return self.fc(out[:, -1, :])


class LSTMPredictService:
    def __init__(self, model_path: Path, window_size: int = 24):
        self.model_path = model_path
        self.window_size = window_size
        self.v_min = 0.0
        self.v_max = 1.0
        self.model = None

    def _normalize(self, series: List[float]) -> List[float]:
        scale = max(self.v_max - self.v_min, 1e-6)
        return [(v - self.v_min) / scale for v in series]

    def _denormalize(self, value: float) -> float:
        return value * (self.v_max - self.v_min) + self.v_min

    def train_and_save(self, raw_series: List[float], epochs: int = 120):
        self.v_min = min(raw_series)
        self.v_max = max(raw_series)
        norm_series = self._normalize(raw_series)

        xs, ys = build_supervised_dataset(norm_series, self.window_size)
        x_tensor = torch.tensor(xs, dtype=torch.float32).unsqueeze(-1)
        y_tensor = torch.tensor(ys, dtype=torch.float32).unsqueeze(-1)

        model = LoadLSTM()
        criterion = nn.MSELoss()
        optimizer = torch.optim.Adam(model.parameters(), lr=0.01)

        model.train()
        for _ in range(epochs):
            optimizer.zero_grad()
            preds = model(x_tensor)
            loss = criterion(preds, y_tensor)
            loss.backward()
            optimizer.step()

        self.model = model
        self.model_path.parent.mkdir(parents=True, exist_ok=True)
        torch.save(
            {
                "state_dict": model.state_dict(),
                "window_size": self.window_size,
                "v_min": self.v_min,
                "v_max": self.v_max,
            },
            self.model_path,
        )

    def load_or_train(self, raw_series: List[float], force_retrain: bool = False):
        force_retrain = force_retrain or (os.getenv("FORCE_RETRAIN", "0") == "1")
        if self.model_path.exists() and not force_retrain:
            checkpoint = torch.load(self.model_path, map_location="cpu")
            self.window_size = int(checkpoint.get("window_size", self.window_size))
            self.v_min = float(checkpoint.get("v_min", 0.0))
            self.v_max = float(checkpoint.get("v_max", 1.0))
            model = LoadLSTM()
            model.load_state_dict(checkpoint["state_dict"])
            model.eval()
            self.model = model
            return

        self.train_and_save(raw_series)
        self.model.eval()

    def forecast_next(self, raw_series: List[float], steps: int = 24, force_retrain: bool = False) -> List[float]:
        if self.model is None:
            self.load_or_train(raw_series, force_retrain=force_retrain)
        elif force_retrain:
            self.train_and_save(raw_series)
            self.model.eval()

        norm_series = self._normalize(raw_series)
        seq = norm_series[-self.window_size:]
        outputs = []

        self.model.eval()
        with torch.no_grad():
            for _ in range(steps):
                x = torch.tensor(seq, dtype=torch.float32).view(1, self.window_size, 1)
                pred = float(self.model(x).item())
                # 防止极端训练漂移造成无效值
                pred = max(0.0, min(1.5, pred))
                seq = seq[1:] + [pred]
                outputs.append(round(max(0.0, self._denormalize(pred)), 2))

        return outputs


predict_service = LSTMPredictService(MODEL_PATH, window_size=WINDOW_SIZE)


@app.route('/api/v1/predict/tomorrow', methods=['GET'])
def predict_tomorrow():
    if torch is None:
        return jsonify({
            "code": 500,
            "message": "缺少 PyTorch 依赖，请先执行: pip install torch",
            "hourly_loads": [],
        }), 500

    try:
        retrain = request.args.get("retrain", "0") in ("1", "true", "True")
        torch.manual_seed(42)
        series = load_hourly_series(DATA_CSV_PATH)
        hourly_loads = predict_service.forecast_next(series, steps=FORECAST_STEPS, force_retrain=retrain)
        return jsonify({
            "code": 200,
            "message": "success",
            "retrain": retrain,
            "hourly_loads": hourly_loads,
        })
    except Exception as exc:
        return jsonify({
            "code": 500,
            "message": f"预测失败: {exc}",
            "hourly_loads": [],
        }), 500


if __name__ == '__main__':
    # 启动体检 + 重算趋势，避免库里趋势口径被手工 SQL 污染
    try:
        print_startup_quality_report(DATA_CSV_PATH)
    except Exception as exc:
        print(f"[Startup Check] 数据质量体检失败: {exc}")

    try:
        sync_trend_from_csv_on_startup(DATA_CSV_PATH)
    except Exception as exc:
        print(f"[Startup Sync] sys_load_trend 回写失败: {exc}")

    # 运行在 8000 端口，对应 Java 后端的 AiForecastService
    app.run(host='0.0.0.0', port=8000)
