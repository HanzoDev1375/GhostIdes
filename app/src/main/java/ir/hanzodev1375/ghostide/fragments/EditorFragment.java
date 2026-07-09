package ir.hanzodev1375.ghostide.fragments;

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
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.lang.Language;
import ir.hanzodev1375.ghostide.activity.EditorActivity;
import ir.hanzodev1375.ghostide.editorlangs.LanguageManager;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.databinding.EditorFragmentBinding;
import ir.hanzodev1375.ghostide.mvvm.viewmodel.EditorViewModel;
import ir.hanzodev1375.ghostide.paged.PagedEditSession;
import ir.hanzodev1375.ghostide.tasks.FileChangeReceiver;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import ir.hanzodev1375.components.WebViewBottomSheetFragment;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditorFragment extends Fragment {
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

  /**
   * روی SDK های جدید (مثل 36)، وقتی میری تو یه اپ دیگه، پروسه ممکنه فریز یا کامل کشته بشه، و
   * FileObserver زنده (که فقط تا وقتی پروسه زنده و در جریانه کار می‌کنه) رویداد رو از دست میده یا
   * اصلاً از نو ساخته میشه. برای همین این چک، که مستقل از FileObserver‌ـه، تنها راه قابل‌اعتماده:
   * دقیقاً موقع onResume (که همیشه اجرا میشه، چه پروسه زنده مونده باشه چه از نو ساخته شده باشه)
   * mtime دیسک رو با آخرین مقداری که خودمون می‌دونستیم مقایسه می‌کنیم.
   */
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

  /** بعد از هر بارگذاری موفق (اولیه یا reload) یا ذخیره‌ی موفق باید صدا زده بشه. */
  private void updateKnownModifiedTime() {
    if (filePath != null) {
      lastKnownModifiedTime = new File(filePath).lastModified();
    }
  }

  /**
   * وقتی auto-save فعاله، خود اپ مدام فایل رو می‌نویسه، پس نیازی به دیدبانی تغییر خارجی نیست — واچر
   * اصلاً استارت نمی‌شه. وقتی auto-save خاموشه، واچر فعال میشه تا اگه یه اپ دیگه فایل رو عوض کرد
   * بفهمیم. عمداً توی onPause استاپ نمی‌کنیم؛ اگه بریم تو یه اپ دیگه فایل رو ادیت کنیم، دقیقاً همون
   * لحظه که پس‌زمینه‌ایم اتفاق میفته و اگه واچر رو خاموش کرده باشیم، همون تغییر از دست میره. واچینگ
   * رو فقط موقع بسته‌شدن واقعی تب (onDestroyView) قطع می‌کنیم. اگه جایی توی تنظیمات auto-save رو در
   * حین باز بودن این فرگمنت toggle کردی، همین متد رو دوباره صدا بزن. این فقط یه لایه‌ی کمکیه؛ تشخیص
   * اصلی توی checkExternalChangesOnResume انجام میشه.
   */
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
        viewModel.saveFile(filePath, content, this::updateKnownModifiedTime);
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
          int bottomInset = Math.max(imeInsets.bottom, navInsets.bottom);
          v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
          return insets;
        });
  }
}
