package ir.hanzodev1375.ghostide.onboarding;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.databinding.FragmentFeaturesBinding;
import ir.theme.M3Theme;

public class FeaturesFragment extends Fragment {

  private FragmentFeaturesBinding binding;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentFeaturesBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    int fallback = 0xFF202020;

    binding.cardIcon.setCardBackgroundColor(fallback(M3Theme.surfaceContainerHigh(), fallback));
    binding.ivIcon.setColorFilter(fallback(M3Theme.primary(), fallback));
    binding.tvHeader.setTextColor(fallback(M3Theme.onSurface(), fallback));
    binding.tvDesc.setTextColor(fallback(M3Theme.onSurfaceVariant(), fallback));
    binding.cardList.setCardBackgroundColor(fallback(M3Theme.surfaceContainerLow(), fallback));

    TextView[] features = {
      binding.tvFeatureLsp,
      binding.tvFeatureTerminal,
      binding.tvFeatureGit,
      binding.tvFeaturePlugins,
      binding.tvFeatureThemes,
      binding.tvFeatureRemote
    };
    int accent = fallback(M3Theme.primary(), fallback);
    for (TextView feature : features) {
      feature.setTextColor(fallback(M3Theme.onSurface(), fallback));
      Drawable indicator = feature.getCompoundDrawablesRelative()[0];
      if (indicator == null) indicator = feature.getCompoundDrawables()[0];
      if (indicator != null) indicator.setTint(accent);
    }

    animateIn();
  }

  private void animateIn() {
    OnboardingAnim.scaleIn(binding.cardIcon, 0);
    OnboardingAnim.slideUpFade(binding.tvHeader, 150);
    OnboardingAnim.slideUpFade(binding.tvDesc, 250);
    OnboardingAnim.revealCard(binding.cardList, 350);
    OnboardingAnim.slideRightFade(binding.tvFeatureLsp, 450);
    OnboardingAnim.slideRightFade(binding.tvFeatureTerminal, 540);
    OnboardingAnim.slideRightFade(binding.tvFeatureGit, 630);
    OnboardingAnim.slideRightFade(binding.tvFeaturePlugins, 720);
    OnboardingAnim.slideRightFade(binding.tvFeatureThemes, 810);
    OnboardingAnim.slideRightFade(binding.tvFeatureRemote, 900);
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}