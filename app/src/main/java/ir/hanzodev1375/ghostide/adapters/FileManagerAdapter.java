package ir.hanzodev1375.ghostide.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.ghostide.materialfileicon.core.FileIconHelper;
import com.bumptech.glide.Glide;

import com.google.android.material.listitem.ListItemCardView;
import com.google.android.material.listitem.ListItemViewHolder;
import ir.hanzodev1375.filetreelib.filetreelibglide.glide.xml.VectorModel;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.models.FileManagerModel;
import ir.hanzodev1375.ghostide.utils.Icon;
import ir.hanzodev1375.ghostide.utils.ShapeUtil;
import ir.theme.M3Theme;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.prefs.Preferences;

public class FileManagerAdapter extends RecyclerView.Adapter<FileManagerAdapter.ViewHolder> {

  private List<FileManagerModel> items = new ArrayList<>();
  private List<FileManagerModel> itemsFull = new ArrayList<>();
  private String searchQuery = "";
  private static final int HIGHLIGHT_TEXT_COLOR = Color.BLACK;
  private static final int HIGHLIGHT_BACKGROUND_COLOR = Color.parseColor("#FFE0B2");
  private Context context;
  private SelectionTracker<Long> selectionTracker;
  private OnItemClickListener itemClickListener;
  private OnMoreClickListener moreClickListener;
  private SelectionStateListener selectionStateListener;
  private Set<String> gitChangedPaths = Collections.emptySet();
  private Set<String> gitChangedDirPrefixes = Collections.emptySet();
  private final Map<Long, Integer> idToPosition = new HashMap<>();
  private PreferencesUtils setting;
  private boolean isGrid = false;
  private boolean isAnimating = false;
  private int animationStartOffset = 0;
  private final Handler stopAnimationHandler = new Handler(Looper.getMainLooper());
  private final Runnable stopAnimationRunnable = this::stopAnimation;
  private RecyclerView recyclerView;
  private static final long STAGGER_STEP_MS = 20L;
  private static final long ANIM_IDLE_TIMEOUT_MS = 500L;
  private final RecyclerView.OnScrollListener clearAnimationListener =
      new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
          if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            clearAnimation();
          }
        }
      };

  public interface OnItemClickListener {
    void onItemClick(FileManagerModel item, View view, int position);
  }

  public interface OnMoreClickListener {
    void onMoreClick(FileManagerModel item, View moreView, int position);
  }

  public interface SelectionStateListener {
    void onSelectionChanged(int count);

    void onSelectionModeStarted();

    void onSelectionModeEnded();
  }

  public FileManagerAdapter(Context context) {
    this.context = context;
    setHasStableIds(true);
    setting = new PreferencesUtils(context);
    isGrid = setting.getGridMod();
  }

  public void setOnItemClickListener(OnItemClickListener listener) {
    this.itemClickListener = listener;
  }

  public void setOnMoreClickListener(OnMoreClickListener listener) {
    this.moreClickListener = listener;
  }

  /**
   * Provides the set of absolute file paths that currently have uncommitted git changes
   * (modified/added/untracked/etc). Items matching these paths (or folders that contain such items)
   * are highlighted with the "modified" color. Pass an empty set to clear the highlighting (e.g.
   * after commit/push, or when the current directory is outside a git repository).
   */
  public void setGitChangedPaths(Set<String> changedPaths) {
    Set<String> newPaths = changedPaths != null ? changedPaths : Collections.emptySet();
    if (newPaths.equals(this.gitChangedPaths)) return;

    Set<String> oldPaths = this.gitChangedPaths;
    Set<String> oldDirPrefixes = this.gitChangedDirPrefixes;
    this.gitChangedPaths = newPaths;
    this.gitChangedDirPrefixes = computeDirPrefixes(newPaths);

    if (oldPaths.isEmpty() && newPaths.isEmpty()) return;
    if (items.isEmpty()) return;

    List<Integer> changedPositions = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) {
      FileManagerModel item = items.get(i);
      boolean wasChanged = isGitChangedWithSet(item, oldPaths, oldDirPrefixes);
      boolean isNowChanged = isGitChanged(item);
      if (wasChanged != isNowChanged) {
        changedPositions.add(i);
      }
    }
    if (!changedPositions.isEmpty()) {
      for (int pos : changedPositions) {
        notifyItemChanged(pos);
      }
    }
  }

  private boolean isGitChangedWithSet(
      FileManagerModel item, Set<String> paths, Set<String> dirPrefixes) {
    if (paths.isEmpty()) return false;
    String path = item.getPath();
    if (path == null) return false;
    if (!item.isDirectory()) return paths.contains(path);
    return dirPrefixes.contains(path);
  }

  /**
   * Builds the set of all ancestor directory paths for every changed file so that folder rows can
   * be highlighted in O(1) instead of scanning the whole changed-files set for every row on every
   * rebind.
   */
  private Set<String> computeDirPrefixes(Set<String> changedPaths) {
    if (changedPaths.isEmpty()) return Collections.emptySet();
    Set<String> prefixes = new HashSet<>();
    for (String path : changedPaths) {
      String parent = new File(path).getParent();
      while (parent != null && prefixes.add(parent)) {
        parent = new File(parent).getParent();
      }
    }
    return prefixes;
  }

  private boolean isGitChanged(FileManagerModel item) {
    if (gitChangedPaths.isEmpty()) return false;
    String path = item.getPath();
    if (path == null) return false;
    if (!item.isDirectory()) return gitChangedPaths.contains(path);
    return gitChangedDirPrefixes.contains(path);
  }

  public void setSelectionStateListener(SelectionStateListener listener) {
    this.selectionStateListener = listener;
  }

  public void setupSelectionTracker(RecyclerView recyclerView) {
    if (selectionTracker != null) return;
    ItemKeyProvider<Long> keyProvider =
        new ItemKeyProvider<Long>(ItemKeyProvider.SCOPE_CACHED) {
          @Nullable
          @Override
          public Long getKey(int position) {
            if (position < 0 || position >= getItemCount()) return null;
            return getItemId(position);
          }

          @Override
          public int getPosition(@NonNull Long key) {
            for (int i = 0; i < getItemCount(); i++) {
              if (getItemId(i) == key) return i;
            }
            return RecyclerView.NO_POSITION;
          }
        };
    ItemDetailsLookup<Long> detailsLookup =
        new ItemDetailsLookup<Long>() {
          @Nullable
          @Override
          public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
              RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(view);
              if (vh instanceof ViewHolder) {
                int pos = vh.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                  final int position = pos;
                  return new ItemDetails<Long>() {
                    @Override
                    public int getPosition() {
                      return position;
                    }

                    @Nullable
                    @Override
                    public Long getSelectionKey() {
                      return getItemId(position);
                    }
                  };
                }
              }
            }
            return null;
          }
        };
    selectionTracker =
        new SelectionTracker.Builder<>(
                "file_selection_id",
                recyclerView,
                keyProvider,
                detailsLookup,
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

  public boolean isGridMode() {
    return isGrid;
  }

  public void refreshLayout() {
    boolean newIsGrid = setting.getGridMod();
    if (newIsGrid == isGrid) return;
    isGrid = newIsGrid;
  }

  public void selectAll() {
    if (selectionTracker == null) return;
    for (int i = 0; i < items.size(); i++) {
      long id = getItemId(i);
      if (!selectionTracker.isSelected(id)) {
        selectionTracker.select(id);
      }
    }
    if (selectionStateListener != null) {
      selectionStateListener.onSelectionChanged(getSelectedItems().size());
      if (getSelectedItems().size() > 0) {
        selectionStateListener.onSelectionModeStarted();
      }
    }
  }

  public List<FileManagerModel> getSelectedItems() {
    List<FileManagerModel> selected = new ArrayList<>();
    if (selectionTracker == null) return selected;
    for (Long id : selectionTracker.getSelection()) {
      Integer pos = idToPosition.get(id);
      if (pos != null && pos < items.size()) selected.add(items.get(pos));
    }
    return selected;
  }

  public void clearSelection() {
    if (selectionTracker != null) selectionTracker.clearSelection();
  }

  public boolean isInSelectionMode() {
    return selectionTracker != null && selectionTracker.hasSelection();
  }

  public void submitList(List<FileManagerModel> newList) {
    submitList(newList, true);
  }

  public void submitList(List<FileManagerModel> newList, boolean animate) {
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FileDiffCallback(items, newList));
    items = newList != null ? newList : new ArrayList<>();
    itemsFull = new ArrayList<>(items);
    rebuildIdMap();
    diffResult.dispatchUpdatesTo(this);
    if (selectionTracker != null) selectionTracker.clearSelection();
    if (animate) {
      resetAnimation();
    }
  }

  private void rebuildIdMap() {
    idToPosition.clear();
    for (int i = 0; i < items.size(); i++) {
      idToPosition.put(getItemId(i), i);
    }
  }

  public void search(String query) {
    this.searchQuery = query == null ? "" : query.trim();
    if (searchQuery.isEmpty()) {
      DiffUtil.DiffResult diffResult =
          DiffUtil.calculateDiff(new FileDiffCallback(items, itemsFull));
      items = new ArrayList<>(itemsFull);
      rebuildIdMap();
      diffResult.dispatchUpdatesTo(this);
      if (selectionTracker != null) selectionTracker.clearSelection();
      return;
    }
    String lowerQuery = searchQuery.toLowerCase();
    List<FileManagerModel> filteredList = new ArrayList<>();
    for (FileManagerModel item : itemsFull) {
      if (item.getName().toLowerCase().contains(lowerQuery)) {
        filteredList.add(item);
      }
    }
    DiffUtil.DiffResult diffResult =
        DiffUtil.calculateDiff(new FileDiffCallback(items, filteredList));
    items = filteredList;
    rebuildIdMap();
    diffResult.dispatchUpdatesTo(this);
    if (selectionTracker != null) selectionTracker.clearSelection();
  }

  @Override
  public long getItemId(int position) {
    return items.get(position).getPath().hashCode();
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
    FileManagerModel item = items.get(position);
    M3Theme.listCard(holder.itemView);
    holder.bindItem(item);
    if (!isGrid) holder.bind(position, getItemCount());
    boolean isSelected =
        selectionTracker != null && selectionTracker.isSelected(getItemId(position));
    int backgroundColor;
    if (isSelected) {
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

    bindViewHolderAnimation(holder);
  }

  @Override
  public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    this.recyclerView = recyclerView;
    recyclerView.addOnScrollListener(clearAnimationListener);
    resetAnimation();
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    recyclerView.removeOnScrollListener(clearAnimationListener);
    this.recyclerView = null;
    stopAnimation();
  }

  /**
   * فایل انیمیشن (مثلاً res/anim/list_item.xml) رو با یه delay پلکانی روی هر ردیف پخش میکنه؛ فقط
   * وقتی isAnimating روشنه (یعنی همین الان یه لیست جدید لود شده). به محض دراگ یا گذشتن ۵۰۰ms
   * بی‌فعالیت، isAnimating خاموش میشه و از این به بعد هیچ انیمیشنی پخش نمیشه.
   */
  private void bindViewHolderAnimation(@NonNull ViewHolder holder) {
    holder.itemView.clearAnimation();
    if (!isAnimating) return;
    Animation animation =
        AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.list_item_enter);
    animation.setStartOffset(animationStartOffset);
    animationStartOffset += STAGGER_STEP_MS;
    holder.itemView.startAnimation(animation);
    postStopAnimation();
  }

  private void stopAnimation() {
    stopAnimationHandler.removeCallbacks(stopAnimationRunnable);
    isAnimating = false;
    animationStartOffset = 0;
  }

  private void postStopAnimation() {
    stopAnimationHandler.removeCallbacks(stopAnimationRunnable);
    stopAnimationHandler.postDelayed(stopAnimationRunnable, ANIM_IDLE_TIMEOUT_MS);
  }

  /**
   * به محض شروع درگ کاربر صدا زده میشه: انیمیشن آینده رو خاموش میکنه و بقیه‌ی در حال اجرا رو قطع
   * میکنه.
   */
  private void clearAnimation() {
    stopAnimation();
    if (recyclerView != null) {
      for (int i = 0; i < recyclerView.getChildCount(); i++) {
        recyclerView.getChildAt(i).clearAnimation();
      }
    }
  }

  /** موقع لود یه لیست تازه (فولدر جدید/رفرش) صدا زده میشه تا ردیف‌ها دوباره با انیمیشن ظاهر بشن. */
  private void resetAnimation() {
    clearAnimation();
    isAnimating = true;
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  public void removeItem(int position) {
    if (position < 0 || position >= items.size()) return;
    items.remove(position);
    itemsFull = new ArrayList<>(items);
    notifyItemRemoved(position);
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
              // Toggle selection instead of always selecting
              long id = FileManagerAdapter.this.getItemId(pos);
              if (selectionTracker.isSelected(id)) {
                selectionTracker.deselect(id);
              } else {
                selectionTracker.select(id);
              }
            } else {
              if (itemClickListener != null) {
                itemClickListener.onItemClick(items.get(pos), itemView, pos);
              }
            }
          });

      itemView.setOnLongClickListener(
          v -> {
            if (selectionTracker != null && !selectionTracker.hasSelection()) {
              int pos = getBindingAdapterPosition();
              if (pos != RecyclerView.NO_POSITION) {
                selectionTracker.select(FileManagerAdapter.this.getItemId(pos));
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

    void bindItem(FileManagerModel item) {
      var iconHelper = new FileIconHelper(item.getPath());
      iconHelper.setDynamicFolderEnabled(true);
      iconHelper.setEnvironmentEnabled(true);
      if (item.getPath().endsWith(".xml")) {
        Glide.with(ivIcon)
            .load(new VectorModel(new File(item.getPath()), ivIcon.getContext()))
            .error(R.drawable.ic_fileicon)
            .into(ivIcon);
      } else {
        iconHelper.bindIcon(ivIcon);
      }

      var icon = new Icon();
      icon.bind(item.getPath(), ivIcon);
      if (searchQuery.isEmpty()) {
        tvName.setText(item.getName());
      } else {
        tvName.setText(getHighlightedText(item.getName(), searchQuery));
      }
      tvDate.setText(item.getLastModifiedFormatted());
      var gd = new GradientDrawable();
      gd.setColor(fallback(M3Theme.surface(), 0));
      gd.setStroke(1, fallback(M3Theme.outline(), 0));
      gd.setCornerRadius(8);
      ivIcon.setPadding(5, 5, 5, 5);
      ivIcon.setBackground(gd);
      int color;
      switch (item.getState()) {
        case CREATOR:
          color = fallback(M3Theme.onPrimary(), 0);
          break;
        case RENAME:
          color = fallback(M3Theme.secondary(), 0);
          break;
        case SERACH:
          color = fallback(M3Theme.tertiary(), 0);
          break;
        default:
          if (isGitChanged(item)) {
            color = ContextCompat.getColor(tvName.getContext(), R.color.tab_git_modified);
          } else {
            color = fallback(M3Theme.onSurface(), 0);
          }
      }
      tvName.setTextColor(color);
    }

    private SpannableString getHighlightedText(String text, String query) {
      SpannableString spannableString = new SpannableString(text);
      String lowerText = text.toLowerCase();
      String lowerQuery = query.toLowerCase();
      int startIndex = 0;
      while ((startIndex = lowerText.indexOf(lowerQuery, startIndex)) != -1) {
        int endIndex = startIndex + query.length();
        spannableString.setSpan(
            new ForegroundColorSpan(HIGHLIGHT_TEXT_COLOR),
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(
            new BackgroundColorSpan(HIGHLIGHT_BACKGROUND_COLOR),
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        startIndex = endIndex;
      }
      return spannableString;
    }
  }

  private static class FileDiffCallback extends DiffUtil.Callback {
    private final List<FileManagerModel> oldList;
    private final List<FileManagerModel> newList;

    FileDiffCallback(List<FileManagerModel> oldList, List<FileManagerModel> newList) {
      this.oldList = oldList;
      this.newList = newList;
    }

    @Override
    public int getOldListSize() {
      return oldList.size();
    }

    @Override
    public int getNewListSize() {
      return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      return oldList.get(oldItemPosition).getPath().equals(newList.get(newItemPosition).getPath());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      FileManagerModel oldItem = oldList.get(oldItemPosition);
      FileManagerModel newItem = newList.get(newItemPosition);
      return oldItem.getName().equals(newItem.getName())
          && oldItem.getLastModified() == newItem.getLastModified()
          && oldItem.getState() == newItem.getState();
    }
  }

  private int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
