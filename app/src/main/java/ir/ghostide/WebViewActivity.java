package ir.hanzodev1375.ghostide.activity;

import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.databinding.ActivityWebBinding;
import ir.hanzodev1375.ghostide.utils.WebViewSetting;

public class WebViewActivity extends BaseCompat {
  private ActivityWebBinding binding;
  private boolean isDesktopMode = false;
  private boolean isZoomEnabled = true;
  private boolean isDarkMode = false;
  private boolean isRef = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityWebBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    setSupportActionBar(binding.toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    binding.toolbar.setNavigationOnClickListener(v -> finish());
    injectEruda();
    WebViewSetting.configure(binding.webView);
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                  binding.webView.goBack();
                } else {
                  setEnabled(false);
                  getOnBackPressedDispatcher().onBackPressed();
                }
              }
            });
    
    binding.webView.setWebViewClient(
        new WebViewClient() {
          @Override
          public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (getSupportActionBar() != null) {
              getSupportActionBar().setTitle(view.getTitle());
              getSupportActionBar().setSubtitle(url);
              binding.swiperefreshlayout.setRefreshing(false);
              injectEruda();
            }
          }
        });

    binding.webView.setWebChromeClient(new WebChromeClient());
    setLoading();
    binding.swiperefreshlayout.setOnRefreshListener(
        () -> {
          setLoading();
        });
  }

  public void setLoading() {

    if (getIntent().hasExtra("keyweb")) {
      String filePath = getIntent().getStringExtra("keyweb");
      binding.webView.loadUrl("file://" + filePath);
      injectEruda();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_webview, menu);
    menu.findItem(R.id.desktop_mode).setChecked(isDesktopMode);
    menu.findItem(R.id.zooming).setChecked(isZoomEnabled);
    menu.findItem(R.id.drakmod).setChecked(isDarkMode);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.back) {
      if (binding.webView.canGoBack()) {
        binding.webView.goBack();
      } else {
        Toast.makeText(this, "Can't go back", Toast.LENGTH_SHORT).show();
      }
    } else if (id == R.id.forward) {
      if (binding.webView.canGoForward()) {
        binding.webView.goForward();
      } else {
        Toast.makeText(this, "Can't go forward", Toast.LENGTH_SHORT).show();
      }
    } else if (id == R.id.refresh) {
      setLoading();
    } else if (id == R.id.desktop_mode) {
      isDesktopMode = !isDesktopMode;
      item.setChecked(isDesktopMode);
      WebViewSetting.setDesktopMode(binding.webView, isDesktopMode);
    } else if (id == R.id.zooming) {
      isZoomEnabled = !isZoomEnabled;
      item.setChecked(isZoomEnabled);
      WebViewSetting.setZoomEnabled(binding.webView, isZoomEnabled);
    } else if (id == R.id.drakmod) {
      isDarkMode = !isDarkMode;
      item.setChecked(isDarkMode);
    } else if (id == R.id.reloader) {
      injectEruda();
    } else if (id == R.id.exit) {
      finish();
    }

    return true;
  }

  private void injectEruda() {
    String erudaPath = "file:///android_asset/web/eruda.js";
    String js =
        "(function(){"
            + "var script = document.createElement('script');"
            + "script.src = '"
            + erudaPath
            + "';"
            + "document.body.appendChild(script);"
            + "eruda.init({"
            + "    defaults: {"
            + "        displaySize: 77,"
            + "        transparency: 1"
            + "        "
            + "    }"
            + "});"
            + "})();";
    binding.webView.post(
        () -> {
          binding.webView.loadUrl("javascript:" + js);
        });
  }
}
