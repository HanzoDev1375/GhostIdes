package ir.hanzodev1375.components.searchdata.adapter;

import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.searchdata.interfaces.OnLineClickListener;
import ir.hanzodev1375.components.searchdata.model.ContentMatch;
import ir.theme.M3Theme;

public class ContentMatchAdapter extends RecyclerView.Adapter<ContentMatchAdapter.VH> {
  private final List<ContentMatch> matches;
  private final String filePath;
  private final OnLineClickListener listener;
  private static final int COLOR_HIGHLIGHT_BG = 0xFFF9AA33;
  private static final int COLOR_HIGHLIGHT_FG = 0xFF000000;

  public ContentMatchAdapter(
      List<ContentMatch> matches, String filePath, OnLineClickListener listener) {
    this.matches = matches;
    this.filePath = filePath;
    this.listener = listener;
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_search_content_match, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    ContentMatch match = matches.get(position);
    holder.tvLineNumber.setText(String.valueOf(match.getLineNumber()));
    holder.tvLine.setText(buildHighlightedSpan(match));
    holder.itemView.setOnClickListener(
        v -> {
          if (listener != null) listener.onLineClick(filePath, match.getLineNumber());
        });
    M3Theme.listCard(holder.itemView);
  }

  private SpannableString buildHighlightedSpan(ContentMatch match) {
    String text = match.getLineText();
    SpannableString span = new SpannableString(text);
    for (int[] range : match.getMatchRanges()) {
      int start = range[0];
      int end = Math.min(range[1], text.length());
      if (start >= end) continue;
      span.setSpan(
          new BackgroundColorSpan(COLOR_HIGHLIGHT_BG),
          start,
          end,
          SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
      span.setSpan(
          new ForegroundColorSpan(COLOR_HIGHLIGHT_FG),
          start,
          end,
          SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return span;
  }

  @Override
  public int getItemCount() {
    return matches.size();
  }

  static class VH extends RecyclerView.ViewHolder {
    TextView tvLineNumber, tvLine;

    VH(@NonNull View itemView) {
      super(itemView);
      tvLineNumber = itemView.findViewById(R.id.tvLineNumber);
      tvLine = itemView.findViewById(R.id.tvLineContent);
    }
  }
}
