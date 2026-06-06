package ir.hanzodev1375.ghostide.activity;

import android.content.Intent;
import android.os.Bundle;
import ir.hanzodev1375.ghostide.databinding.ErrormanagerBinding;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.FileIOUtils;

public class ErrorManagerActivity extends BaseCompat {
  private ErrormanagerBinding bind;
  private Intent i = new Intent();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    bind = ErrormanagerBinding.inflate(getLayoutInflater());
    setContentView(bind.getRoot());
    bindid();
    runApp();
  }

  private void bindid() {

    setSupportActionBar(bind.toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);
    bind.toolbar.setNavigationOnClickListener(
        (__) -> {
          finishAffinity();
        });
    bind.fab.setOnClickListener(
        (v) -> {
          ClipboardUtils.copyText(bind.result.getText().toString());
        });
    bind.result.setSelected(true);
  }

  private void runApp() {
    var error = new StringBuilder();
    error.append(getIntent().getStringExtra("Software"));
    error.append("App version: " + AppUtils.getAppVersionCode());
    error.append("\n\n");
    error.append(getIntent().getStringExtra("Error"));
    error.append("\n\n");
    error.append("minsdk ").append(AppUtils.getAppTargetSdkVersion()).append('\n');
    error.append(getIntent().getStringExtra("Date"));

    bind.result.setText(error.toString());
    setTitle("app Crash");
   // FileIOUtils.writeFileFromString("/sdcard//GhostWebIDE/Error.log",error.toString());
    
  }
}
