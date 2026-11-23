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
import com.cronutils.model.field.expression.And;
import com.cronutils.model.field.expression.Between;
import com.cronutils.model.field.expression.Every;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.expression.On;
import com.cronutils.model.field.value.FieldValue;
import com.cronutils.model.field.value.IntegerFieldValue;
import com.cronutils.utils.Preconditions;
import com.cronutils.utils.StringUtils;
import com.cronutils.utils.VisibleForTesting;

import static com.cronutils.model.field.expression.FieldExpression.always;
import static com.cronutils.model.field.expression.FieldExpression.questionMark;
import static com.cronutils.model.field.value.SpecialChar.QUESTION_MARK;

/**
 * Parses a field from a cron expression.
 */
public class FieldParser {
    private static final String SLASH = "/";
    private static final String DASH = "-";
    private static final String COMMA = ",";
    private static final String L_STRING = "L";
    private static final String QUESTION_MARK_STRING = "?";
    private static final String ASTERISK = "*";
    private static final String ASTERISK_ALWAYS_VALUE = "1";
    private static final char[] STRUCTURAL_DELIMITERS = new char[] { '/', '-', ',' };

    private final FieldConstraints fieldConstraints;
    private final FieldValueMapper valueMapper;
    private final OnFieldExpressionParser onParser;

    public FieldParser(final FieldConstraints constraints) {
        fieldConstraints = Preconditions.checkNotNull(constraints, "FieldConstraints must not be null");
        valueMapper = new FieldValueMapper(fieldConstraints);
        onParser = new OnFieldExpressionParser(fieldConstraints, valueMapper);
    }

    /**
     * Parse given expression for a single cron field.
     *
     * @param expression - String
     * @return CronFieldExpression object with interpretation of given String parameter
     */
    public FieldExpression parse(final String expression) {
        if (!StringUtils.containsAny(expression, STRUCTURAL_DELIMITERS)) {
            return parseSingleToken(expression);
        }
        return parseCompositeExpression(expression);
    }

    private FieldExpression parseSingleToken(final String expression) {
        if (expression.contains(QUESTION_MARK_STRING) && !fieldConstraints.getSpecialChars().contains(QUESTION_MARK)) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
        if (ASTERISK.equals(expression)) {
            return always();
        }
        if (QUESTION_MARK_STRING.equals(expression)) {
            return questionMark();
        }
        return onParser.parseOn(expression);
    }

    private FieldExpression parseCompositeExpression(final String expression) {
        if (RandomFieldExpressionParser.matches(expression)) {
            return RandomFieldExpressionParser.parse(expression);
        }
        final String[] commaParts = expression.split(COMMA);
        if (commaParts.length > 1) {
            return parseCommaList(commaParts);
        }
        return parseDashOrSlash(expression);
    }

    private FieldExpression parseCommaList(final String[] parts) {
        final And and = new And();
        for (final String part : parts) {
            and.and(parse(part));
        }
        return and;
    }

    private FieldExpression parseDashOrSlash(final String expression) {
        final String[] dashParts = expression.split(DASH);
        if (expression.contains(DASH) && dashParts.length != 2) {
            throw new IllegalArgumentException("Missing values for range: " + expression);
        }
        if (dashParts[0].equalsIgnoreCase(L_STRING)) {
            return parseOnWithL(dashParts[0], mapToIntegerFieldValue(dashParts[1]));
        }
        if (dashParts.length > 1) {
            return parseBetween(dashParts);
        }
        return parseSlashStep(expression);
    }

    private FieldExpression parseSlashStep(final String expression) {
        final String[] values = expression.split(SLASH);
        if (values.length == 2) {
            return parseStepFromStart(values[0], values[1]);
        }
        if (values.length == 1) {
            throw new IllegalArgumentException("Missing steps for expression: " + expression);
        }
        throw new IllegalArgumentException("Invalid expression: " + expression);
    }

    private FieldExpression parseStepFromStart(final String start, final String step) {
        final String trimmedStart = start.trim();
        if (ASTERISK.equals(trimmedStart) && ASTERISK_ALWAYS_VALUE.equals(step)) {
            return parseSingleToken(start);
        }
        final IntegerFieldValue period = new IntegerFieldValue(Integer.parseInt(step));
        if (ASTERISK.equals(trimmedStart)) {
            return new Every(period);
        }
        return new Every(new On(mapToIntegerFieldValue(start)), period);
    }

    @VisibleForTesting
    protected FieldExpression parseBetween(final String[] array) {
        if (array[0].isEmpty() || array[1].isEmpty()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid expression! Expression: %s-%s does not describe a range. Negative numbers are not allowed.",
                            array[0], array[1]));
        }
        if (array[1].contains(SLASH)) {
            final String[] every = array[1].split(SLASH);
            return new Every(new Between(map(array[0]), map(every[0])), mapToIntegerFieldValue(every[1]));
        }
        return new Between(map(array[0]), map(array[1]));
    }

    @VisibleForTesting
    protected On parseOn(final String exp) {
        return onParser.parseOn(exp);
    }

    @VisibleForTesting
    protected On parseOnWithHash(final String exp) {
        return onParser.parseOnWithHash(exp);
    }

    @VisibleForTesting
    protected On parseOnWithQuestionMark(final String exp) {
        return onParser.parseOnWithQuestionMark(exp);
    }

    @VisibleForTesting
    protected On parseOnWithLW(final String exp) {
        return onParser.parseOnWithLW(exp);
    }

    @VisibleForTesting
    protected On parseOnWithL(final String exp) {
        return onParser.parseOnWithL(exp);
    }

    protected On parseOnWithL(final String exp, final IntegerFieldValue daysBefore) {
        return onParser.parseOnWithL(exp, daysBefore);
    }

    @VisibleForTesting
    protected On parseOnWithW(final String exp) {
        return onParser.parseOnWithW(exp);
    }

    @VisibleForTesting
    protected IntegerFieldValue mapToIntegerFieldValue(final String string) {
        return valueMapper.mapToIntegerFieldValue(string);
    }

    @VisibleForTesting
    protected FieldValue<?> map(final String string) {
        return valueMapper.map(string);
    }

    /**
     * Maps string expression to integer. If no mapping is found, will try to parse String as Integer
     *
     * @param exp - expression to be mapped
     * @return integer value for string expression
     */
    @VisibleForTesting
    protected int stringToInt(final String exp) {
        return valueMapper.stringToInt(exp);
    }

    /**
     * Maps integer values to another integer equivalence. Always consider mapping higher integers to lower once. Ex.: if 0 and 7 mean the
     * same, map 7 to 0.
     *
     * @param exp - integer to be mapped
     * @return Mapping integer. If no mapping int is found, will return exp
     */
    @VisibleForTesting
    protected int intToInt(final Integer exp) {
        return valueMapper.intToInt(exp);
    }
}
