package org.apache.dubbo.remoting.telnet.support;

import java.util.Arrays;
import java.util.List;

public class TelnetUtils {

    public static String toList(List<List<String>> table) {
        int[] widths = new int[table.get(0).size()];
        for (int j = 0; j < widths.length; j++) {
            for (List<String> row : table) {
                widths[j] = Math.max(widths[j], row.get(j).length());
            }
        }
        StringBuilder buf = new StringBuilder();
        for (List<String> row : table) {
            if (buf.length() > 0) {
                buf.append("\r\n");
            }
            for (int j = 0; j < widths.length; j++) {
                if (j > 0) {
                    buf.append(" - ");
                }
                String value = row.get(j);
                buf.append(value);
                if (j < widths.length - 1) {
                    int pad = widths[j] - value.length();
                    if (pad > 0) {
                        for (int k = 0; k < pad; k++) {
                            buf.append(" ");
                        }
                    }
                }
            }
        }
        return buf.toString();
    }

    public static String toTable(String[] header, List<List<String>> table) {
        return toTable(Arrays.asList(header), table);
    }

    public static String toTable(List<String> header, List<List<String>> table) {
        int totalWidth = 0;
        int[] widths = new int[header.size()];
        int maxwidth = 70;
        int maxcountbefore = 0;
        for (int j = 0; j < widths.length; j++) {
            widths[j] = Math.max(widths[j], header.get(j).length());
        }
        for (List<String> row : table) {
            int countbefore = 0;
            for (int j = 0; j < widths.length; j++) {
                widths[j] = Math.max(widths[j], row.get(j).length());
                totalWidth = (totalWidth + widths[j]) > maxwidth ? maxwidth : (totalWidth + widths[j]);
                if (j < widths.length - 1) {
                    countbefore = countbefore + widths[j];
                }
            }
            maxcountbefore = Math.max(countbefore, maxcountbefore);
        }
        widths[widths.length - 1] = Math.min(widths[widths.length - 1], maxwidth - maxcountbefore);
        StringBuilder buf = new StringBuilder();
        buf.append("+");
        for (int j = 0; j < widths.length; j++) {
            for (int k = 0; k < widths[j] + 2; k++) {
                buf.append("-");
            }
            buf.append("+");
        }
        buf.append("\r\n");
        buf.append("|");
        for (int j = 0; j < widths.length; j++) {
            String cell = header.get(j);
            buf.append(" ");
            buf.append(cell);
            int pad = widths[j] - cell.length();
            if (pad > 0) {
                for (int k = 0; k < pad; k++) {
                    buf.append(" ");
                }
            }
            buf.append(" |");
        }
        buf.append("\r\n");
        buf.append("+");
        for (int j = 0; j < widths.length; j++) {
            for (int k = 0; k < widths[j] + 2; k++) {
                buf.append("-");
            }
            buf.append("+");
        }
        buf.append("\r\n");
        for (List<String> row : table) {
            StringBuffer rowbuf = new StringBuffer();
            rowbuf.append("|");
            for (int j = 0; j < widths.length; j++) {
                String cell = row.get(j);
                rowbuf.append(" ");
                int remaing = cell.length();
                while (remaing > 0) {
                    if (rowbuf.length() >= totalWidth) {
                        buf.append(rowbuf.toString());
                        rowbuf = new StringBuffer();

                    }
                    rowbuf.append(cell.substring(cell.length() - remaing, cell.length() - remaing + 1));
                    remaing--;
                }
                int pad = widths[j] - cell.length();
                if (pad > 0) {
                    for (int k = 0; k < pad; k++) {
                        rowbuf.append(" ");
                    }
                }
                rowbuf.append(" |");
            }
            buf.append(rowbuf).append("\r\n");
        }
        buf.append("+");
        for (int j = 0; j < widths.length; j++) {
            for (int k = 0; k < widths[j] + 2; k++) {
                buf.append("-");
            }
            buf.append("+");
        }
        buf.append("\r\n");
        return buf.toString();
    }

}