package ir.hanzodev1375.ghostide.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {
  public static boolean isExists(String path) {
    return new File(path).exists();
  }

  /**
   * Reads a whole file into a byte array in a way that works on every Android API level.
   *
   * <p>{@link FileInputStream#readAllBytes()} was only added in API 33, so older devices (Android
   * 8–12) crash with {@link NoSuchMethodError}. Always use this helper instead of the newer
   * {@code readAllBytes()} stream methods.
   */
  public static byte[] readBytesCompat(File file) throws IOException {
    try (InputStream in = new FileInputStream(file)) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] chunk = new byte[4096];
      int bytesRead;
      while ((bytesRead = in.read(chunk)) != -1) {
        out.write(chunk, 0, bytesRead);
      }
      return out.toByteArray();
    }
  }
}
