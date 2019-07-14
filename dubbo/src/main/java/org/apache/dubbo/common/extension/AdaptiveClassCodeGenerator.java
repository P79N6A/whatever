package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AdaptiveClassCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveClassCodeGenerator.class);

    private static final String CLASSNAME_INVOCATION = "org.apache.dubbo.rpc.Invocation";

    private static final String CODE_PACKAGE = "package %s;\n";

    private static final String CODE_IMPORTS = "import %s;\n";

    private static final String CODE_CLASS_DECLARATION = "public class %s$Adaptive implements %s {\n";

    private static final String CODE_METHOD_DECLARATION = "public %s %s(%s) %s {\n%s}\n";

    private static final String CODE_METHOD_ARGUMENT = "%s arg%d";

    private static final String CODE_METHOD_THROWS = "throws %s";

    private static final String CODE_UNSUPPORTED = "throw new UnsupportedOperationException(\"The method %s of interface %s is not adaptive method!\");\n";

    private static final String CODE_URL_NULL_CHECK = "if (arg%d == null) throw new IllegalArgumentException(\"url == null\");\n%s url = arg%d;\n";

    private static final String CODE_EXT_NAME_ASSIGNMENT = "String extName = %s;\n";

    private static final String CODE_EXT_NAME_NULL_CHECK = "if(extName == null) " + "throw new IllegalStateException(\"Failed to get extension (%s) name from url (\" + url.toString() + \") use keys(%s)\");\n";

    private static final String CODE_INVOCATION_ARGUMENT_NULL_CHECK = "if (arg%d == null) throw new IllegalArgumentException(\"invocation == null\"); " + "String methodName = arg%d.getMethodName();\n";

    private static final String CODE_EXTENSION_ASSIGNMENT = "%s extension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);\n";

    private final Class<?> type;

    private String defaultExtName;

    public AdaptiveClassCodeGenerator(Class<?> type, String defaultExtName) {
        this.type = type;
        this.defaultExtName = defaultExtName;
    }

    private boolean hasAdaptiveMethod() {
        return Arrays.stream(type.getMethods()).anyMatch(m -> m.isAnnotationPresent(Adaptive.class));
    }

    public String generate() {
        // 方法上必须有@Adaptive注解
        if (!hasAdaptiveMethod()) {
            throw new IllegalStateException("No adaptive method exist on extension " + type.getName() + ", refuse to create the adaptive class!");
        }
        StringBuilder code = new StringBuilder();
        // 生成package代码：package + type 所在包
        code.append(generatePackageInfo());
        // 生成import代码：import + ExtensionLoader 全限定名
        code.append(generateImports());
        // 生成类代码：public class + type简单名称 + $Adaptive + implements + type全限定名 + {
        code.append(generateClassDeclaration());
        // 通过反射获取所有的方法
        Method[] methods = type.getMethods();
        // 遍历方法列表
        for (Method method : methods) {
            // ${生成方法}
            code.append(generateMethod(method));
        }
        code.append("}");
        if (logger.isDebugEnabled()) {
            logger.debug(code.toString());
        }
        return code.toString();
    }

    private String generatePackageInfo() {
        return String.format(CODE_PACKAGE, type.getPackage().getName());
    }

    private String generateImports() {
        return String.format(CODE_IMPORTS, ExtensionLoader.class.getName());
    }

    private String generateClassDeclaration() {
        return String.format(CODE_CLASS_DECLARATION, type.getSimpleName(), type.getCanonicalName());
    }

    private String generateUnsupported(Method method) {
        return String.format(CODE_UNSUPPORTED, method, type.getName());
    }

    private int getUrlTypeIndex(Method method) {
        int urlTypeIndex = -1;
        Class<?>[] pts = method.getParameterTypes();
        // 遍历参数列表，确定 URL 参数位置
        for (int i = 0; i < pts.length; ++i) {
            if (pts[i].equals(URL.class)) {
                urlTypeIndex = i;
                break;
            }
        }
        return urlTypeIndex;
    }

    private String generateMethod(Method method) {
        // 方法返回值
        String methodReturnType = method.getReturnType().getCanonicalName();
        // 方法名
        String methodName = method.getName();
        // 方法体
        String methodContent = generateMethodContent(method);
        // 参数列表代码
        String methodArgs = generateMethodArguments(method);
        // 异常抛出代码
        String methodThrows = generateMethodThrows(method);

        return String.format(CODE_METHOD_DECLARATION, methodReturnType, methodName, methodArgs, methodThrows, methodContent);
        /*
         * 以Protocol的refer方法为例，上面代码生成的内容如下：
         *
         * public com.alibaba.dubbo.rpc.Invoker refer(java.lang.Class arg0, com.alibaba.dubbo.common.URL arg1) {
         *     // 方法体
         * }
         */
    }

    private String generateMethodArguments(Method method) {
        Class<?>[] pts = method.getParameterTypes();

        return IntStream.range(0, pts.length).mapToObj(i -> String.format(CODE_METHOD_ARGUMENT, pts[i].getCanonicalName(), i)).collect(Collectors.joining(", "));
    }

    private String generateMethodThrows(Method method) {

        Class<?>[] ets = method.getExceptionTypes();
        if (ets.length > 0) {
            // getCanonicalName格式输出
            String list = Arrays.stream(ets).map(Class::getCanonicalName).collect(Collectors.joining(", "));
            return String.format(CODE_METHOD_THROWS, list);
        } else {
            return "";
        }
    }

    private String generateUrlNullCheck(int index) {
        return String.format(CODE_URL_NULL_CHECK, index, URL.class.getName(), index);
    }

    /**
     * 获取URL数据，并为之生成判空和赋值代码
     * 以Protocol的refer和export方法为例，上面的代码为它们生成如下内容（代码已格式化）：
     * refer:
     * if (arg1 == null) throw new IllegalArgumentException("url == null");
     * com.alibaba.dubbo.common.URL url = arg1;
     * export:
     * if (arg0 == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");
     * if (arg0.getUrl() == null) throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");
     * com.alibaba.dubbo.common.URL url = arg0.getUrl();
     */
    private String generateMethodContent(Method method) {
        Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);
        StringBuilder code = new StringBuilder(512);
        // 如果方法上无@Adaptive注解，则生成 throw new UnsupportedOperationException(...)
        if (adaptiveAnnotation == null) {
            // ${无@Adaptive注解方法代码生成逻辑}
            return generateUnsupported(method);
        } else {
            int urlTypeIndex = getUrlTypeIndex(method);
            // urlTypeIndex != -1，表示参数列表中存在URL参数
            if (urlTypeIndex != -1) {
                // 为URL类型参数生成判空代码，格式如下：
                // if (arg + urlTypeIndex == null) throw new IllegalArgumentException("url == null");
                // 为URL类型参数生成赋值代码，形如 URL url = arg1
                code.append(generateUrlNullCheck(urlTypeIndex));
            }
            // 参数列表中不存在URL类型参数
            else {
                code.append(generateUrlAssignmentIndirectly(method));
            }
            // 获取@Adaptive注解值
            String[] value = getMethodAdaptiveValue(adaptiveAnnotation);
            // 检测方法列表中是否存在Invocation类型的参数，若存在，则为其生成判空代码和其他一些代码
            boolean hasInvocation = hasInvocationArgument(method);
            // 为Invocation类型参数生成判空代码
            code.append(generateInvocationArgumentNullCheck(method));
            // 根据@SPI和@Adaptive注解值生成“获取拓展名逻辑”
            code.append(generateExtNameAssignment(value, hasInvocation));
            // 生成extName判空代码
            code.append(generateExtNameNullCheck(value));

            // 根据拓展名加载拓展实例，并调用拓展实例的目标方法
            code.append(generateExtensionAssignment());
            // 生成目标方法调用逻辑
            code.append(generateReturnAndInvocation(method));

            /*
             * 以Protocol接口举例说明，上面代码生成的内容如下：
             * com.alibaba.dubbo.rpc.Protocol extension = (com.alibaba.dubbo.rpc.Protocol) ExtensionLoader
             *     .getExtensionLoader(com.alibaba.dubbo.rpc.Protocol.class).getExtension(extName);
             * return extension.refer(arg0, arg1);
             */

        }
        return code.toString();
    }

    private String generateExtNameNullCheck(String[] value) {
        return String.format(CODE_EXT_NAME_NULL_CHECK, type.getName(), Arrays.toString(value));
    }

    /**
     * 可以会生成但不限于下面的代码：
     * String extName = (url.getProtocol() == null ? "dubbo" : url.getProtocol());
     * 或
     * String extName = url.getMethodParameter(methodName, "loadbalance", "random");
     * 或
     * String extName = url.getParameter("client", url.getParameter("transporter", "netty"));
     */
    private String generateExtNameAssignment(String[] value, boolean hasInvocation) {
        String getNameCode = null;
        // 遍历value，这里的value是@Adaptive的注解值
        // 生成从URL中获取拓展名的代码，生成的代码会赋值给getNameCode变量
        // 遍历顺序由后向前
        for (int i = value.length - 1; i >= 0; --i) {
            // 当i为最后一个元素的坐标时
            if (i == value.length - 1) {
                // 默认拓展名非空
                if (null != defaultExtName) {
                    // protocol是url的一部分，可通过getProtocol方法获取，其他的则是从URL参数中获取
                    // 因为获取方式不同，这里要判断value[i]是否为protocol
                    if (!"protocol".equals(value[i])) {
                        // hasInvocation用于标识方法参数列表中是否有Invocation类型参数
                        if (hasInvocation) {
                            // 生成的代码功能等价于下面的代码：url.getMethodParameter(methodName, value[i], defaultExtName)
                            // 以LoadBalance接口的select方法为例，最终生成的代码如下：url.getMethodParameter(methodName, "loadbalance", "random")
                            getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                        } else {
                            // 生成的代码功能等价于下面的代码：url.getParameter(value[i], defaultExtName)
                            getNameCode = String.format("url.getParameter(\"%s\", \"%s\")", value[i], defaultExtName);
                        }
                    } else {
                        // 生成的代码功能等价于下面的代码：( url.getProtocol() == null ? defaultExtName : url.getProtocol() )
                        getNameCode = String.format("( url.getProtocol() == null ? \"%s\" : url.getProtocol() )", defaultExtName);
                    }
                }
                // 默认拓展名为空
                else {
                    if (!"protocol".equals(value[i])) {
                        if (hasInvocation) {
                            // 生成代码格式同上
                            getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                        } else {
                            // 生成的代码功能等价于下面的代码：url.getParameter(value[i])
                            getNameCode = String.format("url.getParameter(\"%s\")", value[i]);
                        }
                    } else {
                        // 生成从url中获取协议的代码，比如"dubbo"
                        getNameCode = "url.getProtocol()";
                    }
                }
            } else {
                if (!"protocol".equals(value[i])) {
                    if (hasInvocation) {
                        // 生成代码格式同上
                        getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                    } else {
                        // 生成的代码功能等价于下面的代码：url.getParameter(value[i], getNameCode)
                        // 以Transporter接口的connect方法为例，最终生成的代码如下：url.getParameter("client", url.getParameter("transporter", "netty"))
                        getNameCode = String.format("url.getParameter(\"%s\", %s)", value[i], getNameCode);
                    }
                } else {
                    // 生成的代码功能等价于下面的代码：url.getProtocol() == null ? getNameCode : url.getProtocol()
                    // 以Protocol接口的connect方法为例，最终生成的代码如下：url.getProtocol() == null ? "dubbo" : url.getProtocol()
                    getNameCode = String.format("url.getProtocol() == null ? (%s) : url.getProtocol()", getNameCode);
                }
            }
        }
        // 生成extName赋值代码
        return String.format(CODE_EXT_NAME_ASSIGNMENT, getNameCode);
    }

    private String generateExtensionAssignment() {
        // 生成拓展获取代码，格式如下：
        // type全限定名 extension = (type全限定名)ExtensionLoader全限定名.getExtensionLoader(type全限定名.class).getExtension(extName);
        // Tips: 格式化字符串中的 %<s 表示使用前一个转换符所描述的参数，即type全限定名
        return String.format(CODE_EXTENSION_ASSIGNMENT, type.getName(), ExtensionLoader.class.getSimpleName(), type.getName());
    }

    private String generateReturnAndInvocation(Method method) {
        // 如果方法返回值类型非void，则生成return语句
        String returnStatement = method.getReturnType().equals(void.class) ? "" : "return ";
        String args = Arrays.stream(method.getParameters()).map(Parameter::getName).collect(Collectors.joining(", "));
        // 生成目标方法调用逻辑，格式为：extension.方法名(arg0, arg2, ..., argN);
        return returnStatement + String.format("extension.%s(%s);\n", method.getName(), args);
    }

    private boolean hasInvocationArgument(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        // 判断当前参数名称是否等于com.alibaba.dubbo.rpc.Invocation
        return Arrays.stream(pts).anyMatch(p -> CLASSNAME_INVOCATION.equals(p.getName()));
    }

    private String generateInvocationArgumentNullCheck(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        return IntStream.range(0, pts.length).filter(i -> CLASSNAME_INVOCATION.equals(pts[i].getName())).mapToObj(i -> String.format(CODE_INVOCATION_ARGUMENT_NULL_CHECK, i, i)).findFirst().orElse("");
    }

    private String[] getMethodAdaptiveValue(Adaptive adaptiveAnnotation) {
        String[] value = adaptiveAnnotation.value();
        // @Adaptive注解值value类型为String[]，默认情况下为空数组
        // 若value为空数组，则类名转换为字符数组，然后遍历字符数组，并将字符放入StringBuilder
        // 若字符为大写字母，则向StringBuilder中添加点号，随后将字符变为小写存入StringBuilder
        // 比如LoadBalance经过处理后，得到load.balance
        if (value.length == 0) {
            String splitName = StringUtils.camelToSplitName(type.getSimpleName(), ".");
            value = new String[]{splitName};
        }
        return value;
    }

    private String generateUrlAssignmentIndirectly(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        // 遍历方法的参数类型列表
        for (int i = 0; i < pts.length; ++i) {
            // 获取某一类型参数的全部方法
            // 遍历方法列表，寻找可返回URL的getter方法
            for (Method m : pts[i].getMethods()) {
                String name = m.getName();
                // 1. 方法名以get开头，或方法名大于3个字符
                // 2. 方法的访问权限为public
                // 3. 非静态方法
                // 4. 方法参数数量为0
                // 5. 方法返回值类型为URL
                if ((name.startsWith("get") || name.length() > 3) && Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && m.getParameterTypes().length == 0 && m.getReturnType() == URL.class) {
                    return generateGetUrlNullCheck(i, pts[i], name);
                }
            }
        }
        throw new IllegalStateException("Failed to create adaptive class for interface " + type.getName() + ": not found url parameter or url attribute in parameters of method " + method.getName());

    }

    private String generateGetUrlNullCheck(int index, Class<?> type, String method) {
        StringBuilder code = new StringBuilder();
        // 为可返回 URL 的参数生成判空代码，格式如下：
        // if (arg + urlTypeIndex == null) throw new IllegalArgumentException("参数全限定名 + argument == null");
        code.append(String.format("if (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");\n", index, type.getName()));
        // 为 getter 方法返回的 URL 生成判空代码，格式如下：
        // if (argN.getter方法名() == null) throw new IllegalArgumentException(参数全限定名 + argument getUrl() == null);
        code.append(String.format("if (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");\n", index, method, type.getName(), method));
        // 生成赋值语句，格式如下：
        // URL全限定名 url = argN.getter方法名()，比如：com.alibaba.dubbo.common.URL url = invoker.getUrl();
        code.append(String.format("%s url = arg%d.%s();\n", URL.class.getName(), index, method));
        return code.toString();
    }

}
