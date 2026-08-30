package ir.hanzodev1375.ghostide.themeengine;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import java.util.stream.Collectors;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class ThemeChooserDialogBuilder {

  private final Context context;
  private DialogCompat builder;
  private ColorAdapter colorAdapter;
  private final List<Theme> themes = Arrays.asList(Theme.values());

  public ThemeChooserDialogBuilder(Context context) {
    this.context = context;
    createDialog();
  }

  private void createDialog() {
    View dialogView = LayoutInflater.from(context).inflate(R.layout.recyclerview, null);
    RecyclerView recyclerView =
        dialogView.findViewById(R.id.recycler_view);

    ThemeEngine themeEngine = ThemeEngine.getInstance(context);
    List<Integer> colorArray =
        themes.stream().map(Theme::getPrimaryColor).collect(Collectors.toList());
    colorAdapter = new ColorAdapter(colorArray);
    colorAdapter.setCheckedPosition(themeEngine.getStaticTheme());

    recyclerView.setLayoutManager(new GridLayoutManager(context, 4));
    recyclerView.setAdapter(colorAdapter);

    builder = new DialogCompat(context).setView(dialogView);
  }

  public ThemeChooserDialogBuilder setTitle(@StringRes int res) {
    builder.setTitle(res);
    return this;
  }

  public ThemeChooserDialogBuilder setIcon(@DrawableRes int iconId) {
    builder.setIcon(iconId);
    return this;
  }

  public ThemeChooserDialogBuilder setPositiveButton(String text, OnClickListener listener) {
    builder.setPositiveButton(
        text,
        (dialog, which) -> {
          int pos = colorAdapter.getCheckedPosition();
          listener.onClick(pos, themes.get(pos));
        });
    return this;
  }

  public ThemeChooserDialogBuilder setPositiveButton(@StringRes int res, OnClickListener listener) {
    return setPositiveButton(context.getString(res), listener);
  }

  public ThemeChooserDialogBuilder setNegativeButton(String text) {
    builder.setNegativeButton(text, null);
    return this;
  }

  public ThemeChooserDialogBuilder setNegativeButton(@StringRes int res) {
    builder.setNegativeButton(res, null);
    return this;
  }

  public ThemeChooserDialogBuilder setNeutralButton(String text, OnClickListener listener) {
    builder.setNeutralButton(
        text,
        (dialog, which) -> {
          int pos = colorAdapter.getCheckedPosition();
          listener.onClick(pos, themes.get(pos));
        });
    return this;
  }

  public ThemeChooserDialogBuilder setNeutralButton(@StringRes int res, OnClickListener listener) {
    return setNeutralButton(context.getString(res), listener);
  }

  public AlertDialog create() {
    return builder.create();
  }

  public interface OnClickListener {
    void onClick(int position, Theme theme);
  }
}
