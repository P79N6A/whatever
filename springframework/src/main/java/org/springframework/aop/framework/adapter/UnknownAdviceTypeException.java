package org.springframework.aop.framework.adapter;

@SuppressWarnings("serial")
public class UnknownAdviceTypeException extends IllegalArgumentException {

    public UnknownAdviceTypeException(Object advice) {
        super("Advice object [" + advice + "] is neither a supported subinterface of " + "[org.aopalliance.aop.Advice] nor an [org.springframework.aop.Advisor]");
    }

    public UnknownAdviceTypeException(String message) {
        super(message);
    }

}
