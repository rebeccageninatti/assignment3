/*
 * Copyright 2014 jmrozanec
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

package com.cronutils.descriptor;

import com.cronutils.model.Cron;
import com.cronutils.model.RebootCron;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.definition.FieldDefinition;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.utils.Preconditions;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * Provides human readable description for a given cron.
 */
public class CronDescriptor {

    public static final Locale DEFAULT_LOCALE = Locale.UK;
    private static final String BUNDLE = "com.cronutils.CronUtilsI18N";
    private static final Pattern MULTIPLE_WHITESPACE = Pattern.compile("\\s+");

    private final ResourceBundle resourceBundle;

    /**
     * Constructor creating a descriptor for given ResourceBundle.
     *
     * @param resourceBundle - ResourceBundle which contains descriptions
     */
    public CronDescriptor(final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    private CronDescriptor(final Locale locale) {
        resourceBundle = ResourceBundle.getBundle(BUNDLE, locale);
    }

    private CronDescriptor() {
        resourceBundle = ResourceBundle.getBundle(BUNDLE, DEFAULT_LOCALE);
    }

    /**
     * Provide a description of given CronFieldParseResult list.
     *
     * @param cron - Cron instance, never null; if null, will throw NullPointerException
     * @return description - String
     */
    public String describe(final Cron cron) {
        Preconditions.checkNotNull(cron, "Cron must not be null");
        if (cron instanceof RebootCron) {
            return resourceBundle.getString("on_reboot");
        }
        final Map<CronFieldName, CronField> expressions = cron.retrieveFieldsAsMap();
        final Map<CronFieldName, FieldDefinition> fieldDefinitions = cron.getCronDefinition().retrieveFieldDefinitionsAsMap();

        final String combined = new StringBuilder()
                .append(describeHHmmss(expressions)).append(' ')
                .append(describeDayOfMonth(expressions)).append(' ')
                .append(describeMonth(expressions)).append(' ')
                .append(describeDayOfWeek(expressions, fieldDefinitions)).append(' ')
                .append(describeYear(expressions))
                .toString();
        return MULTIPLE_WHITESPACE.matcher(combined).replaceAll(" ").trim();
    }

    /**
     * Provide description for hours, minutes and seconds.
     *
     * @param fields - fields to describe;
     * @return description - String
     */
    public String describeHHmmss(final Map<CronFieldName, CronField> fields) {
        return DescriptionStrategyFactory.hhMMssInstance(
                resourceBundle,
                expressionOf(fields, CronFieldName.HOUR),
                expressionOf(fields, CronFieldName.MINUTE),
                expressionOf(fields, CronFieldName.SECOND)
        ).describe();
    }

    /**
     * Provide description for day of month.
     *
     * @param fields - fields to describe;
     * @return description - String
     */
    public String describeDayOfMonth(final Map<CronFieldName, CronField> fields) {
        final String description = DescriptionStrategyFactory.daysOfMonthInstance(
                resourceBundle,
                expressionOf(fields, CronFieldName.DAY_OF_MONTH)
        ).describe();
        return formatWithUnits(description, "day", "days");
    }

    /**
     * Provide description for month.
     *
     * @param fields - fields to describe;
     * @return description - String
     */
    public String describeMonth(final Map<CronFieldName, CronField> fields) {
        final String description = DescriptionStrategyFactory.monthsInstance(
                resourceBundle,
                expressionOf(fields, CronFieldName.MONTH)
        ).describe();
        return formatWithUnits(description, "month", "months");
    }

    /**
     * Provide description for day of week.
     *
     * @param fields      - fields to describe;
     * @param definitions - field definitions;
     * @return description - String
     */
    public String describeDayOfWeek(
            final Map<CronFieldName, CronField> fields,
            final Map<CronFieldName, FieldDefinition> definitions) {
        final String description = DescriptionStrategyFactory.daysOfWeekInstance(
                resourceBundle,
                expressionOf(fields, CronFieldName.DAY_OF_WEEK),
                definitions.get(CronFieldName.DAY_OF_WEEK)
        ).describe();
        return formatWithUnits(description, "day", "days");
    }

    /**
     * Provide description for a year.
     *
     * @param fields - fields to describe;
     * @return description - String
     */
    public String describeYear(final Map<CronFieldName, CronField> fields) {
        final String description = DescriptionStrategyFactory.plainInstance(
                resourceBundle,
                expressionOf(fields, CronFieldName.YEAR)
        ).describe();
        return formatWithUnits(description, "year", "years");
    }

    /**
     * Creates an instance with UK locale.
     *
     * @return CronDescriptor - never null.
     */
    public static CronDescriptor instance() {
        return new CronDescriptor();
    }

    /**
     * Creates an instance with given locale.
     *
     * @param locale - Locale in which descriptions will be given
     * @return CronDescriptor - never null.
     */
    public static CronDescriptor instance(final Locale locale) {
        return new CronDescriptor(locale);
    }

    /**
     * Gets the current resource bundle that is in use to allow custom reuse of text phrases.
     *
     * @return ResourceBundle - never null.
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    private FieldExpression expressionOf(final Map<CronFieldName, CronField> fields, final CronFieldName fieldName) {
        final CronField field = fields.get(fieldName);
        return field != null ? field.getExpression() : null;
    }

    private String formatWithUnits(final String description, final String singularKey, final String pluralKey) {
        return DescriptionStrategyFactory.replaceUnitPlaceholders(
                description, resourceBundle.getString(singularKey), resourceBundle.getString(pluralKey));
    }
}
