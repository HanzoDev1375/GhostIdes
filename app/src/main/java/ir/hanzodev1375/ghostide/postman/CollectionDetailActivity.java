package ir.hanzodev1375.ghostide.postman;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.postman.util.UiUtils;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.postman.adapter.SavedRequestAdapter;
import ir.hanzodev1375.ghostide.postman.data.AppRepository;
import ir.hanzodev1375.ghostide.databinding.ActivityCollectionDetailBinding;
import ir.hanzodev1375.ghostide.postman.model.SavedRequest;

public class CollectionDetailActivity extends BaseCompat {

  private ActivityCollectionDetailBinding binding;
  private AppRepository repository;
  private long collectionId;
  private final List<SavedRequest> items = new ArrayList<>();
  private SavedRequestAdapter adapter;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityCollectionDetailBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    setupBackgroundBlur(binding.backgroundIconCollectionDetail, binding.toolbar, binding.requestsRecyclerView);
    UiUtils.fixUi(binding.getRoot());
    collectionId = getIntent().getLongExtra(CollectionsActivity.EXTRA_COLLECTION_ID, 0);
    String collectionName = getIntent().getStringExtra(CollectionsActivity.EXTRA_COLLECTION_NAME);
    binding.toolbar.setTitle(
        collectionName != null ? collectionName : getString(R.string.title_collection_detail));
    binding.toolbar.setNavigationOnClickListener(v -> finish());

    repository = new AppRepository(this);

    adapter =
        new SavedRequestAdapter(
            items,
            new SavedRequestAdapter.Listener() {
              @Override
              public void onItemClick(SavedRequest request) {
                Intent result = new Intent();
                result.putExtra(PostManActivity.EXTRA_SNAPSHOT_JSON, request.requestJson);
                setResult(RESULT_OK, result);
                finish();
              }

              @Override
              public void onDeleteClick(SavedRequest request) {
                new MaterialAlertDialogBuilder(CollectionDetailActivity.this)
                    .setTitle(R.string.dialog_confirm_title)
                    .setMessage(R.string.msg_confirm_delete_saved_request)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(
                        R.string.action_delete,
                        (dialog, which) -> {
                          new Thread(
                                  () -> {
                                    repository.deleteSavedRequest(request.id);
                                    runOnUiThread(CollectionDetailActivity.this::loadRequests);
                                  })
                              .start();
                        })
                    .show();
              }
            });

    binding.requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    binding.requestsRecyclerView.setAdapter(adapter);

    loadRequests();
  }

  private void loadRequests() {
    new Thread(
            () -> {
              List<SavedRequest> loaded = repository.getSavedRequests(collectionId);
              runOnUiThread(
                  () -> {
                    items.clear();
                    items.addAll(loaded);
                    adapter.notifyDataSetChanged();
                    binding.emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.requestsRecyclerView.setVisibility(
                        items.isEmpty() ? View.GONE : View.VISIBLE);
                  });
            })
        .start();
  }
}
