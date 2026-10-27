package in.mandilens;

import in.mandilens.Forecasters.Forecaster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * MandiLens — crop price forecasting for Indian farmers.
 *
 * <pre>
 *   java MandiLens backtest  [csv]              # walk-forward scores for every crop x mandi
 *   java MandiLens forecast &lt;crop&gt; &lt;mandi&gt; [csv]  # 7-day forecast, best day to sell
 *   java MandiLens export   [csv]              # write docs/metrics.json + docs/data.json for the web demo
 * </pre>
 *
 * Default CSV is {@code data/sample_mandi_prices.csv} (clearly-labelled
 * illustrative sample data — replace it with a real Agmarknet export).
 * The forecasting models are the same maths as {@code docs/forecast.js},
 * so the browser demo runs the identical models with no backend.
 */
public final class MandiLens {
    private static final int HORIZON = 7;
    private static final int TEST_DAYS = 21;

    private MandiLens() {}

    /** crop -> mandi -> sorted daily prices */
    static Map<String, Map<String, List<PriceRecord>>> group(List<PriceRecord> rows) {
        Map<String, Map<String, List<PriceRecord>>> out = new TreeMap<>();
        for (PriceRecord r : rows) {
            out.computeIfAbsent(r.crop(), k -> new TreeMap<>())
               .computeIfAbsent(r.mandi(), k -> new ArrayList<>())
               .add(r);
        }
        for (var byMandi : out.values())
            for (var list : byMandi.values())
                list.sort(Comparator.comparing(PriceRecord::date));
        return out;
    }

    static double[] prices(List<PriceRecord> series) {
        return series.stream().mapToDouble(PriceRecord::price).toArray();
    }

    /** Pick the model with the lowest backtest MAPE for this series. */
    static BacktestResult bestModel(double[] series) {
        BacktestResult best = null;
        for (Forecaster m : Forecasters.all()) {
            BacktestResult r = Backtester.evaluate(m, series, TEST_DAYS);
            if (best == null || r.mape() < best.mape()) best = r;
        }
        return best;
    }

    static Forecaster forecasterNamed(String name) {
        for (Forecaster m : Forecasters.all()) if (m.name().equals(name)) return m;
        return new Forecasters.Ensemble();
    }

    /** Forecast the next 7 days with a 95% band from backtest residual spread. */
    static ForecastResult forecast(String crop, String mandi, List<PriceRecord> series) {
        double[] s = prices(series);
        BacktestResult best = bestModel(s);
        Forecaster model = forecasterNamed(best.model());
        double[] point = model.forecast(s, HORIZON);
        double halfWidth = 1.96 * best.rmse();
        LocalDate last = series.get(series.size() - 1).date();
        LocalDate[] dates = new LocalDate[HORIZON];
        double[] lower = new double[HORIZON], upper = new double[HORIZON];
        int bestIdx = 0;
        for (int i = 0; i < HORIZON; i++) {
            dates[i] = last.plusDays(i + 1);
            lower[i] = Math.max(0, point[i] - halfWidth);
            upper[i] = point[i] + halfWidth;
            if (point[i] > point[bestIdx]) bestIdx = i;
        }
        return new ForecastResult(crop, mandi, best.model(), dates, point, lower, upper,
                dates[bestIdx], point[bestIdx], s[s.length - 1]);
    }

    private static void backtest(List<PriceRecord> rows) {
        System.out.println("Walk-forward backtest — last " + TEST_DAYS + " days, 1-day-ahead, no look-ahead\n");
        for (var byCrop : group(rows).entrySet()) {
            for (var byMandi : byCrop.getValue().entrySet()) {
                double[] s = prices(byMandi.getValue());
                System.out.println(byCrop.getKey() + " @ " + byMandi.getKey() + "  (" + s.length + " days)");
                for (Forecaster m : Forecasters.all()) {
                    BacktestResult r = Backtester.evaluate(m, s, TEST_DAYS);
                    System.out.printf(Locale.US, "  %-26s MAPE %5.1f%%   MAE ₹%6.0f   RMSE ₹%6.0f%n",
                            r.model(), r.mape(), r.mae(), r.rmse());
                }
                System.out.println("  -> best: " + bestModel(s).headline() + "\n");
            }
        }
    }

    private static void forecastCmd(String crop, String mandi, List<PriceRecord> rows) {
        var series = group(rows).getOrDefault(crop, Map.of()).get(mandi);
        if (series == null) {
            System.out.println("No data for " + crop + " @ " + mandi + ". Try: java MandiLens list");
            return;
        }
        ForecastResult f = forecast(crop, mandi, series);
        System.out.printf(Locale.US, "%s @ %s — best model: %s%n%n", crop, mandi, f.model());
        System.out.printf(Locale.US, "Today: ₹%.0f/quintal%n%n", f.sellTodayPrice());
        System.out.println("7-day forecast (₹/quintal):");
        for (int i = 0; i < HORIZON; i++) {
            System.out.printf(Locale.US, "  %s  ₹%6.0f   [₹%.0f – ₹%.0f]%s%n",
                    f.dates()[i], f.point()[i], f.lower()[i], f.upper()[i],
                    i == f.bestSellDayIndex() ? "  <-- BEST DAY TO SELL" : "");
        }
        if (f.expectedGain() >= 0) {
            System.out.printf(Locale.US, "%nBest day to sell: %s at ₹%.0f — ₹%.0f more than selling today.%n",
                    f.bestSellDate(), f.bestSellPrice(), f.expectedGain());
        } else {
            System.out.printf(Locale.US, "%nPrices are heading down — sell today. Even the best day ahead (%s, ₹%.0f) is ₹%.0f below today's price.%n",
                    f.bestSellDate(), f.bestSellPrice(), -f.expectedGain());
        }
    }

    private static void list(List<PriceRecord> rows) {
        for (var byCrop : group(rows).entrySet())
            for (var byMandi : byCrop.getValue().entrySet())
                System.out.println(byCrop.getKey() + " @ " + byMandi.getKey()
                        + " — " + byMandi.getValue().size() + " days, "
                        + byMandi.getValue().get(0).date() + " to "
                        + byMandi.getValue().get(byMandi.getValue().size() - 1).date());
    }

    /** Export metrics + series data for the GitHub Pages demo. */
    private static void export(List<PriceRecord> rows, Path docsDir) throws IOException {
        Files.createDirectories(docsDir);
        StringBuilder metrics = new StringBuilder("{\n  \"generated\": \"")
                .append(LocalDate.now()).append("\",\n  \"testDays\": ").append(TEST_DAYS)
                .append(",\n  \"series\": [");
        StringBuilder data = new StringBuilder("{\n  \"series\": {");
        boolean firstSeries = true;
        for (var byCrop : group(rows).entrySet()) {
            for (var byMandi : byCrop.getValue().entrySet()) {
                String key = byCrop.getKey() + "@" + byMandi.getKey();
                double[] s = prices(byMandi.getValue());
                BacktestResult best = bestModel(s);
                ForecastResult f = forecast(byCrop.getKey(), byMandi.getKey(), byMandi.getValue());
                if (!firstSeries) { metrics.append(","); data.append(","); }
                firstSeries = false;
                metrics.append("\n    {\"key\": \"").append(key)
                        .append("\", \"days\": ").append(s.length)
                        .append(", \"bestModel\": \"").append(best.model())
                        .append("\", \"mape\": ").append(String.format(Locale.US, "%.2f", best.mape()))
                        .append(", \"mae\": ").append(String.format(Locale.US, "%.1f", best.mae()))
                        .append(", \"rmse\": ").append(String.format(Locale.US, "%.1f", best.rmse()))
                        .append(", \"lastPrice\": ").append(String.format(Locale.US, "%.0f", s[s.length - 1]))
                        .append(", \"bestSellDate\": \"").append(f.bestSellDate())
                        .append("\", \"bestSellPrice\": ").append(String.format(Locale.US, "%.0f", f.bestSellPrice()))
                        .append(", \"expectedGain\": ").append(String.format(Locale.US, "%.0f", f.expectedGain()))
                        .append(", \"forecast\": [");
                for (int i = 0; i < HORIZON; i++) {
                    if (i > 0) metrics.append(", ");
                    metrics.append(String.format(Locale.US,
                            "{\"date\": \"%s\", \"price\": %.0f, \"low\": %.0f, \"high\": %.0f}",
                            f.dates()[i], f.point()[i], f.lower()[i], f.upper()[i]));
                }
                metrics.append("]}");
                data.append("\n    \"").append(key).append("\": {\"dates\": [");
                var series = byMandi.getValue();
                for (int i = 0; i < series.size(); i++) {
                    if (i > 0) data.append(", ");
                    data.append("\"").append(series.get(i).date()).append("\"");
                }
                data.append("], \"prices\": [");
                for (int i = 0; i < s.length; i++) {
                    if (i > 0) data.append(", ");
                    data.append(String.format(Locale.US, "%.0f", s[i]));
                }
                data.append("]}");
            }
        }
        metrics.append("\n  ]\n}\n");
        data.append("\n  }\n}\n");
        Files.writeString(docsDir.resolve("metrics.json"), metrics.toString());
        Files.writeString(docsDir.resolve("data.json"), data.toString());
        System.out.println("Wrote " + docsDir.resolve("metrics.json"));
        System.out.println("Wrote " + docsDir.resolve("data.json"));
    }

    public static void main(String[] args) throws IOException {
        Path csv = args.length > 1 && args[1].endsWith(".csv") ? Path.of(args[1])
                : Path.of("data/sample_mandi_prices.csv");
        // allow `forecast <crop> <mandi> [csv]`
        if (args.length > 0 && args[0].equals("forecast") && args.length > 3 && args[3].endsWith(".csv"))
            csv = Path.of(args[3]);
        if (!Files.exists(csv)) {
            System.out.println("CSV not found: " + csv + " — run from the repo root.");
            return;
        }
        List<PriceRecord> rows = CsvLoader.load(csv);
        if (args.length == 0) {
            System.out.println("MandiLens — crop price forecasting for Indian farmers");
            System.out.println("Usage: backtest | forecast <crop> <mandi> | list | export");
            return;
        }
        switch (args[0]) {
            case "backtest" -> backtest(rows);
            case "list" -> list(rows);
            case "forecast" -> {
                if (args.length < 3) { System.out.println("Usage: forecast <crop> <mandi>"); return; }
                forecastCmd(args[1], args[2], rows);
            }
            case "export" -> export(rows, Path.of("docs"));
            default -> System.out.println("Unknown command: " + args[0]);
        }
    }
}
