package in.mandilens;

/** Walk-forward backtest scores for one model on one series. */
public record BacktestResult(String model, double mae, double rmse, double mape) {
    /** Mean absolute percentage error, in percent — the headline metric. */
    public String headline() {
        return String.format("%s — MAPE %.1f%%, MAE ₹%.0f/qtl", model, mape, mae);
    }
}
