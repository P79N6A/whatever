package org.springframework.aop.aspectj;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AspectJAdviceParameterNameDiscoverer implements ParameterNameDiscoverer {

    private static final String THIS_JOIN_POINT = "thisJoinPoint";

    private static final String THIS_JOIN_POINT_STATIC_PART = "thisJoinPointStaticPart";

    // Steps in the binding algorithm...
    private static final int STEP_JOIN_POINT_BINDING = 1;

    private static final int STEP_THROWING_BINDING = 2;

    private static final int STEP_ANNOTATION_BINDING = 3;

    private static final int STEP_RETURNING_BINDING = 4;

    private static final int STEP_PRIMITIVE_ARGS_BINDING = 5;

    private static final int STEP_THIS_TARGET_ARGS_BINDING = 6;

    private static final int STEP_REFERENCE_PCUT_BINDING = 7;

    private static final int STEP_FINISHED = 8;

    private static final Set<String> singleValuedAnnotationPcds = new HashSet<>();

    private static final Set<String> nonReferencePointcutTokens = new HashSet<>();

    static {
        singleValuedAnnotationPcds.add("@this");
        singleValuedAnnotationPcds.add("@target");
        singleValuedAnnotationPcds.add("@within");
        singleValuedAnnotationPcds.add("@withincode");
        singleValuedAnnotationPcds.add("@annotation");
        Set<PointcutPrimitive> pointcutPrimitives = PointcutParser.getAllSupportedPointcutPrimitives();
        for (PointcutPrimitive primitive : pointcutPrimitives) {
            nonReferencePointcutTokens.add(primitive.getName());
        }
        nonReferencePointcutTokens.add("&&");
        nonReferencePointcutTokens.add("!");
        nonReferencePointcutTokens.add("||");
        nonReferencePointcutTokens.add("and");
        nonReferencePointcutTokens.add("or");
        nonReferencePointcutTokens.add("not");
    }

    @Nullable
    private String pointcutExpression;

    private boolean raiseExceptions;

    @Nullable
    private String returningName;

    @Nullable
    private String throwingName;

    private Class<?>[] argumentTypes = new Class<?>[0];

    private String[] parameterNameBindings = new String[0];

    private int numberOfRemainingUnboundArguments;

    public AspectJAdviceParameterNameDiscoverer(@Nullable String pointcutExpression) {
        this.pointcutExpression = pointcutExpression;
    }

    public void setRaiseExceptions(boolean raiseExceptions) {
        this.raiseExceptions = raiseExceptions;
    }

    public void setReturningName(@Nullable String returningName) {
        this.returningName = returningName;
    }

    public void setThrowingName(@Nullable String throwingName) {
        this.throwingName = throwingName;
    }

    @Override
    @Nullable
    public String[] getParameterNames(Method method) {
        this.argumentTypes = method.getParameterTypes();
        this.numberOfRemainingUnboundArguments = this.argumentTypes.length;
        this.parameterNameBindings = new String[this.numberOfRemainingUnboundArguments];
        int minimumNumberUnboundArgs = 0;
        if (this.returningName != null) {
            minimumNumberUnboundArgs++;
        }
        if (this.throwingName != null) {
            minimumNumberUnboundArgs++;
        }
        if (this.numberOfRemainingUnboundArguments < minimumNumberUnboundArgs) {
            throw new IllegalStateException("Not enough arguments in method to satisfy binding of returning and throwing variables");
        }
        try {
            int algorithmicStep = STEP_JOIN_POINT_BINDING;
            while ((this.numberOfRemainingUnboundArguments > 0) && algorithmicStep < STEP_FINISHED) {
                switch (algorithmicStep++) {
                    case STEP_JOIN_POINT_BINDING:
                        if (!maybeBindThisJoinPoint()) {
                            maybeBindThisJoinPointStaticPart();
                        }
                        break;
                    case STEP_THROWING_BINDING:
                        maybeBindThrowingVariable();
                        break;
                    case STEP_ANNOTATION_BINDING:
                        maybeBindAnnotationsFromPointcutExpression();
                        break;
                    case STEP_RETURNING_BINDING:
                        maybeBindReturningVariable();
                        break;
                    case STEP_PRIMITIVE_ARGS_BINDING:
                        maybeBindPrimitiveArgsFromPointcutExpression();
                        break;
                    case STEP_THIS_TARGET_ARGS_BINDING:
                        maybeBindThisOrTargetOrArgsFromPointcutExpression();
                        break;
                    case STEP_REFERENCE_PCUT_BINDING:
                        maybeBindReferencePointcutParameter();
                        break;
                    default:
                        throw new IllegalStateException("Unknown algorithmic step: " + (algorithmicStep - 1));
                }
            }
        } catch (AmbiguousBindingException | IllegalArgumentException ex) {
            if (this.raiseExceptions) {
                throw ex;
            } else {
                return null;
            }
        }
        if (this.numberOfRemainingUnboundArguments == 0) {
            return this.parameterNameBindings;
        } else {
            if (this.raiseExceptions) {
                throw new IllegalStateException("Failed to bind all argument names: " + this.numberOfRemainingUnboundArguments + " argument(s) could not be bound");
            } else {
                // convention for failing is to return null, allowing participation in a chain of responsibility
                return null;
            }
        }
    }

    @Override
    @Nullable
    public String[] getParameterNames(Constructor<?> ctor) {
        if (this.raiseExceptions) {
            throw new UnsupportedOperationException("An advice method can never be a constructor");
        } else {
            // we return null rather than throw an exception so that we behave well
            // in a chain-of-responsibility.
            return null;
        }
    }

    private void bindParameterName(int index, String name) {
        this.parameterNameBindings[index] = name;
        this.numberOfRemainingUnboundArguments--;
    }

    private boolean maybeBindThisJoinPoint() {
        if ((this.argumentTypes[0] == JoinPoint.class) || (this.argumentTypes[0] == ProceedingJoinPoint.class)) {
            bindParameterName(0, THIS_JOIN_POINT);
            return true;
        } else {
            return false;
        }
    }

    private void maybeBindThisJoinPointStaticPart() {
        if (this.argumentTypes[0] == JoinPoint.StaticPart.class) {
            bindParameterName(0, THIS_JOIN_POINT_STATIC_PART);
        }
    }

    private void maybeBindThrowingVariable() {
        if (this.throwingName == null) {
            return;
        }
        // So there is binding work to do...
        int throwableIndex = -1;
        for (int i = 0; i < this.argumentTypes.length; i++) {
            if (isUnbound(i) && isSubtypeOf(Throwable.class, i)) {
                if (throwableIndex == -1) {
                    throwableIndex = i;
                } else {
                    // Second candidate we've found - ambiguous binding
                    throw new AmbiguousBindingException("Binding of throwing parameter '" + this.throwingName + "' is ambiguous: could be bound to argument " + throwableIndex + " or argument " + i);
                }
            }
        }
        if (throwableIndex == -1) {
            throw new IllegalStateException("Binding of throwing parameter '" + this.throwingName + "' could not be completed as no available arguments are a subtype of Throwable");
        } else {
            bindParameterName(throwableIndex, this.throwingName);
        }
    }

    private void maybeBindReturningVariable() {
        if (this.numberOfRemainingUnboundArguments == 0) {
            throw new IllegalStateException("Algorithm assumes that there must be at least one unbound parameter on entry to this method");
        }
        if (this.returningName != null) {
            if (this.numberOfRemainingUnboundArguments > 1) {
                throw new AmbiguousBindingException("Binding of returning parameter '" + this.returningName + "' is ambiguous, there are " + this.numberOfRemainingUnboundArguments + " candidates.");
            }
            // We're all set... find the unbound parameter, and bind it.
            for (int i = 0; i < this.parameterNameBindings.length; i++) {
                if (this.parameterNameBindings[i] == null) {
                    bindParameterName(i, this.returningName);
                    break;
                }
            }
        }
    }

    private void maybeBindAnnotationsFromPointcutExpression() {
        List<String> varNames = new ArrayList<>();
        String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
        for (int i = 0; i < tokens.length; i++) {
            String toMatch = tokens[i];
            int firstParenIndex = toMatch.indexOf('(');
            if (firstParenIndex != -1) {
                toMatch = toMatch.substring(0, firstParenIndex);
            }
            if (singleValuedAnnotationPcds.contains(toMatch)) {
                PointcutBody body = getPointcutBody(tokens, i);
                i += body.numTokensConsumed;
                String varName = maybeExtractVariableName(body.text);
                if (varName != null) {
                    varNames.add(varName);
                }
            } else if (tokens[i].startsWith("@args(") || tokens[i].equals("@args")) {
                PointcutBody body = getPointcutBody(tokens, i);
                i += body.numTokensConsumed;
                maybeExtractVariableNamesFromArgs(body.text, varNames);
            }
        }
        bindAnnotationsFromVarNames(varNames);
    }

    private void bindAnnotationsFromVarNames(List<String> varNames) {
        if (!varNames.isEmpty()) {
            // we have work to do...
            int numAnnotationSlots = countNumberOfUnboundAnnotationArguments();
            if (numAnnotationSlots > 1) {
                throw new AmbiguousBindingException("Found " + varNames.size() + " potential annotation variable(s), and " + numAnnotationSlots + " potential argument slots");
            } else if (numAnnotationSlots == 1) {
                if (varNames.size() == 1) {
                    // it's a match
                    findAndBind(Annotation.class, varNames.get(0));
                } else {
                    // multiple candidate vars, but only one slot
                    throw new IllegalArgumentException("Found " + varNames.size() + " candidate annotation binding variables" + " but only one potential argument binding slot");
                }
            } else {
                // no slots so presume those candidate vars were actually type names
            }
        }
    }

    @Nullable
    private String maybeExtractVariableName(@Nullable String candidateToken) {
        if (!StringUtils.hasLength(candidateToken)) {
            return null;
        }
        if (Character.isJavaIdentifierStart(candidateToken.charAt(0)) && Character.isLowerCase(candidateToken.charAt(0))) {
            char[] tokenChars = candidateToken.toCharArray();
            for (char tokenChar : tokenChars) {
                if (!Character.isJavaIdentifierPart(tokenChar)) {
                    return null;
                }
            }
            return candidateToken;
        } else {
            return null;
        }
    }

    private void maybeExtractVariableNamesFromArgs(@Nullable String argsSpec, List<String> varNames) {
        if (argsSpec == null) {
            return;
        }
        String[] tokens = StringUtils.tokenizeToStringArray(argsSpec, ",");
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = StringUtils.trimWhitespace(tokens[i]);
            String varName = maybeExtractVariableName(tokens[i]);
            if (varName != null) {
                varNames.add(varName);
            }
        }
    }

    private void maybeBindThisOrTargetOrArgsFromPointcutExpression() {
        if (this.numberOfRemainingUnboundArguments > 1) {
            throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments + " unbound args at this(),target(),args() binding stage, with no way to determine between them");
        }
        List<String> varNames = new ArrayList<>();
        String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("this") || tokens[i].startsWith("this(") || tokens[i].equals("target") || tokens[i].startsWith("target(")) {
                PointcutBody body = getPointcutBody(tokens, i);
                i += body.numTokensConsumed;
                String varName = maybeExtractVariableName(body.text);
                if (varName != null) {
                    varNames.add(varName);
                }
            } else if (tokens[i].equals("args") || tokens[i].startsWith("args(")) {
                PointcutBody body = getPointcutBody(tokens, i);
                i += body.numTokensConsumed;
                List<String> candidateVarNames = new ArrayList<>();
                maybeExtractVariableNamesFromArgs(body.text, candidateVarNames);
                // we may have found some var names that were bound in previous primitive args binding step,
                // filter them out...
                for (String varName : candidateVarNames) {
                    if (!alreadyBound(varName)) {
                        varNames.add(varName);
                    }
                }
            }
        }
        if (varNames.size() > 1) {
            throw new AmbiguousBindingException("Found " + varNames.size() + " candidate this(), target() or args() variables but only one unbound argument slot");
        } else if (varNames.size() == 1) {
            for (int j = 0; j < this.parameterNameBindings.length; j++) {
                if (isUnbound(j)) {
                    bindParameterName(j, varNames.get(0));
                    break;
                }
            }
        }
        // else varNames.size must be 0 and we have nothing to bind.
    }

    private void maybeBindReferencePointcutParameter() {
        if (this.numberOfRemainingUnboundArguments > 1) {
            throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments + " unbound args at reference pointcut binding stage, with no way to determine between them");
        }
        List<String> varNames = new ArrayList<>();
        String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
        for (int i = 0; i < tokens.length; i++) {
            String toMatch = tokens[i];
            if (toMatch.startsWith("!")) {
                toMatch = toMatch.substring(1);
            }
            int firstParenIndex = toMatch.indexOf('(');
            if (firstParenIndex != -1) {
                toMatch = toMatch.substring(0, firstParenIndex);
            } else {
                if (tokens.length < i + 2) {
                    // no "(" and nothing following
                    continue;
                } else {
                    String nextToken = tokens[i + 1];
                    if (nextToken.charAt(0) != '(') {
                        // next token is not "(" either, can't be a pc...
                        continue;
                    }
                }

            }
            // eat the body
            PointcutBody body = getPointcutBody(tokens, i);
            i += body.numTokensConsumed;
            if (!nonReferencePointcutTokens.contains(toMatch)) {
                // then it could be a reference pointcut
                String varName = maybeExtractVariableName(body.text);
                if (varName != null) {
                    varNames.add(varName);
                }
            }
        }
        if (varNames.size() > 1) {
            throw new AmbiguousBindingException("Found " + varNames.size() + " candidate reference pointcut variables but only one unbound argument slot");
        } else if (varNames.size() == 1) {
            for (int j = 0; j < this.parameterNameBindings.length; j++) {
                if (isUnbound(j)) {
                    bindParameterName(j, varNames.get(0));
                    break;
                }
            }
        }
        // else varNames.size must be 0 and we have nothing to bind.
    }

    private PointcutBody getPointcutBody(String[] tokens, int startIndex) {
        int numTokensConsumed = 0;
        String currentToken = tokens[startIndex];
        int bodyStart = currentToken.indexOf('(');
        if (currentToken.charAt(currentToken.length() - 1) == ')') {
            // It's an all in one... get the text between the first (and the last)
            return new PointcutBody(0, currentToken.substring(bodyStart + 1, currentToken.length() - 1));
        } else {
            StringBuilder sb = new StringBuilder();
            if (bodyStart >= 0 && bodyStart != (currentToken.length() - 1)) {
                sb.append(currentToken.substring(bodyStart + 1));
                sb.append(" ");
            }
            numTokensConsumed++;
            int currentIndex = startIndex + numTokensConsumed;
            while (currentIndex < tokens.length) {
                if (tokens[currentIndex].equals("(")) {
                    currentIndex++;
                    continue;
                }
                if (tokens[currentIndex].endsWith(")")) {
                    sb.append(tokens[currentIndex].substring(0, tokens[currentIndex].length() - 1));
                    return new PointcutBody(numTokensConsumed, sb.toString().trim());
                }
                String toAppend = tokens[currentIndex];
                if (toAppend.startsWith("(")) {
                    toAppend = toAppend.substring(1);
                }
                sb.append(toAppend);
                sb.append(" ");
                currentIndex++;
                numTokensConsumed++;
            }

        }
        // We looked and failed...
        return new PointcutBody(numTokensConsumed, null);
    }

    private void maybeBindPrimitiveArgsFromPointcutExpression() {
        int numUnboundPrimitives = countNumberOfUnboundPrimitiveArguments();
        if (numUnboundPrimitives > 1) {
            throw new AmbiguousBindingException("Found '" + numUnboundPrimitives + "' unbound primitive arguments with no way to distinguish between them.");
        }
        if (numUnboundPrimitives == 1) {
            // Look for arg variable and bind it if we find exactly one...
            List<String> varNames = new ArrayList<>();
            String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equals("args") || tokens[i].startsWith("args(")) {
                    PointcutBody body = getPointcutBody(tokens, i);
                    i += body.numTokensConsumed;
                    maybeExtractVariableNamesFromArgs(body.text, varNames);
                }
            }
            if (varNames.size() > 1) {
                throw new AmbiguousBindingException("Found " + varNames.size() + " candidate variable names but only one candidate binding slot when matching primitive args");
            } else if (varNames.size() == 1) {
                // 1 primitive arg, and one candidate...
                for (int i = 0; i < this.argumentTypes.length; i++) {
                    if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
                        bindParameterName(i, varNames.get(0));
                        break;
                    }
                }
            }
        }
    }

    private boolean isUnbound(int i) {
        return this.parameterNameBindings[i] == null;
    }

    private boolean alreadyBound(String varName) {
        for (int i = 0; i < this.parameterNameBindings.length; i++) {
            if (!isUnbound(i) && varName.equals(this.parameterNameBindings[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean isSubtypeOf(Class<?> supertype, int argumentNumber) {
        return supertype.isAssignableFrom(this.argumentTypes[argumentNumber]);
    }

    private int countNumberOfUnboundAnnotationArguments() {
        int count = 0;
        for (int i = 0; i < this.argumentTypes.length; i++) {
            if (isUnbound(i) && isSubtypeOf(Annotation.class, i)) {
                count++;
            }
        }
        return count;
    }

    private int countNumberOfUnboundPrimitiveArguments() {
        int count = 0;
        for (int i = 0; i < this.argumentTypes.length; i++) {
            if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
                count++;
            }
        }
        return count;
    }

    private void findAndBind(Class<?> argumentType, String varName) {
        for (int i = 0; i < this.argumentTypes.length; i++) {
            if (isUnbound(i) && isSubtypeOf(argumentType, i)) {
                bindParameterName(i, varName);
                return;
            }
        }
        throw new IllegalStateException("Expected to find an unbound argument of type '" + argumentType.getName() + "'");
    }

    private static class PointcutBody {

        private int numTokensConsumed;

        @Nullable
        private String text;

        public PointcutBody(int tokens, @Nullable String text) {
            this.numTokensConsumed = tokens;
            this.text = text;
        }

    }

    @SuppressWarnings("serial")
    public static class AmbiguousBindingException extends RuntimeException {

        public AmbiguousBindingException(String msg) {
            super(msg);
        }

    }

}
