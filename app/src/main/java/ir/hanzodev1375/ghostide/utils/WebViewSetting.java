package ir.hanzodev1375.ghostide.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class WebViewSetting {

  @SuppressLint("SetJavaScriptEnabled")
  public static void configure(WebView webView) {
    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
    webSettings.setAllowFileAccess(true);
    webSettings.setAllowContentAccess(true);
    webSettings.setDomStorageEnabled(true);
    webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
    webSettings.setLoadWithOverviewMode(true);
    webSettings.setUseWideViewPort(true);
    webSettings.setSupportZoom(true);
    webSettings.setBuiltInZoomControls(true);
    webSettings.setDisplayZoomControls(false);
    webSettings.setDefaultTextEncodingName("UTF-8");
    webSettings.setLoadsImagesAutomatically(true);
    webSettings.setBlockNetworkImage(false);
    webSettings.setBlockNetworkLoads(false);
    webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    webSettings.setMediaPlaybackRequiresUserGesture(false);
    webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    webSettings.setTextZoom(100);
    webSettings.setMinimumFontSize(8);
    webSettings.setDefaultFontSize(16);
    webSettings.setDefaultFixedFontSize(13);
    webSettings.setGeolocationEnabled(true);
    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.setAcceptCookie(true);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      cookieManager.setAcceptThirdPartyCookies(webView, true);
      cookieManager.flush();
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      webSettings.setSafeBrowsingEnabled(true);
    }

    
    webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
    webView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
    webView.setOverScrollMode(WebView.OVER_SCROLL_ALWAYS);
  }

  
  public static void setDesktopMode(WebView webView, boolean enabled) {
    WebSettings webSettings = webView.getSettings();
    webSettings.setUseWideViewPort(enabled);
    webSettings.setLoadWithOverviewMode(enabled);
    webSettings.setSupportZoom(enabled);
    webSettings.setBuiltInZoomControls(enabled);

    String js =
        "javascript:!function(e){var t,n,o;window.innerWidth>=window.innerHeight||(t=1024/innerWidth,(n=document.querySelector(\"meta[name=viewport]\"))||((n=document.createElement(\"meta\")).name=\"viewport\",document.head.appendChild(n)),e?(o=window.innerHeight*t,sessionStorage.setItem(\"__old_viewport_content\",n.content),n.content=\"width=1024, height=\"+o):(o=sessionStorage.__old_viewport_content)&&(n.content=o))}("
            + enabled
            + ");";
    webView.loadUrl(js);
  }

  
  public static void setForceDark(WebView webView, boolean enabled) {
//    
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//      if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
//        WebSettingsCompat.setForceDark(
//            webView.getSettings(),
//            enabled ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF);
//      }
//    }
  }

  public static void setZoomEnabled(WebView webView, boolean enabled) {
    WebSettings webSettings = webView.getSettings();
    webSettings.setSupportZoom(enabled);
    webSettings.setBuiltInZoomControls(enabled);
  }

  public static void clearAllCache(WebView webView) {
    webView.clearCache(true);
    webView.clearHistory();
    CookieManager.getInstance().removeAllCookies(null);
    CookieManager.getInstance().flush();
  }
}
