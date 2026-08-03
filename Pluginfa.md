# توسعهی پلاگین برای GhostIDE

این راهنما توضیح میده سیستم پلاگین چطور کنار هم قرار میگیره: کدوم قسمت تو کدوم ماژوله، فایل
`.gpl` چطور ساخته میشه، و کد شما چطور با IDE در حال اجرا حرف میزنه. فرض شده با توسعهی اندروید
آشنایی دارید.

## سه ماژول API

پلاگین شما هیچوقت مستقیم به `:app` وابسته نیست. فقط به این سه تا وابستهست، که هر سه به شکل jar/aar
ساده منتشر میشن:

| ماژول | برای چیه | نوع اندرویدی داره؟ |
| --- | --- | --- |
| `plugin-api` | چرخهی عمر پلاگین، رجیستری extension/service | نه — کاملاً JVM خالص |
| `ide-api` | افزودن Language Server (LSP) | نه — فقط JVM + lsp4j |
| `ide-ui-api` | دسترسی به ادیتور/فایلمنیجر، صفحات پلاگین | بله — نیاز به `Context`، `Fragment` داره |

این تفکیک به این خاطره که پلاگینی که فقط یه language server اضافه میکنه اصلاً لازم نیست به اندروید
وابسته بشه، و کلاسهای واقعی `EditorActivity`/`FileManagerActivity` تو `:app` هیچوقت مستقیم از
کد پلاگین دیده نمیشن — فقط همون اینترفیسهای کوچیک تو `ide-ui-api` رو میبینید.

## دو الگوی مشارکت

هر قابلیتی که اضافه میکنید یکی از این دو الگوئه، که تو `plugin-api` تعریف شدن:

- **Extension point** (`ExtensionPoint<T>`) — چند تا پلاگین میتونن هرکدوم یک یا چند نمونه ثبت
  کنن. برای `LspServerProvider` (ide-api) و `PluginScreen` (ide-ui-api) استفاده میشه. با
  `context.getExtensions().extensions(SomePoint)` پیداشون میکنید.
- **Service** (`ServiceKey<T>`) — دقیقاً یک نمونه که هاست منتشر میکنه، از سمت پلاگین فقط
  خواندنیه. برای `EditorHost`، `FileManagerHost`، و `Context` مخصوص خود پلاگین استفاده میشه. با
  `context.getServices().require(SomeKey)` پیداشون میکنید.

## نقطهی ورود

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

هر `Disposable` که از `register(...)` میگیرید باید به `context.registerDisposable(...)` داده بشه.
هاست موقع unload همینو صدا میزنه تا ثبتهای شما از عمر پلاگین بیشتر عمر نکنن.

## اضافه کردن Language Server

`LspServerProvider` رو (تو `ide-api`) پیاده کنید: `supports(LspServerRequest)` تعیین میکنه یه
فایل خاص رو شما پوشش میدید یا نه، `createDefinition(...)` یه `LspServerDefinition` با `Builder`ش
برمیگردونه، که `connectionFactory`ش بهصورت lazy پروسس سرور شما رو استارت میکنه و یک
`LspServerConnection` برمیگردونه (`start()`, `getInputStream()`, `getOutputStream()`,
`isClosed()`, `close()`). هیچوقت لاگهای خود سرور رو روی output stream ننویسید — فقط فریمهای
پروتکل LSP باید اونجا باشن.

## اضافه کردن یه صفحه ("Activity" پلاگین)

اندروید اجازه نمیده یه کلاس داینامیکلودشده `<activity>` جدید تو مانیفست هاست تعریف کنه. بهجاش
`PluginScreen` رو (تو `ide-ui-api`) پیاده کنید و یک `Fragment` برگردونید؛ اونو تو
`PluginUiExtensionPoints.PLUGIN_SCREEN` ثبت کنید. `PluginScreenActivity` خودِ هاست میزبانش میشه.

برای inflate کردن layout باید از context مخصوص خودتون استفاده کنید، نه context پیشفرض
fragment، وگرنه مقادیر `R` شما resolve نمیشن:

```java
Context pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
LayoutInflater inflater = LayoutInflater.from(pluginContext).cloneInContext(pluginContext);
View root = inflater.inflate(R.layout.my_screen, container, false);
```

## خواندن و نوشتن ادیتور باز

```java
EditorHost editor = context.getServices().require(IdeHostServices.EDITOR_HOST);
File open = editor.getOpenFile();
String text = editor.getEditorText();
editor.setEditorText(text + "\n// edited by plugin");
```

`FileManagerHost` هم دقیقاً همین شکل رو برای صفحهی درخت فایل داره.

## ساخت پکیج `.gpl`

`.gpl` یه ماژول عادی **android-application**ه — دقیقاً مثل هر اپ دیگهای تو Android Studio
میسازیدش، فقط هیچوقت APK خروجی رو بهصورت عادی نصب نمیکنید. `assembleRelease` فایل APK رو
میسازه؛ تغییر پسوندش به `.gpl` کل مرحلهی پکیجینگه.

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

`compileOnly` مهمه: این کلاسها همین الان داخل اپ هاست در حال اجرا وجود دارن، پلاگین شما نباید
نسخهی خودشو ازشون بسازه.

`src/main/assets/plugin.json`:

```json
{
  "id": "com.example.myplugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "entryClass": "com.example.myplugin.MyPlugin",
  "description": "یک توضیح کوتاه که تو مدیریت پلاگینها نشون داده میشه."
}
```

`entryClass` باید یه constructor عمومی بدون آرگومان داشته باشه و `GhostPlugin` رو implement کنه.

## هاست موقع لود `.gpl` شما چیکار میکنه

۱. `assets/plugin.json` رو مستقیم از داخل zip میخونه، قبل از اینکه اصلاً به کدتون دست بزنه.
۲. یه `DexClassLoader` روی خودِ فایل `.gpl` باز میکنه و `entryClass` رو نمونهسازی میکنه.
۳. `Context` هاست رو میپیچه (wrap) تا `getResources()`/`getAssets()` شما به پکیج خودتون resolve
   بشه (با همون تکنیک `AssetManager.addAssetPath` که فریمورکهای پلاگین اندرویدی سالهاست ازش
   استفاده میکنن — API عمومی نیست، پس اگه یه نسخهی آیندهی اندروید مسدودش کنه، پلاگین شما بازم
   لود میشه، فقط بدون layout/drawable سفارشی خودتون).
۴. `activate(context)` رو صدا میزنه.

موقع unload، هاست `deactivate()` شما رو صدا میزنه، هر چیزی که ثبت کردید رو dispose میکنه، و
ثبتهای extension شما رو با شناسهی پلاگین حذف میکنه — لازم نیست خودتون ردش رو بگیرید، فقط کافیه
هر `Disposable` رو ثبت کرده باشید.
