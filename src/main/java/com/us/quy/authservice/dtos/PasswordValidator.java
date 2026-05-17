package com.us.quy.authservice.dtos;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    /* Giải thích Regex:
       (?=.*[0-9])        : Có ít nhất 1 chữ số
       (?=.*[a-z])        : Có ít nhất 1 chữ cái viết thường
       (?=.*[A-Z])        : Có ít nhất 1 chữ cái viết hoa
       (?=.*[@#$%^&+=!])  : Có ít nhất 1 ký tự đặc biệt
       .{9,}              : Độ dài lớn hơn 8 (tức là từ 9 ký tự trở lên)
    */
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{9,}$";

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        return password.matches(PASSWORD_PATTERN);
    }
}