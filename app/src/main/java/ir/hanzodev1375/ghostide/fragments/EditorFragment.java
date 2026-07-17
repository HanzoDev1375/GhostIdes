package ir.hanzodev1375.ghostide.fragments;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.LongPressEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.event.EditorFocusChangeEvent;
import io.github.rosemoe.sora.lang.Language;
import ir.hanzodev1375.ghostide.activity.EditorActivity;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.ClangdServer;
import ir.hanzodev1375.ghostide.editorlangs.LanguageManager;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.PylspServer;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.TsServer;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.PhpServer;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.databinding.EditorFragmentBinding;
import ir.hanzodev1375.ghostide.mvvm.viewmodel.EditorViewModel;
import ir.hanzodev1375.ghostide.paged.PagedEditSession;
import ir.hanzodev1375.ghostide.refactor.renameclass.ui.RenameClassBottomSheet;
import ir.hanzodev1375.ghostide.tasks.FileChangeReceiver;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import ir.hanzodev1375.components.WebViewBottomSheetFragment;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditorFragment extends Fragment {

  private static File findSourceRootForPackageDirectory(File directory) {
    File current = directory;
    while (current != null) {
      String name = current.getName();
      if (name.equals("java") || name.equals("kotlin")) {
        File parent = current.getParentFile();
        File grandParent = parent != null ? parent.getParentFile() : null;
        if (grandParent != null && grandParent.getName().equals("src")) {
          return current;
        }
      }
      current = current.getParentFile();
    }
    return null;
  }

  private static final long PAGED_EDIT_THRESHOLD = 2L * 1024 * 1024;
  private static final int PAGED_EDIT_PAGE_SIZE = 1024 * 1024;
  private final ExecutorService pagedExecutor = Executors.newSingleThreadExecutor();
  private EditorFragmentBinding binding;
  private EditorViewModel viewModel;
  private IdeEditor editor;
  private String filePath;
  private long lastKnownModifiedTime = -1;
  private ThemeUtils theme;
  private PreferencesUtils setting;
  private PagedEditSession pagedSession;
  private int pageIndex = -1;
  private volatile LspEditor lspEditor;

  public static EditorFragment newInstance(String path) {
    EditorFragment f = new EditorFragment();
    Bundle args = new Bundle();
    args.putString("file_path", path);
    f.setArguments(args);
    return f;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = EditorFragmentBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    filePath = getArguments().getString("file_path");
    viewModel = new ViewModelProvider(this).get(EditorViewModel.class);
    editor = binding.editor;
    var manager = new ThemeManager(requireActivity());
    theme = new ThemeUtils(manager);
    theme.applyEditor(editor);
    applyImeInsets(binding.getRoot());
    setting = new PreferencesUtils(getContext());
    editor.subscribeEvent(
        ContentChangeEvent.class,
        (event, unevent) -> {
          if (event.getAction() == ContentChangeEvent.ACTION_SET_NEW_TEXT) return;
          if (getActivity() instanceof EditorActivity && filePath != null) {
            ((EditorActivity) getActivity()).setTabDirty(filePath, true);
          }
          if (setting.autoSaveFiles()) {
            saveCurrentFile();
          }
        });
    viewModel
        .getLoading()
        .observe(
            getViewLifecycleOwner(),
            loading -> {
              binding.prograssLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            });

    viewModel
        .getText()
        .observe(
            getViewLifecycleOwner(),
            content -> {
              var b = new Bundle();
              b.putString("path", filePath);
              if (content != null) editor.setText(content, b);
              updateKnownModifiedTime();
            });

    if (filePath != null) {
      File file = new File(filePath);
      if (file.exists() && file.length() > PAGED_EDIT_THRESHOLD) {
        openPagedSession(file);
      } else {
        viewModel.loadFile(filePath);
      }
    }
    Language lang = LanguageManager.resolve(getContext(), filePath);
    if (lang != null) editor.setEditorLanguage(lang);
    boolean isPython = filePath != null && filePath.toLowerCase(Locale.ROOT).endsWith(".py");
    boolean isCpp = filePath != null && ClangdServer.isCppFile(filePath);
    boolean isJs = filePath != null && TsServer.isJsFile(filePath);
    boolean isPhp = filePath != null && PhpServer.isPhpFile(filePath);
    if (isPython || isCpp || isJs || isPhp) {
      final String targetFilePath = filePath;
      final Context appContext = getContext() != null ? getContext().getApplicationContext() : null;
      final IdeEditor targetEditor = editor;
      if (appContext != null) {
        new Thread(
                () -> {
                  File targetFile = new File(targetFilePath);
                  String projectRoot =
                      targetFile.getParent() != null
                          ? targetFile.getParent()
                          : targetFile.getAbsolutePath();
                  if (isPython) {
                    lspEditor =
                        PylspServer.connectFile(
                            appContext, projectRoot, targetFilePath, targetEditor);
                  } else if (isCpp) {
                    lspEditor =
                        ClangdServer.connectFile(
                            appContext, projectRoot, targetFilePath, targetEditor);
                  } else if (isJs) {
                    lspEditor =
                        TsServer.connectFile(
                            appContext, projectRoot, targetFilePath, targetEditor);
                  } else {
                    lspEditor =
                        PhpServer.connectFile(
                            appContext, projectRoot, targetFilePath, targetEditor);
                  }
                })
            .start();
      }
    }
    GradientDrawable color = (GradientDrawable) binding.tvCursorPosition.getBackground().mutate();
    color.setColor(theme.getMenuColor());
    editor.subscribeEvent(
        SelectionChangeEvent.class,
        (event, unevent) -> {
          var cursor = editor.getCursor();
          binding.tvCursorPosition.setText(
              "L " + (cursor.getLeftLine() + 1) + ", C " + (cursor.getLeftColumn() + 1));
        });
    binding.tvCursorPosition.setVisibility(
        setting.getShowLineColPanel() ? View.VISIBLE : View.GONE);
    editor.subscribeEvent(
        LongPressEvent.class,
        (event, unevent) -> {
          if (filePath == null) return;
          if (!filePath.endsWith(".java") && !filePath.endsWith(".kt")) return;
          File file = new File(filePath);
          File sourceRoot = findSourceRootForPackageDirectory(file.getParentFile());
          File moduleRoot =
              sourceRoot != null
                      && sourceRoot.getParentFile() != null
                      && sourceRoot.getParentFile().getParentFile() != null
                  ? sourceRoot.getParentFile().getParentFile().getParentFile()
                  : file.getParentFile();
          if (moduleRoot == null || getActivity() == null) return;
          String name = file.getName();
          int dot = name.lastIndexOf('.');
          String className = dot > 0 ? name.substring(0, dot) : name;
          RenameClassBottomSheet.newInstance(
                  moduleRoot.getAbsolutePath(), file.getAbsolutePath(), className)
              .show(getActivity().getSupportFragmentManager(), RenameClassBottomSheet.TAG);
        });

    binding.ivWrapToggle1.setOnClickListener(v -> goToPreviousPage());
    binding.ivWrapToggle2.setOnClickListener(v -> goToNextPage());
    theme.applyViewPagePanel(
        binding.ivWrapToggle1, binding.ivWrapToggle2, binding.tvWrapInfo, binding.llWrapIndicator);
    if (filePath != null) {
      binding.editor.setCurrentFilePath(filePath);
    }
    binding.editor.setOnLinkClick(
        links -> {
          var webs = WebViewBottomSheetFragment.newInstance(links);
          theme.applyWebViewBottomSheetFragment(webs);
          webs.show(getParentFragmentManager(), "WebViewBottomSheet");
        });
  }

  @Override
  public void onResume() {
    super.onResume();
    checkExternalChangesOnResume();
    refreshFileWatching();
  }

  private void checkExternalChangesOnResume() {
    if (filePath == null) return;
    File file = new File(filePath);
    if (!file.exists()) return;

    long diskModifiedTime = file.lastModified();
    if (lastKnownModifiedTime > 0 && diskModifiedTime != lastKnownModifiedTime) {
      FileChangeReceiver.showFileChangedDialog(
          requireActivity(),
          filePath,
          () -> {
            if (viewModel != null) viewModel.loadFile(filePath);
          });
    }
    lastKnownModifiedTime = diskModifiedTime;
  }

  private void updateKnownModifiedTime() {
    if (filePath != null) {
      lastKnownModifiedTime = new File(filePath).lastModified();
    }
  }

  private void refreshFileWatching() {
    FileChangeReceiver.stopWatching();
    if (filePath != null && !setting.autoSaveFiles()) {
      FileChangeReceiver.startWatching(
          requireActivity(),
          filePath,
          changedPath ->
              FileChangeReceiver.showFileChangedDialog(
                  requireActivity(),
                  changedPath,
                  () -> {
                    if (viewModel != null) viewModel.loadFile(changedPath);
                  }));
    }
  }

  private void openPagedSession(File file) {
    var context = requireContext();
    binding.prograssLoading.setVisibility(View.VISIBLE);
    pagedExecutor.execute(
        () -> {
          try {
            File tmpDir =
                new File(context.getCacheDir(), "paged_edit_" + System.currentTimeMillis());
            PagedEditSession session;
            try (Reader reader = new FileReader(file)) {
              session = new PagedEditSession(reader, tmpDir, PAGED_EDIT_PAGE_SIZE);
            }
            pagedSession = session;
            pageIndex = 0;
            if (!isAdded() || binding == null) return;
            requireActivity()
                .runOnUiThread(
                    () -> {
                      if (binding == null || pagedSession == null) return;
                      pagedSession.loadPageToEditor(
                          0,
                          editor,
                          new PagedEditSession.Callback() {
                            @Override
                            public void onSuccess() {
                              if (binding == null) return;
                              binding.prograssLoading.setVisibility(View.GONE);
                              binding.llWrapIndicator.setVisibility(View.VISIBLE);
                              updatePageIndicator();
                              updateKnownModifiedTime();
                            }

                            @Override
                            public void onError(IOException e) {
                              Log.e("EditorFragment", "خطا در صفحه‌بندی فایل", e);
                              if (binding != null) binding.prograssLoading.setVisibility(View.GONE);
                            }
                          });
                    });
          } catch (IOException e) {
            Log.e("EditorFragment", "خطا در باز کردن فایل بزرگ", e);
            if (!isAdded()) return;
            requireActivity()
                .runOnUiThread(
                    () -> {
                      if (binding != null) binding.prograssLoading.setVisibility(View.GONE);
                    });
          }
        });
  }

  private void updatePageIndicator() {
    if (binding == null || pagedSession == null) return;
    binding.tvWrapInfo.setText("Page " + (pageIndex + 1) + "/" + pagedSession.getPageCount());
  }

  private PagedEditSession.Callback logOnlyCallback() {
    return new PagedEditSession.Callback() {
      @Override
      public void onSuccess() {
        updatePageIndicator();
      }

      @Override
      public void onError(IOException e) {
        Log.e("EditorFragment", "خطا در صفحه‌بندی فایل", e);
      }
    };
  }

  private void goToNextPage() {
    if (pagedSession == null || pageIndex == -1 || pageIndex >= pagedSession.getPageCount() - 1) {
      return;
    }
    int current = pageIndex;
    pagedSession.unloadPageFromEditor(
        current,
        editor,
        new PagedEditSession.Callback() {
          @Override
          public void onSuccess() {
            pageIndex = current + 1;
            pagedSession.loadPageToEditor(pageIndex, editor, logOnlyCallback());
          }

          @Override
          public void onError(IOException e) {
            Log.e("EditorFragment", "خطا در رفتن به صفحه بعد", e);
          }
        });
  }

  private void goToPreviousPage() {
    if (pagedSession == null || pageIndex <= 0) return;
    int current = pageIndex;
    pagedSession.unloadPageFromEditor(
        current,
        editor,
        new PagedEditSession.Callback() {
          @Override
          public void onSuccess() {
            pageIndex = current - 1;
            pagedSession.loadPageToEditor(pageIndex, editor, logOnlyCallback());
          }

          @Override
          public void onError(IOException e) {
            Log.e("EditorFragment", "خطا در رفتن به صفحه قبل", e);
          }
        });
  }

  public void saveCurrentFile() {
    if (pagedSession != null) {
      saveCurrentPagedFile();
      return;
    }
    if (filePath != null && viewModel != null && editor != null) {
      String content = editor.getText().toString();
      if (content != null) {
        FileChangeReceiver.notifyInternalWrite();
        viewModel.saveFile(
            filePath,
            content,
            () -> {
              updateKnownModifiedTime();
              if (getActivity() instanceof EditorActivity) {
                ((EditorActivity) getActivity()).setTabDirty(filePath, false);
              }
            });
      } else {
        Log.e("EditorFragment", "محتوای ادیتور نال است");
      }
    }
  }

  private void saveCurrentPagedFile() {
    if (pagedSession == null || pageIndex == -1 || filePath == null) return;
    pagedSession.unloadPageFromEditor(
        pageIndex,
        editor,
        new PagedEditSession.Callback() {
          @Override
          public void onSuccess() {
            FileChangeReceiver.notifyInternalWrite();
            pagedSession.writeTo(
                new File(filePath),
                new PagedEditSession.Callback() {
                  @Override
                  public void onSuccess() {
                    Log.d("EditorFragment", "فایل بزرگ ذخیره شد: " + filePath);
                    updateKnownModifiedTime();
                    if (getActivity() instanceof EditorActivity) {
                      ((EditorActivity) getActivity()).setTabDirty(filePath, false);
                    }
                  }

                  @Override
                  public void onError(IOException e) {
                    Log.e("EditorFragment", "خطا در ذخیره فایل بزرگ", e);
                  }
                });
          }

          @Override
          public void onError(IOException e) {
            Log.e("EditorFragment", "خطا در ذخیره فایل بزرگ", e);
          }
        });
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    FileChangeReceiver.stopWatching();
    if (lspEditor != null) {
      final LspEditor toClose = lspEditor;
      lspEditor = null;
      new Thread(
              () -> {
                try {
                  toClose.dispose();
                } catch (Exception e) {
                  Log.e("EditorFragment", "بستن اتصال lsp با خطا مواجه شد", e);
                }
              })
          .start();
    }
    pagedExecutor.shutdownNow();
    if (pagedSession != null) {
      pagedSession.close();
      pagedSession = null;
    }
    binding = null;
  }

  public IdeEditor getEditor() {
    return editor;
  }

  void applyImeInsets(@NonNull final View target) {
    ViewCompat.setOnApplyWindowInsetsListener(
        target,
        (v, insets) -> {
          Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
          Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
          boolean thisEditorIsFocused = editor != null && editor.hasFocus();
          int bottomInset =
              thisEditorIsFocused ? Math.max(imeInsets.bottom, navInsets.bottom) : navInsets.bottom;
          v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
          return insets;
        });

    if (editor != null) {
      editor.subscribeEvent(
          EditorFocusChangeEvent.class,
          (event, un) -> {
            ViewCompat.requestApplyInsets(target);
          });
    }
  }
}
