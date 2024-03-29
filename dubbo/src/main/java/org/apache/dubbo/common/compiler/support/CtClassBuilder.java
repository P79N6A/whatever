package org.apache.dubbo.common.compiler.support;

import javassist.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CtClassBuilder {

    private String className;

    private String superClassName = "java.lang.Object";

    private List<String> imports = new ArrayList<>();

    private Map<String, String> fullNames = new HashMap<>();

    private List<String> ifaces = new ArrayList<>();

    private List<String> constructors = new ArrayList<>();

    private List<String> fields = new ArrayList<>();

    private List<String> methods = new ArrayList<>();

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = getQualifiedClassName(superClassName);
    }

    public List<String> getImports() {
        return imports;
    }

    public void addImports(String pkg) {
        int pi = pkg.lastIndexOf('.');
        if (pi > 0) {
            String pkgName = pkg.substring(0, pi);
            this.imports.add(pkgName);
            if (!pkg.endsWith(".*")) {
                fullNames.put(pkg.substring(pi + 1), pkg);
            }
        }
    }

    public List<String> getInterfaces() {
        return ifaces;
    }

    public void addInterface(String iface) {
        this.ifaces.add(getQualifiedClassName(iface));
    }

    public List<String> getConstructors() {
        return constructors;
    }

    public void addConstructor(String constructor) {
        this.constructors.add(constructor);
    }

    public List<String> getFields() {
        return fields;
    }

    public void addField(String field) {
        this.fields.add(field);
    }

    public List<String> getMethods() {
        return methods;
    }

    public void addMethod(String method) {
        this.methods.add(method);
    }

    protected String getQualifiedClassName(String className) {
        if (className.contains(".")) {
            return className;
        }
        if (fullNames.containsKey(className)) {
            return fullNames.get(className);
        }
        return ClassUtils.forName(imports.toArray(new String[0]), className).getName();
    }

    public CtClass build(ClassLoader classLoader) throws NotFoundException, CannotCompileException {
        ClassPool pool = new ClassPool(true);
        pool.appendClassPath(new LoaderClassPath(classLoader));
        CtClass ctClass = pool.makeClass(className, pool.get(superClassName));
        imports.stream().forEach(pool::importPackage);
        for (String iface : ifaces) {
            ctClass.addInterface(pool.get(iface));
        }
        for (String constructor : constructors) {
            ctClass.addConstructor(CtNewConstructor.make(constructor, ctClass));
        }
        for (String field : fields) {
            ctClass.addField(CtField.make(field, ctClass));
        }
        for (String method : methods) {
            ctClass.addMethod(CtNewMethod.make(method, ctClass));
        }
        return ctClass;
    }

}
