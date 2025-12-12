package com.azeem.billing.parser;

import com.azeem.billing.etl.BillParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class BillParserTest {
    // Test to verify that BillParser initializes correctly with a valid file path
    @Test
    public void testBillParserInitialization() {
        String testFilePath = "src/main/resources/senate_billing_2025.csv";
        BillParser billParser = new BillParser(testFilePath);
        assertNotNull(billParser, "BillParser should be initialized successfully");
    }

}
