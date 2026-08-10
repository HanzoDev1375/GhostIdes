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

## خواندن و نوشتن ادیتور باز

```java
EditorHost editor = context.getServices().require(IdeHostServices.EDITOR_HOST);
File open = editor.getOpenFile();
String text = editor.getEditorText();
editor.setEditorText(text + "\n// edited by plugin");
```

`FileManagerHost` هم دقیقاً همین شکل رو برای صفحه ی درخت فایل داره.

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
