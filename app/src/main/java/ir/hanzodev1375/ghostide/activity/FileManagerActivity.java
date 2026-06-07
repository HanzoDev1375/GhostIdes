package ir.hanzodev1375.ghostide.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ir.hanzodev1375.components.RenameDialogFragment;
import ir.hanzodev1375.components.TextInputDialogFragment;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.adapters.FileManagerAdapter;
import ir.hanzodev1375.ghostide.databinding.ActivityFilemanagerBinding;
import ir.hanzodev1375.ghostide.databinding.SelectionPanelBinding;
import ir.hanzodev1375.ghostide.fragments.MusicPlayerBottomSheetFragment;
import ir.hanzodev1375.ghostide.models.FileManagerModel;
import ir.hanzodev1375.ghostide.mvvm.viewmodel.FileViewModel;
import ir.hanzodev1375.ghostide.plugin.PluginManager;
import ir.hanzodev1375.ghostide.utils.MarginItemDecoration;
import ir.hanzodev1375.ghostide.utils.MusicPlayerManager;
import ir.hanzodev1375.ghostide.utils.PermissionUtils;
import ir.hanzodev1375.ghostide.utils.ShapeUtil;
import ir.theme.themeeditor.ThemeEditorActivity;

public class FileManagerActivity extends BaseCompat {

    private ActivityFilemanagerBinding bind;
    private FileViewModel viewModel;
    private FileManagerAdapter adapter;
    private View selectionPanel;
    private TextView selectionCount;
    private ImageView btnCopy, btnCut, btnDelete, btnPaste, btnClose, btnSelectall;
    private boolean isCutOperation = false;
    private List<FileManagerModel> pendingClipboard = new ArrayList<>();
    private SelectionPanelBinding selectionPanelBinding;
    private Set<String> itemname = new HashSet<>(Arrays.asList(".html", ".java", ".cpp", ".css", ".js"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bind = ActivityFilemanagerBinding.inflate(getLayoutInflater());
        setContentView(bind.getRoot());
        setupInsets();

        Toolbar toolbar = bind.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("مدیریت فایل");
        }

        new Handler(Looper.getMainLooper())
                .postDelayed(() -> {
                    try {
                        PluginManager.getInstance().setCurrentFileManagerActivity(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 100);
        bind.headline.setBackground(ShapeUtil.shape(40f, this));
        viewModel = new ViewModelProvider(this).get(FileViewModel.class);
        adapter = new FileManagerAdapter(this);
        bind.rvfiles.setLayoutManager(new LinearLayoutManager(this));
        bind.rvfiles.setAdapter(adapter);
        bind.rvfiles.addItemDecoration(new MarginItemDecoration(this));

        adapter.setupSelectionTracker(bind.rvfiles);

        viewModel.getFiles().observe(this, files -> {
            adapter.submitList(new ArrayList<>(files));
            bind.rvfiles.post(() -> adapter.notifyDataSetChanged());
            if (files == null || files.isEmpty()) {
                bind.emptystates.setVisibility(View.VISIBLE);
                bind.rvfiles.setVisibility(View.GONE);
            } else {
                bind.emptystates.setVisibility(View.GONE);
                bind.rvfiles.setVisibility(View.VISIBLE);
            }
        });
        viewModel.getIsLoading().observe(this, loading -> bind.loadingprogass.setVisibility(loading ? View.VISIBLE : View.GONE));
        viewModel.savePath(true);
        viewModel.getCurrentPath().observe(this, path -> {
            if (path != null) {
                bind.navmodel.setFile(new File(path));
            }
        });

        adapter.setOnItemClickListener((item, pos) -> {
            if (item.isDirectory()) {
                viewModel.navigateTo(item.getPath());
            } else {
                setupClick(item.getPath(), item.getName());
            }
        });

        bind.fab.setOnClickListener(v -> startActivity(new Intent(FileManagerActivity.this, SettingActivity.class)));
        bind.navmodel.getAdapter().setOnItemClickListener((view, nav, pos) -> viewModel.navigateTo(nav.getFilePath()));
        stepMoreAdapter();
        setupSelectionPanel();
        adapter.setSelectionStateListener(new FileManagerAdapter.SelectionStateListener() {
            @Override
            public void onSelectionChanged(int count) {
                if (count == 0 && pendingClipboard.isEmpty()) {
                    if (selectionPanel != null) selectionPanel.setVisibility(View.GONE);
                } else if (count > 0) {
                    selectionPanel.setVisibility(View.VISIBLE);
                    selectionCount.setText("Item select " + count);
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

        setOnBackPress();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_filemanager, menu);
        new Handler(Looper.getMainLooper()).post(() -> {
            Toolbar toolbar = bind.toolbar;
            if (toolbar != null) {
                View musicItemView = toolbar.findViewById(R.id.action_music);
                if (musicItemView != null) {
                    musicItemView.setOnLongClickListener(v -> {
                        stopMusic();
                        return true;
                    });
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_music) {
            showMusicChooserDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMusicChooserDialog() {
        if (!PermissionUtils.hasPermissions(this)) {
            PermissionUtils.requestPermissions(this);
            Toast.makeText(this, "لطفاً مجوزهای لازم را بدهید", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> musicPaths = getMusicFilesList();
        if (musicPaths.isEmpty()) {
            Toast.makeText(this, "هیچ فایل موسیقی یافت نشد", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> musicNames = new ArrayList<>();
        for (String path : musicPaths) {
            musicNames.add(new File(path).getName());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("انتخاب آهنگ")
                .setItems(musicNames.toArray(new String[0]), (dialog, which) -> {
                    String selectedPath = musicPaths.get(which);
                    MusicPlayerManager.getInstance(this).play(selectedPath);
                    MusicPlayerBottomSheetFragment bottomSheet = new MusicPlayerBottomSheetFragment();
                    bottomSheet.show(getSupportFragmentManager(), "MusicPlayer");
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private List<String> getMusicFilesList() {
        List<String> songs = new ArrayList<>();
        android.database.Cursor cursor = null;
        try {
            android.net.Uri collection = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {android.provider.MediaStore.Audio.Media.DATA};
            cursor = getContentResolver().query(collection, projection, null, null, null);
            if (cursor != null) {
                int dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA);
                while (cursor.moveToNext()) {
                    String path = cursor.getString(dataCol);
                    if (path != null && new File(path).exists()) {
                        songs.add(path);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return songs;
    }

    private void stopMusic() {
        MusicPlayerManager player = MusicPlayerManager.getInstance(this);
        if (player.isPlaying()) {
            player.stop();
            Toast.makeText(this, "پخش موسیقی متوقف شد", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "هیچ موسیقی در حال پخش نیست", Toast.LENGTH_SHORT).show();
        }
    }

    void setupClick(String path, String name) {
        int lastDot = name.lastIndexOf(".");
        String extension = (lastDot > 0) ? name.substring(lastDot).toLowerCase() : "";
        if (itemname.contains(extension)) {
            Intent intent = new Intent(FileManagerActivity.this, EditorActivity.class);
            intent.putExtra("file_path", path);
            intent.putExtra("file_name", name);
            startActivity(intent);
        } else if (path.endsWith(".gth")) {
            Intent i = new Intent(FileManagerActivity.this, ThemeEditorActivity.class);
            i.putExtra(ThemeEditorActivity.EXTRA_THEME_PATH, path);
            startActivity(i);
        } else {
            Toast.makeText(this, "فرمت فایل پشتیبانی نمی‌شود", Toast.LENGTH_SHORT).show();
        }
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
        btnCopy.setOnClickListener(v -> {
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

        btnCut.setOnClickListener(v -> {
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
        btnDelete.setOnClickListener(v -> {
            List<FileManagerModel> selected = adapter.getSelectedItems();
            if (!selected.isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Delete")
                        .setMessage("Delete " + selected.size() + " items?")
                        .setPositiveButton("Delete", (d, w) -> {
                            viewModel.deleteFiles(selected);
                            adapter.clearSelection();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        btnPaste.setOnClickListener(v -> {
            if (pendingClipboard.isEmpty()) return;
            String currentDir = viewModel.getCurrentPath().getValue();
            if (currentDir != null) {
                viewModel.pasteFiles(pendingClipboard, currentDir, isCutOperation, success -> {
                    if (success) {
                        pendingClipboard.clear();
                        btnPaste.clearColorFilter();
                        adapter.clearSelection();
                        selectionPanel.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Paste failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnSelectall.setOnClickListener(v -> {
            adapter.selectAll();
            selectionCount.setText("Item select " + adapter.getSelectedItems().size());
            if (selectionPanel.getVisibility() != View.VISIBLE) {
                selectionPanel.setVisibility(View.VISIBLE);
            }
        });
        btnClose.setOnClickListener(v -> {
            pendingClipboard.clear();
            adapter.clearSelection();
            btnPaste.clearColorFilter();
            selectionPanel.setVisibility(View.GONE);
        });

        selectionPanel.setVisibility(View.GONE);
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(bind.coordinator, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            bind.toolbar.setPadding(0, systemBars.top, 0, 0);
            bind.fab.post(() -> {
                int fabSpace = bind.fab.getHeight() + 48;
                bind.rvfiles.setPadding(
                        bind.rvfiles.getPaddingLeft(),
                        bind.rvfiles.getPaddingTop(),
                        bind.rvfiles.getPaddingRight(),
                        systemBars.bottom + fabSpace);
            });
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bind = null;
    }

    private void setOnBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewModel.getCurrentPath().getValue() != null
                        && !viewModel.getCurrentPath().getValue().equals("/storage/emulated/0")) {
                    viewModel.navigateUp();
                } else {
                    new MaterialAlertDialogBuilder(FileManagerActivity.this)
                            .setTitle("Exit")
                            .setMessage("Exit Ghost IDE?")
                            .setNegativeButton("Yes", (c, f) -> finishAffinity())
                            .setPositiveButton("No", null)
                            .show();
                }
            }
        });
    }

    void stepMoreAdapter() {
        adapter.setOnMoreClickListener((filemodel, view, pos) -> {
            PowerMenu menu = new PowerMenu.Builder(view.getContext()).setIsMaterial(true).build();
            menu.addItem(new PowerMenuItem(getString(R.string.removed)));
            menu.addItem(new PowerMenuItem(getString(R.string.rename)));
            menu.addItem(new PowerMenuItem(getString(R.string.removed)));
            menu.addItem(new PowerMenuItem(getString(R.string.rename)));
            menu.setMenuColor(MaterialColors.getColor(view.getContext(), com.google.android.material.R.attr.colorSurface, 0));
            menu.setTextColor(MaterialColors.getColor(view.getContext(), com.google.android.material.R.attr.colorOnSurface, 0));
            menu.setShowBackground(false);
            menu.setAutoDismiss(true);
            menu.setMenuRadius(30f);
            menu.setAnimation(MenuAnimation.FADE);
            menu.setOnMenuItemClickListener((index, item) -> {
                switch (index) {
                    case 0 -> removedItem(filemodel);
                    case 1 -> renameItem(filemodel);
                    case 2 -> creatorFile(filemodel);
                    case 3 -> creatorFolder(filemodel);
                }
            });
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];
            var dm = view.getResources().getDisplayMetrics();
            int screenHeight = dm.heightPixels;
            int menuHeight = menu.getContentViewHeight();
            if (menuHeight <= 0) menuHeight = 200;
            int spaceAbove = y;
            int spaceBelow = screenHeight - (y + view.getHeight());
            if (spaceBelow < menuHeight && spaceAbove > spaceBelow) {
                y -= menuHeight;
            } else {
                y += view.getHeight();
            }
            menu.showAtLocation(view, Gravity.TOP | Gravity.START, x, y);
        });
    }

    void renameItem(FileManagerModel model) {
        RenameDialogFragment dialog = RenameDialogFragment.getInstance(model.getName(),
                (prefix, extension) -> {
                    String displayName = TextUtils.isEmpty(extension) ? prefix : prefix + "." + extension;
                    viewModel.renameFile(model, displayName);
                });
        dialog.show(getSupportFragmentManager(), RenameDialogFragment.TAG);
    }

    void removedItem(FileManagerModel model) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.removed))
                .setMessage(getString(R.string.removedmassges, model.getName() + "?"))
                .setPositiveButton("OK", (d, w) -> viewModel.deleteFile(model))
                .setNegativeButton("Cancel", null)
                .show();
    }

    void creatorFile(FileManagerModel model) {
        TextInputDialogFragment.newInstance("ساخت فایل", "نام فایل", null)
                .setCallback(text -> viewModel.createFile(text))
                .show(getSupportFragmentManager(), null);
    }

    void creatorFolder(FileManagerModel model) {
        TextInputDialogFragment.newInstance("ساخت پوشه", "پوشه", null)
                .setCallback(text -> viewModel.createFolder(text))
                .show(getSupportFragmentManager(), null);
    }
}
