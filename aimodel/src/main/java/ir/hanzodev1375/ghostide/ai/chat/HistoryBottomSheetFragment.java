package ir.hanzodev1375.ghostide.ai.chat;

import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import ir.hanzodev1375.components.views.GhostToast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.listitem.ListItemCardView;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.ghostide.ai.R;
import ir.hanzodev1375.ghostide.ai.database.ChatRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryBottomSheetFragment extends BaseBlurBottomSheet {

  private ChatRepository repository;
  private HistoryAdapter adapter;
  private OnChatsSelectedListener listener;
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private Handler mainHandler = new Handler(Looper.getMainLooper());
  private RecyclerView rvHistory;
  private ProgressBar progressHistory;
  private LinearLayout emptyView;
  private Button btnDeleteSelected;
  private Button btnCancel;

  public interface OnChatsSelectedListener {
    void onLoadChat(long chatId);

    void onDeleteChats(List<Long> chatIds);
  }

  public void setOnChatsSelectedListener(OnChatsSelectedListener listener) {
    this.listener = listener;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
    if (getContext() != null) {
      repository = new ChatRepository(getContext());
    }
  }

  @Override
  protected void onContentReady(@Nullable ViewGroup contentContainer) {
    View view =
        LayoutInflater.from(contentContainer.getContext())
            .inflate(R.layout.bottom_sheet_history_fragment, contentContainer, false);
    contentContainer.addView(view);

    rvHistory = view.findViewById(R.id.rvhistory);
    progressHistory = view.findViewById(R.id.progresshistory);
    emptyView = view.findViewById(R.id.emptyview);
    btnDeleteSelected = view.findViewById(R.id.btndeleteselected);
    btnCancel = view.findViewById(R.id.btncancel);
    if (rvHistory != null) {
      rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
      rvHistory.addItemDecoration(new MarginItemDecoration());
      adapter = new HistoryAdapter();
      rvHistory.setAdapter(adapter);
    }

    loadChatsAsync();

    btnDeleteSelected.setOnClickListener(
        v -> {
          List<Long> selectedIds = adapter.getSelectedIds();
          if (!selectedIds.isEmpty() && listener != null) {
            listener.onDeleteChats(selectedIds);
            loadChatsAsync();
            GhostToast.makeText(
                    getContext(), "Deleted " + selectedIds.size() + " chats", GhostToast.LENGTH_SHORT)
                .show();
          } else {
            GhostToast.makeText(getContext(), "No chat selected", GhostToast.LENGTH_SHORT).show();
          }
        });

    btnCancel.setOnClickListener(v -> dismiss());
  }

  private void loadChatsAsync() {
    if (progressHistory != null) progressHistory.setVisibility(View.VISIBLE);
    executor.execute(
        () -> {
          try {
            List<ChatRepository.ChatItem> loaded = repository.getAllChats();
            mainHandler.post(
                () -> {
                  emptyView.setVisibility(!loaded.isEmpty() ? View.GONE : View.VISIBLE);
                  adapter.setData(loaded);
                  progressHistory.setVisibility(View.GONE);
                });
          } catch (Exception e) {
            mainHandler.post(
                () -> {
                  progressHistory.setVisibility(View.GONE);
                  GhostToast.makeText(getContext(), "Error: " + e.getMessage(), GhostToast.LENGTH_SHORT)
                      .show();
                });
          }
        });
  }

  private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<ChatRepository.ChatItem> items = new ArrayList<>();
    private List<Boolean> selected = new ArrayList<>();

    @Override
    public long getItemId(int position) {
      return items.get(position).id;
    }

    public void setData(List<ChatRepository.ChatItem> newItems) {
      items.clear();
      items.addAll(newItems);
      selected.clear();
      for (int i = 0; i < items.size(); i++) {
        selected.add(false);
      }
      notifyDataSetChanged();
    }

    List<Long> getSelectedIds() {
      List<Long> ids = new ArrayList<>();
      for (int i = 0; i < items.size(); i++) {
        if (selected.get(i)) ids.add(items.get(i).id);
      }
      return ids;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View itemView =
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.item_history_chat, parent, false);
      return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      ChatRepository.ChatItem chat = items.get(position);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
      String date = sdf.format(new Date(chat.createdAt));
      String title = (chat.title == null || chat.title.isEmpty()) ? "Chat " + chat.id : chat.title;

      holder.tvChatTitle.setText(title);
      holder.tvChatDate.setText(date);

      holder.checkboxSelect.setOnCheckedChangeListener(null);
      holder.checkboxSelect.setChecked(selected.get(position));
      holder.checkboxSelect.setOnCheckedChangeListener(
          (buttonView, isChecked) -> selected.set(position, isChecked));

      holder.itemView.setOnClickListener(
          v -> {
            if (listener != null && !selected.get(position)) {
              listener.onLoadChat(chat.id);
              dismiss();
            }
          });
    }

    @Override
    public int getItemCount() {
      return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
      TextView tvChatTitle;
      TextView tvChatDate;
      CheckBox checkboxSelect;
      ListItemCardView card;

      ViewHolder(@NonNull View itemView) {
        super(itemView);
        tvChatTitle = itemView.findViewById(R.id.tv_chat_title);
        tvChatDate = itemView.findViewById(R.id.tv_chat_date);
        checkboxSelect = itemView.findViewById(R.id.checkbox_select);
        card = itemView.findViewById(R.id.card);
        card.setCardBackgroundColor(
            ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(
                    MaterialColors.getColor(card, R.attr.colorSurfaceContainerHighest), 128)));
      }
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    executor.shutdownNow();
  }

  class MarginItemDecoration extends RecyclerView.ItemDecoration {
    private final int itemMargin = 2;

    @Override
    public void getItemOffsets(
        @NonNull Rect outRect,
        @NonNull View view,
        @NonNull RecyclerView parent,
        @NonNull RecyclerView.State state) {
      int position = parent.getChildAdapterPosition(view);
      if (position != state.getItemCount() - 1) {
        outRect.bottom = itemMargin;
      }
    }
  }
}
