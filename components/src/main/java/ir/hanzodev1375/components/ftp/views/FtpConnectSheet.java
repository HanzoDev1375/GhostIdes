package ir.hanzodev1375.components.ftp.views;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ir.hanzodev1375.components.views.GhostToast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import ir.hanzodev1375.components.ftp.impl.FtpClientImpl;
import ir.hanzodev1375.components.ftp.impl.SftpClientImpl;
import ir.hanzodev1375.components.ftp.impl.SmbClientImpl;
import ir.hanzodev1375.components.ftp.interfaces.RemoteClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ir.hanzodev1375.components.R;

public class FtpConnectSheet extends BaseBlurBottomSheet {

  public static final String TAG = "FtpConnectSheet";

  private TextInputEditText etHost, etPort, etUsername, etPassword, etShare;
  private TextInputLayout tilHost, tilPort, tilUsername, tilPassword, tilShare;
  private MaterialButtonToggleGroup toggleProtocol;
  private MaterialButton btnConnect;
  private MaterialButton btnFtp, btnSftp, btnSmb;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public interface OnConnectedListener {
    void onConnected(RemoteClient client, String host, boolean isSftp);
  }

  private OnConnectedListener listener;

  public void setOnConnectedListener(OnConnectedListener l) {
    listener = l;
  }

  public static FtpConnectSheet newInstance() {
    return new FtpConnectSheet();
  }

  @Override
  protected void onContentReady(@NonNull ViewGroup contentContainer) {
    View view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_ftp_connect, null, false);
    contentContainer.addView(view);

    etHost = view.findViewById(R.id.etHost);
    etPort = view.findViewById(R.id.etPort);
    etUsername = view.findViewById(R.id.etUsername);
    etPassword = view.findViewById(R.id.etPassword);
    etShare = view.findViewById(R.id.etShare);
    tilHost = view.findViewById(R.id.tilHost);
    tilPort = view.findViewById(R.id.tilPort);
    tilUsername = view.findViewById(R.id.tilUsername);
    tilPassword = view.findViewById(R.id.tilPassword);
    tilShare = view.findViewById(R.id.tilShare);
    toggleProtocol = view.findViewById(R.id.toggleProtocol);
    btnConnect = view.findViewById(R.id.btnConnect);
    btnFtp = view.findViewById(R.id.btnFtp);
    btnSftp = view.findViewById(R.id.btnSftp);
    btnSmb = view.findViewById(R.id.btnSmb);

    toggleProtocol.check(R.id.btnFtp);

    toggleProtocol.addOnButtonCheckedListener(
        (group, checkedId, isChecked) -> {
          if (!isChecked) return;
          if (checkedId == R.id.btnSftp) {
            etPort.setText("22");
            tilShare.setVisibility(View.GONE);
          } else if (checkedId == R.id.btnFtp) {
            etPort.setText("21");
            tilShare.setVisibility(View.GONE);
          } else if (checkedId == R.id.btnSmb) {
            etPort.setText("445");
            tilShare.setVisibility(View.VISIBLE);
          }
        });

    btnConnect.setOnClickListener(v -> onConnectClick());
  }

  private void onConnectClick() {
    String host = getText(etHost);
    String portStr = getText(etPort);
    String user = getText(etUsername);
    String password = getText(etPassword);
    String share = getText(etShare);

    if (TextUtils.isEmpty(host)) {
      tilHost.setError(getString(R.string.ftp_error_host));
      return;
    }
    tilHost.setError(null);

    int port;
    try {
      port = Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      tilPort.setError(getString(R.string.ftp_error_port));
      return;
    }
    tilPort.setError(null);

    boolean isSftp = toggleProtocol.getCheckedButtonId() == R.id.btnSftp;
    boolean isSmb = toggleProtocol.getCheckedButtonId() == R.id.btnSmb;

    String finalHost = host;
    if (isSmb) {
      if (TextUtils.isEmpty(share)) {
        tilShare.setError("Share is required");
        return;
      }
      tilShare.setError(null);
      finalHost = host + "/" + share;
    }

    btnConnect.setEnabled(false);
    btnConnect.setText(R.string.ftp_connecting);

    final String finalHost1 = finalHost;
    final int finalPort = port;
    final String finalUser = user;
    final String finalPassword = password;

    executor.execute(
        () -> {
          RemoteClient client;
          if (isSmb) {
            client = new SmbClientImpl();
          } else if (isSftp) {
            client = new SftpClientImpl();
          } else {
            client = new FtpClientImpl();
          }
          try {
            client.connect(finalHost1, finalPort, finalUser, finalPassword);
            if (getActivity() == null) return;
            getActivity()
                .runOnUiThread(
                    () -> {
                      if (!isAdded()) return;
                      dismiss();
                      if (listener != null) listener.onConnected(client, finalHost1, isSftp);
                    });
          } catch (Exception e) {
            if (getActivity() == null) return;
            getActivity()
                .runOnUiThread(
                    () -> {
                      if (!isAdded()) return;
                      btnConnect.setEnabled(true);
                      btnConnect.setText(R.string.ftp_btn_connect);
                      GhostToast.makeText(
                              requireContext(),
                              getString(R.string.ftp_error_connect, e.getMessage()),
                              GhostToast.LENGTH_LONG)
                          .show();
                    });
          }
        });
  }

  private String getText(TextInputEditText et) {
    return et.getText() != null ? et.getText().toString().trim() : "";
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    executor.shutdownNow();
  }

  public void show(FragmentManager manager) {
    show(manager, TAG);
  }
}
