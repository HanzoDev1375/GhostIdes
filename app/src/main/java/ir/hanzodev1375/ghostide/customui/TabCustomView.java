package ir.hanzodev1375.ghostide.customui;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.SpannableString;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import ir.hanzodev1375.ghostide.materialfileicon.core.FileIconHelper;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.Constants;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.databinding.CustomTabBinding;
import ir.hanzodev1375.ghostide.models.TabModel;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;

public class TabCustomView extends LinearLayout
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  private CustomTabBinding binding;
  private PreferencesUtils setting;
  private TabModel currentModel;
  private boolean showTabIcon = false;
  private boolean gitChanged = false;
  private final int normalTitleColor;
  private final int gitModifiedTitleColor;
  private boolean hasStar;

  public TabCustomView(Context context) {
    super(context);
    binding = CustomTabBinding.inflate(LayoutInflater.from(context));
    removeAllViews();
    if (binding != null) {
      addView(binding.getRoot());
    }
    normalTitleColor = binding.tabTitle.getCurrentTextColor();
    gitModifiedTitleColor = ContextCompat.getColor(context, R.color.tab_git_modified);
    setting = new PreferencesUtils(getContext());
    showTabIcon = setting.getShowIconTab();
    setting.getDefaultPreferences().registerOnSharedPreferenceChangeListener(this);
    var theme = new ThemeManager(context);
    var themeutil = new ThemeUtils(theme);
    themeutil.applyImageView(binding.tabPinIcon);
    binding.tabIcon.clearColorFilter();
    themeutil.applyImageView(binding.tabIcon);
  }

  public void bind(TabModel tabModel) {
    this.currentModel = tabModel;
    this.hasStar = tabModel.getHasStar();
    updateTitleText();
    binding.tabPinIcon.setVisibility(tabModel.isPinned() ? View.VISIBLE : View.GONE);

    FileIconHelper icon = new FileIconHelper(tabModel.getFilePath());
    icon.setDynamicFolderEnabled(false);
    icon.setEnvironmentEnabled(false);

    if (showTabIcon) {
      binding.tabIcon.setVisibility(View.VISIBLE);
      icon.bindIcon(binding.tabIcon);
    } else {
      binding.tabIcon.setVisibility(View.GONE);
    }

    updateGitTextColor();
  }

  public void setGitChanged(boolean changed) {
    if (this.gitChanged == changed) return;
    this.gitChanged = changed;
    updateGitTextColor();
  }

  public boolean isGitChanged() {
    return gitChanged;
  }

  private void updateGitTextColor() {
    if (binding == null || binding.tabTitle == null) return;
    binding.tabTitle.setTextColor(gitChanged ? gitModifiedTitleColor : normalTitleColor);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(Constants.SharedPreferenceKeys.KEY_SHOWTAB_ICON)) {
      showTabIcon = setting.getShowIconTab();
      if (currentModel != null) {
        bind(currentModel);
      }
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (setting != null) {
      setting.getDefaultPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
  }

  public boolean getHasStar() {
    return this.hasStar;
  }

  public void setHasStar(boolean hasStar) {
    if (this.hasStar == hasStar) return;
    this.hasStar = hasStar;
    updateTitleText();
  }

  private void updateTitleText() {
    if (binding == null || binding.tabTitle == null || currentModel == null) return;
    String displayName = hasStar ? "*" + currentModel.getFileName() : currentModel.getFileName();
    binding.tabTitle.setText(displayName);
    applyErrorSpan();
  }

  public void setHasErrors(boolean hasError) {
    if (currentModel != null) {
      currentModel.setHasError(hasError);
    }
    applyErrorSpan();
  }

  private void applyErrorSpan() {
    if (binding == null || binding.tabTitle == null || currentModel == null) return;
    boolean hasError = currentModel.getHasError();
    String text = binding.tabTitle.getText().toString();
    SpannableString spannable = new SpannableString(text);
    if (hasError) {
      WavyUnderlineSpan waveSpan = new WavyUnderlineSpan(WavyUnderlineSpan.StatosMod.ERROR);
      waveSpan.setEnabled(true);
      spannable.setSpan(waveSpan, 0, text.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    binding.tabTitle.setText(spannable);
  }
}
