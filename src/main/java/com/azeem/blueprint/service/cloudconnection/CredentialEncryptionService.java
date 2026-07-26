/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.service.cloudconnection;

import com.azeem.blueprint.exception.core.CloudConnectionDecryptionException;
import com.azeem.blueprint.exception.core.CloudConnectionEncryptionException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CredentialEncryptionService {
  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;
  private static final int IV_LENGTH = 12;

  private final SecretKey secretKey;
  private final ObjectMapper objectMapper;
  private final SecureRandom secureRandom = new SecureRandom();

  public CredentialEncryptionService(
      @Value("${cloud-connection.encryption-key}") String base64Key, ObjectMapper objectMapper) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    this.secretKey = new SecretKeySpec(keyBytes, "AES");
    this.objectMapper = objectMapper;
  }

  public String encrypt(Map<String, String> credentials) {
    try {
      byte[] plaintext = objectMapper.writeValueAsBytes(credentials);
      byte[] iv = new byte[IV_LENGTH];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      byte[] ciphertext = cipher.doFinal(plaintext);

      byte[] combined = new byte[IV_LENGTH + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
      System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      throw new CloudConnectionEncryptionException("Failed to encrypt credentials", e);
    }
  }

  public Map<String, String> decrypt(String encryptedBase64) {
    try {
      byte[] combined = Base64.getDecoder().decode(encryptedBase64);
      byte[] iv = new byte[IV_LENGTH];
      System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

      byte[] ciphertext = new byte[combined.length - IV_LENGTH];
      System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      byte[] plaintext = cipher.doFinal(ciphertext);

      return objectMapper.readValue(plaintext, new TypeReference<>() {});
    } catch (Exception e) {
      throw new CloudConnectionDecryptionException("Failed to decrypt credentials", e);
    }
  }
}
