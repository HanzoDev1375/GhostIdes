package ir.hanzodev1375.ghostide.onboarding;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.example.liquidglass.GlassMaterial;
import com.example.liquidglass.LiquidGlassFab;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.activity.FileManagerActivity;
import ir.hanzodev1375.ghostide.databinding.ActivityOnboardingBinding;
import ir.theme.M3Theme;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends BaseCompat {

  private static final int UI_FALLBACK = 0xFF17161A;
  private static final int BTN_BG_FALLBACK = 0xFF2A2830;
  private static final int DOT_ACTIVE_FALLBACK = 0xFF6750A4;
  private static final int DOT_INACTIVE_FALLBACK = 0xFF44343C;

  private ActivityOnboardingBinding bind;
  private OnboardingViewModel viewModel;
  private boolean syncingPager = false;
  private final List<View> dots = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    bind = ActivityOnboardingBinding.inflate(getLayoutInflater());
    setContentView(bind.getRoot());

    viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
    setupBackgroundBlur(bind.backgroundOnboarding, bind.contentContainer);
    styleUi();
    M3Theme.refreshOnThemeChange(bind.getRoot());
    applyEdgeToEdgeInsets();
    buildDots();

    bind.pager.setAdapter(new OnboardingPagerAdapter());
    bind.pager.registerOnPageChangeCallback(
        new ViewPager2.OnPageChangeCallback() {
          @Override
          public void onPageSelected(int position) {
            super.onPageSelected(position);
            if (!syncingPager) viewModel.setPage(position);
          }
        });

    bind.btnBack.setOnClickListener(v -> viewModel.back());
    bind.btnNext.setOnClickListener(
        v -> {
          Integer page = viewModel.getPage().getValue();
          if (page != null && page >= OnboardingViewModel.PAGE_COUNT - 1) {
            finishOnboarding();
          } else {
            viewModel.next();
          }
        });

    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                Integer page = viewModel.getPage().getValue();
                if (page != null && page > 0) viewModel.back();
                else finish();
              }
            });

    viewModel.getPage().observe(this, this::onPageChanged);
    onPageChanged(viewModel.getPage().getValue() == null ? 0 : viewModel.getPage().getValue());
  }

  private void styleUi() {
    bind.contentContainer.setBackgroundColor(fallback(M3Theme.surface(), UI_FALLBACK));
    bind.divider.setBackgroundColor(fallback(M3Theme.outlineVariant(), 0xFF38343C));
    styleGlassFab(bind.btnBack);
    styleGlassFab(bind.btnNext);
  }

  private void styleGlassFab(LiquidGlassFab fab) {
    fab.setEnableDynamicBackground(true);
    fab.setMaterial(GlassMaterial.REGULAR);
    int darkTint = fallback(M3Theme.surfaceContainerHighest(), BTN_BG_FALLBACK);
    fab.setGlassTint(darkTint, 0.88f);
    fab.setIconTint(fallback(M3Theme.onSurface(), 0xFFE6E1E5));
  }

  private void buildDots() {
    dots.clear();
    bind.dotsHost.removeAllViews();
    for (int i = 0; i < OnboardingViewModel.PAGE_COUNT; i++) {
      View dot = new View(this);
      dot.setLayoutParams(new LinearLayout.LayoutParams(dp(6), dp(6)));
      bind.dotsHost.addView(dot);
      dots.add(dot);
    }
    Integer page = viewModel.getPage().getValue();
    updateDots(page == null ? 0 : page);
  }

  private void updateDots(int page) {
    if (dots.isEmpty()) return;
    int active = fallback(M3Theme.primary(), DOT_ACTIVE_FALLBACK);
    int inactive = fallback(M3Theme.surfaceContainerHighest(), DOT_INACTIVE_FALLBACK);
    for (int i = 0; i < dots.size(); i++) {
      View dot = dots.get(i);
      GradientDrawable shape = new GradientDrawable();
      shape.setShape(GradientDrawable.OVAL);
      shape.setColor(i == page ? active : inactive);
      dot.setBackground(shape);
      LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
      lp.width = i == page ? dp(24) : dp(8);
      lp.height = dp(8);
      lp.setMarginEnd(dp(6));
      dot.setLayoutParams(lp);
    }
  }

  private void applyEdgeToEdgeInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(
        bind.getRoot(),
        (v, insets) -> {
          Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          bind.contentContainer.setPadding(0, bars.top, 0, 0);
          FrameLayout bar = bind.bottomBar;
          bar.setPadding(
              bar.getPaddingLeft(),
              bar.getPaddingTop(),
              bar.getPaddingRight(),
              bars.bottom + dp(16));
          return insets;
        });
  }

  private void onPageChanged(int page) {
    syncingPager = true;
    bind.pager.setCurrentItem(page, true);
    syncingPager = false;
    updateDots(page);
    animateNav(page);
    bind.btnBack.setContentDescription(
        getString(
            page >= OnboardingViewModel.PAGE_COUNT - 1
                ? R.string.onboarding_done
                : R.string.onboarding_back));
    bind.btnNext.setContentDescription(
        getString(
            page >= OnboardingViewModel.PAGE_COUNT - 1
                ? R.string.onboarding_done
                : R.string.onboarding_next));
  }

  private void animateNav(int page) {
    bind.btnBack.setEnabled(page > 0);
    bind.btnBack.animate().cancel();
    bind.btnNext.animate().cancel();
    bind.btnBack.animate().alpha(page > 0 ? 1f : 0.35f).setDuration(250).start();
    bind.btnNext.animate().scaleX(1.12f).scaleY(1.12f).setDuration(220).withEndAction(
        () -> bindingScaleBack(bind.btnNext)).start();
    OnboardingAnim.shimmer(bind.dotsHost);
  }

  private void bindingScaleBack(com.example.liquidglass.LiquidGlassFab fab) {
    fab.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private void finishOnboarding() {
    OnboardingPrefs.markCompleted(this);
    startActivity(new Intent(this, FileManagerActivity.class));
    finish();
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }

  private final class OnboardingPagerAdapter extends FragmentStateAdapter {
    OnboardingPagerAdapter() {
      super(OnboardingActivity.this);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
      switch (position) {
        case OnboardingViewModel.PAGE_TERMINAL:
          return new TerminalFragment();
        case OnboardingViewModel.PAGE_FEATURES:
          return new FeaturesFragment();
        default:
          return new WelcomeFragment();
      }
    }

    @Override
    public int getItemCount() {
      return OnboardingViewModel.PAGE_COUNT;
    }
  }
}