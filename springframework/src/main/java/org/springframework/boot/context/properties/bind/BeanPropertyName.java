package org.springframework.boot.context.properties.bind;

abstract class BeanPropertyName {

    private BeanPropertyName() {
    }

    public static String toDashedForm(String name) {
        return toDashedForm(name, 0);
    }

    public static String toDashedForm(String name, int start) {
        StringBuilder result = new StringBuilder();
        String replaced = name.replace('_', '-');
        for (int i = start; i < replaced.length(); i++) {
            char ch = replaced.charAt(i);
            if (Character.isUpperCase(ch) && result.length() > 0 && result.charAt(result.length() - 1) != '-') {
                result.append('-');
            }
            result.append(Character.toLowerCase(ch));
        }
        return result.toString();
    }

}
