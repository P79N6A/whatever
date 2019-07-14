package org.apache.dubbo.rpc.cluster.router.condition;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.ConfigConstants.HOST_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.*;

public class ConditionRouter extends AbstractRouter {
    public static final String NAME = "condition";

    private static final Logger logger = LoggerFactory.getLogger(ConditionRouter.class);

    protected static final Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");

    protected Map<String, MatchPair> whenCondition;

    protected Map<String, MatchPair> thenCondition;

    private boolean enabled;

    public ConditionRouter(String rule, boolean force, boolean enabled) {
        this.force = force;
        this.enabled = enabled;
        this.init(rule);
    }

    public ConditionRouter(URL url) {
        this.url = url;
        this.priority = url.getParameter(PRIORITY_KEY, 0);
        this.force = url.getParameter(FORCE_KEY, false);
        this.enabled = url.getParameter(ENABLED_KEY, true);
        init(url.getParameterAndDecoded(RULE_KEY));
    }

    public void init(String rule) {
        try {
            if (rule == null || rule.trim().length() == 0) {
                throw new IllegalArgumentException("Illegal route rule!");
            }
            rule = rule.replace("consumer.", "").replace("provider.", "");
            // 定位 => 分隔符
            int i = rule.indexOf("=>");
            // 分别获取服务消费者和提供者匹配规则
            String whenRule = i < 0 ? null : rule.substring(0, i).trim();
            String thenRule = i < 0 ? rule.trim() : rule.substring(i + 2).trim();
            // 解析服务消费者匹配规则
            Map<String, MatchPair> when = StringUtils.isBlank(whenRule) || "true".equals(whenRule) ? new HashMap<String, MatchPair>() : parseRule(whenRule);
            // 解析服务提供者匹配规则
            Map<String, MatchPair> then = StringUtils.isBlank(thenRule) || "false".equals(thenRule) ? null : parseRule(thenRule);
            // 将解析出的匹配规则分别赋值给 whenCondition 和 thenCondition 成员变量
            this.whenCondition = when;
            this.thenCondition = then;
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static Map<String, MatchPair> parseRule(String rule) throws ParseException {
        // 定义条件映射集合
        Map<String, MatchPair> condition = new HashMap<String, MatchPair>();
        if (StringUtils.isBlank(rule)) {
            return condition;
        }
        MatchPair pair = null;
        Set<String> values = null;
        // 通过正则表达式匹配路由规则，ROUTE_PATTERN = ([&!=,]*)\s*([^&!=,\s]+)
        // 这个表达式看起来不是很好理解，第一个括号内的表达式用于匹配"&", "!", "=" 和 "," 等符号。
        // 第二括号内的用于匹配英文字母，数字等字符。
        // 举个例子说明一下：
        //    host = 2.2.2.2 & host != 1.1.1.1 & method = hello
        // 匹配结果如下：
        //     括号一      括号二
        // 1.  null       host
        // 2.   =         2.2.2.2
        // 3.   &         host
        // 4.   !=        1.1.1.1
        // 5.   &         method
        // 6.   =         hello


        /*
         * 现在线程进入 while 循环：
         *
         * 第一次循环：分隔符 separator = null，content = "host"。此时创建 MatchPair 对象，并存入到 condition 中，condition = {"host": MatchPair@123}
         *
         * 第二次循环：分隔符 separator = "="，content = "2.2.2.2"，pair = MatchPair@123。此时将 2.2.2.2 放入到 MatchPair@123 对象的 matches 集合中。
         *
         * 第三次循环：分隔符 separator = "&"，content = "host"。host 已存在于 condition 中，因此 pair = MatchPair@123。
         *
         * 第四次循环：分隔符 separator = "!="，content = "1.1.1.1"，pair = MatchPair@123。此时将 1.1.1.1 放入到 MatchPair@123 对象的 mismatches 集合中。
         *
         * 第五次循环：分隔符 separator = "&"，content = "method"。condition.get("method") = null，因此新建一个 MatchPair 对象，并放入到 condition 中。此时 condition = {"host": MatchPair@123, "method": MatchPair@ 456}
         *
         * 第六次循环：分隔符 separator = "="，content = "2.2.2.2"，pair = MatchPair@456。此时将 hello 放入到 MatchPair@456 对象的 matches 集合中。
         *
         * 循环结束，此时 condition 的内容如下：
         *
         * {
         *     "host": {
         *         "matches": ["2.2.2.2"],
         *         "mismatches": ["1.1.1.1"]
         *     },
         *     "method": {
         *         "matches": ["hello"],
         *         "mismatches": []
         *     }
         * }
         */
        final Matcher matcher = ROUTE_PATTERN.matcher(rule);
        while (matcher.find()) {
            // 获取括号一内的匹配结果
            String separator = matcher.group(1);
            // 获取括号二内的匹配结果
            String content = matcher.group(2);
            // 分隔符为空，表示匹配的是表达式的开始部分
            if (StringUtils.isEmpty(separator)) {
                // 创建MatchPair对象
                pair = new MatchPair();
                // 存储 <匹配项, MatchPair> ，比如 <host, MatchPair>
                condition.put(content, pair);
            }
            // 如果分隔符为&，表明接下来也是一个条件
            else if ("&".equals(separator)) {
                // 尝试从condition获取MatchPair
                if (condition.get(content) == null) {
                    // 未获取到MatchPair，重新创建一个，并放入condition中
                    pair = new MatchPair();
                    condition.put(content, pair);
                } else {
                    pair = condition.get(content);
                }
            }
            // 分隔符为 =
            else if ("=".equals(separator)) {
                if (pair == null) {
                    throw new ParseException("Illegal route rule \"" + rule + "\", The error char '" + separator + "' at index " + matcher.start() + " before \"" + content + "\".", matcher.start());
                }
                values = pair.matches;
                // 将content存入到MatchPair的matches集合中
                values.add(content);
            }
            //  分隔符为!=
            else if ("!=".equals(separator)) {
                if (pair == null) {
                    throw new ParseException("Illegal route rule \"" + rule + "\", The error char '" + separator + "' at index " + matcher.start() + " before \"" + content + "\".", matcher.start());
                }
                values = pair.mismatches;
                // 将content存入到MatchPair的mismatches集合中
                values.add(content);
            }
            // 分隔符为,
            else if (",".equals(separator)) {
                if (values == null || values.isEmpty()) {
                    throw new ParseException("Illegal route rule \"" + rule + "\", The error char '" + separator + "' at index " + matcher.start() + " before \"" + content + "\".", matcher.start());
                }
                // 将content存入到上一步获取到的values中，可能是matches，也可能是mismatches
                values.add(content);
            } else {
                throw new ParseException("Illegal route rule \"" + rule + "\", The error char '" + separator + "' at index " + matcher.start() + " before \"" + content + "\".", matcher.start());
            }
        }
        return condition;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (!enabled) {
            return invokers;
        }
        // 先对服务消费者条件进行匹配，如果匹配失败，表明服务消费者url不符合匹配规则，无需进行后续匹配，直接返回Invoker列表即可
        // 比如下面的规则：host = 10.20.153.10 => host = 10.0.0.10
        // 这条路由规则希望IP为10.20.153.10的服务消费者调用IP为10.0.0.10机器上的服务
        // 当消费者ip为10.20.153.11时，matchWhen返回false，表明当前这条路由规则不适用于当前的服务消费者，此时无需再进行后续匹配，直接返回即可
        if (CollectionUtils.isEmpty(invokers)) {
            return invokers;
        }
        try {
            if (!matchWhen(url, invocation)) {
                return invokers;
            }
            List<Invoker<T>> result = new ArrayList<Invoker<T>>();
            // 服务提供者匹配条件未配置，表明对指定的服务消费者禁用服务，也就是服务消费者在黑名单中
            if (thenCondition == null) {
                logger.warn("The current consumer in the service blacklist. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey());
                return result;
            }
            // 这里可以简单的把Invoker理解为服务提供者，现在使用服务提供者匹配规则对Invoker列表进行匹配
            for (Invoker<T> invoker : invokers) {
                // 若匹配成功，表明当前Invoker符合服务提供者匹配规则，此时将Invoker添加到result列表中
                if (matchThen(invoker.getUrl(), url)) {
                    result.add(invoker);
                }
            }
            // 返回匹配结果，如果result为空列表，且force = true，表示强制返回空列表，否则路由结果为空的路由规则将自动失效
            if (!result.isEmpty()) {
                return result;
            } else if (force) {
                logger.warn("The route result is empty and force execute. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey() + ", router: " + url.getParameterAndDecoded(RULE_KEY));
                return result;
            }
        } catch (Throwable t) {
            logger.error("Failed to execute condition router rule: " + getUrl() + ", invokers: " + invokers + ", cause: " + t.getMessage(), t);
        }
        // 原样返回，force = false，表示该条路由规则失效
        return invokers;
    }

    @Override
    public boolean isRuntime() {
        return this.url.getParameter(RUNTIME_KEY, false);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    boolean matchWhen(URL url, Invocation invocation) {
        // 服务消费者条件为null或空，均返回true，比如：=> host != 172.22.3.91
        // 表示所有的服务消费者都不得调用IP为172.22.3.91的机器上的服务
        return CollectionUtils.isEmptyMap(whenCondition) || matchCondition(whenCondition, url, null, invocation);
    }

    private boolean matchThen(URL url, URL param) {
        // 服务提供者条件为null或空，表示禁用服务
        return CollectionUtils.isNotEmptyMap(thenCondition) && matchCondition(thenCondition, url, param, null);
    }

    private boolean matchCondition(Map<String, MatchPair> condition, URL url, URL param, Invocation invocation) {
        // 将服务提供者或消费者url转成Map
        Map<String, String> sample = url.toMap();
        boolean result = false;
        // 遍历condition列表
        for (Map.Entry<String, MatchPair> matchPair : condition.entrySet()) {
            // 获取匹配项名称，比如host、method
            String key = matchPair.getKey();
            String sampleValue;
            // 如果invocation不为空，且key为mehtod(s)，表示进行方法匹配
            if (invocation != null && (METHOD_KEY.equals(key) || METHODS_KEY.equals(key))) {
                // 从invocation获取被调用方法的名称
                sampleValue = invocation.getMethodName();
            } else if (ADDRESS_KEY.equals(key)) {
                // 从服务提供者或消费者 url中获取指定字段值，比如host、application
                sampleValue = url.getAddress();
            } else if (HOST_KEY.equals(key)) {
                sampleValue = url.getHost();
            } else {
                sampleValue = sample.get(key);
                if (sampleValue == null) {
                    // 尝试通过default.xxx获取相应的值
                    sampleValue = sample.get(DEFAULT_KEY_PREFIX + key);
                }
            }
            // --------------------✨ 分割线 ✨-------------------- //
            if (sampleValue != null) {
                // 调用MatchPair的isMatch方法进行匹配
                if (!matchPair.getValue().isMatch(sampleValue, param)) {
                    // 只要有一个规则匹配失败，立即返回false结束方法逻辑
                    return false;
                } else {
                    result = true;
                }
            } else {
                // sampleValue为空，表明服务提供者或消费者url中不包含相关字段
                // 此时如果MatchPair的matches不为空，表示匹配失败，返回false
                // 比如有这样一条匹配条件loadbalance = random
                // 假设url中并不包含loadbalance参数，此时sampleValue = null
                // 既然路由规则里限制了loadbalance必须为random，但sampleValue = null，明显不符合规则，因此返回false
                if (!matchPair.getValue().matches.isEmpty()) {
                    return false;
                } else {
                    result = true;
                }
            }
        }
        return result;
    }

    protected static final class MatchPair {
        final Set<String> matches = new HashSet<String>();

        final Set<String> mismatches = new HashSet<String>();

        /**
         * 条件	过程
         * 情况一	matches 非空，mismatches 为空	遍历 matches 集合元素，并与入参进行匹配。只要有一个元素成功匹配入参，即可返回 true。若全部失配，则返回 false。
         * 情况二	matches 为空，mismatches 非空	遍历 mismatches 集合元素，并与入参进行匹配。只要有一个元素成功匹配入参，立即 false。若全部失配，则返回 true。
         * 情况三	matches 非空，mismatches 非空	优先使用 mismatches 集合元素对入参进行匹配，只要任一元素与入参匹配成功，就立即返回 false，结束方法逻辑。否则再使用 matches 中的集合元素进行匹配，只要有任意一个元素匹配成功，即可返回 true。若全部失配，则返回 false
         * 情况四	matches 为空，mismatches 为空	直接返回 false
         */
        private boolean isMatch(String value, URL param) {
            // 情况一：matches 非空，mismatches 为空
            if (!matches.isEmpty() && mismatches.isEmpty()) {
                // 遍历matches集合，检测入参value是否能被matches集合元素匹配到
                // 举个例子：value = 10.20.153.11，matches = [10.20.153.*],此时isMatchGlobPattern方法返回true
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)) {
                        return true;
                    }
                }
                // 如果所有匹配项都无法匹配到入参，则返回false
                return false;
            }
            // 情况二：matches为空，mismatches非空
            if (!mismatches.isEmpty() && matches.isEmpty()) {
                for (String mismatch : mismatches) {
                    // 只要入参被mismatches集合中的任意一个元素匹配到，就返回false
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                        return false;
                    }
                }
                // mismatches集合中所有元素都无法匹配到入参，此时返回true
                return true;
            }
            // 情况三：matches非空，mismatches非空
            if (!matches.isEmpty() && !mismatches.isEmpty()) {
                // matches和mismatches均为非空，此时优先使用mismatches集合元素对入参进行匹配
                // 只要mismatches集合中任意一个元素与入参匹配成功，就立即返回false，结束方法逻辑
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                        return false;
                    }
                }
                // mismatches集合元素无法匹配到入参，此时再使用matches继续匹配
                for (String match : matches) {
                    // 只要matches集合中任意一个元素与入参匹配成功，就立即返回true
                    if (UrlUtils.isMatchGlobPattern(match, value, param)) {
                        return true;
                    }
                }
                // 全部失配，则返回false
                return false;
            }
            // 情况四：matches和mismatches均为空，此时返回false
            return false;
        }

    }

}
