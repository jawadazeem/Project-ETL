package com.azeem.billing.etl;

import com.azeem.billing.model.BillingRecord;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a raw CSV billing file and converts each row into a {@link BillingRecord}.
 * <p>
 * This class acts as the "Extract" stage of the ETL pipeline. It is responsible for:
 * <ul>
 *     <li>Reading the input CSV file line by line</li>
 *     <li>Manually tokenizing each field</li>
 *     <li>Constructing {@link BillingRecord} objects</li>
 *     <li>Storing parsed records in memory for downstream processing</li>
 * </ul>
 *
 * <p>
 * The parser performs minimal validation and assumes that each line contains the expected
 * number of fields in a consistent order. More advanced CSV handling, such as quoted fields,
 * embedded commas, or schema enforcement, can be added in future iterations.
 * </p>
 *
 * <p>
 * Parsed records can be accessed via {@link #getRecords()}, and the {@link #load()} method
 * must be called before attempting to retrieve data.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * BillParser parser = new BillParser("senate_billing_2025.csv");
 * parser.load();
 * List<BillingRecord> results = parser.getRecords();
 * }</pre>
 *
 * <p>
 * This class does not perform any transformation or analytics; those concerns are managed
 * in higher layers (e.g., SummaryBuilder and BillingService).
 * </p>
 */

public class BillParser {
    private static final Logger log = LoggerFactory.getLogger(BillParser.class);

    private final String filePath;
    private final List<BillingRecord> records;
    private boolean loaded = false;

    public BillParser(String filePath) {
        this.filePath = filePath;
        this.records = new ArrayList<>();
    }

    /**
     * Returns all parsed billing records.
     */
    public List<BillingRecord> getRecords() {
        return records;
    }

    // Loads and parses the CSV file into BillingRecord objects. Synchronized to prevent concurrent loads.
    public synchronized void load() {
        if (loaded) {
            log.debug("BillParser.load() called again, but already loaded. Skipping.");
            return;
        }

        records.clear();

        log.info("Loading CSV from {} (existing records: {})", filePath, records.size());

        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            csvReader.readNext(); // Skip header row
            String[] tokens;

            while ((tokens = csvReader.readNext()) != null) {

                // Expect the exact 9 fields BillingRecord uses
                String accountName = tokens[0];
                String senatorId = tokens[1];
                String state = tokens[2];
                String phoneNumber = tokens[3];
                String billingPeriod = tokens[4];

                int minutesUsed = Integer.parseInt(tokens[5]);
                double dataGbUsed = Double.parseDouble(tokens[6]);
                int smsCount = Integer.parseInt(tokens[7]);
                double totalCharge = Double.parseDouble(tokens[8]);

                BillingRecord record = new BillingRecord(
                        accountName,
                        senatorId,
                        state,
                        phoneNumber,
                        billingPeriod,
                        minutesUsed,
                        dataGbUsed,
                        smsCount,
                        totalCharge
                );

                records.add(record);
            }

        } catch (IOException | CsvValidationException e) {
            log.error("Error loading CSV file", e);
        }

        log.info("Successfully loaded from CSV. {} records were parsed.", records.size());
    }


    @Override
    public String toString() {
        return "etl.BillParser{filePath='" + filePath + "', recordsLoaded=" + records.size();
    }
}