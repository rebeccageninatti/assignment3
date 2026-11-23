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

import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.expression.RandomExpression;
import com.cronutils.utils.RandomUtils;

import java.util.regex.Pattern;

/**
 * Parses OpenBSD-style random ({@code ~}) cron field expressions.
 */
final class RandomFieldExpressionParser {
    private static final String TILDE = "~";
    private static final String SLASH = "/";
    private static final Pattern RANDOM_PATTERN =
            Pattern.compile("~|[0-9]*~[0-9]*(/[0-9]+)?", Pattern.CASE_INSENSITIVE);

    private RandomFieldExpressionParser() {
    }

    static boolean matches(final String expression) {
        return RANDOM_PATTERN.matcher(expression).matches();
    }

    static FieldExpression parse(final String expression) {
        final RandomUtils randomUtils = new RandomUtils();
        if (TILDE.equals(expression)) {
            return new RandomExpression(randomUtils);
        }
        final String[] parts = expression.split(TILDE, -1);
        if (parts.length < 2) {
            return new RandomExpression(randomUtils);
        }
        final Integer from = parseOptionalInt(parts[0]);
        final String toAndStep = parts[1];
        if (toAndStep.contains(SLASH)) {
            final String[] toStepParts = toAndStep.split(SLASH);
            final Integer to = parseOptionalInt(toStepParts[0]);
            final Integer step = Integer.parseInt(toStepParts[1]);
            return new RandomExpression(from, to, step, randomUtils);
        }
        final Integer to = parseOptionalInt(toAndStep);
        return new RandomExpression(from, to, null, randomUtils);
    }

    private static Integer parseOptionalInt(final String token) {
        return token.isEmpty() ? null : Integer.parseInt(token);
    }
}
