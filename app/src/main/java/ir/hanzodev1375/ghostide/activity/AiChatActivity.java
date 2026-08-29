package ir.hanzodev1375.ghostide.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import ir.hanzodev1375.components.childern.ViewChilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.ai.chat.AttachedFilesAdapter;
import ir.hanzodev1375.ghostide.ai.chat.ChatAdapter;
import ir.hanzodev1375.ghostide.ai.chat.HistoryBottomSheetFragment;
import ir.hanzodev1375.ghostide.ai.database.ChatRepository;
import ir.hanzodev1375.ghostide.ai.model.AttachedFile;
import ir.hanzodev1375.ghostide.ai.model.ChatMessage;
import ir.hanzodev1375.ghostide.ai.network.AiClient;
import ir.hanzodev1375.ghostide.ai.network.AiClientFactory;
import ir.hanzodev1375.ghostide.ai.utils.AiConstants;
import ir.hanzodev1375.ghostide.ai.utils.AiPreferencesUtils;
import ir.hanzodev1375.ghostide.ai.utils.FileReadUtils;

public class AiChatActivity extends BaseCompat {

  private RecyclerView recyclerView;
  private TextInputEditText etInput;
  private MaterialButton btnSend;
  private MaterialButton btnAttach;
  private AutoCompleteTextView actvProvider;
  private RecyclerView rvAttachedFiles;
  private ViewChilder child;
  private ChatAdapter adapter;
  private AttachedFilesAdapter attachedFilesAdapter;
  private final List<ChatMessage> messages = new ArrayList<>();
  private final List<AttachedFile> attachedFiles = new ArrayList<>();

  private AiPreferencesUtils prefs;
  private AiClient currentClient;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private ChatRepository chatRepository;

  private long currentChatId = -1;

  private static final String[] PROVIDERS = {
    AiConstants.AiProvider.CLAUDE,
    AiConstants.AiProvider.CHATGPT,
    AiConstants.AiProvider.DEEPSEEK,
    AiConstants.AiProvider.GEMINI,
    AiConstants.AiProvider.OPENROUTER
  };
  private static final String[] PROVIDER_LABELS = {
    "Claude (Anthropic)", "ChatGPT (OpenAI)", "DeepSeek", "Gemini (Google)", "OpenRouter"
  };

  @NonNull
  private final ActivityResultLauncher<Intent> filePicker =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
              handlePickedUri(result.getData().getData());
            }
          });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ai_chat);

    prefs = new AiPreferencesUtils(this);
    chatRepository = new ChatRepository(this);

    Toolbar toolbar = findViewById(R.id.toolbar_ai_chat);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setTitle("Ghost AI");
    }

    recyclerView = findViewById(R.id.rv_chat);
    etInput = findViewById(R.id.et_input);
    btnSend = findViewById(R.id.btn_send);
    btnAttach = findViewById(R.id.btn_attach);
    actvProvider = findViewById(R.id.actv_provider);
    rvAttachedFiles = findViewById(R.id.rv_attached_files);
    child = findViewById(R.id.backgroundIconSetting);
    setupBackgroundBlur(child, findViewById(R.id.rootAi),findViewById(R.id.appbar));
    setupProviderDropdown();
    setupChatRecyclerView();
    setupAttachedFilesRecyclerView();
    setupSendButton();
    setupAttachButton();
    refreshClient();
    setupBackPressed();

    if (savedInstanceState == null) {
      long chatIdFromIntent = getIntent().getLongExtra("chat_id", -1);
      if (chatIdFromIntent != -1) {

        currentChatId = chatIdFromIntent;
        List<ChatMessage> savedMessages = chatRepository.getMessagesByChatId(currentChatId);
        messages.clear();
        messages.addAll(savedMessages);
        adapter.notifyDataSetChanged();
        if (!messages.isEmpty()) {
          recyclerView.scrollToPosition(messages.size() - 1);
        }
        updateChatTitle();
      } else {

        currentChatId = -1;
      }
    } else {
      currentChatId = savedInstanceState.getLong("current_chat_id", -1);
    }
  }

  private void setupBackPressed() {
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (currentChatId != -1 && !messages.isEmpty()) {
                  startNewChat();
                } else {
                  finish();
                }
              }
            });
  }

  private void startNewChat() {
    currentChatId = -1;
    messages.clear();
    adapter.notifyDataSetChanged();
    attachedFiles.clear();
    attachedFilesAdapter.notifyDataSetChanged();
    updateAttachedFilesVisibility();
    if (getSupportActionBar() != null) getSupportActionBar().setTitle("Ghost AI");
    etInput.setText("");
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putLong("current_chat_id", currentChatId);
  }

  private void updateChatTitle() {
    if (getSupportActionBar() != null && !messages.isEmpty()) {
      String firstUserMessage = null;
      for (ChatMessage msg : messages) {
        if (msg.getType() == ChatMessage.TYPE_USER) {
          firstUserMessage = msg.getContent();
          if (firstUserMessage != null && firstUserMessage.length() > 30) {
            firstUserMessage = firstUserMessage.substring(0, 30) + "...";
          }
          break;
        }
      }
      if (firstUserMessage != null && !firstUserMessage.isEmpty()) {
        getSupportActionBar().setTitle(firstUserMessage);
        chatRepository.updateChatTitle(currentChatId, firstUserMessage);
      }
    }
  }

  private void setupProviderDropdown() {
    ArrayAdapter<String> dropdownAdapter =
        new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, PROVIDER_LABELS);
    actvProvider.setAdapter(dropdownAdapter);

    String currentProvider = prefs.getSelectedProvider();
    for (int i = 0; i < PROVIDERS.length; i++) {
      if (PROVIDERS[i].equals(currentProvider)) {
        actvProvider.setText(PROVIDER_LABELS[i], false);
        break;
      }
    }

    actvProvider.setOnItemClickListener(
        (parent, view, position, id) -> {
          String selected = PROVIDERS[position];
          prefs.setSelectedProvider(selected);
          if (!prefs.hasApiKeyForProvider(selected)) {
            Toast.makeText(
                    this, "API key not set. Go to Settings → AI Settings.", Toast.LENGTH_LONG)
                .show();
          }
          refreshClient();
        });
  }

  private void setupChatRecyclerView() {
    adapter = new ChatAdapter(messages);
    LinearLayoutManager lm = new LinearLayoutManager(this);
    lm.setStackFromEnd(true);
    recyclerView.setLayoutManager(lm);
    recyclerView.setAdapter(adapter);
  }

  private void setupAttachedFilesRecyclerView() {
    attachedFilesAdapter =
        new AttachedFilesAdapter(
            attachedFiles,
            position -> {
              attachedFiles.remove(position);
              attachedFilesAdapter.notifyItemRemoved(position);
              updateAttachedFilesVisibility();
            });
    LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
    rvAttachedFiles.setLayoutManager(lm);
    rvAttachedFiles.setAdapter(attachedFilesAdapter);
    rvAttachedFiles.setVisibility(View.GONE);
  }

  private void setupSendButton() {
    btnSend.setOnClickListener(v -> sendMessage());
    etInput.setOnEditorActionListener(
        (v, actionId, event) -> {
          sendMessage();
          return true;
        });
  }

  private void setupAttachButton() {
    btnAttach.setOnClickListener(v -> openFilePicker());
  }

  private void openFilePicker() {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("*/*");
    intent.putExtra(
        Intent.EXTRA_MIME_TYPES,
        new String[] {
          "image/*", "text/plain", "text/x-java-source", "text/x-kotlin",
          "application/json", "application/xml", "text/html", "text/css",
          "application/javascript", "application/pdf"
        });
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    filePicker.launch(Intent.createChooser(intent, "Attach file"));
  }

  private void handlePickedUri(Uri uri) {
    if (uri == null || isFinishing() || isDestroyed()) return;
    try {
      getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (SecurityException ignored) {
    }

    String mime = FileReadUtils.getMimeType(this, uri);
    boolean isImage = FileReadUtils.isImageMime(mime);
    int type = isImage ? AttachedFile.TYPE_IMAGE : AttachedFile.TYPE_TEXT;
    String name = resolveFileName(uri);
    AttachedFile file = new AttachedFile(uri, name, type);

    executor.execute(
        () -> {
          if (isFinishing() || isDestroyed()) return;
          try {
            if (isImage) {
              String b64 = FileReadUtils.readImageAsBase64(this, uri);
              if (isFinishing() || isDestroyed()) return;
              file.setBase64Data(b64);
            } else {
              String text = FileReadUtils.readTextFile(this, uri);
              if (isFinishing() || isDestroyed()) return;
              file.setTextContent(text);
            }
            runOnUiThread(
                () -> {
                  if (isFinishing() || isDestroyed()) return;
                  attachedFiles.add(file);
                  attachedFilesAdapter.notifyItemInserted(attachedFiles.size() - 1);
                  updateAttachedFilesVisibility();
                });
          } catch (Exception e) {
            if (isFinishing() || isDestroyed()) return;
            runOnUiThread(
                () ->
                    Toast.makeText(
                            this, "Could not read file: " + e.getMessage(), Toast.LENGTH_SHORT)
                        .show());
          }
        });
  }

  private String resolveFileName(Uri uri) {
    try (android.database.Cursor cursor =
        getContentResolver()
            .query(
                uri,
                new String[] {android.provider.OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null)) {
      if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
    } catch (Exception ignored) {
    }
    String path = uri.getLastPathSegment();
    return path != null ? path : "file";
  }

  private void updateAttachedFilesVisibility() {
    rvAttachedFiles.setVisibility(attachedFiles.isEmpty() ? View.GONE : View.VISIBLE);
  }

  private void sendMessage() {
    String input = etInput.getText() != null ? etInput.getText().toString().trim() : "";
    if (TextUtils.isEmpty(input) && attachedFiles.isEmpty()) return;

    String provider = prefs.getSelectedProvider();
    if (!prefs.hasApiKeyForProvider(provider)) {
      Toast.makeText(this, "Please set the API key in Settings → AI Settings.", Toast.LENGTH_LONG)
          .show();
      return;
    }

    List<AttachedFile> snapshot = new ArrayList<>(attachedFiles);
    attachedFiles.clear();
    attachedFilesAdapter.notifyDataSetChanged();
    updateAttachedFilesVisibility();
    etInput.setText("");
    setInputEnabled(false);

    String firstImageUri = null;
    for (AttachedFile f : snapshot) {
      if (f.isImage()) {
        firstImageUri = f.getUri().toString();
        break;
      }
    }

    StringBuilder displayText = new StringBuilder();
    if (!TextUtils.isEmpty(input)) displayText.append(input);
    for (AttachedFile f : snapshot) {
      if (displayText.length() > 0) displayText.append("\n");
      displayText.append("📎 ").append(f.getName());
    }

    addMessage(
        new ChatMessage(displayText.toString(), ChatMessage.TYPE_USER, provider, firstImageUri));
    addMessage(new ChatMessage("", ChatMessage.TYPE_LOADING, provider));

    currentClient.sendMessage(
        messages,
        input,
        snapshot,
        new AiClient.Callback() {
          @Override
          public void onSuccess(String response) {
            removeLastMessage();
            addMessage(new ChatMessage(response, ChatMessage.TYPE_AI, provider));
            setInputEnabled(true);
          }

          @Override
          public void onError(String errorMessage) {
            removeLastMessage();
            addMessage(new ChatMessage("Error: " + errorMessage, ChatMessage.TYPE_ERROR, provider));
            setInputEnabled(true);
          }
        });
  }

  private void setInputEnabled(boolean enable) {
    runOnUiThread(
        () -> {
          etInput.setEnabled(enable);
          btnSend.setEnabled(enable);
          btnAttach.setEnabled(enable);
          if (enable) etInput.requestFocus();
        });
  }

  private void addMessage(ChatMessage message) {
    runOnUiThread(
        () -> {
          if (currentChatId == -1) {
            currentChatId = chatRepository.createNewChat("", System.currentTimeMillis());
          }

          messages.add(message);
          adapter.notifyItemInserted(messages.size() - 1);
          recyclerView.smoothScrollToPosition(messages.size() - 1);
          if (message.getType() != ChatMessage.TYPE_LOADING
              && message.getType() != ChatMessage.TYPE_ERROR) {
            chatRepository.saveMessage(currentChatId, message);
            if (message.getType() == ChatMessage.TYPE_USER && messages.size() == 1) {
              updateChatTitle();
            }
          }
        });
  }

  private void removeLastMessage() {
    runOnUiThread(
        () -> {
          if (!messages.isEmpty()) {
            int last = messages.size() - 1;
            messages.remove(last);
            adapter.notifyItemRemoved(last);
          }
        });
  }

  private void refreshClient() {
    currentClient = AiClientFactory.create(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_ai_chat, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {

      getOnBackPressedDispatcher().onBackPressed();
      return true;
    } else if (item.getItemId() == R.id.action_new_chat) {
      Snackbar.make(findViewById(android.R.id.content), "New Chat", Snackbar.LENGTH_LONG).show();
      startNewChat();
      return true;
    } else if (item.getItemId() == R.id.action_clear_chat) {

      messages.clear();
      adapter.notifyDataSetChanged();
      if (currentChatId != -1) {
        chatRepository.deleteChat(currentChatId);
      }
      currentChatId = -1;
      if (getSupportActionBar() != null) getSupportActionBar().setTitle("Ghost AI");
      Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
      return true;
    } else if (item.getItemId() == R.id.action_history) {
      HistoryBottomSheetFragment bottomSheet = new HistoryBottomSheetFragment();
      bottomSheet.setOnChatsSelectedListener(
          new HistoryBottomSheetFragment.OnChatsSelectedListener() {
            @Override
            public void onLoadChat(long chatId) {
              currentChatId = chatId;
              messages.clear();
              messages.addAll(chatRepository.getMessagesByChatId(currentChatId));
              adapter.notifyDataSetChanged();
              recyclerView.scrollToPosition(messages.size() - 1);
              updateChatTitle();
            }

            @Override
            public void onDeleteChats(List<Long> chatIds) {
              chatRepository.deleteChats(chatIds);
              if (chatIds.contains(currentChatId)) {

                currentChatId = -1;
                messages.clear();
                adapter.notifyDataSetChanged();
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Ghost AI");
              }
            }
          });
      bottomSheet.show(getSupportFragmentManager(), "history");
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    executor.shutdownNow();
  }
}
