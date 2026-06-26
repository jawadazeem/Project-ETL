/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PdfStorageServiceTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID DATASET_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final String BUCKET = "cloud-billing";

  @Mock private S3Template s3Template;

  @Test
  @DisplayName("Uploads PDF to S3 with correct key format")
  void shouldUploadToS3WithCorrectKey() {
    PdfStorageService service = new PdfStorageService(s3Template, BUCKET);
    byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};

    String key = service.store(USER_ID, DATASET_ID, "2026-01", pdfBytes);

    String expectedKey = USER_ID + "/" + DATASET_ID + "/reports/2026-01-report.pdf";
    assertThat(key).isEqualTo(expectedKey);

    ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(s3Template).upload(eq(BUCKET), eq(expectedKey), streamCaptor.capture());
    assertThat(streamCaptor.getValue()).isNotNull();
  }

  @Test
  @DisplayName("Retrieves PDF from S3")
  void shouldRetrieveFromS3() throws IOException {
    PdfStorageService service = new PdfStorageService(s3Template, BUCKET);
    S3Resource resource = org.mockito.Mockito.mock(S3Resource.class);
    InputStream expected = new ByteArrayInputStream(new byte[0]);

    when(s3Template.download(BUCKET, "some/key.pdf")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(expected);

    InputStream result = service.retrieve("some/key.pdf");

    assertThat(result).isSameAs(expected);
  }
}
