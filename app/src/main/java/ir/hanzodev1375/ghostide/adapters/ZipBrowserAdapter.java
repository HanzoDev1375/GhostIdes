package ir.hanzodev1375.ghostide.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import androidx.core.graphics.ColorUtils;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.ghostide.materialfileicon.core.FileIconHelper;

import com.google.android.material.listitem.ListItemCardView;
import com.google.android.material.listitem.ListItemViewHolder;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.models.ZipEntryModel;
import ir.hanzodev1375.ghostide.utils.ShapeUtil;
import ir.theme.M3Theme;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZipBrowserAdapter extends RecyclerView.Adapter<ZipBrowserAdapter.ViewHolder> {

  private List<ZipEntryModel> items = new ArrayList<>();
  private List<ZipEntryModel> itemsFull = new ArrayList<>();
  private String searchQuery = "";
  private String currentZipPath = "";
  private String currentInternalPath = "";
  private static final int HIGHLIGHT_COLOR = 0xFF200180;
  private final Context context;
  private SelectionTracker<Long> selectionTracker;
  private OnItemClickListener itemClickListener;
  private OnMoreClickListener moreClickListener;
  private SelectionStateListener selectionStateListener;
  private ZipLoadListener zipLoadListener;
  private PreferencesUtils setting;
  private boolean isGrid = false;
  private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

  public interface OnItemClickListener {
    void onItemClick(ZipEntryModel item, int position);
  }

  public interface OnMoreClickListener {

    void onMoreClick(ZipEntryModel item, View anchorView, int position);
  }

  public interface SelectionStateListener {
    void onSelectionChanged(int count);

    void onSelectionModeStarted();

    void onSelectionModeEnded();
  }

  public interface ZipLoadListener {

    void onLoadStarted();

    void onLoadFinished(String internalPath, boolean hasParent);

    void onLoadError(String message);
  }

  public ZipBrowserAdapter(@NonNull Context context) {
    this.context = context;
    setHasStableIds(true);
    setting = new PreferencesUtils(context);
    isGrid = setting.getGridMod();
  }

  public void setOnItemClickListener(OnItemClickListener l) {
    itemClickListener = l;
  }

  public void setOnMoreClickListener(OnMoreClickListener l) {
    moreClickListener = l;
  }

  public void setSelectionStateListener(SelectionStateListener l) {
    selectionStateListener = l;
  }

  public void setZipLoadListener(ZipLoadListener l) {
    zipLoadListener = l;
  }

  public void loadZip(@NonNull String zipFilePath, @NonNull String internalPath) {
    this.currentZipPath = zipFilePath;
    this.currentInternalPath = internalPath;
    if (zipLoadListener != null) zipLoadListener.onLoadStarted();

    ioExecutor.execute(
        () -> {
          try {
            List<ZipEntryModel> result = readZipEntries(zipFilePath, internalPath);
            var mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(
                () -> {
                  submitList(result);
                  boolean hasParent = !internalPath.isEmpty();
                  if (zipLoadListener != null)
                    zipLoadListener.onLoadFinished(internalPath, hasParent);
                });
          } catch (ZipException e) {
            var mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(
                () -> {
                  if (zipLoadListener != null) zipLoadListener.onLoadError(e.getMessage());
                });
          }
        });
  }

  public boolean navigateUp() {
    if (currentInternalPath.isEmpty()) return false;
    String parent = getParentPath(currentInternalPath);
    loadZip(currentZipPath, parent);
    return true;
  }

  public String getCurrentInternalPath() {
    return currentInternalPath;
  }

  public String getCurrentZipPath() {
    return currentZipPath;
  }

  public void search(String query) {
    this.searchQuery = query == null ? "" : query.trim();
    if (searchQuery.isEmpty()) {
      submitList(new ArrayList<>(itemsFull));
      return;
    }
    String lower = searchQuery.toLowerCase();
    List<ZipEntryModel> filtered = new ArrayList<>();
    for (ZipEntryModel item : itemsFull) {
      if (item.getName().toLowerCase().contains(lower)) filtered.add(item);
    }
    DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new ZipDiffCallback(items, filtered));
    items = filtered;
    diff.dispatchUpdatesTo(this);
    if (selectionTracker != null) selectionTracker.clearSelection();
  }

  public void setupSelectionTracker(@NonNull RecyclerView recyclerView) {
    if (selectionTracker != null) return;
    ItemKeyProvider<Long> keyProvider =
        new ItemKeyProvider<Long>(ItemKeyProvider.SCOPE_CACHED) {
          @Nullable
          @Override
          public Long getKey(int pos) {
            return (pos < 0 || pos >= getItemCount()) ? null : getItemId(pos);
          }

          @Override
          public int getPosition(@NonNull Long key) {
            for (int i = 0; i < getItemCount(); i++) if (getItemId(i) == key) return i;
            return RecyclerView.NO_POSITION;
          }
        };
    ItemDetailsLookup<Long> lookup =
        new ItemDetailsLookup<Long>() {
          @Nullable
          @Override
          public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View v = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (v == null) return null;
            RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(v);
            if (!(vh instanceof ViewHolder)) return null;
            int pos = vh.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return null;
            final int p = pos;
            return new ItemDetails<Long>() {
              @Override
              public int getPosition() {
                return p;
              }

              @Nullable
              @Override
              public Long getSelectionKey() {
                return getItemId(p);
              }
            };
          }
        };
    selectionTracker =
        new SelectionTracker.Builder<>(
                "zip_selection",
                recyclerView,
                keyProvider,
                lookup,
                StorageStrategy.createLongStorage())
            .build();
    selectionTracker.addObserver(
        new SelectionTracker.SelectionObserver<Long>() {
          @Override
          public void onSelectionChanged() {
            int count = selectionTracker.getSelection().size();
            if (selectionStateListener != null) {
              selectionStateListener.onSelectionChanged(count);
              if (count > 0) selectionStateListener.onSelectionModeStarted();
              else selectionStateListener.onSelectionModeEnded();
            }
          }
        });
  }

  public SelectionTracker<Long> getSelectionTracker() {
    return selectionTracker;
  }

  public List<ZipEntryModel> getSelectedItems() {
    List<ZipEntryModel> sel = new ArrayList<>();
    if (selectionTracker == null) return sel;
    for (Long id : selectionTracker.getSelection()) {
      for (int i = 0; i < items.size(); i++) {
        if (getItemId(i) == id) {
          sel.add(items.get(i));
          break;
        }
      }
    }
    return sel;
  }

  public void selectAll() {
    if (selectionTracker == null) return;
    for (int i = 0; i < items.size(); i++) {
      long id = getItemId(i);
      if (!selectionTracker.isSelected(id)) selectionTracker.select(id);
    }
    if (selectionStateListener != null) {
      selectionStateListener.onSelectionChanged(getSelectedItems().size());
      if (!getSelectedItems().isEmpty()) selectionStateListener.onSelectionModeStarted();
    }
  }

  public void clearSelection() {
    if (selectionTracker != null) selectionTracker.clearSelection();
  }

  public boolean isInSelectionMode() {
    return selectionTracker != null && selectionTracker.hasSelection();
  }

  @Override
  public long getItemId(int pos) {
    return items.get(pos).getEntryPath().hashCode();
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(
                isGrid ? R.layout.item_filemanager_grid : R.layout.item_file_manager,
                parent,
                false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    ZipEntryModel item = items.get(position);
    holder.bind(item);
    if (!isGrid) holder.bind(position, getItemCount());
    boolean selected = selectionTracker != null && selectionTracker.isSelected(getItemId(position));
    int backgroundColor;

    if (selected) {
      backgroundColor = ShapeUtil.getcolorPrimaryContainer(holder.card);
    } else {
      Integer surfaceColor = ShapeUtil.getcolorSurfaceContainer(holder.card);
      if (surfaceColor != null) {
        backgroundColor = surfaceColor;
      } else if (isGrid) {
        backgroundColor = Color.TRANSPARENT;
      } else {
        backgroundColor = ShapeUtil.getcolorPrimaryContainer(holder.card);
      }
    }

    holder.card.setCardBackgroundColor(
        ColorStateList.valueOf(
            setting.isShowBackground()
                ? ColorUtils.setAlphaComponent(backgroundColor, 128)
                : backgroundColor));
    M3Theme.listCard(holder.itemView);
  }

  class ViewHolder extends ListItemViewHolder {
    ImageView ivIcon;
    TextView tvName;
    TextView tvDate;
    ImageView ivMore;
    ListItemCardView card;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      ivIcon = itemView.findViewById(R.id.ivIcon);
      tvName = itemView.findViewById(R.id.tvName);
      tvDate = itemView.findViewById(R.id.tvDate);
      ivMore = itemView.findViewById(R.id.ivMore);
      card = itemView.findViewById(R.id.listcard);

      itemView.setOnClickListener(
          v -> {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (selectionTracker != null && selectionTracker.hasSelection()) {
              long id = ZipBrowserAdapter.this.getItemId(pos);
              if (selectionTracker.isSelected(id)) selectionTracker.deselect(id);
              else selectionTracker.select(id);
            } else {
              if (itemClickListener != null) itemClickListener.onItemClick(items.get(pos), pos);
            }
          });

      itemView.setOnLongClickListener(
          v -> {
            if (selectionTracker != null && !selectionTracker.hasSelection()) {
              int pos = getBindingAdapterPosition();
              if (pos != RecyclerView.NO_POSITION) {
                selectionTracker.select(ZipBrowserAdapter.this.getItemId(pos));
                return true;
              }
            }
            return false;
          });

      ivMore.setOnClickListener(
          v -> {
            int pos = getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION
                && moreClickListener != null
                && (selectionTracker == null || !selectionTracker.hasSelection())) {
              moreClickListener.onMoreClick(items.get(pos), ivMore, pos);
            }
          });
    }

    void bind(ZipEntryModel item) {

      if (item.isDirectory()) {
        ivIcon.setImageTintList(ColorStateList.valueOf(M3Theme.onSurface()));
        ivIcon.setImageResource(R.drawable.folder);
      } else {
        ivIcon.setImageTintList(null);
        ivIcon.clearColorFilter();
        FileIconHelper iconHelper = new FileIconHelper(item.getName());
        iconHelper.setDynamicFolderEnabled(false);
        iconHelper.setEnvironmentEnabled(false);
        iconHelper.bindIcon(ivIcon);
      }

      GradientDrawable gd = new GradientDrawable();
      gd.setColor(M3Theme.surface());
      gd.setStroke(1, M3Theme.outline());
      gd.setCornerRadius(8);
      ivIcon.setPadding(5, 5, 5, 5);
      ivIcon.setBackground(gd);

      if (searchQuery.isEmpty()) {
        tvName.setText(item.getName());
      } else {
        tvName.setText(highlight(item.getName(), searchQuery));
      }
      tvName.setTextColor(M3Theme.onSurface());

      tvDate.setText(item.getSubtitle());
    }

    private SpannableString highlight(String text, String query) {
      SpannableString ss = new SpannableString(text);
      String lower = text.toLowerCase();
      String q = query.toLowerCase();
      int start = 0, idx;
      while ((idx = lower.indexOf(q, start)) != -1) {
        ss.setSpan(
            new ForegroundColorSpan(HIGHLIGHT_COLOR),
            idx,
            idx + query.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        start = idx + query.length();
      }
      return ss;
    }
  }

  private List<ZipEntryModel> readZipEntries(String zipPath, String internalPrefix)
      throws ZipException {

    ZipFile zipFile = new ZipFile(zipPath);
    List<FileHeader> headers = zipFile.getFileHeaders();

    Map<String, ZipEntryModel> dirs = new LinkedHashMap<>();
    List<ZipEntryModel> files = new ArrayList<>();

    String prefix =
        internalPrefix.endsWith("/") || internalPrefix.isEmpty()
            ? internalPrefix
            : internalPrefix + "/";

    for (FileHeader header : headers) {
      String name = header.getFileName();

      if (!name.startsWith(prefix)) continue;
      String relative = name.substring(prefix.length());
      if (relative.isEmpty()) continue;

      int slash = relative.indexOf('/');

      if (slash == -1) {

        files.add(new ZipEntryModel(header, zipPath));
      } else {

        String dirName = relative.substring(0, slash);
        String dirEntry = prefix + dirName + "/";
        if (!dirs.containsKey(dirName)) {

          boolean explicit = false;
          for (FileHeader h : headers) {
            if (h.getFileName().equals(dirEntry)) {
              dirs.put(dirName, new ZipEntryModel(h, zipPath));
              explicit = true;
              break;
            }
          }
          if (!explicit) {
            dirs.put(dirName, new ZipEntryModel(dirName, dirEntry, zipPath));
          }
        }
      }
    }

    List<ZipEntryModel> result = new ArrayList<>(dirs.values());
    result.addAll(files);
    return result;
  }

  private void submitList(List<ZipEntryModel> newList) {
    DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new ZipDiffCallback(items, newList));
    items = newList != null ? newList : new ArrayList<>();
    itemsFull = new ArrayList<>(items);
    diff.dispatchUpdatesTo(this);
    if (selectionTracker != null) selectionTracker.clearSelection();
  }

  private static String getParentPath(String path) {
    String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    int idx = p.lastIndexOf('/');
    return idx <= 0 ? "" : p.substring(0, idx);
  }

  private static class ZipDiffCallback extends DiffUtil.Callback {
    private final List<ZipEntryModel> old;
    private final List<ZipEntryModel> newer;

    ZipDiffCallback(List<ZipEntryModel> old, List<ZipEntryModel> newer) {
      this.old = old;
      this.newer = newer;
    }

    @Override
    public int getOldListSize() {
      return old.size();
    }

    @Override
    public int getNewListSize() {
      return newer.size();
    }

    @Override
    public boolean areItemsTheSame(int o, int n) {
      return old.get(o).getEntryPath().equals(newer.get(n).getEntryPath());
    }

    @Override
    public boolean areContentsTheSame(int o, int n) {
      ZipEntryModel a = old.get(o), b = newer.get(n);
      return a.getName().equals(b.getName())
          && a.getUncompressedSize() == b.getUncompressedSize()
          && a.getLastModifiedTime() == b.getLastModifiedTime();
    }
  }
}
