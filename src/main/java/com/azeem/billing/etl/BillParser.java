package com.azeem.billing.etl;

import com.azeem.billing.model.BillingRecord;
import com.azeem.billing.util.CsvReader;

import java.io.*;
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

public class BillParser extends CsvReader {

    private final String filePath;
    private final List<BillingRecord> records;

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

    /**
     * Reads the CSV file and loads model.BillingRecord objects
     * into the internal records list.
     */
    public void load() {
        // TODO: Implement more robust CSV parsing needed, accounting for quoted fields with commas, etc.
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                String accountName = "";
                String senatorId = "";
                String state = "";
                String phoneNumber = "";
                String billingPeriod = "";
                int minutesUsed = 0;
                double dataGbUsed = 0.0;
                int smsCount = 0;
                double totalCharge = 0.0;

                StringBuilder currToken = new StringBuilder();

                int tokenNum = 0;
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    if (c != ',') {
                        currToken.append(c);
                    } else {
                        String token = currToken.toString();
                        switch (tokenNum) {
                            case 0:
                                accountName = currToken.toString();
                                break;
                            case 1:
                                senatorId = currToken.toString();
                                break;
                            case 2:
                                state = currToken.toString();
                                break;
                            case 3:
                                phoneNumber = currToken.toString();
                                break;
                            case 4:
                                billingPeriod = currToken.toString();
                                break;
                            case 5:
                                String minutesUsedStr = currToken.toString();
                                minutesUsed = Integer.parseInt(minutesUsedStr);
                                break;
                            case 6:
                                String dataGbUsedStr = currToken.toString();
                                dataGbUsed = Double.parseDouble(dataGbUsedStr);
                                break;
                            case 7:
                                String smsCountStr = currToken.toString();
                                smsCount = Integer.parseInt(smsCountStr);
                                break;
                        }
                        currToken.setLength(0);
                        tokenNum++;
                    }
                }
                totalCharge = Double.parseDouble(currToken.toString());

                BillingRecord currentRecord = new BillingRecord(
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
                records.add(currentRecord);
            }
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "etl.BillParser{filePath='" + filePath + "', recordsLoaded=" + records.size();
    }
}
