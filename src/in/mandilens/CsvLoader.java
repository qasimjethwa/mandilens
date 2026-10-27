package in.mandilens;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads mandi price CSVs. Expected header:
 * {@code crop,mandi,date,price_inr_per_quintal} with ISO dates.
 * Lines starting with '#' are comments (used for the SAMPLE-data disclaimer).
 */
public final class CsvLoader {
    private CsvLoader() {}

    public static List<PriceRecord> load(Path csv) throws IOException {
        List<PriceRecord> out = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(csv)) {
            String line;
            boolean headerSeen = false;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (!headerSeen) { headerSeen = true; continue; } // header row
                String[] p = line.split(",");
                if (p.length < 4) continue;
                out.add(new PriceRecord(
                        p[0].trim(), p[1].trim(),
                        LocalDate.parse(p[2].trim()),
                        Double.parseDouble(p[3].trim())));
            }
        }
        return out;
    }
}
