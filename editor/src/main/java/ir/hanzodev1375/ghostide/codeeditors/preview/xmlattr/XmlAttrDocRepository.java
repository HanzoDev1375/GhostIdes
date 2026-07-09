package ir.hanzodev1375.ghostide.codeeditors.preview.xmlattr;

import android.content.Context;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class XmlAttrDocRepository {

  private static volatile XmlAttrDocRepository instance;
  private final Map<String, XmlAttrDoc> attributeToDoc;

  private XmlAttrDocRepository(Context context) {
    attributeToDoc = loadDocs(context);
  }

  public static XmlAttrDocRepository getInstance(Context context) {
    if (instance == null) {
      synchronized (XmlAttrDocRepository.class) {
        if (instance == null) {
          instance = new XmlAttrDocRepository(context.getApplicationContext());
        }
      }
    }
    return instance;
  }

  private Map<String, XmlAttrDoc> loadDocs(Context context) {
    try (InputStreamReader reader =
        new InputStreamReader(context.getAssets().open("important.json"))) {
      ImportantXmlFile file = new Gson().fromJson(reader, ImportantXmlFile.class);
      Map<String, XmlAttrDoc> map = new HashMap<>();
      if (file != null && file.importantXMLs != null) {
        for (XmlAttrDoc doc : file.importantXMLs) {
          if (doc.attribute != null) {
            // Some attributes (e.g. android:gravity) repeat for different widgets;
            // the first (most general) definition wins.
            map.putIfAbsent(doc.attribute, doc);
          }
        }
      }
      return map;
    } catch (Exception e) {
      e.printStackTrace();
      return Collections.emptyMap();
    }
  }

  public XmlAttrDoc getDoc(String attribute) {
    return attributeToDoc.get(attribute);
  }

  private static final class ImportantXmlFile {
    List<XmlAttrDoc> importantXMLs;
  }
}
