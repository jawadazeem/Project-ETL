package com.azeem.billing.etl;

import com.azeem.billing.model.BillingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Transforms raw tabular billing data into {@link BillingRecord} domain objects.
 *
 * <p>This class represents the <b>Transform</b> phase of the ingestion pipeline.</p>
 *
 * <p>The assembler interprets column positions, performs type conversion,
 * and constructs validated domain records.</p>
 *
 * <p>This component is stateless and thread-safe by design.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Interpret column ordering</li>
 *   <li>Convert string values to domain types</li>
 *   <li>Create {@link BillingRecord} instances</li>
 * </ul>
 *
 * <h3>Failure Behavior</h3>
 * <p>Malformed rows or conversion errors result in runtime exceptions,
 * terminating ingestion.</p>
 *
 * <h3>Non-responsibilities</h3>
 * <ul>
 *   <li>File I/O</li>
 *   <li>Delimiter handling</li>
 *   <li>Persistence</li>
 * </ul>
 */
@Component
public class BillingRecordAssembler {
    public BillingRecordAssembler() {}

    public List<BillingRecord> assembleRecord(List<String[]> entries) {
        Logger log = LoggerFactory.getLogger(BillingRecordAssembler.class);
        final List<BillingRecord> records = new ArrayList<>();
        for (String[] entry : entries) {
            // Expect the exact 9 fields BillingRecord uses
            String accountName = entry[0];
            String senatorId = entry[1];
            String state = entry[2];
            String phoneNumber = entry[3];
            String billingPeriod = entry[4];

            int minutesUsed = Integer.parseInt(entry[5]);
            double dataGbUsed = Double.parseDouble(entry[6]);
            int smsCount = Integer.parseInt(entry[7]);
            double totalCharge = Double.parseDouble(entry[8]);

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
        log.info("Assembled {} BillingRecord instances from raw data.", records.size());
        return records;
    }
}