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

import com.cronutils.Function;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.value.FieldValue;
import com.cronutils.model.field.value.IntegerFieldValue;
import com.cronutils.utils.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.cronutils.model.field.expression.FieldExpression.always;

/**
 * Description strategy where a cron field number can be mapped to a name.
 * Ex.: days of week or months.
 */
class NominalDescriptionStrategy extends DescriptionStrategy {
    private static final Function<Integer, String> DEFAULT_NOMINAL_FUNCTION = integer -> WHITE_SPACE + integer;

    private final FieldExpression expression;
    private final Function<Integer, String> nominalValueFunction;
    private final List<Function<FieldExpression, String>> descriptions;

    /**
     * Constructor.
     *
     * @param bundle               - locale in which description should be given
     * @param nominalValueFunction - function that maps an integer field value to its nominal String representation;
     *                               if {@code null}, defaults to the integer value prefixed with a space.
     * @param expression           - CronFieldExpression instance, the expression to be described.
     */
    public NominalDescriptionStrategy(
            final ResourceBundle bundle,
            final Function<Integer, String> nominalValueFunction,
            final FieldExpression expression) {
        super(bundle);
        this.descriptions = new ArrayList<>();
        this.nominalValueFunction = nominalValueFunction != null ? nominalValueFunction : DEFAULT_NOMINAL_FUNCTION;
        this.expression = expression != null ? expression : always();
    }

    @Override
    public String describe() {
        for (final Function<FieldExpression, String> function : descriptions) {
            final String description = function.apply(expression);
            if (!"".equals(description)) {
                return description;
            }
        }
        return describe(expression);
    }

    @Override
    protected String nominalValue(final FieldValue<?> fieldValue) {
        Preconditions.checkNotNull(fieldValue, "FieldValue must not be null");
        if (fieldValue instanceof IntegerFieldValue intValue) {
            return nominalValueFunction.apply(intValue.getValue());
        }
        return fieldValue.toString();
    }

    /**
     * Allows to provide a specific description to handle a CronFieldExpression instance.
     *
     * @param desc - function that maps CronFieldExpression to String.
     *             The function should return "" if does not match criteria,
     *             or the description otherwise.
     * @return NominalDescriptionStrategy, this instance
     */
    public NominalDescriptionStrategy addDescription(final Function<FieldExpression, String> desc) {
        descriptions.add(desc);
        return this;
    }
}
