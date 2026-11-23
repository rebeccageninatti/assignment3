/*
 * Copyright 2026 cron-utils refactoring verification
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

package com.cronutils;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronConstraint;
import com.cronutils.model.definition.CronConstraintsFactory;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.definition.CronNicknames;
import com.cronutils.model.field.constraint.FieldConstraintsBuilder;
import com.cronutils.parser.CronParser;
import com.cronutils.parser.FieldParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization and behavior-preservation tests covering refactored parser, definition,
 * and descriptor components.
 */
class RefactoringBehaviorPreservationTest {

    @ParameterizedTest
    @CsvSource({
        "QUARTZ, '* * * * * ?', 'every second'",
        "QUARTZ, '0 * * * * ?', 'every minute'",
        "QUARTZ, '30 * * * * ?', 'every minute at second 30'",
        "QUARTZ, '0 0 * * * ?', 'every hour'",
        "QUARTZ, '0 15 * * * ?', 'every hour at minute 15'",
        "QUARTZ, '10 15 * * * ?', 'every hour at minute 15 and second 10'",
        "QUARTZ, '* 45 11 * * ?', 'every second at 11:45'",
        "QUARTZ, '0 30 11 * * ?', 'at 11:30'",
        "QUARTZ, '45 30 11 * * ?', 'at 11:30:45'",
        "QUARTZ, '* * 11 * * ?', 'at 11:00'",
        "QUARTZ, '0 0-10 11 * * ?', 'every minute between 11:00 and 11:10'",
        "QUARTZ, '* 0-10 11 * * ?', 'every second between 11:00 and 11:10'",
        "QUARTZ, '0 */5 * * * ?', 'every 5 minutes'",
        "QUARTZ, '0 0 */1 * * ?', 'every hour'",
        "QUARTZ, '0 15 */2 * * ?', 'every 2 hours at minute 15'",
        "QUARTZ, '* 15 * * * ?', 'every second at minute 15 of every hour'",
        "QUARTZ, '* 10,20 * * * ?', 'every second at 10 and 20 minutes'",
        "QUARTZ, '* * 8,12 * * ?', 'every second at 8 and 12 hours'",
        "SPRING53, '@daily', 'at 00:00'",
        "SPRING53, '@hourly', 'every hour'",
        "SPRING53, '0 0 9 LW * ?', 'at 09:00 last weekday of month'",
        "SPRING53, '0 0 9 15W * ?', 'at 09:00 the nearest weekday to the 15 of the month'"
    })
    void testAllTimeDescriptionRulesPreserved(final CronType type, final String expression, final String expected) {
        final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(type));
        final Cron cron = parser.parse(expression);
        assertEquals(expected, CronDescriptor.instance(Locale.UK).describe(cron));
    }

    @Test
    void testFieldParserPreservesEvaluationOrderAndStackTraceCause() {
        final FieldParser parser = new FieldParser(FieldConstraintsBuilder.instance().createConstraintsInstance());
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parser.parse("foo/bar"));
        assertTrue(ex.getMessage().contains("Invalid chars in expression!"));
        assertInstanceOf(NumberFormatException.class, ex.getCause());
    }

    @Test
    void testCronDefinitionBuilderDefensiveCopyPreventsAliasing() {
        final CronDefinitionBuilder builder = CronDefinitionBuilder.defineCron()
                .withMinutes().and()
                .withHours().and()
                .withDayOfMonth().and()
                .withMonth().and()
                .withDayOfWeek().and()
                .withSupportedNicknameDaily();

        final CronDefinition first = builder.instance();
        builder.withSupportedNicknameHourly();
        final CronDefinition second = builder.instance();

        assertFalse(first.getCronNicknames().contains(CronNicknames.HOURLY));
        assertTrue(second.getCronNicknames().contains(CronNicknames.HOURLY));
    }

    @Test
    void testCronConstraintsSerializationAndEqualityPreserved() throws Exception {
        final CronConstraint c1 = CronConstraintsFactory.ensureEitherDayOfWeekOrDayOfMonth();
        final CronConstraint c2 = CronConstraintsFactory.ensureEitherDayOfWeekOrDayOfMonth();
        assertEquals(c1, c2);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(c1);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            final Object deserialized = ois.readObject();
            assertNotNull(deserialized);
            assertEquals(c1, deserialized);
        }
    }

    @Test
    void testAllPredefinedCronTypesLoadFromRegistry() {
        for (final CronType type : CronType.values()) {
            final CronDefinition def = CronDefinitionBuilder.instanceDefinitionFor(type);
            assertNotNull(def);
            assertFalse(def.getFieldDefinitions().isEmpty());
        }
        assertThrows(NullPointerException.class, () -> CronDefinitionBuilder.instanceDefinitionFor(null));
    }
}
