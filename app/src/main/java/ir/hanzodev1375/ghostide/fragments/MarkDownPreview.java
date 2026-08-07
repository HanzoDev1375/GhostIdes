package ir.hanzodev1375.ghostide.fragments;

import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import com.blankj.utilcode.util.FileIOUtils;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ninja.coder.appuploader.main.appupdate.MarkwonHelper;

public class MarkDownPreview extends BaseBlurBottomSheet {

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    ScrollView scrollView = new ScrollView(contentContainer.getContext());
    scrollView.setVerticalScrollBarEnabled(false);
    scrollView.setHorizontalScrollBarEnabled(false);
    scrollView.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    TextView tv = new TextView(contentContainer.getContext());
    tv.setPadding(9, 9, 9, 9);
    tv.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    scrollView.addView(tv);
    contentContainer.addView(scrollView);

    MarkwonHelper.setMarkdown(tv, FileIOUtils.readFile2String(getArguments().getString("md")));
  }
}
