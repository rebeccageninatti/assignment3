/*
 * Copyright 2014 jmrozanec
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cronutils.parser;

import com.cronutils.StringValidations;
import com.cronutils.model.field.constraint.FieldConstraints;
import com.cronutils.model.field.value.FieldValue;
import com.cronutils.model.field.value.IntegerFieldValue;
import com.cronutils.model.field.value.SpecialChar;
import com.cronutils.model.field.value.SpecialCharFieldValue;

/**
 * Maps raw cron field value tokens to {@link FieldValue} and integer equivalents using {@link FieldConstraints}.
 */
final class FieldValueMapper {
    private static final String TILDE = "~";

    private final FieldConstraints fieldConstraints;

    FieldValueMapper(final FieldConstraints fieldConstraints) {
        this.fieldConstraints = fieldConstraints;
    }

    IntegerFieldValue mapToIntegerFieldValue(final String value) {
        try {
            return new IntegerFieldValue(intToInt(stringToInt(value)));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid value. Expected some integer, found %s", value), e);
        }
    }

    FieldValue<?> map(final String value) {
        if (TILDE.equals(value)) {
            return new SpecialCharFieldValue(SpecialChar.TILDE);
        }
        for (final SpecialChar specialChar : SpecialChar.values()) {
            if (specialChar.toString().equals(value)) {
                return new SpecialCharFieldValue(specialChar);
            }
        }
        return new IntegerFieldValue(stringToInt(value));
    }

    int stringToInt(final String expression) {
        final Integer mappedValue = fieldConstraints.getStringMappingValue(expression);
        if (mappedValue != null) {
            return mappedValue;
        }
        try {
            return Integer.parseInt(expression);
        } catch (final NumberFormatException e) {
            final String invalidChars = new StringValidations(fieldConstraints).removeValidChars(expression);
            throw new IllegalArgumentException(
                    String.format("Invalid chars in expression! Expression: %s Invalid chars: %s",
                            expression, invalidChars), e);
        }
    }

    int intToInt(final Integer value) {
        final Integer mappedValue = fieldConstraints.getIntMappingValue(value);
        return mappedValue != null ? mappedValue : value;
    }
}
