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

package com.cronutils.model.definition;

import com.cronutils.model.CronType;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.definition.FieldDayOfWeekDefinitionBuilder;
import com.cronutils.model.field.definition.FieldDefinition;
import com.cronutils.model.field.definition.FieldDefinitionBuilder;
import com.cronutils.model.field.definition.FieldQuestionMarkDefinitionBuilder;
import com.cronutils.model.field.definition.FieldSpecialCharsDefinitionBuilder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builder that allows to define and create CronDefinition instances.
 */
public class CronDefinitionBuilder {
    private final Map<CronFieldName, FieldDefinition> fields = new EnumMap<>(CronFieldName.class);
    private final Set<CronConstraint> cronConstraints = new HashSet<>();
    private final Set<CronNicknames> cronNicknames = new HashSet<>();
    private boolean matchDayOfWeekAndDayOfMonth;

    private CronDefinitionBuilder() {
    }

    /**
     * Creates a builder instance.
     *
     * @return new CronDefinitionBuilder instance
     */
    public static CronDefinitionBuilder defineCron() {
        return new CronDefinitionBuilder();
    }

    /**
     * Adds definition for seconds field.
     *
     * @return new FieldDefinitionBuilder instance
     */
    public FieldDefinitionBuilder withSeconds() {
        return new FieldDefinitionBuilder(this, CronFieldName.SECOND);
    }

    /**
     * Adds definition for minutes field.
     *
     * @return new FieldDefinitionBuilder instance
     */
    public FieldDefinitionBuilder withMinutes() {
        return new FieldDefinitionBuilder(this, CronFieldName.MINUTE);
    }

    /**
     * Adds definition for hours field.
     *
     * @return new FieldDefinitionBuilder instance
     */
    public FieldDefinitionBuilder withHours() {
        return new FieldDefinitionBuilder(this, CronFieldName.HOUR);
    }

    /**
     * Adds definition for day of month field.
     *
     * @return new FieldSpecialCharsDefinitionBuilder instance
     */
    public FieldSpecialCharsDefinitionBuilder withDayOfMonth() {
        return new FieldSpecialCharsDefinitionBuilder(this, CronFieldName.DAY_OF_MONTH);
    }

    /**
     * Adds definition for month field.
     *
     * @return new FieldDefinitionBuilder instance
     */
    public FieldDefinitionBuilder withMonth() {
        return new FieldDefinitionBuilder(this, CronFieldName.MONTH);
    }

    /**
     * Adds definition for day of week field.
     *
     * @return new FieldDayOfWeekDefinitionBuilder instance
     */
    public FieldDayOfWeekDefinitionBuilder withDayOfWeek() {
        return new FieldDayOfWeekDefinitionBuilder(this, CronFieldName.DAY_OF_WEEK);
    }

    /**
     * Adds definition for year field.
     *
     * @return new FieldDefinitionBuilder instance
     */
    public FieldDefinitionBuilder withYear() {
        return new FieldDefinitionBuilder(this, CronFieldName.YEAR);
    }

    /**
     * Adds definition for day of year field.
     *
     * @return new FieldQuestionMarkDefinitionBuilder instance
     */
    public FieldQuestionMarkDefinitionBuilder withDayOfYear() {
        return new FieldQuestionMarkDefinitionBuilder(this, CronFieldName.DAY_OF_YEAR);
    }

    /**
     * Sets matchDayOfWeekAndDayOfMonth value to true.
     *
     * @return this CronDefinitionBuilder instance
     */
    public CronDefinitionBuilder matchDayOfWeekAndDayOfMonth() {
        matchDayOfWeekAndDayOfMonth = true;
        return this;
    }

    /**
     * Supports cron nickname @yearly
     *
     * @return this CronDefinitionBuilder instance
     */
    public CronDefinitionBuilder withSupportedNicknameYearly() {
        return withSupportedNickname(CronNicknames.YEARLY);
    }

    /**
     * Supports cron nickname @annually
     *
     * @return this CronDefinitionBuilder instance
     */
    public CronDefinitionBuilder withSupportedNicknameAnnually() {
        return withSupportedNickname(CronNicknames.ANNUALLY);
    }

    /**
     * Supports cron nickname @monthly
     *
     * @return this CronDefinitionBuilder instance
     */
    public CronDefinitionBuilder withSupportedNicknameMonthly() {
        return withSupportedNickname(CronNicknames.MONTHLY);
    }

    /**
     * Supports cron nickname @weekly
     *
     * @return this CronDefinitionBuilder instance
     */
    public CronDefinitionBuilder withSupportedNicknameWeekly() {
        return withSupportedNickname(CronNicknames.WEEKLY);
    }

    /**
     * Supports cron nickname @daily
     *
     * @return this CronDefinitionBuilder instance
     */
    public CronDefinitionBuilder withSupportedNicknameDaily() {
        return withSupportedNickname(CronNicknames.DAILY);
    }

    /**
     * Supports cron nickname @midnight
     *
     * @return this CronDefinitionBuilder instance
     */
    public CronDefinitionBuilder withSupportedNicknameMidnight() {
        return withSupportedNickname(CronNicknames.MIDNIGHT);
    }

    /**
     * Supports cron nickname @hourly
     *
     * @return this CronDefinitionBuilder instance
     */
    public CronDefinitionBuilder withSupportedNicknameHourly() {
        return withSupportedNickname(CronNicknames.HOURLY);
    }

    /**
     * Supports cron nickname @reboot
     *
     * @return this CronDefinitionBuilder instance
     */
    public CronDefinitionBuilder withSupportedNicknameReboot() {
        return withSupportedNickname(CronNicknames.REBOOT);
    }

    private CronDefinitionBuilder withSupportedNickname(final CronNicknames nickname) {
        cronNicknames.add(nickname);
        return this;
    }

    /**
     * Adds a cron validation.
     *
     * @param validation - constraint validation
     * @return this CronDefinitionBuilder instance
     */
    public CronDefinitionBuilder withCronValidation(final CronConstraint validation) {
        cronConstraints.add(validation);
        return this;
    }

    /**
     * Registers a certain FieldDefinition.
     *
     * @param definition - FieldDefinition instance, never null
     */
    public void register(final FieldDefinition definition) {
        final boolean hasOptionalField = fields.values().stream().anyMatch(FieldDefinition::isOptional);
        if (!definition.isOptional() && hasOptionalField) {
            throw new IllegalArgumentException("Can't register mandatory definition after a optional definition.");
        }
        fields.put(definition.getFieldName(), definition);
    }

    /**
     * Creates a new CronDefinition instance with provided field definitions.
     *
     * @return returns CronDefinition instance, never null
     */
    public CronDefinition instance() {
        final List<FieldDefinition> values = new ArrayList<>(fields.values());
        values.sort(FieldDefinition.createFieldDefinitionComparator());
        return new CronDefinition(
                values,
                new HashSet<>(cronConstraints),
                new HashSet<>(cronNicknames),
                matchDayOfWeekAndDayOfMonth);
    }

    /**
     * Creates CronDefinition instance matching cronType specification.
     *
     * @param cronType - some cron type. If null, a RuntimeException will be raised.
     * @return CronDefinition instance if definition is found; a RuntimeException otherwise.
     */
    public static CronDefinition instanceDefinitionFor(final CronType cronType) {
        return CronTypeDefinitionRegistry.getDefinition(cronType);
    }
}
