package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.session.Configuration;

import java.util.Map;

public class ForEachSqlNode implements SqlNode {

    public static final String ITEM_PREFIX = "__frch_";

    private final ExpressionEvaluator evaluator;
    private final String collectionExpression;
    private final SqlNode contents;
    private final String open;
    private final String close;
    private final String separator;
    private final String item;
    private final String index;
    private final Configuration configuration;

    public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, String index, String item, String open, String close, String separator) {
        this.evaluator = new ExpressionEvaluator();
        this.collectionExpression = collectionExpression;
        this.contents = contents;
        this.open = open;
        this.close = close;
        this.separator = separator;
        this.index = index;
        this.item = item;
        this.configuration = configuration;
    }

    @Override
    public boolean apply(DynamicContext context) {
        Map<String, Object> bindings = context.getBindings();

        // 将Map/Array/List统一包装为迭代器接口
        final Iterable<?> iterable = evaluator.evaluateIterable(collectionExpression, bindings);
        if (!iterable.iterator().hasNext()) {
            return true;
        }
        boolean first = true;
        applyOpen(context);
        int i = 0;

        // 遍历集合
        for (Object o : iterable) {
            DynamicContext oldContext = context;
            if (first || separator == null) {
                context = new PrefixedContext(context, "");
            } else {
                context = new PrefixedContext(context, separator);
            }
            int uniqueNumber = context.getUniqueNumber();

            // Map处理
            if (o instanceof Map.Entry) {
                @SuppressWarnings("unchecked") Map.Entry<Object, Object> mapEntry = (Map.Entry<Object, Object>) o;
                applyIndex(context, mapEntry.getKey(), uniqueNumber);
                applyItem(context, mapEntry.getValue(), uniqueNumber);
            }

            // List处理
            else {
                applyIndex(context, i, uniqueNumber);
                applyItem(context, o, uniqueNumber);
            }

            // 子节点SqlNode处理，将#{item.XXX}转换为#{__frch_item_N.XXX}，这样在JDBC设置参数的时候就能够找到对应的参数值了
            contents.apply(new FilteredDynamicContext(configuration, context, index, item, uniqueNumber));
            if (first) {
                first = !((PrefixedContext) context).isPrefixApplied();
            }
            context = oldContext;
            i++;
        }
        applyClose(context);
        context.getBindings().remove(item);
        context.getBindings().remove(index);
        return true;
    }

    private void applyIndex(DynamicContext context, Object o, int i) {
        if (index != null) {
            context.bind(index, o);
            context.bind(itemizeItem(index, i), o);
        }
    }

    private void applyItem(DynamicContext context, Object o, int i) {
        if (item != null) {
            context.bind(item, o);
            context.bind(itemizeItem(item, i), o);
        }
    }

    private void applyOpen(DynamicContext context) {
        if (open != null) {
            context.appendSql(open);
        }
    }

    private void applyClose(DynamicContext context) {
        if (close != null) {
            context.appendSql(close);
        }
    }

    private static String itemizeItem(String item, int i) {
        return ITEM_PREFIX + item + "_" + i;
    }

    private static class FilteredDynamicContext extends DynamicContext {
        private final DynamicContext delegate;
        private final int index;
        private final String itemIndex;
        private final String item;

        public FilteredDynamicContext(Configuration configuration, DynamicContext delegate, String itemIndex, String item, int i) {
            super(configuration, null);
            this.delegate = delegate;
            this.index = i;
            this.itemIndex = itemIndex;
            this.item = item;
        }

        @Override
        public Map<String, Object> getBindings() {
            return delegate.getBindings();
        }

        @Override
        public void bind(String name, Object value) {
            delegate.bind(name, value);
        }

        @Override
        public String getSql() {
            return delegate.getSql();
        }

        @Override
        public void appendSql(String sql) {
            GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> {

                // 将#{item.XXX}转换为#{__frch_item_N.XXX}
                String newContent = content.replaceFirst("^\\s*" + item + "(?![^.,:\\s])", itemizeItem(item, index));
                if (itemIndex != null && newContent.equals(content)) {
                    newContent = content.replaceFirst("^\\s*" + itemIndex + "(?![^.,:\\s])", itemizeItem(itemIndex, index));
                }
                return "#{" + newContent + "}";
            });

            delegate.appendSql(parser.parse(sql));
        }

        @Override
        public int getUniqueNumber() {
            return delegate.getUniqueNumber();
        }

    }

    private class PrefixedContext extends DynamicContext {
        private final DynamicContext delegate;
        private final String prefix;
        private boolean prefixApplied;

        public PrefixedContext(DynamicContext delegate, String prefix) {
            super(configuration, null);
            this.delegate = delegate;
            this.prefix = prefix;
            this.prefixApplied = false;
        }

        public boolean isPrefixApplied() {
            return prefixApplied;
        }

        @Override
        public Map<String, Object> getBindings() {
            return delegate.getBindings();
        }

        @Override
        public void bind(String name, Object value) {
            delegate.bind(name, value);
        }

        @Override
        public void appendSql(String sql) {
            if (!prefixApplied && sql != null && sql.trim().length() > 0) {
                delegate.appendSql(prefix);
                prefixApplied = true;
            }
            delegate.appendSql(sql);
        }

        @Override
        public String getSql() {
            return delegate.getSql();
        }

        @Override
        public int getUniqueNumber() {
            return delegate.getUniqueNumber();
        }
    }

}
