package ir.hanzodev1375.ghostide.postman;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.postman.util.UiUtils;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.postman.adapter.HistoryAdapter;
import ir.hanzodev1375.ghostide.postman.data.AppRepository;
import ir.hanzodev1375.ghostide.databinding.ActivityHistoryBinding;
import ir.hanzodev1375.ghostide.postman.model.HistoryItem;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class HistoryActivity extends BaseCompat {

  private ActivityHistoryBinding binding;
  private AppRepository repository;
  private final List<HistoryItem> items = new ArrayList<>();
  private HistoryAdapter adapter;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityHistoryBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    setupBackgroundBlur(binding.backgroundIconHistory, binding.appBarLayout, binding.contentFrame);
    UiUtils.tintContentForBackground(this, binding.appBarLayout, binding.contentFrame);
    UiUtils.fixUi(binding.appBarLayout, binding.contentFrame);

    repository = new AppRepository(this);
    binding.toolbar.setNavigationOnClickListener(v -> finish());

    adapter =
        new HistoryAdapter(
            items,
            new HistoryAdapter.Listener() {
              @Override
              public void onItemClick(HistoryItem item) {
                Intent result = new Intent();
                result.putExtra(PostManActivity.EXTRA_SNAPSHOT_JSON, item.requestJson);
                setResult(RESULT_OK, result);
                finish();
              }

              @Override
              public void onDeleteClick(HistoryItem item) {
                new DialogCompat(HistoryActivity.this)
                    .setTitle(R.string.dialog_confirm_title)
                    .setMessage(R.string.msg_confirm_delete_history_item)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(
                        R.string.action_delete,
                        (dialog, which) -> {
                          new Thread(
                                  () -> {
                                    repository.deleteHistory(item.id);
                                    runOnUiThread(HistoryActivity.this::loadHistory);
                                  })
                              .start();
                        })
                    .show();
              }
            });

    binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    binding.historyRecyclerView.setAdapter(adapter);

    loadHistory();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_history, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_clear_all) {
      new DialogCompat(this)
          .setTitle(R.string.dialog_confirm_title)
          .setMessage(R.string.msg_confirm_clear_history)
          .setNegativeButton(R.string.action_cancel, null)
          .setPositiveButton(
              R.string.action_clear_all,
              (dialog, which) -> {
                new Thread(
                        () -> {
                          repository.clearHistory();
                          runOnUiThread(this::loadHistory);
                        })
                    .start();
              })
          .show();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void loadHistory() {
    new Thread(
            () -> {
              List<HistoryItem> loaded = repository.getHistory();
              runOnUiThread(
                  () -> {
                    items.clear();
                    items.addAll(loaded);
                    adapter.notifyDataSetChanged();
                    binding.emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.historyRecyclerView.setVisibility(
                        items.isEmpty() ? View.GONE : View.VISIBLE);
                  });
            })
        .start();
  }
}
