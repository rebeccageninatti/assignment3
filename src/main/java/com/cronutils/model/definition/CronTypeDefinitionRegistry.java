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
import com.cronutils.utils.Preconditions;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry of predefined {@link CronDefinition} specifications indexed by {@link CronType}.
 */
final class CronTypeDefinitionRegistry {
    private static final int MIN_SECONDS = 0;
    private static final int MAX_SECONDS = 59;
    private static final int MIN_MINUTES = 0;
    private static final int MAX_MINUTES = 59;
    private static final int MIN_HOURS = 0;
    private static final int MAX_HOURS = 23;
    private static final int MIN_MONTH = 1;
    private static final int MAX_MONTH = 12;

    private static final Map<CronType, Supplier<CronDefinition>> DEFINITIONS = buildRegistry();

    private CronTypeDefinitionRegistry() {
    }

    static CronDefinition getDefinition(final CronType cronType) {
        Preconditions.checkNotNull(cronType, "CronType must not be null");
        final Supplier<CronDefinition> supplier = DEFINITIONS.get(cronType);
        if (supplier == null) {
            throw new IllegalArgumentException(String.format("No cron definition found for %s", cronType));
        }
        return supplier.get();
    }

    private static Map<CronType, Supplier<CronDefinition>> buildRegistry() {
        final Map<CronType, Supplier<CronDefinition>> registry = new EnumMap<>(CronType.class);
        registry.put(CronType.CRON4J, CronTypeDefinitionRegistry::cron4j);
        registry.put(CronType.QUARTZ, CronTypeDefinitionRegistry::quartz);
        registry.put(CronType.UNIX, CronTypeDefinitionRegistry::unixCrontab);
        registry.put(CronType.SPRING, CronTypeDefinitionRegistry::spring);
        registry.put(CronType.SPRING53, CronTypeDefinitionRegistry::spring53);
        return registry;
    }

    private static CronDefinitionBuilder withStrictMinutesAndHours(final CronDefinitionBuilder builder) {
        return builder
                .withMinutes().withValidRange(MIN_MINUTES, MAX_MINUTES).withStrictRange().and()
                .withHours().withValidRange(MIN_HOURS, MAX_HOURS).withStrictRange().and();
    }

    private static CronDefinitionBuilder withSpringTimeFields(final CronDefinitionBuilder builder) {
        return withStrictMinutesAndHours(
                builder.withSeconds().withValidRange(MIN_SECONDS, MAX_SECONDS).withStrictRange().and());
    }

    private static CronDefinition cron4j() {
        return withStrictMinutesAndHours(CronDefinitionBuilder.defineCron())
                .withDayOfMonth().withValidRange(0, 31).supportsL().withStrictRange().and()
                .withMonth().withValidRange(MIN_MONTH, MAX_MONTH).withStrictRange().and()
                .withDayOfWeek().withValidRange(0, 6).withMondayDoWValue(1).withStrictRange().and()
                .matchDayOfWeekAndDayOfMonth()
                .instance();
    }

    private static CronDefinition quartz() {
        return CronDefinitionBuilder.defineCron()
                .withSeconds().withValidRange(MIN_SECONDS, MAX_SECONDS).and()
                .withMinutes().withValidRange(MIN_MINUTES, MAX_MINUTES).and()
                .withHours().withValidRange(MIN_HOURS, MAX_HOURS).and()
                .withDayOfMonth().withValidRange(1, 31).supportsL().supportsW().supportsLW().supportsQuestionMark().and()
                .withMonth().withValidRange(MIN_MONTH, MAX_MONTH).and()
                .withDayOfWeek().withValidRange(1, 7).withMondayDoWValue(2).supportsHash().supportsL().supportsQuestionMark().and()
                .withYear().withValidRange(1970, 2099).withStrictRange().optional().and()
                .withCronValidation(CronConstraintsFactory.ensureQuartzDayOfMonthAndDayOfWeekValidation())
                .instance();
    }

    private static CronDefinition spring() {
        return withSpringTimeFields(CronDefinitionBuilder.defineCron())
                .withDayOfMonth().withValidRange(1, 31).supportsQuestionMark().and()
                .withMonth().withValidRange(MIN_MONTH, MAX_MONTH).and()
                .withDayOfWeek().withValidRange(0, 7).withMondayDoWValue(1).withIntMapping(7, 0)
                    .supportsHash().supportsQuestionMark().and()
                .instance();
    }

    private static CronDefinition spring53() {
        return withSpringTimeFields(CronDefinitionBuilder.defineCron())
                .withDayOfMonth().withValidRange(1, 31).supportsL().supportsW().supportsLW().supportsQuestionMark().and()
                .withMonth().withValidRange(MIN_MONTH, MAX_MONTH).and()
                .withDayOfWeek().withValidRange(0, 7).withMondayDoWValue(1).withIntMapping(7, 0)
                    .supportsHash().supportsL().supportsQuestionMark().and()
                .withSupportedNicknameYearly().withSupportedNicknameAnnually()
                .withSupportedNicknameMonthly()
                .withSupportedNicknameWeekly()
                .withSupportedNicknameDaily().withSupportedNicknameMidnight()
                .withSupportedNicknameHourly()
                .instance();
    }

    private static CronDefinition unixCrontab() {
        return withStrictMinutesAndHours(CronDefinitionBuilder.defineCron())
                .withDayOfMonth().withValidRange(1, 31).withStrictRange().and()
                .withMonth().withValidRange(MIN_MONTH, MAX_MONTH).withStrictRange().and()
                .withDayOfWeek().withValidRange(0, 7).withMondayDoWValue(1).withIntMapping(7, 0).withStrictRange().and()
                .instance();
    }
}
