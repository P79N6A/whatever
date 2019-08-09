package org;

import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.Transactional;

public class TEST {

    public static void main(String[] args) {
    }

    @Transactional
    public void A() {
        // jdbc...
        try {
            ((TEST) AopContext.currentProxy()).B();
        } catch (Exception e) {
        }
    }

    @Transactional
    public void B() {
        // jdbc...
        throw new RuntimeException();
    }

}
