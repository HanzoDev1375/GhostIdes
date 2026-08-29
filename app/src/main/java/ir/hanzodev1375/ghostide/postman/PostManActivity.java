package ir.hanzodev1375.ghostide.postman;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import com.example.liquidglass.LiquidGlassView;
import ir.hanzodev1375.ghostide.GhostIdeAppLoader;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.activity.SettingActivity;

import ir.hanzodev1375.ghostide.utils.BlurTransformation;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.postman.adapter.KeyValueAdapter;
import ir.hanzodev1375.ghostide.postman.data.AppRepository;
import ir.hanzodev1375.ghostide.databinding.ActivityPostmanBinding;
import ir.hanzodev1375.ghostide.postman.model.HistoryItem;
import ir.hanzodev1375.ghostide.postman.model.KeyValueItem;
import ir.hanzodev1375.ghostide.postman.model.RequestSnapshot;
import ir.hanzodev1375.ghostide.postman.network.HttpEngine;
import ir.hanzodev1375.ghostide.postman.util.ColorUtils;
import ir.hanzodev1375.ghostide.postman.util.JsonUtils;
import ir.hanzodev1375.ghostide.postman.util.PrefsManager;
import ir.hanzodev1375.ghostide.postman.util.TimeUtils;
import ir.hanzodev1375.ghostide.postman.util.UiUtils;

public class PostManActivity extends BaseCompat {

  public static final String EXTRA_SNAPSHOT_JSON = "extra_snapshot_json";

  private ActivityPostmanBinding binding;
  private AppRepository repository;
  private PrefsManager prefs;
  private final Gson gson = new Gson();

  private final List<KeyValueItem> paramItems = new ArrayList<>();
  private final List<KeyValueItem> headerItems = new ArrayList<>();
  private final List<KeyValueItem> formItems = new ArrayList<>();
  private KeyValueAdapter paramsAdapter;
  private KeyValueAdapter headersAdapter;
  private KeyValueAdapter formAdapter;

  private int bodyType = 0; // 0 none, 1 raw, 2 form url-encoded
  private HttpEngine.HttpResult lastResult;

  private final ActivityResultLauncher<Intent> historyLauncher =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(), this::handlePickedSnapshotResult);

  private final ActivityResultLauncher<Intent> collectionsLauncher =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(), this::handlePickedSnapshotResult);

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityPostmanBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    setSupportActionBar(binding.toolbar);
    UiUtils.fixUi(binding.appBarLayout, binding.contentScroll);
    UiUtils.fixBottomBar(binding.responseSheet);
    repository = new AppRepository(this);
    prefs = new PrefsManager(this);
    setupMethodAndContentTypeDropdowns();
    setupRecyclerViews();
    setupRequestTabs();
    setupBodyTypeToggle();
    setupResponseSheet();
    setupSendButton();
    stepBackground();
  }

  private void setupMethodAndContentTypeDropdowns() {
    ArrayAdapter<String> methodAdapter =
        new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            getResources().getStringArray(R.array.http_methods));
    binding.methodDropdown.setAdapter(methodAdapter);
    binding.methodDropdown.setText("GET", false);

    ArrayAdapter<String> contentTypeAdapter =
        new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            getResources().getStringArray(R.array.content_types));
    binding.contentTypeDropdown.setAdapter(contentTypeAdapter);
    binding.contentTypeDropdown.setText("application/json", false);
  }

  void stepBackground() {
    var setting = GhostIdeAppLoader.getInstance().getSetting();
    var themeManager = new ThemeManager(this);
    var themeUtil = new ThemeUtils(themeManager);
    var weget = themeUtil.getTheme().getWidget();
    float ids = weget.getBlursize();
    var getImagePath = weget.getImagepath();
    if (!setting.isShowBackground()) {
      return;
    }
    binding.iconBackground.setVisibility(View.VISIBLE);
    getWindow().setStatusBarColor(Color.TRANSPARENT);
    getWindow().setNavigationBarColor(Color.TRANSPARENT);
    Glide.with(this)
        .load(getImagePath)
        .transform(new BlurTransformation((int) ids))
        .into(binding.iconBackground);
    binding.getRoot().setBackgroundColor(Color.TRANSPARENT);
    var theme = themeUtil.getTheme();
    if (theme != null && theme.getActivity() != null && theme.getActivity().getBackground() != null) {
      int bg = Color.parseColor(theme.getActivity().getBackground());
      int tint = androidx.core.graphics.ColorUtils.setAlphaComponent(bg, 128);
      binding.toolbar.setBackgroundColor(tint);
      binding.appBarLayout.setBackgroundColor(tint);
      binding.contentScroll.setBackgroundColor(tint);
    }
    int cardBackColor = MaterialColors.getColor(this, R.attr.colorSurfaceContainerLow, 0);
    binding.responseEmptyState.setBackgroundTintList(
        ColorStateList.valueOf(
            androidx.core.graphics.ColorUtils.setAlphaComponent(cardBackColor, 128)));
  }

  private void setupRecyclerViews() {
    paramItems.add(new KeyValueItem());
    paramsAdapter = new KeyValueAdapter(paramItems);
    binding.paramsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    binding.paramsRecyclerView.setAdapter(paramsAdapter);
    binding.paramsRecyclerView.setNestedScrollingEnabled(false);
    binding.addParamButton.setOnClickListener(v -> paramsAdapter.addRow());

    headerItems.add(new KeyValueItem());
    headersAdapter = new KeyValueAdapter(headerItems);
    binding.headersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    binding.headersRecyclerView.setAdapter(headersAdapter);
    binding.headersRecyclerView.setNestedScrollingEnabled(false);
    binding.addHeaderButton.setOnClickListener(v -> headersAdapter.addRow());

    formItems.add(new KeyValueItem());
    formAdapter = new KeyValueAdapter(formItems);
    binding.formRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    binding.formRecyclerView.setAdapter(formAdapter);
    binding.formRecyclerView.setNestedScrollingEnabled(false);
    binding.addFormFieldButton.setOnClickListener(v -> formAdapter.addRow());
  }

  private void setupRequestTabs() {
    binding.requestTabs.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            binding.paramsPanel.setVisibility(tab.getPosition() == 0 ? View.VISIBLE : View.GONE);
            binding.headersPanel.setVisibility(tab.getPosition() == 1 ? View.VISIBLE : View.GONE);
            binding.bodyPanel.setVisibility(tab.getPosition() == 2 ? View.VISIBLE : View.GONE);
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(TabLayout.Tab tab) {}
        });
  }

  private void setupBodyTypeToggle() {
    binding.bodyTypeToggleGroup.addOnButtonCheckedListener(
        (group, checkedId, isChecked) -> {
          if (!isChecked) return;
          if (checkedId == binding.bodyTypeRaw.getId()) {
            bodyType = 1;
            binding.noneBodyText.setVisibility(View.GONE);
            binding.rawBodyContainer.setVisibility(View.VISIBLE);
            binding.formBodyContainer.setVisibility(View.GONE);
          } else if (checkedId == binding.bodyTypeForm.getId()) {
            bodyType = 2;
            binding.noneBodyText.setVisibility(View.GONE);
            binding.rawBodyContainer.setVisibility(View.GONE);
            binding.formBodyContainer.setVisibility(View.VISIBLE);
          } else {
            bodyType = 0;
            binding.noneBodyText.setVisibility(View.VISIBLE);
            binding.rawBodyContainer.setVisibility(View.GONE);
            binding.formBodyContainer.setVisibility(View.GONE);
          }
        });

    binding.formatJsonButton.setOnClickListener(
        v -> {
          String raw =
              binding.bodyRawInput.getText() == null
                  ? ""
                  : binding.bodyRawInput.getText().toString();
          binding.bodyRawInput.setText(JsonUtils.prettyPrint(raw));
        });
  }

  private void setupResponseSheet() {
    LiquidGlassView glass = binding.responseSheetGlassView;
    glass.setBackdropSource(binding.mainContentContainer);
    glass.setEnableDynamicBackground(true);
    BottomSheetBehavior<LinearLayout> behavior = BottomSheetBehavior.from(binding.responseSheet);
    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

    binding.handleClickArea.setOnClickListener(
        v -> {
          int state = behavior.getState();
          behavior.setState(
              state == BottomSheetBehavior.STATE_EXPANDED
                  ? BottomSheetBehavior.STATE_COLLAPSED
                  : BottomSheetBehavior.STATE_EXPANDED);
        });

    binding.responseTabs.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            binding.responseBodyScroll.setVisibility(
                tab.getPosition() == 0 ? View.VISIBLE : View.GONE);
            binding.responseHeadersScroll.setVisibility(
                tab.getPosition() == 1 ? View.VISIBLE : View.GONE);
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(TabLayout.Tab tab) {}
        });

    binding.copyBodyButton.setOnClickListener(
        v -> {
          if (lastResult == null) return;
          ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
          if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("response body", lastResult.body));
            Snackbar.make(binding.getRoot(), R.string.msg_copied, Snackbar.LENGTH_SHORT).show();
          }
        });
  }

  private void setupSendButton() {
    binding.sendButton.setOnClickListener(v -> sendRequest());
    binding.urlInput.setOnEditorActionListener(
        (v, actionId, event) -> {
          sendRequest();
          return true;
        });
  }

  private void sendRequest() {
    RequestSnapshot snapshot = buildSnapshot();
    if (snapshot.url.trim().isEmpty()) {
      binding.urlInput.setError(getString(R.string.msg_invalid_url));
      return;
    }
    binding.urlInput.setError(null);
    hideKeyboard();
    setSending(true);

    HttpEngine engine =
        new HttpEngine(prefs.getTimeoutSeconds(), prefs.isSslVerificationDisabled());
    engine.execute(
        snapshot,
        result -> {
          setSending(false);
          showResponse(result);
          saveToHistory(snapshot, result);
        });
  }

  private void setSending(boolean sending) {
    binding.progressIndicator.setVisibility(sending ? View.VISIBLE : View.INVISIBLE);
    binding.sendButton.setEnabled(!sending);
  }

  private RequestSnapshot buildSnapshot() {
    RequestSnapshot snapshot = new RequestSnapshot();
    String method =
        binding.methodDropdown.getText() == null
            ? "GET"
            : binding.methodDropdown.getText().toString().trim();
    snapshot.method = method.isEmpty() ? "GET" : method;
    snapshot.url =
        binding.urlInput.getText() == null ? "" : binding.urlInput.getText().toString().trim();
    snapshot.params = paramItems;
    snapshot.headers = headerItems;
    snapshot.bodyType = bodyType;
    snapshot.rawBody =
        binding.bodyRawInput.getText() == null ? "" : binding.bodyRawInput.getText().toString();
    snapshot.rawContentType =
        binding.contentTypeDropdown.getText() == null
            ? "application/json"
            : binding.contentTypeDropdown.getText().toString().trim();
    snapshot.formFields = formItems;
    return snapshot;
  }

  private void showResponse(HttpEngine.HttpResult result) {
    lastResult = result;
    binding.responseEmptyState.setVisibility(View.GONE);
    binding.responseContentContainer.setVisibility(View.VISIBLE);
    binding.responseStatusChip.setVisibility(View.VISIBLE);
    binding.copyBodyButton.setVisibility(View.VISIBLE);

    if (result.success) {
      binding.responseStatusChip.setText(
          UiUtils.statusLabel(result.statusCode, result.statusMessage));
      binding.responseStatusChip.setBackgroundTintList(
          ColorStateList.valueOf(UiUtils.statusColor(this, result.statusCode)));
      binding.responseMetaText.setText(
          TimeUtils.formatDuration(result.timeMs) + " · " + TimeUtils.formatSize(result.sizeBytes));

      String body = result.body == null ? "" : result.body;
      if (JsonUtils.looksLikeJson(body)) {
        String pretty = JsonUtils.prettyPrint(body);
        int keyColor = ColorUtils.resolveAttrColor(this, R.attr.colorPrimary);
        int stringColor = ColorUtils.resolveAttrColor(this, R.attr.colorTertiary);
        int numberColor = ColorUtils.resolveAttrColor(this, R.attr.colorSecondary);
        binding.responseBodyText.setText(
            JsonUtils.highlight(this, pretty, keyColor, stringColor, numberColor));
      } else {
        binding.responseBodyText.setText(body.isEmpty() ? "(empty body)" : body);
      }
      if (result.bodyTruncated) {
        binding.responseBodyText.append(
            "\n\n… truncated for display (" + TimeUtils.formatSize(result.sizeBytes) + " total)");
      }

      StringBuilder headersText = new StringBuilder();
      for (String[] header : result.headers) {
        headersText.append(header[0]).append(": ").append(header[1]).append("\n");
      }
      binding.responseHeadersText.setText(
          headersText.length() == 0 ? "(no headers)" : headersText.toString().trim());
    } else {
      binding.responseStatusChip.setText("error");
      binding.responseStatusChip.setBackgroundTintList(
          ColorStateList.valueOf(UiUtils.statusColor(this, 500)));
      binding.responseMetaText.setText("");
      binding.responseBodyText.setText(getString(R.string.msg_request_failed, result.errorMessage));
      binding.responseHeadersText.setText("");
      binding.copyBodyButton.setVisibility(View.GONE);
    }

    BottomSheetBehavior.from(binding.responseSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
  }

  private void saveToHistory(RequestSnapshot snapshot, HttpEngine.HttpResult result) {
    HistoryItem item = new HistoryItem();
    item.method = snapshot.method;
    item.url = snapshot.url;
    item.statusCode = result.success ? result.statusCode : 0;
    item.timeMs = result.timeMs;
    item.timestamp = System.currentTimeMillis();
    item.requestJson = gson.toJson(snapshot);
    new Thread(() -> repository.insertHistory(item)).start();
  }

  private void hideKeyboard() {
    View focused = getCurrentFocus();
    if (focused == null) return;
    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    if (imm != null) imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
  }

  private void handlePickedSnapshotResult(androidx.activity.result.ActivityResult result) {
    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
    String json = result.getData().getStringExtra(EXTRA_SNAPSHOT_JSON);
    applySnapshot(json);
  }

  private void applySnapshot(@Nullable String json) {
    if (json == null || json.isEmpty()) return;
    RequestSnapshot snapshot;
    try {
      snapshot = gson.fromJson(json, RequestSnapshot.class);
    } catch (Exception e) {
      return;
    }
    if (snapshot == null) return;

    binding.methodDropdown.setText(snapshot.method == null ? "GET" : snapshot.method, false);
    binding.urlInput.setText(snapshot.url);

    paramItems.clear();
    if (snapshot.params != null) paramItems.addAll(snapshot.params);
    if (paramItems.isEmpty()) paramItems.add(new KeyValueItem());
    paramsAdapter.notifyDataSetChanged();

    headerItems.clear();
    if (snapshot.headers != null) headerItems.addAll(snapshot.headers);
    if (headerItems.isEmpty()) headerItems.add(new KeyValueItem());
    headersAdapter.notifyDataSetChanged();

    formItems.clear();
    if (snapshot.formFields != null) formItems.addAll(snapshot.formFields);
    if (formItems.isEmpty()) formItems.add(new KeyValueItem());
    formAdapter.notifyDataSetChanged();

    binding.bodyRawInput.setText(snapshot.rawBody);
    binding.contentTypeDropdown.setText(
        snapshot.rawContentType == null ? "application/json" : snapshot.rawContentType, false);

    int checkedButtonId;
    if (snapshot.bodyType == 1) {
      checkedButtonId = binding.bodyTypeRaw.getId();
    } else if (snapshot.bodyType == 2) {
      checkedButtonId = binding.bodyTypeForm.getId();
    } else {
      checkedButtonId = binding.bodyTypeNone.getId();
    }
    binding.bodyTypeToggleGroup.check(checkedButtonId);

    Snackbar.make(binding.getRoot(), R.string.msg_request_loaded, Snackbar.LENGTH_SHORT).show();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    int tint = ColorUtils.resolveAttrColor(this, com.google.android.material.R.attr.colorOnSurface);
    for (int i = 0; i < menu.size(); i++) {
      MenuItem item = menu.getItem(i);
      if (item.getIcon() != null) {
        item.getIcon().mutate().setTint(tint);
      }
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_history) {
      historyLauncher.launch(new Intent(this, HistoryActivity.class));
      return true;
    } else if (id == R.id.action_collections) {
      collectionsLauncher.launch(new Intent(this, CollectionsActivity.class));
      return true;
    } else if (id == R.id.action_save) {
      RequestSnapshot snapshot = buildSnapshot();
      if (snapshot.url.trim().isEmpty()) {
        Snackbar.make(binding.getRoot(), R.string.msg_invalid_url, Snackbar.LENGTH_SHORT).show();
        return true;
      }
      String json = gson.toJson(snapshot);
      SaveRequestBottomSheet sheet =
          SaveRequestBottomSheet.newInstance(snapshot.method, snapshot.url, json);
      sheet.setListener(
          () ->
              Snackbar.make(binding.getRoot(), R.string.msg_request_saved, Snackbar.LENGTH_SHORT)
                  .show());
      sheet.show(getSupportFragmentManager(), "save_request");
      return true;
    } else if (id == R.id.action_settings) {
      startActivity(new Intent(this, SettingActivity.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
