package ir.hanzodev1375.components.childern;

import android.view.View;

public interface IChild {

  View view();

  String pathTheme();

  /** Releases any held resource (player, webview, ...). Default: nothing to release. */
  default void release() {}
}
