// //
// // Source code recreated from a .class file by IntelliJ IDEA
// // (powered by Fernflower decompiler)
// //
//
package org.apache.dubbo.common.bytecode;
//

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.dubbo.common.bytecode.ClassGenerator.DC;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.rpc.service.EchoService;
//
// public class Proxy0 extends Proxy implements DC {
//     public Object newInstance(InvocationHandler var1) {
//         return new proxy0(var1);
//     }
//
//     public Proxy0() {
//     }
//
// }
//
// /**
//  * 因为Windows文件名大小写不敏感，就放这里了
//  */
// public class proxy0 implements DC, EchoService, DemoService {
//     public static Method[] methods;
//
//     private InvocationHandler handler;
//
//     public String sayHello(String var1) {
//         Object[] var2 = new Object[]{var1};
//         Object var3 = this.handler.invoke(this, methods[0], var2);
//         return (String) var3;
//     }
//
//     public Object $echo(Object var1) {
//         Object[] var2 = new Object[]{var1};
//         Object var3 = this.handler.invoke(this, methods[1], var2);
//         return (Object) var3;
//     }
//
//     public proxy0() {
//     }
//
//     public proxy0(InvocationHandler var1) {
//         this.handler = var1;
//     }
//
// }
