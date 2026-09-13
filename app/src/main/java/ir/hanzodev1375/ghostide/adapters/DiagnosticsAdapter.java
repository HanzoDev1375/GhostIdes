package ir.hanzodev1375.ghostide.adapters;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.ghostide.R;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

public final class DiagnosticsAdapter extends RecyclerView.Adapter<DiagnosticsAdapter.Holder> {

  public interface OnItemClickListener {
    void onItemClick(int startLine, int startColumn, int endLine, int endColumn);
  }

  private List<DiagItem> items = new ArrayList<>();
  private OnItemClickListener listener;

  public void setOnItemClickListener(OnItemClickListener l) {
    this.listener = l;
  }

  public void submitList(List<Diagnostic> diagnostics) {
    items.clear();
    if (diagnostics != null) {
      for (Diagnostic d : diagnostics) {
        items.add(DiagItem.from(d));
      }
    }
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_diag, parent, false);
    return new Holder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {
    DiagItem item = items.get(position);
    holder.message.setText(item.message);
    String lineStr = String.valueOf(item.startLine + 1);
    String severity = holder.itemView.getContext().getString(severityLabel(item.severity));
    holder.meta.setText(holder.itemView.getContext().getString(R.string.diagnostics_line, item.startLine + 1) + " · " + severity);
    holder.icon.setImageResource(severityIcon(item.severity));
    holder.icon.setColorFilter(severityColor(item.severity), PorterDuff.Mode.SRC_IN);
    holder.itemView.setOnClickListener(
        v -> {
          if (listener != null) {
            listener.onItemClick(item.startLine, item.startColumn, item.endLine, item.endColumn);
          }
        });
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  @ColorInt
  static int severityColor(DiagnosticSeverity severity) {
    if (severity == null) return Color.GRAY;
    switch (severity) {
      case Error:
        return Color.parseColor("#F44336");
      case Warning:
        return Color.parseColor("#FFC107");
      case Information:
        return Color.parseColor("#2196F3");
      default:
        return Color.parseColor("#9E9E9E");
    }
  }

  @DrawableRes
  static int severityIcon(DiagnosticSeverity severity) {
    if (severity == null) return R.drawable.ic_info;
    switch (severity) {
      case Error:
        return R.drawable.ic_close;
      case Warning:
      case Information:
      default:
        return R.drawable.ic_info;
    }
  }

  private int severityLabel(DiagnosticSeverity severity) {
    if (severity == null) return R.string.diagnostics_info;
    switch (severity) {
      case Error:
        return R.string.diagnostics_error;
      case Warning:
        return R.string.diagnostics_warning;
      case Information:
        return R.string.diagnostics_info;
      default:
        return R.string.diagnostics_hint;
    }
  }

  static final class DiagItem {
    int startLine;
    int startColumn;
    int endLine;
    int endColumn;
    DiagnosticSeverity severity;
    String message;

    static DiagItem from(Diagnostic d) {
      DiagItem item = new DiagItem();
      if (d.getRange() != null && d.getRange().getStart() != null) {
        item.startLine = d.getRange().getStart().getLine();
        item.startColumn = d.getRange().getStart().getCharacter();
      }
      if (d.getRange() != null && d.getRange().getEnd() != null) {
        item.endLine = d.getRange().getEnd().getLine();
        item.endColumn = d.getRange().getEnd().getCharacter();
      }
      item.severity = d.getSeverity();
      item.message = extractMessage(d);
      if (item.message == null || item.message.isEmpty()) {
        item.message = d.getSeverity() != null ? d.getSeverity().toString() : "";
      }
      return item;
    }

    private static String extractMessage(Diagnostic d) {
      Either<String, MarkupContent> msg = d.getMessage();
      if (msg == null) {
        return "";
      }
      if (msg.isLeft()) {
        return msg.getLeft() != null ? msg.getLeft() : "";
      }
      MarkupContent markdown = msg.getRight();
      return markdown != null && markdown.getValue() != null ? markdown.getValue() : "";
    }
  }

  static final class Holder extends RecyclerView.ViewHolder {
    final ImageView icon;
    final TextView message;
    final TextView meta;

    Holder(@NonNull View itemView) {
      super(itemView);
      icon = itemView.findViewById(R.id.ivDiagIcon);
      message = itemView.findViewById(R.id.tvDiagMessage);
      meta = itemView.findViewById(R.id.tvDiagMeta);
    }
  }
}