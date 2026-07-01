/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.blueprint.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class MdFileValidator implements ConstraintValidator<ValidMdFile, MultipartFile> {
  @Override
  public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {

    // Rule 1: must exist
    if (file == null) {
      return false;
    }

    // Rule 2: must not be empty
    if (file.isEmpty()) {
      return false;
    }

    // Rule 3: must have a filename
    String name = file.getOriginalFilename();
    if (name == null || name.trim().isEmpty()) {
      return false;
    }

    // Rule 4: must end in .csv (case-insensitive)
    return name.toLowerCase().endsWith(".md");
  }
}
