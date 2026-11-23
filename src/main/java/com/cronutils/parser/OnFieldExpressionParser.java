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

import com.cronutils.model.field.constraint.FieldConstraints;
import com.cronutils.model.field.expression.On;
import com.cronutils.model.field.value.IntegerFieldValue;
import com.cronutils.model.field.value.SpecialChar;
import com.cronutils.model.field.value.SpecialCharFieldValue;

import java.util.regex.Pattern;

/**
 * Parses single-value {@link On} cron field expressions including special characters ({@code #}, {@code L}, {@code W}, {@code LW}, {@code ?}).
 */
final class OnFieldExpressionParser {
    private static final String EMPTY_STRING = "";
    private static final String QUESTION_MARK_TOKEN = "?";
    private static final String HASH_TOKEN = "#";
    private static final String LW_TOKEN = "LW";
    private static final String L_TOKEN = "L";
    private static final String W_TOKEN = "W";
    private static final IntegerFieldValue NO_VALUE = new IntegerFieldValue(-1);

    private static final Pattern L_PATTERN = Pattern.compile("[0-9]L", Pattern.CASE_INSENSITIVE);
    private static final Pattern W_PATTERN = Pattern.compile("[0-9]W", Pattern.CASE_INSENSITIVE);

    private final FieldConstraints fieldConstraints;
    private final FieldValueMapper valueMapper;

    OnFieldExpressionParser(final FieldConstraints fieldConstraints, final FieldValueMapper valueMapper) {
        this.fieldConstraints = fieldConstraints;
        this.valueMapper = valueMapper;
    }

    On parseOn(final String expression) {
        if (QUESTION_MARK_TOKEN.equals(expression)) {
            return parseOnWithQuestionMark(expression);
        }
        if (expression.contains(HASH_TOKEN)) {
            return parseOnWithHash(expression);
        }
        if (expression.contains(LW_TOKEN)) {
            return parseOnWithLW(expression);
        }
        if (L_PATTERN.matcher(expression).find() || expression.equalsIgnoreCase(L_TOKEN)) {
            return parseOnWithL(expression);
        }
        if (W_PATTERN.matcher(expression).find()) {
            return parseOnWithW(expression);
        }
        return new On(valueMapper.mapToIntegerFieldValue(expression), new SpecialCharFieldValue(SpecialChar.NONE), NO_VALUE);
    }

    On parseOnWithHash(final String expression) {
        if (!fieldConstraints.getSpecialChars().contains(SpecialChar.HASH)) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
        final String[] parts = expression.split(HASH_TOKEN);
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid Position of # Character!");
        }
        final IntegerFieldValue nth = valueMapper.mapToIntegerFieldValue(parts[1]);
        if (parts[0].isEmpty()) {
            throw new IllegalArgumentException("Time should be specified!");
        }
        return new On(valueMapper.mapToIntegerFieldValue(parts[0]), new SpecialCharFieldValue(SpecialChar.HASH), nth);
    }

    On parseOnWithQuestionMark(final String expression) {
        return parseExactSpecialChar(expression, QUESTION_MARK_TOKEN, SpecialChar.QUESTION_MARK, "'?'");
    }

    On parseOnWithLW(final String expression) {
        return parseExactSpecialChar(expression, LW_TOKEN, SpecialChar.LW, LW_TOKEN);
    }

    On parseOnWithL(final String expression) {
        return parseOnWithL(expression, NO_VALUE);
    }

    On parseOnWithL(final String expression, final IntegerFieldValue daysBefore) {
        final String stripped = expression.replace(L_TOKEN, EMPTY_STRING);
        final IntegerFieldValue time = EMPTY_STRING.equals(stripped)
                ? NO_VALUE
                : valueMapper.mapToIntegerFieldValue(stripped);
        return new On(time, new SpecialCharFieldValue(SpecialChar.L), daysBefore);
    }

    On parseOnWithW(final String expression) {
        final String stripped = expression.replace(W_TOKEN, EMPTY_STRING);
        return new On(valueMapper.mapToIntegerFieldValue(stripped), new SpecialCharFieldValue(SpecialChar.W), NO_VALUE);
    }

    private On parseExactSpecialChar(
            final String expression,
            final String token,
            final SpecialChar specialChar,
            final String expectedLabel) {
        final String remainder = expression.replace(token, EMPTY_STRING);
        if (EMPTY_STRING.equals(remainder)) {
            return new On(NO_VALUE, new SpecialCharFieldValue(specialChar), NO_VALUE);
        }
        throw new IllegalArgumentException(String.format("Expected: %s, found: %s", expectedLabel, remainder));
    }
}
