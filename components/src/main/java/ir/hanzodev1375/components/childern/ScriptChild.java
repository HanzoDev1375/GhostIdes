package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;

/**
 * Renders an html/htm file (or content Uri) inside a WebView.
 *
 * <p>Lifecycle aware: when the host pauses, JS timers/layout/rendering pause with it
 * (per-view {@link WebView#onPause()}, never the global timers), so an html background
 * costs nothing while the app is in the background.</p>
 */
public class ScriptChild implements IChild, DefaultLifecycleObserver {

  private final WebView webView;
  private final String path;
  private final LifecycleOwner owner;

  public ScriptChild(Context context, String path, @Nullable LifecycleOwner owner) {
    this.path = path;
    this.owner = owner;
    this.webView = new WebView(context);
    this.webView.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    configure(this.webView.getSettings());
    // Decorative content: must never take focus or open the soft keyboard
    // (e.g. html scripts with <video>/gif autoplay inside editor backgrounds).
    this.webView.setFocusable(false);
    this.webView.setFocusableInTouchMode(false);
    this.webView.setClickable(false);
    this.webView.setLongClickable(false);
    this.webView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    this.webView.setVerticalScrollBarEnabled(false);
    this.webView.setHorizontalScrollBarEnabled(false);
    this.webView.setBackgroundColor(Color.TRANSPARENT);
    this.webView.loadUrl(toLocalUrl(path));
    // Delivers onResume() immediately when the owner is already resumed.
    if (owner != null) {
      owner.getLifecycle().addObserver(this);
    }
  }

  @Override
  public View view() {
    return webView;
  }

  @Override
  public String pathTheme() {
    return path;
  }

  @Override
  public void onResume(@NonNull LifecycleOwner lifecycleOwner) {
    webView.onResume();
  }

  @Override
  public void onPause(@NonNull LifecycleOwner lifecycleOwner) {
    webView.onPause();
  }

  @Override
  public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
    release();
  }

  @Override
  public void release() {
    if (owner != null) {
      owner.getLifecycle().removeObserver(this);
    }
    webView.stopLoading();
    webView.loadUrl("about:blank");
    webView.onPause();
    webView.removeAllViews();
    webView.destroyDrawingCache();
    webView.destroy();
  }

  private void configure(WebSettings settings) {
    settings.setJavaScriptEnabled(true);
    settings.setAllowFileAccess(true);
    settings.setAllowContentAccess(true);
    settings.setDomStorageEnabled(true);
    settings.setMediaPlaybackRequiresUserGesture(false);
    settings.setLoadWithOverviewMode(true);
    settings.setUseWideViewPort(true);
    settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
  }

  private String toLocalUrl(String rawPath) {
    if (rawPath == null || rawPath.isEmpty()) return "about:blank";
    if (rawPath.startsWith("file:")
        || rawPath.startsWith("content:")
        || rawPath.startsWith("http:")
        || rawPath.startsWith("https:")
        || rawPath.startsWith("data:")) {
      return rawPath;
    }
    // Properly encode spaces / non-ascii characters in file paths.
    return Uri.fromFile(new File(rawPath)).toString();
  }
}
