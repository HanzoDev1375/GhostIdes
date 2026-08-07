package ninja.coder.appuploader.main.appupdate;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;

import io.noties.markwon.Markwon;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;

public final class MarkwonHelper {

  private static volatile Markwon markwon;

  private MarkwonHelper() {
    throw new AssertionError("No instances.");
  }

  @NonNull
  private static Markwon get(@NonNull Context context) {
    if (markwon == null) {
      synchronized (MarkwonHelper.class) {
        if (markwon == null) {
          markwon =
              Markwon.builder(context.getApplicationContext())
                  .usePlugin(StrikethroughPlugin.create())
                  .usePlugin(LinkifyPlugin.create())
                  .usePlugin(TablePlugin.create(context))
                  .usePlugin(TaskListPlugin.create(context))
                  .usePlugin(HtmlPlugin.create())
                  .usePlugin(ImagesPlugin.create())
                  .build();
        }
      }
    }
    return markwon;
  }

  @NonNull
  public static CharSequence toCharSequence(@NonNull Context context, @NonNull String markdown) {
    return get(context).toMarkdown(markdown);
  }

  public static void setMarkdown(@NonNull TextView textView, @NonNull String markdown) {
    get(textView.getContext()).setMarkdown(textView, markdown);
    textView.setMovementMethod(LinkMovementMethod.getInstance());
  }
}
