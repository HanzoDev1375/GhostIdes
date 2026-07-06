package ir.hanzodev1375.components;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import com.google.android.material.color.MaterialColors;
import ir.hanzodev1375.components.views.TouchableWebView;

public class WebViewBottomSheetFragment extends BottomSheetDialogFragment {

  private static final String ARG_URL = "arg_url";
  private static final float MAX_CORNER_RADIUS_DP = 16f;

  private TouchableWebView webView;
  private ProgressBar progressBar;
  private ImageView ivIcon;
  private TextView tvHeaderText;
  private ImageButton btnClose;
  private ImageButton btnDropdown;
  private ImageButton btnMenu;

  private String pageUrl;
  private String currentTitle = "";
  private String currentHost = "";
  private boolean isExpanded = true;
  int backgroundColor;
  View headerback;

  private GradientDrawable sheetBackground;
  private ValueAnimator progressAnimator;

  // رنگ‌هایی که قبل از ساخته شدن ویو ست شده‌اند و باید بعد از onViewCreated اعمال شوند
  private Integer pendingSheetBgColor;
  private Integer pendingHeaderTextColor;
  private Integer pendingIconColor;
  private boolean pendingClearIconColor = false;
  private Integer pendingCloseIconTint;
  private Integer pendingDropdownIconTint;
  private Integer pendingMenuIconTint;
  private Integer pendingProgressBarColor;

  public static WebViewBottomSheetFragment newInstance(String url) {
    WebViewBottomSheetFragment fragment = new WebViewBottomSheetFragment();
    Bundle args = new Bundle();
    args.putString(ARG_URL, url);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public int getTheme() {
    return R.style.AppBottomSheetDialogTheme;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.bottom_sheet_webview, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    backgroundColor = MaterialColors.getColor(getContext(), R.attr.colorSurface, 0);

    if (getArguments() != null) {
      String rawUrl = getArguments().getString(ARG_URL);
      if (!TextUtils.isEmpty(rawUrl)) {
        if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
          pageUrl = "https://" + rawUrl;
        } else if (rawUrl.startsWith("http://")) {
          pageUrl = rawUrl.replaceFirst("http://", "https://");
        } else {
          pageUrl = rawUrl;
        }
      }
    }

    webView = view.findViewById(R.id.web_view);
    progressBar = view.findViewById(R.id.progress_web);
    ivIcon = view.findViewById(R.id.iv_icon);
    tvHeaderText = view.findViewById(R.id.tv_header_text);
    btnClose = view.findViewById(R.id.btn_close);
    btnDropdown = view.findViewById(R.id.btn_dropdown);
    btnMenu = view.findViewById(R.id.btn_menu);
    headerback = view.findViewById(R.id.headerback);

    setupWebView();
    setupClicks();

    if (!TextUtils.isEmpty(pageUrl)) {
      currentTitle = getString(R.string.web_sheet_loading);
      currentHost = getHost(pageUrl);
      updateHeaderText();
      webView.loadUrl(pageUrl);
    }

    applyPendingThemeIfAny();
  }

  private void applyPendingThemeIfAny() {
    if (pendingSheetBgColor != null) setSheetBackgroundColor(pendingSheetBgColor);
    if (pendingHeaderTextColor != null) setHeaderTextColor(pendingHeaderTextColor);
    if (pendingIconColor != null) setIconColorFilter(pendingIconColor);
    if (pendingClearIconColor) clearIconColorFilter();
    if (pendingCloseIconTint != null) setCloseIconTint(pendingCloseIconTint);
    if (pendingDropdownIconTint != null) setDropdownIconTint(pendingDropdownIconTint);
    if (pendingMenuIconTint != null) setMenuIconTint(pendingMenuIconTint);
    if (pendingProgressBarColor != null) setProgressBarColor(pendingProgressBarColor);
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void setupWebView() {
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setDomStorageEnabled(true);
    webView.getSettings().setLoadWithOverviewMode(true);
    webView.getSettings().setUseWideViewPort(true);

    webView.setWebViewClient(
        new WebViewClient() {
          @Override
          public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            showProgressBar();
            ivIcon.setImageResource(R.drawable.ic_public_24);
            currentTitle = getString(R.string.web_sheet_loading);
            currentHost = getHost(url);
            updateHeaderText();
          }

          @Override
          public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            hideProgressBarAnimated();
          }
        });

    webView.setWebChromeClient(
        new WebChromeClient() {
          @Override
          public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            animateProgressTo(newProgress);
            if (newProgress >= 100) {
              hideProgressBarAnimated();
            }
          }

          @Override
          public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (!TextUtils.isEmpty(title)) {
              currentTitle = title;
              updateHeaderText();
            }
          }

          @Override
          public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
            if (icon != null) {
              ivIcon.setImageBitmap(icon);
            }
          }
        });
  }

  private void updateHeaderText() {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    builder.append(currentTitle);
    builder.setSpan(
        new StyleSpan(Typeface.BOLD), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    if (!TextUtils.isEmpty(currentHost)) {
      builder.append(" · ").append(currentHost);
    }

    tvHeaderText.setText(builder);
  }

  private void showProgressBar() {
    progressBar.animate().cancel();
    progressBar.setAlpha(1f);
    progressBar.setProgress(0);
    progressBar.setVisibility(View.VISIBLE);
  }

  private void animateProgressTo(int target) {
    if (progressAnimator != null) {
      progressAnimator.cancel();
    }
    int current = progressBar.getProgress();
    progressAnimator = ValueAnimator.ofInt(current, target);
    progressAnimator.setDuration(180);
    progressAnimator.setInterpolator(new DecelerateInterpolator());
    progressAnimator.addUpdateListener(
        new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator animation) {
            progressBar.setProgress((Integer) animation.getAnimatedValue());
          }
        });
    progressAnimator.start();
  }

  private void hideProgressBarAnimated() {
    progressBar
        .animate()
        .alpha(0f)
        .setDuration(250)
        .setStartDelay(150)
        .withEndAction(
            new Runnable() {
              @Override
              public void run() {
                progressBar.setVisibility(View.GONE);
                progressBar.setProgress(0);
                progressBar.setAlpha(1f);
              }
            })
        .start();
  }

  private void setupClicks() {
    btnClose.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            dismiss();
          }
        });

    btnDropdown.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            toggleSheetState();
          }
        });

    btnMenu.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            showOptionsMenu(v);
          }
        });
  }

  private void toggleSheetState() {
    Dialog dialog = getDialog();
    if (!(dialog instanceof BottomSheetDialog)) return;
    BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
    FrameLayout bottomSheet =
        bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
    if (bottomSheet == null) return;
    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
    bottomSheet.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
    if (isExpanded) {
      webView.setVisibility(View.GONE);
      behavior.setFitToContents(true);
      behavior.setSkipCollapsed(true);
      behavior.setHideable(true);
      behavior.setDraggable(true);
      ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
      params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
      bottomSheet.setLayoutParams(params);
      behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    } else {
      webView.setVisibility(View.VISIBLE);
      ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
      params.height = ViewGroup.LayoutParams.MATCH_PARENT;
      bottomSheet.setLayoutParams(params);
      behavior.setFitToContents(false);
      behavior.setExpandedOffset(0);
      behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
  }

  private void showOptionsMenu(View anchor) {
    PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
    popupMenu.getMenu().add(0, 1, 0, getString(R.string.web_sheet_menu_reload));
    popupMenu.getMenu().add(0, 2, 1, getString(R.string.web_sheet_menu_copy_link));
    popupMenu.getMenu().add(0, 3, 2, getString(R.string.web_sheet_menu_open_browser));

    popupMenu.setOnMenuItemClickListener(
        new PopupMenu.OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            int id = item.getItemId();
            if (id == 1) {
              webView.reload();
              return true;
            } else if (id == 2) {
              copyLinkToClipboard();
              return true;
            } else if (id == 3) {
              openInExternalBrowser();
              return true;
            }
            return false;
          }
        });

    popupMenu.show();
  }

  private void copyLinkToClipboard() {
    Context context = requireContext();
    ClipboardManager clipboard =
        (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText("link", pageUrl);
    if (clipboard != null) {
      clipboard.setPrimaryClip(clip);
      Toast.makeText(context, getString(R.string.web_sheet_link_copied), Toast.LENGTH_SHORT).show();
    }
  }

  private void openInExternalBrowser() {
    if (TextUtils.isEmpty(pageUrl)) return;
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl));
    startActivity(intent);
  }

  private String getHost(String url) {
    if (url == null) return "";
    Uri uri = Uri.parse(url);
    String host = uri.getHost();
    return host != null ? host : url;
  }

  private float dpToPx(float dp) {
    DisplayMetrics metrics = getResources().getDisplayMetrics();
    return dp * (metrics.densityDpi / 160f);
  }

  @Nullable
  private BottomSheetBehavior<FrameLayout> getBehavior() {
    Dialog dialog = getDialog();
    if (!(dialog instanceof BottomSheetDialog)) return null;

    BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
    FrameLayout bottomSheet =
        bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
    if (bottomSheet == null) return null;

    return BottomSheetBehavior.from(bottomSheet);
  }

  @Override
  public void onStart() {
    super.onStart();

    Dialog dialog = getDialog();
    if (!(dialog instanceof BottomSheetDialog)) return;

    BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
    final FrameLayout bottomSheet =
        bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
    if (bottomSheet == null) return;
    bottomSheet.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
    final BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
    if (webView.getVisibility() == View.VISIBLE) {

      behavior.setHideable(false);
      behavior.setSkipCollapsed(false);
      behavior.setDraggable(true);
      ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
      params.height = ViewGroup.LayoutParams.MATCH_PARENT;
      bottomSheet.setLayoutParams(params);

      behavior.setFitToContents(false);
      behavior.setExpandedOffset(0);
    }
    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    behavior.addBottomSheetCallback(
        new BottomSheetBehavior.BottomSheetCallback() {
          @Override
          public void onStateChanged(@NonNull View sheetView, int newState) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
              isExpanded = true;
              btnDropdown.animate().rotation(0f).setDuration(150).start();
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
              isExpanded = false;
              btnDropdown.animate().rotation(180f).setDuration(150).start();
            } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
              dismiss();
            }
          }

          @Override
          public void onSlide(@NonNull View sheetView, float slideOffset) {
            if (sheetBackground == null) return;
            float clamped = Math.max(0f, Math.min(1f, slideOffset));
            float radiusPx = dpToPx(MAX_CORNER_RADIUS_DP) * (1f - clamped);
            sheetBackground.setCornerRadii(
                new float[] {radiusPx, radiusPx, radiusPx, radiusPx, 0, 0, 0, 0});
          }
        });

    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
  }

  @Override
  public void onDestroyView() {
    if (progressAnimator != null) {
      progressAnimator.cancel();
    }
    if (webView != null) {
      webView.stopLoading();
      webView.setWebViewClient(null);
      webView.setWebChromeClient(null);
    }
    super.onDestroyView();
  }

  public void setSheetBackgroundColor(int color) {
    this.backgroundColor = color;
    this.pendingSheetBgColor = color;
    Dialog dialog = getDialog();
    if (dialog instanceof BottomSheetDialog) {
      BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
      
      FrameLayout bottomSheet =
          bottomSheetDialog.findViewById(R.id.design_bottom_sheet);
      if (bottomSheet != null) {
        bottomSheet.setBackgroundTintList(ColorStateList.valueOf(color));
      }
    }
  }

  public void setHeaderTextColor(int color) {
    pendingHeaderTextColor = color;
    if (tvHeaderText != null) {
      tvHeaderText.setTextColor(color);
    }
  }

  public void setIconColorFilter(int color) {
    pendingIconColor = color;
    pendingClearIconColor = false;
    if (ivIcon != null) {
      ivIcon.setColorFilter(color);
    }
  }

  public void clearIconColorFilter() {
    pendingClearIconColor = true;
    pendingIconColor = null;
    if (ivIcon != null) {
      ivIcon.clearColorFilter();
    }
  }

  public void setCloseIconTint(int color) {
    pendingCloseIconTint = color;
    if (btnClose != null) {
      btnClose.setColorFilter(color);
    }
  }

  public void setDropdownIconTint(int color) {
    pendingDropdownIconTint = color;
    if (btnDropdown != null) {
      btnDropdown.setColorFilter(color);
    }
  }

  public void setMenuIconTint(int color) {
    pendingMenuIconTint = color;
    if (btnMenu != null) {
      btnMenu.setColorFilter(color);
    }
  }

  public void setProgressBarColor(int color) {
    pendingProgressBarColor = color;
    if (progressBar != null && progressBar.getProgressDrawable() != null) {
      progressBar
          .getProgressDrawable()
          .setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
    }
  }
}
