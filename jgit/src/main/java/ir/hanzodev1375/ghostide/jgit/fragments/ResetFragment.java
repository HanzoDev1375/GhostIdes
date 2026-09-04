package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import ir.hanzodev1375.components.views.GhostToast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import ir.hanzodev1375.components.utils.GlassColors;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.theme.M3Theme;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.ResetMode;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class ResetFragment extends Fragment {
  private GitViewModel viewModel;
  private int stepsBack = 1;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_reset, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);

    MaterialTextView tvSteps = view.findViewById(R.id.tvStepsCount);
    MaterialButton btnMinus = view.findViewById(R.id.btnStepsMinus);
    MaterialButton btnPlus = view.findViewById(R.id.btnStepsPlus);
    RadioGroup radioGroup = view.findViewById(R.id.radioGroupReset);
    MaterialButton btnReset = view.findViewById(R.id.btnDoReset);

    GlassColors.setBackgroundAlpha(
        view.findViewById(R.id.cardResetOptions),
        fallback(M3Theme.surface(), 0),
        120);

    btnMinus.setOnClickListener(v -> {
      if (stepsBack > 1) { stepsBack--; tvSteps.setText(String.valueOf(stepsBack)); }
    });
    btnPlus.setOnClickListener(v -> {
      stepsBack++; tvSteps.setText(String.valueOf(stepsBack));
    });

    btnReset.setOnClickListener(v -> {
      ResetMode mode;
      int id = radioGroup.getCheckedRadioButtonId();
      if (id == R.id.radioSoft) mode = ResetMode.SOFT;
      else if (id == R.id.radioHard) mode = ResetMode.HARD;
      else mode = ResetMode.MIXED;

      String modeName = mode.name().substring(0, 1) + mode.name().substring(1).toLowerCase();
      new DialogCompat(requireContext())
          .setTitle(getString(R.string.reset_confirm_title))
          .setMessage(getString(R.string.reset_confirm_msg, modeName))
          .setPositiveButton(getString(R.string.reset_title), (d, w) ->
              viewModel.reset(mode, stepsBack))
          .setNegativeButton(getString(R.string.cancel), null)
          .show();
    });

    viewModel.operationResult.observe(getViewLifecycleOwner(), result -> {
      if (result != null)
        GhostToast.makeText(getContext(), result.getMessage(), GhostToast.LENGTH_SHORT).show();
    });
    M3Theme.applyTopLevel(view);
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
