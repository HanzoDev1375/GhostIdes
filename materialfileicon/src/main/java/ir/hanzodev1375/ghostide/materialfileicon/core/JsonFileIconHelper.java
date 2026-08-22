package ir.hanzodev1375.ghostide.materialfileicon.core;

import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.json.JSONObject;

public class JsonFileIconHelper {

    public interface ExternalResolver {
        String resolve(String filePath);
    }

    private static final String DATA_ASSET = "data/file_icons.json";
    private static final Object LOCK = new Object();

    private static volatile boolean loaded;
    private static volatile ExternalResolver externalResolver;
    private static String assetDir = "vscode_icons";
    private static JSONObject extensions;
    private static JSONObject filenames;
    private static JSONObject folders;
    private static JSONObject languages;
    private static List<String> extKeysSorted;
    private static String defaultFile = "default_file";
    private static String defaultFolder = "default_folder";

    private final String filePath;
    private transient String resolvedExternal;

    public JsonFileIconHelper(String filePath) {
        this.filePath = filePath == null ? "" : filePath;
    }

    public static void setExternalResolver(ExternalResolver resolver) {
        externalResolver = resolver;
    }

    public static ExternalResolver getExternalResolver() {
        return externalResolver;
    }

    private String resolveExternalOnce() {
        String resolved = resolvedExternal;
        if (resolved != null) return resolved.isEmpty() ? null : resolved;
        ExternalResolver resolver = externalResolver;
        if (resolver != null) {
            try {
                String icon = resolver.resolve(filePath);
                if (icon != null && !icon.trim().isEmpty()) {
                    resolvedExternal = icon;
                    return icon;
                }
            } catch (Throwable ignored) {
            }
        }
        resolvedExternal = "";
        return null;
    }

    public static void load(Context context) {
        if (loaded) return;
        synchronized (LOCK) {
            if (loaded) return;
            try (InputStream is = context.getAssets().open(DATA_ASSET)) {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                apply(new JSONObject(sb.toString()));
            } catch (Exception ignored) {
            }
            loaded = true;
        }
    }

    static void apply(JSONObject root) {
        if (root == null) return;
        assetDir = root.optString("asset_dir", assetDir);
        extensions = root.optJSONObject("extensions");
        filenames = root.optJSONObject("filenames");
        folders = root.optJSONObject("folders");
        languages = root.optJSONObject("languages");
        JSONObject def = root.optJSONObject("defaults");
        if (def != null) {
            defaultFile = def.optString("file", defaultFile);
            defaultFolder = def.optString("folder", defaultFolder);
        }
        extKeysSorted = new ArrayList<>();
        if (extensions != null) {
            for (Iterator<String> it = extensions.keys(); it.hasNext(); ) {
                String k = it.next();
                extKeysSorted.add(k);
            }
            Collections.sort(extKeysSorted, (a, b) -> b.length() - a.length());
        }
    }

    public String getIconName() {
        return getIconName(true);
    }

    public String getIconName(boolean matchFolderNames) {
        String provided = resolveExternalOnce();
        if (provided != null && !provided.contains("://")) return provided;
        File file = new File(filePath);
        if (file.isDirectory()) {
            if (!matchFolderNames) return defaultFolder;
            String key = file.getName().toLowerCase();
            if (folders != null && folders.has(key)) return folders.optString(key);
            return defaultFolder;
        }
        String lower = file.getName().toLowerCase();
        if (!lower.isEmpty()) {
            if (filenames != null && filenames.has(lower)) return filenames.optString(lower);
            if (extKeysSorted != null) {
                int size = extKeysSorted.size();
                for (int i = 0; i < size; i++) {
                    String k = extKeysSorted.get(i);
                    int cut = lower.length() - k.length();
                    if (cut > 0 && lower.charAt(cut - 1) == '.' && lower.endsWith(k))
                        return extensions.optString(k);
                }
            }
        }
        return defaultFile;
    }

    public String getIconPath() {
        return getIconPath(true);
    }

    public String getIconPath(boolean matchFolderNames) {
        return assetDir + "/" + getIconName(matchFolderNames) + ".svg";
    }

    public String getIconUri() {
        return externalUri(true);
    }

    public String getIconUri(boolean matchFolderNames) {
        return externalUri(matchFolderNames);
    }

    private String externalUri(boolean matchFolderNames) {
        String provided = resolveExternalOnce();
        if (provided != null) {
            if (provided.contains("://")) return provided;
            return "file:///android_asset/" + assetDir + "/" + provided + ".svg";
        }
        return "file:///android_asset/" + getIconPath(matchFolderNames);
    }

    public void bindIcon(ImageView imageView) {
        load(imageView.getContext());
        Glide.with(imageView).load(getIconUri()).into(imageView);
    }
}
