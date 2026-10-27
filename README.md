# MandiLens

**Crop price forecasting for Indian farmers — know tomorrow's mandi price before you load the truck.**

Farmers sell at a loss when prices crash *after* harvest. MandiLens runs four forecasting models over 120 days of mandi prices, picks the winner in a walk-forward backtest with no look-ahead, forecasts the next 7 days — and names the best day to sell.

**Live demo:** https://qasimjethwa.github.io/mandilens/ — pick a crop and mandi, see the forecast chart, the model shootout, and upload your own mandi CSV to forecast on real data. No backend; the Java models run in the browser.

## How it works

1. **Load prices** — daily modal prices per crop & mandi (`data/sample_mandi_prices.csv`, Agmarknet format: `crop,mandi,date,price_inr_per_quintal`).
2. **Four models race** — Seasonal Naive (7-day cycle), 7-day Moving Average, 30-day Linear Trend, and an Ensemble (median of all three).
3. **Backtest picks the winner** — each of the last 21 days predicted 1 day ahead using only data before that day. Lowest MAPE wins, per crop & mandi.
4. **Forecast 7 days** — with a 95% confidence band from the backtest error spread.
5. **Name the best day** — the highest forecast price, and what waiting is worth. If prices are falling, it says so: sell today.

The browser demo runs the **identical** models — `docs/forecast.js` is a 1:1 port of the Java engine, and the demo cross-checks its numbers against the Java output in `docs/metrics.json`.

## Measured performance

Walk-forward backtest, last 21 days, 1-day-ahead, no look-ahead. Best model per series (MAPE):

| Crop @ Mandi | Best model | MAPE | MAE (₹/qtl) |
|---|---|---|---|
| Onion @ Vashi | Moving Average (7d) | 6.7% | 157 |
| Onion @ Lasalgaon | Moving Average (7d) | 6.3% | 135 |
| Onion @ Azadpur | Moving Average (7d) | 5.5% | 134 |
| Potato @ Vashi | Moving Average (7d) | 5.5% | 69 |
| Potato @ Lasalgaon | Ensemble | 5.2% | 59 |
| Potato @ Azadpur | Moving Average (7d) | 5.0% | 73 |
| Tomato @ Vashi | Linear Trend (30d) | 22.1% | 188 |
| Tomato @ Lasalgaon | Linear Trend (30d) | 23.4% | 210 |
| Tomato @ Azadpur | Seasonal Naive (7d) | 21.7% | 192 |

Onion and potato forecast well (5–7% error). Tomato doesn't — it crashed mid-September in this data and stays volatile, which the wide confidence band shows honestly. A model that admits what it can't predict is the point.

## Quickstart

Requires Java 21+ (no other dependencies).

```bash
./build.sh                          # compile to out/
./run.sh list                       # crops & mandis in the data
./run.sh backtest                   # full model shootout for every series
./run.sh forecast onion Vashi      # 7-day forecast + best day to sell
./run.sh export                     # regenerate docs/metrics.json + docs/data.json
```

Use your own data — any CSV in Agmarknet format (real data: https://agmarknet.gov.in/):

```bash
./run.sh forecast tomato Azadpur my_prices.csv
./run.sh backtest my_prices.csv
```

## Data honesty

The bundled `data/sample_mandi_prices.csv` is **clearly-labelled illustrative sample data, not real market prices** — it exists so the pipeline runs end to end. Bring a real Agmarknet export (upload it in the demo, or pass it to the CLI) to forecast on live data.

## Project layout

```
src/in/mandilens/   Java engine (pure Java 21, zero dependencies)
  MandiLens.java    CLI: backtest | forecast | list | export
  Forecasters.java  the 4 models
  Backtester.java   walk-forward evaluation, no look-ahead
  ForecastResult.java  7-day forecast + best-sell day
  CsvLoader.java    Agmarknet-format CSV parsing
  PriceRecord.java  one daily modal price
data/               sample mandi prices (illustrative — see above)
docs/               GitHub Pages demo (index.html + forecast.js + app.js)
scripts/            sample-data generator
brag-output/        launch video
```

Built in a day, 2026-10-27.
