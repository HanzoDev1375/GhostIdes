package ir.hanzodev1375.ghostide.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import ir.hanzodev1375.components.utils.RoundedCornersTransformation;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.utils.FileUtil;
import ir.theme.GhostTheme;
import ir.theme.M3Theme;
import ir.theme.ThemeMediaPath;
import ir.theme.WidgetTheme;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemeFilesAdapter extends RecyclerView.Adapter<ThemeFilesAdapter.ViewHolder> {

  public interface OnFileClickListener {
    void onFileClick(File file);
  }

  public interface OnFileLongClickListener {
    void onFileLongClick(File file, ViewHolder holder);
  }

  private final List<File> files;
  private final Map<String, ThemeData> dataCache = new HashMap<>();
  private final OnFileClickListener listener;
  private OnFileLongClickListener longClickListener;
  private String selectedPath = "";

  public ThemeFilesAdapter(List<File> files, OnFileClickListener listener) {
    this.files = files;
    this.listener = listener;
  }

  public void setOnFileLongClickListener(OnFileLongClickListener listener) {
    this.longClickListener = listener;
  }

  public void setSelectedPath(String selectedPath) {
    this.selectedPath = selectedPath != null ? selectedPath : "";
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme_grid, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    File file = files.get(position);
    ThemeData data = getThemeData(file);

    String displayName = file.getName();
    if (displayName.toLowerCase().endsWith(".gth")) {
      displayName = displayName.substring(0, displayName.length() - 4);
    }
    holder.name.setText(displayName);

    boolean selected = file.getAbsolutePath().equals(selectedPath);
    holder.checked.setVisibility(selected ? View.VISIBLE : View.GONE);

    if (data.hasImage && data.imagePath != null && !data.imagePath.isEmpty()) {
      holder.icon.setBackground(null);
      holder.icon.setColorFilter(null);
      Glide.with(holder.icon.getContext())
      .load(data.imagePath)
      .transform(new RoundedCornersTransformation(26))
      .override(112,122)
      .into(holder.icon);
    } else {
      GradientDrawable gradient =
          new GradientDrawable(
              GradientDrawable.Orientation.TL_BR,
              new int[] {data.primaryColor, data.secondaryColor, data.tertiaryColor});
      gradient.setCornerRadius(28);
      gradient.setStroke(2, 0x33FFFFFF);
      holder.icon.setBackground(gradient);
      holder.icon.setColorFilter(null);
      Glide.with(holder.icon.getContext()).clear(holder.icon);
      holder.icon.setImageDrawable(null);
    }

    holder.itemView.setOnClickListener(
        v -> {
          if (listener != null) listener.onFileClick(file);
        });
    holder.itemView.setOnLongClickListener(
        v -> {
          if (longClickListener != null) {
            longClickListener.onFileLongClick(file, holder);
            return true;
          }
          return false;
        });
  }

  @Override
  public int getItemCount() {
    return files.size();
  }

  public void refresh(List<File> newFiles) {
    files.clear();
    files.addAll(newFiles);
    dataCache.clear();
    notifyDataSetChanged();
  }

  private ThemeData getThemeData(File file) {
    ThemeData cached = dataCache.get(file.getAbsolutePath());
    if (cached != null) return cached;

    int primary = 0xFF4C4C4C;
    int secondary = 0xFF4C4C4C;
    int tertiary = 0xFF4C4C4C;
    boolean hasImage = false;
    String imagePath = null;

    try {
      String json = new String(FileUtil.readBytesCompat(file), StandardCharsets.UTF_8);
      GhostTheme theme = new Gson().fromJson(json, GhostTheme.class);
      if (theme != null) {
        String accent = theme.getWidget() != null ? theme.getWidget().getAccent() : null;

        Integer m3Primary = null;
        Integer m3Secondary = null;
        Integer m3Tertiary = null;
        if (theme.getMaterial3() != null) {
          m3Primary = M3Theme.color(theme.getMaterial3().getPrimary());
          m3Secondary = M3Theme.color(theme.getMaterial3().getSecondary());
          m3Tertiary = M3Theme.color(theme.getMaterial3().getTertiary());
        }

        primary = firstColor(m3Primary, M3Theme.color(accent), primary);
        secondary = firstColor(m3Secondary, m3Primary, M3Theme.color(accent), secondary);
        tertiary = firstColor(m3Tertiary, m3Secondary, m3Primary, tertiary);

        WidgetTheme w = theme.getWidget();
        if (w != null && w.getImagepath() != null && !w.getImagepath().isEmpty()) {
          hasImage = true;
          imagePath = ThemeMediaPath.resolve(file.getAbsolutePath(), w.getImagepath());
        }
      }
    } catch (Exception ignored) {
    }

    ThemeData data = new ThemeData(primary, secondary, tertiary, hasImage, imagePath);
    dataCache.put(file.getAbsolutePath(), data);
    return data;
  }

  private static int firstColor(Integer... colors) {
    for (Integer c : colors) {
      if (c != null) return c;
    }
    return colors.length > 0 ? colors[colors.length - 1] : 0xFF4C4C4C;
  }

  static class ThemeData {
    final int primaryColor;
    final int secondaryColor;
    final int tertiaryColor;
    final boolean hasImage;
    final String imagePath;

    ThemeData(
        int primaryColor,
        int secondaryColor,
        int tertiaryColor,
        boolean hasImage,
        String imagePath) {
      this.primaryColor = primaryColor;
      this.secondaryColor = secondaryColor;
      this.tertiaryColor = tertiaryColor;
      this.hasImage = hasImage;
      this.imagePath = imagePath;
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    final ImageView icon;
    final ImageView checked;
    final TextView name;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      icon = itemView.findViewById(R.id.theme_icon);
      checked = itemView.findViewById(R.id.theme_checked);
      name = itemView.findViewById(R.id.theme_name);
    }
  }
}
