package ir.theme.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ThemeFileUtil {

  private ThemeFileUtil() {}

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
