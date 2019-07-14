package org.apache.dubbo.common;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Version {

    private static final Logger logger = LoggerFactory.getLogger(Version.class);

    private static final Pattern PREFIX_DIGITS_PATTERN = Pattern.compile("^([0-9]*).*");

    public static final String DEFAULT_DUBBO_PROTOCOL_VERSION = "2.0.2";

    private static final String VERSION = getVersion(Version.class, "");

    private static final int LOWEST_VERSION_FOR_RESPONSE_ATTACHMENT = 2000200;

    private static final Map<String, Integer> VERSION2INT = new HashMap<String, Integer>();

    static {
        Version.checkDuplicate(Version.class);
    }

    private Version() {
    }

    public static String getProtocolVersion() {
        return DEFAULT_DUBBO_PROTOCOL_VERSION;
    }

    public static String getVersion() {
        return VERSION;
    }

    public static boolean isRelease270OrHigher(String version) {
        if (StringUtils.isEmpty(version)) {
            return false;
        }
        if (getIntVersion(version) >= 2070000) {
            return true;
        }
        return false;
    }

    public static boolean isRelease263OrHigher(String version) {
        return isSupportResponseAttachment(version);
    }

    public static boolean isSupportResponseAttachment(String version) {
        if (StringUtils.isEmpty(version)) {
            return false;
        }
        int iVersion = getIntVersion(version);
        if (iVersion >= 2001000 && iVersion <= 2060200) {
            return false;
        }
        if (iVersion >= 2080000 && iVersion < 2090000) {
            return false;
        }
        return iVersion >= LOWEST_VERSION_FOR_RESPONSE_ATTACHMENT;
    }

    public static int getIntVersion(String version) {
        Integer v = VERSION2INT.get(version);
        if (v == null) {
            v = parseInt(version);
            if (version.split("\\.").length == 3) {
                v = v * 100;
            }
            VERSION2INT.put(version, v);
        }
        return v;
    }

    private static int parseInt(String version) {
        int v = 0;
        String[] vArr = version.split("\\.");
        int len = vArr.length;
        for (int i = 0; i < len; i++) {
            v += Integer.parseInt(getPrefixDigits(vArr[i])) * Math.pow(10, (len - i - 1) * 2);
        }
        return v;
    }

    private static String getPrefixDigits(String v) {
        Matcher matcher = PREFIX_DIGITS_PATTERN.matcher(v);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public static String getVersion(Class<?> cls, String defaultVersion) {
        try {
            Package pkg = cls.getPackage();
            String version = null;
            if (pkg != null) {
                version = pkg.getImplementationVersion();
                if (!StringUtils.isEmpty(version)) {
                    return version;
                }
                version = pkg.getSpecificationVersion();
                if (!StringUtils.isEmpty(version)) {
                    return version;
                }
            }
            CodeSource codeSource = cls.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                logger.info("No codeSource for class " + cls.getName() + " when getVersion, use default version " + defaultVersion);
                return defaultVersion;
            }
            String file = codeSource.getLocation().getFile();
            if (!StringUtils.isEmpty(file) && file.endsWith(".jar")) {
                version = getFromFile(file);
            }
            return StringUtils.isEmpty(version) ? defaultVersion : version;
        } catch (Throwable e) {
            logger.error("return default version, ignore exception " + e.getMessage(), e);
            return defaultVersion;
        }
    }

    private static String getFromFile(String file) {
        file = file.substring(0, file.length() - 4);
        int i = file.lastIndexOf('/');
        if (i >= 0) {
            file = file.substring(i + 1);
        }
        i = file.indexOf("-");
        if (i >= 0) {
            file = file.substring(i + 1);
        }
        while (file.length() > 0 && !Character.isDigit(file.charAt(0))) {
            i = file.indexOf("-");
            if (i >= 0) {
                file = file.substring(i + 1);
            } else {
                break;
            }
        }
        return file;
    }

    public static void checkDuplicate(Class<?> cls, boolean failOnError) {
        checkDuplicate(cls.getName().replace('.', '/') + ".class", failOnError);
    }

    public static void checkDuplicate(Class<?> cls) {
        checkDuplicate(cls, false);
    }

    public static void checkDuplicate(String path, boolean failOnError) {
        try {
            Set<String> files = getResources(path);
            if (files.size() > 1) {
                String error = "Duplicate class " + path + " in " + files.size() + " jar " + files;
                if (failOnError) {
                    throw new IllegalStateException(error);
                } else {
                    logger.error(error);
                }
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static Set<String> getResources(String path) throws IOException {
        Enumeration<URL> urls = ClassUtils.getCallerClassLoader(Version.class).getResources(path);
        Set<String> files = new HashSet<String>();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (url != null) {
                String file = url.getFile();
                if (file != null && file.length() > 0) {
                    files.add(file);
                }
            }
        }
        return files;
    }

}
