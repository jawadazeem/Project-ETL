//TODO: Rewrite tests to account for service layer's major refactor and change in dependencies.

//package com.azeem.billing.service;
//
//import com.azeem.billing.etl.BillParser;
//import com.azeem.billing.model.BillingRecord;
//import com.azeem.billing.model.BillingSummary;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class BillingServiceTest {
//    @Mock
//    BillParser parser;
//
//    @InjectMocks
//    BillingService service;
//
//    @Test
//    void getAllRecords_shouldLoadDataAndReturnRecords() {
//        // Arrange: create fake CSV rows
//        List<BillingRecord> fakeRecords = List.of(
//                new BillingRecord("Account1", "S1", "CA", "555-1234", "2025-Jan",
//                        100, 1.5, 20, 50.0)
//        );
//
//        // Stub parser.getRecords() to return our fake list
//        when(parser.getRecords()).thenReturn(fakeRecords);
//
//        // Act: call the service method
//        List<BillingRecord> result = service.getAllRecords();
//
//        // Assert: verify the result is correct
//        assertEquals(1, result.size());
//        assertEquals("CA", result.get(0).department());
//
//        // And verify parser.load() was called once because of lazy loading
//        verify(parser).load();
//    }
//
//    @Test
//    void getAllRecordsTwice_shouldLoadDataOnce() {
//        // Arrange: create fake CSV rows
//        List<BillingRecord> fakeRecords = List.of(
//                new BillingRecord("Account1", "S1", "CA", "555-1234", "2025-Jan",
//                        100, 1.5, 20, 50.0)
//        );
//
//        // Stub parser.getRecords() to return our fake list
//        when(parser.getRecords()).thenReturn(fakeRecords);
//
//        // Act: call the service method twice
//        List<BillingRecord> result1 = service.getAllRecords();
//        List<BillingRecord> result2 = service.getAllRecords();
//
//        // Assert: verify the result is correct (just for completeness)
//        assertEquals(1, result1.size());
//        assertEquals("CA", result1.get(0).department());
//
//        assertEquals(1, result2.size());
//        assertEquals("CA", result2.get(0).department());
//
//        // verify parser.load() was called once because of lazy loading
//        verify(parser, times(1)).load();
//    }
//
//    @Test
//    void generateSummary_test() {
//        // Arrange: create fake CSV rows
//        List<BillingRecord> fakeRecords = List.of(
//                new BillingRecord("Account1", "S1", "MD", "555-1234", "2025-Jan",
//                        100, 1.5, 20, 10.0),
//                new BillingRecord("Account2", "S2", "NY", "555-5678", "2025-Jan",
//                        200, 2.0, 30, 10.0),
//                new BillingRecord("Account3", "S3", "VA", "555-8765", "2025-Jan",
//                        150, 1.0, 25, 20.0),
//                new BillingRecord("Account4", "S4", "TX", "555-4321", "2025-Jan",
//                        120, 0.5, 15, 10.0));
//
//        // Stub parser.getRecords() to return our fake list
//        when(parser.getRecords()).thenReturn(fakeRecords);
//
//        // Act: call the service method
//        BillingSummary result = service.generateSummary();
//
//        // Assert: verify the result is correct
//        assertEquals(50.0, result.getTotalCharges());
//        assertEquals(4, result.getTotalRecords());
//        assertEquals(Map.of("TX", 10.0, "MD",10.0, "NY",10.0,"VA", 20.0), result.getChargesByState());
//        assertEquals(12.5, result.getAverageCharge());
//        assertEquals("Account3", result.getHighestChargeRecord().accountName());
//        assertEquals("S3", result.getHighestChargeRecord().employeeId());
//
//        // verify parser.load() was called once because of lazy loading
//        verify(parser).load();
//    }
//
//}
