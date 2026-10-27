/* MandiLens forecasting models — a 1:1 port of src/in/mandilens/Forecasters.java
   + Backtester.java, so the browser demo runs the identical models with no backend. */
"use strict";

const Forecasters = {
  seasonalNaive: {
    name: "Seasonal Naive (7d)",
    forecast(h, horizon) {
      const f = [];
      for (let i = 0; i < horizon; i++) {
        const idx = Math.max(0, h.length - 7 + (i % 7));
        f.push(h[idx]);
      }
      return f;
    },
  },
  movingAverage: {
    name: "Moving Average (7d)",
    forecast(h, horizon) {
      const n = Math.min(7, h.length);
      const mean = h.slice(h.length - n).reduce((a, b) => a + b, 0) / n;
      return Array(horizon).fill(mean);
    },
  },
  linearTrend: {
    name: "Linear Trend (30d)",
    forecast(h, horizon) {
      const n = Math.min(30, h.length);
      let sx = 0, sy = 0, sxx = 0, sxy = 0;
      for (let i = 0; i < n; i++) {
        const x = i, y = h[h.length - n + i];
        sx += x; sy += y; sxx += x * x; sxy += x * y;
      }
      const denom = n * sxx - sx * sx;
      const slope = denom === 0 ? 0 : (n * sxy - sx * sy) / denom;
      const intercept = (sy - slope * sx) / n;
      const f = [];
      for (let i = 0; i < horizon; i++) f.push(intercept + slope * (n + i));
      return f;
    },
  },
  ensemble: {
    name: "Ensemble (median of 3)",
    forecast(h, horizon) {
      const fs = [
        Forecasters.seasonalNaive.forecast(h, horizon),
        Forecasters.movingAverage.forecast(h, horizon),
        Forecasters.linearTrend.forecast(h, horizon),
      ];
      const f = [];
      for (let i = 0; i < horizon; i++) {
        const v = [fs[0][i], fs[1][i], fs[2][i]].sort((a, b) => a - b);
        f.push(v[1]);
      }
      return f;
    },
  },
};
const MODEL_KEYS = ["seasonalNaive", "movingAverage", "linearTrend", "ensemble"];

/* Walk-forward backtest: last `testDays` days, 1-day-ahead, no look-ahead. */
function backtest(model, series, testDays = 21) {
  const test = Math.min(testDays, series.length - 30);
  let ae = 0, se = 0, ape = 0, n = 0;
  for (let i = series.length - test; i < series.length; i++) {
    const pred = model.forecast(series.slice(0, i), 1)[0];
    const actual = series[i];
    const err = Math.abs(pred - actual);
    ae += err; se += err * err; ape += err / actual; n++;
  }
  return { mae: ae / n, rmse: Math.sqrt(se / n), mape: (100 * ape) / n };
}

function bestModel(series) {
  let best = null;
  for (const k of MODEL_KEYS) {
    const r = backtest(Forecasters[k], series);
    if (!best || r.mape < best.mape) best = { key: k, name: Forecasters[k].name, ...r };
  }
  return best;
}

/* 7-day forecast with a 95% band from backtest residual spread. */
function forecast7(prices, dates) {
  const best = bestModel(prices);
  const point = Forecasters[best.key].forecast(prices, 7);
  const half = 1.96 * best.rmse;
  const last = new Date(dates[dates.length - 1] + "T00:00:00");
  const fDates = [], low = [], high = [];
  let bestIdx = 0;
  for (let i = 0; i < 7; i++) {
    const d = new Date(last);
    d.setDate(d.getDate() + i + 1);
    fDates.push(d.toISOString().slice(0, 10));
    low.push(Math.max(0, point[i] - half));
    high.push(point[i] + half);
    if (point[i] > point[bestIdx]) bestIdx = i;
  }
  return {
    model: best, point, low, high, dates: fDates,
    bestIdx, bestDate: fDates[bestIdx], bestPrice: point[bestIdx],
    today: prices[prices.length - 1],
    gain: point[bestIdx] - prices[prices.length - 1],
  };
}
