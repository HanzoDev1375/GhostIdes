package ir.hanzodev1375.ghostide.terminal;

import com.termux.terminal.TerminalSession;

/** یه تب/سشن ترمینال؛ فقط برای نگه‌داری سشن به‌همراه یه آی‌دی و عنوان قابل‌نمایش. */
public class TerminalTab {

  public final int id;
  public final TerminalSession session;
  private String customTitle;

  public TerminalTab(int id, TerminalSession session) {
    this.id = id;
    this.session = session;
  }

  /** عنوانی که توی تب نشون داده میشه: عنوانِ ست‌شده توسط شل (escape sequence) یا "Session N". */
  public String getDisplayTitle() {
    if (customTitle != null && !customTitle.isEmpty()) return customTitle;
    String shellTitle = session.getTitle();
    if (shellTitle != null && !shellTitle.isEmpty()) return shellTitle;
    return "Session " + id;
  }

  public void setCustomTitle(String customTitle) {
    this.customTitle = customTitle;
  }
}
