package ir.ghostide.logcat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.listitem.ListItemCardView;
import com.google.android.material.listitem.ListItemViewHolder;
import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> implements Filterable {

  private List<LogEntry> originalList;
  private List<LogEntry> filteredList;
  private String lastQuery = "";

  public LogAdapter(List<LogEntry> logs) {
    this.originalList = new ArrayList<>(logs);
    this.filteredList = new ArrayList<>(logs);
  }

  public void updateData(List<LogEntry> newLogs) {
    this.originalList.clear();
    this.originalList.addAll(newLogs);
    this.filteredList.clear();
    this.filteredList.addAll(originalList);
    this.lastQuery = "";
    notifyDataSetChanged();
  }

  public String getAllFilteredMessages() {
    StringBuilder sb = new StringBuilder();
    for (LogEntry log : filteredList) {
      sb.append(log.getTimestamp())
          .append(" ")
          .append(log.getPriority())
          .append("/")
          .append(log.getTag())
          .append(": ")
          .append(log.getMessage())
          .append("\n");
    }
    return sb.toString();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    LogEntry log = filteredList.get(position);
    holder.tvTimestamp.setText(log.getTimestamp());
    holder.tvPriority.setText(log.getPriority());
    holder.tvTag.setText(highlightText(log.getTag(), lastQuery, holder.tvTag.getContext()));
    holder.tvMessage.setText(
        highlightText(log.getMessage(), lastQuery, holder.tvMessage.getContext()));
    holder.tvPriority.setTextColor(getPriorityColor(log.getPriority()));
    holder.itemView.setOnClickListener(v -> showPopupMenu(v, log));
    holder.bind(position, getItemCount());
  }

  @Override
  public int getItemCount() {
    return filteredList.size();
  }

  private SpannableString highlightText(String text, String query, Context context) {
    if (query == null || query.isEmpty() || !text.toLowerCase().contains(query.toLowerCase())) {
      return new SpannableString(text);
    }
    SpannableString spannable = new SpannableString(text);
    String lowerText = text.toLowerCase();
    String lowerQuery = query.toLowerCase();
    int start = 0;
    while ((start = lowerText.indexOf(lowerQuery, start)) != -1) {
      int end = start + query.length();
      spannable.setSpan(
          new ForegroundColorSpan(
              MaterialColors.getColor(context, R.attr.colorOnPrimary, 0xDCB304)),
          start,
          end,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      start = end;
    }
    return spannable;
  }

  private void showPopupMenu(View anchor, LogEntry log) {
    PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
    popup.getMenu().add("Copy Log");
    popup.getMenu().add("Share Log");
    popup.getMenu().add("Copy Tag");
    popup.getMenu().add("Copy Message");
    popup.getMenu().add("Copy All Messages");

    popup.setOnMenuItemClickListener(
        item -> {
          String title = item.getTitle().toString();
          ClipboardManager clipboard =
              (ClipboardManager) anchor.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
          switch (title) {
            case "Copy Log":
              String fullLog =
                  log.getTimestamp()
                      + " "
                      + log.getPriority()
                      + "/"
                      + log.getTag()
                      + ": "
                      + log.getMessage();
              clipboard.setPrimaryClip(ClipData.newPlainText("log", fullLog));
              Toast.makeText(anchor.getContext(), "Copied", Toast.LENGTH_SHORT).show();
              break;
            case "Share Log":
              fullLog =
                  log.getTimestamp()
                      + " "
                      + log.getPriority()
                      + "/"
                      + log.getTag()
                      + ": "
                      + log.getMessage();
              Intent share = new Intent(Intent.ACTION_SEND);
              share.setType("text/plain");
              share.putExtra(Intent.EXTRA_TEXT, fullLog);
              anchor.getContext().startActivity(Intent.createChooser(share, "Share Log"));
              break;
            case "Copy Tag":
              clipboard.setPrimaryClip(ClipData.newPlainText("tag", log.getTag()));
              Toast.makeText(anchor.getContext(), "Tag copied", Toast.LENGTH_SHORT).show();
              break;
            case "Copy Message":
              clipboard.setPrimaryClip(ClipData.newPlainText("message", log.getMessage()));
              Toast.makeText(anchor.getContext(), "Message copied", Toast.LENGTH_SHORT).show();
              break;
            case "Copy All Messages":
              String all = getAllFilteredMessages();
              clipboard.setPrimaryClip(ClipData.newPlainText("all_logs", all));
              Toast.makeText(
                      anchor.getContext(),
                      "All messages copied (" + filteredList.size() + " lines)",
                      Toast.LENGTH_LONG)
                  .show();
              break;
          }
          return true;
        });
    popup.show();
  }

  @Override
  public Filter getFilter() {
    return new Filter() {
      @Override
      protected FilterResults performFiltering(CharSequence constraint) {
        lastQuery = constraint.toString();
        String query = lastQuery.toLowerCase();
        List<LogEntry> filtered = new ArrayList<>();
        if (query.isEmpty()) {
          filtered.addAll(originalList);
        } else {
          for (LogEntry log : originalList) {
            if (log.getTag().toLowerCase().contains(query)
                || log.getMessage().toLowerCase().contains(query)) {
              filtered.add(log);
            }
          }
        }
        FilterResults results = new FilterResults();
        results.values = filtered;
        return results;
      }

      @Override
      protected void publishResults(CharSequence constraint, FilterResults results) {
        filteredList.clear();
        filteredList.addAll((List<LogEntry>) results.values);
        notifyDataSetChanged();
      }
    };
  }

  private int getPriorityColor(String priority) {
    switch (priority) {
      case "V":
        return 0xFF808080;
      case "D":
        return 0xFF00796B;
      case "I":
        return 0xFF1976D2;
      case "W":
        return 0xFFFFA000;
      case "E":
        return 0xFFD32F2F;
      default:
        return 0xFF000000;
    }
  }

  static class ViewHolder extends ListItemViewHolder {
    TextView tvTimestamp, tvPriority, tvTag, tvMessage;
    ListItemCardView root;

    ViewHolder(View itemView) {
      super(itemView);
      tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
      tvPriority = itemView.findViewById(R.id.tv_priority);
      tvTag = itemView.findViewById(R.id.tv_tag);
      tvMessage = itemView.findViewById(R.id.tv_message);
      root = itemView.findViewById(R.id.root);
      root.setCardBackgroundColor(
          ColorUtils.setAlphaComponent(
              MaterialColors.getColor(root, R.attr.colorSurfaceContainer), 128));
    }
  }
}
