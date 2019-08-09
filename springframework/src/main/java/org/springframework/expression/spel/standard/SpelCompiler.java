package org.springframework.expression.spel.standard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class SpelCompiler implements Opcodes {

    private static final Log logger = LogFactory.getLog(SpelCompiler.class);

    private static final int CLASSES_DEFINED_LIMIT = 100;

    // A compiler is created for each classloader, it manages a child class loader of that
    // classloader and the child is used to load the compiled expressions.
    private static final Map<ClassLoader, SpelCompiler> compilers = new ConcurrentReferenceHashMap<>();

    // The child ClassLoader used to load the compiled expression classes
    private ChildClassLoader ccl;

    // Counter suffix for generated classes within this SpelCompiler instance
    private final AtomicInteger suffixId = new AtomicInteger(1);

    private SpelCompiler(@Nullable ClassLoader classloader) {
        this.ccl = new ChildClassLoader(classloader);
    }

    @Nullable
    public CompiledExpression compile(SpelNodeImpl expression) {
        if (expression.isCompilable()) {
            if (logger.isDebugEnabled()) {
                logger.debug("SpEL: compiling " + expression.toStringAST());
            }
            Class<? extends CompiledExpression> clazz = createExpressionClass(expression);
            if (clazz != null) {
                try {
                    return ReflectionUtils.accessibleConstructor(clazz).newInstance();
                } catch (Throwable ex) {
                    throw new IllegalStateException("Failed to instantiate CompiledExpression", ex);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("SpEL: unable to compile " + expression.toStringAST());
        }
        return null;
    }

    private int getNextSuffix() {
        return this.suffixId.incrementAndGet();
    }

    @Nullable
    private Class<? extends CompiledExpression> createExpressionClass(SpelNodeImpl expressionToCompile) {
        // Create class outline 'spel/ExNNN extends org.springframework.expression.spel.CompiledExpression'
        String className = "spel/Ex" + getNextSuffix();
        ClassWriter cw = new ExpressionClassWriter();
        cw.visit(V1_5, ACC_PUBLIC, className, null, "org/springframework/expression/spel/CompiledExpression", null);
        // Create default constructor
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "org/springframework/expression/spel/CompiledExpression", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        // Create getValue() method
        mv = cw.visitMethod(ACC_PUBLIC, "getValue", "(Ljava/lang/Object;Lorg/springframework/expression/EvaluationContext;)Ljava/lang/Object;", null, new String[]{"org/springframework/expression/EvaluationException"});
        mv.visitCode();
        CodeFlow cf = new CodeFlow(className, cw);
        // Ask the expression AST to generate the body of the method
        try {
            expressionToCompile.generateCode(mv, cf);
        } catch (IllegalStateException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(expressionToCompile.getClass().getSimpleName() + ".generateCode opted out of compilation: " + ex.getMessage());
            }
            return null;
        }
        CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor());
        if ("V".equals(cf.lastDescriptor())) {
            mv.visitInsn(ACONST_NULL);
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);  // not supplied due to COMPUTE_MAXS
        mv.visitEnd();
        cw.visitEnd();
        cf.finish();
        byte[] data = cw.toByteArray();
        // TODO need to make this conditionally occur based on a debug flag
        // dump(expressionToCompile.toStringAST(), clazzName, data);
        return loadClass(StringUtils.replace(className, "/", "."), data);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends CompiledExpression> loadClass(String name, byte[] bytes) {
        if (this.ccl.getClassesDefinedCount() > CLASSES_DEFINED_LIMIT) {
            this.ccl = new ChildClassLoader(this.ccl.getParent());
        }
        return (Class<? extends CompiledExpression>) this.ccl.defineClass(name, bytes);
    }

    public static SpelCompiler getCompiler(@Nullable ClassLoader classLoader) {
        ClassLoader clToUse = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
        synchronized (compilers) {
            SpelCompiler compiler = compilers.get(clToUse);
            if (compiler == null) {
                compiler = new SpelCompiler(clToUse);
                compilers.put(clToUse, compiler);
            }
            return compiler;
        }
    }

    public static boolean compile(Expression expression) {
        return (expression instanceof SpelExpression && ((SpelExpression) expression).compileExpression());
    }

    public static void revertToInterpreted(Expression expression) {
        if (expression instanceof SpelExpression) {
            ((SpelExpression) expression).revertToInterpreted();
        }
    }

    private static class ChildClassLoader extends URLClassLoader {

        private static final URL[] NO_URLS = new URL[0];

        private int classesDefinedCount = 0;

        public ChildClassLoader(@Nullable ClassLoader classLoader) {
            super(NO_URLS, classLoader);
        }

        int getClassesDefinedCount() {
            return this.classesDefinedCount;
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            Class<?> clazz = super.defineClass(name, bytes, 0, bytes.length);
            this.classesDefinedCount++;
            return clazz;
        }

    }

    private class ExpressionClassWriter extends ClassWriter {

        public ExpressionClassWriter() {
            super(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        }

        @Override
        protected ClassLoader getClassLoader() {
            return ccl;
        }

    }

}
