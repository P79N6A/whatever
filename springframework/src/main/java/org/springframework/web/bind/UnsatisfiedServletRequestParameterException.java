package org.springframework.web.bind;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class UnsatisfiedServletRequestParameterException extends ServletRequestBindingException {

    private final List<String[]> paramConditions;

    private final Map<String, String[]> actualParams;

    public UnsatisfiedServletRequestParameterException(String[] paramConditions, Map<String, String[]> actualParams) {
        super("");
        this.paramConditions = Arrays.<String[]>asList(paramConditions);
        this.actualParams = actualParams;
    }

    public UnsatisfiedServletRequestParameterException(List<String[]> paramConditions, Map<String, String[]> actualParams) {
        super("");
        Assert.notEmpty(paramConditions, "Parameter conditions must not be empty");
        this.paramConditions = paramConditions;
        this.actualParams = actualParams;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Parameter conditions ");
        int i = 0;
        for (String[] conditions : this.paramConditions) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append("\"");
            sb.append(StringUtils.arrayToDelimitedString(conditions, ", "));
            sb.append("\"");
            i++;
        }
        sb.append(" not met for actual request parameters: ");
        sb.append(requestParameterMapToString(this.actualParams));
        return sb.toString();
    }

    public final String[] getParamConditions() {
        return this.paramConditions.get(0);
    }

    public final List<String[]> getParamConditionGroups() {
        return this.paramConditions;
    }

    public final Map<String, String[]> getActualParams() {
        return this.actualParams;
    }

    private static String requestParameterMapToString(Map<String, String[]> actualParams) {
        StringBuilder result = new StringBuilder();
        for (Iterator<Map.Entry<String, String[]>> it = actualParams.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String[]> entry = it.next();
            result.append(entry.getKey()).append('=').append(ObjectUtils.nullSafeToString(entry.getValue()));
            if (it.hasNext()) {
                result.append(", ");
            }
        }
        return result.toString();
    }

}
