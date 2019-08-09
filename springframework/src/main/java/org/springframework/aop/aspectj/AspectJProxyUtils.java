package org.springframework.aop.aspectj;

import org.springframework.aop.Advisor;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;

import java.util.List;

public abstract class AspectJProxyUtils {

    public static boolean makeAdvisorChainAspectJCapableIfNecessary(List<Advisor> advisors) {
        // 不为空
        if (!advisors.isEmpty()) {
            boolean foundAspectJAdvice = false;
            // 遍历
            for (Advisor advisor : advisors) {
                // 是AspectJAdvice
                if (isAspectJAdvice(advisor)) {
                    foundAspectJAdvice = true;
                    break;
                }
            }
            // 没包含
            if (foundAspectJAdvice && !advisors.contains(ExposeInvocationInterceptor.ADVISOR)) {
                // 加
                advisors.add(0, ExposeInvocationInterceptor.ADVISOR);
                return true;
            }
        }
        return false;
    }

    private static boolean isAspectJAdvice(Advisor advisor) {

        return (advisor instanceof InstantiationModelAwarePointcutAdvisor || advisor.getAdvice() instanceof AbstractAspectJAdvice || (advisor instanceof PointcutAdvisor && ((PointcutAdvisor) advisor).getPointcut() instanceof AspectJExpressionPointcut));
    }

}
