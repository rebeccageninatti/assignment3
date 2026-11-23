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

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.CompositeCron;
import com.cronutils.model.Cron;
import com.cronutils.model.SingleCron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronNicknames;
import com.cronutils.model.field.CronField;
import com.cronutils.utils.Preconditions;
import com.cronutils.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser for cron expressions.
 * The class is thread safe.
 */
public class CronParser {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final String COMPOSITE_OR_SEPARATOR = "||";
    private static final String PIPE_SEPARATOR = "|";
    private static final String NICKNAME_PREFIX = "@";

    private record NicknameSpec(CronNicknames nickname, Function<CronDefinition, Cron> factory) {
    }

    private static final Map<String, NicknameSpec> NICKNAME_SPECS = Map.of(
            "@yearly", new NicknameSpec(CronNicknames.YEARLY, CronBuilder::yearly),
            "@annually", new NicknameSpec(CronNicknames.ANNUALLY, CronBuilder::annually),
            "@monthly", new NicknameSpec(CronNicknames.MONTHLY, CronBuilder::monthly),
            "@weekly", new NicknameSpec(CronNicknames.WEEKLY, CronBuilder::weekly),
            "@daily", new NicknameSpec(CronNicknames.DAILY, CronBuilder::daily),
            "@midnight", new NicknameSpec(CronNicknames.MIDNIGHT, CronBuilder::midnight),
            "@hourly", new NicknameSpec(CronNicknames.HOURLY, CronBuilder::hourly),
            "@reboot", new NicknameSpec(CronNicknames.REBOOT, CronBuilder::reboot)
    );

    private final Map<Integer, List<CronParserField>> expressionsByLength = new HashMap<>();
    private final CronDefinition cronDefinition;

    /**
     * @param cronDefinition - cronDefinition of cron expressions to be parsed if null, a NullPointerException will be raised.
     */
    public CronParser(final CronDefinition cronDefinition) {
        this.cronDefinition = Preconditions.checkNotNull(cronDefinition, "CronDefinition must not be null");
        buildPossibleExpressions(cronDefinition);
    }

    /**
     * Build possible cron expressions from definitions. One is built for sure. A second one may be built if last field is optional.
     *
     * @param cronDefinition - cron definition instance
     */
    private void buildPossibleExpressions(final CronDefinition cronDefinition) {
        final List<CronParserField> sortedExpression = cronDefinition.getFieldDefinitions().stream()
                .map(def -> new CronParserField(def.getFieldName(), def.getConstraints(), def.isOptional()))
                .sorted(CronParserField.createFieldTypeComparator())
                .collect(Collectors.toList());

        List<CronParserField> currentExpression = sortedExpression;
        while (lastFieldIsOptional(currentExpression)) {
            final int expressionLength = currentExpression.size() - 1;
            final List<CronParserField> shorterExpression = new ArrayList<>(currentExpression.subList(0, expressionLength));
            expressionsByLength.put(expressionLength, shorterExpression);
            currentExpression = shorterExpression;
        }

        expressionsByLength.put(sortedExpression.size(), sortedExpression);
    }

    private boolean lastFieldIsOptional(final List<CronParserField> fields) {
        return !fields.isEmpty() && fields.get(fields.size() - 1).isOptional();
    }

    /**
     * Parse string with cron expression.
     *
     * @param expression - cron expression, never null
     * @return Cron instance, corresponding to cron expression received
     * @throws java.lang.IllegalArgumentException if expression does not match cron definition
     */
    public Cron parse(final String expression) {
        Preconditions.checkNotNull(expression, "Expression must not be null");
        final String normalized = WHITESPACE_PATTERN.matcher(expression).replaceAll(" ").trim();
        if (StringUtils.isEmpty(normalized)) {
            throw new IllegalArgumentException("Empty expression!");
        }

        final Optional<Cron> nicknameCron = tryParseNickname(expression);
        if (nicknameCron.isPresent()) {
            return nicknameCron.get();
        }
        if (expression.contains(COMPOSITE_OR_SEPARATOR)) {
            return parseCompositeOr(expression);
        }
        if (expression.contains(PIPE_SEPARATOR)) {
            return parsePipeMultiCron(expression);
        }
        return parseSingleCron(normalized);
    }

    private Optional<Cron> tryParseNickname(final String expression) {
        if (!expression.startsWith(NICKNAME_PREFIX)) {
            return Optional.empty();
        }
        final Set<CronNicknames> supportedNicknames = cronDefinition.getCronNicknames();
        if (supportedNicknames.isEmpty()) {
            throw new IllegalArgumentException("Nicknames not supported!");
        }
        final NicknameSpec spec = NICKNAME_SPECS.get(expression);
        if (spec == null) {
            return Optional.empty();
        }
        if (!supportedNicknames.contains(spec.nickname())) {
            throw new IllegalArgumentException(String.format("Nickname %s not supported!", expression));
        }
        return Optional.of(spec.factory().apply(cronDefinition));
    }

    private Cron parseCompositeOr(final String expression) {
        final List<Cron> crons = Arrays.stream(expression.split("\\|\\|"))
                .map(this::parse)
                .collect(Collectors.toList());
        return new CompositeCron(crons);
    }

    private Cron parsePipeMultiCron(final String expression) {
        final String[] tokens = WHITESPACE_PATTERN.split(expression);
        final String[][] splitTokens = new String[tokens.length][];
        int cronCount = 0;
        for (int i = 0; i < tokens.length; i++) {
            splitTokens[i] = tokens[i].split("\\|");
            cronCount = Math.max(cronCount, splitTokens[i].length);
        }

        final List<String> expandedExpressions = new ArrayList<>(cronCount);
        for (int cronIndex = 0; cronIndex < cronCount; cronIndex++) {
            final StringBuilder expanded = new StringBuilder();
            for (int tokenIndex = 0; tokenIndex < tokens.length; tokenIndex++) {
                final String part = tokens[tokenIndex].contains(PIPE_SEPARATOR)
                        ? splitTokens[tokenIndex][cronIndex]
                        : tokens[tokenIndex];
                expanded.append(part).append(' ');
            }
            expandedExpressions.add(expanded.toString().trim());
        }
        return new CompositeCron(expandedExpressions.stream().map(this::parse).collect(Collectors.toList()));
    }

    private Cron parseSingleCron(final String normalizedExpression) {
        final String[] expressionParts = normalizedExpression.toUpperCase().split(" ");
        validateNoTrailingCommas(expressionParts);

        final List<CronParserField> fields = expressionsByLength.get(expressionParts.length);
        if (fields == null) {
            throw new IllegalArgumentException(
                    String.format("Cron expression contains %s parts but we expect one of %s",
                            expressionParts.length, expressionsByLength.keySet()));
        }
        try {
            final List<CronField> results = new ArrayList<>(expressionParts.length + 1);
            for (int i = 0; i < expressionParts.length; i++) {
                results.add(fields.get(i).parse(expressionParts[i]));
            }
            return new SingleCron(cronDefinition, results).validate();
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Failed to parse cron expression. %s", e.getMessage()), e);
        }
    }

    private void validateNoTrailingCommas(final String[] expressionParts) {
        for (final String part : expressionParts) {
            if (part.endsWith(",")) {
                throw new IllegalArgumentException(
                        String.format("Invalid field value! Trailing commas not permitted! '%s'", part));
            }
        }
    }
}
