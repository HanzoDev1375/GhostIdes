package ir.hanzodev1375.ghostide.postman;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.textfield.TextInputEditText;

import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.postman.util.UiUtils;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.postman.adapter.CollectionAdapter;
import ir.hanzodev1375.ghostide.postman.data.AppRepository;
import ir.hanzodev1375.ghostide.databinding.ActivityCollectionsBinding;
import ir.hanzodev1375.ghostide.postman.model.RequestCollection;
import ir.theme.M3Theme;

public class CollectionsActivity extends BaseCompat {

  public static final String EXTRA_COLLECTION_ID = "extra_collection_id";
  public static final String EXTRA_COLLECTION_NAME = "extra_collection_name";

  private ActivityCollectionsBinding binding;
  private AppRepository repository;
  private final List<RequestCollection> items = new ArrayList<>();
  private CollectionAdapter adapter;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityCollectionsBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    setupBackgroundBlur(binding.backgroundIconCollections, binding.appBarLayout, binding.contentFrame);
    UiUtils.tintContentForBackground(this, binding.appBarLayout, binding.contentFrame);
    UiUtils.fixUi(binding.appBarLayout, binding.contentFrame);
    repository = new AppRepository(this);
    binding.toolbar.setNavigationOnClickListener(v -> finish());

    adapter =
        new CollectionAdapter(
            items,
            new CollectionAdapter.Listener() {
              @Override
              public void onItemClick(RequestCollection collection) {
                Intent intent =
                    new Intent(CollectionsActivity.this, CollectionDetailActivity.class);
                intent.putExtra(EXTRA_COLLECTION_ID, collection.id);
                intent.putExtra(EXTRA_COLLECTION_NAME, collection.name);
                collectionDetailLauncher.launch(intent);
              }

              @Override
              public void onDeleteClick(RequestCollection collection) {
                new DialogCompat(CollectionsActivity.this)
                    .setTitle(R.string.dialog_confirm_title)
                    .setMessage(R.string.msg_confirm_delete_collection)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(
                        R.string.action_delete,
                        (dialog, which) -> {
                          new Thread(
                                  () -> {
                                    repository.deleteCollection(collection.id);
                                    runOnUiThread(CollectionsActivity.this::loadCollections);
                                  })
                              .start();
                        })
                    .show();
              }
            });

    binding.collectionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    binding.collectionsRecyclerView.setAdapter(adapter);

    binding.newCollectionFab.setOnClickListener(v -> showNewCollectionDialog());

    M3Theme.apply(binding.getRoot());

    loadCollections();
  }

  @Override
  protected void onResume() {
    super.onResume();
    loadCollections();
  }

  private final androidx.activity.result.ActivityResultLauncher<Intent> collectionDetailLauncher =
      registerForActivityResult(
          new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
          result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
              // A request was picked inside a collection — bubble it straight back up to
              // MainActivity.
              setResult(RESULT_OK, result.getData());
              finish();
            }
          });

  private void showNewCollectionDialog() {
    TextInputEditText input = new TextInputEditText(this);
    input.setHint(R.string.hint_collection_name);
    int pad = (int) (20 * getResources().getDisplayMetrics().density);
    input.setPadding(pad, pad, pad, pad);

    new DialogCompat(this)
        .setTitle(R.string.dialog_new_collection_title)
        .setView(input)
        .setNegativeButton(R.string.action_cancel, null)
        .setPositiveButton(
            R.string.action_create,
            (dialog, which) -> {
              String name = input.getText() == null ? "" : input.getText().toString().trim();
              if (name.isEmpty()) return;
              new Thread(
                      () -> {
                        repository.getOrCreateCollection(name);
                        runOnUiThread(this::loadCollections);
                      })
                  .start();
            })
        .show();
  }

  private void loadCollections() {
    new Thread(
            () -> {
              List<RequestCollection> loaded = repository.getCollections();
              runOnUiThread(
                  () -> {
                    items.clear();
                    items.addAll(loaded);
                    adapter.notifyDataSetChanged();
                    binding.emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.collectionsRecyclerView.setVisibility(
                        items.isEmpty() ? View.GONE : View.VISIBLE);
                  });
            })
        .start();
  }
}
