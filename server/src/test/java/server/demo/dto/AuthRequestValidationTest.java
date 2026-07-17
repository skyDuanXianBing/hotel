package server.demo.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import server.demo.dto.auth.RegisterRequest;
import server.demo.dto.auth.SendVerificationCodeRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "12345678901234567890"})
    void registerPassword_shouldAcceptInclusiveLengthBounds(String password) {
        RegisterRequest request = validRegisterRequest(password);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertFalse(hasViolationFor(violations, "password"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "123456789012345678901"})
    void registerPassword_shouldRejectLengthsOutsideBounds(String password) {
        RegisterRequest request = validRegisterRequest(password);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(hasViolationFor(violations, "password"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"login", "register", "reset_password"})
    void verificationCodeType_shouldAcceptSupportedValues(String type) {
        SendVerificationCodeRequest request = validVerificationCodeRequest(type);

        Set<ConstraintViolation<SendVerificationCodeRequest>> violations = validator.validate(request);

        assertFalse(hasViolationFor(violations, "type"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"password_reset", "REGISTER", "other"})
    void verificationCodeType_shouldRejectUnsupportedValues(String type) {
        SendVerificationCodeRequest request = validVerificationCodeRequest(type);

        Set<ConstraintViolation<SendVerificationCodeRequest>> violations = validator.validate(request);

        assertTrue(hasViolationFor(violations, "type"));
    }

    private static RegisterRequest validRegisterRequest(String password) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("owner@example.com");
        request.setVerificationCode("123456");
        request.setPassword(password);
        return request;
    }

    private static SendVerificationCodeRequest validVerificationCodeRequest(String type) {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("owner@example.com");
        request.setType(type);
        return request;
    }

    private static boolean hasViolationFor(
            Set<? extends ConstraintViolation<?>> violations,
            String propertyName) {
        return violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(propertyName));
    }
}
