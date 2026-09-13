# Ghost IDE — ارتقای کد ادیتور (پرامپت کامل)

هدف: پیاده‌سازی ویژگی‌هایی که در کتابخانه‌ی `sora-editor`/`sora-editor-lsp` (نسخه 0.24.6) عمومی است ولی در اپِ Ghost IDE استفاده نشده، تا ادیتور از همه ادیتورهای اندرویدی بهتر شود.

## قوانین پروژه (قبل از هر کاری)

- **زبان کد: جاوا فقط** (یک فایل `LspUriBridge.kt` استثناست؛ کد جدید را جاوا بنویس).
- **کامنت‌ها: فارسی** (مانند بقیه‌ی پروژه).
- **ساختار پوشه‌ها:** کلاس‌های رابط کاربری در سطح ریشه‌ی پکیج `ir.hanzodev1375.ghostide` در پوشه‌های `activity/`, `fragments/`, `adapters/`, `dialogs/`, `listeners/`, `customui/` — هرگز زیر `editorlangs/` نساز.
- **اینترنت قطع/گران است:** هیچ دانلودی نکن (نه `gradlew`، نه وابستگی جدید، نه تصویر). گریدل در دسترس نیست — تغییرات فقط با اینشِپمِن منطقی و سینتکس بررسی شود.
- پیش‌فرض‌های `SharedPreferences` را همان‌طور که هست حفظ کن؛ تغییر نوع‌های موجود ممنوع (چون داده‌ی کاربر ذخیره‌شده است).
- فقط کدی را که پرامپت می‌گوید تغییر بده؛ فایل‌های نامرتبط را دست نزن.

## نقشه‌ی معماری فعلی (چه‌جایی چی هست)

| بخش | فایل |
|---|---|
| کلاس ادیتور (extends `io.github.rosemoe.sora.widget.CodeEditor`) | `editor/src/main/java/ir/hanzodev1375/ghostide/codeeditors/IdeEditor.java` (586 خط) |
| اکشن‌ویندو (کپی/پیست/فرمت/LSP...) | `editor/src/main/java/ir/hanzodev1375/ghostide/codeeditors/ui/CustomEditorTextActionWindow.java` |
| تنظیمات/کلیدها | `editor/.../codeeditors/setting/Constants.java` + `PreferencesUtils.java` |
| فرگمنت ادیتور (اتصال LSP، وضعیت دیاگنوستیک، گوشی‌دادن‌ها) | `app/src/main/java/ir/hanzodev1375/ghostide/fragments/EditorFragment.java` (681 خط) |
| اکتیویتی (نوار ابزار، عمل‌ها، دیالوگ‌ها) | `app/src/main/java/ir/hanzodev1375/ghostide/activity/EditorActivity.java` (1595 خط) |
| سرچ‌ویندو (جایگزین/جستجو) | `app/.../customui/GhostIdeEditorSearch.java` |
| نمادبار | `app/.../customui/LayoutSymbolbar.java` |
| روتینگ LSP | `editor/.../langs/lsp/LspRouter.java`, `LspExtensionBridge.java`, `LspInitParamsHook.java` |
| اتصال LSP از فرگمنت | `LspViewModel.connect(projectRoot, filePath, editor)` → `EditorFragment` خط ۱۸۳ |

### نکات مهم معماری `IdeEditor` (قبلاً خوانده شده)
- `init()` (خط ۷۴-۱۳۶): همه‌ی کامپوننت‌ها و تنظیمات را راه‌اندازی می‌کند؛ `onSharedPreferenceChanged` (خط ۴۴۳-۵۱۴) با `switch (key)` تنظیمات را به‌روز می‌کند. هر پیشنهاد تنظیم جدید اینجا لحاظ شود.
- گوشی‌دادن رویدادها با `subscribeEvent(Class, (ev, un) -> {...})` (فرم lambda).
- `setEditorLanguage()` (خط ۱۴۶) override شده — اگر کاری به زبان جدید در تازه‌هایت داری اینجا اضافه کن.
- `setCurrentFilePath(String)` (خط ۱۶۸) — بعد از ست شدن فایل، همه‌ی وابسته‌ها آرگومان گرفته‌اند.
- `setCutLine()` (خط ۱۶۱-۱۶۶): فعلاً فقط `cutLine()` صدا می‌زند؛ دو عمل `duplicateLine()` و `selectCurrentWord()` داخل کامنت هستند — تکمیلشان کن.
- `setLspEditor(LspEditor)` (خط ۲۰۷): LspEditorِ از قبل وصل‌شده را نگه می‌دارد (کاراکشن‌ویندو از همین استفاده می‌کند).
- override شده: `setInlayHints()` (خط ۲۷۶) که گوست را ادغام می‌کند و `setInlayHintsRaw()` (خط ۲۸۸). اگر hint می‌فرستی از `setInlayHints` رد بشو که حتماً `super.setInlayHints` را صدا بزنی.

### اکشن‌ویندو (نباید خراب شود)
دکمه‌ها: select all / cut / paste / copy / long select / format / expand selection (بدون عمل، TODO در خط ۳۴۶-۳۵۱) / translate / LSP definition / references / rename / code action. وضعیت‌ها در `updateButtonState()` (خط ۲۸۵) و عمل در `onClick(View)` (خط ۳۱۶) است. `formatBtn` در خط ۲۹۱ همیشه `VISIBLE` است (even بدون انتخاب).

### نوار ابزار EditorActivity (`stepToolbar()` خط ۶۶۱-۷۰۷)
دکمه‌ها با attention: `git`, split, «file tree», search, undo, redo, more, plugins.
مکان اضافه‌کردن عمل‌های جدید (comment/duplicate/indent/goto-line): یا در `setupMenuCalltoAction(View)` (خط ۱۰۷۱) یا در اکشن‌ویندو (فقط وقتی متن انتخاب/ادیتور فوکوس است).

### اتصال LSP (فرگمنت، خط ۱۸۰-۲۱۰)
```java
lspViewModel.connect(projectRoot, filePath, editor);
// هنگام isConnected():
editor.setLspEditor(connected);
lspViewModel.applySettings(setting.isHover(), setting.isInlayHint(), setting.isSignatureHelp());
connected.setDiagnostics(setting.isDiagnostics() ? connected.getDiagnostics() : Collections.emptyList());
registerDiagnosticsListener(connected);
```
دیاگنوستیک‌لیسنر: `LspDiagnosticsEventListener` (رویداد `editor/publishDiagnostics`) + `registerDiagnosticsListener()` خط ۶۲۵.

## واقعیت LSP که نباید دوباره پیاده شود (نکن!)
کتابخانه `editor-lsp` داخل خودش (کلاس داخلی `LspEditorUIDelegate`) **خودکار** این‌ها را مدیریت می‌کند وقتی `lspEditor.setEditor(codeEditor)` صدا زده شود:
- هوان‌پول‌آپ (فقط در Mouse mode واقعی؛ روی لمس/موبایل فعال نمی‌شود مگر خودت trigger کنی)
- سینتکِس سیگنچر/امضای تابع (`SignatureHelpWindow`)
- inlay-hint های LSP (خودش `TextInlayHintRenderer`/`ColorInlayHintRenderer` ثبت می‌کند)
- رنگ‌ها (documentColor)
- پنجره‌ی کداکشن (`CodeActionWindow`) و تاول‌تیپ دیاگنوستیک
- فرمت (اگر سرور `documentFormattingProvider` داشته باشد، `LspFormatter` می‌سازد)

پس پرامپت‌های زیر برای هوان/سیگنچر/inlay/فرمت را **«تأیید که کار می‌کند + استایل»** انجام بده، نه پیاده‌سازی صفر.

---

## تکلیف ۱ — میانبرهای کیبورد سخت‌افزاری (بالاترین اولویت)
وضعیت: ادیتور رویداد `io.github.rosemoe.sora.event.KeyBindingEvent` و `EditorKeyEvent` را dispatch می‌کند ولی هیچ‌جا subscribe نشده. یعنی با کیبورد خارجی/Bluetooth هیچ میانبری کار نمی‌کند.
کار:
1. در `IdeEditor.init()` گوش کن:
```java
subscribeEvent(KeyBindingEvent.class, (ev, un) -> {
    if (!ev.isSingleKey() /* یا بررسی ctrl/shift */) return;
    int key = ev.getKeyCode();                 // KeyEvent.KEYCODE_*
    boolean ctrl = ev.isCtrlPressed();
    boolean shift = ev.isShiftPressed();
    // پیاده‌سازی میانبرها:
    // ctrl+Z='undo'  ctrl+Y='redo'  ctrl+C/V/X/A  ctrl+F='فعال‌سازی سرچ فرگمنت'
    // ctrl+S='ذخیره' (از طریق callback به EditorActivity.setTabDirty/saveCurrentFile)
    // ctrl+D='duplicateLine'  ctrl+/  یا ctrl+Shift+* = توضیح/۲۴
    // Tab/Shift+Tab = indent/unindent (به تکلیف ۲)
    // ctrl+G = گوتو لاین (ابه setSelectionRegion)
});
```
2. API واقعی: `KeyBindingEvent` از `EditorKeyEvent` می‌آید؛ متدهای `getKeyCode()`, `getKeyMetaState()`, `isCtrlPressed()`, `isShiftPressed()`, `isAltPressed()` را از سورس 0.24.6 استفاده کن (در `/tmp` نیست؛ از `io.github.rosemoe.sora.event.EditorKeyEvent`).
3. تغییر تمرکز: از `EditorFocusChangeEvent` (در `EditorFragment`) پرهیز؛ بهتر است میانبرها عمومی در `IdeEditor` باشند.
4. توضیح/غیرتوضیح: چون API آماده‌ای در sora ندارد، پیاده‌سازی کن: زبانِ فایل فعلی را از `editor.getEditorLanguage()` بگیر و `//` یا `/* */` (بر اساس کامنت‌های `EditorLanguage` در `codeeditors/langs`) به خط‌های انتخاب اضافه/حذف کن. مجموعه‌ی زبان‌ها در `LanguageManager.resolve()` موجود است.
5. WHERE: فقط در `IdeEditor.java` + شاید یک callback برای «فعال‌کردن سرچ» که از `EditorFragment` پر می‌شود. بک‌کنندگانی برای `undo/redo/copy/cut/paste/selectAll/format` نیاز نیست (روی خود editor هستند).

## تکلیف ۲ — ایندنت/آندرایند انتخاب + Tab = indent
APIهای آماده عمومی (استفاده نشده): `CodeEditor.indentSelection()`, `indentLines(int, int)`, `unindentSelection()`, `indentLines(...)`, `indentOrCommitTab()`.
کار:
1. به اکشن‌ویندو یا منوی more یک «Indent» و «Unindent» اضافه کن (`editor.indentSelection()/editor.unindentSelection()`).
2. در `IdeEditor` حین Tab: رفتار کنار بگذار = اگر `setting.useTabIndentation()` true و cursor در ابتدای line بود → `indentOrCommitTab()`. (این pref در `PreferencesUtils.useTabIndentation()` هست ولی هیچ‌جا اعمال نشده.)
3. `SharedPreferenceKeys.KEY_CODE_EDITOR_TAB_INDENT` را در `onSharedPreferenceChanged` بگیر.

## تکلیف ۳ — duplicate خط/انتخاب + واژه‌گزینی
APIهای آماده: `duplicateLine()`, `duplicateSelection()`, `selectWord()`, `selectCurrentWord()`, `getWordRange()`.
کار:
1. `setCutLine()` را کامل کن: علاوه بر `cutLine()`, به اکشن‌ویندو `duplicateLine` اضافه کن (و `duplicateSelection` وقتی انتخاب هست).
2. `selectCurrentWord()` را به «long-select روی insert handle» یا دکمه‌ی select-word اضافه نکن — به‌جای آن دبل‌تپ (`DoubleClickEvent.subscribeEvent`) را به انتخاب واژه متصل کن.
3. `setStickyTextSelection(true)` در `init()`: انتخاب را قابل امتداد با درگ پنجره‌ی حاشیه می‌کند.

## تکلیف ۴ — هایلایت بلاک + BlockLine + میزانی
APIهای آماده: `setHighlightCurrentBlock(boolean)`, `setBlockLineEnabled(boolean)`, `setBlockLineWidth(double)`, `isHighlightBracketPair`...
رنگ‌ها در `EditorColorScheme`: `BLOCK_LINE`, `BLOCK_LINE_CURRENT`, `SIDE_BLOCK_LINE`.
کار:
1. در `init()`: `setHighlightCurrentBlock(true)` (زیرا تحلیل‌گرها `CodeBlock` می‌سازند ولی هیچ‌جا فعال نیست).
2. یک تنظیم جدید `pref_code_editor_block_line` در `Constants` + getter/setter در `PreferencesUtils` + اعمال در `updateEditor*()` با همان الگوی بقیه.
3. اطمینان از سازگاری با `GhostColorScheme` (در `editor/.../codeeditors/colorscheme/GhostColorScheme.java`) — رنگ‌های جدید را override کن.

## تکلیف ۵ — انیمیشن cursor + هایلایت براکت جفت‌نشده
۱. `setCursorAnimationEnabled(true)` در `init()`؛ احساس صحیح در تلفن.
۲. `setHighlightDelimiterMismatches(true)` — هایلایتِ جداگانه برای براکت جفت‌نشده (رنگ: `EditorColorScheme` همان `DELIMITER_MISMATCH` یا از `ColorScheme` بگیر).
۳. پیشنهاد تنظیم فعلی را در منوی تنظیمات (فایل `app/src/main/res/xml/*_settings.xml`) پیدا و اضافه کن.

## تکلیف ۶ — شماره‌خط: Tip + Divider + Align
APIهای آماده: `setLineNumberTipTextProvider(LineNumberTipTextProvider)`, `setFirstLineNumberAlwaysVisible(boolean)`, `setLineNumberAlign(Paint.Align)`, `setDividerWidth(float)`, `setDividerMargin(float/float)`, `setLineNumberMarginLeft(float)`.
رنگ: `EditorColorScheme.LINE_DIVIDER`.
کار:
1. `setFirstLineNumberAlwaysVisible(true)` و `setLineNumberAlign(Paint.Align.RIGHT)` در `init()`.
2. `LineNumberTipTextProvider` را پیاده کن: وقتی روی شماره‌خط تپ می‌شود پیام (مثلاً «خط X از Y» یاBreakpoint آینده) ست کند (API در `io.github.rosemoe.sora.widget.LineNumberTipTextProvider` — مقداردهی در اینترفیس همان‌طور وابسته).
3. یک اکسترنال برای لمس شماره‌خط به‌عنوان نقطه‌ی breakpoint نگه ندار (فعلاً فقط Tip).

## تکلیف ۷ — Code Folding (بزرگ، مرحله‌ای)
وضعیت: `CodeBlock` محاسبه می‌شود ولی هیچ fold وجود ندارد. `FoldingRegion` در کتابخانه (لایبری) و `RequestManager.foldingRange(FoldingRangeRequestParams)` در LSP موجود است اما استفاده نمی‌شود.
مراحل:
1. در `LspRouter`/فرگمنت: بعد از اتصال LSP، `lspEditor.getRequestManager().foldingRange(...)` را صدا بزن (یا از `CodeBlock`های موجود استفاده کن) → لیست `FoldingRegion`.
2. حالت fold را در `IdeEditor` نگه‌دار (`Set<Integer> foldStartLines`).
3. رندر: در `onDraw` (که override شده در `IdeEditor`, خط ۵۱۷) بعد از `super.onDraw(canvas)`، اگر خط شروعِ block تا شده بود، خطوط میانی را پنهان کن — برای MVVM ساده: وقتی تا شد، متن را `replaceBlockLinesText`؟ بهتر:
   - روی `ContentChangeEvent`/`setText`، اگر block تا شده بود محدوده‌ی خطوط را hide نکن، بلکه با `HighlightTextContainer` و `clipLine` نرم نکن. چون کتابخانه ۰٫۲۴٫۶ رندر fold ندارد، MOST beautiful approach: با `editor.getRenderer()` و `clip` رسم نشود. **اگر پیاده‌سازی دشوار شد، فقط توگل نقطه + ذخیره‌ی state + نمایش «...» در شیار شماره‌ خط بساز و پنهان‌سازی متن را به مرحله‌ی بعد واگذار؛ حتماً `CodeBlock` پاس می‌دهد.**
4. توگل: از `GutterLayout` یا یک `LineSideIcon` (کتابخانه `LineSideIcon`) در شماره‌خط + گوشی‌دادن `SideIconClickEvent` (رویداد عمومی، خط ۴۰).
5. حداقل پذیرش: فلش گاتر برای block های موجود + کلیک → پنهان/نمایش با `setText`/`setInlayHintsRaw` (یا scroll). میان‌مقیاس: `Set` تا شده + رندر `^`/`v` در گاتر + توگل.

## تکلیف ۸ — منوی کانتکست سفارشی (EditorContextMenuCreator)
رابط: `io.github.rosemoe.sora.widget.component.EditorContextMenuCreator` (EditorBuiltinComponent). ثبت نشده.
کار:
1. کلاس جدید `EditorContextMenu` در `editor/.../ui/` extends آن.
2. Item های پیشنهادی: copy path, comment/uncomment (توکلیف ۱), duplicate line, cut/copy/paste/select all، «بحث کد» (فراخوانی back به اکتیویتی).
3. در `IdeEditor.init()`: `registerComponent/`replaceComponent با همان الگوی `EditorTextActionWindow` و `EditorAutoCompletion` (خط ۹۶-۹۷).

## تکلیف ۹ — LSP واقعیِ ناقص (نه دوباره‌کاری)
۱. **Document highlight**: `lspEditor.showDocumentHighlight(list)` آماده است و رنگ‌ها (`TEXT_HIGHLIGHT_BACKGROUND`/`BORDER`) خودکار. Request را از `lspEditor.getRequestManager().documentHighlight(DocumentHighlightParams)` صدا بزن و نتیجه را روی SelectionChangeEvent (در فرگمنت، خط ۲۱۴) با throttle بفرست. اگر `requestManager` قابل دسترس‌ نبود، از `lspEditor` متد عمومی معادل (در سورس 0.24.6 `RequestManager.documentHighlight` هست) استفاده کن.
۲. **هوان لمسی**: هوانِ خودکار فقط mouse-mode است. یک ژست «به‌ماندن انگشت روی کلمه» (LongPressEvent در فرگمنت خط ۲۲۵ – فعلاً فقط برای فایل‌های java/kt است؛ تعمیمش نده) یا از `editor.subscribeEvent(LongPressEvent)` گذشت: برای بقیه‌ی فایل‌ها هم هوان نزن مگر بررسی کنی. به‌جایش: وقتی متن انتخاب/کانسیوس حرکت می‌کند، position فعلی را بگیر و `lspEditor.showHover(...)` را با `requestManager.hover(...)` صدا بزن؛ بعداً استایل پنجره.
۳. **Quick-fix از دیاگنوستیک**: `ClickEvent` (رویداد عمومی) را گوش بده؛ اگر position داخل یک `DiagnosticRegion` از `editor.getDiagnostics()` بود، کداکشن را مثل `handleCodeAction()` در اکشن‌ویندو صدا بزن و نتیجه را در bottom sheet/bottomsheet نمایش بده (فقط برای خدمت‌گزارانی که `codeActionProvider` دارند).
۴. **فقط تأیید**: Signature help، LSP inlay، documentColor، LSP format — اینها خودکارند؛ دست نزن مگر تست نشان دهد نمایش نمی‌شود. اگر نشد، اول `setEnable*` را بررسی کن نه اینکه از نو بسازی.

## تکلیف ۱۰ — جابه‌جایی به خط (jump to line) + موارد خرد
- `CodeEditor.jumpToLine(int)` عمومی است. یک دیالوگ ورودی خط در `setupMenuCalltoAction` یا منوی more اضافه کن → `editor.jumpToLine(line-1)`.
- `setEditable(false)` برای view-mode: در `EditorFragment.applyReadOnly()` (خط ~178) هست؛ یک توگل «view mode» در اکشن‌ویندو زیر LSP اضافه نکن (کوچک).
- `setInputType`, `setTextScaleX`, `setTextLetterSpacing`, `setEdgeEffectColor`, `setLineSpacingExtra/Multiplier`: تنظیمات اضافه نکن مگر که خواسته شود.

## ترتیب پیشنهادی اجرا
۱ ← ۲ ← ۳ ← ۴ ← ۵ ← ۶ ← ۸ ← ۹ ← ۱۰ ← ۷ (۷ بزرگ‌ترین و آخر است).

## معیار پذیرش کلی
- همه‌ی کدها فقط در فایل‌های لیست‌شده‌ی بالا؛ هیچ ثابتی در `Constants` تکراری نشود.
- پس از هر تکلیف، `IdeEditor.onSharedPreferenceChanged` برای کلیدهای جدید به‌روز شود تا حالا درشت تنوع تنظیمات رد شود.
- کد باید با جاوا 17 (سیستم پروژه) کامپایل شود؛ سینتکس را دقیق چک کن چون گریدل در دسترس نیست.