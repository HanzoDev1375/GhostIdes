package ir.hanzodev1375.ghostide.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import ir.hanzodev1375.components.WebViewBottomSheetFragment;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.databinding.ActivityWebBinding;
import ir.hanzodev1375.ghostide.utils.WebViewSetting;

@SuppressLint("SetJavaScriptEnabled")
public class WebViewActivity extends BaseCompat {

  private ActivityWebBinding b;

  private boolean isDesktopMode = false;
  private boolean isZoomEnabled = true;
  private boolean isDarkMode = false;
  private boolean isDevToolsOpen = false;
  private boolean isPickerActive = false;
  private boolean isErudaReady = false;

  private static final int TAB_CONSOLE = 0;
  private static final int TAB_ELEMENTS = 1;
  private static final int TAB_NETWORK = 2;
  private static final int TAB_STORAGE = 3;
  private int currentTab = TAB_CONSOLE;

  // ─────────────────────────────────────────────────────────────────────────
  // Lifecycle
  // ─────────────────────────────────────────────────────────────────────────

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    b = ActivityWebBinding.inflate(getLayoutInflater());
    setContentView(b.getRoot());

    setupToolbar();
    setupWebView();
    setupUrlBar();
    setupDevToolsPanel();
    setupFab();
    setupBackHandler();
    loadIntentUrl();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Setup
  // ─────────────────────────────────────────────────────────────────────────

  private void setupToolbar() {
    setSupportActionBar(b.toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    b.toolbar.setNavigationOnClickListener(
        v ->
            WebViewBottomSheetFragment.newInstance("www.google.com")
                .show(getSupportFragmentManager(), ""));
  }

  private void setupWebView() {
    WebViewSetting.configure(b.webView);
    b.webView.addJavascriptInterface(new Bridge(), "GhostBridge");

    b.webView.setWebViewClient(
        new WebViewClient() {
          @Override
          public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            b.pageProgress.setVisibility(View.VISIBLE);
            b.pageProgress.setProgress(10);
            b.urlBar.setText(url);
          }

          @Override
          public void onPageFinished(WebView view, String url) {
            b.pageProgress.setVisibility(View.GONE);
            b.urlBar.setText(url);
            b.swiperefreshlayout.setRefreshing(false);
            if (getSupportActionBar() != null) {
              getSupportActionBar().setTitle(view.getTitle());
            }
            injectEruda();
            injectConsoleHook();
            injectNetworkHook();
          }

          @Override
          public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false;
          }
        });

    b.webView.setWebChromeClient(
        new WebChromeClient() {
          @Override
          public void onProgressChanged(WebView view, int progress) {
            b.pageProgress.setProgress(progress);
            b.pageProgress.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
          }

          @Override
          public boolean onConsoleMessage(android.webkit.ConsoleMessage msg) {
            appendConsole(
                msg.messageLevel().name(), msg.message(), msg.sourceId(), msg.lineNumber());
            return true;
          }
        });

    b.swiperefreshlayout.setOnRefreshListener(() -> b.webView.reload());
  }

  private void setupUrlBar() {
    b.urlBar.setOnEditorActionListener(
        (v, actionId, event) -> {
          boolean isGo = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO;
          boolean isEnter =
              event != null
                  && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                  && event.getAction() == KeyEvent.ACTION_DOWN;
          if (isGo || isEnter) {
            navigateTo(b.urlBar.getText().toString().trim());
            return true;
          }
          return false;
        });

    b.btnGo.setOnClickListener(v -> navigateTo(b.urlBar.getText().toString().trim()));
  }

  private void setupDevToolsPanel() {
    b.tabConsole.setOnClickListener(v -> switchTab(TAB_CONSOLE));
    b.tabElements.setOnClickListener(v -> switchTab(TAB_ELEMENTS));
    b.tabNetwork.setOnClickListener(v -> switchTab(TAB_NETWORK));
    b.tabStorage.setOnClickListener(v -> switchTab(TAB_STORAGE));

    b.consoleInput.setOnEditorActionListener(
        (v, actionId, event) -> {
          boolean isSend = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND;
          boolean isEnter =
              event != null
                  && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                  && event.getAction() == KeyEvent.ACTION_DOWN;
          if (isSend || isEnter) {
            runConsoleCommand(b.consoleInput.getText().toString().trim());
            b.consoleInput.setText("");
            return true;
          }
          return false;
        });

    b.consoleClear.setOnClickListener(
        v -> {
          WebViewBottomSheetFragment.newInstance("https//www.google.com")
              .show(getSupportFragmentManager(), "");
          b.consoleOutput.setText("");
        });

    b.btnPickElement.setOnClickListener(
        v -> {
          if (isPickerActive) cancelPicker();
          else activatePicker();
        });

    b.btnRefreshDom.setOnClickListener(v -> loadDomTree());
    b.btnClearNetwork.setOnClickListener(v -> b.networkLog.setText(""));
    b.btnLocalStorage.setOnClickListener(v -> loadStorage("localStorage"));
    b.btnSessionStorage.setOnClickListener(v -> loadStorage("sessionStorage"));
    b.btnCookies.setOnClickListener(v -> loadCookies());
  }

  private void setupFab() {
    b.fabDevTools.setOnClickListener(v -> toggleDevTools());
  }

  private void setupBackHandler() {
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (isDevToolsOpen) {
                  toggleDevTools();
                } else if (b.webView.canGoBack()) {
                  b.webView.goBack();
                } else {
                  setEnabled(false);
                  getOnBackPressedDispatcher().onBackPressed();
                }
              }
            });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Navigation
  // ─────────────────────────────────────────────────────────────────────────

  private void loadIntentUrl() {
    if (getIntent().hasExtra("keyweb")) {
      String path = getIntent().getStringExtra("keyweb");
      b.webView.loadUrl("file://" + path);
    }
  }

  private void navigateTo(String input) {
    if (input.isEmpty()) return;
    String url;
    if (input.startsWith("http://")
        || input.startsWith("https://")
        || input.startsWith("file://")) {
      url = input;
    } else if (input.contains(".") && !input.contains(" ")) {
      url = "https://" + input;
    } else {
      url = "https://www.google.com/search?q=" + android.net.Uri.encode(input);
    }
    b.webView.loadUrl(url);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Eruda
  // ─────────────────────────────────────────────────────────────────────────

  private void injectEruda() {
    isErudaReady = false;
    String erudaPath = "file:///android_asset/web/eruda.js";
    String js =
        "(function() {"
            + "  if (typeof eruda !== 'undefined') { GhostBridge.onErudaReady(); return; }"
            + "  var s = document.createElement('script');"
            + "  s.src = '"
            + erudaPath
            + "';"
            + "  s.onload = function() {"
            + "    eruda.init({ defaults: { displaySize: 50, transparency: 0.95 } });"
            + "    eruda.hide();"
            + "    GhostBridge.onErudaReady();"
            + "  };"
            + "  document.body.appendChild(s);"
            + "})();";
    evalJs(js);
  }

  private void showEruda(String panel) {
    if (!isErudaReady) {
      Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();
      return;
    }
    String js =
        "(function() {"
            + "  eruda.show();"
            + "  if ('"
            + panel
            + "') eruda.show('"
            + panel
            + "');"
            + "})();";
    evalJs(js);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // DevTools Panel
  // ─────────────────────────────────────────────────────────────────────────

  private void toggleDevTools() {
    isDevToolsOpen = !isDevToolsOpen;
    b.devToolsPanel.setVisibility(isDevToolsOpen ? View.VISIBLE : View.GONE);
    if (isDevToolsOpen) switchTab(currentTab);
  }

  private void switchTab(int tab) {
    currentTab = tab;

    b.consoleContainer.setVisibility(View.GONE);
    b.elementsContainer.setVisibility(View.GONE);
    b.networkContainer.setVisibility(View.GONE);
    b.storageContainer.setVisibility(View.GONE);

    b.tabConsole.setAlpha(0.45f);
    b.tabElements.setAlpha(0.45f);
    b.tabNetwork.setAlpha(0.45f);
    b.tabStorage.setAlpha(0.45f);

    switch (tab) {
      case TAB_CONSOLE:
        b.consoleContainer.setVisibility(View.VISIBLE);
        b.tabConsole.setAlpha(1f);
        break;
      case TAB_ELEMENTS:
        b.elementsContainer.setVisibility(View.VISIBLE);
        b.tabElements.setAlpha(1f);
        loadDomTree();
        break;
      case TAB_NETWORK:
        b.networkContainer.setVisibility(View.VISIBLE);
        b.tabNetwork.setAlpha(1f);
        break;
      case TAB_STORAGE:
        b.storageContainer.setVisibility(View.VISIBLE);
        b.tabStorage.setAlpha(1f);
        loadStorage("localStorage");
        break;
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Console
  // ─────────────────────────────────────────────────────────────────────────

  private void injectConsoleHook() {
    String js =
        "(function() {"
            + "  if (window.__ghostConsoleHooked) return;"
            + "  window.__ghostConsoleHooked = true;"
            + "  ['log','warn','error','info','debug'].forEach(function(m) {"
            + "    var orig = console[m].bind(console);"
            + "    console[m] = function() {"
            + "      var msg = Array.prototype.slice.call(arguments).map(function(a) {"
            + "        return typeof a === 'object' ? JSON.stringify(a, null, 2) : String(a);"
            + "      }).join(' ');"
            + "      GhostBridge.onConsole(m.toUpperCase(), msg);"
            + "      orig.apply(console, arguments);"
            + "    };"
            + "  });"
            + "})();";
    evalJs(js);
  }

  private void runConsoleCommand(String code) {
    if (code.isEmpty()) return;
    appendConsole("INPUT", code, "", 0);
    String js =
        "(function() {"
            + "  try {"
            + "    var r = eval("
            + escapeJs(code)
            + ");"
            + "    GhostBridge.onConsole('RESULT', r === undefined ? 'undefined' : JSON.stringify(r, null, 2));"
            + "  } catch(e) {"
            + "    GhostBridge.onConsole('ERROR', e.message);"
            + "  }"
            + "})();";
    evalJs(js);
  }

  private void appendConsole(String level, String message, String source, int line) {
    runOnUiThread(
        () -> {
          int color;
          switch (level) {
            case "ERROR":
              color = 0xFFFF5252;
              break;
            case "WARN":
              color = 0xFFFFB300;
              break;
            case "INFO":
              color = 0xFF40C4FF;
              break;
            case "RESULT":
              color = 0xFF69F0AE;
              break;
            case "INPUT":
              color = 0xFFCCCCCC;
              break;
            default:
              color = 0xFFAAAAAA;
              break;
          }
          String prefix = level.equals("INPUT") ? "› " : "[" + level + "] ";
          String suffix =
              (!source.isEmpty() && !level.equals("INPUT"))
                  ? "  (" + source.replaceAll(".*/", "") + ":" + line + ")"
                  : "";
          String text = prefix + message + suffix + "\n";

          SpannableString span = new SpannableString(text);
          span.setSpan(
              new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          b.consoleOutput.append(span);
          b.consoleScroll.post(() -> b.consoleScroll.fullScroll(View.FOCUS_DOWN));
        });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Elements
  // ─────────────────────────────────────────────────────────────────────────

  private void loadDomTree() {
    String js =
        "(function() {"
            + "  function node(el, d) {"
            + "    var pad = Array(d * 2 + 1).join(' ');"
            + "    if (!el.tagName) {"
            + "      var t = el.textContent.trim();"
            + "      return t ? pad + t + '\\n' : '';"
            + "    }"
            + "    var tag = el.tagName.toLowerCase();"
            + "    var attrs = Array.from(el.attributes).map(function(a) {"
            + "      return ' ' + a.name + '=\"' + a.value + '\"';"
            + "    }).join('');"
            + "    var out = pad + '<' + tag + attrs + '>\\n';"
            + "    Array.from(el.childNodes).forEach(function(c) { out += node(c, d + 1); });"
            + "    out += pad + '</' + tag + '>\\n';"
            + "    return out;"
            + "  }"
            + "  GhostBridge.onDomTree(node(document.documentElement, 0));"
            + "})();";
    evalJs(js);
  }

  private void activatePicker() {
    isPickerActive = true;
    b.btnPickElement.setText("✕ Cancel");
    Toast.makeText(this, "Tap any element", Toast.LENGTH_SHORT).show();

    String js =
        "(function() {"
            + "  if (window.__ghostPicker) return;"
            + "  window.__ghostPicker = true;"
            + "  var last = null, savedOutline = '';"
            + "  function onOver(e) {"
            + "    if (last) last.style.outline = savedOutline;"
            + "    last = e.target;"
            + "    savedOutline = last.style.outline;"
            + "    last.style.outline = '2px solid #FF6200';"
            + "  }"
            + "  function onClick(e) {"
            + "    e.preventDefault(); e.stopPropagation();"
            + "    if (last) last.style.outline = savedOutline;"
            + "    var el = e.target;"
            + "    var cs = window.getComputedStyle(el);"
            + "    var props = ['color','background-color','font-size','font-family',"
            + "                 'margin','padding','border','width','height','display','position'];"
            + "    var css = props.map(function(p) { return p + ': ' + cs.getPropertyValue(p); }).join('\\n');"
            + "    GhostBridge.onElementPicked(el.outerHTML, css, el.tagName.toLowerCase());"
            + "    cleanup();"
            + "  }"
            + "  function cleanup() {"
            + "    document.removeEventListener('mouseover', onOver, true);"
            + "    document.removeEventListener('click', onClick, true);"
            + "    window.__ghostPicker = false;"
            + "    GhostBridge.onPickerDone();"
            + "  }"
            + "  document.addEventListener('mouseover', onOver, true);"
            + "  document.addEventListener('click', onClick, true);"
            + "})();";
    evalJs(js);
  }

  private void cancelPicker() {
    isPickerActive = false;
    b.btnPickElement.setText("Pick Element");
    evalJs("window.__ghostPicker = false;");
  }

  private void showElementEditor(String outerHtml, String computedCss, String tag) {
    android.widget.LinearLayout root = new android.widget.LinearLayout(this);
    root.setOrientation(android.widget.LinearLayout.VERTICAL);
    root.setPadding(dp(16), dp(8), dp(16), dp(8));

    android.widget.TextView tvTag = new android.widget.TextView(this);
    tvTag.setText("<" + tag + ">");
    tvTag.setTypeface(android.graphics.Typeface.MONOSPACE);
    tvTag.setTextColor(0xFF4CAF50);
    tvTag.setPadding(0, 0, 0, dp(8));
    root.addView(tvTag);

    android.widget.TextView lHtml = new android.widget.TextView(this);
    lHtml.setText("HTML");
    lHtml.setTextSize(11);
    root.addView(lHtml);

    android.widget.EditText etHtml = new android.widget.EditText(this);
    etHtml.setText(outerHtml);
    etHtml.setMinLines(4);
    etHtml.setMaxLines(10);
    etHtml.setTypeface(android.graphics.Typeface.MONOSPACE);
    etHtml.setTextSize(12);
    etHtml.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    root.addView(etHtml);

    android.widget.TextView lCss = new android.widget.TextView(this);
    lCss.setText("Computed CSS");
    lCss.setTextSize(11);
    lCss.setPadding(0, dp(12), 0, 0);
    root.addView(lCss);

    android.widget.EditText etCss = new android.widget.EditText(this);
    etCss.setText(computedCss);
    etCss.setMinLines(4);
    etCss.setMaxLines(8);
    etCss.setTypeface(android.graphics.Typeface.MONOSPACE);
    etCss.setTextSize(11);
    etCss.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    root.addView(etCss);

    android.widget.ScrollView scroll = new android.widget.ScrollView(this);
    scroll.addView(root);

    new AlertDialog.Builder(this)
        .setTitle("Element Editor — <" + tag + ">")
        .setView(scroll)
        .setPositiveButton(
            "Apply HTML",
            (d, w) -> {
              String html =
                  etHtml
                      .getText()
                      .toString()
                      .replace("\\", "\\\\")
                      .replace("'", "\\'")
                      .replace("\n", "\\n");
              evalJs(
                  "(function() {"
                      + "  var tmp = document.createElement('div');"
                      + "  tmp.innerHTML = '"
                      + html
                      + "';"
                      + "  if (tmp.firstChild && window.__ghostLastEl) {"
                      + "    window.__ghostLastEl.replaceWith(tmp.firstChild);"
                      + "  }"
                      + "})();");
            })
        .setNeutralButton(
            "Inject CSS",
            (d, w) -> {
              String css =
                  etCss
                      .getText()
                      .toString()
                      .replace("\\", "\\\\")
                      .replace("'", "\\'")
                      .replace("\n", " ");
              evalJs(
                  "(function() {"
                      + "  var s = document.createElement('style');"
                      + "  s.textContent = '"
                      + css
                      + "';"
                      + "  document.head.appendChild(s);"
                      + "})();");
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Network
  // ─────────────────────────────────────────────────────────────────────────

  private void injectNetworkHook() {
    String js =
        "(function() {"
            + "  if (window.__ghostNetHooked) return;"
            + "  window.__ghostNetHooked = true;"
            + "  var origFetch = window.fetch;"
            + "  window.fetch = function(url, opts) {"
            + "    var t = Date.now();"
            + "    var method = (opts && opts.method) ? opts.method.toUpperCase() : 'GET';"
            + "    return origFetch.apply(this, arguments)"
            + "      .then(function(r) { GhostBridge.onNetwork(method, String(url), r.status, Date.now()-t); return r; })"
            + "      .catch(function(e) { GhostBridge.onNetwork(method, String(url), 0, Date.now()-t); throw e; });"
            + "  };"
            + "  var origOpen = XMLHttpRequest.prototype.open;"
            + "  XMLHttpRequest.prototype.open = function(m, u) {"
            + "    this._m = m; this._u = u; this._t = Date.now();"
            + "    this.addEventListener('loadend', function() {"
            + "      GhostBridge.onNetwork(this._m, this._u, this.status, Date.now()-this._t);"
            + "    });"
            + "    return origOpen.apply(this, arguments);"
            + "  };"
            + "})();";
    evalJs(js);
  }

  private void appendNetwork(String method, String url, int status, long ms) {
    runOnUiThread(
        () -> {
          int color = status == 0 ? 0xFFFFB300 : status >= 400 ? 0xFFFF5252 : 0xFF69F0AE;
          String shortUrl = url.length() > 60 ? url.substring(0, 57) + "..." : url;
          String text =
              method + "  " + (status == 0 ? "ERR" : status) + "  " + ms + "ms  " + shortUrl + "\n";
          SpannableString span = new SpannableString(text);
          span.setSpan(
              new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
          b.networkLog.append(span);
        });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Storage
  // ─────────────────────────────────────────────────────────────────────────

  private void loadStorage(String type) {
    evalJs(
        "(function() {"
            + "  var s = window."
            + type
            + ", out = '';"
            + "  for (var i = 0; i < s.length; i++) {"
            + "    var k = s.key(i);"
            + "    out += k + ':\\n  ' + s.getItem(k) + '\\n\\n';"
            + "  }"
            + "  GhostBridge.onStorage('"
            + type
            + "', out || '(empty)');"
            + "})();");
  }

  private void loadCookies() {
    evalJs("GhostBridge.onStorage('cookies', document.cookie || '(empty)');");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Bridge
  // ─────────────────────────────────────────────────────────────────────────

  private class Bridge {

    @JavascriptInterface
    public void onErudaReady() {
      runOnUiThread(() -> isErudaReady = true);
    }

    @JavascriptInterface
    public void onConsole(String level, String message) {
      appendConsole(level, message, "", 0);
    }

    @JavascriptInterface
    public void onDomTree(String html) {
      runOnUiThread(() -> b.domTree.setText(html));
    }

    @JavascriptInterface
    public void onElementPicked(String outerHtml, String css, String tag) {
      evalJs("window.__ghostLastEl = document.querySelector(':hover');");
      runOnUiThread(() -> showElementEditor(outerHtml, css, tag));
    }

    @JavascriptInterface
    public void onPickerDone() {
      runOnUiThread(
          () -> {
            isPickerActive = false;
            b.btnPickElement.setText("Pick Element");
          });
    }

    @JavascriptInterface
    public void onNetwork(String method, String url, int status, long ms) {
      appendNetwork(method, url, status, ms);
    }

    @JavascriptInterface
    public void onStorage(String type, String data) {
      runOnUiThread(() -> b.storageOutput.setText("[" + type + "]\n\n" + data));
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Menu
  // ─────────────────────────────────────────────────────────────────────────

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
      if (b.webView.canGoBack()) b.webView.goBack();
      else Toast.makeText(this, "Can't go back", Toast.LENGTH_SHORT).show();

    } else if (id == R.id.forward) {
      if (b.webView.canGoForward()) b.webView.goForward();
      else Toast.makeText(this, "Can't go forward", Toast.LENGTH_SHORT).show();

    } else if (id == R.id.refresh) {
      b.webView.reload();

    } else if (id == R.id.desktop_mode) {
      isDesktopMode = !isDesktopMode;
      item.setChecked(isDesktopMode);
      WebViewSetting.setDesktopMode(b.webView, isDesktopMode);

    } else if (id == R.id.zooming) {
      isZoomEnabled = !isZoomEnabled;
      item.setChecked(isZoomEnabled);
      WebViewSetting.setZoomEnabled(b.webView, isZoomEnabled);

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

  // ─────────────────────────────────────────────────────────────────────────
  // Helpers
  // ─────────────────────────────────────────────────────────────────────────

  private void evalJs(String js) {
    b.webView.post(() -> b.webView.evaluateJavascript(js, null));
  }

  private String escapeJs(String code) {
    return "\""
        + code.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        + "\"";
  }

  private int dp(int val) {
    return Math.round(val * getResources().getDisplayMetrics().density);
  }
}
