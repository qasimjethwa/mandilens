package in.mandilens;

import in.mandilens.Forecasters.Forecaster;

/**
 * Walk-forward backtest: for each of the last {@code testDays} days, train on
 * everything before it, forecast 1 day ahead, and score the error. No
 * look-ahead — the model never sees the day it predicts.
 */
public final class Backtester {
    private Backtester() {}

    public static BacktestResult evaluate(Forecaster model, double[] series, int testDays) {
        int test = Math.min(testDays, series.length - 30);
        double ae = 0, se = 0, ape = 0;
        int n = 0;
        for (int i = series.length - test; i < series.length; i++) {
            double[] train = new double[i];
            System.arraycopy(series, 0, train, 0, i);
            double pred = model.forecast(train, 1)[0];
            double actual = series[i];
            double err = Math.abs(pred - actual);
            ae += err;
            se += err * err;
            ape += err / actual;
            n++;
        }
        return new BacktestResult(model.name(), ae / n, Math.sqrt(se / n), 100 * ape / n);
    }
}
