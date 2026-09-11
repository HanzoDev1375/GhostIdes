package ir.hanzodev1375.ghostide.terminal;

import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.databinding.FragmentTerminalSessionBinding;
import ir.hanzodev1375.ghostide.terminal.activity.TerminalActivity;
import ir.theme.GhostTheme;
import ir.theme.M3Theme;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;

public class TerminalSessionFragment extends Fragment
    implements GhostTerminalViewClient.KeyModifierState {

  private static final String ARG_SESSION_ID = "session_id";

  private FragmentTerminalSessionBinding binding;
  private TerminalViewModel viewModel;
  private int sessionId = -1;

  public static TerminalSessionFragment newInstance(int sessionId) {
    TerminalSessionFragment fragment = new TerminalSessionFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_SESSION_ID, sessionId);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      sessionId = getArguments().getInt(ARG_SESSION_ID, -1);
    }
    viewModel = new ViewModelProvider(requireActivity()).get(TerminalViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentTerminalSessionBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupTerminalView();
    attachSession();
  }

  private void setupTerminalView() {
    binding.terminalView.setTerminalViewClient(
        new GhostTerminalViewClient(binding.terminalView, this));
    TerminalColorsUtil.apply(requireActivity(), requireContext());
    applyTerminalBackground();

    binding.terminalView.setOnTouchListener(
        (v, event) -> {
          detectTerminalSwipe(event);
          return false;
        });
  }

  private void applyTerminalBackground() {
    if (binding == null || binding.terminalView == null) return;
    boolean hasImage = false;
    try {
      GhostTheme theme = new ThemeUtils(new ThemeManager(requireContext())).getTheme();
      hasImage =
          new PreferencesUtils(requireContext()).isShowBackground()
              && theme != null
              && theme.getWidget() != null
              && theme.getWidget().getImagepath() != null
              && !theme.getWidget().getImagepath().isEmpty();
    } catch (Throwable ignored) {
    }
    if (hasImage) {
      binding.terminalView.setBackgroundColor(Color.TRANSPARENT);
    } else {
      Integer bg = M3Theme.surfaceContainerLow();
      if (bg == null) bg = M3Theme.surface();
      binding.terminalView.setBackgroundColor(bg != null ? bg : 0xFF121212);
    }
  }

  public void attachSession() {
    if (binding == null) return;
    TerminalSession session = viewModel.getSessionById(sessionId);
    if (session == null) return;
    TerminalColorsUtil.refreshSession(session);
    binding.terminalView.attachSession(session);
    binding.terminalView.invalidate();
    viewModel.registerScreenRefresher(
        sessionId,
        () -> {
          if (binding != null) binding.terminalView.onScreenUpdated();
        });
  }

  public TerminalView getTerminalView() {
    return binding != null ? binding.terminalView : null;
  }

  public void sendKeyEvent(int keyCode) {
    if (binding == null || binding.terminalView == null) return;
    long now = SystemClock.uptimeMillis();
    binding.terminalView.dispatchKeyEvent(
        new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
    binding.terminalView.dispatchKeyEvent(
        new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
  }

  private float terminalDownX;
  private float terminalDownY;
  private long terminalDownTime;

  private static final int TERMINAL_SWIPE_MAX_MS = 350;

  private void detectTerminalSwipe(MotionEvent event) {
    int touchSlop = ViewConfiguration.get(requireContext()).getScaledTouchSlop();
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        terminalDownX = event.getRawX();
        terminalDownY = event.getRawY();
        terminalDownTime = event.getEventTime();
        break;
      case MotionEvent.ACTION_UP:
        if (event.getEventTime() - terminalDownTime > TERMINAL_SWIPE_MAX_MS) break;
        float dx = event.getRawX() - terminalDownX;
        float dy = event.getRawY() - terminalDownY;
        if (Math.abs(dx) <= touchSlop * 2 || Math.abs(dx) <= Math.abs(dy) * 1.6f) break;
        if (getActivity() instanceof TerminalActivity) {
          TerminalActivity activity = (TerminalActivity) getActivity();
          if (dx < 0) {
            activity.getInputDock().showInputPage();
          } else {
            activity.getInputDock().showButtonsPage();
          }
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (binding != null) {
      binding.terminalView.post(
          () -> {
            if (binding != null && binding.terminalView != null) {
              binding.terminalView.setTerminalCursorBlinkerState(true, false);
              binding.terminalView.onScreenUpdated();
              binding.terminalView.requestFocus();
            }
          });
    }
  }

  @Override
  public void onPause() {
    if (binding != null && binding.terminalView != null) {
      binding.terminalView.setTerminalCursorBlinkerState(false, false);
    }
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    viewModel.unregisterScreenRefresher(sessionId);
    if (binding != null) binding.terminalView.setTerminalViewClient(null);
    super.onDestroyView();
    binding = null;
  }

  @Override
  public boolean isCtrlToggled() {
    return viewModel.isCtrlToggled();
  }

  @Override
  public boolean isAltToggled() {
    return viewModel.isAltToggled();
  }

  @Override
  public void consumeCtrlToggle() {
    viewModel.consumeCtrlToggle();
  }

  @Override
  public void consumeAltToggle() {
    viewModel.consumeAltToggle();
  }
}