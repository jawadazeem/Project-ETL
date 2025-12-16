package com.azeem.billing.etl;

import com.azeem.billing.util.BillingFileReader;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads billing data from a tab-separated values (TSV) file and returns
 * raw rows as string arrays.
 *
 * <p>This reader behaves identically to {@link CsvBillingReader} but uses
 * tab-delimited input instead of commas.</p>
 *
 * <p>The class exists to isolate file format differences and avoid
 * conditional parsing logic.</p>
 *
 * <p>All data is returned in raw string form and must be transformed
 * by a downstream assembler.</p>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Format-specific reader implementation</li>
 *   <li>No domain awareness</li>
 *   <li>Fail-fast on malformed input</li>
 * </ul>
 */

public class TsvBillingReader implements BillingFileReader {
    private static final Logger log = LoggerFactory.getLogger(TsvBillingReader.class);

    private final String filePath;
    private final boolean hasHeader;

    public TsvBillingReader(String filePath, boolean hasHeader) {
        this.filePath = filePath;
        this.hasHeader = hasHeader;
    }

    @Override
    public List<String[]> parse() {

        log.info("Loading TSV from {}", filePath);

        // Modify parser for tab delimitation
        CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();

        try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(filePath))
                .withCSVParser(parser)
                .build()) {
            if (hasHeader) {
                // Skip header row
                csvReader.readNext();
            }

            String[] tokens;
            List<String[]> entries = new ArrayList<>();

            while ((tokens = csvReader.readNext()) != null) {
                entries.add(tokens);
            }

            log.info("Successfully loaded from TSV. {} records were parsed.", entries.size());
            return entries;

        } catch (IOException | CsvValidationException e) {
            log.error("Error loading TSV file", e);
            throw new IllegalStateException("Failed to load TSV file", e);
        }
    }
}
