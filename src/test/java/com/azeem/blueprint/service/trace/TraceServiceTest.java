/// *
// * (C) Copyright 2026 Jawad Azeem
// * Apache 2.0 License
// */
//
// package com.azeem.blueprint.service.trace;
//
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.when;
//
// import com.azeem.blueprint.exception.core.TraceResponseInvalidException;
// import com.azeem.blueprint.model.trace.TraceResponse;
// import com.azeem.blueprint.service.trace.sql.QueryExecutionService;
// import com.azeem.blueprint.service.trace.sql.SchemaService;
// import com.azeem.blueprint.service.trace.sql.SqlValidationService;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import java.util.List;
// import java.util.UUID;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Answers;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.Spy;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.ai.chat.prompt.Prompt;
//
// @ExtendWith(MockitoExtension.class)
// class TraceServiceTest {
//
//  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
//  private ChatModel chatModel;
//
//  @Mock private SchemaService schemaService;
//  @Mock private QueryExecutionService queryExecutionService;
//  @Mock private SqlValidationService sqlValidationService;
//
//  @Spy private ObjectMapper objectMapper = new ObjectMapper();
//
//  @InjectMocks private AiExecutionGateway aiExecutionGateway;
//
//  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
//  private static final String BILLING_PERIOD = "2026-01";
//  private static final String VALID_SQL_JSON =
//      "{\"sql\":\"SELECT * FROM billing_records WHERE dataset_id =
// '00000000-0000-0000-0000-000000000001'\","
//          + "\"reasoning\":\"Fetching all records for dataset\"}";
//
//  @Test
//  @DisplayName("ask() returns TraceResponse when SQL is valid and query executes")
//  void ask_validInput_returnsResponse() {
//    when(schemaService.getSchema()).thenReturn("billing_records(id, dataset_id, ...)");
//    when(chatModel.call(any(Prompt.class)).getResult().getOutput().getText())
//        .thenReturn(VALID_SQL_JSON)
//        .thenReturn("Your account spent $500 in January.");
//    when(sqlValidationService.isValidSql(any())).thenReturn(true);
//    when(queryExecutionService.executeQuery(any())).thenReturn(List.of());
//
//    TraceResponse response =
//        aiExecutionGateway.ask("How much did we spend?", DATASET_ID, BILLING_PERIOD);
//
//    assertThat(response).isNotNull();
//    assertThat(response.answer).isEqualTo("Your account spent $500 in January.");
//    assertThat(response.sql).startsWith("SELECT");
//    assertThat(response.reasoning).isEqualTo("Fetching all records for dataset");
//  }
//
//  @Test
//  @DisplayName("ask() throws TraceResponseInvalidException when SQL fails validation")
//  void ask_invalidSql_throwsTraceResponseInvalidException() {
//    when(schemaService.getSchema()).thenReturn("schema");
//    when(chatModel.call(any(Prompt.class)).getResult().getOutput().getText())
//        .thenReturn(VALID_SQL_JSON);
//    when(sqlValidationService.isValidSql(any())).thenReturn(false);
//
//    assertThatThrownBy(() -> aiExecutionGateway.ask("Drop all tables", DATASET_ID,
// BILLING_PERIOD))
//        .isInstanceOf(TraceResponseInvalidException.class)
//        .hasMessageContaining("Unsafe SQL");
//  }
//
//  @Test
//  @DisplayName("ask() throws TraceResponseInvalidException when model returns malformed JSON")
//  void ask_malformedJson_throwsTraceResponseInvalidException() {
//    when(schemaService.getSchema()).thenReturn("schema");
//    when(chatModel.call(any(Prompt.class)).getResult().getOutput().getText())
//        .thenReturn("this is not json at all");
//
//    assertThatThrownBy(() -> aiExecutionGateway.ask("Show me records", DATASET_ID,
// BILLING_PERIOD))
//        .isInstanceOf(TraceResponseInvalidException.class);
//  }
// }
