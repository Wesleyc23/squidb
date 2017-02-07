/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.SquidbLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

class CompiledArgumentResolver {

    private static final Pattern REPLACEABLE_ARRAY_PARAM_PATTERN =
            Pattern.compile(SqlStatement.REPLACEABLE_ARRAY_PARAMETER_REGEX);

    private final String compiledSql;
    private final List<Object> sqlArgs;
    private final CompileContext compileContext;
    private final boolean needsValidation;

    private List<Collection<?>> collectionArgs;

    private static final int CACHE_SIZE = 5;
    private SimpleLruCache<String, String> compiledSqlCache;
    private SimpleLruCache<String, Object[]> argArrayCache;

    private Object[] compiledArgs = null;

    public CompiledArgumentResolver(@Nonnull SqlBuilder builder) {
        this.compiledSql = builder.getSqlString();
        this.sqlArgs = builder.getBoundArguments();
        this.compileContext = builder.compileContext;
        this.needsValidation = builder.needsValidation();
        if (compiledSql.contains(SqlStatement.REPLACEABLE_ARRAY_PARAMETER)) {
            collectionArgs = new ArrayList<>();
            findCollectionArgs();
            compiledSqlCache = new SimpleLruCache<>(CACHE_SIZE);
            argArrayCache = new SimpleLruCache<>(CACHE_SIZE);
        }
    }

    private boolean hasCollectionArgs() {
        return collectionArgs != null;
    }

    private void findCollectionArgs() {
        for (Object arg : sqlArgs) {
            if (arg instanceof Collection<?>) {
                collectionArgs.add((Collection<?>) arg);
            }
        }
    }

    @Nonnull
    public CompiledStatement resolveToCompiledStatement() {
        String cacheKey = hasCollectionArgs() ? getCacheKey() : null;
        int totalArgSize = calculateArgsSizeWithCollectionArgs();
        boolean largeArgMode = totalArgSize > SqlStatement.MAX_VARIABLE_NUMBER;
        return new CompiledStatement(resolveSqlString(cacheKey, largeArgMode),
                resolveSqlArguments(cacheKey, totalArgSize, largeArgMode), needsValidation);
    }

    private String getCacheKey() {
        StringBuilder cacheKey = new StringBuilder();
        if (hasCollectionArgs()) {
            for (Collection<?> collection : collectionArgs) {
                cacheKey.append(collection.size()).append(":");
            }
        }
        return cacheKey.toString();
    }

    private String resolveSqlString(String cacheKey, boolean largeArgMode) {
        if (hasCollectionArgs()) {
            if (!largeArgMode) {
                String cachedResult = compiledSqlCache.get(cacheKey);
                if (cachedResult != null) {
                    return cachedResult;
                }
            }

            StringBuilder result = new StringBuilder(compiledSql.length());
            Matcher m = REPLACEABLE_ARRAY_PARAM_PATTERN.matcher(compiledSql);
            int index = 0;
            int lastStringIndex = 0;
            while (m.find()) {
                result.append(compiledSql.substring(lastStringIndex, m.start()));
                Collection<?> values = collectionArgs.get(index);
                if (largeArgMode) {
                    SqlUtils.addInlineCollectionToSqlString(result, compileContext.getArgumentResolver(), values);
                } else {
                    appendCollectionVariableStringForSize(result, values.size());
                }
                lastStringIndex = m.end();
                index++;
            }
            result.append(compiledSql.substring(lastStringIndex, compiledSql.length()));

            String resultSql = result.toString();
            if (!largeArgMode) {
                compiledSqlCache.put(cacheKey, resultSql);
            } else {
                SquidbLog.w(SquidbLog.LOG_TAG,
                        "The SQL statement \"" + resultSql.substring(0, Math.min(200, resultSql.length()))
                                + " ...\" had too many arguments to bind, so arguments were inlined into the SQL "
                                + "instead. Consider revising your statement to have fewer arguments.");
            }
            return resultSql;
        } else {
            return compiledSql;
        }
    }

    private void appendCollectionVariableStringForSize(StringBuilder builder, int size) {
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(SqlStatement.REPLACEABLE_PARAMETER);
        }
    }

    private Object[] resolveSqlArguments(String cacheKey, int totalArgSize, boolean largeArgMode) {
        if (hasCollectionArgs()) {
            Object[] cachedResult = argArrayCache.get(cacheKey);
            if (cachedResult == null) {
                int size = largeArgMode ? calculateArgsSizeWithoutCollectionArgs() : totalArgSize;
                if (compiledArgs == null || compiledArgs.length != size) {
                    cachedResult = new Object[size];
                } else {
                    cachedResult = compiledArgs;
                }
                argArrayCache.put(cacheKey, cachedResult);
            }
            compiledArgs = cachedResult;
            populateCompiledArgs(largeArgMode);
        } else {
            if (compiledArgs == null) {
                compiledArgs = sqlArgs.toArray(new Object[sqlArgs.size()]);
            }
        }
        return applyArgumentResolver(compiledArgs);
    }

    private Object[] applyArgumentResolver(Object[] args) {
        ArgumentResolver resolver = compileContext.getArgumentResolver();
        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = resolver.resolveArgument(args[i]);
        }
        return result;
    }

    private int calculateArgsSizeWithCollectionArgs() {
        int startSize = sqlArgs.size();
        if (hasCollectionArgs()) {
            for (Collection<?> collection : collectionArgs) {
                startSize += (collection.size() - 1);
            }
        }
        return startSize;
    }

    private int calculateArgsSizeWithoutCollectionArgs() {
        return sqlArgs.size() - (hasCollectionArgs() ? collectionArgs.size() : 0);
    }

    private void populateCompiledArgs(boolean largeArgMode) {
        int i = 0;
        for (Object arg : sqlArgs) {
            if (arg instanceof Collection<?>) {
                if (!largeArgMode) {
                    Collection<?> values = (Collection<?>) arg;
                    for (Object obj : values) {
                        compiledArgs[i++] = obj;
                    }
                }
            } else {
                compiledArgs[i++] = arg;
            }
        }
    }

    @SuppressWarnings("serial")
    static class SimpleLruCache<K, V> extends LinkedHashMap<K, V> {

        private final int maxCapacity;

        public SimpleLruCache(int maxCapacity) {
            super(0, 0.75f, true);
            this.maxCapacity = maxCapacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxCapacity;
        }
    }
}
