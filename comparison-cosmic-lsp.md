# مقایسه ماژول LSP: پروژه GhostIdes در برابر Cosmic IDE

## خلاصه کلی

هر دو پروژه از کتابخانه Sora LSP (همان هسته `io.github.rosemoe.sora.lsp`) استفاده می‌کنند و معماری «پلاگین/extension» برای سرویس‌دهنده‌های LSP دارند. اما تفاوت‌های اساسی در زبان پیاده‌سازی، نحوه‌ی اتصال، و نحوه‌ی مدیریت قابلیت‌های سرویس‌دهنده وجود دارد.

| ویژگی | **GhostIdes (پروژه شما)** | **Cosmic IDE** |
|---|---|---|
| زبان پیاده‌سازی | **Java** (با چند فایل Kotlin) | **Kotlin** |
| نقطه‌ی گسترش | `LspServerProvider` / `LspServerRequest` / `LspServerDefinition` / `LspServerConnection` در `ide-api` | همان مدل (اینترفیس + data class) در `ide-api` |
| مسیر اصلی اتصال | `LspRouter` (سرویس‌های built-in) + `LspExtensionBridge` (برای extension) | `EditorLanguageRouter` + `LspEditorLanguageProvider` |
| اجرای باینری سرویس‌دهنده | داخل **proot Debian rootfs** (`ProotStdioConnectionProvider`) | مستقیم یا با `ProcessExecutor` |
| سرویس‌دهنده‌های داخلی | کلاس پایه `LspContentImpl` + سرویس‌های جدا (`JavaServer`, `ClangdServer`, ...) | هر زبان یک `*LanguageProvider` ثابت |
| گرامر/هیچ | سرویس‌های own با توکنایزر/ANTLR | گرامر **TextMate** با کش |

---

## ۱. نقاط مشترک معماری

- هر دو از `LspProject`، `LspEditor`، `LspLanguage` و `CustomLanguageServerDefinition` سورا استفاده می‌کنند.
- هر دو `LspServerProvider` را در قالب extension point `LSP_SERVER_PROVIDER` ثبت می‌کنند و فقط یک سرویس‌دهنده به‌ازای هر extension فعال است.
- هر دو اتصال را روی main thread با `Handler` انجام می‌دهند و از `connectWithTimeoutBlocking` استفاده می‌کنند.
- هر دو برای جلوگیری از ثبت تکراری server-definition به‌ازای هر پروژه، نگاشت بر پایه‌ی `projectRoot` دارند.
- هر دو برای ساخت `ClientCapabilities` و `RootUri` از هوک Pine روی `LanguageServerWrapper.getInitParams` استفاده می‌کنند (نقطه‌ی مشترک).

---

## ۲. تفاوت‌های کلیدی

### ۲.۱ سیستم پیش‌فرض (fallback) در برابر یکپارچه
- **شما:** دو مسیر موازی دارید — ابتدا `LspExtensionBridge.findProvider` صدا زده می‌شود و اگر سرویس‌دهنده‌ای از پلاگین نبود به `switch` روی `Lang` و سرویس‌های hardcoded (`JavaServer.INSTANCE`, `PylspServer`, ...) برمی‌گردد. یعنی **دو مکانیزم تکراری** برای اتصال.
- **Cosmic:** یک مسیر واحد — `EditorLanguageRouter` همه‌ی `LANGUAGE_PROVIDER`ها را طبق `priority` امتحان می‌کند؛ `LspEditorLanguageProvider` (پریوریتی ۲۰۰) خودش سرویس‌دهنده‌های LSP را رزولوشن می‌کند. هیچ switch سخت‌کد شده‌ای نیست.

### ۲.۲ اجرای command در تکمیل خودکار (Completion)
- **شما:** از `LspCompletionItem` استاندارد سورا استفاده می‌کنید.
- **Cosmic:** کلاس `CommandAwareLspCompletionItem` دارد که `workSpaceExecuteCommand` را بعد از پذیرش آیتم اجرا می‌کند (رفع باگ Sora 0.24.6 که command را اجرا نمی‌کند).

### ۲.۳ ثبت قابلیت‌های کلاینت (ClientCapabilities / InitParams)
- **شما:** با `LspInitParamsHook` و کتابخانه‌ی **Pine** روی متد خصوصی `LanguageServerWrapper.getInitParams` هوک می‌زنید تا `ClientCapabilities` (codeAction، completion، rename و...) سفارشی بسازید.
- **Cosmic:** هم دقیقاً همین کار را می‌کند — در `App.kt` با `HookManager`/Pine روی همان `getInitParams` هوک می‌زند و `ClientCapabilities` بسیار مشابهی (با `CompletionItemCapabilities` غنی‌تر مثل `deprecatedSupport`, `labelDetailsSupport`, `commitCharactersSupport`, `insertReplaceSupport`, `preselectSupport`) می‌سازد. پس اینجا **مشترک** هستند.

> **نکته‌ی مهم:** Cosmic علاوه بر `getInitParams`، **سه هوک Pine دیگر** هم ثبت می‌کند که در پروژه‌ی شما وجود ندارد:
> 1. **`GenericEndpoint.request`** برای `window/showDocument` → `handleLspShowDocument` (پرش به موقعیت در ادیتور).
> 2. **`WorkSpaceApplyEditEvent.handle`** → تبدیل `WorkspaceEdit` به `ApplyWorkspaceEditParams`.
> 3. **`WorkSpaceApplyEditEvent.applyChanges`** → اعمال واقعی text edits روی ادیتور (پشتیبانی کامل از rename/refactor که سورا به‌صورت پیش‌فرض پشتیبانی نمی‌کند).

### ۲.۴ اتصال Formatter / Wrapper Language
- **شما:** در `JavaServer.onEditorCreated` به‌صورت دستی `JavaLanguage` را set و `lang.setFormatter(...)` را وصل می‌کند؛ در `LspExtensionBridge.connectFile` از `EditorLanguageFactory.create` برای wrapper استفاده می‌کند (اما یادداشت خودتان می‌گوید این مسیر **فرمتر را وصل نمی‌کند**).
- **Cosmic:** یک `EditorFormatterRouter` (با `AsyncFormatter` و extension registry) + گرامر TextMate برای هر زبان — یکپارچه‌تر.

### ۲.۵ پشتیبانی از گرامر
- **شما:** از زبان‌های مبتنی بر ANTLR/توکنایزر (پوشه `langs/`) به‌عنوان wrapper استفاده می‌کنید و hover-markdown را با `LspHoverHighlighter` (switch بزرگ ۳۰+ زبان) مدیریت می‌کنید.
- **Cosmic:** گرامر **TextMate** با کش ۷ روزه و پشتیبانی از http(s)/content/file و تشخیص خودکار JSON/XML/YAML.

### ۲.۶ لاگ‌برداری پیام‌های LSP (Tracing)
- **شما:** stderr را با یک thread جدا (`stderrPump`) می‌خوانید و به logcat می‌ریزید (در `ProotStdioConnectionProvider`).
- **Cosmic:** `LspMessageTracingInputStream` دارد که **فریم‌های JSON-RPC** روی stdout را پارس می‌کند (پارس هدر `Content-Length`) و به `LspLogStore` (StateFlow، قابل مشاهده در UI) ثبت می‌کند — خیلی پیشرفته‌تر برای دیباگ.

### ۲.۷ درخواست‌های پیشرفته
- **شما:** bootstrap breadcrumbs (`fetchBreadcrumbs` با `documentSymbol` و تایم‌اوت ۲ ثانیه).
- **Cosmic:** `handleLspShowDocument` (پرش به موقعیت در ادیتور از side-effect سرویس‌دهنده) و تنظیم خودکار `Timeout` سورا (حداقل ۱۰ ثانیه).

---

## ۳. نقاط قوت پروژه شما نسبت به Cosmic

- **زبان‌های داخلی بیشتر:** شما ده‌ها `Language` مبتنی بر ANTLR/توکنایزر دارید (Java, Kotlin, Rust, Go, C, C++, C#, Python, Ruby, PHP, Dart, Lua, SQL, Shell و حتی Elixir/Haskell/Nim/Solidity) و با extension پوشش می‌دهید — Cosmic فقط ۳-۴ زبان دارد.
- **پروژه‌ی Invisible برای jdtls:** `buildInvisibleProjectInitOptions` + `AndroidClasspathResolver` برای پیدا کردن source-roots و library-jars — در Cosmic وجود ندارد.
- **تکامل proot:** `ProotStdioConnectionProvider` مدیریت کامل rootfs، `LD_LIBRARY_PATH`، `PROOT_TMP_DIR` و پمپ جداگانه stderr — مناسب‌تر برای محیط Android/debian نسبت به رویکرد ساده Cosmic.

---

## ۴. پیشنهادهای احتمالی برای بهبود (اختیاری)

1. یکپارچه‌سازی مسیرهای `LspRouter` و `LspExtensionBridge` برای حذف منطق تکراری اتصال.
2. افزودن کلاسی شبیه `CommandAwareLspCompletionItem` برای اجرای command بعد از completion.
3. افزودن هوک‌های `WorkSpaceApplyEditEvent` (handle/applyChanges) و `window/showDocument` برای پشتیبانی کامل از rename/refactor، مانند Cosmic در `App.kt`.
4. افزودن گرامر TextMate (اختیاری) یا حداقل جداسازی واضح‌تر wrapper/فرمتر در پلاگین‌های LSP.
