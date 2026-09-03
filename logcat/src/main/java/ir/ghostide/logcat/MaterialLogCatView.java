package ir.ghostide.logcat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import android.os.Build;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import ir.hanzodev1375.components.views.GhostToast;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MaterialLogCatView extends LinearLayout {

  private RecyclerView recyclerView;
  private EditText searchBox;
  private FloatingActionButton btnSave;
  private LogAdapter adapter;
  private static final int PERMISSION_REQUEST_CODE = 100;

  public MaterialLogCatView(Context context) {
    super(context);
    init(context);
  }

  public MaterialLogCatView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    inflate(context, R.layout.view_material_logcat, this);
    recyclerView = findViewById(R.id.recycler_logs);
    searchBox = findViewById(R.id.search_logs);
    btnSave = findViewById(R.id.fab_save_logs);

    recyclerView.setLayoutManager(new LinearLayoutManager(context));

    List<LogEntry> logs = LogcatReader.getCurrentAppLogs();
    adapter = new LogAdapter(logs);
    recyclerView.setAdapter(adapter);
    recyclerView.addItemDecoration(new MarginItemDecoration());

    searchBox.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            adapter.getFilter().filter(s);
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });

    btnSave.setOnClickListener(v -> saveLogsToFile());
  }

  private void saveLogsToFile() {
    String allLogs = adapter.getAllFilteredMessages();
    if (allLogs.isEmpty()) {
      GhostToast.makeText(getContext(), "No logs to save", GhostToast.LENGTH_SHORT).show();
      return;
    }
    try {

      File downloadDir = new File("/storage/emulated/0/ghostide/");
      File logDir = new File(downloadDir, "applog");
      if (!logDir.exists()) {
        logDir.mkdirs();
      }

      String fileName = "logs_" + System.currentTimeMillis() + ".log";
      File logFile = new File(logDir, fileName);

      FileOutputStream fos = new FileOutputStream(logFile);
      OutputStreamWriter writer = new OutputStreamWriter(fos);
      writer.write(allLogs);
      writer.close();
      fos.close();

      GhostToast.makeText(
              getContext(),
              "Logs saved to " + logFile.getAbsolutePath(),
              GhostToast.LENGTH_LONG)
          .show();
    } catch (Exception e) {
      GhostToast.makeText(getContext(), "Save error: " + e.getMessage(), GhostToast.LENGTH_SHORT).show();
      e.printStackTrace();
    }
  }

  public void refreshLogs() {
    List<LogEntry> newLogs = LogcatReader.getCurrentAppLogs();
    adapter.updateData(newLogs);
  }

  public class MarginItemDecoration extends RecyclerView.ItemDecoration {
    private final int itemMargin;

    public MarginItemDecoration() {
      itemMargin = 2;
    }

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