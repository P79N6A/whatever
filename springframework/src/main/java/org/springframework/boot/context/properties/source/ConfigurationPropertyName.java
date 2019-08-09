package org.springframework.boot.context.properties.source;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class ConfigurationPropertyName implements Comparable<ConfigurationPropertyName> {

    private static final String EMPTY_STRING = "";

    public static final ConfigurationPropertyName EMPTY = new ConfigurationPropertyName(Elements.EMPTY);

    private Elements elements;

    private final CharSequence[] uniformElements;

    private String string;

    private ConfigurationPropertyName(Elements elements) {
        this.elements = elements;
        this.uniformElements = new CharSequence[elements.getSize()];
    }

    public boolean isEmpty() {
        return this.elements.getSize() == 0;
    }

    public boolean isLastElementIndexed() {
        int size = getNumberOfElements();
        return (size > 0 && isIndexed(size - 1));
    }

    boolean isIndexed(int elementIndex) {
        return this.elements.getType(elementIndex).isIndexed();
    }

    public boolean isNumericIndex(int elementIndex) {
        return this.elements.getType(elementIndex) == ElementType.NUMERICALLY_INDEXED;
    }

    public String getLastElement(Form form) {
        int size = getNumberOfElements();
        return (size != 0) ? getElement(size - 1, form) : EMPTY_STRING;
    }

    public String getElement(int elementIndex, Form form) {
        CharSequence element = this.elements.get(elementIndex);
        ElementType type = this.elements.getType(elementIndex);
        if (type.isIndexed()) {
            return element.toString();
        }
        if (form == Form.ORIGINAL) {
            if (type != ElementType.NON_UNIFORM) {
                return element.toString();
            }
            return convertToOriginalForm(element).toString();
        }
        if (form == Form.DASHED) {
            if (type == ElementType.UNIFORM || type == ElementType.DASHED) {
                return element.toString();
            }
            return convertToDashedElement(element).toString();
        }
        CharSequence uniformElement = this.uniformElements[elementIndex];
        if (uniformElement == null) {
            uniformElement = (type != ElementType.UNIFORM) ? convertToUniformElement(element) : element;
            this.uniformElements[elementIndex] = uniformElement.toString();
        }
        return uniformElement.toString();
    }

    private CharSequence convertToOriginalForm(CharSequence element) {
        return convertElement(element, false, (ch, i) -> ch == '_' || ElementsParser.isValidChar(Character.toLowerCase(ch), i));
    }

    private CharSequence convertToDashedElement(CharSequence element) {
        return convertElement(element, true, ElementsParser::isValidChar);
    }

    private CharSequence convertToUniformElement(CharSequence element) {
        return convertElement(element, true, (ch, i) -> ElementsParser.isAlphaNumeric(ch));
    }

    private CharSequence convertElement(CharSequence element, boolean lowercase, ElementCharPredicate filter) {
        StringBuilder result = new StringBuilder(element.length());
        for (int i = 0; i < element.length(); i++) {
            char ch = lowercase ? Character.toLowerCase(element.charAt(i)) : element.charAt(i);
            if (filter.test(ch, i)) {
                result.append(ch);
            }
        }
        return result;
    }

    public int getNumberOfElements() {
        return this.elements.getSize();
    }

    public ConfigurationPropertyName append(String elementValue) {
        if (elementValue == null) {
            return this;
        }
        Elements additionalElements = probablySingleElementOf(elementValue);
        return new ConfigurationPropertyName(this.elements.append(additionalElements));
    }

    public ConfigurationPropertyName chop(int size) {
        if (size >= getNumberOfElements()) {
            return this;
        }
        return new ConfigurationPropertyName(this.elements.chop(size));
    }

    public boolean isParentOf(ConfigurationPropertyName name) {
        Assert.notNull(name, "Name must not be null");
        if (this.getNumberOfElements() != name.getNumberOfElements() - 1) {
            return false;
        }
        return isAncestorOf(name);
    }

    public boolean isAncestorOf(ConfigurationPropertyName name) {
        Assert.notNull(name, "Name must not be null");
        if (this.getNumberOfElements() >= name.getNumberOfElements()) {
            return false;
        }
        return elementsEqual(name);
    }

    @Override
    public int compareTo(ConfigurationPropertyName other) {
        return compare(this, other);
    }

    private int compare(ConfigurationPropertyName n1, ConfigurationPropertyName n2) {
        int l1 = n1.getNumberOfElements();
        int l2 = n2.getNumberOfElements();
        int i1 = 0;
        int i2 = 0;
        while (i1 < l1 || i2 < l2) {
            try {
                ElementType type1 = (i1 < l1) ? n1.elements.getType(i1) : null;
                ElementType type2 = (i2 < l2) ? n2.elements.getType(i2) : null;
                String e1 = (i1 < l1) ? n1.getElement(i1++, Form.UNIFORM) : null;
                String e2 = (i2 < l2) ? n2.getElement(i2++, Form.UNIFORM) : null;
                int result = compare(e1, type1, e2, type2);
                if (result != 0) {
                    return result;
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new RuntimeException(ex);
            }
        }
        return 0;
    }

    private int compare(String e1, ElementType type1, String e2, ElementType type2) {
        if (e1 == null) {
            return -1;
        }
        if (e2 == null) {
            return 1;
        }
        int result = Boolean.compare(type2.isIndexed(), type1.isIndexed());
        if (result != 0) {
            return result;
        }
        if (type1 == ElementType.NUMERICALLY_INDEXED && type2 == ElementType.NUMERICALLY_INDEXED) {
            long v1 = Long.parseLong(e1);
            long v2 = Long.parseLong(e2);
            return Long.compare(v1, v2);
        }
        return e1.compareTo(e2);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        ConfigurationPropertyName other = (ConfigurationPropertyName) obj;
        if (getNumberOfElements() != other.getNumberOfElements()) {
            return false;
        }
        if (this.elements.canShortcutWithSource(ElementType.UNIFORM) && other.elements.canShortcutWithSource(ElementType.UNIFORM)) {
            return toString().equals(other.toString());
        }
        return elementsEqual(other);
    }

    private boolean elementsEqual(ConfigurationPropertyName name) {
        for (int i = this.elements.getSize() - 1; i >= 0; i--) {
            if (elementDiffers(this.elements, name.elements, i)) {
                return false;
            }
        }
        return true;
    }

    private boolean elementDiffers(Elements e1, Elements e2, int i) {
        ElementType type1 = e1.getType(i);
        ElementType type2 = e2.getType(i);
        if (type1.allowsFastEqualityCheck() && type2.allowsFastEqualityCheck()) {
            return !fastElementEquals(e1, e2, i);
        } else if (type1.allowsDashIgnoringEqualityCheck() && type2.allowsDashIgnoringEqualityCheck()) {
            return !dashIgnoringElementEquals(e1, e2, i);
        } else {
            return !defaultElementEquals(e1, e2, i);
        }
    }

    private boolean defaultElementEquals(Elements e1, Elements e2, int i) {
        int l1 = e1.getLength(i);
        int l2 = e2.getLength(i);
        boolean indexed1 = e1.getType(i).isIndexed();
        boolean indexed2 = e2.getType(i).isIndexed();
        int i1 = 0;
        int i2 = 0;
        while (i1 < l1) {
            if (i2 >= l2) {
                return false;
            }
            char ch1 = indexed1 ? e1.charAt(i, i1) : Character.toLowerCase(e1.charAt(i, i1));
            char ch2 = indexed2 ? e2.charAt(i, i2) : Character.toLowerCase(e2.charAt(i, i2));
            if (!indexed1 && !ElementsParser.isAlphaNumeric(ch1)) {
                i1++;
            } else if (!indexed2 && !ElementsParser.isAlphaNumeric(ch2)) {
                i2++;
            } else if (ch1 != ch2) {
                return false;
            } else {
                i1++;
                i2++;
            }
        }
        while (i2 < l2) {
            char ch2 = Character.toLowerCase(e2.charAt(i, i2++));
            if (indexed2 || ElementsParser.isAlphaNumeric(ch2)) {
                return false;
            }
        }
        return true;
    }

    private boolean dashIgnoringElementEquals(Elements e1, Elements e2, int i) {
        int l1 = e1.getLength(i);
        int l2 = e2.getLength(i);
        int i1 = 0;
        int i2 = 0;
        while (i1 < l1) {
            if (i2 >= l2) {
                return false;
            }
            char ch1 = e1.charAt(i, i1);
            char ch2 = e2.charAt(i, i2);
            if (ch1 == '-') {
                i1++;
            } else if (ch2 == '-') {
                i2++;
            } else if (ch1 != ch2) {
                return false;
            } else {
                i1++;
                i2++;
            }
        }
        boolean indexed2 = e2.getType(i).isIndexed();
        while (i2 < l2) {
            char ch2 = e2.charAt(i, i2++);
            if (indexed2 || ch2 != '-') {
                return false;
            }
        }
        return true;
    }

    private boolean fastElementEquals(Elements e1, Elements e2, int i) {
        int length1 = e1.getLength(i);
        int length2 = e2.getLength(i);
        if (length1 == length2) {
            int i1 = 0;
            while (length1-- != 0) {
                char ch1 = e1.charAt(i, i1);
                char ch2 = e2.charAt(i, i1);
                if (ch1 != ch2) {
                    return false;
                }
                i1++;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        if (this.string == null) {
            this.string = buildToString();
        }
        return this.string;
    }

    private String buildToString() {
        if (this.elements.canShortcutWithSource(ElementType.UNIFORM, ElementType.DASHED)) {
            return this.elements.getSource().toString();
        }
        int elements = getNumberOfElements();
        StringBuilder result = new StringBuilder(elements * 8);
        for (int i = 0; i < elements; i++) {
            boolean indexed = isIndexed(i);
            if (result.length() > 0 && !indexed) {
                result.append('.');
            }
            if (indexed) {
                result.append('[');
                result.append(getElement(i, Form.ORIGINAL));
                result.append(']');
            } else {
                result.append(getElement(i, Form.DASHED));
            }
        }
        return result.toString();
    }

    public static boolean isValid(CharSequence name) {
        return of(name, true) != null;
    }

    public static ConfigurationPropertyName of(CharSequence name) {
        return of(name, false);
    }

    static ConfigurationPropertyName of(CharSequence name, boolean returnNullIfInvalid) {
        Elements elements = elementsOf(name, returnNullIfInvalid);
        return (elements != null) ? new ConfigurationPropertyName(elements) : null;
    }

    private static Elements probablySingleElementOf(CharSequence name) {
        return elementsOf(name, false, 1);
    }

    private static Elements elementsOf(CharSequence name, boolean returnNullIfInvalid) {
        return elementsOf(name, returnNullIfInvalid, ElementsParser.DEFAULT_CAPACITY);
    }

    private static Elements elementsOf(CharSequence name, boolean returnNullIfInvalid, int parserCapacity) {
        if (name == null) {
            Assert.isTrue(returnNullIfInvalid, "Name must not be null");
            return null;
        }
        if (name.length() == 0) {
            return Elements.EMPTY;
        }
        if (name.charAt(0) == '.' || name.charAt(name.length() - 1) == '.') {
            if (returnNullIfInvalid) {
                return null;
            }
            throw new InvalidConfigurationPropertyNameException(name, Collections.singletonList('.'));
        }
        Elements elements = new ElementsParser(name, '.', parserCapacity).parse();
        for (int i = 0; i < elements.getSize(); i++) {
            if (elements.getType(i) == ElementType.NON_UNIFORM) {
                if (returnNullIfInvalid) {
                    return null;
                }
                throw new InvalidConfigurationPropertyNameException(name, getInvalidChars(elements, i));
            }
        }
        return elements;
    }

    private static List<Character> getInvalidChars(Elements elements, int index) {
        List<Character> invalidChars = new ArrayList<>();
        for (int charIndex = 0; charIndex < elements.getLength(index); charIndex++) {
            char ch = elements.charAt(index, charIndex);
            if (!ElementsParser.isValidChar(ch, charIndex)) {
                invalidChars.add(ch);
            }
        }
        return invalidChars;
    }

    static ConfigurationPropertyName adapt(CharSequence name, char separator) {
        return adapt(name, separator, null);
    }

    static ConfigurationPropertyName adapt(CharSequence name, char separator, Function<CharSequence, CharSequence> elementValueProcessor) {
        Assert.notNull(name, "Name must not be null");
        if (name.length() == 0) {
            return EMPTY;
        }
        Elements elements = new ElementsParser(name, separator).parse(elementValueProcessor);
        if (elements.getSize() == 0) {
            return EMPTY;
        }
        return new ConfigurationPropertyName(elements);
    }

    public enum Form {

        ORIGINAL,

        DASHED,

        UNIFORM

    }

    private static class Elements {

        private static final int[] NO_POSITION = {};

        private static final ElementType[] NO_TYPE = {};

        public static final Elements EMPTY = new Elements("", 0, NO_POSITION, NO_POSITION, NO_TYPE, null);

        private final CharSequence source;

        private final int size;

        private final int[] start;

        private final int[] end;

        private final ElementType[] type;

        private final CharSequence[] resolved;

        Elements(CharSequence source, int size, int[] start, int[] end, ElementType[] type, CharSequence[] resolved) {
            super();
            this.source = source;
            this.size = size;
            this.start = start;
            this.end = end;
            this.type = type;
            this.resolved = resolved;
        }

        public Elements append(Elements additional) {
            Assert.isTrue(additional.getSize() == 1, () -> "Element value '" + additional.getSource() + "' must be a single item");
            ElementType[] type = new ElementType[this.size + 1];
            System.arraycopy(this.type, 0, type, 0, this.size);
            type[this.size] = additional.type[0];
            CharSequence[] resolved = newResolved(this.size + 1);
            resolved[this.size] = additional.get(0);
            return new Elements(this.source, this.size + 1, this.start, this.end, type, resolved);
        }

        public Elements chop(int size) {
            CharSequence[] resolved = newResolved(size);
            return new Elements(this.source, size, this.start, this.end, this.type, resolved);
        }

        private CharSequence[] newResolved(int size) {
            CharSequence[] resolved = new CharSequence[size];
            if (this.resolved != null) {
                System.arraycopy(this.resolved, 0, resolved, 0, Math.min(size, this.size));
            }
            return resolved;
        }

        public int getSize() {
            return this.size;
        }

        public CharSequence get(int index) {
            if (this.resolved != null && this.resolved[index] != null) {
                return this.resolved[index];
            }
            int start = this.start[index];
            int end = this.end[index];
            return this.source.subSequence(start, end);
        }

        public int getLength(int index) {
            if (this.resolved != null && this.resolved[index] != null) {
                return this.resolved[index].length();
            }
            int start = this.start[index];
            int end = this.end[index];
            return end - start;
        }

        public char charAt(int index, int charIndex) {
            if (this.resolved != null && this.resolved[index] != null) {
                return this.resolved[index].charAt(charIndex);
            }
            int start = this.start[index];
            return this.source.charAt(start + charIndex);
        }

        public ElementType getType(int index) {
            return this.type[index];
        }

        public CharSequence getSource() {
            return this.source;
        }

        public boolean canShortcutWithSource(ElementType requiredType) {
            return canShortcutWithSource(requiredType, requiredType);
        }

        public boolean canShortcutWithSource(ElementType requiredType, ElementType alternativeType) {
            if (this.resolved != null) {
                return false;
            }
            for (int i = 0; i < this.size; i++) {
                ElementType type = this.type[i];
                if (type != requiredType && type != alternativeType) {
                    return false;
                }
                if (i > 0 && this.end[i - 1] + 1 != this.start[i]) {
                    return false;
                }
            }
            return true;
        }

    }

    private static class ElementsParser {

        private static final int DEFAULT_CAPACITY = 6;

        private final CharSequence source;

        private final char separator;

        private int size;

        private int[] start;

        private int[] end;

        private ElementType[] type;

        private CharSequence[] resolved;

        ElementsParser(CharSequence source, char separator) {
            this(source, separator, DEFAULT_CAPACITY);
        }

        ElementsParser(CharSequence source, char separator, int capacity) {
            this.source = source;
            this.separator = separator;
            this.start = new int[capacity];
            this.end = new int[capacity];
            this.type = new ElementType[capacity];
        }

        public Elements parse() {
            return parse(null);
        }

        public Elements parse(Function<CharSequence, CharSequence> valueProcessor) {
            int length = this.source.length();
            int openBracketCount = 0;
            int start = 0;
            ElementType type = ElementType.EMPTY;
            for (int i = 0; i < length; i++) {
                char ch = this.source.charAt(i);
                if (ch == '[') {
                    if (openBracketCount == 0) {
                        add(start, i, type, valueProcessor);
                        start = i + 1;
                        type = ElementType.NUMERICALLY_INDEXED;
                    }
                    openBracketCount++;
                } else if (ch == ']') {
                    openBracketCount--;
                    if (openBracketCount == 0) {
                        add(start, i, type, valueProcessor);
                        start = i + 1;
                        type = ElementType.EMPTY;
                    }
                } else if (!type.isIndexed() && ch == this.separator) {
                    add(start, i, type, valueProcessor);
                    start = i + 1;
                    type = ElementType.EMPTY;
                } else {
                    type = updateType(type, ch, i - start);
                }
            }
            if (openBracketCount != 0) {
                type = ElementType.NON_UNIFORM;
            }
            add(start, length, type, valueProcessor);
            return new Elements(this.source, this.size, this.start, this.end, this.type, this.resolved);
        }

        private ElementType updateType(ElementType existingType, char ch, int index) {
            if (existingType.isIndexed()) {
                if (existingType == ElementType.NUMERICALLY_INDEXED && !isNumeric(ch)) {
                    return ElementType.INDEXED;
                }
                return existingType;
            }
            if (existingType == ElementType.EMPTY && isValidChar(ch, index)) {
                return (index == 0) ? ElementType.UNIFORM : ElementType.NON_UNIFORM;
            }
            if (existingType == ElementType.UNIFORM && ch == '-') {
                return ElementType.DASHED;
            }
            if (!isValidChar(ch, index)) {
                if (existingType == ElementType.EMPTY && !isValidChar(Character.toLowerCase(ch), index)) {
                    return ElementType.EMPTY;
                }
                return ElementType.NON_UNIFORM;
            }
            return existingType;
        }

        private void add(int start, int end, ElementType type, Function<CharSequence, CharSequence> valueProcessor) {
            if ((end - start) < 1 || type == ElementType.EMPTY) {
                return;
            }
            if (this.start.length == this.size) {
                this.start = expand(this.start);
                this.end = expand(this.end);
                this.type = expand(this.type);
                this.resolved = expand(this.resolved);
            }
            if (valueProcessor != null) {
                if (this.resolved == null) {
                    this.resolved = new CharSequence[this.start.length];
                }
                CharSequence resolved = valueProcessor.apply(this.source.subSequence(start, end));
                Elements resolvedElements = new ElementsParser(resolved, '.').parse();
                Assert.state(resolvedElements.getSize() == 1, "Resolved element must not contain multiple elements");
                this.resolved[this.size] = resolvedElements.get(0);
                type = resolvedElements.getType(0);
            }
            this.start[this.size] = start;
            this.end[this.size] = end;
            this.type[this.size] = type;
            this.size++;
        }

        private int[] expand(int[] src) {
            int[] dest = new int[src.length + DEFAULT_CAPACITY];
            System.arraycopy(src, 0, dest, 0, src.length);
            return dest;
        }

        private ElementType[] expand(ElementType[] src) {
            ElementType[] dest = new ElementType[src.length + DEFAULT_CAPACITY];
            System.arraycopy(src, 0, dest, 0, src.length);
            return dest;
        }

        private CharSequence[] expand(CharSequence[] src) {
            if (src == null) {
                return null;
            }
            CharSequence[] dest = new CharSequence[src.length + DEFAULT_CAPACITY];
            System.arraycopy(src, 0, dest, 0, src.length);
            return dest;
        }

        public static boolean isValidChar(char ch, int index) {
            return isAlpha(ch) || isNumeric(ch) || (index != 0 && ch == '-');
        }

        public static boolean isAlphaNumeric(char ch) {
            return isAlpha(ch) || isNumeric(ch);
        }

        private static boolean isAlpha(char ch) {
            return ch >= 'a' && ch <= 'z';
        }

        private static boolean isNumeric(char ch) {
            return ch >= '0' && ch <= '9';
        }

    }

    private enum ElementType {

        EMPTY(false),

        UNIFORM(false),

        DASHED(false),

        NON_UNIFORM(false),

        INDEXED(true),

        NUMERICALLY_INDEXED(true);

        private final boolean indexed;

        ElementType(boolean indexed) {
            this.indexed = indexed;
        }

        public boolean isIndexed() {
            return this.indexed;
        }

        public boolean allowsFastEqualityCheck() {
            return this == UNIFORM || this == NUMERICALLY_INDEXED;
        }

        public boolean allowsDashIgnoringEqualityCheck() {
            return allowsFastEqualityCheck() || this == DASHED;
        }

    }

    private interface ElementCharPredicate {

        boolean test(char ch, int index);

    }

}
