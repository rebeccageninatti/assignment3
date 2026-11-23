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

import com.cronutils.model.field.expression.Always;
import com.cronutils.model.field.expression.Every;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.expression.On;
import com.cronutils.model.field.value.IntegerFieldValue;
import com.cronutils.utils.Preconditions;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import static com.cronutils.model.field.expression.FieldExpression.always;

/**
 * Strategy to provide a human readable description to hh:mm:ss variations.
 */
class TimeDescriptionStrategy extends DescriptionStrategy {

    static final String EVERY = "every";
    static final String SECOND = "second";
    static final String SECONDS = "seconds";
    static final String MINUTE = "minute";
    static final String MINUTES = "minutes";
    static final String HOUR = "hour";
    static final String HOURS = "hours";
    static final String AT = "at";
    static final String AND = "and";
    static final String BETWEEN = "between";
    static final String OF_EVERY_HOUR = "of_every_hour";

    private static final int DEFAULT_TIME_VALUE = 0;
    private static final String EVERY_UNIT_FORMAT = "%s %s ";

    private static final List<Function<TimeDescriptionStrategy, Optional<String>>> DESCRIPTION_RULES = List.of(
            RecurringTimeRules::describeEverySecondOrMinute,
            RecurringTimeRules::describeEveryHourAtMinuteAndSecond,
            FixedHourTimeRules::describeEverySecondAtHourAndMinute,
            FixedHourTimeRules::describeExactTime,
            FixedHourTimeRules::describeAtHourOnTheHour,
            FixedHourTimeRules::describeBetweenMinutesAtHour,
            RecurringTimeRules::describeEveryXMinutes,
            RecurringTimeRules::describeEveryXHours,
            RecurringTimeRules::describeEverySecondAtMinuteOfEveryHour,
            RecurringTimeRules::describeEverySecondAtAndMinutes,
            RecurringTimeRules::describeEverySecondAtAndHours
    );

    private final FieldExpression hours;
    private final FieldExpression minutes;
    private final FieldExpression seconds;

    /**
     * Constructor.
     *
     * @param bundle  - locale considered when creating the description
     * @param hours   - CronFieldExpression for hours. If no instance is provided, an Always instance is created.
     * @param minutes - CronFieldExpression for minutes. If no instance is provided, an Always instance is created.
     * @param seconds - CronFieldExpression for seconds. If no instance is provided, an On instance is created.
     */
    TimeDescriptionStrategy(
            final ResourceBundle bundle,
            final FieldExpression hours,
            final FieldExpression minutes,
            final FieldExpression seconds) {
        super(bundle);
        this.hours = ensureInstance(hours, always());
        this.minutes = ensureInstance(minutes, always());
        this.seconds = ensureInstance(seconds, new On(new IntegerFieldValue(DEFAULT_TIME_VALUE)));
    }

    private FieldExpression ensureInstance(final FieldExpression expression, final FieldExpression defaultExpression) {
        Preconditions.checkNotNull(defaultExpression, "Default expression must not be null");
        return expression != null ? expression : defaultExpression;
    }

    @Override
    public String describe() {
        for (final Function<TimeDescriptionStrategy, Optional<String>> rule : DESCRIPTION_RULES) {
            final Optional<String> description = rule.apply(this);
            if (description.isPresent()) {
                return description.get();
            }
        }
        return describeFallback();
    }

    private String describeFallback() {
        final String hoursDesc = hours instanceof Always
                ? ""
                : describeFieldWithUnits(hours, HOUR, HOURS);
        final String minutesDesc = shouldDescribeMinutes()
                ? describeFieldWithUnits(minutes, MINUTE, MINUTES)
                : "";
        final String secondsDesc = shouldDescribeSeconds()
                ? describeFieldWithUnits(seconds, SECOND, SECONDS)
                : "";
        return String.format("%s %s %s", secondsDesc, minutesDesc, hoursDesc);
    }

    private boolean shouldDescribeMinutes() {
        final boolean isDefaultMinute = minutes instanceof On onMinutes && isZero(onMinutes);
        final boolean bothAlways = minutes instanceof Always && hours instanceof Always;
        return !isDefaultMinute && !bothAlways;
    }

    private boolean shouldDescribeSeconds() {
        return !(seconds instanceof On onSeconds && isZero(onSeconds));
    }

    @Override
    protected String describe(final Always always, final boolean and) {
        return describe(new Every(new IntegerFieldValue(1)), and);
    }

    FieldExpression getHours() {
        return hours;
    }

    FieldExpression getMinutes() {
        return minutes;
    }

    FieldExpression getSeconds() {
        return seconds;
    }

    String text(final String bundleKey) {
        return bundle.getString(bundleKey);
    }

    String formatEveryUnit(final String unitKey) {
        return String.format(EVERY_UNIT_FORMAT, bundle.getString(EVERY), bundle.getString(unitKey));
    }

    String describeFieldWithUnits(final FieldExpression expression, final String singularKey, final String pluralKey) {
        return describe(expression)
                .replace("%s", bundle.getString(singularKey))
                .replace("replace_plural", bundle.getString(pluralKey));
    }

    static boolean isZero(final On on) {
        return on.getTime().getValue() == DEFAULT_TIME_VALUE;
    }
}
