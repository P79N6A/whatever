package org.springframework.web.bind;

import org.springframework.lang.Nullable;

import javax.servlet.ServletRequest;

public abstract class ServletRequestUtils {

    private static final IntParser INT_PARSER = new IntParser();

    private static final LongParser LONG_PARSER = new LongParser();

    private static final FloatParser FLOAT_PARSER = new FloatParser();

    private static final DoubleParser DOUBLE_PARSER = new DoubleParser();

    private static final BooleanParser BOOLEAN_PARSER = new BooleanParser();

    private static final StringParser STRING_PARSER = new StringParser();

    @Nullable
    public static Integer getIntParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        if (request.getParameter(name) == null) {
            return null;
        }
        return getRequiredIntParameter(request, name);
    }

    public static int getIntParameter(ServletRequest request, String name, int defaultVal) {
        if (request.getParameter(name) == null) {
            return defaultVal;
        }
        try {
            return getRequiredIntParameter(request, name);
        } catch (ServletRequestBindingException ex) {
            return defaultVal;
        }
    }

    public static int[] getIntParameters(ServletRequest request, String name) {
        try {
            return getRequiredIntParameters(request, name);
        } catch (ServletRequestBindingException ex) {
            return new int[0];
        }
    }

    public static int getRequiredIntParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        return INT_PARSER.parseInt(name, request.getParameter(name));
    }

    public static int[] getRequiredIntParameters(ServletRequest request, String name) throws ServletRequestBindingException {
        return INT_PARSER.parseInts(name, request.getParameterValues(name));
    }

    @Nullable
    public static Long getLongParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        if (request.getParameter(name) == null) {
            return null;
        }
        return getRequiredLongParameter(request, name);
    }

    public static long getLongParameter(ServletRequest request, String name, long defaultVal) {
        if (request.getParameter(name) == null) {
            return defaultVal;
        }
        try {
            return getRequiredLongParameter(request, name);
        } catch (ServletRequestBindingException ex) {
            return defaultVal;
        }
    }

    public static long[] getLongParameters(ServletRequest request, String name) {
        try {
            return getRequiredLongParameters(request, name);
        } catch (ServletRequestBindingException ex) {
            return new long[0];
        }
    }

    public static long getRequiredLongParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        return LONG_PARSER.parseLong(name, request.getParameter(name));
    }

    public static long[] getRequiredLongParameters(ServletRequest request, String name) throws ServletRequestBindingException {
        return LONG_PARSER.parseLongs(name, request.getParameterValues(name));
    }

    @Nullable
    public static Float getFloatParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        if (request.getParameter(name) == null) {
            return null;
        }
        return getRequiredFloatParameter(request, name);
    }

    public static float getFloatParameter(ServletRequest request, String name, float defaultVal) {
        if (request.getParameter(name) == null) {
            return defaultVal;
        }
        try {
            return getRequiredFloatParameter(request, name);
        } catch (ServletRequestBindingException ex) {
            return defaultVal;
        }
    }

    public static float[] getFloatParameters(ServletRequest request, String name) {
        try {
            return getRequiredFloatParameters(request, name);
        } catch (ServletRequestBindingException ex) {
            return new float[0];
        }
    }

    public static float getRequiredFloatParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        return FLOAT_PARSER.parseFloat(name, request.getParameter(name));
    }

    public static float[] getRequiredFloatParameters(ServletRequest request, String name) throws ServletRequestBindingException {
        return FLOAT_PARSER.parseFloats(name, request.getParameterValues(name));
    }

    @Nullable
    public static Double getDoubleParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        if (request.getParameter(name) == null) {
            return null;
        }
        return getRequiredDoubleParameter(request, name);
    }

    public static double getDoubleParameter(ServletRequest request, String name, double defaultVal) {
        if (request.getParameter(name) == null) {
            return defaultVal;
        }
        try {
            return getRequiredDoubleParameter(request, name);
        } catch (ServletRequestBindingException ex) {
            return defaultVal;
        }
    }

    public static double[] getDoubleParameters(ServletRequest request, String name) {
        try {
            return getRequiredDoubleParameters(request, name);
        } catch (ServletRequestBindingException ex) {
            return new double[0];
        }
    }

    public static double getRequiredDoubleParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        return DOUBLE_PARSER.parseDouble(name, request.getParameter(name));
    }

    public static double[] getRequiredDoubleParameters(ServletRequest request, String name) throws ServletRequestBindingException {
        return DOUBLE_PARSER.parseDoubles(name, request.getParameterValues(name));
    }

    @Nullable
    public static Boolean getBooleanParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        if (request.getParameter(name) == null) {
            return null;
        }
        return (getRequiredBooleanParameter(request, name));
    }

    public static boolean getBooleanParameter(ServletRequest request, String name, boolean defaultVal) {
        if (request.getParameter(name) == null) {
            return defaultVal;
        }
        try {
            return getRequiredBooleanParameter(request, name);
        } catch (ServletRequestBindingException ex) {
            return defaultVal;
        }
    }

    public static boolean[] getBooleanParameters(ServletRequest request, String name) {
        try {
            return getRequiredBooleanParameters(request, name);
        } catch (ServletRequestBindingException ex) {
            return new boolean[0];
        }
    }

    public static boolean getRequiredBooleanParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        return BOOLEAN_PARSER.parseBoolean(name, request.getParameter(name));
    }

    public static boolean[] getRequiredBooleanParameters(ServletRequest request, String name) throws ServletRequestBindingException {
        return BOOLEAN_PARSER.parseBooleans(name, request.getParameterValues(name));
    }

    @Nullable
    public static String getStringParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        if (request.getParameter(name) == null) {
            return null;
        }
        return getRequiredStringParameter(request, name);
    }

    public static String getStringParameter(ServletRequest request, String name, String defaultVal) {
        String val = request.getParameter(name);
        return (val != null ? val : defaultVal);
    }

    public static String[] getStringParameters(ServletRequest request, String name) {
        try {
            return getRequiredStringParameters(request, name);
        } catch (ServletRequestBindingException ex) {
            return new String[0];
        }
    }

    public static String getRequiredStringParameter(ServletRequest request, String name) throws ServletRequestBindingException {
        return STRING_PARSER.validateRequiredString(name, request.getParameter(name));
    }

    public static String[] getRequiredStringParameters(ServletRequest request, String name) throws ServletRequestBindingException {
        return STRING_PARSER.validateRequiredStrings(name, request.getParameterValues(name));
    }

    private abstract static class ParameterParser<T> {

        protected final T parse(String name, String parameter) throws ServletRequestBindingException {
            validateRequiredParameter(name, parameter);
            try {
                return doParse(parameter);
            } catch (NumberFormatException ex) {
                throw new ServletRequestBindingException("Required " + getType() + " parameter '" + name + "' with value of '" + parameter + "' is not a valid number", ex);
            }
        }

        protected final void validateRequiredParameter(String name, @Nullable Object parameter) throws ServletRequestBindingException {
            if (parameter == null) {
                throw new MissingServletRequestParameterException(name, getType());
            }
        }

        protected abstract String getType();

        protected abstract T doParse(String parameter) throws NumberFormatException;

    }

    private static class IntParser extends ParameterParser<Integer> {

        @Override
        protected String getType() {
            return "int";
        }

        @Override
        protected Integer doParse(String s) throws NumberFormatException {
            return Integer.valueOf(s);
        }

        public int parseInt(String name, String parameter) throws ServletRequestBindingException {
            return parse(name, parameter);
        }

        public int[] parseInts(String name, String[] values) throws ServletRequestBindingException {
            validateRequiredParameter(name, values);
            int[] parameters = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = parseInt(name, values[i]);
            }
            return parameters;
        }

    }

    private static class LongParser extends ParameterParser<Long> {

        @Override
        protected String getType() {
            return "long";
        }

        @Override
        protected Long doParse(String parameter) throws NumberFormatException {
            return Long.valueOf(parameter);
        }

        public long parseLong(String name, String parameter) throws ServletRequestBindingException {
            return parse(name, parameter);
        }

        public long[] parseLongs(String name, String[] values) throws ServletRequestBindingException {
            validateRequiredParameter(name, values);
            long[] parameters = new long[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = parseLong(name, values[i]);
            }
            return parameters;
        }

    }

    private static class FloatParser extends ParameterParser<Float> {

        @Override
        protected String getType() {
            return "float";
        }

        @Override
        protected Float doParse(String parameter) throws NumberFormatException {
            return Float.valueOf(parameter);
        }

        public float parseFloat(String name, String parameter) throws ServletRequestBindingException {
            return parse(name, parameter);
        }

        public float[] parseFloats(String name, String[] values) throws ServletRequestBindingException {
            validateRequiredParameter(name, values);
            float[] parameters = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = parseFloat(name, values[i]);
            }
            return parameters;
        }

    }

    private static class DoubleParser extends ParameterParser<Double> {

        @Override
        protected String getType() {
            return "double";
        }

        @Override
        protected Double doParse(String parameter) throws NumberFormatException {
            return Double.valueOf(parameter);
        }

        public double parseDouble(String name, String parameter) throws ServletRequestBindingException {
            return parse(name, parameter);
        }

        public double[] parseDoubles(String name, String[] values) throws ServletRequestBindingException {
            validateRequiredParameter(name, values);
            double[] parameters = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = parseDouble(name, values[i]);
            }
            return parameters;
        }

    }

    private static class BooleanParser extends ParameterParser<Boolean> {

        @Override
        protected String getType() {
            return "boolean";
        }

        @Override
        protected Boolean doParse(String parameter) throws NumberFormatException {
            return (parameter.equalsIgnoreCase("true") || parameter.equalsIgnoreCase("on") || parameter.equalsIgnoreCase("yes") || parameter.equals("1"));
        }

        public boolean parseBoolean(String name, String parameter) throws ServletRequestBindingException {
            return parse(name, parameter);
        }

        public boolean[] parseBooleans(String name, String[] values) throws ServletRequestBindingException {
            validateRequiredParameter(name, values);
            boolean[] parameters = new boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                parameters[i] = parseBoolean(name, values[i]);
            }
            return parameters;
        }

    }

    private static class StringParser extends ParameterParser<String> {

        @Override
        protected String getType() {
            return "string";
        }

        @Override
        protected String doParse(String parameter) throws NumberFormatException {
            return parameter;
        }

        public String validateRequiredString(String name, String value) throws ServletRequestBindingException {
            validateRequiredParameter(name, value);
            return value;
        }

        public String[] validateRequiredStrings(String name, String[] values) throws ServletRequestBindingException {
            validateRequiredParameter(name, values);
            for (String value : values) {
                validateRequiredParameter(name, value);
            }
            return values;
        }

    }

}
