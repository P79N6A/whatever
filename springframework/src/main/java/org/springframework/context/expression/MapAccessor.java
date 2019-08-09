package org.springframework.context.expression;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompilablePropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Map;

public class MapAccessor implements CompilablePropertyAccessor {

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[]{Map.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return (target instanceof Map && ((Map<?, ?>) target).containsKey(name));
    }

    @Override
    public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        Assert.state(target instanceof Map, "Target must be of type Map");
        Map<?, ?> map = (Map<?, ?>) target;
        Object value = map.get(name);
        if (value == null && !map.containsKey(name)) {
            throw new MapAccessException(name);
        }
        return new TypedValue(value);
    }

    @Override
    public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) throws AccessException {
        Assert.state(target instanceof Map, "Target must be a Map");
        Map<Object, Object> map = (Map<Object, Object>) target;
        map.put(name, newValue);
    }

    @Override
    public boolean isCompilable() {
        return true;
    }

    @Override
    public Class<?> getPropertyType() {
        return Object.class;
    }

    @Override
    public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
        String descriptor = cf.lastDescriptor();
        if (descriptor == null || !descriptor.equals("Ljava/util/Map")) {
            if (descriptor == null) {
                cf.loadTarget(mv);
            }
            CodeFlow.insertCheckCast(mv, "Ljava/util/Map");
        }
        mv.visitLdcInsn(propertyName);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
    }

    @SuppressWarnings("serial")
    private static class MapAccessException extends AccessException {

        private final String key;

        public MapAccessException(String key) {
            super("");
            this.key = key;
        }

        @Override
        public String getMessage() {
            return "Map does not contain a value for key '" + this.key + "'";
        }

    }

}
