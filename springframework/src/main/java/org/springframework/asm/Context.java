package org.springframework.asm;

final class Context {

    Attribute[] attributePrototypes;

    int parsingOptions;

    char[] charBuffer;
    // Information about the current method, i.e. the one read in the current (or latest) call
    // to {@link ClassReader#readMethod()}.

    int currentMethodAccessFlags;

    String currentMethodName;

    String currentMethodDescriptor;

    Label[] currentMethodLabels;
    // Information about the current type annotation target, i.e. the one read in the current
    // (or latest) call to {@link ClassReader#readAnnotationTarget()}.

    int currentTypeAnnotationTarget;

    TypePath currentTypeAnnotationTargetPath;

    Label[] currentLocalVariableAnnotationRangeStarts;

    Label[] currentLocalVariableAnnotationRangeEnds;

    int[] currentLocalVariableAnnotationRangeIndices;
    // Information about the current stack map frame, i.e. the one read in the current (or latest)
    // call to {@link ClassReader#readFrame()}.

    int currentFrameOffset;

    int currentFrameType;

    int currentFrameLocalCount;

    int currentFrameLocalCountDelta;

    Object[] currentFrameLocalTypes;

    int currentFrameStackCount;

    Object[] currentFrameStackTypes;

}
