/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.content.beans.query.builder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.query.builder.FilterConstraint.Operator;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.repository.util.DateTools;

class FieldFilterBuilder extends FilterBuilderAdapter {

    private final String fieldName;

    private List<FilterConstraint> filterConstraints;

    protected FieldFilterBuilder(final String fieldName) {
        super();
        this.fieldName = fieldName;
    }

    @Override
    protected Filter doBuild(final HstQueryBuilder queryBuilder, final Session session) throws FilterException {

        final Filter filter = new FilterImpl(session, queryBuilder.defaultResolution());
        if (filterConstraints == null) {
            // we default back to a that fieldName must exists
            filter.addNotNull(fieldName);
            return filter;
        }

        Operator operator;
        Object value;

        for (FilterConstraint constraint : filterConstraints) {
            operator = constraint.operator();
            value = constraint.value();

            if (operator == Operator.EQUAL) {
                if (value instanceof Calendar && constraint.dateResolution() != null) {
                    filter.addEqualTo(fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    if (value instanceof String && !constraint.caseSensitive()) {
                        filter.addEqualToCaseInsensitive(fieldName(), (String)value);
                    } else {
                        filter.addEqualTo(fieldName(), value);
                    }
                }
            } else if (operator == Operator.GE) {
                if (value instanceof Calendar) {
                    filter.addGreaterOrEqualThan(fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    filter.addGreaterOrEqualThan(fieldName(), value);
                }
            } else if (operator == Operator.GT) {
                if (value instanceof Calendar) {
                    filter.addGreaterThan(fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    filter.addGreaterThan(fieldName(), value);
                }
            } else if (operator == Operator.LE) {
                if (value instanceof Calendar) {
                    filter.addLessOrEqualThan(fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    filter.addLessOrEqualThan(fieldName(), value);
                }
            } else if (operator == Operator.LT) {
                if (value instanceof Calendar) {
                    filter.addLessThan(fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    filter.addLessThan(fieldName(), value);
                }
            } else if (operator == Operator.CONTAINS) {
                filter.addContains(fieldName(), (String)value);
            } else if (operator == Operator.LIKE) {
                filter.addLike(fieldName(), (String)value);
            } else if (operator == Operator.BETWEEN) {
                Object[] values = (Object[])value;

                if (values[0] instanceof Calendar) {
                    filter.addBetween(fieldName(), (Calendar)values[0], (Calendar)values[1], constraint.dateResolution());
                } else {
                    filter.addBetween(fieldName(), values[0], values[1]);
                }
            } else if (operator == Operator.NOT_EQUAL) {
                if (value instanceof Calendar) {
                    filter.addNotEqualTo(fieldName(), (Calendar)value, constraint.dateResolution());
                } else {
                    if (value instanceof String && !constraint.caseSensitive()) {
                        filter.addNotEqualToCaseInsensitive(fieldName(), (String)value);
                    } else {
                        filter.addNotEqualTo(fieldName(), value);
                    }
                }
            } else if (operator == Operator.NOT_LIKE) {
                filter.addNotLike(fieldName(), (String)value);
            } else if (operator == Operator.NOT_NULL) {
                filter.addNotNull(fieldName());
            } else if (operator == Operator.IS_NULL) {
                filter.addIsNull(fieldName());
            } else if (operator == Operator.NOT_CONTAINS) {
                filter.addNotContains(fieldName(), (String)value);
            } else if (operator == Operator.NOT_BETWEEN) {
                Object[] values = (Object[])value;

                if (values[0] instanceof Calendar) {
                    filter.addNotBetween(fieldName(), (Calendar)values[0], (Calendar)values[1], constraint.dateResolution());
                } else {
                    filter.addNotBetween(fieldName(), values[0], values[1]);
                }
            } else if (operator == Operator.XPATH_EXPRESSION) {
                filter.addJCRExpression((String)value);
            }
        }

        return filter;
    }

    public String fieldName() {
        return fieldName;
    }

    @Override
    public FilterBuilder equalTo(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.EQUAL, value);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder equalTo(final Calendar value, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.EQUAL, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder equalToCaseInsensitive(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.EQUAL, value).caseSensitive(false);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder notEqualTo(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_EQUAL, value);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder notEqualTo(final Calendar value, DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_EQUAL, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder notEqualToCaseInsensitive(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_EQUAL, value).caseSensitive(false);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder greaterOrEqualThan(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.GE, value);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder greaterOrEqualThan(final Calendar value, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.GE, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder greaterThan(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.GT, value);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder greaterThan(final Calendar value, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.GT, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder lessOrEqualThan(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.LE, value);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder lessOrEqualThan(final Calendar value, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.LE, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder lessThan(final Object value) {
        FilterConstraint constraint = new FilterConstraint(Operator.LT, value);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder lessThan(final Calendar value, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.LT, value).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder contains(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.CONTAINS, value);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder notContains(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_CONTAINS, value);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder between(final Object value1, final Object value2) {
        FilterConstraint constraint = new FilterConstraint(Operator.BETWEEN, new Object[]{value1, value2});
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder between(final Calendar start, final Calendar end, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.BETWEEN, new Object[]{start, end}).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder notBetween(final Object value1, final Object value2) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_BETWEEN, new Object[]{value1, value2});
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder notBetween(final Calendar start, final Calendar end, final DateTools.Resolution dateResolution) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_BETWEEN, new Object[]{start, end}).dateResolution(dateResolution);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder like(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.LIKE, value);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder notLike(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_LIKE, value);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder exists() {
        FilterConstraint constraint = new FilterConstraint(Operator.NOT_NULL);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder notExists() {
        FilterConstraint constraint = new FilterConstraint(Operator.IS_NULL);
        addFilterConstraint(constraint);
        return this;
    }

    @Override
    public FilterBuilder jcrExpression(final String value) {
        FilterConstraint constraint = new FilterConstraint(Operator.XPATH_EXPRESSION, value);
        addFilterConstraint(constraint);
        return this;
    }

    private void addFilterConstraint(final FilterConstraint filterConstraint) {
        if (filterConstraints == null) {
            filterConstraints = new ArrayList<>();
        }

        filterConstraints.add(filterConstraint);
    }
}
