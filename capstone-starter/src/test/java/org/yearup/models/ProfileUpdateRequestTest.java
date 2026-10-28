package org.yearup.models;

import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.*;

class ProfileUpdateRequestTest
{
    @Test
    void validRequestPassesValidation()
    {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory())
        {
            Validator validator = factory.getValidator();
            assertTrue(validator.validate(validRequest()).isEmpty());
        }
    }

    @Test
    void missingRequiredFieldFailsValidation()
    {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory())
        {
            ProfileUpdateRequest request = validRequest();
            request.setFirstName(null);
            assertFalse(factory.getValidator().validate(request).isEmpty());
        }
    }

    @Test
    void malformedEmailFailsValidation()
    {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory())
        {
            ProfileUpdateRequest request = validRequest();
            request.setEmail("not-an-email");
            assertFalse(factory.getValidator().validate(request).isEmpty());
        }
    }

    @Test
    void overlongFieldFailsValidation()
    {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory())
        {
            ProfileUpdateRequest request = validRequest();
            request.setCity("x".repeat(51));
            assertFalse(factory.getValidator().validate(request).isEmpty());
        }
    }

    private ProfileUpdateRequest validRequest()
    {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFirstName("Alice");
        request.setLastName("Example");
        request.setPhone("412-555-0100");
        request.setEmail("alice@example.com");
        request.setAddress("123 Main Street");
        request.setCity("Pittsburgh");
        request.setState("PA");
        request.setZip("15222");
        return request;
    }
}
