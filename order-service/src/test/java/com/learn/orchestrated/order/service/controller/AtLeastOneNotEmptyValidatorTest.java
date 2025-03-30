package com.learn.orchestrated.order.service.controller;

import com.learn.orchestrated.order.service.anotation.AtLeastOneNotEmpty;
import com.learn.orchestrated.order.service.anotation.AtLeastOneNotEmptyValidator;
import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AtLeastOneNotEmptyValidatorTest {

    private AtLeastOneNotEmptyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AtLeastOneNotEmptyValidator();

        validator.initialize(new AtLeastOneNotEmpty() {
            @Override
            public String[] fields() {
                return new String[]{"fieldA", "fieldB"};
            }

            @Override
            public String message() {
                return "At least one field must be filled";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return AtLeastOneNotEmpty.class;
            }
        });
    }

    static class DummyObject {
        private String fieldA;
        private String fieldB;

        public DummyObject(String fieldA, String fieldB) {
            this.fieldA = fieldA;
            this.fieldB = fieldB;
        }
    }

    @Test
    void shouldReturnTrueWhenAtLeastOneFieldIsNotEmpty() {
        DummyObject obj = new DummyObject("value", null);
        assertTrue(validator.isValid(obj, null));
    }

    @Test
    void shouldReturnFalseWhenAllFieldsAreEmpty() {
        DummyObject obj = new DummyObject("   ", "");
        assertFalse(validator.isValid(obj, null));
    }

    @Test
    void shouldReturnTrueWhenObjectIsNull() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void shouldThrowExceptionWhenFieldDoesNotExist() {
        validator.initialize(new AtLeastOneNotEmpty() {
            @Override
            public String[] fields() {
                return new String[]{"nonExistentField"};
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return AtLeastOneNotEmpty.class;
            }
        });

        DummyObject obj = new DummyObject("A", "B");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            validator.isValid(obj, null);
        });

        assertTrue(ex.getMessage().contains("Erro ao acessar o campo nonExistentField"));
    }
}