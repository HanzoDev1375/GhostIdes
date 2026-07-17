package ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
/**
 * فرمت‌کننده HTML با استفاده از Prettier (Node.js) در محیط Debian نیاز به نصب: apt install nodejs
 * npm -y && npm install -g prettier
 *
 * <p>توجه: Prettier برخلاف tidy، HTML ناقص را قبول نمی‌کند. بهتر است ابتدا با tidy ساختار را درست
 * کنید، سپس با Prettier زیباسازی نهایی را انجام دهید.
 */
public class PrettierFormatter {
    private static final String TAG = "HtmlPrettierFormatter";

    public String format(Context context, String code, String formatcodeType) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        File rootfs = DebianBootstrap.getRootfsDir(context);
        if (!rootfs.exists() || !rootfs.isDirectory()) {
            Log.e(TAG, "rootfs دبیان پیدا نشد");
            return null;
        }
        String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
        File prootBinary = new File(nativeLibDir, "libproot.so");
        if (!prootBinary.exists()) {
            Log.e(TAG, "libproot.so پیدا نشد");
            return null;
        }
        File loaderBinary = new File(nativeLibDir, "libloader.so");
        if (!loaderBinary.exists()) {
            Log.e(TAG, "libloader.so پیدا نشد");
            return null;
        }
        File cacheDir = context.getCacheDir();
        String tempFileName = "temp_" + formatcodeType + "_prettier." + formatcodeType;
        File tempFile = new File(cacheDir, tempFileName);
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(code);
        } catch (Exception e) {
            Log.e(TAG, "خطا در نوشتن فایل موقت", e);
            return null;
        }
        String dataDir = context.getFilesDir().getParentFile().getAbsolutePath();
        String tempFilePathInProot = dataDir + "/cache/" + tempFileName;
        List<String> command = new ArrayList<>();
        command.add(prootBinary.getAbsolutePath());
        command.add("--kill-on-exit");
        command.add("-0");
        command.add("--link2symlink");
        command.add("-r");
        command.add(rootfs.getAbsolutePath());
        command.add("-b");
        command.add("/dev");
        command.add("-b");
        command.add("/proc");
        command.add("-b");
        command.add("/sys");
        command.add("-b");
        command.add(dataDir);
        command.add("-w");
        command.add("/root");
        command.add("/usr/local/bin/prettier");
        command.add("--write");
        command.add(tempFilePathInProot);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.environment().clear();
        pb.environment().put("PROOT_TMP_DIR", context.getCacheDir().getAbsolutePath() + "/proot-tmp");
        pb.environment().put("PROOT_LOADER", loaderBinary.getAbsolutePath());
        pb.environment().put("LD_LIBRARY_PATH", nativeLibDir);
        pb.environment().put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin");
        try {
            Log.d(TAG, "دستور: " + String.join(" ", command));
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            int exitCode = process.waitFor();
            Log.d(TAG, "Exit code: " + exitCode);
            Log.d(TAG, "خروجی Prettier: " + output.toString());
            if (exitCode == 0) {
                StringBuilder formatted = new StringBuilder();
                try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(tempFile), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = fileReader.readLine()) != null) {
                        if (formatted.length() > 0) formatted.append('\n');
                        formatted.append(line);
                    }
                }
                return formatted.toString();
            } else {
                Log.e(TAG, "Prettier با خطا مواجه شد. Exit code: " + exitCode);
                Log.e(TAG, "خروجی: " + output);
                return code;
            }
        } catch (Exception e) {
            Log.e(TAG, "خطا در اجرای proot/prettier", e);
            return null;
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }
}