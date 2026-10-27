package in.mandilens;

import java.util.Arrays;

/**
 * Forecasting models for a daily price series.
 * Every model is pure Java, dependency-free, and deterministic — the same
 * maths is ported 1:1 to {@code docs/forecast.js} so the browser demo runs
 * the identical models without a backend.
 */
public final class Forecasters {

    private Forecasters() {}

    public interface Forecaster {
        String name();
        /** Forecast the next {@code horizon} values from {@code history} (oldest first). */
        double[] forecast(double[] history, int horizon);
    }

    /** Repeat the value from one week ago — captures the weekly mandi cycle. */
    public static final class SeasonalNaive implements Forecaster {
        public String name() { return "Seasonal Naive (7d)"; }
        public double[] forecast(double[] h, int horizon) {
            double[] f = new double[horizon];
            for (int i = 0; i < horizon; i++) {
                int idx = h.length - 7 + (i % 7);
                f[i] = h[Math.max(0, idx)];
            }
            return f;
        }
    }

    /** Mean of the last 7 days, held flat. */
    public static final class MovingAverage implements Forecaster {
        public String name() { return "Moving Average (7d)"; }
        public double[] forecast(double[] h, int horizon) {
            int n = Math.min(7, h.length);
            double mean = Arrays.stream(h, h.length - n, h.length).average().orElse(0);
            double[] f = new double[horizon];
            Arrays.fill(f, mean);
            return f;
        }
    }

    /** Ordinary least squares on the last 30 days, extrapolated. */
    public static final class LinearTrend implements Forecaster {
        public String name() { return "Linear Trend (30d)"; }
        public double[] forecast(double[] h, int horizon) {
            int n = Math.min(30, h.length);
            double sx = 0, sy = 0, sxx = 0, sxy = 0;
            for (int i = 0; i < n; i++) {
                double x = i, y = h[h.length - n + i];
                sx += x; sy += y; sxx += x * x; sxy += x * y;
            }
            double denom = n * sxx - sx * sx;
            double slope = denom == 0 ? 0 : (n * sxy - sx * sy) / denom;
            double intercept = (sy - slope * sx) / n;
            double[] f = new double[horizon];
            for (int i = 0; i < horizon; i++) f[i] = intercept + slope * (n + i);
            return f;
        }
    }

    /** Median of Seasonal Naive, Moving Average and Linear Trend — robust to any single model's miss. */
    public static final class Ensemble implements Forecaster {
        private final Forecaster[] members = {new SeasonalNaive(), new MovingAverage(), new LinearTrend()};
        public String name() { return "Ensemble (median of 3)"; }
        public double[] forecast(double[] h, int horizon) {
            double[][] fs = new double[3][];
            for (int m = 0; m < 3; m++) fs[m] = members[m].forecast(h, horizon);
            double[] f = new double[horizon];
            for (int i = 0; i < horizon; i++) {
                double[] v = {fs[0][i], fs[1][i], fs[2][i]};
                Arrays.sort(v);
                f[i] = v[1];
            }
            return f;
        }
    }

    public static Forecaster[] all() {
        return new Forecaster[]{new SeasonalNaive(), new MovingAverage(), new LinearTrend(), new Ensemble()};
    }
}
