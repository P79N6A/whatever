package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public abstract class SpelNodeImpl implements SpelNode, Opcodes {

    private static final SpelNodeImpl[] NO_CHILDREN = new SpelNodeImpl[0];

    private final int startPos;

    private final int endPos;

    protected SpelNodeImpl[] children = SpelNodeImpl.NO_CHILDREN;

    @Nullable
    private SpelNodeImpl parent;

    @Nullable
    protected volatile String exitTypeDescriptor;

    public SpelNodeImpl(int startPos, int endPos, SpelNodeImpl... operands) {
        this.startPos = startPos;
        this.endPos = endPos;
        if (!ObjectUtils.isEmpty(operands)) {
            this.children = operands;
            for (SpelNodeImpl operand : operands) {
                Assert.notNull(operand, "Operand must not be null");
                operand.parent = this;
            }
        }
    }

    protected boolean nextChildIs(Class<?>... classes) {
        if (this.parent != null) {
            SpelNodeImpl[] peers = this.parent.children;
            for (int i = 0, max = peers.length; i < max; i++) {
                if (this == peers[i]) {
                    if (i + 1 >= max) {
                        return false;
                    }
                    Class<?> peerClass = peers[i + 1].getClass();
                    for (Class<?> desiredClass : classes) {
                        if (peerClass == desiredClass) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    @Nullable
    public final Object getValue(ExpressionState expressionState) throws EvaluationException {
        return getValueInternal(expressionState).getValue();
    }

    @Override
    public final TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException {
        return getValueInternal(expressionState);
    }

    // by default Ast nodes are not writable
    @Override
    public boolean isWritable(ExpressionState expressionState) throws EvaluationException {
        return false;
    }

    @Override
    public void setValue(ExpressionState expressionState, @Nullable Object newValue) throws EvaluationException {
        throw new SpelEvaluationException(getStartPosition(), SpelMessage.SETVALUE_NOT_SUPPORTED, getClass());
    }

    @Override
    public SpelNode getChild(int index) {
        return this.children[index];
    }

    @Override
    public int getChildCount() {
        return this.children.length;
    }

    @Override
    @Nullable
    public Class<?> getObjectClass(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }
        return (obj instanceof Class ? ((Class<?>) obj) : obj.getClass());
    }

    @Override
    public int getStartPosition() {
        return this.startPos;
    }

    @Override
    public int getEndPosition() {
        return this.endPos;
    }

    public boolean isCompilable() {
        return false;
    }

    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        throw new IllegalStateException(getClass().getName() + " has no generateCode(..) method");
    }

    @Nullable
    public String getExitDescriptor() {
        return this.exitTypeDescriptor;
    }

    @Nullable
    protected final <T> T getValue(ExpressionState state, Class<T> desiredReturnType) throws EvaluationException {
        return ExpressionUtils.convertTypedValue(state.getEvaluationContext(), getValueInternal(state), desiredReturnType);
    }

    protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
        throw new SpelEvaluationException(getStartPosition(), SpelMessage.NOT_ASSIGNABLE, toStringAST());
    }

    public abstract TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException;

    protected static void generateCodeForArguments(MethodVisitor mv, CodeFlow cf, Member member, SpelNodeImpl[] arguments) {
        String[] paramDescriptors = null;
        boolean isVarargs = false;
        if (member instanceof Constructor) {
            Constructor<?> ctor = (Constructor<?>) member;
            paramDescriptors = CodeFlow.toDescriptors(ctor.getParameterTypes());
            isVarargs = ctor.isVarArgs();
        } else { // Method
            Method method = (Method) member;
            paramDescriptors = CodeFlow.toDescriptors(method.getParameterTypes());
            isVarargs = method.isVarArgs();
        }
        if (isVarargs) {
            // The final parameter may or may not need packaging into an array, or nothing may
            // have been passed to satisfy the varargs and so something needs to be built.
            int p = 0; // Current supplied argument being processed
            int childCount = arguments.length;
            // Fulfill all the parameter requirements except the last one
            for (p = 0; p < paramDescriptors.length - 1; p++) {
                generateCodeForArgument(mv, cf, arguments[p], paramDescriptors[p]);
            }
            SpelNodeImpl lastChild = (childCount == 0 ? null : arguments[childCount - 1]);
            String arrayType = paramDescriptors[paramDescriptors.length - 1];
            // Determine if the final passed argument is already suitably packaged in array
            // form to be passed to the method
            if (lastChild != null && arrayType.equals(lastChild.getExitDescriptor())) {
                generateCodeForArgument(mv, cf, lastChild, paramDescriptors[p]);
            } else {
                arrayType = arrayType.substring(1); // trim the leading '[', may leave other '['
                // build array big enough to hold remaining arguments
                CodeFlow.insertNewArrayCode(mv, childCount - p, arrayType);
                // Package up the remaining arguments into the array
                int arrayindex = 0;
                while (p < childCount) {
                    SpelNodeImpl child = arguments[p];
                    mv.visitInsn(DUP);
                    CodeFlow.insertOptimalLoad(mv, arrayindex++);
                    generateCodeForArgument(mv, cf, child, arrayType);
                    CodeFlow.insertArrayStore(mv, arrayType);
                    p++;
                }
            }
        } else {
            for (int i = 0; i < paramDescriptors.length; i++) {
                generateCodeForArgument(mv, cf, arguments[i], paramDescriptors[i]);
            }
        }
    }

    protected static void generateCodeForArgument(MethodVisitor mv, CodeFlow cf, SpelNodeImpl argument, String paramDesc) {
        cf.enterCompilationScope();
        argument.generateCode(mv, cf);
        String lastDesc = cf.lastDescriptor();
        Assert.state(lastDesc != null, "No last descriptor");
        boolean primitiveOnStack = CodeFlow.isPrimitive(lastDesc);
        // Check if need to box it for the method reference?
        if (primitiveOnStack && paramDesc.charAt(0) == 'L') {
            CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
        } else if (paramDesc.length() == 1 && !primitiveOnStack) {
            CodeFlow.insertUnboxInsns(mv, paramDesc.charAt(0), lastDesc);
        } else if (!paramDesc.equals(lastDesc)) {
            // This would be unnecessary in the case of subtyping (e.g. method takes Number but Integer passed in)
            CodeFlow.insertCheckCast(mv, paramDesc);
        }
        cf.exitCompilationScope();
    }

}
