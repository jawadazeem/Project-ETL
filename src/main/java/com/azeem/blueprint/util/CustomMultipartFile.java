/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.util;

import java.io.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Jawad
 * @version 1.0 06/30/2026
 */
public class CustomMultipartFile implements MultipartFile {
  private final byte[] input;
  private final String name;
  private final String originalFilename;
  private final String contentType;

  public CustomMultipartFile(
      byte[] input, String name, String originalFilename, String contentType) {
    this.input = input;
    this.name = name;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getOriginalFilename() {
    return originalFilename;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public boolean isEmpty() {
    return input == null || input.length == 0;
  }

  @Override
  public long getSize() {
    return input.length;
  }

  @Override
  public byte[] getBytes() throws IOException {
    return input;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(input);
  }

  @Override
  public void transferTo(File dest) throws IOException, IllegalStateException {
    try (FileOutputStream fos = new FileOutputStream(dest)) {
      fos.write(input);
    }
  }
}
