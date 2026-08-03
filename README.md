# نحوه‌ی ادغام

این زیپ فقط شامل فایل‌های جدید/تغییرکرده‌ست، نه کل پروژه. مسیرها دقیقاً مطابق ریشه‌ی پروژه‌ی
Ghostide هستن؛ کافیه محتویاتش رو روی پروژه‌ی خودت extract کنی.

## فایل‌های جایگزین‌شونده (۴ تا)

- `settings.gradle` — سه ماژول جدید include شدن: `plugin-api`, `ide-api`, `ide-ui-api`
- `app/build.gradle` — دو خط `implementation project(...)` اضافه شده
- `editor/build.gradle` — یک خط `implementation project(":ide-api")` اضافه شده
- `app/src/main/AndroidManifest.xml` — یک `<activity>` جدید (`PluginScreenActivity`) اضافه شده

## پوشه/فایل‌های کاملاً جدید

- `plugin-api/` — ماژول java-library خالص: چرخه‌ی عمر پلاگین، رجیستری extension/service
- `ide-api/` — ماژول java-library، وابسته به plugin-api + lsp4j: افزودن Language Server
- `ide-ui-api/` — ماژول android-library، وابسته به plugin-api: دسترسی به ادیتور/فایل‌منیجر،
  صفحات پلاگین (`EditorHost`, `FileManagerHost`, `PluginScreen`)
- `app/.../activity/PluginScreenActivity.java` + `app/.../res/layout/activity_plugin_screen.xml`
  — Activity عمومی که صفحات پلاگین‌ها رو میزبانی می‌کنه
- `app/.../plugin/gpl/` — بارگذاری واقعی فایل `.gpl` (manifest، DexClassLoader، منابع، unload)؛
  کاملاً جدا از سیستم قدیمی `plugin/` (که دست‌نخورده باقی مونده)
- `editor/.../codeeditors/langs/lsp/LspServerConnectionStreamAdapter.java` +
  `LspExtensionBridge.java` — پل بین `LspServerProvider` جدید و LSP سورا-ادیتور موجود
- `.github/workflows/android-ci.yml` — **جایگزین فایل CI واقعی شما بشه** (هر اسمی که تو ریپو
  داره)؛ همون محتوای Android CI شماست بعلاوه‌ی جاب `build-plugin-sdk` که jar های `plugin-api`/
  `ide-api` و aar ماژول `ide-ui-api` رو می‌سازه و آپلود می‌کنه (و روی release بهش attach می‌کنه)
- `Plugin.md` / `Pluginfa.md` — راهنمای توسعه‌ی پلاگین (انگلیسی/فارسی)

بعد از ادغام: `./gradlew :plugin-api:compileJava :ide-api:compileJava :ide-ui-api:compileDebugJavaWithJavac :app:compileDebugJavaWithJavac`
رو اجرا کن. توی sandbox من نه JDK کامل هست نه شبکه، پس نتونستم خودم بیلد بگیرم — همه‌چی رو دستی
مرور کردم (balance آکولاد/پکیج، امضای متدهای StreamConnectionProvider که از `ProotStdioConnectionProvider`
موجودتون گرفتم، و minSdk 26 برای خوندن فایل که با `readAllBytes` مشکل داشت رو با یه حلقه‌ی دستی
عوض کردم) ولی حتماً خودت کامپایل رو تایید کن.

## چیزی که هنوز وصل نشده

`LspRouter` و `JavaServer` هنوز دست‌نخورده‌ان. `LspExtensionBridge.findProvider(...)` و
`toSoraDefinition(...)` آماده‌ان ولی جایی صداشون نمی‌زنه. قدم بعدی: یک `LspServerProvider` برای
جاوا بسازیم (دور همون منطق `JavaServer` که از قبل کار می‌کنه) و تو `LspRouter` قبل از switch
هاردکدش، اول از رجیستری بپرسیم. سیستم پلاگین قدیمی (`plugin/Plugin.java` و بقیه) هم هنوز جداست؛
مهاجرتش به `.gpl` قدم بعدیه.
