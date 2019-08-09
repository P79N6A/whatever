package org.springframework.expression.spel.ast;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

public class Elvis extends SpelNodeImpl {

    public Elvis(int startPos, int endPos, SpelNodeImpl... args) {
        super(startPos, endPos, args);
    }

    @Override
    public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
        TypedValue value = this.children[0].getValueInternal(state);
        // If this check is changed, the generateCode method will need changing too
        if (!StringUtils.isEmpty(value.getValue())) {
            return value;
        } else {
            TypedValue result = this.children[1].getValueInternal(state);
            computeExitTypeDescriptor();
            return result;
        }
    }

    @Override
    public String toStringAST() {
        return getChild(0).toStringAST() + " ?: " + getChild(1).toStringAST();
    }

    @Override
    public boolean isCompilable() {
        SpelNodeImpl condition = this.children[0];
        SpelNodeImpl ifNullValue = this.children[1];
        return (condition.isCompilable() && ifNullValue.isCompilable() && condition.exitTypeDescriptor != null && ifNullValue.exitTypeDescriptor != null);
    }

    @Override
    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        // exit type descriptor can be null if both components are literal expressions
        computeExitTypeDescriptor();
        cf.enterCompilationScope();
        this.children[0].generateCode(mv, cf);
        String lastDesc = cf.lastDescriptor();
        Assert.state(lastDesc != null, "No last descriptor");
        CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
        cf.exitCompilationScope();
        Label elseTarget = new Label();
        Label endOfIf = new Label();
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNULL, elseTarget);
        // Also check if empty string, as per the code in the interpreted version
        mv.visitInsn(DUP);
        mv.visitLdcInsn("");
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        mv.visitJumpInsn(IFEQ, endOfIf);  // if not empty, drop through to elseTarget
        mv.visitLabel(elseTarget);
        mv.visitInsn(POP);
        cf.enterCompilationScope();
        this.children[1].generateCode(mv, cf);
        if (!CodeFlow.isPrimitive(this.exitTypeDescriptor)) {
            lastDesc = cf.lastDescriptor();
            Assert.state(lastDesc != null, "No last descriptor");
            CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
        }
        cf.exitCompilationScope();
        mv.visitLabel(endOfIf);
        cf.pushDescriptor(this.exitTypeDescriptor);
    }

    private void computeExitTypeDescriptor() {
        if (this.exitTypeDescriptor == null && this.children[0].exitTypeDescriptor != null && this.children[1].exitTypeDescriptor != null) {
            String conditionDescriptor = this.children[0].exitTypeDescriptor;
            String ifNullValueDescriptor = this.children[1].exitTypeDescriptor;
            if (ObjectUtils.nullSafeEquals(conditionDescriptor, ifNullValueDescriptor)) {
                this.exitTypeDescriptor = conditionDescriptor;
            } else {
                // Use the easiest to compute common super type
                this.exitTypeDescriptor = "Ljava/lang/Object";
            }
        }
    }

}
