package ir.hanzodev1375.ghostide.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.ghostide.adapters.DiagnosticsAdapter;
import ir.hanzodev1375.ghostide.databinding.DiagSheetBinding;
import ir.hanzodev1375.ghostide.listeners.LspDiagnosticsEventListener;
import ir.theme.M3Theme;
import java.util.List;
import org.eclipse.lsp4j.Diagnostic;

/** Bottom sheet listing the diagnostics of a single file. Opens from the editor's status icon. */
public final class DiagnosticsBottomSheet extends BaseBlurBottomSheet {

  public static final String TAG = "DiagnosticsBottomSheet";

  public interface OnDiagnosticClickListener {
    void onDiagnosticClick(int startLine, int startColumn, int endLine, int endColumn);
  }

  private LspEditor lspEditor;
  private OnDiagnosticClickListener clickListener;
  private DiagSheetBinding binding;
  private DiagnosticsAdapter adapter;
  private LspDiagnosticsEventListener diagnosticsListener;

  public static DiagnosticsBottomSheet newInstance() {
    return new DiagnosticsBottomSheet();
  }

  public void setLspEditor(@NonNull LspEditor lspEditor) {
    this.lspEditor = lspEditor;
  }

  public void setOnDiagnosticClickListener(OnDiagnosticClickListener l) {
    this.clickListener = l;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    binding = DiagSheetBinding.inflate(getLayoutInflater(), contentContainer, false);
    contentContainer.addView(binding.getRoot());

    adapter = new DiagnosticsAdapter();
    adapter.setOnItemClickListener(
        (startLine, startColumn, endLine, endColumn) -> {
          if (clickListener != null) {
            clickListener.onDiagnosticClick(startLine, startColumn, endLine, endColumn);
          }
          dismiss();
        });
    binding.rvDiagnostics.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.rvDiagnostics.setAdapter(adapter);

    M3Theme.applyTopLevel(binding.getRoot());

    if (lspEditor != null) {
      registerDiagnosticsListener();
      refresh(lspEditor.getDiagnostics());
    } else {
      refresh(null);
    }
  }

  private void registerDiagnosticsListener() {
    unregisterDiagnosticsListener();
    diagnosticsListener =
        new LspDiagnosticsEventListener(
            lspEditor,
            (editor, diagnostics) -> {
              if (binding == null) {
                return;
              }
              binding.getRoot().post(() -> refresh(diagnostics));
            });
    lspEditor.getEventManager().addEventListener(diagnosticsListener);
  }

  private void unregisterDiagnosticsListener() {
    if (diagnosticsListener == null) {
      return;
    }
    try {
      diagnosticsListener
          .getEditor()
          .getEventManager()
          .removeEventListener(diagnosticsListener.getClass());
    } catch (Exception ignored) {
    }
    diagnosticsListener = null;
  }

  private void refresh(@Nullable List<Diagnostic> diagnostics) {
    if (binding == null) {
      return;
    }
    List<Diagnostic> list = diagnostics == null ? lspEditor.getDiagnostics() : diagnostics;
    adapter.submitList(list);
    boolean empty = list == null || list.isEmpty();
    binding.rvDiagnostics.setVisibility(empty ? View.GONE : View.VISIBLE);
    binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    binding.tvDiagCount.setVisibility(empty ? View.GONE : View.VISIBLE);
    binding.tvDiagCount.setText(String.valueOf(list == null ? 0 : list.size()));
  }

  @Override
  public void onDestroyView() {
    unregisterDiagnosticsListener();
    super.onDestroyView();
    binding = null;
  }
}
