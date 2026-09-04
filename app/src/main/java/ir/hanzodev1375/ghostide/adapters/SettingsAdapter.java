package ir.hanzodev1375.ghostide.adapters;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.customui.PreferenceSwitchGroup;
import ir.hanzodev1375.ghostide.models.SettingItem;
import ir.theme.M3Theme;
import java.util.ArrayList;
import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.ViewHolder> {

  private final List<SettingItem> items;
  private final List<SettingItem> backupList;
  private OnItemClickListener listener;
  private String currentQuery = "";

  public interface OnItemClickListener {
    void onItemClick(int position);
  }

  public SettingsAdapter(List<SettingItem> items) {
    this.items = new ArrayList<>(items);
    this.backupList = new ArrayList<>(items);
  }

  public void setOnItemClickListener(OnItemClickListener listener) {
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    PreferenceSwitchGroup view = new PreferenceSwitchGroup(parent.getContext());
    view.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    return new ViewHolder(view);
  }

  public void updateItem(int position, SettingItem newItem) {
    items.set(position, newItem);
    notifyItemChanged(position);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    SettingItem item = items.get(position);
    holder.switchGroup.setListPosition(position, getItemCount());

    // Set plain text first (fast)
    holder.switchGroup.setTitle(item.getTitle());
    if (item.getDescription() != null && !item.getDescription().isEmpty()) {
      holder.switchGroup.setDescription(item.getDescription());
    }

    if (currentQuery != null && !currentQuery.isEmpty()) {
      String text = item.getTitle();
      String desc = item.getDescription();
      String query = currentQuery;

      holder.itemView.post(
          () -> {
            int bindingPos = holder.getBindingAdapterPosition();
            if (bindingPos == RecyclerView.NO_POSITION) {
              return;
            }

            if (bindingPos >= items.size() || items.get(bindingPos) != item) {
              return;
            }

            SpannableString highlightedTitle = highlightText(text, query, holder);
            holder.switchGroup.setTitle(highlightedTitle);

            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
              SpannableString highlightedDesc = highlightText(desc, query, holder);
              holder.switchGroup.setDescription(highlightedDesc);
            }
          });
    } else {
      holder.switchGroup.setTitle(item.getTitle());
      if (item.getDescription() != null && !item.getDescription().isEmpty()) {
        holder.switchGroup.setDescription(item.getDescription());
      }
    }

    if (item.getIconRes() != 0) {
      holder.switchGroup.setIcon(item.getIconRes());
    }
    holder.switchGroup.setValue(item.isChecked());
    holder.switchGroup.getSwitch().setOnCheckedChangeListener(null);
    if (item.getListener() != null) {
      holder.switchGroup.getSwitch().setVisibility(android.view.View.VISIBLE);
      holder.switchGroup.setSwitchChangedListener(
          (button, isChecked) -> {
            item.setChecked(isChecked);
            item.getListener().onCheckedChanged(isChecked);
          });
      holder.switchGroup.setOnClickListener(holder.switchGroup);
    } else {
      holder.switchGroup.getSwitch().setVisibility(android.view.View.GONE);
      holder.switchGroup.setOnClickListener(
          v -> {
            if (listener != null) {
              int originalPosition = backupList.indexOf(item);
              listener.onItemClick(originalPosition);
            }
          });
    }
    M3Theme.listCard(holder.itemView);
  }

  private SpannableString highlightText(String text, String query, ViewHolder holder) {
    SpannableString spannableString = new SpannableString(text);
    String lowerText = text.toLowerCase();
    int startIndex = lowerText.indexOf(query);

    if (startIndex == -1) {
      return new SpannableString(text); // No highlight needed
    }

    while (startIndex != -1) {
      int endIndex = startIndex + query.length();
      spannableString.setSpan(
          new ForegroundColorSpan(
              fallback(M3Theme.error(), 0)),
          startIndex,
          endIndex,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      startIndex = lowerText.indexOf(query, endIndex);
    }
    return spannableString;
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  public void resetToFull() {
    filter("");
  }

  public SettingItem getItemAtPosition(int position) {
    return items.get(position);
  }

  public void filter(String query) {
    String newQuery = query != null ? query.toLowerCase().trim() : "";

    // If query changed, force rebind by treating as different items
    boolean queryChanged = !newQuery.equals(currentQuery);
    currentQuery = newQuery;

    List<SettingItem> newList;
    if (currentQuery.isEmpty()) {
      newList = new ArrayList<>(backupList);
    } else {
      newList = new ArrayList<>();
      for (SettingItem item : backupList) {
        if (item.getTitle().toLowerCase().contains(currentQuery)
            || (item.getDescription() != null
                && item.getDescription().toLowerCase().contains(currentQuery))) {
          newList.add(item);
        }
      }
    }

    DiffUtil.DiffResult diffResult =
        DiffUtil.calculateDiff(
            new DiffUtil.Callback() {
              @Override
              public int getOldListSize() {
                return items.size();
              }

              @Override
              public int getNewListSize() {
                return newList.size();
              }

              @Override
              public boolean areItemsTheSame(int oldPos, int newPos) {
                // Force rebind if query changed
                if (queryChanged) return false;
                return items.get(oldPos).getTitle().equals(newList.get(newPos).getTitle());
              }

              @Override
              public boolean areContentsTheSame(int oldPos, int newPos) {
                return false; // Always rebind to update highlight
              }
            });

    items.clear();
    items.addAll(newList);
    diffResult.dispatchUpdatesTo(this);
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    PreferenceSwitchGroup switchGroup;

    ViewHolder(PreferenceSwitchGroup itemView) {
      super(itemView);
      switchGroup = itemView;
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
