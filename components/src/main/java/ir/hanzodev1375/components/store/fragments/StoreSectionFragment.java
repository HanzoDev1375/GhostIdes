package ir.hanzodev1375.components.store.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import ir.hanzodev1375.components.R;
import ir.theme.M3Theme;

public class StoreSectionFragment extends Fragment {

  private static final String ARG_ICON = "arg_icon";
  private static final String ARG_TITLE = "arg_title";
  private static final String ARG_DESC = "arg_desc";

  public static StoreSectionFragment newInstance(int iconRes, String title, String description) {
    StoreSectionFragment fragment = new StoreSectionFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_ICON, iconRes);
    args.putString(ARG_TITLE, title);
    args.putString(ARG_DESC, description);
    fragment.setArguments(args);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_store_section, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Bundle args = getArguments();
    if (args == null) return;

    int iconRes = args.getInt(ARG_ICON, R.drawable.ic_outline_project);
    String title = args.getString(ARG_TITLE, getString(R.string.store_section_title));
    String desc = args.getString(ARG_DESC, "");

    ImageView icon = view.findViewById(R.id.emptyIcon);
    TextView titleView = view.findViewById(R.id.emptyTitle);
    TextView descView = view.findViewById(R.id.emptyDescription);
    View iconContainer = view.findViewById(R.id.iconContainer);

    icon.setImageResource(iconRes);
    titleView.setText(title);
    if (desc == null || desc.isEmpty()) {
      descView.setVisibility(View.GONE);
    } else {
      descView.setText(desc);
    }

    int circle =
        fallback(
            M3Theme.surfaceContainerHighest(),
            0xFFEEEEEE);
    iconContainer.setBackgroundResource(R.drawable.bg_store_icon_circle);
    iconContainer.getBackground().setTint(androidx.core.graphics.ColorUtils.setAlphaComponent(circle, 120));
    M3Theme.applyTopLevel(view);
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
