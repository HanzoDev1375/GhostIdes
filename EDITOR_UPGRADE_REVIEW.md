# Ghost IDE — بررسی سورس sora-editor 0.24.6 و برآورد هر تکلیف

منبع بررسی: سورس کامل 0.24.6 در `/tmp/opencode/sora/sora-editor-0.24.6/` (جاوا/کاتلین، نه jar).
نتیجه‌گیری کلی: تکلیف ۷ (فولدینگ) و بخش «Delimiter Mismatch» تکلیف ۵ = **اسکیپ**؛ بقیه = قابل پیاده‌سازی.

## ۱ — میانبرهای کیبورد سخت‌افزاری  ✅ 100%
- `KeyBindingEvent` در `editor/.../event/KeyBindingEvent.java` موجود و `extends EditorKeyEvent`.
- متدها: `getKeyCode()` (خط 69)، `isShiftPressed()` (خط 107)، `isCtrlPressed()` (خط 118).
- این رویداد **فقط برای ترکیب‌های چندکلیده** dispatch می‌شود (`EditorKeyEventHandler` خط 79 به بعد)؛ پس نیازی به چک `isSingleKey` نیست.
- اجرا: `subscribeEvent(KeyBindingEvent.class, (ev, un) -> {...})` در `IdeEditor.init()` — الگو مثل `ContentChangeEvent` خط 121-127.
- undo/redo/copy/cut/paste/selectAll همان روی خود `CodeEditor` است؛ فقط ctrl+S به callback فرگمنت/اکتیویتی وصل می‌شود.

## ۲ — ایندنت/آندرایند + Tab  ✅ 100%
- `CodeEditor.indentSelection()` خط 1743، `unindentSelection()` خط 1803، `indentOrCommitTab()` خط 1866.
- `onSharedPreferenceChanged` بعد از تشخیص کلید جدید فقط متد مربوطه را صدا بزند.

## ۳ — duplicate خط/انتخاب + واژه‌گزینی  ✅ 100%
- `duplicateLine()` خط 3837، `duplicateSelection()` خط 3853.
- `selectCurrentWord()` خط 3895، `selectWord(...)` خط 3906، `getWordRange(...)` خط 3916.
- `DoubleClickEvent` موجود است (`editor/.../event/DoubleClickEvent.java`).
- `setStickyTextSelection(boolean)` خط 917.

## ۴ — هایلایت بلاک + BlockLine  ✅ 100%
- `setHighlightCurrentBlock(boolean)` خط 893 (در `CodeEditor` خط 640 هم active پیش‌فرض هست).
- `setBlockLineEnabled(boolean)` خط 3353، `setBlockLineWidth(float)` خط 1071.
- رنگ‌ها در `widget/schemes/EditorColorScheme.java`: `BLOCK_LINE=14`، `BLOCK_LINE_CURRENT=15`، `SIDE_BLOCK_LINE=38`.
- `GhostColorScheme` بایستی همان‌های 14/15/38 را override کند.

## ۵ — انیمیشن cursor + Delimiter Mismatch  ⚠️ نیمه
- `setCursorAnimationEnabled(boolean)` خط 1155 ✅.
- **`setHighlightDelimiterMismatches` در 0.24.6 اصلاً وجود ندارد** — این بخش را اسکیپ می‌کنیم (نمی‌شود ۱۰۰٪).
- رنگ‌های `HIGHLIGHTED_DELIMITERS_BACKGROUND/UNDERLINE/FOREGROUND/BORDER` (41/40/39/75) برای براکت جفت است، نه mismatch.

## ۶ — شماره‌خط: Tip + Divider + Align  ✅ 100%
- `setLineNumberTipTextProvider(LineNumberTipTextProvider)` خط 1294.
- اینترفیس: `widget/style/LineNumberTipTextProvider.kt` → متد `fun getCurrentText(editor: CodeEditor): String`.
- `setFirstLineNumberAlwaysVisible(boolean)` خط 769، `setLineNumberAlign(Paint.Align)` خط 3000، `setDividerWidth(float)` خط 2907، `setDividerMargin(float,float)` خط 2857، `setLineNumberMarginLeft(float)` خط 2879.
- رنگ: `LINE_DIVIDER=1`.

## ۷ — Code Folding  ✗ اسکیپ (ریسک بالا)
دلایل (از سورس):
- `editor/.../lang/folding/FoldingRegion.java` یک مدلِ مرده است — **هیچ‌جا در ادیتور استفاده نمی‌شود**.
- در `CodeEditor.java` / `EditorRenderer.java` هیچ state یا رندر «خط تا شده» نیست (جستجوی `fold` خالی بود).
- پنهان‌کردن متن فقط با دست‌کاری `onDraw`/clip با محاسبه‌ی سطر + اسکرول + wrap ممکن است → غیرقابل تضمین بدون بیلد.
- فلش گاتر (`LineSideIcon`) فقط از طریق line-stylesِ خودِ آنالایزرِ هر زبان می‌آید (دست‌زدن ~۲۰ فایل زبان = خارج از قانون پرامپت).

## ۸ — منوی کانتکست سفارشی  ✅ 100%
- `EditorContextMenuCreator` کلاس باز (open) در `widget/component/EditorContextMenuCreator.kt`.
- در `CodeEditor` پیش‌فرض ثبت شده (خط 634) و با `replaceComponent(EditorContextMenuCreator.class, ...)` جایگزین می‌شود (خط 437-450).
- ساخت از جاوا: کلاس جدید `editor/.../ui/EditorContextMenu.java` `extends EditorContextMenuCreator` → override `onCreateContextMenu(CreateContextMenuEvent)` + `buildMenu(menu) { item { title = ...; onClick { } } }`.
- رویداد: `CreateContextMenuEvent` موجود است.

## ۹ — LSP واقعیِ ناقص  ✅ (کد کامپایل‌امن) — رفتار نیاز به تست میدانی
### ۹.۱ Document Highlight
- `RequestManager.documentHighlight(DocumentHighlightParams)` در `DefaultRequestManager.kt` خط 470.
- نمایش: `LspEditor.showDocumentHighlight(List<DocumentHighlight>)` در `LspEditor.kt` خط 357.
- کار: در فرگمنت روی `SelectionChangeEvent` با throttle صدازدن و نتیجه به `showDocumentHighlight`.
### ۹.۲ هوان لمسی
- `RequestManager.hover(...)` در `RequestManager.kt` خط 149/152.
- نمایش: `LspEditor.showHover(Hover)` در `LspEditor.kt` خط 349.
- کار: موقع حرکت cursor، position فعلی → `hover` → `showHover` (با throttle).
### ۹.۳ Quick-fix از دیاگنوستیک
- `RequestManager.codeAction(CodeActionParams)` در `RequestManager.kt` خط 170.
- چک position داخل `DiagnosticRegion` از `editor.getDiagnostics()`، بعد مثل `handleCodeAction()` اکشن‌ویندو.
### ۹.۴ موارد خودکار (signature/inlay/color/format)
- دست نزن — تأیید شده که `LspEditorUIDelegate` (در سورس `editor-lsp`) خودش مدیریت می‌کند.

## ۱۰ — jump to line + موارد خرد  ✅ 100%
- `CodeEditor.jumpToLine(int)` خط 4193 (دقت: ورودی صفر-بیس؛ از دیالوگ `line-1` بده).
- `CodeEditor.setEditable(boolean)` خط 3324.

---

## جدول جمع‌بندی

| تکلیف | نتیجه | اقدام |
|---|---|---|
| ۱ میانبر کیبورد | ✅ 100% | پیاده‌سازی در `IdeEditor.init()` |
| ۲ ایندنت | ✅ 100% | اکشن‌ویندو + Tab |
| ۳ duplicate | ✅ 100% | `setCutLine()` + `DoubleClickEvent` |
| ۴ بلاک | ✅ 100% | `init()` + کلید pref جدید |
| ۵ cursor | ⚠️ نیمه | فقط `setCursorAnimationEnabled`؛ Delimiter Mismatch = اسکیپ |
| ۶ شماره‌خط | ✅ 100% | `init()` + `LineNumberTipTextProvider` |
| ۷ فولدینگ | ✗ اسکیپ | ریسک بالا، بدون پشتیبانی کتابخانه |
| ۸ منوی کانتکست | ✅ 100% | کلاس جدید + `replaceComponent` |
| ۹ LSP ناقص | ✅ کد/⚠️ رفتار | نیاز به تست میدانی دارد |
| ۱۰ jump-to-line | ✅ 100% | دیالوگ + `jumpToLine(line-1)` |

## نکات ایمنی (برای اد کردن)
- فقط فایل‌های لیست‌شده در پرامپت دست خورند؛ `Constants` تکراری نشود.
- کلیدهای جدید در `onSharedPreferenceChanged` اضافه شوند.
- بدون گریدل: فقط سینتکس‌چک دستی (جاوا 17).