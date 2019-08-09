package org.springframework.boot;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public final class SpringBootVersion {

    private SpringBootVersion() {
    }

    public static String getVersion() {
        return determineSpringBootVersion();
    }

    private static String determineSpringBootVersion() {
        String implementationVersion = SpringBootVersion.class.getPackage().getImplementationVersion();
        if (implementationVersion != null) {
            return implementationVersion;
        }
        URL codeSourceLocation = SpringBootVersion.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            URLConnection connection = codeSourceLocation.openConnection();
            if (connection instanceof JarURLConnection) {
                return getImplementationVersion(((JarURLConnection) connection).getJarFile());
            }
            try (JarFile jarFile = new JarFile(new File(codeSourceLocation.toURI()))) {
                return getImplementationVersion(jarFile);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private static String getImplementationVersion(JarFile jarFile) throws IOException {
        return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    }

}
