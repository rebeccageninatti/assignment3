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
import com.cronutils.model.field.expression.Between;
import com.cronutils.model.field.expression.On;

import java.util.Optional;

/**
 * Stateless time-description rules for cron expressions whose hour field is a fixed {@link On} value.
 */
final class FixedHourTimeRules {

    private FixedHourTimeRules() {
    }

    static Optional<String> describeEverySecondAtHourAndMinute(final TimeDescriptionStrategy strategy) {
        if (strategy.getHours() instanceof On onHours
                && strategy.getMinutes() instanceof On onMinutes
                && strategy.getSeconds() instanceof Always) {
            return Optional.of(String.format(
                    "%s %s %s %02d:%02d",
                    strategy.text(TimeDescriptionStrategy.EVERY),
                    strategy.text(TimeDescriptionStrategy.SECOND),
                    strategy.text(TimeDescriptionStrategy.AT),
                    onHours.getTime().getValue(),
                    onMinutes.getTime().getValue()));
        }
        return Optional.empty();
    }

    static Optional<String> describeExactTime(final TimeDescriptionStrategy strategy) {
        if (strategy.getHours() instanceof On onHours
                && strategy.getMinutes() instanceof On onMinutes
                && strategy.getSeconds() instanceof On onSeconds) {
            if (TimeDescriptionStrategy.isZero(onSeconds)) {
                return Optional.of(String.format(
                        "%s %02d:%02d",
                        strategy.text(TimeDescriptionStrategy.AT),
                        onHours.getTime().getValue(),
                        onMinutes.getTime().getValue()));
            }
            return Optional.of(String.format(
                    "%s %02d:%02d:%02d",
                    strategy.text(TimeDescriptionStrategy.AT),
                    onHours.getTime().getValue(),
                    onMinutes.getTime().getValue(),
                    onSeconds.getTime().getValue()));
        }
        return Optional.empty();
    }

    static Optional<String> describeAtHourOnTheHour(final TimeDescriptionStrategy strategy) {
        if (strategy.getHours() instanceof On onHours
                && strategy.getMinutes() instanceof Always
                && strategy.getSeconds() instanceof Always) {
            return Optional.of(String.format(
                    "%s %02d:00",
                    strategy.text(TimeDescriptionStrategy.AT),
                    onHours.getTime().getValue()));
        }
        return Optional.empty();
    }

    static Optional<String> describeBetweenMinutesAtHour(final TimeDescriptionStrategy strategy) {
        if (!(strategy.getHours() instanceof On onHours)
                || !(strategy.getMinutes() instanceof Between betweenMinutes)) {
            return Optional.empty();
        }
        final String unitKey;
        if (strategy.getSeconds() instanceof On) {
            unitKey = TimeDescriptionStrategy.MINUTE;
        } else if (strategy.getSeconds() instanceof Always) {
            unitKey = TimeDescriptionStrategy.SECOND;
        } else {
            return Optional.empty();
        }
        final Integer hourValue = onHours.getTime().getValue();
        return Optional.of(String.format(
                "%s %s %s %02d:%02d %s %02d:%02d",
                strategy.text(TimeDescriptionStrategy.EVERY),
                strategy.text(unitKey),
                strategy.text(TimeDescriptionStrategy.BETWEEN),
                hourValue,
                betweenMinutes.getFrom().getValue(),
                strategy.text(TimeDescriptionStrategy.AND),
                hourValue,
                betweenMinutes.getTo().getValue()));
    }
}
