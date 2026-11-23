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
import com.cronutils.model.field.expression.And;
import com.cronutils.model.field.expression.Between;
import com.cronutils.model.field.expression.Every;
import com.cronutils.model.field.expression.On;

import java.util.Optional;

/**
 * Stateless time-description rules for recurring ({@link Always}, {@link Every}, {@link And}) hour expressions.
 */
final class RecurringTimeRules {

    private RecurringTimeRules() {
    }

    static Optional<String> describeEverySecondOrMinute(final TimeDescriptionStrategy strategy) {
        if (!(strategy.getHours() instanceof Always) || !(strategy.getMinutes() instanceof Always)) {
            return Optional.empty();
        }
        if (strategy.getSeconds() instanceof Always) {
            return Optional.of(strategy.formatEveryUnit(TimeDescriptionStrategy.SECOND));
        }
        if (strategy.getSeconds() instanceof On onSeconds) {
            if (TimeDescriptionStrategy.isZero(onSeconds)) {
                return Optional.of(strategy.formatEveryUnit(TimeDescriptionStrategy.MINUTE));
            }
            return Optional.of(String.format(
                    "%s %s %s %s %02d",
                    strategy.text(TimeDescriptionStrategy.EVERY),
                    strategy.text(TimeDescriptionStrategy.MINUTE),
                    strategy.text(TimeDescriptionStrategy.AT),
                    strategy.text(TimeDescriptionStrategy.SECOND),
                    onSeconds.getTime().getValue()));
        }
        return Optional.empty();
    }

    static Optional<String> describeEveryHourAtMinuteAndSecond(final TimeDescriptionStrategy strategy) {
        if (!(strategy.getHours() instanceof Always)
                || !(strategy.getMinutes() instanceof On onMinutes)
                || !(strategy.getSeconds() instanceof On onSeconds)) {
            return Optional.empty();
        }
        if (TimeDescriptionStrategy.isZero(onSeconds)) {
            if (TimeDescriptionStrategy.isZero(onMinutes)) {
                return Optional.of(strategy.formatEveryUnit(TimeDescriptionStrategy.HOUR));
            }
            return Optional.of(String.format(
                    "%s %s %s %s %s",
                    strategy.text(TimeDescriptionStrategy.EVERY),
                    strategy.text(TimeDescriptionStrategy.HOUR),
                    strategy.text(TimeDescriptionStrategy.AT),
                    strategy.text(TimeDescriptionStrategy.MINUTE),
                    onMinutes.getTime().getValue()));
        }
        return Optional.of(String.format(
                "%s %s %s %s %s %s %s %s",
                strategy.text(TimeDescriptionStrategy.EVERY),
                strategy.text(TimeDescriptionStrategy.HOUR),
                strategy.text(TimeDescriptionStrategy.AT),
                strategy.text(TimeDescriptionStrategy.MINUTE),
                onMinutes.getTime().getValue(),
                strategy.text(TimeDescriptionStrategy.AND),
                strategy.text(TimeDescriptionStrategy.SECOND),
                onSeconds.getTime().getValue()));
    }

    static Optional<String> describeEveryXMinutes(final TimeDescriptionStrategy strategy) {
        if (!(strategy.getHours() instanceof Always)
                || !(strategy.getMinutes() instanceof Every everyMinutes)
                || !(strategy.getSeconds() instanceof On onSeconds)) {
            return Optional.empty();
        }
        if (everyMinutes.getExpression() instanceof Between) {
            return Optional.empty();
        }
        if (everyMinutes.getPeriod().getValue() == 1 && TimeDescriptionStrategy.isZero(onSeconds)) {
            return Optional.of(strategy.formatEveryUnit(TimeDescriptionStrategy.MINUTE));
        }
        return Optional.of(String.format(
                "%s %s %s ",
                strategy.text(TimeDescriptionStrategy.EVERY),
                everyMinutes.getPeriod().getValue(),
                strategy.text(TimeDescriptionStrategy.MINUTES)));
    }

    static Optional<String> describeEveryXHours(final TimeDescriptionStrategy strategy) {
        if (!(strategy.getHours() instanceof Every everyHours)
                || !(strategy.getMinutes() instanceof On onMinutes)
                || !(strategy.getSeconds() instanceof On onSeconds)) {
            return Optional.empty();
        }
        final Integer period = everyHours.getPeriod().getValue();
        if (TimeDescriptionStrategy.isZero(onMinutes) && TimeDescriptionStrategy.isZero(onSeconds)
                && (period == null || period == 1)) {
            return Optional.of(strategy.formatEveryUnit(TimeDescriptionStrategy.HOUR));
        }
        if (TimeDescriptionStrategy.isZero(onSeconds)) {
            return Optional.of(String.format(
                    "%s %s %s %s %s %s ",
                    strategy.text(TimeDescriptionStrategy.EVERY),
                    period,
                    strategy.text(TimeDescriptionStrategy.HOURS),
                    strategy.text(TimeDescriptionStrategy.AT),
                    strategy.text(TimeDescriptionStrategy.MINUTE),
                    onMinutes.getTime().getValue()));
        }
        return Optional.of(String.format(
                "%s %s %s",
                strategy.text(TimeDescriptionStrategy.AND),
                strategy.text(TimeDescriptionStrategy.SECOND),
                onSeconds.getTime().getValue()));
    }

    static Optional<String> describeEverySecondAtMinuteOfEveryHour(final TimeDescriptionStrategy strategy) {
        if (strategy.getHours() instanceof Always
                && strategy.getMinutes() instanceof On onMinutes
                && strategy.getSeconds() instanceof Always) {
            return Optional.of(String.format(
                    "%s %s %s %s %02d %s",
                    strategy.text(TimeDescriptionStrategy.EVERY),
                    strategy.text(TimeDescriptionStrategy.SECOND),
                    strategy.text(TimeDescriptionStrategy.AT),
                    strategy.text(TimeDescriptionStrategy.MINUTE),
                    onMinutes.getTime().getValue(),
                    strategy.text(TimeDescriptionStrategy.OF_EVERY_HOUR)));
        }
        return Optional.empty();
    }

    static Optional<String> describeEverySecondAtAndMinutes(final TimeDescriptionStrategy strategy) {
        if (strategy.getHours() instanceof Always
                && strategy.getMinutes() instanceof And
                && strategy.getSeconds() instanceof Always) {
            final String minutesDesc = strategy.describeFieldWithUnits(
                    strategy.getMinutes(), TimeDescriptionStrategy.MINUTE, TimeDescriptionStrategy.MINUTES);
            return Optional.of(String.format(
                    "%s %s %s",
                    strategy.text(TimeDescriptionStrategy.EVERY),
                    strategy.text(TimeDescriptionStrategy.SECOND),
                    minutesDesc));
        }
        return Optional.empty();
    }

    static Optional<String> describeEverySecondAtAndHours(final TimeDescriptionStrategy strategy) {
        if (strategy.getHours() instanceof And
                && strategy.getMinutes() instanceof Always
                && strategy.getSeconds() instanceof Always) {
            final String hoursDesc = strategy.describeFieldWithUnits(
                    strategy.getHours(), TimeDescriptionStrategy.HOUR, TimeDescriptionStrategy.HOURS);
            return Optional.of(String.format(
                    "%s %s %s",
                    strategy.text(TimeDescriptionStrategy.EVERY),
                    strategy.text(TimeDescriptionStrategy.SECOND),
                    hoursDesc));
        }
        return Optional.empty();
    }
}
