package org.springframework.web.util.pattern;

class SubSequence implements CharSequence {

    private final char[] chars;

    private final int start;

    private final int end;

    SubSequence(char[] chars, int start, int end) {
        this.chars = chars;
        this.start = start;
        this.end = end;
    }

    @Override
    public int length() {
        return (this.end - this.start);
    }

    @Override
    public char charAt(int index) {
        return this.chars[this.start + index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new SubSequence(this.chars, this.start + start, this.start + end);
    }

    @Override
    public String toString() {
        return new String(this.chars, this.start, this.end - this.start);
    }

}
