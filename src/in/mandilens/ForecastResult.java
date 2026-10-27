package in.mandilens;

import java.time.LocalDate;

/** A 7-day forecast with a 95% confidence band and the best day to sell. */
public record ForecastResult(
        String crop, String mandi, String model,
        LocalDate[] dates, double[] point, double[] lower, double[] upper,
        LocalDate bestSellDate, double bestSellPrice, double sellTodayPrice) {

    /** Expected gain (₹/quintal) from waiting for the best day instead of selling today. */
    public double expectedGain() { return bestSellPrice - sellTodayPrice; }

    public int bestSellDayIndex() {
        for (int i = 0; i < dates.length; i++) if (dates[i].equals(bestSellDate)) return i;
        return -1;
    }
}
