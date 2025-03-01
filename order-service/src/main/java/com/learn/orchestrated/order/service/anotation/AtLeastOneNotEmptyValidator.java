package com.learn.orchestrated.order.service.anotation;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;


public class AtLeastOneNotEmptyValidator implements ConstraintValidator<AtLeastOneNotEmpty, Object> {

    private String[] fields;

    @Override
    public void initialize(AtLeastOneNotEmpty constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        if (object == null) {
            return true;
        }

        for (String fieldName : fields) {
            try {
                Field field = object.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(object);

                if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                    return true;
                }
            } catch (Exception e) {
                throw new RuntimeException("Erro ao acessar o campo " + fieldName, e);
            }
        }

        return false;
    }
}