package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class CustomizableTraceInterceptor extends AbstractTraceInterceptor {

    public static final String PLACEHOLDER_METHOD_NAME = "$[methodName]";

    public static final String PLACEHOLDER_TARGET_CLASS_NAME = "$[targetClassName]";

    public static final String PLACEHOLDER_TARGET_CLASS_SHORT_NAME = "$[targetClassShortName]";

    public static final String PLACEHOLDER_RETURN_VALUE = "$[returnValue]";

    public static final String PLACEHOLDER_ARGUMENT_TYPES = "$[argumentTypes]";

    public static final String PLACEHOLDER_ARGUMENTS = "$[arguments]";

    public static final String PLACEHOLDER_EXCEPTION = "$[exception]";

    public static final String PLACEHOLDER_INVOCATION_TIME = "$[invocationTime]";

    private static final String DEFAULT_ENTER_MESSAGE = "Entering method '" + PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

    private static final String DEFAULT_EXIT_MESSAGE = "Exiting method '" + PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

    private static final String DEFAULT_EXCEPTION_MESSAGE = "Exception thrown in method '" + PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

    private static final Pattern PATTERN = Pattern.compile("\\$\\[\\p{Alpha}+\\]");

    private static final Set<Object> ALLOWED_PLACEHOLDERS = new Constants(CustomizableTraceInterceptor.class).getValues("PLACEHOLDER_");

    private String enterMessage = DEFAULT_ENTER_MESSAGE;

    private String exitMessage = DEFAULT_EXIT_MESSAGE;

    private String exceptionMessage = DEFAULT_EXCEPTION_MESSAGE;

    public void setEnterMessage(String enterMessage) throws IllegalArgumentException {
        Assert.hasText(enterMessage, "enterMessage must not be empty");
        checkForInvalidPlaceholders(enterMessage);
        Assert.doesNotContain(enterMessage, PLACEHOLDER_RETURN_VALUE, "enterMessage cannot contain placeholder " + PLACEHOLDER_RETURN_VALUE);
        Assert.doesNotContain(enterMessage, PLACEHOLDER_EXCEPTION, "enterMessage cannot contain placeholder " + PLACEHOLDER_EXCEPTION);
        Assert.doesNotContain(enterMessage, PLACEHOLDER_INVOCATION_TIME, "enterMessage cannot contain placeholder " + PLACEHOLDER_INVOCATION_TIME);
        this.enterMessage = enterMessage;
    }

    public void setExitMessage(String exitMessage) {
        Assert.hasText(exitMessage, "exitMessage must not be empty");
        checkForInvalidPlaceholders(exitMessage);
        Assert.doesNotContain(exitMessage, PLACEHOLDER_EXCEPTION, "exitMessage cannot contain placeholder" + PLACEHOLDER_EXCEPTION);
        this.exitMessage = exitMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        Assert.hasText(exceptionMessage, "exceptionMessage must not be empty");
        checkForInvalidPlaceholders(exceptionMessage);
        Assert.doesNotContain(exceptionMessage, PLACEHOLDER_RETURN_VALUE, "exceptionMessage cannot contain placeholder " + PLACEHOLDER_RETURN_VALUE);
        this.exceptionMessage = exceptionMessage;
    }

    @Override
    protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
        String name = ClassUtils.getQualifiedMethodName(invocation.getMethod());
        StopWatch stopWatch = new StopWatch(name);
        Object returnValue = null;
        boolean exitThroughException = false;
        try {
            stopWatch.start(name);
            writeToLog(logger, replacePlaceholders(this.enterMessage, invocation, null, null, -1));
            returnValue = invocation.proceed();
            return returnValue;
        } catch (Throwable ex) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            exitThroughException = true;
            writeToLog(logger, replacePlaceholders(this.exceptionMessage, invocation, null, ex, stopWatch.getTotalTimeMillis()), ex);
            throw ex;
        } finally {
            if (!exitThroughException) {
                if (stopWatch.isRunning()) {
                    stopWatch.stop();
                }
                writeToLog(logger, replacePlaceholders(this.exitMessage, invocation, returnValue, null, stopWatch.getTotalTimeMillis()));
            }
        }
    }

    protected String replacePlaceholders(String message, MethodInvocation methodInvocation, @Nullable Object returnValue, @Nullable Throwable throwable, long invocationTime) {
        Matcher matcher = PATTERN.matcher(message);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String match = matcher.group();
            if (PLACEHOLDER_METHOD_NAME.equals(match)) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(methodInvocation.getMethod().getName()));
            } else if (PLACEHOLDER_TARGET_CLASS_NAME.equals(match)) {
                String className = getClassForLogging(methodInvocation.getThis()).getName();
                matcher.appendReplacement(output, Matcher.quoteReplacement(className));
            } else if (PLACEHOLDER_TARGET_CLASS_SHORT_NAME.equals(match)) {
                String shortName = ClassUtils.getShortName(getClassForLogging(methodInvocation.getThis()));
                matcher.appendReplacement(output, Matcher.quoteReplacement(shortName));
            } else if (PLACEHOLDER_ARGUMENTS.equals(match)) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(StringUtils.arrayToCommaDelimitedString(methodInvocation.getArguments())));
            } else if (PLACEHOLDER_ARGUMENT_TYPES.equals(match)) {
                appendArgumentTypes(methodInvocation, matcher, output);
            } else if (PLACEHOLDER_RETURN_VALUE.equals(match)) {
                appendReturnValue(methodInvocation, matcher, output, returnValue);
            } else if (throwable != null && PLACEHOLDER_EXCEPTION.equals(match)) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(throwable.toString()));
            } else if (PLACEHOLDER_INVOCATION_TIME.equals(match)) {
                matcher.appendReplacement(output, Long.toString(invocationTime));
            } else {
                throw new IllegalArgumentException("Unknown placeholder [" + match + "]");
            }
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private void appendReturnValue(MethodInvocation methodInvocation, Matcher matcher, StringBuffer output, @Nullable Object returnValue) {
        if (methodInvocation.getMethod().getReturnType() == void.class) {
            matcher.appendReplacement(output, "void");
        } else if (returnValue == null) {
            matcher.appendReplacement(output, "null");
        } else {
            matcher.appendReplacement(output, Matcher.quoteReplacement(returnValue.toString()));
        }
    }

    private void appendArgumentTypes(MethodInvocation methodInvocation, Matcher matcher, StringBuffer output) {
        Class<?>[] argumentTypes = methodInvocation.getMethod().getParameterTypes();
        String[] argumentTypeShortNames = new String[argumentTypes.length];
        for (int i = 0; i < argumentTypeShortNames.length; i++) {
            argumentTypeShortNames[i] = ClassUtils.getShortName(argumentTypes[i]);
        }
        matcher.appendReplacement(output, Matcher.quoteReplacement(StringUtils.arrayToCommaDelimitedString(argumentTypeShortNames)));
    }

    private void checkForInvalidPlaceholders(String message) throws IllegalArgumentException {
        Matcher matcher = PATTERN.matcher(message);
        while (matcher.find()) {
            String match = matcher.group();
            if (!ALLOWED_PLACEHOLDERS.contains(match)) {
                throw new IllegalArgumentException("Placeholder [" + match + "] is not valid");
            }
        }
    }

}
