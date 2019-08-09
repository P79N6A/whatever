package org.springframework.expression.spel.support;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.*;
import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectiveConstructorResolver implements ConstructorResolver {

    @Override
    @Nullable
    public ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes) throws AccessException {
        try {
            TypeConverter typeConverter = context.getTypeConverter();
            Class<?> type = context.getTypeLocator().findType(typeName);
            Constructor<?>[] ctors = type.getConstructors();
            Arrays.sort(ctors, (c1, c2) -> {
                int c1pl = c1.getParameterCount();
                int c2pl = c2.getParameterCount();
                return (c1pl < c2pl ? -1 : (c1pl > c2pl ? 1 : 0));
            });
            Constructor<?> closeMatch = null;
            Constructor<?> matchRequiringConversion = null;
            for (Constructor<?> ctor : ctors) {
                Class<?>[] paramTypes = ctor.getParameterTypes();
                List<TypeDescriptor> paramDescriptors = new ArrayList<>(paramTypes.length);
                for (int i = 0; i < paramTypes.length; i++) {
                    paramDescriptors.add(new TypeDescriptor(new MethodParameter(ctor, i)));
                }
                ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
                if (ctor.isVarArgs() && argumentTypes.size() >= paramTypes.length - 1) {
                    // *sigh* complicated
                    // Basically.. we have to have all parameters match up until the varargs one, then the rest of what is
                    // being provided should be
                    // the same type whilst the final argument to the method must be an array of that (oh, how easy...not) -
                    // or the final parameter
                    // we are supplied does match exactly (it is an array already).
                    matchInfo = ReflectionHelper.compareArgumentsVarargs(paramDescriptors, argumentTypes, typeConverter);
                } else if (paramTypes.length == argumentTypes.size()) {
                    // worth a closer look
                    matchInfo = ReflectionHelper.compareArguments(paramDescriptors, argumentTypes, typeConverter);
                }
                if (matchInfo != null) {
                    if (matchInfo.isExactMatch()) {
                        return new ReflectiveConstructorExecutor(ctor);
                    } else if (matchInfo.isCloseMatch()) {
                        closeMatch = ctor;
                    } else if (matchInfo.isMatchRequiringConversion()) {
                        matchRequiringConversion = ctor;
                    }
                }
            }
            if (closeMatch != null) {
                return new ReflectiveConstructorExecutor(closeMatch);
            } else if (matchRequiringConversion != null) {
                return new ReflectiveConstructorExecutor(matchRequiringConversion);
            } else {
                return null;
            }
        } catch (EvaluationException ex) {
            throw new AccessException("Failed to resolve constructor", ex);
        }
    }

}
