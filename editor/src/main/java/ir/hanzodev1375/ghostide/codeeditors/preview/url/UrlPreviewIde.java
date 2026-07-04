package ir.hanzodev1375.ghostide.codeeditors.preview.url;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import ir.hanzodev1375.ghostide.codeeditors.preview.EditorPopUp;
import ir.hanzodev1375.ghostide.codeeditors.preview.Match;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.base.EditorPopupWindow;

/**
 * Tap-to-preview for http/https links inside the editor. Works like {@link ImagePreviewIde}:
 * tapping on a URL shows a small popup near the cursor. The page (title / description / og:image)
 * is fetched with Jsoup on a background thread so the UI thread is never blocked.
 */
public final class UrlPreviewIde {

  private static final long PROCESS_DELAY_MS = 100;
  private static final int TIMEOUT_MS = 6000;
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
  private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

  private final CodeEditor editor;
  private long lastProcessTime = 0;
  private String lastUrl = "";
  private EditorPopupWindow activePopup;
  private OnLinkClickEventListener call;
  public UrlPreviewIde(CodeEditor editor) {
    this.editor = editor;
  }
  
  public void setEvent(OnLinkClickEventListener call){
    this.call=call;
  }

  public void attach() {
    editor.subscribeEvent(
        SelectionChangeEvent.class,
        (event, unsubscribe) -> {
          if (event.getCause() != SelectionChangeEvent.CAUSE_TAP) return;

          long now = System.currentTimeMillis();
          if (now - lastProcessTime < PROCESS_DELAY_MS) return;
          lastProcessTime = now;

          editor.post(
              () -> {
                try {
                  handleSelectionChange(event);
                } catch (Exception e) {
                  dismissPopup();
                }
              });
        });
  }

  private void handleSelectionChange(SelectionChangeEvent event) {
    if (editor.getCursor().isSelected()) {
      dismissPopup();
      lastUrl = "";
      return;
    }

    int line = event.getLeft().getLine();
    int column = event.getLeft().getColumn();
    String lineText = editor.getText().getLineString(line);

    Match match = UrlRefUtils.findUrlAtPosition(lineText, column);

    if (match == null) {
      dismissPopup();
      lastUrl = "";
      return;
    }

    if (match.path.equals(lastUrl) && activePopup != null && activePopup.isShowing()) {
      return;
    }

    lastUrl = match.path;
    showLoadingPreview(match.path);
    fetchPreview(match.path);
  }

  private void showLoadingPreview(String url) {
    dismissPopup();
    activePopup = EditorPopUp.showCustomViewAtCursor(editor, buildLoadingView(url));
  }

  private View buildLoadingView(String url) {
    LinearLayout root = makeRoot();
    root.addView(infoLine("Loading...."));
    root.addView(infoLine(shorten(url)));
    root.setOnClickListener(v -> call.onLinkClick(url));
    return root;
  }

  // ── Background fetch (Jsoup) ─────────────────────────────────────────────

  private void fetchPreview(String url) {
    EXECUTOR.execute(
        () -> {
          UrlMeta meta;
          try {
            Document doc =
                Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();

            String title =
                firstNonEmpty(doc.select("meta[property=og:title]").attr("content"), doc.title());
            String description =
                firstNonEmpty(
                    doc.select("meta[property=og:description]").attr("content"),
                    doc.select("meta[name=description]").attr("content"));
            String image = doc.select("meta[property=og:image]").attr("content");

            meta = new UrlMeta(url, title, description, image);
          } catch (Exception e) {
            meta = new UrlMeta(url, null, null, null);
          }

          UrlMeta finalMeta = meta;
          editor.post(
              () -> {
                // Discard the result if the user has since moved on to another link.
                if (!url.equals(lastUrl)) return;
                showLoadedPreview(finalMeta);
              });
        });
  }

  // ── Loaded state ──────────────────────────────────────────────────────────

  private void showLoadedPreview(UrlMeta meta) {
    dismissPopup();
    activePopup = EditorPopUp.showCustomViewAtCursor(editor, buildLoadedView(meta));
  }

  private View buildLoadedView(UrlMeta meta) {
    LinearLayout root = makeRoot();

    if (meta.imageUrl != null && !meta.imageUrl.isEmpty()) {
      ImageView imageView = makeImageView();
      Glide.with(editor.getContext())
          .load(meta.imageUrl)
          .error(android.R.drawable.ic_delete)
          .into(imageView);
      root.addView(imageView);
    }

    boolean hasTitle = meta.title != null && !meta.title.isEmpty();
    TextView titleView = infoLine(hasTitle ? meta.title : "پیش‌نمایش در دسترس نیست");
    titleView.setTypeface(Typeface.DEFAULT_BOLD);
    titleView.setMaxLines(2);
    titleView.setEllipsize(TextUtils.TruncateAt.END);
    titleView.setMaxWidth(dp(220));
    root.addView(titleView);

    if (meta.description != null && !meta.description.isEmpty()) {
      TextView descView = infoLine(meta.description);
      descView.setMaxLines(3);
      descView.setEllipsize(TextUtils.TruncateAt.END);
      descView.setMaxWidth(dp(220));
      root.addView(descView);
    }

    root.addView(infoLine(shorten(meta.url)));
    root.setOnClickListener(v -> call.onLinkClick(meta.url));

    return root;
  }

  private String firstNonEmpty(String a, String b) {
    if (a != null && !a.trim().isEmpty()) return a.trim();
    if (b != null && !b.trim().isEmpty()) return b.trim();
    return "";
  }

  private String shorten(String url) {
    return url.length() > 40 ? "..." + url.substring(url.length() - 40) : url;
  }

  // ── View helpers ──────────────────────────────────────────────────────────

  private LinearLayout makeRoot() {
    LinearLayout root = new LinearLayout(editor.getContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(12), dp(12), dp(12), dp(12));
    return root;
  }

  private ImageView makeImageView() {
    ImageView imageView = new ImageView(editor.getContext());
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(160), dp(160));
    params.gravity = Gravity.CENTER_HORIZONTAL;
    imageView.setLayoutParams(params);
    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    return imageView;
  }

  private TextView infoLine(String text) {
    TextView tv = new TextView(editor.getContext());
    tv.setText(text);
    tv.setTextSize(12);
    tv.setTextColor(Color.WHITE);
    tv.setPadding(0, dp(4), 0, 0);
    return tv;
  }

  private void dismissPopup() {
    if (activePopup != null) {
      try {
        activePopup.dismiss();
      } catch (Exception ignored) {
      }
      activePopup = null;
    }
  }

  private int dp(int value) {
    float density = editor.getContext().getResources().getDisplayMetrics().density;
    return (int) (value * density);
  }

  private static final class UrlMeta {
    final String url;
    final String title;
    final String description;
    final String imageUrl;

    UrlMeta(String url, String title, String description, String imageUrl) {
      this.url = url;
      this.title = title;
      this.description = description;
      this.imageUrl = imageUrl;
    }
  }
}
