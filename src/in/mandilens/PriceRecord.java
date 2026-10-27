package in.mandilens;

import java.time.LocalDate;

/** One daily modal (most-traded) mandi price, in rupees per quintal. */
public record PriceRecord(String crop, String mandi, LocalDate date, double price) {
    public PriceRecord {
        if (price <= 0) throw new IllegalArgumentException("price must be positive");
    }
}
