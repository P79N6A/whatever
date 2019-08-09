package org.springframework.web.bind;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

@SuppressWarnings("serial")
public class MethodArgumentNotValidException extends Exception {

    private final MethodParameter parameter;

    private final BindingResult bindingResult;

    public MethodArgumentNotValidException(MethodParameter parameter, BindingResult bindingResult) {
        this.parameter = parameter;
        this.bindingResult = bindingResult;
    }

    public MethodParameter getParameter() {
        return this.parameter;
    }

    public BindingResult getBindingResult() {
        return this.bindingResult;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Validation failed for argument [").append(this.parameter.getParameterIndex()).append("] in ").append(this.parameter.getExecutable().toGenericString());
        if (this.bindingResult.getErrorCount() > 1) {
            sb.append(" with ").append(this.bindingResult.getErrorCount()).append(" errors");
        }
        sb.append(": ");
        for (ObjectError error : this.bindingResult.getAllErrors()) {
            sb.append("[").append(error).append("] ");
        }
        return sb.toString();
    }

}
