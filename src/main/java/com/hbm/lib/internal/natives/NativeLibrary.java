// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal.natives;

import java.util.Locale;

/**
 * Backport stub. NTM: NEXT's FFM loader is not part of this build:
 * Java 21 only has FFM as a preview API, so the native library
 * is never available and every caller takes its Java path.
 */
public final class NativeLibrary {

    private static final String RESOURCE_DIR = "ntm_natives/";

    public static final String LIB_BASE_NAME = "ntm_next_native";

    public static final String NORMALIZED_ARCH = normalizeArch(System.getProperty("os.arch", ""));
    public static final String NORMALIZED_OS = normalizeOs(System.getProperty("os.name", ""));

    public static final int ABI_VERSION = -1;

    public static final int ACTIVE_PATH = -1;

    public static final boolean AVAILABLE = false;

    private NativeLibrary() {}

    public static String artifactName(String baseName) {
        return String.format("%s-%s-%s", NORMALIZED_OS, NORMALIZED_ARCH, System.mapLibraryName(baseName));
    }

    public static String resourcePath(String baseName) {
        return RESOURCE_DIR + artifactName(baseName);
    }

    public static boolean artifactPresent() {
        return false;
    }

    public static String availability() {
        return "unavailable (disabled in the 1.21.1 backport: FFM is preview-only on Java 21)";
    }



    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }


    private static String normalizeArch(String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) return "x86_64";
        if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) return "x86_32";
        if ("aarch64".equals(value)) return "aarch_64";
        if (value.matches("^(arm|arm32)$")) return "arm_32";
        if ("ppc64le".equals(value)) return "ppcle_64";
        if ("ppc64".equals(value)) return "ppc_64";
        if ("s390x".equals(value)) return "s390_64";
        if ("loongarch64".equals(value)) return "loongarch_64";
        return "unknown";
    }


    private static String normalizeOs(String value) {
        value = normalize(value);
        if (value.startsWith("linux")) return "linux";
        if (value.startsWith("macosx") || value.startsWith("osx") || value.startsWith("darwin"))
            return "osx";
        if (value.startsWith("windows")) return "windows";
        if (value.startsWith("freebsd")) return "freebsd";
        if (value.startsWith("openbsd")) return "openbsd";
        if (value.startsWith("netbsd")) return "netbsd";
        if (value.startsWith("solaris") || value.startsWith("sunos")) return "sunos";
        if (value.startsWith("aix")) return "aix";
        return "unknown";
    }
}
