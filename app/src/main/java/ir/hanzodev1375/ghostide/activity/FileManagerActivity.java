package ir.hanzodev1375.ghostide.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.blankj.utilcode.util.ClipboardUtils;
import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ir.ghostide.logcat.BottomSheetLogView;
import ir.hanzodev1375.components.RenameDialogFragment;
import ir.hanzodev1375.components.TextInputDialogFragment;
import ir.hanzodev1375.components.ftp.interfaces.RemoteClient;
import ir.hanzodev1375.components.ftp.views.FtpConnectSheet;
import ir.hanzodev1375.components.ftp.views.FtpBrowserSheet;
import ir.hanzodev1375.components.searchdata.model.FileSearchResult;
import ir.hanzodev1375.components.searchdata.ui.SearchBottomSheet;
import ir.hanzodev1375.components.searchdata.interfaces.OnLineClickListener;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.CustomItemSheet;
import ir.hanzodev1375.components.ui.ProfileView;
import ir.hanzodev1375.ghostide.adapters.FileManagerAdapter;
import ir.hanzodev1375.ghostide.adapters.ToolbarAdapter;
import ir.hanzodev1375.ghostide.adapters.ZipBrowserAdapter;
import ir.hanzodev1375.ghostide.ai.chat.AiChatActivity;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.databinding.ActivityFilemanagerBinding;
import ir.hanzodev1375.ghostide.databinding.SelectionPanelBinding;
import ir.hanzodev1375.ghostide.dialogs.CopyProgressDialog;
import ir.hanzodev1375.ghostide.dialogs.DeleteProgressDialog;
import ir.hanzodev1375.ghostide.fragments.BatchRenameSheet;
import ir.hanzodev1375.ghostide.fragments.FilePropertiesSheet;
import ir.hanzodev1375.ghostide.jgit.GitHubClient;
import ir.hanzodev1375.ghostide.jgit.GitHubProfileSheet;
import ir.hanzodev1375.ghostide.jgit.fragments.GitBottomSheetFragment;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitManager;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.FileChange;
import ir.hanzodev1375.ghostide.models.FileManagerModel;
import ir.hanzodev1375.ghostide.models.ZipEntryModel;
import ir.hanzodev1375.ghostide.postman.PostManActivity;
import ir.hanzodev1375.ghostide.refactor.rename.ui.RenamePackageBottomSheet;
import ir.hanzodev1375.ghostide.bookmark.BookmarkBottomSheet;
import ir.hanzodev1375.ghostide.bookmark.BookmarkViewModel;
import ir.hanzodev1375.ghostide.models.ZipInfo;
import ir.hanzodev1375.ghostide.project.NewProjectDialog;
import ir.hanzodev1375.ghostide.history.HistoryBottomSheet;
import ir.hanzodev1375.ghostide.history.HistoryViewModel;

import ir.hanzodev1375.ghostide.mvvm.viewmodel.FileViewModel;
import ir.hanzodev1375.ghostide.adapters.FileManagerHostAdapter;
import ir.hanzodev1375.ghostide.ide.ui.api.IdeHostServices;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.plugin.PluginManager;
import ir.hanzodev1375.ghostide.plugin.api.Disposable;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
import ir.hanzodev1375.ghostide.plugin.gpl.GplInstalledPlugins;
import ir.hanzodev1375.ghostide.plugin.gpl.GplManifest;
import ir.hanzodev1375.ghostide.plugin.gpl.GplManifestReader;
import ir.hanzodev1375.ghostide.plugin.gpl.GplPluginLoader;
import ir.hanzodev1375.ghostide.adapters.PluginPopupAdapter;
import ir.hanzodev1375.ghostide.shizuku.ShizukuManager;
import androidx.core.content.FileProvider;
import ir.hanzodev1375.ghostide.terminal.activity.TerminalActivity;
import ir.hanzodev1375.ghostide.utils.MarginItemDecoration;
import ir.hanzodev1375.ghostide.utils.NetworkChangeReceiver;
import ir.hanzodev1375.ghostide.utils.ObjectUtil;
import ir.hanzodev1375.ghostide.utils.ShapeUtil;
import ir.hanzodev1375.ghostide.utils.ShortcutHelper;
import ir.hanzodev1375.ghostide.utils.StorageUtils;
import ir.hanzodev1375.ghostide.utils.ZipUtil;
import ir.hanzodev1375.ghostide.utils.zip.ZipOperationManager;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import ir.theme.themeeditor.ThemeEditorActivity;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import java.util.Set;
import ninja.coder.appuploader.main.appupdate.UpadteAppView;
import net.lingala.zip4j.ZipFile;
import ir.hanzodev1375.components.store.activitys.StoreActivity;
import ir.hanzodev1375.ghostide.translator.ui.StringsTranslatorSheet;
import irhanzodev1375.musicpreview.MusicPlayerBottomSheetFragment;

public class FileManagerActivity extends BaseCompat
    implements NetworkChangeReceiver.CallBackNetWork {

  private ActivityFilemanagerBinding bind;
  private FileViewModel viewModel;
  private Disposable fileManagerHostRegistration;
  private FileManagerAdapter adapter;
  private ZipBrowserAdapter zipAdapter;
  private View selectionPanel;
  private TextView selectionCount;
  private ImageView btnCopy, btnCut, btnDelete, btnPaste, btnClose, btnSelectall;
  private boolean isCutOperation = false;
  private List<FileManagerModel> pendingClipboard = new ArrayList<>();
  private SelectionPanelBinding selectionPanelBinding;
  private FileManagerModel fileModels;
  private UpadteAppView app;
  private PreferencesUtils appsetting;
  private ProfileView profileview;
  private ThemeUtils themeutil;
  private NetworkChangeReceiver networkChangeReceiver;
  private Set<String> itemname =
      new HashSet<>(
          Arrays.asList(
              ".html",
              ".java",
              ".c",
              ".cs",
              ".cpp",
              ".cxx",
              ".hpp",
              ".hxx",
              ".cc",
              ".h",
              ".css",
              ".js",
              ".py",
              ".json",
              ".xml",
              ".kt",
              ".kts",
              ".ts",
              ".tsx",
              ".toml",
              ".groovy",
              ".gradle",
              ".sass",
              ".scss",
              ".md",
              ".markdown",
              ".yml",
              ".yaml",
              ".lua",
              ".go",
              ".php",
              ".dart",
              ".tsx",
              ".jsx",
              ".sql",
              ".sh",
              ".rc",
              ".bash",
              ".bashrc",
              ".ash",
              ".zsh",
              ".zshrc",
              ".rs",
              ".rb",
              ".g4",
              ".ini",
              ".zig",
              ".vue",
              ".asm",
              ".s",
              ".nasm",
              ".swift",
              ".scala",
              ".sc",
              ".pl",
              ".pm",
              ".jl",
              ".r",
              ".ex",
              ".exs",
              ".hs",
              ".nim",
              ".sol"));
  private Set<String> images =
      new HashSet<>(
          Arrays.asList(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".avif", ".webp", ".svg"));
  private Set<String> audio =
      new HashSet<>(
          Arrays.asList(".mp3", ".wav", ".m4a", ".ogg", ".flac", ".aac", ".opus", ".wma"));
  private CopyProgressDialog copyProgressDialog;
  private DeleteProgressDialog deleteProgressDialog;
  private boolean isZipMode = false;
  private String currentZipFilePath = null;
  private HistoryViewModel historyViewModel;
  private BookmarkViewModel bookmarkViewModel;
  private final ExecutorService gitStatusExecutor = Executors.newSingleThreadExecutor();
  private Set<String> gitChangedAbsPaths = new HashSet<>();
  private GitViewModel gitViewModel;
  private final AtomicBoolean gitStatusRunning = new AtomicBoolean(false);
  private final AtomicBoolean gitStatusPending = new AtomicBoolean(false);
  private final ExecutorService ftpExecutor = Executors.newSingleThreadExecutor();
  private String currentDir;
  private int systemBarsBottomInset = 0;
  private MusicPlayerBottomSheetFragment musicBottomSheet;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    bind = ActivityFilemanagerBinding.inflate(getLayoutInflater());
    setContentView(bind.getRoot());
    setupInsets();
    setupSearchLayoutInsets();
    appsetting = new PreferencesUtils(this);
    var themeManager = new ThemeManager(this);
    themeutil = new ThemeUtils(themeManager);
    bind.fab.bindOfAcivity(this);
    if (appsetting.isShowBackground()) {
      themeutil.setFileManagerBack(bind.headline, bind.headtop, bind.backgroundiconfilemanager);
    } else {
      bind.headtop.setBackgroundColor(
          MaterialColors.getColor(bind.headtop, R.attr.colorSurfaceContainer));
      bind.headline.setBackground(ShapeUtil.shape(40f, this));
    }
    networkChangeReceiver = new NetworkChangeReceiver(this);
    IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    this.registerReceiver(networkChangeReceiver, filter);
    new Handler(Looper.getMainLooper())
        .postDelayed(
            () -> {
              try {
                PluginManager.getInstance().setCurrentFileManagerActivity(this);
              } catch (Exception e) {
                e.printStackTrace();
              }
            },
            100);

    viewModel = new ViewModelProvider(this).get(FileViewModel.class);
    fileManagerHostRegistration =
        GlobalRegistry.services()
            .register(IdeHostServices.FILE_MANAGER_HOST, new FileManagerHostAdapter(this));
    historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
    bookmarkViewModel = new ViewModelProvider(this).get(BookmarkViewModel.class);
    gitViewModel = new ViewModelProvider(this).get(GitViewModel.class);
    gitViewModel.changedFiles.observe(
        this,
        changes -> {
          String repoRoot = findGitRepositoryPath();
          if (repoRoot == null) return;
          applyChangedFiles(repoRoot, changes);
        });
    adapter = new FileManagerAdapter(this);
    bind.rvfiles.setLayoutManager(
        appsetting.getGridMod()
            ? new GridLayoutManager(this, appsetting.getGridSpanCount())
            : new LinearLayoutManager(this));
    bind.rvfiles.setAdapter(adapter);
    bind.rvfiles.addItemDecoration(new MarginItemDecoration(this));
    app = new UpadteAppView(this, bind.downloader, () -> {});
    stepSearch();
    adapter.setupSelectionTracker(bind.rvfiles);

    viewModel
        .getFiles()
        .observe(
            this,
            files -> {
              if (files != null && !files.isEmpty()) fileModels = files.get(0);
              adapter.submitList(new ArrayList<>(files));
              if (files == null || files.isEmpty()) {
                bind.emptystates.setVisibility(View.VISIBLE);
                bind.rvfiles.setVisibility(View.GONE);
              } else {
                bind.emptystates.setVisibility(View.GONE);
                bind.rvfiles.setVisibility(View.VISIBLE);
              }
              refreshGitStatus();
            });

    viewModel
        .getIsLoading()
        .observe(
            this, loading -> bind.loadingprogass.setVisibility(loading ? View.VISIBLE : View.GONE));

    viewModel.savePath(true);
    String startPath = getIntent().getStringExtra("start_path");
    if (startPath != null && new File(startPath).exists()) {
      viewModel.navigateTo(startPath);
    }
    viewModel
        .getCurrentPath()
        .observe(
            this,
            path -> {
              if (path != null) bind.navmodel.setFile(new File(path));
            });

    copyProgressDialog = new CopyProgressDialog(this);
    viewModel
        .getCopyProgress()
        .observe(
            this,
            progress -> {
              if (progress == null) return;
              if (progress.isRunning) {
                if (!copyProgressDialog.isShowing()) copyProgressDialog.show();
                copyProgressDialog.update(progress);
              } else {
                copyProgressDialog.dismiss();
              }
            });

    deleteProgressDialog = new DeleteProgressDialog(this);
    viewModel
        .getDeleteProgress()
        .observe(
            this,
            progress -> {
              if (progress == null) return;
              if (progress.isRunning) {
                if (!deleteProgressDialog.isShowing()) deleteProgressDialog.show();
                deleteProgressDialog.update(progress);
              } else {
                deleteProgressDialog.dismiss();
              }
            });

    adapter.setOnItemClickListener(
        (item, pos) -> {
          historyViewModel.addToHistory(item.getPath(), item.getName(), item.isDirectory());
          if (item.isDirectory()) {
            viewModel.navigateTo(item.getPath());
          } else if (item.getPath().toLowerCase().endsWith(".zip")) {
            enterZipMode(item.getPath());
          } else {
            setupClick(item.getPath(), item.getName());
          }
          String currentPath = viewModel.getCurrentPath().getValue();
          if (currentPath != null) {
            bind.gitActionButton.setVisibility(
                isGitRepository(currentPath) ? View.VISIBLE : View.GONE);
          }
          if (bind.ser.isShow()) {
            bind.ser.hide();
            bind.fab.setVisibility(View.VISIBLE);
            bind.ser.setQuery("");
          }
        });

    List<Integer> listIcon = new ArrayList<>();
    listIcon.add(R.drawable.folder);
    listIcon.add(R.drawable.ic_fileicon);
    listIcon.add(R.drawable.add);
    bind.fab
        .getRecyclerView()
        .setAdapter(
            new ToolbarAdapter(
                listIcon,
                (view2, mypos) -> {
                  switch (mypos) {
                    case 0 -> {
                      creatorFolder(fileModels);
                      bind.fab.dismiss();
                    }
                    case 1 -> {
                      creatorFile(fileModels);
                      bind.fab.dismiss();
                    }
                    case 2 -> {
                      String currentDir = viewModel.getCurrentPath().getValue();
                      if (currentDir != null) {
                        new NewProjectDialog(
                                FileManagerActivity.this,
                                currentDir,
                                projectPath ->
                                    runOnUiThread(
                                        () -> {
                                          viewModel.loadFiles(currentDir);
                                          Toast.makeText(
                                                  FileManagerActivity.this,
                                                  getString(R.string.project_created_toast),
                                                  Toast.LENGTH_SHORT)
                                              .show();
                                        }))
                            .show();
                      }
                      bind.fab.dismiss();
                    }
                  }
                }));

    bind.fab
        .getFab()
        .setOnClickListener(
            v -> {
              if (!bind.fab.isExpanded()) bind.fab.expand();
              else bind.fab.collapse();
            });

    bind.navmodel
        .getAdapter()
        .setOnItemClickListener((view, nav, pos) -> viewModel.navigateTo(nav.getFilePath()));

    bind.navmodel.setOnNavigateListener(
        path -> {
          viewModel.navigateTo(path);
          bind.gitActionButton.setVisibility(isGitRepository(path) ? View.VISIBLE : View.GONE);
        });
    bind.btnGoToDir.setOnClickListener(this::showGoToDirMenu);

    stepMoreAdapter();
    setupSelectionPanel();

    adapter.setSelectionStateListener(
        new FileManagerAdapter.SelectionStateListener() {
          @Override
          public void onSelectionChanged(int count) {
            if (count == 0 && pendingClipboard.isEmpty()) {
              if (selectionPanel != null) selectionPanel.setVisibility(View.GONE);
            } else if (count > 0) {
              selectionPanel.setVisibility(View.VISIBLE);
              selectionCount.setText(getString(R.string.selected_items_count, count));
            } else if (count == 0 && !pendingClipboard.isEmpty()) {
              selectionCount.setText("0");
              selectionPanel.setVisibility(View.VISIBLE);
            }
          }

          @Override
          public void onSelectionModeStarted() {}

          @Override
          public void onSelectionModeEnded() {
            if (pendingClipboard.isEmpty() && selectionPanel != null) {
              selectionPanel.setVisibility(View.GONE);
            }
          }
        });

    bind.buttonAi.setOnClickListener(
        v -> startActivity(new Intent(getApplicationContext(), AiChatActivity.class)));

    bind.buttonPlugins.setOnClickListener(this::showPluginPopup);

    setOnBackPress();
    setupGitButton();
    observePathForGit();
    initZipBrowserAdapter();
  }

  private void initZipBrowserAdapter() {
    zipAdapter = new ZipBrowserAdapter(this);
    zipAdapter.setZipLoadListener(
        new ZipBrowserAdapter.ZipLoadListener() {
          @Override
          public void onLoadStarted() {
            bind.loadingprogass.setVisibility(View.VISIBLE);
          }

          @Override
          public void onLoadFinished(String internalPath, boolean hasParent) {
            bind.loadingprogass.setVisibility(View.GONE);
          }

          @Override
          public void onLoadError(String message) {
            bind.loadingprogass.setVisibility(View.GONE);
            Toast.makeText(FileManagerActivity.this, "خطا: " + message, Toast.LENGTH_SHORT).show();
            exitZipMode();
          }
        });
    zipAdapter.setOnItemClickListener(
        (item, position) -> {
          if (item.isDirectory()) {
            zipAdapter.loadZip(currentZipFilePath, item.getEntryPath());
          } else {
            extractAndOpenZipEntry(item);
          }
        });
    zipAdapter.setOnMoreClickListener(
        (item, anchor, pos) -> {
          List<String> items =
              List.of(
                  getString(R.string.removed),
                  getString(R.string.rename),
                  getString(R.string.zip_extract_here),
                  getString(R.string.zip_extract_to),
                  getString(R.string.zip_info));
          ObjectUtil.showGlassMenu(
              FileManagerActivity.this,
              anchor,
              items,
              (index, title) -> {
                ZipOperationManager zipOp = new ZipOperationManager();
                String destDefault = new File(currentZipFilePath).getParent();
                switch (index) {
                  case 0 -> new MaterialAlertDialogBuilder(FileManagerActivity.this)
                      .setTitle(getString(R.string.removed))
                      .setMessage(getString(R.string.removedmassges, item.getName()))
                      .setPositiveButton(
                          getString(R.string.ok),
                          (d, w) ->
                              zipOp.deleteEntries(
                                  currentZipFilePath,
                                  item.getEntryPath(),
                                  new ZipOperationManager.Callback() {
                                    @Override
                                    public void onSuccess(String msg) {
                                      Toast.makeText(
                                              FileManagerActivity.this,
                                              getString(R.string.zip_deleted_ok),
                                              Toast.LENGTH_SHORT)
                                          .show();
                                      zipAdapter.loadZip(
                                          currentZipFilePath, zipAdapter.getCurrentInternalPath());
                                    }

                                    @Override
                                    public void onError(String err) {
                                      Toast.makeText(
                                              FileManagerActivity.this,
                                              getString(R.string.zip_error_prefix, err),
                                              Toast.LENGTH_SHORT)
                                          .show();
                                    }
                                  }))
                      .setNegativeButton(getString(R.string.cancel), null)
                      .show();
                  case 1 -> {
                    RenameDialogFragment dialog =
                        RenameDialogFragment.getInstance(
                            item.getName(),
                            (prefix, extension) -> {
                              String newName =
                                  (extension != null && !extension.isEmpty())
                                      ? prefix + "." + extension
                                      : prefix;
                              zipOp.renameEntry(
                                  currentZipFilePath,
                                  item.getEntryPath(),
                                  newName,
                                  new ZipOperationManager.Callback() {
                                    @Override
                                    public void onSuccess(String msg) {
                                      Toast.makeText(
                                              FileManagerActivity.this,
                                              getString(R.string.zip_renamed_ok),
                                              Toast.LENGTH_SHORT)
                                          .show();
                                      zipAdapter.loadZip(
                                          currentZipFilePath, zipAdapter.getCurrentInternalPath());
                                    }

                                    @Override
                                    public void onError(String err) {
                                      Toast.makeText(
                                              FileManagerActivity.this,
                                              getString(R.string.zip_error_prefix, err),
                                              Toast.LENGTH_SHORT)
                                          .show();
                                    }
                                  });
                            });
                    dialog.show(getSupportFragmentManager(), RenameDialogFragment.TAG);
                  }
                  case 2 -> zipOp.extractSingle(
                      currentZipFilePath,
                      item.getEntryPath(),
                      destDefault,
                      new ZipOperationManager.Callback() {
                        @Override
                        public void onSuccess(String msg) {
                          Toast.makeText(
                                  FileManagerActivity.this,
                                  getString(R.string.zip_extracted_ok),
                                  Toast.LENGTH_SHORT)
                              .show();
                        }

                        @Override
                        public void onError(String err) {
                          Toast.makeText(
                                  FileManagerActivity.this,
                                  getString(R.string.zip_error_prefix, err),
                                  Toast.LENGTH_SHORT)
                              .show();
                        }
                      });
                  case 3 -> new MaterialAlertDialogBuilder(FileManagerActivity.this)
                      .setTitle(getString(R.string.zip_extract_to))
                      .setMessage(getString(R.string.zip_extract_dest, destDefault))
                      .setPositiveButton(
                          getString(R.string.ok),
                          (d, w) ->
                              zipOp.extractSingle(
                                  currentZipFilePath,
                                  item.getEntryPath(),
                                  destDefault,
                                  new ZipOperationManager.Callback() {
                                    @Override
                                    public void onSuccess(String msg) {
                                      Toast.makeText(
                                              FileManagerActivity.this,
                                              getString(R.string.zip_extracted_ok),
                                              Toast.LENGTH_SHORT)
                                          .show();
                                    }

                                    @Override
                                    public void onError(String err) {
                                      Toast.makeText(
                                              FileManagerActivity.this,
                                              getString(R.string.zip_error_prefix, err),
                                              Toast.LENGTH_SHORT)
                                          .show();
                                    }
                                  }))
                      .setNegativeButton(getString(R.string.cancel), null)
                      .show();
                  case 4 -> zipOp.getZipInfo(
                      currentZipFilePath,
                      new ZipOperationManager.ZipInfoCallback() {
                        @Override
                        public void onInfo(ZipInfo info) {
                          new MaterialAlertDialogBuilder(FileManagerActivity.this)
                              .setTitle(getString(R.string.zip_info))
                              .setMessage(
                                  getString(R.string.zip_info_files, info.fileCount)
                                      + "\n"
                                      + getString(R.string.zip_info_dirs, info.dirCount)
                                      + "\n"
                                      + getString(
                                          R.string.zip_info_original,
                                          formatSize(info.totalUncompressed))
                                      + "\n"
                                      + getString(
                                          R.string.zip_info_compressed,
                                          formatSize(info.totalCompressed))
                                      + "\n"
                                      + getString(R.string.zip_info_ratio, info.compressionRatio)
                                      + "\n"
                                      + getString(
                                          R.string.zip_info_encrypted,
                                          info.isEncrypted
                                              ? getString(R.string.zip_info_yes)
                                              : getString(R.string.zip_info_no)))
                              .setPositiveButton(getString(R.string.ok), null)
                              .show();
                        }

                        @Override
                        public void onError(String err) {
                          Toast.makeText(
                                  FileManagerActivity.this,
                                  getString(R.string.zip_error_prefix, err),
                                  Toast.LENGTH_SHORT)
                              .show();
                        }
                      });
                }
              });
        });
    zipAdapter.setSelectionStateListener(
        new ZipBrowserAdapter.SelectionStateListener() {
          @Override
          public void onSelectionChanged(int count) {
            if (count == 0 && pendingClipboard.isEmpty()) {
              if (selectionPanel != null) selectionPanel.setVisibility(View.GONE);
            } else if (count > 0) {
              selectionPanel.setVisibility(View.VISIBLE);
              selectionCount.setText(getString(R.string.selected_items_count, count));
            } else if (count == 0 && !pendingClipboard.isEmpty()) {
              selectionCount.setText("0");
              selectionPanel.setVisibility(View.VISIBLE);
            }
          }

          @Override
          public void onSelectionModeStarted() {}

          @Override
          public void onSelectionModeEnded() {}
        });
  }

  private void enterZipMode(String zipFilePath) {
    isZipMode = true;
    currentZipFilePath = zipFilePath;
    bind.rvfiles.setAdapter(zipAdapter);
    zipAdapter.setupSelectionTracker(bind.rvfiles);
    zipAdapter.loadZip(zipFilePath, "");
    bind.fab.setVisibility(View.GONE);
    bind.gitActionButton.setVisibility(View.GONE);
    // bind.navmodel.setVisibility(View.GONE);
  }

  private void exitZipMode() {
    isZipMode = false;
    currentZipFilePath = null;
    bind.rvfiles.setAdapter(adapter);
    adapter.setupSelectionTracker(bind.rvfiles);
    viewModel.loadFiles(viewModel.getCurrentPath().getValue());
    bind.fab.setVisibility(View.VISIBLE);
    // bind.navmodel.setVisibility(View.VISIBLE);
    String currentPath = viewModel.getCurrentPath().getValue();
    if (currentPath != null) {
      bind.gitActionButton.setVisibility(isGitRepository(currentPath) ? View.VISIBLE : View.GONE);
    }
  }

  private void extractAndOpenZipEntry(ZipEntryModel entry) {
    File cacheDir = new File(getCacheDir(), "zip_extract");
    if (!cacheDir.exists()) cacheDir.mkdirs();
    File outFile = new File(cacheDir, entry.getName());
    new Thread(
            () -> {
              try (ZipFile zipFile = new ZipFile(entry.getParentZipPath())) {
                zipFile.extractFile(
                    entry.getEntryPath(), cacheDir.getAbsolutePath(), entry.getName());
                runOnUiThread(
                    () -> {
                      if (entry.isEncrypted()) {
                        Toast.makeText(
                                FileManagerActivity.this, "File Has Encrypted", Toast.LENGTH_LONG)
                            .show();
                      } else setupClick(outFile.getAbsolutePath(), entry.getName());
                    });
              } catch (Exception e) {
                runOnUiThread(
                    () ->
                        Toast.makeText(
                                FileManagerActivity.this, "Error to UnZip", Toast.LENGTH_SHORT)
                            .show());
              }
            })
        .start();
  }

  private void setupSearchLayoutInsets() {
    bind.fab.post(
        () -> {
          ViewCompat.setOnApplyWindowInsetsListener(
              bind.ser,
              (view, insets) -> {
                int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                int fabBottomMargin = getFabBottomMargin();
                int targetBottomMargin;
                if (imeHeight > 0) {
                  targetBottomMargin = imeHeight;
                } else {
                  targetBottomMargin =
                      fabBottomMargin + (int) (8 * getResources().getDisplayMetrics().density);
                }
                ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                params.bottomMargin = targetBottomMargin;
                view.setLayoutParams(params);
                return insets;
              });
          ViewCompat.requestApplyInsets(bind.ser);
        });
  }

  private int getFabBottomMargin() {
    int fabBottom = bind.fab.getBottom();
    int screenHeight = bind.fab.getRootView().getHeight();
    return screenHeight - fabBottom;
  }

  public String getCurrentDirectoryPath() {
    return viewModel.getCurrentPath().getValue();
  }

  public void refreshCurrentDirectory() {
    viewModel.loadFiles(viewModel.getCurrentPath().getValue());
  }

  public void setupClick(String path, String name) {
    int lastDot = name.lastIndexOf(".");
    String extension = (lastDot > 0) ? name.substring(lastDot).toLowerCase() : "";
    if (itemname.contains(extension)) {
      Intent intent = new Intent(FileManagerActivity.this, EditorActivity.class);
      intent.putExtra("file_path", path);
      intent.putExtra("file_name", name);
      startActivity(intent);
    } else if (path.endsWith(".gth")) {
      var sheets = new CustomItemSheet(FileManagerActivity.this);
      sheets.add(getString(R.string.theme_edit), R.drawable.ic_edit);
      sheets.add(getString(R.string.theme_apply), R.drawable.outline_color_lens);
      sheets.setOnClickListener(
          (models, pos, view) -> {
            switch (pos) {
              case 0 -> {
                sheets.dismiss();
                Intent i = new Intent(FileManagerActivity.this, ThemeEditorActivity.class);
                i.putExtra(ThemeEditorActivity.EXTRA_THEME_PATH, path);
                startActivity(i);
              }
              case 1 -> {
                appsetting.setAppThemeFile(path);
                sheets.dismiss();
              }
            }
          });
      sheets.show();

    } else if (images.contains(extension)) {
      String currentDir = new File(path).getParent();
      File dir = new File(currentDir);
      File[] allFiles = dir.listFiles();
      ArrayList<String> imagePaths = new ArrayList<>();
      int currentIndex = 0;
      if (allFiles != null) {
        for (int i = 0; i < allFiles.length; i++) {
          File f = allFiles[i];
          if (f.isFile()) {
            String ext = "";
            int dot = f.getName().lastIndexOf(".");
            if (dot > 0) ext = f.getName().substring(dot).toLowerCase();
            if (images.contains(ext)) {
              imagePaths.add(f.getAbsolutePath());
              if (f.getAbsolutePath().equals(path)) currentIndex = imagePaths.size() - 1;
            }
          }
        }
      }
      if (!imagePaths.isEmpty()) {
        Intent setImage = new Intent(FileManagerActivity.this, ImageViewerActivity.class);
        setImage.putStringArrayListExtra(ImageViewerActivity.EXTRA_IMAGE_URIS, imagePaths);
        setImage.putExtra(ImageViewerActivity.EXTRA_CURRENT_INDEX, currentIndex);
        startActivity(setImage);
      } else {
        Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show();
      }
    } else if (audio.contains(extension)) {
      showMusicPreview(path);
    } else if (extension.equals(".apk")) {
      installApk(path);
    } else {
      Toast.makeText(this, getString(R.string.error_file_format_not_supported), Toast.LENGTH_SHORT)
          .show();
    }
  }

  private void setupGitButton() {
    bind.gitActionButton.setOnClickListener(
        v -> {
          String repoPath = findGitRepositoryPath();
          if (repoPath == null) {
            Toast.makeText(this, "Git dir not found", Toast.LENGTH_LONG).show();
            return;
          }
          GitBottomSheetFragment.newInstance(repoPath)
              .show(getSupportFragmentManager(), "git_bottom_sheet");
        });
  }

  private String findGitRepositoryPath() {
    String currentDir = viewModel.getCurrentPath().getValue();
    if (currentDir == null) return null;
    File dir = new File(currentDir);
    while (dir != null) {
      File gitDir = new File(dir, ".git");
      if (gitDir.exists() && gitDir.isDirectory()) return dir.getAbsolutePath();
      dir = dir.getParentFile();
    }
    return null;
  }

  private void observePathForGit() {
    viewModel
        .getCurrentPath()
        .observe(
            this,
            path -> {
              if (path != null && isGitRepository(path)) {
                bind.gitActionButton.setVisibility(View.VISIBLE);
              } else {
                bind.gitActionButton.setVisibility(View.GONE);
              }
            });
  }

  private boolean isGitRepository(String path) {
    File gitDir = new File(path, ".git");
    return gitDir.exists() && gitDir.isDirectory();
  }

  /**
   * Refreshes the git status of the repository that the current directory belongs to and highlights
   * files/folders with uncommitted changes in the file list. Call this whenever the visible
   * directory may have changed on disk (navigation, returning to the activity, after commit/push,
   * etc).
   */
  private void refreshGitStatus() {
    String repoRoot = findGitRepositoryPath();
    if (repoRoot == null) {
      gitStatusPending.set(false);
      if (!gitChangedAbsPaths.isEmpty()) {
        gitChangedAbsPaths = new HashSet<>();
        if (adapter != null && bind != null) {
          Set<String> empty = gitChangedAbsPaths;
          bind.rvfiles.post(() -> adapter.setGitChangedPaths(empty));
        }
      }
      return;
    }
    if (!gitStatusRunning.compareAndSet(false, true)) {
      // A scan is already running; make sure it re-runs once more after it finishes
      // so the latest state on disk is reflected.
      gitStatusPending.set(true);
      return;
    }
    gitStatusExecutor.execute(
        () -> {
          try {
            GitManager manager = new GitManager(repoRoot);
            if (manager.openRepository()) {
              applyChangedFiles(repoRoot, manager.getChangedFiles());
            }
          } finally {
            gitStatusRunning.set(false);
            if (gitStatusPending.compareAndSet(true, false)) {
              refreshGitStatus();
            }
          }
        });
  }

  private void applyChangedFiles(String repoRoot, List<FileChange> changes) {
    Set<String> absPaths = new HashSet<>();
    if (changes != null) {
      for (FileChange change : changes) {
        if (change.getPath() != null) {
          absPaths.add(new File(repoRoot, change.getPath()).getAbsolutePath());
        }
      }
    }
    runOnUiThread(
        () -> {
          gitChangedAbsPaths = absPaths;
          if (adapter != null && bind != null) {
            bind.rvfiles.post(() -> adapter.setGitChangedPaths(gitChangedAbsPaths));
          }
        });
  }

  private void setupSelectionPanel() {
    selectionPanelBinding = bind.selectionPanel;
    selectionPanel = selectionPanelBinding.getRoot();
    selectionCount = selectionPanelBinding.txtSelectedCount;
    btnCopy = selectionPanelBinding.btnCopy;
    btnCut = selectionPanelBinding.btnCut;
    btnDelete = selectionPanelBinding.btnDelete;
    btnPaste = selectionPanelBinding.btnPaste;
    btnClose = selectionPanelBinding.btnClose;
    btnSelectall = selectionPanelBinding.btnSelectall;
    selectionPanelBinding.getRoot().setBackground(ShapeUtil.shapeCustomView(this));
    var selectionMore = selectionPanelBinding.selectionmore;
    btnCopy.setOnClickListener(
        v -> {
          List<FileManagerModel> selected = adapter.getSelectedItems();
          if (!selected.isEmpty()) {
            pendingClipboard = new ArrayList<>(selected);
            isCutOperation = false;
            adapter.clearSelection();
            btnPaste.setColorFilter(0xff00ff00);
            selectionPanel.setVisibility(View.VISIBLE);
            selectionCount.setText("0");
          }
        });

    btnCut.setOnClickListener(
        v -> {
          List<FileManagerModel> selected = adapter.getSelectedItems();
          if (!selected.isEmpty()) {
            pendingClipboard = new ArrayList<>(selected);
            isCutOperation = true;
            adapter.clearSelection();
            btnPaste.setColorFilter(0xff00ff00);
            selectionPanel.setVisibility(View.VISIBLE);
            selectionCount.setText("0");
          }
        });

    btnDelete.setOnClickListener(
        v -> {
          List<FileManagerModel> selected = adapter.getSelectedItems();
          if (!selected.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.removed))
                .setMessage(getString(R.string.removedmassges, selected.size()))
                .setPositiveButton(
                    getString(R.string.ok),
                    (d, w) -> {
                      viewModel.deleteFiles(selected);
                      adapter.clearSelection();
                    })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
          }
        });

    btnPaste.setOnClickListener(
        v -> {
          if (pendingClipboard.isEmpty()) return;
          String currentDir = viewModel.getCurrentPath().getValue();
          if (currentDir != null) {
            copyProgressDialog.setMoveMode(isCutOperation);
            viewModel.pasteFiles(
                pendingClipboard,
                currentDir,
                isCutOperation,
                success -> {
                  pendingClipboard.clear();
                  btnPaste.clearColorFilter();
                  adapter.clearSelection();
                  selectionPanel.setVisibility(View.GONE);
                  adapter.notifyDataSetChanged();
                  if (!success) Toast.makeText(this, "Paste failed", Toast.LENGTH_SHORT).show();
                });
          }
        });

    btnSelectall.setOnClickListener(
        v -> {
          adapter.selectAll();
          selectionCount.setText(
              getString(R.string.selected_items_count, adapter.getSelectedItems().size()));
          if (selectionPanel.getVisibility() != View.VISIBLE) {
            selectionPanel.setVisibility(View.VISIBLE);
          }
        });

    btnClose.setOnClickListener(
        v -> {
          pendingClipboard.clear();
          adapter.clearSelection();
          btnPaste.clearColorFilter();
          selectionPanel.setVisibility(View.GONE);
        });
    selectionMore.setOnClickListener(
        v -> {
          List<FileManagerModel> selected = adapter.getSelectedItems();
          if (selected.isEmpty()) return;

          List<String> items =
              List.of(
                  getString(R.string.zip), getString(R.string.props_title_multi), "Rename Group");
          ObjectUtil.showGlassMenu(
              this,
              v,
              items,
              (index, title) -> {
                if (index == 0) {
                  List<File> filesToZip = new ArrayList<>();
                  for (FileManagerModel model : selected) {
                    filesToZip.add(new File(model.getPath()));
                  }
                  btnClose.performClick();
                  ZipUtil.showZipDialog(FileManagerActivity.this, filesToZip);
                } else if (index == 1) {
                  FilePropertiesSheet.newInstance(selected)
                      .show(getSupportFragmentManager(), FilePropertiesSheet.TAG);
                  btnClose.performClick();
                } else if (index == 2) {
                  BatchRenameSheet sheet = BatchRenameSheet.newInstance(selected);
                  sheet.setOnRenameListener(
                      (items1, pattern, find, replace, useRegex) -> {
                        btnClose.performClick();
                        viewModel.loadFiles(viewModel.getCurrentPath().getValue());
                        refreshFileList();
                      });
                  sheet.show(getSupportFragmentManager(), BatchRenameSheet.TAG);
                }
              });
        });
    selectionPanel.setVisibility(View.GONE);
  }

  private void setupInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(
        bind.coordinator,
        (view, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
          systemBarsBottomInset = systemBars.bottom;
          bind.headtop.setPadding(0, systemBars.top, 0, 0);
          bind.musicPreview.setPadding(
              bind.musicPreview.getPaddingLeft(),
              bind.musicPreview.getPaddingTop(),
              bind.musicPreview.getPaddingRight(),
              systemBars.bottom);
          bind.fab.post(
              () -> {
                int fabSpace = bind.fab.getHeight() + 48;
                int extraBottom = (imeBottom > 0) ? imeBottom : 0;
                bind.rvfiles.setPadding(
                    bind.rvfiles.getPaddingLeft(),
                    bind.rvfiles.getPaddingTop(),
                    bind.rvfiles.getPaddingRight(),
                    systemBars.bottom + fabSpace + extraBottom);
                updateFabBottomMargin();
              });
          return insets;
        });
  }

  private void updateFabBottomMargin() {
    ViewGroup.MarginLayoutParams fabParams =
        (ViewGroup.MarginLayoutParams) bind.fab.getLayoutParams();
    fabParams.bottomMargin =
        systemBarsBottomInset
            + (bind.musicPreview.getVisibility() == View.VISIBLE
                ? bind.musicPreview.getHeight()
                : 0);
    bind.fab.setLayoutParams(fabParams);
  }

  private void showMusicPreview(String path) {
    bind.musicPreview.setMusicPath(path);
    bind.musicPreview.setSongName(new File(path).getName());
    bind.musicPreview.setOnMusicClickListener(() -> showMusicPlayerBottomSheet());
    bind.musicPreview.setVisibility(View.VISIBLE);
    bind.musicPreview.post(this::updateFabBottomMargin);
  }

  private void showMusicPlayerBottomSheet() {
    if (bind.musicPreview.getVisibility() != View.VISIBLE) return;
    String path = bind.musicPreview.getMusicPath();
    if (path == null) return;
    String songName = bind.musicPreview.getSongName();
    String artistName = "";
    try {
      artistName = bind.musicPreview.getArtistName();
    } catch (Exception ignored) {
    }
    musicBottomSheet = MusicPlayerBottomSheetFragment.newInstance(path, songName, artistName);
    musicBottomSheet.setMusicControl(bind.musicPreview);
    musicBottomSheet.setOnDismissListener(() -> musicBottomSheet = null);
    musicBottomSheet.show(getSupportFragmentManager(), "music_player_sheet");
  }

  private void hideMusicPreview() {
    if (musicBottomSheet != null && musicBottomSheet.isAdded()) {
      musicBottomSheet.dismiss();
    }
    if (bind.musicPreview.getVisibility() != View.VISIBLE) return;
    bind.musicPreview.release();
    bind.musicPreview.setVisibility(View.GONE);
    updateFabBottomMargin();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    bind.musicPreview.release();
    bind = null;
    this.unregisterReceiver(networkChangeReceiver);
    gitStatusExecutor.shutdownNow();
    ftpExecutor.shutdownNow();
    if (fileManagerHostRegistration != null) {
      fileManagerHostRegistration.dispose();
      fileManagerHostRegistration = null;
    }
  }

  private void setOnBackPress() {
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (musicBottomSheet != null && musicBottomSheet.isAdded()) {
                  musicBottomSheet.dismiss();
                  return;
                }
                if (bind.musicPreview.getVisibility() == View.VISIBLE) {
                  hideMusicPreview();
                } else if (isZipMode) {
                  if (!zipAdapter.navigateUp()) {
                    exitZipMode();
                  }
                } else {
                  String path = viewModel.getCurrentPath().getValue();
                  if (path != null
                      && !path.equals("/storage/emulated/0")
                      && !StorageUtils.isStorageRoot(FileManagerActivity.this, path)) {
                    viewModel.navigateUp();
                    String currentPath = viewModel.getCurrentPath().getValue();
                    if (currentPath != null) {
                      bind.gitActionButton.setVisibility(
                          isGitRepository(currentPath) ? View.VISIBLE : View.GONE);
                    }
                  } else {
                    new MaterialAlertDialogBuilder(FileManagerActivity.this)
                        .setTitle(getString(R.string.dialog_exit_title))
                        .setMessage(getString(R.string.dialog_exit_message))
                        .setNegativeButton(getString(R.string.ok), (c, f) -> finishAffinity())
                        .setPositiveButton(getString(R.string.cancel), null)
                        .show();
                  }
                }
              }
            });
  }

  void stepMoreAdapter() {
    adapter.setOnMoreClickListener(
        (filemodel, view, pos) -> {
          boolean showRenamePackage = isRenameablePackageDirectory(filemodel);
          boolean showRenameClass = isRenameableClassFile(filemodel);
          List<String> items = new ArrayList<>();
          items.add(getString(R.string.removed));
          items.add(getString(R.string.rename));
          items.add(getString(R.string.props_title_single));
          items.add(getString(R.string.bookmark_add));
          items.add(getString(R.string.shortcut_menu_item));
          items.add(getString(R.string.copyfullpath));
          if (showRenamePackage) {
            items.add(getString(R.string.rename_package_menu_item));
          }
          if (showRenameClass) {
            items.add(getString(R.string.rename_class_menu_item));
          }
          ObjectUtil.showGlassMenu(
              this,
              view,
              items,
              (index, title) -> {
                switch (index) {
                  case 0 -> removedItem(filemodel);
                  case 1 -> renameItem(filemodel);
                  case 2 -> FilePropertiesSheet.newInstance(Collections.singletonList(filemodel))
                      .show(getSupportFragmentManager(), FilePropertiesSheet.TAG);
                  case 3 -> bookmarkViewModel.toggle(
                      filemodel.getPath(),
                      filemodel.getName(),
                      filemodel.isDirectory(),
                      isNowBookmarked ->
                          runOnUiThread(
                              () ->
                                  Toast.makeText(
                                          FileManagerActivity.this,
                                          isNowBookmarked
                                              ? getString(R.string.bookmark_added)
                                              : getString(R.string.bookmark_removed),
                                          Toast.LENGTH_SHORT)
                                      .show()));
                  case 4 -> ShortcutHelper.showShortcutDialog(this, filemodel);
                  case 5 -> {
                    ClipboardUtils.copyText(filemodel.getPath());
                    Toast.makeText(
                            FileManagerActivity.this, filemodel.getPath(), Toast.LENGTH_SHORT)
                        .show();
                  }
                  case 6 -> {
                    if (showRenamePackage) {
                      openRenamePackageSheet(filemodel);
                    } else if (showRenameClass) {
                      openRenameClassSheet(filemodel);
                    }
                  }
                }
              });
        });
  }

  private File findSourceRootForPackageDirectory(File directory) {
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

  private boolean isRenameablePackageDirectory(FileManagerModel model) {
    if (!model.isDirectory()) {
      return false;
    }
    File directory = new File(model.getPath());
    File sourceRoot = findSourceRootForPackageDirectory(directory);
    return sourceRoot != null && !directory.equals(sourceRoot);
  }

  private boolean isRenameableClassFile(FileManagerModel model) {
    if (model.isDirectory()) {
      return false;
    }
    String name = model.getName();
    return name.endsWith(".java") || name.endsWith(".kt");
  }

  private void openRenameClassSheet(FileManagerModel model) {
    File file = new File(model.getPath());
    File sourceRoot = findSourceRootForPackageDirectory(file.getParentFile());
    File moduleRoot =
        sourceRoot != null
                && sourceRoot.getParentFile() != null
                && sourceRoot.getParentFile().getParentFile() != null
            ? sourceRoot.getParentFile().getParentFile().getParentFile()
            : file.getParentFile();
    if (moduleRoot == null) {
      return;
    }
    String name = model.getName();
    int dot = name.lastIndexOf('.');
    String className = dot > 0 ? name.substring(0, dot) : name;
    ir.hanzodev1375.ghostide.refactor.renameclass.ui.RenameClassBottomSheet.newInstance(
            moduleRoot.getAbsolutePath(), file.getAbsolutePath(), className)
        .show(
            getSupportFragmentManager(),
            ir.hanzodev1375.ghostide.refactor.renameclass.ui.RenameClassBottomSheet.TAG);
  }

  private void openRenamePackageSheet(FileManagerModel model) {
    File directory = new File(model.getPath());
    File sourceRoot = findSourceRootForPackageDirectory(directory);
    if (sourceRoot == null) {
      return;
    }
    File moduleRoot =
        sourceRoot.getParentFile() != null && sourceRoot.getParentFile().getParentFile() != null
            ? sourceRoot.getParentFile().getParentFile().getParentFile()
            : null;
    if (moduleRoot == null) {
      return;
    }
    String relativePath = sourceRoot.toURI().relativize(directory.toURI()).getPath();
    if (relativePath.endsWith("/")) {
      relativePath = relativePath.substring(0, relativePath.length() - 1);
    }
    String packageName = relativePath.replace('/', '.');
    RenamePackageBottomSheet.newInstance(moduleRoot.getAbsolutePath(), packageName)
        .show(getSupportFragmentManager(), RenamePackageBottomSheet.TAG);
  }

  void renameItem(FileManagerModel model) {
    RenameDialogFragment dialog =
        RenameDialogFragment.getInstance(
            model.getName(),
            (prefix, extension) -> {
              String displayName =
                  !TextUtils.isEmpty(extension) ? prefix + "." + extension : prefix;
              viewModel.renameFile(model, displayName);
            });
    dialog.show(getSupportFragmentManager(), RenameDialogFragment.TAG);
  }

  void removedItem(FileManagerModel model) {
    new MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.removed))
        .setMessage(getString(R.string.removedmassges, model.getName() + "?"))
        .setPositiveButton(getString(R.string.ok), (d, w) -> viewModel.deleteFile(model))
        .setNegativeButton(getString(R.string.cancel), null)
        .show();
  }

  void creatorFile(FileManagerModel model) {
    TextInputDialogFragment.newInstance(
            getString(R.string.dialog_create_file_title),
            getString(R.string.dialog_create_file_hint),
            null)
        .setCallback(text -> viewModel.createFile(text))
        .show(getSupportFragmentManager(), null);
  }

  void creatorFolder(FileManagerModel model) {
    TextInputDialogFragment.newInstance(
            getString(R.string.dialog_create_folder_title),
            getString(R.string.dialog_create_folder_hint),
            null)
        .setCallback(text -> viewModel.createFolder(text))
        .show(getSupportFragmentManager(), null);
  }

  private void setupHeader() {
    GitHubClient gitHub = new GitHubClient(this);
    if (gitHub.isLoggedIn()) {
      bind.userNameText.setText(gitHub.getName());
      profileview = new ProfileView(this);
      profileview.bindImageView(bind.userAvatar, gitHub.getAvatarUrl(), R.drawable.user);
      Glide.with(this)
          .load(gitHub.getAvatarUrl())
          .circleCrop()
          .placeholder(R.drawable.user)
          .into(bind.userAvatar);
    } else {
      bind.userNameText.setText(getString(R.string.github_account_not_logged_in));
      bind.userAvatar.setImageResource(R.drawable.user);
    }
    bind.userAvatar.setOnClickListener(
        v -> {
          if (gitHub.isLoggedIn()) {
            GitHubProfileSheet.newInstance().show(getSupportFragmentManager(), "github_profile");
          } else {
            new MaterialAlertDialogBuilder(v.getContext())
                .setTitle(getString(R.string.github_tokenerrortitle))
                .setMessage(getString(R.string.github_tokenerrormsg))
                .setPositiveButton(
                    getString(R.string.ok),
                    (c, e) -> {
                      Intent i = new Intent(getApplicationContext(), SettingActivity.class);
                      i.putExtra("open_section", "githublogin");
                      startActivity(i);
                    })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
          }
        });
    bind.btnSettings.setOnClickListener(v -> stepButton(v));
  }

  @Override
  protected void onResume() {
    super.onResume();
    setupHeader();
    if (appsetting.isShowBackground()) {
      themeutil.setFileManagerBack(bind.headline, bind.headtop, bind.backgroundiconfilemanager);
    } else {
      bind.headtop.setBackgroundColor(
          MaterialColors.getColor(bind.headtop, R.attr.colorSurfaceContainer));
      bind.headline.setBackground(ShapeUtil.shape(40f, this));
    }

    boolean currentGrid = appsetting.getGridMod();
    int currentSpan = appsetting.getGridSpanCount();
    boolean spanChanged =
        currentGrid
            && (bind.rvfiles.getLayoutManager() instanceof GridLayoutManager)
            && ((GridLayoutManager) bind.rvfiles.getLayoutManager()).getSpanCount() != currentSpan;

    if (adapter.isGridMode() != currentGrid || spanChanged) {
      adapter = new FileManagerAdapter(this);
      var gr = new GridLayoutManager(this, 2);
      gr.setSpanCount(appsetting.getGridSpanCount());
      bind.rvfiles.setLayoutManager(currentGrid ? gr : new LinearLayoutManager(this));
      bind.rvfiles.setRecycledViewPool(new RecyclerView.RecycledViewPool());
      bind.rvfiles.setAdapter(adapter);
      adapter.setupSelectionTracker(bind.rvfiles);

      adapter.setOnItemClickListener(
          (item, pos) -> {
            historyViewModel.addToHistory(item.getPath(), item.getName(), item.isDirectory());
            if (item.isDirectory()) {
              viewModel.navigateTo(item.getPath());
            } else if (item.getPath().toLowerCase().endsWith(".zip")) {
              enterZipMode(item.getPath());
            } else {
              setupClick(item.getPath(), item.getName());
            }
            String currentPath = viewModel.getCurrentPath().getValue();
            if (currentPath != null) {
              bind.gitActionButton.setVisibility(
                  isGitRepository(currentPath) ? View.VISIBLE : View.GONE);
            }
            if (bind.ser.isShow()) {
              bind.ser.hide();
              bind.fab.setVisibility(View.VISIBLE);
              bind.ser.setQuery("");
            }
          });

      adapter.setSelectionStateListener(
          new FileManagerAdapter.SelectionStateListener() {
            @Override
            public void onSelectionChanged(int count) {
              if (count == 0 && pendingClipboard.isEmpty()) {
                if (selectionPanel != null) selectionPanel.setVisibility(View.GONE);
              } else if (count > 0) {
                selectionPanel.setVisibility(View.VISIBLE);
                selectionCount.setText(getString(R.string.selected_items_count, count));
              } else if (count == 0 && !pendingClipboard.isEmpty()) {
                selectionCount.setText("0");
                selectionPanel.setVisibility(View.VISIBLE);
              }
            }

            @Override
            public void onSelectionModeStarted() {}

            @Override
            public void onSelectionModeEnded() {
              if (pendingClipboard.isEmpty() && selectionPanel != null) {
                selectionPanel.setVisibility(View.GONE);
              }
            }
          });

      stepMoreAdapter();

      String currentPath = viewModel.getCurrentPath().getValue();
      if (currentPath != null) viewModel.loadFiles(currentPath);
    }

    if (!isZipMode) {
      String currentPath = viewModel.getCurrentPath().getValue();
      if (currentPath != null) {
        bind.gitActionButton.setVisibility(isGitRepository(currentPath) ? View.VISIBLE : View.GONE);
      }
      refreshGitStatus();
    }
  }

  void stepButton(View v) {
    List<String> items =
        List.of(
            getString(R.string.settings_title),
            getString(R.string.search_hint),
            getString(R.string.serachdata),
            getString(R.string.openlogcat),
            getString(R.string.history_title),
            getString(R.string.bookmark_title),
            getString(R.string.ftp_connect),
            getString(R.string.sd_card_menu_item),
            getString(R.string.store),
            getString(R.string.aboutapp),
            getString(R.string.translator_title),
            getString(R.string.terminal_title),
            getString(R.string.postman_title),
            getString(R.string.plugin_manager_title));
    ObjectUtil.showGlassMenu(
        this,
        v,
        items,
        (index, title) -> {
          switch (index) {
            case 0 -> startActivity(new Intent(getApplicationContext(), SettingActivity.class));
            case 1 -> {
              if (!bind.ser.isShow()) {
                bind.ser.show();
                bind.fab.setVisibility(View.GONE);
              } else {
                bind.ser.hide();
                bind.fab.setVisibility(View.VISIBLE);
              }
            }
            case 2 -> {
              openSearchSheet();
            }
            case 3 -> {
              var log = new BottomSheetLogView();
              log.show(getSupportFragmentManager(), "log");
            }
            case 4 -> {
              HistoryBottomSheet sheet = HistoryBottomSheet.newInstance();
              sheet.setOnHistoryItemSelectedListener(
                  item -> {
                    if (item.isDirectory) {
                      viewModel.navigateTo(item.path);
                    } else {
                      setupClick(item.path, item.name);
                    }
                  });
              sheet.show(getSupportFragmentManager(), HistoryBottomSheet.TAG);
            }
            case 5 -> {
              BookmarkBottomSheet bsheet = BookmarkBottomSheet.newInstance();
              bsheet.setOnBookmarkSelectedListener(
                  item -> {
                    if (item.isDirectory) {
                      viewModel.navigateTo(item.path);
                    } else {
                      setupClick(item.path, item.name);
                    }
                  });
              bsheet.show(getSupportFragmentManager(), BookmarkBottomSheet.TAG);
            }
            case 6 -> {
              showFtpConnectSheet();
            }
            case 7 -> openSdCard();
            case 8 -> startActivity(new Intent(FileManagerActivity.this, StoreActivity.class));
            case 9 -> startActivity(new Intent(FileManagerActivity.this, AboutActivity.class));
            case 10 -> StringsTranslatorSheet.newInstance(viewModel.getCurrentPath().getValue())
                .show(getSupportFragmentManager(), StringsTranslatorSheet.TAG);
            case 11 -> startActivity(new Intent(FileManagerActivity.this, TerminalActivity.class));
            case 12 -> startActivity(new Intent(FileManagerActivity.this, PostManActivity.class));
            case 13 -> startActivity(
                new Intent(FileManagerActivity.this, PluginManagerActivity.class));
          }
        });
  }

  private void openSearchSheet() {
    String currentPath = viewModel.getCurrentPath().getValue();
    if (currentPath == null) return;
    SearchBottomSheet sheet = SearchBottomSheet.newInstance(currentPath);
    sheet.setOnLineClickListener(
        new OnLineClickListener() {
          @Override
          public void onLineClick(String filePath, int lineNumber) {
            setupClick(filePath, new File(filePath).getName());
          }

          @Override
          public void onFileClick(FileSearchResult result) {
            if (new File(result.getFilePath()).isDirectory()) {
              viewModel.navigateTo(result.getFilePath());
            } else {
              setupClick(result.getFilePath(), result.getFileName());
            }
          }
        });
    sheet.show(getSupportFragmentManager(), SearchBottomSheet.TAG);
  }

  void stepSearch() {
    bind.ser.setOnTextChangedListener(
        qer -> {
          if (qer.length() > 0) adapter.search(qer);
          else adapter.search("");
        });
    bind.ser.setIconClose(R.drawable.ic_close);
    bind.ser.setIconSearch(R.drawable.outline_search);
  }

  private void showGoToDirMenu(View anchor) {
    List<String> items =
        List.of(
            getString(R.string.goto_dir),
            getString(R.string.internal_storage_label),
            getString(R.string.sd_card_menu_item),
            getString(R.string.goto_root));
    ObjectUtil.showGlassMenu(
        this,
        anchor,
        items,
        (index, title) -> {
          switch (index) {
            case 0 -> bind.navmodel.showGoToDirDialog();
            case 1 -> navigateToPath(Environment.getExternalStorageDirectory().getAbsolutePath());
            case 2 -> {
              StorageUtils.StorageEntry sd = StorageUtils.getSdCardVolume(this);
              if (sd != null) {
                navigateToPath(sd.path);
              } else {
                Toast.makeText(this, R.string.sd_card_not_found, Toast.LENGTH_SHORT).show();
              }
            }
            case 3 -> navigateToPath(getCacheDir().getAbsolutePath());
          }
        });
  }

  private void showPluginPopup(View anchor) {
    var installedFiles = GplInstalledPlugins.listInstalled(this);

    if (installedFiles.isEmpty()) {
      Toast.makeText(this, R.string.no_plugins, Toast.LENGTH_SHORT).show();
      return;
    }

    var loader = GplPluginLoader.getInstance(this);
    for (var f : installedFiles) {
      try {
        var manifest = GplManifestReader.read(f);
        if (manifest == null) continue;
        if (!loader.isLoaded(manifest.id())) {
          loader.load(f);
        }
      } catch (Exception e) {
        Log.e("FileManagerActivity", "showPluginPopup: failed to load " + f.getName(), e);
      }
    }

    var registeredScreens =
        GlobalRegistry.extensions()
            .extensions(PluginUiExtensionPoints.PLUGIN_SCREEN);

    var pluginItems =
        installedFiles.stream()
            .map(
                f -> {
                  try {
                    GplManifest manifest = GplManifestReader.read(f);
                    if (manifest == null) {
                      return Optional.<PluginPopupAdapter.PluginItem>empty();
                    }

                    var matchingScreen =
                        registeredScreens.stream()
                            .filter(s -> manifest.id().equals(s.getId()))
                            .findFirst();

                    if (matchingScreen.isPresent()) {
                      return Optional.of(
                          new PluginPopupAdapter.PluginItem(
                              matchingScreen.get().getId(),
                              matchingScreen.get().getTitle(),
                              f,
                              manifest));
                    } else {
                      return Optional.of(
                          new PluginPopupAdapter.PluginItem(
                              manifest.id(),
                              manifest.name(),
                              f,
                              manifest));
                    }
                  } catch (Exception e) {
                    Log.e("FileManagerActivity", "showPluginPopup: error reading: " + f.getName(), e);
                    return Optional.<PluginPopupAdapter.PluginItem>empty();
                  }
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

    if (pluginItems.isEmpty()) {
      Toast.makeText(this, R.string.no_plugins, Toast.LENGTH_SHORT).show();
      return;
    }

    var rv = new RecyclerView(this);
    rv.setLayoutManager(new LinearLayoutManager(this));
    var popupRef = new PopupWindow[1];
    rv.setAdapter(
        new PluginPopupAdapter(
            (view, item, pos) -> {
              if (popupRef[0] != null) popupRef[0].dismiss();
              var allScreens =
                  GlobalRegistry.extensions()
                      .extensions(PluginUiExtensionPoints.PLUGIN_SCREEN);

              var matchingScreen =
                  allScreens.stream()
                      .filter(
                          s ->
                              item.id().equals(s.getId())
                                  || (item.manifest() != null
                                      && item.manifest().id().equals(s.getId())))
                      .findFirst();
              if (matchingScreen.isPresent()) {
                startActivity(
                    PluginScreenActivity.createIntent(
                        this, matchingScreen.get().getId()));
                return;
              }

              Toast.makeText(
                      this,
                      getString(R.string.plugin_manager_installed_toast, item.name()),
                      Toast.LENGTH_SHORT)
                  .show();
            }));

    ((PluginPopupAdapter) rv.getAdapter()).submit(pluginItems);

    popupRef[0] = ObjectUtil.showGlassPopup(this, anchor, rv);
  }

  private void navigateToPath(String path) {
    if (path == null || path.isEmpty()) return;
    viewModel.navigateTo(path);
    bind.gitActionButton.setVisibility(isGitRepository(path) ? View.VISIBLE : View.GONE);
  }

  @Override
  public void ConnectionNOT() {}

  @Override
  public void ConnectionIS() {
    app.init();
  }

  public void refreshFileList() {
    runOnUiThread(
        () -> {
          String currentPath = viewModel.getCurrentPath().getValue();
          if (currentPath != null) {
            viewModel.loadFiles(currentPath);
          }
          if (isZipMode && zipAdapter != null && currentZipFilePath != null) {
            zipAdapter.loadZip(currentZipFilePath, zipAdapter.getCurrentInternalPath());
          }
        });
  }

  private String formatSize(long bytes) {
    if (bytes >= 1024 * 1024)
      return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
    else if (bytes >= 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
    else return bytes + " B";
  }

  private void showFtpConnectSheet() {
    FtpConnectSheet sheet = FtpConnectSheet.newInstance();
    sheet.setOnConnectedListener(this::openFtpBrowser);
    sheet.show(getSupportFragmentManager(), FtpConnectSheet.TAG);
  }

  private void openSdCard() {
    StorageUtils.StorageEntry sdCard = StorageUtils.getSdCardVolume(this);
    if (sdCard != null) {
      viewModel.navigateTo(sdCard.path);
      bind.gitActionButton.setVisibility(isGitRepository(sdCard.path) ? View.VISIBLE : View.GONE);
      Toast.makeText(
              this,
              getString(
                  R.string.sd_card_space_info,
                  sdCard.getFreeFormatted(),
                  sdCard.getTotalFormatted()),
              Toast.LENGTH_SHORT)
          .show();
    } else {
      Toast.makeText(this, R.string.sd_card_not_found, Toast.LENGTH_SHORT).show();
    }
  }

  private void openFtpBrowser(RemoteClient client, String host, boolean isSftp) {
    FtpBrowserSheet sheet = FtpBrowserSheet.newInstance(client, host, isSftp);
    sheet.setOnDownloadListener(
        new FtpBrowserSheet.OnDownloadListener() {
          @Override
          public void onDownload(String remotePath, String fileName) {
            currentDir = viewModel.getCurrentPath().getValue();
            if (currentDir == null || !new File(currentDir).exists()) {
              currentDir =
                  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                      .getAbsolutePath();
            }
            File localFile = new File(currentDir, fileName);
            ftpExecutor.execute(
                () -> {
                  try {
                    client.download(remotePath, localFile.getAbsolutePath());
                    runOnUiThread(
                        () -> {
                          Toast.makeText(
                                  FileManagerActivity.this,
                                  R.string.ftp_download_success,
                                  Toast.LENGTH_LONG)
                              .show();
                          viewModel.loadFiles(currentDir);
                        });
                  } catch (Exception e) {
                    runOnUiThread(
                        () -> {
                          Toast.makeText(
                                  FileManagerActivity.this,
                                  R.string.ftp_download_error + ": " + e.getMessage(),
                                  Toast.LENGTH_LONG)
                              .show();
                        });
                  }
                });
          }
        });
    sheet.show(getSupportFragmentManager(), FtpBrowserSheet.TAG);
  }

  private void installApk(String path) {
    if (ShizukuManager.isAvailable() && ShizukuManager.hasPermission()) {
      ShizukuManager.exec(
          new String[] {"pm", "install", "-r", "-i", getPackageName(), path},
          new ShizukuManager.ExecCallback() {
            @Override
            public void onResult(String output) {
              boolean ok = output.toLowerCase().contains("success");
              Toast.makeText(
                      FileManagerActivity.this, ok ? "installsuccess" : output, Toast.LENGTH_LONG)
                  .show();
            }

            @Override
            public void onUnavailable() {
              installApkNormal(path);
            }
          });
    } else if (ShizukuManager.isAvailable()) {
      ShizukuManager.requestPermission();
      Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
    } else {
      installApkNormal(path);
    }
  }

  private void installApkNormal(String path) {
    var apkFile = new File(path);
    var uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(uri, "application/vnd.android.package-archive");
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(intent);
  }
}
