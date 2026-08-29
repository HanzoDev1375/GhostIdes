package ir.hanzodev1375.ghostide.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.graphics.Color;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.FileIOUtils;
import ir.hanzodev1375.ghostide.databinding.ErrormanagerBinding;
import ir.hanzodev1375.ghostide.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class ErrorManagerActivity extends BaseCompat {
  private ErrormanagerBinding bind;

  private static final Map<String, String> exceptionMap =
      new HashMap<String, String>() {
        {
          put("StringIndexOutOfBoundsException", "Invalid string operation");
          put("IndexOutOfBoundsException", "Invalid list operation");
          put("ArithmeticException", "Invalid arithmetical operation");
          put("NumberFormatException", "Invalid toNumber block operation");
          put("ActivityNotFoundException", "Invalid intent operation");
          put("NullPointerException", "Null reference accessed");
          put("ClassCastException", "Invalid type cast");
          put("OutOfMemoryError", "Application ran out of memory");
          put("StackOverflowError", "Infinite loop or deep recursion detected");
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    bind = ErrormanagerBinding.inflate(getLayoutInflater());
    setContentView(bind.getRoot());
    setupBackgroundBlur();
    setSupportActionBar(bind.toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle("💀 App Crashed");
    }

    Intent intent = getIntent();
    String errorMessage =
        (intent != null && intent.hasExtra("error")) ? intent.getStringExtra("error") : "";

    SpannableStringBuilder full = new SpannableStringBuilder();

    // ── Header ──────────────────────────────────────────────
    appendSection(
        full,
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
            + "  GHOST IDE — CRASH REPORT\n"
            + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n",
        Color.parseColor("#FF4444"),
        true);

    // ── App Info ────────────────────────────────────────────
    appendSection(full, "\n📦 APP INFO\n", Color.parseColor("#4FC3F7"), true);
    appendLine(full, "App Name", AppUtils.getAppName());
    appendLine(
        full, "Version", AppUtils.getAppVersionName() + " (" + AppUtils.getAppVersionCode() + ")");
    appendLine(full, "Package", getPackageName());
    appendLine(full, "Debug Build", AppUtils.isAppDebug() ? "Yes" : "No");

    // ── Device Info ─────────────────────────────────────────
    appendSection(full, "\n📱 DEVICE INFO\n", Color.parseColor("#4FC3F7"), true);
    appendLine(full, "Manufacturer", Build.MANUFACTURER);
    appendLine(full, "Model", Build.MODEL);
    appendLine(full, "Android", Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
    appendLine(full, "ABI", Build.SUPPORTED_ABIS[0]);
    appendLine(full, "Fingerprint", Build.FINGERPRINT);

    // ── Runtime Info ────────────────────────────────────────
    Runtime rt = Runtime.getRuntime();
    long usedMem = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    long totalMem = rt.totalMemory() / (1024 * 1024);
    long maxMem = rt.maxMemory() / (1024 * 1024);

    appendSection(full, "\n⚙️ RUNTIME\n", Color.parseColor("#4FC3F7"), true);
    appendLine(full, "Memory Used", usedMem + " MB");
    appendLine(full, "Memory Total", totalMem + " MB");
    appendLine(full, "Memory Max", maxMem + " MB");
    appendLine(full, "CPU Cores", String.valueOf(rt.availableProcessors()));

    // ── Time ────────────────────────────────────────────────
    String time =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    appendSection(full, "\n🕒 TIMESTAMP\n", Color.parseColor("#4FC3F7"), true);
    appendLine(full, "Crashed At", time);

    // ── Error ────────────────────────────────────────────────
    appendSection(full, "\n🔴 ERROR DETAILS\n", Color.parseColor("#FF4444"), true);

    if (errorMessage != null && !errorMessage.isEmpty()) {
      String[] split = errorMessage.split("\n");
      String exceptionType = split[0].trim();
      String friendlyMsg =
          exceptionMap.containsKey(exceptionType)
              ? exceptionMap.get(exceptionType)
              : "Unknown error";

      appendLine(full, "Exception", exceptionType);
      appendLine(full, "Meaning", friendlyMsg);
      full.append("\n📋 Stack Trace:\n");

      for (int i = 1; i < split.length; i++) {
        full.append("  ").append(split[i]).append("\n");
      }
    } else {
      full.append("No error message available.\n");
    }

    appendSection(full, "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n", Color.parseColor("#FF4444"), false);

    // ── Display ──────────────────────────────────────────────
    bind.result.setText(full);

    // ── FAB: Copy + Save ─────────────────────────────────────
    bind.fab.setImageResource(R.drawable.ic_copy);
    final String plainText = full.toString();
    bind.fab.setOnClickListener(
        v -> {
          ClipboardUtils.copyText(plainText);
          FileIOUtils.writeFileFromString("/sdcard/Ghostide/error.log", plainText);
        });
  }

  private void setupBackgroundBlur() {
    setupBackgroundBlur(bind.backgroundIconError, bind.appbar, bind.toolbar, bind.errorContent);
  }

  private void appendSection(SpannableStringBuilder sb, String text, int color, boolean bold) {
    SpannableString ss = new SpannableString(text);
    ss.setSpan(new ForegroundColorSpan(color), 0, text.length(), 0);
    if (bold) ss.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
    sb.append(ss);
  }

  private void appendLine(SpannableStringBuilder sb, String key, String value) {
    SpannableString label = new SpannableString(key + ": ");
    label.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), 0);
    label.setSpan(new ForegroundColorSpan(Color.parseColor("#B0BEC5")), 0, label.length(), 0);
    sb.append(label).append(value).append("\n");
  }
}
