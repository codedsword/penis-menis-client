package dev.sinepsinem.util.imgui;

import dev.sinepsinem.SinepSinem;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * imgui-java only publishes x86_64 natives for linux, which cannot load on Android launchers
 * (MojoLauncher, PojavLauncher). We bundle our own arm64 Android build (scripts/build-imgui-android.sh)
 * and point imgui-java's loader at it before the ImGui class initializes.
 */
public final class ImGuiNatives {
    private static final String ANDROID_ARM64 = "/natives/android-arm64/libimgui-java64.so";
    private static final String LIB_PATH_PROP = "imgui.library.path";

    private ImGuiNatives() {
    }

    public static void prepare() {
        if (System.getProperty(LIB_PATH_PROP) != null || !isAndroid() || !isArm64()) return;

        try (InputStream in = ImGuiNatives.class.getResourceAsStream(ANDROID_ARM64)) {
            if (in == null) {
                SinepSinem.LOGGER.warn("No bundled ImGui natives for Android arm64");
                return;
            }
            byte[] bytes = in.readAllBytes();
            // keyed by content so an updated library never collides with an old extraction
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).substring(0, 16);
            Path dir = Paths.get(System.getProperty("java.io.tmpdir")).resolve("sinepsinem-natives").resolve(hash);
            Path lib = dir.resolve("libimgui-java64.so");
            if (!Files.exists(lib) || Files.size(lib) != bytes.length) {
                Files.createDirectories(dir);
                Path tmp = Files.createTempFile(dir, "imgui", ".so");
                Files.write(tmp, bytes);
                Files.move(tmp, lib, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            System.setProperty(LIB_PATH_PROP, dir.toAbsolutePath().toString());
            SinepSinem.LOGGER.info("Using bundled Android arm64 ImGui natives from {}", dir);
        } catch (Exception e) {
            SinepSinem.LOGGER.error("Failed to extract Android ImGui natives", e);
        }
    }

    private static boolean isAndroid() {
        return System.getenv("ANDROID_ROOT") != null
                || System.getenv("ANDROID_DATA") != null
                || Files.exists(Paths.get("/system/build.prop"))
                || Files.exists(Paths.get("/system/bin/linker64"));
    }

    private static boolean isArm64() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return arch.equals("aarch64") || arch.equals("arm64");
    }
}
