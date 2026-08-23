# توسعه ی پلاگین برای GhostIDE

این راهنما توضیح می ده سیستم پلاگین چطور کنار هم قرار می گیره: کدوم قسمت تو کدوم ماژوله، فایل
`.gpl` چطور ساخته می شه، و کد شما چطور با IDE در حال اجرا حرف می زنه. فرض شده با توسعه ی اندروید
آشنایی دارید.

## سه ماژول API

پلاگین شما هیچ وقت مستقیم به `:app` وابسته نیست. فقط به این سه تا وابسته ست، که هر سه به شکل jar/aar
ساده منتشر می شن:

| ماژول | برای چیه | نوع اندرویدی داره؟ |
| --- | --- | --- |
| `plugin-api` | چرخه ی عمر پلاگین، رجیستری extension/service | نه — کاملاً JVM خالص |
| `ide-api` | افزودن Language Server (LSP) | نه — فقط JVM + lsp4j |
| `ide-ui-api` | دسترسی به ادیتور/فایل منیجر، صفحات پلاگین | بله — نیاز به `Context`، `Fragment` داره |

این تفکیک به این خاطره که پلاگینی که فقط یه language server اضافه می کنه اصلاً لازم نیست به اندروید
وابسته بشه، و کلاس های واقعی `EditorActivity`/`FileManagerActivity` تو `:app` هیچ وقت مستقیم از
کد پلاگین دیده نمی شن — فقط همون اینترفیس های کوچیک تو `ide-ui-api` رو می بینید.

## دو الگوی مشارکت

هر قابلیتی که اضافه می کنید یکی از این دو الگوئه، که تو `plugin-api` تعریف شدن:

- **Extension point** (`ExtensionPoint<T>`) — چند تا پلاگین می تونن هرکدوم یک یا چند نمونه ثبت
  کنن. برای `LspServerProvider` (ide-api) و `PluginScreen` (ide-ui-api) استفاده می شه. با
  `context.getExtensions().extensions(SomePoint)` پیداشون می کنید.
- **Service** (`ServiceKey<T>`) — دقیقاً یک نمونه که هاست منتشر می کنه، از سمت پلاگین فقط
  خواندنیه. برای `EditorHost`، `FileManagerHost`، و `Context` مخصوص خود پلاگین استفاده می شه. با
  `context.getServices().require(SomeKey)` پیداشون می کنید.

## نقطه ی ورود

```java
public final class MyPlugin implements GhostPlugin {
  @Override
  public void activate(PluginContext context) {
    Disposable registration = context.getExtensions()
        .register(EditorExtensionPoints.LSP_SERVER_PROVIDER, new MyLspProvider());
    context.registerDisposable(registration);
  }
}
```

هر `Disposable` که از `register(...)` می گیرید باید به `context.registerDisposable(...)` داده بشه.
هاست موقع unload همینو صدا می زنه تا ثبت های شما از عمر پلاگین بیشتر عمر نکنن.

## اضافه کردن Language Server

`LspServerProvider` رو (تو `ide-api`) پیاده کنید: `supports(LspServerRequest)` تعیین می کنه یه
فایل خاص رو شما پوشش می دید یا نه، `createDefinition(...)` یه `LspServerDefinition` با `Builder`ش
برمی گردونه، که `connectionFactory`ش به صورت lazy پروسس سرور شما رو استارت می کنه و یک
`LspServerConnection` برمی گردونه (`start()`, `getInputStream()`, `getOutputStream()`,
`isClosed()`, `close()`). هیچ وقت لاگ های خود سرور رو روی output stream ننویسید — فقط فریم های
پروتکل LSP باید اونجا باشن.

## اضافه کردن یه صفحه ("Activity" پلاگین)

اندروید اجازه نمی ده یه کلاس داینامیک لود شده `<activity>` جدید تو مانیفست هاست تعریف کنه. به جاش
`PluginScreen` رو (تو `ide-ui-api`) پیاده کنید و یک `Fragment` برگردونید؛ اونو تو
`PluginUiExtensionPoints.PLUGIN_SCREEN` ثبت کنید. `PluginScreenActivity` خودِ هاست میزبانش می شه.

برای inflate کردن layout باید از context مخصوص خودتون استفاده کنید، نه context پیش فرض
fragment، وگرنه مقادیر `R` شما resolve نمی شن:

```java
Context pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
LayoutInflater inflater = LayoutInflater.from(pluginContext).cloneInContext(pluginContext);
View root = inflater.inflate(R.layout.my_screen, container, false);
```

## اضافه کردن پنل داخل صفحه (به سبک VS Code)

`PluginScreen` کل صفحه رو می گیره. برای این که UI رو *کنار* یه صفحه ی در حال اجرا بذارید — یه
سایدبار چت، یه inspector، یه پیش نمایش زنده — اینترفیس `EditorPanel` رو (تو `ide-ui-api`) پیاده
کنید و تو `PluginUiExtensionPoints.EDITOR_PANEL` ثبت کنید. هاست اونو به صورت side sheet داخل
صفحه ی ادیتور نشون می ده، و به ازای هر پنل ثبت شده یه دکمه تو نوار ابزار اضافه می کنه. API قبلی
`PluginScreen` بدون تغییر کار می کنه.

```java
public final class ChatPanel implements EditorPanel {
  private final Context pluginContext;

  ChatPanel(PluginContext context) {
    pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
  }

  @Override
  public String getId() {
    return "com.example.myplugin.chat";
  }

  @Override
  public String getTitle() {
    return "Chat";
  }

  @Override
  public View createView() {
    // همون قانون inflate با context مخصوص خودتون، تا R.layout.chat resolve بشه.
    return LayoutInflater.from(pluginContext).cloneInContext(pluginContext)
        .inflate(R.layout.chat_panel, null, false);
  }
}

// داخل activate():
context.registerDisposable(
    context.getExtensions().register(PluginUiExtensionPoints.EDITOR_PANEL, new ChatPanel(context)));
```

هاست یک بار `createView()` رو صدا می زنه و بعد همون `View` برگشتی رو دوباره استفاده می کنه؛ پس
view رو lazy بسازید و state پنل رو داخل خودش نگه دارید. این متد روی ترد UI صدا زده می شه.

## خواندن و نوشتن ادیتور باز

```java
EditorHost editor = context.getServices().require(IdeHostServices.EDITOR_HOST);
File open = editor.getOpenFile();
String text = editor.getEditorText();
editor.setEditorText(text + "\n// edited by plugin");
```

`FileManagerHost` هم دقیقاً همین شکل رو برای صفحه ی درخت فایل داره.

همچنین می‌تونید به ویجت خام ادیتور پشت تب فعلی هم دسترسی بگیرید. `IdeEditor` جزئی از این API
نیست، پس به شکل `Object` برگردونده می‌شه و فقط یک نقطه ی دسترسی اختیاریه — اگه خواستید cast کنید
ماژول ادیتور هاست رو به‌صورت `compileOnly` به `.gpl` خودتون اضافه کنید:

```java
Object raw = editorHost.getEditor();
if (raw instanceof IdeEditor ide) {   // compileOnly ':editor' برای cast
  ide.getCurrentFilePath();
  ide.getLspEditor();
}
```

## اجرای کد از داخل پلاگین (Code Runner)

هاست یک سرویس `CodeRunnerHost` رو زیر `IdeHostServices.CODE_RUNNER_HOST` منتشر می‌کنه. این سرویس
دقیقاً مثل دکمه ی اجرا (FAB) ادیتور، دستورهای شل و فایل‌ها رو اجرا می‌کنه — خروجی توی ترمینال IDE
نشون داده می‌شه، یا به‌صورت bottom sheet یا تمام‌صفحه:

```java
CodeRunnerHost runner = context.getServices().require(IdeHostServices.CODE_RUNNER_HOST);

runner.runShell("python3 main.py", true);        // هر دستور شل دلخواه، bottom sheet
runner.runCurrentFile(false);                     // فایل باز در ادیتور، تمام‌صفحه
runner.runFile("/sdcard/Project/main.py", true);  // هر فایلی با مسیر
boolean ok = runner.isSupported("/sdcard/a.py");  // آیا زبان این فایل شناخته‌شده‌ست؟
```

`runCurrentFile()` خودش مسیر رو نمی‌خونه — از پنل‌های ادیتور ثبت‌شده `EditorPanel#getLastPath()`
می‌پرسه (اولین مقدار غیرخالی برنده‌ست) و فقط وقتی هیچ پنلی جواب نده به فایل بازِ فعلی ادیتور
برمی‌گرده. با override کردن `getLastPath()` توی پنل‌تون می‌تونید تعیین کنید کدوم فایل اجرا بشه؛
هاست به‌صورت پیش‌فرض فایل بازِ فعلی رو برمی‌گردونه.

یک پلاگین LSP هم بدون نوشتن کد کلاینت از همین runner استفاده می‌کنه: کافیه زبان‌سرور شما توی یک
code action یک `command` از این دستورهای داخلی برگردونه تا ادیتور خودش اونو اجرا کنه:

| command | arguments |
| --- | --- |
| `ghostide.runShell` | `["echo hi", true]` — متن شل، `asBottomSheet` اختیاری |
| `ghostide.runFile` | `["/path/file.py", true]` — مسیر فایل، `asBottomSheet` اختیاری |
| `ghostide.runCurrentFile` | `[true]` — `asBottomSheet` اختیاری |

### اجرای headless با گرفتن خروجی

وقتی خروجی یه دستور رو برنامه‌نویسی‌شده می‌خواید (نه نمایش تو ترمینال)، از `exec()` استفاده کنید.
این متد بدون هیچ UI ای (`sh -c`) اجرا می‌کنه و تا تموم شدن پروسس بلاک می‌شه — فقط از ترد
پس‌زمینه صدا بزنید:

```java
CodeRunnerHost.ExecResult result =
    runner.exec("git -C /sdcard/Project status --porcelain", null);
if (result.success()) {
  String listing = result.output();
}
```

با پاس دادن یه `Consumer<String>` به عنوان آرگومان دوم، هر خط از stdout/stderr (ادغام‌شده) همون
لحظه به دستتون می‌رسه. توجه: این اجرا تو محیط شل خود اپه؛ باینری‌های داخل rootfs پرووت باید از
`ProotProcessLauncher` برن.

## گوش دادن به رویدادهای IDE

یه `FileEventListener` پیاده کنید و تو `IdeEvents.FILE_EVENT` (تو `ide-ui-api`) ثبتش کنید تا
رویدادهای چرخه‌ی عمر فایل در کل IDE رو ببینید:

```java
context.registerDisposable(
    context.getExtensions().register(
        IdeEvents.FILE_EVENT,
        event -> {
          switch (event.type()) {
            case SAVED -> lint(event.path());
            case RENAMED -> updateIndex(event.previousPath(), event.path());
            case OPENED, CLOSED, DELETED -> {}
          }
        }));
```

انواع رویداد: `OPENED`، `SAVED`، `CLOSED`، `DELETED`، `RENAMED`. مسیرها absolute هستن؛ برای
`RENAMED` مقدار `path()` مسیر جدید و `previousPath()` مسیر قبلیه. لیسنر ممکنه روی هر تردی صدا
زده بشه (ذخیره و حذف روی ترد پس‌زمینه اتفاق می‌افته) — قبل از کار با UI به ترد اصلی سوییچ کنید.

## نشون دادن بازخورد به کاربر (toast / ورودی / تأیید)

هاست یه سرویس `UiFeedbackHost` زیر `IdeHostServices.UI_FEEDBACK` منتشر می‌کنه. همه‌ی متدهاش از
هر تردی امنن؛ اگر Activity ای برای میزبانی دیالوگ نباشه، callback ها با `null`/`false` صدا زده
می شن:

```java
UiFeedbackHost ui = context.getServices().require(IdeHostServices.UI_FEEDBACK);

ui.toast("۴۲ فایل ایندکس شد");

ui.confirm("فرمت همه؟", "این کار ۱۲ فایل رو بازنویسی می‌کنه.", confirmed -> {
  if (confirmed) formatAll();
});

ui.promptInput("اسم پروژه", "my-app", name -> {
  if (name == null) return; // کنسل شد
  createProject(name);
});
```

## فضای ذخیره‌سازی دائمی مخصوص پلاگین

هر پلاگین یه key-value store ایزوله‌ی خودش داره که زیر `CoreServices.PLUGIN_STORAGE` منتشر می‌شه
(تعریفش تو `plugin-api`ـه، پس پلاگین‌های LSP خالص هم می‌تونن استفاده کنن). داده بعد از ری‌استارت و
reload پلاگین می‌مونه؛ مقادیر رو کوچیک نگه دارید و برای حجم زیاد از دایرکتوری خصوصی خودتون استفاده
کنید:

```java
PluginStorage storage = context.getServices().require(CoreServices.PLUGIN_STORAGE);

int runs = storage.getInt("runCount", 0);
storage.putInt("runCount", runs + 1);
storage.putString("lastPath", "/sdcard/Project/main.py");
```

## ثبت Command

اینترفیس `PluginCommand` رو (تو `plugin-api`) پیاده کنید و تو `CoreExtensionPoints.PLUGIN_COMMAND`
ثبتش کنید تا هاست بتونه اکشن شما رو لیست و اجرا کنه (مثلاً از command palette):

```java
public final class FormatCommand implements PluginCommand {
  @Override public String getId() { return "com.example.myplugin.format"; }
  @Override public String getTitle() { return "Format project"; }
  @Override public void execute() { /* ممکنه خارج از ترد اصلی صدا زده بشه */ }
}

// داخل activate():
context.registerDisposable(
    context.getExtensions().register(CoreExtensionPoints.PLUGIN_COMMAND, new FormatCommand()));
```

شناسه‌ها باید یکتا و به سبک reverse-domain باشن.

## هندل کردن اکشن‌های LSP خودتون (دسترسی اختیاری به ادیتور)

دستورهایی که `ghostide.*` نیستن همچنان از طریق `workspace/executeCommand` به سرور شما فرستاده
می‌شن. برای این که یک command رو خودِ کلاینت هندل کنه — و ادیتور خام هم در دسترستون باشه —
اینترفیس `EditorActionHandler` (`ide-ui-api`) رو پیاده کنید و زیر
`PluginUiExtensionPoints.EDITOR_ACTION_HANDLER` ثبتش کنید. آرگومان `editor` یک خروجی اختیاریه:
همون `IdeEditor` هاست پشت اکشن، یا `null`. `IdeEditor` جزئی از ماژول‌های API نیست؛ پس برای cast
ماژول ادیتور هاست رو به‌صورت `compileOnly` اضافه کنید:

```java
public final class RunMyPluginAction implements EditorActionHandler {
  @Override public String getCommandId() { return "com.example.myplugin.run"; }

  @Override public boolean execute(Object editor, String command, List<Object> arguments) {
    if (editor instanceof IdeEditor ide) {       // compileOnly ':editor' برای cast
      String file = ide.getCurrentFilePath();
      // ...
    }
    return true; // هندل شد — به سرور زبان forward نمی‌شه
  }
}

// داخل activate():
context.registerDisposable(
    context.getExtensions().register(PluginUiExtensionPoints.EDITOR_ACTION_HANDLER,
        new RunMyPluginAction()));
```

حواستون باشه هندلر ممکنه روی ترد پس‌زمینه صدا زده بشه؛ قبل از دست زدن به ادیتور به ترد UI برید.

## مشارکت در آیکون فایل‌ها

یه `FileIconContributor` (از `ide-ui-api`) پیاده کنید و در
`PluginUiExtensionPoints.FILE_ICON_CONTRIBUTOR` ثبتش کنید. برای هر آیکونی که تو فایل منیجر، تب‌های
ادیتور، تاریخچه و بوکمارک‌ها نمایش داده می شه، *قبل از* مجموعه پیش‌فرض `file_icons.json` از شما
پرسیده می شه؛ برای مسیرهایی که هندل نمی کنید `null` برگردونید تا contributor بعدی (یا مجموعه
پیش‌فرض) جواب بده.

- یه URI کامل بر گردونید (`file://`، `content://`، ...) تا آرت ورک داخل `.gpl` خودتون لود بشه.
  موقع `activate()` یک بار فایل ها رو از asset به حافظه خصوصی کپی کنید — Glide از طریق
  `android_asset` نمی تونه asset پلاگین رو باز کنه.
- فقط یه اسم بر گردونید (مثل `file_type_kotlin`) تا از مجموعه داخلی `vscode_icons` استفاده بشه.
- برای مپ کردن گروهی آیکون ها، یه فایل JSON با همون ساختار `data/file_icons.json` (`asset_dir`,
  `extensions`, `filenames`, `folders`, `defaults`) به همراه SVG های خودتون داخل پلاگین بذارید و
  `JsonFileIconContributor` آماده رو ثبت کنید. این کلاس فقط SVG هایی که واقعا داخل پلاگین هستند
  رو استخراج و به صورت `file://` سرو می کنه، و اسم های ناشناخته رو بدون تغییر به مجموعه داخلی
  می سپاره:

```java
public void activate(PluginContext context) {
  Context pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
  context.registerDisposable(context.getExtensions().register(
      PluginUiExtensionPoints.FILE_ICON_CONTRIBUTOR,
      new JsonFileIconContributor(pluginContext, "myicons.json")));
}
```

چند تا افزونه آیکون می تونن همزمان فعال باشن: به ترتیب اولویت نزولی پرسیده می شن و اولین جواب
non-null برنده است. با unload شدن پلاگین، contribution هاش خودکار حذف می شن.

## ساخت پکیج `.gpl`

`.gpl` یه ماژول عادی **android-application**ه — دقیقاً مثل هر اپ دیگه ای تو Android Studio
می سازیدش، فقط هیچ وقت APK خروجی رو به صورت عادی نصب نمی کنید. `assembleRelease` فایل APK رو
می سازه؛ تغییر پسوندش به `.gpl` کل مرحله ی پکیجینگه.

`build.gradle`:

```groovy
plugins {
  id 'com.android.application'
}

android {
  namespace 'com.example.myplugin'
  defaultConfig {
    applicationId 'com.example.myplugin'
    minSdk 26
  }
}

dependencies {
  compileOnly project(':plugin-api')   // یا jar منتشرشده
  compileOnly project(':ide-api')
  compileOnly project(':ide-ui-api')
}
```

`compileOnly` مهمه: این کلاس ها همین الان داخل اپ هاست در حال اجرا وجود دارن، پلاگین شما نباید
نسخه ی خودشو ازشون بسازه.

`src/main/assets/plugin.json`:

```json
{
  "id": "com.example.myplugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "entryClass": "com.example.myplugin.MyPlugin",
  "description": "یک توضیح کوتاه که تو مدیریت پلاگین ها نشون داده می شه."
}
```

`entryClass` باید یه constructor عمومی بدون آرگومان داشته باشه و `GhostPlugin` رو implement کنه.

## هاست موقع لود `.gpl` شما چیکار می کنه

۱. `assets/plugin.json` رو مستقیم از داخل zip می خونه، قبل از این که اصلاً به کدتون دست بزنه.
۲. یه `DexClassLoader` روی خودِ فایل `.gpl` باز می کنه و `entryClass` رو نمونه سازی می کنه.
۳. `Context` هاست رو می پیچه (wrap) تا `getResources()`/`getAssets()` شما به پکیج خودتون resolve
   بشه (با همون تکنیک `AssetManager.addAssetPath` که فریمورک های پلاگین اندرویدی سال هاست ازش
   استفاده می کنن — API عمومی نیست، پس اگه یه نسخه ی آینده ی اندروید مسدودش کنه، پلاگین شما بازم
   لود می شه، فقط بدون layout/drawable سفارشی خودتون).
۴. `activate(context)` رو صدا می زنه.

موقع unload، هاست `deactivate()` شما رو صدا می زنه، هر چیزی که ثبت کردید رو dispose می کنه، و
ثبت های extension شما رو با شناسه ی پلاگین حذف می کنه — لازم نیست خودتون ردش رو بگیرید، فقط کافیه
هر `Disposable` رو ثبت کرده باشید.
