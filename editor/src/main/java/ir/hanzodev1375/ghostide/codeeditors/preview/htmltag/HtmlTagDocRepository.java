package ir.hanzodev1375.ghostide.codeeditors.preview.htmltag;

import android.content.Context;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class HtmlTagDocRepository {

  private static volatile HtmlTagDocRepository instance;
  private final Map<String, HtmlTagDoc> tagToDoc;

  private HtmlTagDocRepository(Context context) {
    tagToDoc = loadDocs(context);
  }

  public static HtmlTagDocRepository getInstance(Context context) {
    if (instance == null) {
      synchronized (HtmlTagDocRepository.class) {
        if (instance == null) {
          instance = new HtmlTagDocRepository(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  private Map<String, HtmlTagDoc> loadDocs(Context context) {
    try (InputStreamReader reader =
        new InputStreamReader(context.getAssets().open("mdn_html.json"))) {
      MdnHtmlFile file = new Gson().fromJson(reader, MdnHtmlFile.class);
      Map<String, HtmlTagDoc> map = new HashMap<>();
      if (file != null && file.tags != null) {
        for (Map.Entry<String, RawTagEntry> entry : file.tags.entrySet()) {
          RawTagEntry raw = entry.getValue();
          if (raw == null || raw.markdown == null) continue;

          HtmlTagDoc doc = new HtmlTagDoc();
          doc.name = raw.name != null ? raw.name : entry.getKey();
          doc.markdown = HtmlMarkdownCleaner.buildRenderableMarkdown(raw.markdown);
          map.put(entry.getKey().toLowerCase(Locale.ROOT), doc);
        }
      }
      return map;
    } catch (Exception e) {
      e.printStackTrace();
      return Collections.emptyMap();
    }
  }

  public HtmlTagDoc getDoc(String tagName) {
    if (tagName == null) return null;
    return tagToDoc.get(tagName.toLowerCase(Locale.ROOT));
  }

  private static final class MdnHtmlFile {
    Map<String, RawTagEntry> tags;
  }

  private static final class RawTagEntry {
    String name;
    String markdown;
  }
}
