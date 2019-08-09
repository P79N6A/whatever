package org.springframework.asm;

final class AnnotationWriter extends AnnotationVisitor {

    private final SymbolTable symbolTable;

    private final boolean useNamedValues;

    private final ByteVector annotation;

    private final int numElementValuePairsOffset;

    private int numElementValuePairs;

    private final AnnotationWriter previousAnnotation;

    private AnnotationWriter nextAnnotation;
    // -----------------------------------------------------------------------------------------------
    // Constructors and factories
    // -----------------------------------------------------------------------------------------------

    AnnotationWriter(final SymbolTable symbolTable, final boolean useNamedValues, final ByteVector annotation, final AnnotationWriter previousAnnotation) {
        super(Opcodes.ASM7);
        this.symbolTable = symbolTable;
        this.useNamedValues = useNamedValues;
        this.annotation = annotation;
        // By hypothesis, num_element_value_pairs is stored in the last unsigned short of 'annotation'.
        this.numElementValuePairsOffset = annotation.length == 0 ? -1 : annotation.length - 2;
        this.previousAnnotation = previousAnnotation;
        if (previousAnnotation != null) {
            previousAnnotation.nextAnnotation = this;
        }
    }

    static AnnotationWriter create(final SymbolTable symbolTable, final String descriptor, final AnnotationWriter previousAnnotation) {
        // Create a ByteVector to hold an 'annotation' JVMS structure.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.
        ByteVector annotation = new ByteVector();
        // Write type_index and reserve space for num_element_value_pairs.
        annotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0);
        return new AnnotationWriter(symbolTable, true, annotation, previousAnnotation);
    }

    static AnnotationWriter create(final SymbolTable symbolTable, final int typeRef, final TypePath typePath, final String descriptor, final AnnotationWriter previousAnnotation) {
        // Create a ByteVector to hold a 'type_annotation' JVMS structure.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.
        ByteVector typeAnnotation = new ByteVector();
        // Write target_type, target_info, and target_path.
        TypeReference.putTarget(typeRef, typeAnnotation);
        TypePath.put(typePath, typeAnnotation);
        // Write type_index and reserve space for num_element_value_pairs.
        typeAnnotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0);
        return new AnnotationWriter(symbolTable, true, typeAnnotation, previousAnnotation);
    }
    // -----------------------------------------------------------------------------------------------
    // Implementation of the AnnotationVisitor abstract class
    // -----------------------------------------------------------------------------------------------

    @Override
    public void visit(final String name, final Object value) {
        // Case of an element_value with a const_value_index, class_info_index or array_index field.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
        ++numElementValuePairs;
        if (useNamedValues) {
            annotation.putShort(symbolTable.addConstantUtf8(name));
        }
        if (value instanceof String) {
            annotation.put12('s', symbolTable.addConstantUtf8((String) value));
        } else if (value instanceof Byte) {
            annotation.put12('B', symbolTable.addConstantInteger(((Byte) value).byteValue()).index);
        } else if (value instanceof Boolean) {
            int booleanValue = ((Boolean) value).booleanValue() ? 1 : 0;
            annotation.put12('Z', symbolTable.addConstantInteger(booleanValue).index);
        } else if (value instanceof Character) {
            annotation.put12('C', symbolTable.addConstantInteger(((Character) value).charValue()).index);
        } else if (value instanceof Short) {
            annotation.put12('S', symbolTable.addConstantInteger(((Short) value).shortValue()).index);
        } else if (value instanceof Type) {
            annotation.put12('c', symbolTable.addConstantUtf8(((Type) value).getDescriptor()));
        } else if (value instanceof byte[]) {
            byte[] byteArray = (byte[]) value;
            annotation.put12('[', byteArray.length);
            for (byte byteValue : byteArray) {
                annotation.put12('B', symbolTable.addConstantInteger(byteValue).index);
            }
        } else if (value instanceof boolean[]) {
            boolean[] booleanArray = (boolean[]) value;
            annotation.put12('[', booleanArray.length);
            for (boolean booleanValue : booleanArray) {
                annotation.put12('Z', symbolTable.addConstantInteger(booleanValue ? 1 : 0).index);
            }
        } else if (value instanceof short[]) {
            short[] shortArray = (short[]) value;
            annotation.put12('[', shortArray.length);
            for (short shortValue : shortArray) {
                annotation.put12('S', symbolTable.addConstantInteger(shortValue).index);
            }
        } else if (value instanceof char[]) {
            char[] charArray = (char[]) value;
            annotation.put12('[', charArray.length);
            for (char charValue : charArray) {
                annotation.put12('C', symbolTable.addConstantInteger(charValue).index);
            }
        } else if (value instanceof int[]) {
            int[] intArray = (int[]) value;
            annotation.put12('[', intArray.length);
            for (int intValue : intArray) {
                annotation.put12('I', symbolTable.addConstantInteger(intValue).index);
            }
        } else if (value instanceof long[]) {
            long[] longArray = (long[]) value;
            annotation.put12('[', longArray.length);
            for (long longValue : longArray) {
                annotation.put12('J', symbolTable.addConstantLong(longValue).index);
            }
        } else if (value instanceof float[]) {
            float[] floatArray = (float[]) value;
            annotation.put12('[', floatArray.length);
            for (float floatValue : floatArray) {
                annotation.put12('F', symbolTable.addConstantFloat(floatValue).index);
            }
        } else if (value instanceof double[]) {
            double[] doubleArray = (double[]) value;
            annotation.put12('[', doubleArray.length);
            for (double doubleValue : doubleArray) {
                annotation.put12('D', symbolTable.addConstantDouble(doubleValue).index);
            }
        } else {
            Symbol symbol = symbolTable.addConstant(value);
            annotation.put12(".s.IFJDCS".charAt(symbol.tag), symbol.index);
        }
    }

    @Override
    public void visitEnum(final String name, final String descriptor, final String value) {
        // Case of an element_value with an enum_const_value field.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
        ++numElementValuePairs;
        if (useNamedValues) {
            annotation.putShort(symbolTable.addConstantUtf8(name));
        }
        annotation.put12('e', symbolTable.addConstantUtf8(descriptor)).putShort(symbolTable.addConstantUtf8(value));
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
        // Case of an element_value with an annotation_value field.
        // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1.
        ++numElementValuePairs;
        if (useNamedValues) {
            annotation.putShort(symbolTable.addConstantUtf8(name));
        }
        // Write tag and type_index, and reserve 2 bytes for num_element_value_pairs.
        annotation.put12('@', symbolTable.addConstantUtf8(descriptor)).putShort(0);
        return new AnnotationWriter(symbolTable, true, annotation, null);
    }

    @Override
    public AnnotationVisitor visitArray(final String name) {
        // Case of an element_value with an array_value field.
        // https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1
        ++numElementValuePairs;
        if (useNamedValues) {
            annotation.putShort(symbolTable.addConstantUtf8(name));
        }
        // Write tag, and reserve 2 bytes for num_values. Here we take advantage of the fact that the
        // end of an element_value of array type is similar to the end of an 'annotation' structure: an
        // unsigned short num_values followed by num_values element_value, versus an unsigned short
        // num_element_value_pairs, followed by num_element_value_pairs { element_name_index,
        // element_value } tuples. This allows us to use an AnnotationWriter with unnamed values to
        // visit the array elements. Its num_element_value_pairs will correspond to the number of array
        // elements and will be stored in what is in fact num_values.
        annotation.put12('[', 0);
        return new AnnotationWriter(symbolTable, false, annotation, null);
    }

    @Override
    public void visitEnd() {
        if (numElementValuePairsOffset != -1) {
            byte[] data = annotation.data;
            data[numElementValuePairsOffset] = (byte) (numElementValuePairs >>> 8);
            data[numElementValuePairsOffset + 1] = (byte) numElementValuePairs;
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------

    int computeAnnotationsSize(final String attributeName) {
        if (attributeName != null) {
            symbolTable.addConstantUtf8(attributeName);
        }
        // The attribute_name_index, attribute_length and num_annotations fields use 8 bytes.
        int attributeSize = 8;
        AnnotationWriter annotationWriter = this;
        while (annotationWriter != null) {
            attributeSize += annotationWriter.annotation.length;
            annotationWriter = annotationWriter.previousAnnotation;
        }
        return attributeSize;
    }

    static int computeAnnotationsSize(final AnnotationWriter lastRuntimeVisibleAnnotation, final AnnotationWriter lastRuntimeInvisibleAnnotation, final AnnotationWriter lastRuntimeVisibleTypeAnnotation, final AnnotationWriter lastRuntimeInvisibleTypeAnnotation) {
        int size = 0;
        if (lastRuntimeVisibleAnnotation != null) {
            size += lastRuntimeVisibleAnnotation.computeAnnotationsSize(Constants.RUNTIME_VISIBLE_ANNOTATIONS);
        }
        if (lastRuntimeInvisibleAnnotation != null) {
            size += lastRuntimeInvisibleAnnotation.computeAnnotationsSize(Constants.RUNTIME_INVISIBLE_ANNOTATIONS);
        }
        if (lastRuntimeVisibleTypeAnnotation != null) {
            size += lastRuntimeVisibleTypeAnnotation.computeAnnotationsSize(Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
        }
        if (lastRuntimeInvisibleTypeAnnotation != null) {
            size += lastRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
        }
        return size;
    }

    void putAnnotations(final int attributeNameIndex, final ByteVector output) {
        int attributeLength = 2; // For num_annotations.
        int numAnnotations = 0;
        AnnotationWriter annotationWriter = this;
        AnnotationWriter firstAnnotation = null;
        while (annotationWriter != null) {
            // In case the user forgot to call visitEnd().
            annotationWriter.visitEnd();
            attributeLength += annotationWriter.annotation.length;
            numAnnotations++;
            firstAnnotation = annotationWriter;
            annotationWriter = annotationWriter.previousAnnotation;
        }
        output.putShort(attributeNameIndex);
        output.putInt(attributeLength);
        output.putShort(numAnnotations);
        annotationWriter = firstAnnotation;
        while (annotationWriter != null) {
            output.putByteArray(annotationWriter.annotation.data, 0, annotationWriter.annotation.length);
            annotationWriter = annotationWriter.nextAnnotation;
        }
    }

    static void putAnnotations(final SymbolTable symbolTable, final AnnotationWriter lastRuntimeVisibleAnnotation, final AnnotationWriter lastRuntimeInvisibleAnnotation, final AnnotationWriter lastRuntimeVisibleTypeAnnotation, final AnnotationWriter lastRuntimeInvisibleTypeAnnotation, final ByteVector output) {
        if (lastRuntimeVisibleAnnotation != null) {
            lastRuntimeVisibleAnnotation.putAnnotations(symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_ANNOTATIONS), output);
        }
        if (lastRuntimeInvisibleAnnotation != null) {
            lastRuntimeInvisibleAnnotation.putAnnotations(symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_ANNOTATIONS), output);
        }
        if (lastRuntimeVisibleTypeAnnotation != null) {
            lastRuntimeVisibleTypeAnnotation.putAnnotations(symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS), output);
        }
        if (lastRuntimeInvisibleTypeAnnotation != null) {
            lastRuntimeInvisibleTypeAnnotation.putAnnotations(symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS), output);
        }
    }

    static int computeParameterAnnotationsSize(final String attributeName, final AnnotationWriter[] annotationWriters, final int annotableParameterCount) {
        // Note: attributeName is added to the constant pool by the call to computeAnnotationsSize
        // below. This assumes that there is at least one non-null element in the annotationWriters
        // sub-array (which is ensured by the lazy instantiation of this array in MethodWriter).
        // The attribute_name_index, attribute_length and num_parameters fields use 7 bytes, and each
        // element of the parameter_annotations array uses 2 bytes for its num_annotations field.
        int attributeSize = 7 + 2 * annotableParameterCount;
        for (int i = 0; i < annotableParameterCount; ++i) {
            AnnotationWriter annotationWriter = annotationWriters[i];
            attributeSize += annotationWriter == null ? 0 : annotationWriter.computeAnnotationsSize(attributeName) - 8;
        }
        return attributeSize;
    }

    static void putParameterAnnotations(final int attributeNameIndex, final AnnotationWriter[] annotationWriters, final int annotableParameterCount, final ByteVector output) {
        // The num_parameters field uses 1 byte, and each element of the parameter_annotations array
        // uses 2 bytes for its num_annotations field.
        int attributeLength = 1 + 2 * annotableParameterCount;
        for (int i = 0; i < annotableParameterCount; ++i) {
            AnnotationWriter annotationWriter = annotationWriters[i];
            attributeLength += annotationWriter == null ? 0 : annotationWriter.computeAnnotationsSize(null) - 8;
        }
        output.putShort(attributeNameIndex);
        output.putInt(attributeLength);
        output.putByte(annotableParameterCount);
        for (int i = 0; i < annotableParameterCount; ++i) {
            AnnotationWriter annotationWriter = annotationWriters[i];
            AnnotationWriter firstAnnotation = null;
            int numAnnotations = 0;
            while (annotationWriter != null) {
                // In case user the forgot to call visitEnd().
                annotationWriter.visitEnd();
                numAnnotations++;
                firstAnnotation = annotationWriter;
                annotationWriter = annotationWriter.previousAnnotation;
            }
            output.putShort(numAnnotations);
            annotationWriter = firstAnnotation;
            while (annotationWriter != null) {
                output.putByteArray(annotationWriter.annotation.data, 0, annotationWriter.annotation.length);
                annotationWriter = annotationWriter.nextAnnotation;
            }
        }
    }

}
