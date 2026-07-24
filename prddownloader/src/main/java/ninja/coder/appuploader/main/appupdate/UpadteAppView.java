package ninja.coder.appuploader.main.appupdate;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import com.blankj.utilcode.util.AppUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import ninja.coder.appuploader.main.ViewDownloder;
import ninja.coder.appuploader.main.appupdate.model.AppUpdateModel;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UpadteAppView {
  private Context context;
  private String TAG = getClass().getName();
  private ViewDownloder downloder;
  private String version;

  private AppUpdateCallBack call;
  private final String constLinkGithub =
      "https://raw.githubusercontent.com/HanzoDev1375/GhostIdes/main/update.json";
  private AppUpdateModel model;

  private final OkHttpClient okHttpClient =
      new OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(15, TimeUnit.SECONDS)
          .build();

  public UpadteAppView(
      Context context,
      ViewDownloder downloder,
      AppUpdateCallBack call) {
    this.context = context;
    this.downloder = downloder;
    this.call = call;
  }

  public void init() {
    Request request = new Request.Builder().url(constLinkGithub).get().build();

    okHttpClient
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage());

                runOnUiThread(
                    () -> {
                      if (context != null) {
                        showMessage(context, "خطا در بررسی آپدیت: " + e.getMessage());
                      }
                    });
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                  runOnUiThread(
                      () -> {
                        if (context != null) {
                          showMessage(context, "خطا در بررسی آپدیت: کد " + response.code());
                        }
                      });
                  return;
                }

                String jsonData = response.body() != null ? response.body().string() : "";
                try {
                  model = new Gson().fromJson(jsonData, AppUpdateModel.class);
                } catch (Exception e) {
                  Log.e(TAG, "JSON parsing error: " + e.getMessage());
                  runOnUiThread(() -> showMessage(context, "خطا در تحلیل پاسخ سرور"));
                  return;
                }
                if (model != null && !model.getVersion().equals(AppUtils.getAppVersionName())) {
                  runOnUiThread(() -> showUpdateDialog());
                } else {

                  runOnUiThread(
                      () -> {
                        if (call != null) call.cancel();
                      });
                }
              }
            });
  }

  private void showUpdateDialog() {
    if (context == null) return;

    MaterialAlertDialogBuilder di = new MaterialAlertDialogBuilder(context);
    di.setTitle(model.getTitle());
    di.setMessage(model.getMassges());
    di.setCancelable(false);

    di.setNeutralButton(
        "Update",
        (p, d) -> {
          downloder.setTitle(model.getTitle());
          downloder.setVisibility(View.VISIBLE);
          String link = model.getLink();
          String fileName = link.substring(link.lastIndexOf('/') + 1);
          downloder.setDownload(link, fileName);
          if (call != null) {
            call.call();
          }
        });
    di.setPositiveButton("Ask Later", null);
    di.show();
  }

  void showMessage(Context c, String d) {
    Snackbar.make(((Activity) c).findViewById(android.R.id.content), d, Snackbar.LENGTH_LONG)
        .show();
  }

  private void runOnUiThread(Runnable action) {
    if (context instanceof Activity) {
      ((Activity) context).runOnUiThread(action);
    } else {
      new Handler(Looper.getMainLooper()).post(action);
    }
  }
}
