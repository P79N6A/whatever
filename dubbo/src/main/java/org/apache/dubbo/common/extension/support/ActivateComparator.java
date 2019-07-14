package org.apache.dubbo.common.extension.support;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Comparator;

public class ActivateComparator implements Comparator<Object> {

    public static final Comparator<Object> COMPARATOR = new ActivateComparator();

    @Override
    public int compare(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        if (o1.equals(o2)) {
            return 0;
        }
        Class<?> inf = findSpi(o1.getClass());
        ActivateInfo a1 = parseActivate(o1.getClass());
        ActivateInfo a2 = parseActivate(o2.getClass());
        if ((a1.applicableToCompare() || a2.applicableToCompare()) && inf != null) {
            ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(inf);
            if (a1.applicableToCompare()) {
                String n2 = extensionLoader.getExtensionName(o2.getClass());
                if (a1.isLess(n2)) {
                    return -1;
                }
                if (a1.isMore(n2)) {
                    return 1;
                }
            }
            if (a2.applicableToCompare()) {
                String n1 = extensionLoader.getExtensionName(o1.getClass());
                if (a2.isLess(n1)) {
                    return 1;
                }
                if (a2.isMore(n1)) {
                    return -1;
                }
            }
        }
        int n1 = a1 == null ? 0 : a1.order;
        int n2 = a2 == null ? 0 : a2.order;
        return n1 > n2 ? 1 : -1;
    }

    private Class<?> findSpi(Class clazz) {
        if (clazz.getInterfaces().length <= 0) {
            return null;
        }
        for (Class<?> intf : clazz.getInterfaces()) {
            if (intf.isAnnotationPresent(SPI.class)) {
                return intf;
            } else {
                Class result = findSpi(intf);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private ActivateInfo parseActivate(Class<?> clazz) {
        ActivateInfo info = new ActivateInfo();
        if (clazz.isAnnotationPresent(Activate.class)) {
            Activate activate = clazz.getAnnotation(Activate.class);
            info.before = activate.before();
            info.after = activate.after();
            info.order = activate.order();
        } else {
            // com.alibaba.dubbo.common.extension.Activate activate = clazz.getAnnotation(com.alibaba.dubbo.common.extension.Activate.class);
            // info.before = activate.before();
            // info.after = activate.after();
            // info.order = activate.order();
        }
        return info;
    }

    private static class ActivateInfo {
        private String[] before;

        private String[] after;

        private int order;

        private boolean applicableToCompare() {
            return ArrayUtils.isNotEmpty(before) || ArrayUtils.isNotEmpty(after);
        }

        private boolean isLess(String name) {
            return Arrays.asList(before).contains(name);
        }

        private boolean isMore(String name) {
            return Arrays.asList(after).contains(name);
        }

    }

}
