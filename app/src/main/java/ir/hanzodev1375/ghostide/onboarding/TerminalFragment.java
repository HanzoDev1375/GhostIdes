package ir.hanzodev1375.ghostide.onboarding;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.termux.terminal.TerminalSession;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.databinding.FragmentTerminalBinding;
import ir.hanzodev1375.ghostide.terminal.GhostTerminalViewClient;
import ir.hanzodev1375.ghostide.terminal.TerminalColorsUtil;
import ir.theme.M3Theme;

public class TerminalFragment extends Fragment implements GhostTerminalViewClient.KeyModifierState {

  private static final int TERM_BG_FALLBACK = 0xFF0F0F13;

  private FragmentTerminalBinding binding;
  private OnboardingViewModel viewModel;
  private boolean ctrlToggled = false;
  private boolean altToggled = false;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentTerminalBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);
    styleColors();
    setupTerminalView();
    viewModel.setInvalidator(() -> binding.terminalView.invalidate());

    binding.btnInstall.setOnClickListener(v -> viewModel.startInstall());
    binding.btnSkip.setOnClickListener(v -> viewModel.next());

    viewModel.isTerminalInstalling().observe(getViewLifecycleOwner(), this::renderInstalling);
    viewModel.isTerminalExtracting().observe(getViewLifecycleOwner(), el -> updateProgress());
    viewModel
        .getTerminalProgress()
        .observe(getViewLifecycleOwner(), p -> updateProgress());
    viewModel
        .getTerminalStatus()
        .observe(getViewLifecycleOwner(), status -> binding.tvInstallStatus.setText(status));
    viewModel
        .getActiveSession()
        .observe(getViewLifecycleOwner(), this::attachSessionToView);
    viewModel
        .isTerminalDone()
        .observe(
            getViewLifecycleOwner(),
            done -> {
              if (Boolean.TRUE.equals(done)) renderDone();
            });

    if (viewModel.isDebianInstalled()) {
      viewModel.setTerminalDone(true);
      viewModel.startDebianSession();
    } else {
      viewModel.ensureInstallSession();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (binding != null) animateIn();
  }

  private void animateIn() {
    OnboardingAnim.slideUpFade(binding.cardWarning, 0);
    OnboardingAnim.revealCard(binding.cardTerminal, 120);
    OnboardingAnim.slideUpFade(binding.tvDescription, 240);
    OnboardingAnim.slideUpFadeBounce(binding.btnInstall, 340);
    OnboardingAnim.pop(binding.btnSkip, 430);
  }

  private void setupTerminalView() {
    binding.terminalView.setTerminalViewClient(
        new GhostTerminalViewClient(binding.terminalView, this));
    TerminalColorsUtil.apply(requireActivity(), requireContext());
    int bg = fallback(M3Theme.surfaceContainerLow(), TERM_BG_FALLBACK);
    binding.terminalView.setBackgroundColor(bg);
  }

  private void attachSessionToView(TerminalSession session) {
    if (session == null) return;
    if (binding != null && binding.terminalView.mTermSession != session) {
      binding.terminalView.attachSession(session);
    }
  }

  private void styleColors() {
    int fallback = 0xFF202020;
    binding.cardWarning.setCardBackgroundColor(fallback(M3Theme.errorContainer(), fallback));
    binding.ivWarning.setColorFilter(fallback(M3Theme.onErrorContainer(), fallback));
    binding.tvWarning.setTextColor(fallback(M3Theme.onErrorContainer(), fallback));
    binding.cardTerminal.setCardBackgroundColor(fallback(M3Theme.surfaceContainerHigh(), 0xFF16151B));
    binding.cardTerminal.setStrokeColor(
        ColorStateList.valueOf(fallback(M3Theme.outlineVariant(), 0xFF38343C)));
    binding.tvDescription.setTextColor(fallback(M3Theme.onSurfaceVariant(), fallback));
    binding.tvInstallStatus.setTextColor(fallback(M3Theme.onSurfaceVariant(), fallback));
    binding.btnInstall.setBackgroundTintList(
        ColorStateList.valueOf(fallback(M3Theme.primary(), fallback)));
    binding.btnInstall.setTextColor(fallback(M3Theme.onPrimary(), fallback));
    binding.btnSkip.setStrokeColor(
        ColorStateList.valueOf(fallback(M3Theme.outline(), fallback)));
    binding.btnSkip.setTextColor(fallback(M3Theme.onSurface(), fallback));
    binding.progressInstall.setIndicatorColor(fallback(M3Theme.primary(), fallback));
    binding.progressInstall.setTrackColor(fallback(M3Theme.surfaceContainerHighest(), fallback));
  }

  private void updateProgress() {
    if (binding == null) return;
    Boolean extracting = viewModel.isTerminalExtracting().getValue();
    Integer progress = viewModel.getTerminalProgress().getValue();
    binding.progressInstall.setIndeterminate(Boolean.TRUE.equals(extracting));
    if (!Boolean.TRUE.equals(extracting) && progress != null) {
      binding.progressInstall.setProgress(progress);
    }
  }

  private void renderInstalling(boolean installing) {
    if (binding == null) return;
    if (installing) {
      binding.btnInstall.setEnabled(false);
      binding.btnSkip.setEnabled(false);
      binding.btnInstall.setText(R.string.onboarding_terminal_installing);
      binding.btnSkip.setText(R.string.onboarding_terminal_skip);
      binding.progressInstall.setVisibility(View.VISIBLE);
      binding.tvInstallStatus.setVisibility(View.VISIBLE);
      updateProgress();
    } else if (!Boolean.TRUE.equals(viewModel.isTerminalDone().getValue())) {
      renderReady();
    }
  }

  private void renderReady() {
    if (binding == null) return;
    binding.btnInstall.setEnabled(true);
    binding.btnInstall.setText(R.string.onboarding_terminal_install);
    binding.btnSkip.setEnabled(true);
    binding.btnSkip.setText(R.string.onboarding_terminal_skip);
    binding.progressInstall.setVisibility(View.GONE);
    binding.tvInstallStatus.setVisibility(View.GONE);
  }

  private void renderDone() {
    if (binding == null) return;
    binding.btnInstall.setEnabled(false);
    binding.btnInstall.setText(R.string.onboarding_terminal_installed);
    binding.btnSkip.setEnabled(true);
    binding.btnSkip.setText(R.string.onboarding_terminal_continue);
    binding.progressInstall.setVisibility(View.GONE);
    binding.tvInstallStatus.setVisibility(View.GONE);
    OnboardingAnim.bounceOnce(binding.btnInstall, 0);
    animatePulseDots();
  }

  private void animatePulseDots() {
    binding.btnSkip.animate().cancel();
    binding.btnSkip
        .animate()
        .scaleX(1.05f)
        .scaleY(1.05f)
        .setDuration(250)
        .withEndAction(
            () ->
                binding.btnSkip
                    .animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .start())
        .start();
  }

  @Override
  public void onDestroyView() {
    if (binding != null) binding.terminalView.setTerminalViewClient(null);
    super.onDestroyView();
    binding = null;
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }

  @Override
  public boolean isCtrlToggled() {
    return ctrlToggled;
  }

  @Override
  public boolean isAltToggled() {
    return altToggled;
  }

  @Override
  public void consumeCtrlToggle() {
    ctrlToggled = false;
  }

  @Override
  public void consumeAltToggle() {
    altToggled = false;
  }
}