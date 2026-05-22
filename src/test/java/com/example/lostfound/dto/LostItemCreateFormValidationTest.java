package com.example.lostfound.dto;

import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.domain.enums.LostItemType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LostItemCreateFormValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validateRequiresCoordinates() {
        LostItemCreateForm form = new LostItemCreateForm();
        form.setCategory(LostItemCategory.REPORT);
        form.setItemType(LostItemType.WALLET);
        form.setTitle("학생증을 주웠어요");
        form.setDescription("310관 앞에서 발견");
        form.setLocationName("310관 앞");
        form.setContactInfo("010-1234-5678");

        Set<ConstraintViolation<LostItemCreateForm>> violations = validator.validate(form);
        Set<String> fields = violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(fields.contains("latitude"));
        assertTrue(fields.contains("longitude"));
    }

    @Test
    void validateRequiresPhoneNumberFormat() {
        LostItemCreateForm form = new LostItemCreateForm();
        form.setCategory(LostItemCategory.REPORT);
        form.setItemType(LostItemType.WALLET);
        form.setTitle("학생증을 주웠어요");
        form.setDescription("310관 앞에서 발견");
        form.setLocationName("310관 앞");
        form.setContactInfo("01012345678");
        form.setLatitude(37.5052);
        form.setLongitude(126.9571);

        Set<ConstraintViolation<LostItemCreateForm>> violations = validator.validate(form);
        Set<String> fields = violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(fields.contains("contactInfo"));
        assertFalse(violations.isEmpty());
    }
}
