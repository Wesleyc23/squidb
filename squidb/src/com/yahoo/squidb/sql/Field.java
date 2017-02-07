/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.SquidUtilities;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Fields represent a selectable attribute, e.g. a column, function, or literal value. Most clients will not use this
 * class directly.
 *
 * @param <TYPE> the type of this Field
 */
public class Field<TYPE> extends DBObject<Field<TYPE>> {

    /** SQLite NULL literal value */
    @Nonnull
    public static final Field<Void> NULL = new Field<>("NULL");

    /**
     * @param expression the string-literal representation of this field
     */
    protected Field(@Nonnull String expression) {
        super(expression);
    }

    /**
     * @param expression the string-literal representation of this field
     * @param qualifier the string-literal representation of a qualifying object, e.g. a table name
     */
    protected Field(@Nonnull String expression, @Nullable String qualifier) {
        super(expression, qualifier);
    }

    /**
     * Construct a Field with the given expression
     *
     * @param expression the string-literal representation of the returned field
     */
    @Nonnull
    public static <T> Field<T> field(@Nonnull String expression) {
        return new Field<>(expression);
    }

    /**
     * Construct a Field with the given expression and qualifier
     *
     * @param expression the string-literal representation of this field
     * @param qualifier the string-literal representation of a qualifying object, e.g. a table name
     */
    @Nonnull
    public static <T> Field<T> field(@Nonnull String expression, @Nullable String qualifier) {
        return new Field<>(expression, qualifier);
    }

    /**
     * @return a {@link Criterion} that the field must be equal to the given value
     */
    @Nonnull
    public Criterion eq(@Nullable Object value) {
        if (value == null) {
            return isNull();
        }
        return new BinaryCriterion(this, Operator.eq, value);
    }

    /**
     * @return a {@link Criterion} that the field must be equal to the given string, ignoring case. This will only work
     * for ASCII characters.
     */
    @Nonnull
    public Criterion eqCaseInsensitive(@Nullable String value) {
        if (value == null) {
            return isNull();
        }
        return new CaseInsensitiveEqualsCriterion(this, Operator.eq, value);
    }

    /**
     * @return a {@link Criterion} comparing the field to the given value using the IS operator.
     */
    @Nonnull
    public Criterion is(@Nullable Object value) {
        return new BinaryCriterion(this, Operator.is, value);
    }

    /**
     * @return a {@link Criterion} comparing the field to the given value using the IS NOT operator.
     */
    @Nonnull
    public Criterion isNot(@Nullable Object value) {
        return new BinaryCriterion(this, Operator.isNot, value);
    }

    /**
     * @return a {@link Criterion} that the field must not be equal to the given value
     */
    @Nonnull
    public Criterion neq(@Nullable Object value) {
        if (value == null) {
            return isNotNull();
        }
        return new BinaryCriterion(this, Operator.neq, value);
    }

    /**
     * @return a {@link Criterion} that the field must be greater than the given value
     */
    @Nonnull
    public Criterion gt(@Nullable Object value) {
        return new BinaryCriterion(this, Operator.gt, value);
    }

    /**
     * @return a {@link Criterion} that the field must be greater than or equal to the given value
     */
    @Nonnull
    public Criterion gte(@Nullable Object value) {
        return new BinaryCriterion(this, Operator.gte, value);
    }

    /**
     * @return a {@link Criterion} that the field must be less than the given value
     */
    @Nonnull
    public Criterion lt(@Nullable Object value) {
        return new BinaryCriterion(this, Operator.lt, value);
    }

    /**
     * @return a {@link Criterion} that the field must be less than or equal to the given value
     */
    @Nonnull
    public Criterion lte(@Nullable Object value) {
        return new BinaryCriterion(this, Operator.lte, value);
    }

    /**
     * @return a {@link Criterion} that the field must be null
     */
    @Nonnull
    public Criterion isNull() {
        return is(null);
    }

    /**
     * @return a {@link Criterion} that the field must not be null
     */
    @Nonnull
    public Criterion isNotNull() {
        return isNot(null);
    }

    /**
     * @return a {@link Criterion} that the field must be between the specified lower and upper bounds
     */
    @Nonnull
    public Criterion between(@Nullable Object lower, @Nullable Object upper) {
        return new BetweenCriterion(this, Operator.between, lower, upper);
    }

    /**
     * @return a {@link Criterion} that the field must not be between the specified lower and upper bounds
     */
    @Nonnull
    public Criterion notBetween(@Nullable Object lower, @Nullable Object upper) {
        return new BetweenCriterion(this, Operator.notBetween, lower, upper);
    }

    /**
     * @param value the pattern to compare against
     * @return a {@link Criterion} that the field is LIKE the given pattern
     */
    @Nonnull
    public Criterion like(@Nullable Object value) {
        return new LikeCriterion(this, Operator.like, value, '\0');
    }

    /**
     * Returns a {@link Criterion} that the field is LIKE the given pattern, using the specified escape character to
     * escape the '%' and '_' meta-characters and itself. If your pattern is a string,
     * note that this method will not alter the input string for you; use {@link SqlUtils#escapeLikePattern(String,
     * char) SqlUtils.escapeLikePattern()} to add escapes where necessary.
     *
     * @param pattern the pattern to compare against
     * @param escape a character in the like pattern that escapes the '%' and '_' meta-characters and itself
     * @return a {@link Criterion} that the field is LIKE the given pattern
     */
    @Nonnull
    public Criterion like(@Nullable Object pattern, final char escape) {
        return new LikeCriterion(this, Operator.like, pattern, escape);
    }

    /**
     * @param value the pattern to compare against
     * @return a {@link Criterion} that the field is NOT LIKE the given pattern
     */
    @Nonnull
    public Criterion notLike(@Nullable Object value) {
        return new LikeCriterion(this, Operator.notLike, value, '\0');
    }

    /**
     * Returns a {@link Criterion} that the field is NOT LIKE the given pattern, using the specified escape character to
     * escape the '%' and '_' meta-characters and itself. If your pattern is a string,
     * note that this method will not alter the input string for you; use {@link SqlUtils#escapeLikePattern(String,
     * char) SqlUtils.escapeLikePattern()} to add escapes where necessary.
     *
     * @param pattern the pattern to compare against
     * @param escape a character in the like pattern that escapes the '%' and '_' meta-characters and itself
     * @return a {@link Criterion} that the field is NOT LIKE the given pattern
     */
    @Nonnull
    public Criterion notLike(@Nullable Object pattern, final char escape) {
        return new LikeCriterion(this, Operator.notLike, pattern, escape);
    }

    /**
     * @param value the pattern to compare against
     * @return a {@link Criterion} that the field matches the given pattern
     */
    @Nonnull
    public Criterion glob(@Nullable Object value) {
        return new BinaryCriterion(this, Operator.glob, value);
    }

    /**
     * @param value the pattern to compare against
     * @return a {@link Criterion} that the field does not match the given pattern
     */
    @Nonnull
    public Criterion notGlob(@Nullable Object value) {
        return new BinaryCriterion(this, Operator.notGlob, value);
    }

    /**
     * @return a {@link Criterion} that the field's value is in the list of values specified
     */
    @Nonnull
    public Criterion in(@Nullable Object... values) {
        return in(SquidUtilities.asList(values));
    }

    /**
     * @return a {@link Criterion} that the field's value is in the collection of values. Values that are not primitive
     * types will be converted to String literals
     */
    @Nonnull
    public Criterion in(@Nullable final Collection<?> values) {
        return new InCollectionCriterion(this, Operator.in, values);
    }

    /**
     * @return a {@link Criterion} that the field's value is not in the list of values specified
     */
    @Nonnull
    public Criterion notIn(@Nullable Object... values) {
        return notIn(SquidUtilities.asList(values));
    }

    /**
     * @return a {@link Criterion} that the field's value is not in the collection of values. Values that are not
     * primitive types will be converted to String literals
     */
    @Nonnull
    public Criterion notIn(@Nullable final Collection<?> values) {
        return new InCollectionCriterion(this, Operator.notIn, values);
    }

    /**
     * @return a {@link Criterion} that the field's value is in the result of the {@link Query}. If the query is null,
     * this is equivalent to 'IN ()', which will always be false.
     */
    @Nonnull
    public Criterion in(@Nullable Query query) {
        if (query == null) {
            return in((Collection<?>) null);
        }
        return new BinaryCriterion(this, Operator.in, query);
    }

    /**
     * @return a {@link Criterion} that the field's value is not in the result of the {@link Query}. If the query is
     * null, this is equivalent to 'NOT IN ()', which will always be true.
     */
    @Nonnull
    public Criterion notIn(@Nullable Query query) {
        if (query == null) {
            return notIn((Collection<?>) null);
        }
        return new BinaryCriterion(this, Operator.notIn, query);
    }

    /**
     * @return a {@link Criterion} using this field as a literal value
     */
    @Nonnull
    public Criterion asCriterion() {
        return Criterion.literal(this);
    }

    /**
     * @return an {@link Order} that sorts ascending by this field's value
     */
    @Nonnull
    public Order asc() {
        return Order.asc(this);
    }

    /**
     * @return an {@link Order} that sorts descending by this field's value
     */
    @Nonnull
    public Order desc() {
        return Order.desc(this);
    }

    /**
     * @return an {@link Order} that sorts by this field's value in the order specified by the array of values
     */
    @Nonnull
    public Order byArray(@Nullable TYPE[] order) {
        return Order.byArray(this, order);
    }
}
