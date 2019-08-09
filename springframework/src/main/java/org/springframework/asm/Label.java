package org.springframework.asm;

public class Label {

    static final int FLAG_DEBUG_ONLY = 1;

    static final int FLAG_JUMP_TARGET = 2;

    static final int FLAG_RESOLVED = 4;

    static final int FLAG_REACHABLE = 8;

    static final int FLAG_SUBROUTINE_CALLER = 16;

    static final int FLAG_SUBROUTINE_START = 32;

    static final int FLAG_SUBROUTINE_END = 64;

    static final int LINE_NUMBERS_CAPACITY_INCREMENT = 4;

    static final int FORWARD_REFERENCES_CAPACITY_INCREMENT = 6;

    static final int FORWARD_REFERENCE_TYPE_MASK = 0xF0000000;

    static final int FORWARD_REFERENCE_TYPE_SHORT = 0x10000000;

    static final int FORWARD_REFERENCE_TYPE_WIDE = 0x20000000;

    static final int FORWARD_REFERENCE_HANDLE_MASK = 0x0FFFFFFF;

    static final Label EMPTY_LIST = new Label();

    public Object info;

    short flags;

    private short lineNumber;

    private int[] otherLineNumbers;

    int bytecodeOffset;

    private int[] forwardReferences;
    // -----------------------------------------------------------------------------------------------
    // Fields for the control flow and data flow graph analysis algorithms (used to compute the
    // maximum stack size or the stack map frames). A control flow graph contains one node per "basic
    // block", and one edge per "jump" from one basic block to another. Each node (i.e., each basic
    // block) is represented with the Label object that corresponds to the first instruction of this
    // basic block. Each node also stores the list of its successors in the graph, as a linked list of
    // Edge objects.
    //
    // The control flow analysis algorithms used to compute the maximum stack size or the stack map
    // frames are similar and use two steps. The first step, during the visit of each instruction,
    // builds information about the state of the local variables and the operand stack at the end of
    // each basic block, called the "output frame", <i>relatively</i> to the frame state at the
    // beginning of the basic block, which is called the "input frame", and which is <i>unknown</i>
    // during this step. The second step, in {@link MethodWriter#computeAllFrames} and {@link
    // MethodWriter#computeMaxStackAndLocal}, is a fix point algorithm
    // that computes information about the input frame of each basic block, from the input state of
    // the first basic block (known from the method signature), and by the using the previously
    // computed relative output frames.
    //
    // The algorithm used to compute the maximum stack size only computes the relative output and
    // absolute input stack heights, while the algorithm used to compute stack map frames computes
    // relative output frames and absolute input frames.

    short inputStackSize;

    short outputStackSize;

    short outputStackMax;

    short subroutineId;

    Frame frame;

    Label nextBasicBlock;

    Edge outgoingEdges;

    Label nextListElement;
    // -----------------------------------------------------------------------------------------------
    // Constructor and accessors
    // -----------------------------------------------------------------------------------------------

    public Label() {
        // Nothing to do.
    }

    public int getOffset() {
        if ((flags & FLAG_RESOLVED) == 0) {
            throw new IllegalStateException("Label offset position has not been resolved yet");
        }
        return bytecodeOffset;
    }

    final Label getCanonicalInstance() {
        return frame == null ? this : frame.owner;
    }
    // -----------------------------------------------------------------------------------------------
    // Methods to manage line numbers
    // -----------------------------------------------------------------------------------------------

    final void addLineNumber(final int lineNumber) {
        if (this.lineNumber == 0) {
            this.lineNumber = (short) lineNumber;
        } else {
            if (otherLineNumbers == null) {
                otherLineNumbers = new int[LINE_NUMBERS_CAPACITY_INCREMENT];
            }
            int otherLineNumberIndex = ++otherLineNumbers[0];
            if (otherLineNumberIndex >= otherLineNumbers.length) {
                int[] newLineNumbers = new int[otherLineNumbers.length + LINE_NUMBERS_CAPACITY_INCREMENT];
                System.arraycopy(otherLineNumbers, 0, newLineNumbers, 0, otherLineNumbers.length);
                otherLineNumbers = newLineNumbers;
            }
            otherLineNumbers[otherLineNumberIndex] = lineNumber;
        }
    }

    final void accept(final MethodVisitor methodVisitor, final boolean visitLineNumbers) {
        methodVisitor.visitLabel(this);
        if (visitLineNumbers && lineNumber != 0) {
            methodVisitor.visitLineNumber(lineNumber & 0xFFFF, this);
            if (otherLineNumbers != null) {
                for (int i = 1; i <= otherLineNumbers[0]; ++i) {
                    methodVisitor.visitLineNumber(otherLineNumbers[i], this);
                }
            }
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Methods to compute offsets and to manage forward references
    // -----------------------------------------------------------------------------------------------

    final void put(final ByteVector code, final int sourceInsnBytecodeOffset, final boolean wideReference) {
        if ((flags & FLAG_RESOLVED) == 0) {
            if (wideReference) {
                addForwardReference(sourceInsnBytecodeOffset, FORWARD_REFERENCE_TYPE_WIDE, code.length);
                code.putInt(-1);
            } else {
                addForwardReference(sourceInsnBytecodeOffset, FORWARD_REFERENCE_TYPE_SHORT, code.length);
                code.putShort(-1);
            }
        } else {
            if (wideReference) {
                code.putInt(bytecodeOffset - sourceInsnBytecodeOffset);
            } else {
                code.putShort(bytecodeOffset - sourceInsnBytecodeOffset);
            }
        }
    }

    private void addForwardReference(final int sourceInsnBytecodeOffset, final int referenceType, final int referenceHandle) {
        if (forwardReferences == null) {
            forwardReferences = new int[FORWARD_REFERENCES_CAPACITY_INCREMENT];
        }
        int lastElementIndex = forwardReferences[0];
        if (lastElementIndex + 2 >= forwardReferences.length) {
            int[] newValues = new int[forwardReferences.length + FORWARD_REFERENCES_CAPACITY_INCREMENT];
            System.arraycopy(forwardReferences, 0, newValues, 0, forwardReferences.length);
            forwardReferences = newValues;
        }
        forwardReferences[++lastElementIndex] = sourceInsnBytecodeOffset;
        forwardReferences[++lastElementIndex] = referenceType | referenceHandle;
        forwardReferences[0] = lastElementIndex;
    }

    final boolean resolve(final byte[] code, final int bytecodeOffset) {
        this.flags |= FLAG_RESOLVED;
        this.bytecodeOffset = bytecodeOffset;
        if (forwardReferences == null) {
            return false;
        }
        boolean hasAsmInstructions = false;
        for (int i = forwardReferences[0]; i > 0; i -= 2) {
            final int sourceInsnBytecodeOffset = forwardReferences[i - 1];
            final int reference = forwardReferences[i];
            final int relativeOffset = bytecodeOffset - sourceInsnBytecodeOffset;
            int handle = reference & FORWARD_REFERENCE_HANDLE_MASK;
            if ((reference & FORWARD_REFERENCE_TYPE_MASK) == FORWARD_REFERENCE_TYPE_SHORT) {
                if (relativeOffset < Short.MIN_VALUE || relativeOffset > Short.MAX_VALUE) {
                    // Change the opcode of the jump instruction, in order to be able to find it later in
                    // ClassReader. These ASM specific opcodes are similar to jump instruction opcodes, except
                    // that the 2 bytes offset is unsigned (and can therefore represent values from 0 to
                    // 65535, which is sufficient since the size of a method is limited to 65535 bytes).
                    int opcode = code[sourceInsnBytecodeOffset] & 0xFF;
                    if (opcode < Opcodes.IFNULL) {
                        // Change IFEQ ... JSR to ASM_IFEQ ... ASM_JSR.
                        code[sourceInsnBytecodeOffset] = (byte) (opcode + Constants.ASM_OPCODE_DELTA);
                    } else {
                        // Change IFNULL and IFNONNULL to ASM_IFNULL and ASM_IFNONNULL.
                        code[sourceInsnBytecodeOffset] = (byte) (opcode + Constants.ASM_IFNULL_OPCODE_DELTA);
                    }
                    hasAsmInstructions = true;
                }
                code[handle++] = (byte) (relativeOffset >>> 8);
                code[handle] = (byte) relativeOffset;
            } else {
                code[handle++] = (byte) (relativeOffset >>> 24);
                code[handle++] = (byte) (relativeOffset >>> 16);
                code[handle++] = (byte) (relativeOffset >>> 8);
                code[handle] = (byte) relativeOffset;
            }
        }
        return hasAsmInstructions;
    }
    // -----------------------------------------------------------------------------------------------
    // Methods related to subroutines
    // -----------------------------------------------------------------------------------------------

    final void markSubroutine(final short subroutineId) {
        // Data flow algorithm: put this basic block in a list of blocks to process (which are blocks
        // belonging to subroutine subroutineId) and, while there are blocks to process, remove one from
        // the list, mark it as belonging to the subroutine, and add its successor basic blocks in the
        // control flow graph to the list of blocks to process (if not already done).
        Label listOfBlocksToProcess = this;
        listOfBlocksToProcess.nextListElement = EMPTY_LIST;
        while (listOfBlocksToProcess != EMPTY_LIST) {
            // Remove a basic block from the list of blocks to process.
            Label basicBlock = listOfBlocksToProcess;
            listOfBlocksToProcess = listOfBlocksToProcess.nextListElement;
            basicBlock.nextListElement = null;
            // If it is not already marked as belonging to a subroutine, mark it as belonging to
            // subroutineId and add its successors to the list of blocks to process (unless already done).
            if (basicBlock.subroutineId == 0) {
                basicBlock.subroutineId = subroutineId;
                listOfBlocksToProcess = basicBlock.pushSuccessors(listOfBlocksToProcess);
            }
        }
    }

    final void addSubroutineRetSuccessors(final Label subroutineCaller) {
        // Data flow algorithm: put this basic block in a list blocks to process (which are blocks
        // belonging to a subroutine starting with this label) and, while there are blocks to process,
        // remove one from the list, put it in a list of blocks that have been processed, add a return
        // edge to the successor of subroutineCaller if applicable, and add its successor basic blocks
        // in the control flow graph to the list of blocks to process (if not already done).
        Label listOfProcessedBlocks = EMPTY_LIST;
        Label listOfBlocksToProcess = this;
        listOfBlocksToProcess.nextListElement = EMPTY_LIST;
        while (listOfBlocksToProcess != EMPTY_LIST) {
            // Move a basic block from the list of blocks to process to the list of processed blocks.
            Label basicBlock = listOfBlocksToProcess;
            listOfBlocksToProcess = basicBlock.nextListElement;
            basicBlock.nextListElement = listOfProcessedBlocks;
            listOfProcessedBlocks = basicBlock;
            // Add an edge from this block to the successor of the caller basic block, if this block is
            // the end of a subroutine and if this block and subroutineCaller do not belong to the same
            // subroutine.
            if ((basicBlock.flags & FLAG_SUBROUTINE_END) != 0 && basicBlock.subroutineId != subroutineCaller.subroutineId) {
                basicBlock.outgoingEdges = new Edge(basicBlock.outputStackSize,
                        // By construction, the first outgoing edge of a basic block that ends with a jsr
                        // instruction leads to the jsr continuation block, i.e. where execution continues
                        // when ret is called (see {@link #FLAG_SUBROUTINE_CALLER}).
                        subroutineCaller.outgoingEdges.successor, basicBlock.outgoingEdges);
            }
            // Add its successors to the list of blocks to process. Note that {@link #pushSuccessors} does
            // not push basic blocks which are already in a list. Here this means either in the list of
            // blocks to process, or in the list of already processed blocks. This second list is
            // important to make sure we don't reprocess an already processed block.
            listOfBlocksToProcess = basicBlock.pushSuccessors(listOfBlocksToProcess);
        }
        // Reset the {@link #nextListElement} of all the basic blocks that have been processed to null,
        // so that this method can be called again with a different subroutine or subroutine caller.
        while (listOfProcessedBlocks != EMPTY_LIST) {
            Label newListOfProcessedBlocks = listOfProcessedBlocks.nextListElement;
            listOfProcessedBlocks.nextListElement = null;
            listOfProcessedBlocks = newListOfProcessedBlocks;
        }
    }

    private Label pushSuccessors(final Label listOfLabelsToProcess) {
        Label newListOfLabelsToProcess = listOfLabelsToProcess;
        Edge outgoingEdge = outgoingEdges;
        while (outgoingEdge != null) {
            // By construction, the second outgoing edge of a basic block that ends with a jsr instruction
            // leads to the jsr target (see {@link #FLAG_SUBROUTINE_CALLER}).
            boolean isJsrTarget = (flags & Label.FLAG_SUBROUTINE_CALLER) != 0 && outgoingEdge == outgoingEdges.nextEdge;
            if (!isJsrTarget && outgoingEdge.successor.nextListElement == null) {
                // Add this successor to the list of blocks to process, if it does not already belong to a
                // list of labels.
                outgoingEdge.successor.nextListElement = newListOfLabelsToProcess;
                newListOfLabelsToProcess = outgoingEdge.successor;
            }
            outgoingEdge = outgoingEdge.nextEdge;
        }
        return newListOfLabelsToProcess;
    }
    // -----------------------------------------------------------------------------------------------
    // Overridden Object methods
    // -----------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "L" + System.identityHashCode(this);
    }

}
