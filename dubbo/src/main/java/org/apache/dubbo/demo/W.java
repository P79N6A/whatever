package org.apache.dubbo.demo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class W {

    public static void main(String[] args) {
        ExtensionLoader extensionLoader = ExtensionLoader.getExtensionLoader(Robot.class);
        URL url = new URL("", "", 0);
        url = url.addParameter("abc", "mmp");
        List list = extensionLoader.getActivateExtension(url, "", "y");

    }

}

// @SPI标识接口是一个扩展点，属性value指定默认适配扩展点的名称
@SPI("xxx")
interface Robot {

    /**
     * @Adaptive()如果没有定义值，默认驼峰转换为小写，并以.号区分 如上文的接口名为Robot，key值就是robot
     * 如果接口名为HelloWorld，key值就为hello.world
     * @Adaptive注解在实现类上，这个类就是缺省的适配扩展
     * @Adaptive注解在扩展点Interface方法上时，Dubbo动态生成一个这个扩展点的适配扩展类，名称为扩展点Interface的简单类名 + $Adaptive，如：ProxyFactory$Adaptive
     * 目的是为了在运行时去适配不同的扩展实例，在运行时通过传入的URL类型的参数或者内部含有获取URL方法的参数，从URL中获取到要使用的扩展类的名称，再去根据名称加载对应的扩展实例
     * 如果运行时没有适配到运行的扩展实例，就使用@SPI注解缺省指定的扩展
     * 例如Dubbo框架中的接口RegistryFactory，该接口的自适应类将会从URL以protocol为key来找实现类的extName
     * URL url = URL.valueOf("test://localhost/test?protocol=xxx");
     * URL url = URL.valueOf("test://localhost/test").addParameter("service", "helloService").addParameter("protocol","xxx");
     * 优先级：@Adaptive实现类 > @Adaptive接口方法 + URL > @SPI
     * 注解在类上：代表人工实现，实现一个装饰类，目前只有2个，AdaptiveCompiler、AdaptiveExtensionFactory
     * 注解在方法上：代表自动生成和编译一个动态的Adaptive类，主要用于SPI，动态$Adaptive类
     */
    @Adaptive({"protocol"})
    String sayHello(URL url, String content);

}

/**
 * @Activate注解在扩展点的实现类上，表示了一个扩展类被激活加载的条件，根据group、value属性来过滤 具体参考ExtensionLoader#getActivateExtension
 */
@Activate(group = "y", order = 2)
class OptimusPrime implements Robot {

    private Logger logger = LoggerFactory.getLogger(OptimusPrime.class);

    @Override
    public String sayHello(URL url, String content) {
        logger.debug("url {}", url);
        return content;
    }

}

@Activate(group = {"x", "y"}, order = 1, value = {"abc"})
class Bumblebee implements Robot {

    private Logger logger = LoggerFactory.getLogger(Bumblebee.class);

    @Override
    public String sayHello(URL url, String content) {
        logger.debug("url {}", url);
        return content;
    }

}



