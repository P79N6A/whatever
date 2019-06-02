package io.netty.resolver;

import io.netty.util.NetUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.UnstableApi;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.*;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

@UnstableApi
public final class HostsFileParser {

    private static final String WINDOWS_DEFAULT_SYSTEM_ROOT = "C:\\Windows";
    private static final String WINDOWS_HOSTS_FILE_RELATIVE_PATH = "\\system32\\drivers\\etc\\hosts";
    private static final String X_PLATFORMS_HOSTS_FILE_PATH = "/etc/hosts";

    private static final Pattern WHITESPACES = Pattern.compile("[ \t]+");

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HostsFileParser.class);

    private HostsFileParser() {
    }

    private static File locateHostsFile() {
        File hostsFile;
        if (PlatformDependent.isWindows()) {
            hostsFile = new File(System.getenv("SystemRoot") + WINDOWS_HOSTS_FILE_RELATIVE_PATH);
            if (!hostsFile.exists()) {
                hostsFile = new File(WINDOWS_DEFAULT_SYSTEM_ROOT + WINDOWS_HOSTS_FILE_RELATIVE_PATH);
            }
        } else {
            hostsFile = new File(X_PLATFORMS_HOSTS_FILE_PATH);
        }
        return hostsFile;
    }

    public static HostsFileEntries parseSilently() {
        return parseSilently(Charset.defaultCharset());
    }

    public static HostsFileEntries parseSilently(Charset... charsets) {
        File hostsFile = locateHostsFile();
        try {
            return parse(hostsFile, charsets);
        } catch (IOException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to load and parse hosts file at " + hostsFile.getPath(), e);
            }
            return HostsFileEntries.EMPTY;
        }
    }

    public static HostsFileEntries parse() throws IOException {
        return parse(locateHostsFile());
    }

    public static HostsFileEntries parse(File file) throws IOException {
        return parse(file, Charset.defaultCharset());
    }

    public static HostsFileEntries parse(File file, Charset... charsets) throws IOException {
        checkNotNull(file, "file");
        checkNotNull(charsets, "charsets");
        if (file.exists() && file.isFile()) {
            for (Charset charset : charsets) {
                HostsFileEntries entries = parse(new BufferedReader(new InputStreamReader(new FileInputStream(file), charset)));
                if (entries != HostsFileEntries.EMPTY) {
                    return entries;
                }
            }
        }
        return HostsFileEntries.EMPTY;
    }

    public static HostsFileEntries parse(Reader reader) throws IOException {
        checkNotNull(reader, "reader");
        BufferedReader buff = new BufferedReader(reader);
        try {
            Map<String, Inet4Address> ipv4Entries = new HashMap<String, Inet4Address>();
            Map<String, Inet6Address> ipv6Entries = new HashMap<String, Inet6Address>();
            String line;
            while ((line = buff.readLine()) != null) {

                int commentPosition = line.indexOf('#');
                if (commentPosition != -1) {
                    line = line.substring(0, commentPosition);
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                List<String> lineParts = new ArrayList<String>();
                for (String s : WHITESPACES.split(line)) {
                    if (!s.isEmpty()) {
                        lineParts.add(s);
                    }
                }

                if (lineParts.size() < 2) {

                    continue;
                }

                byte[] ipBytes = NetUtil.createByteArrayFromIpAddressString(lineParts.get(0));

                if (ipBytes == null) {

                    continue;
                }

                for (int i = 1; i < lineParts.size(); i++) {
                    String hostname = lineParts.get(i);
                    String hostnameLower = hostname.toLowerCase(Locale.ENGLISH);
                    InetAddress address = InetAddress.getByAddress(hostname, ipBytes);
                    if (address instanceof Inet4Address) {
                        Inet4Address previous = ipv4Entries.put(hostnameLower, (Inet4Address) address);
                        if (previous != null) {

                            ipv4Entries.put(hostnameLower, previous);
                        }
                    } else {
                        Inet6Address previous = ipv6Entries.put(hostnameLower, (Inet6Address) address);
                        if (previous != null) {

                            ipv6Entries.put(hostnameLower, previous);
                        }
                    }
                }
            }
            return ipv4Entries.isEmpty() && ipv6Entries.isEmpty() ? HostsFileEntries.EMPTY : new HostsFileEntries(ipv4Entries, ipv6Entries);
        } finally {
            try {
                buff.close();
            } catch (IOException e) {
                logger.warn("Failed to close a reader", e);
            }
        }
    }
}
