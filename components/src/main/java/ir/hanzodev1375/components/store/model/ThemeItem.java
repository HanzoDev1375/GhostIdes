package ir.hanzodev1375.components.store.model;

import java.util.ArrayList;
import java.util.List;

public record ThemeItem(
    String name,
    String image1,
    String image2,
    String image3,
    String icon,
    String doc,
    int version,
    String devname,
    String linkdownload) {

  public List<String> images(String base) {
    List<String> out = new ArrayList<>(3);
    out.add(url(image1, base));
    out.add(url(image2, base));
    out.add(url(image3, base));
    return out;
  }

  public String iconUrl(String base) {
    return url(icon, base);
  }

  private static String url(String path, String base) {
    if (path == null || path.isEmpty()) return null;
    return path.startsWith("http") ? path : base + path;
  }
}
