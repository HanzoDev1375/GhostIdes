# پرامپت: پیاده‌سازی تم واکنشی به سبک تلگرام (حذف `recreate`)

> همین فایل رو صبح بده دست یک AI کدنویس (یا خودت قدم‌به‌قدم اجرا کن).
> پروژه: Ghost IDE — `thememanager` و `app` دو ماژول اصلی‌ان.

---

## هدف نهایی
عوض‌شدن تِم **بدون `Activity.recreate()`**؛ فقط رنگ ویوهای موجود عوض بشه + انیمیشن (lerp بین رنگ قدیم و جدید)، دقیقاً مثل `ActionBarLayout.setThemeAnimationValue` تلگرام.

## پیش‌زمینه (مهم، بخوان)
- رنگ‌ها الان از `ThemeManager.getTheme()` (توی ماژول `thememanager`) خونده می‌شن و `M3Theme.applyShallow/applyTopLevel/listCard` رنگ رو بر اساس **نوع View** ست می‌کنن. این رو رایگان داریم.
- `ThemeUtils.applyEditor(IdeEditor)` توی `app/.../ThemeUtils.java` ادیتور رو live رنگ می‌کنه (الان در پیش‌نمایش تم استفاده می‌شه).
- مشکل اصلی فقط اینه: وقتی تِم عوض می‌شه `BaseCompat` همه‌ی اکتیویتی‌ها رو `recreate()` می‌کنه.

---

## قدم ۰ — حذف recreate (اولین کار)
فایل: `app/src/main/java/ir/hanzodev1375/ghostide/activity/BaseCompat.java`
1. توی `onResume` بلوک `currentTheme != lastTheme → recreate()` حذف کن (خط ۱۲۸-۱۳۲).
2. متد `recreateAllActivities()` (خط ۱۴۸-۱۵۹) رو با «صدا زدن bus و اعمال تم روی همه Activity های زنده» جایگزین کن.

فایل‌هایی که الان `recreate()` رو برای عوض‌کردن تم صدا می‌زنن:
- `app/.../activity/SettingActivity.java` خط ۶۵۳
- `app/.../activity/FileManagerActivity.java` خط ۱۵۵۳
این‌ها رو به «فرستادن رویداد به bus» تبدیل کن.

## قدم ۱ — بساز `ThemeBus` (فایل جدید)
داخل ماژول `thememanager`, مسیر `thememanager/src/main/java/ir/theme/`.
محتوای لازم:
```java
public interface ThemeChangeListener {
  void onThemeChanged(GhostTheme oldTheme, GhostTheme newTheme, boolean animated);
}
```
- یک `ThemeBus` سینگلتون: `register/unregister/notifyThemeChanged`.
- لیست شنونده‌ها باید `CopyOnWriteArrayList` باشه (خطر ConcurrentModification موقع مسابقه).

## قدم ۲ — انتشار رویداد از `ThemeManager`
فایل: `thememanager/src/main/java/ir/theme/ThemeManager.java`
- در `saveTheme` ، `setThemeFromFile` ، `resetToDefault` : قبل از اعمال، `GhostTheme` قدیمی رو ذخیره کن؛ بعد از اعمال، `newTheme = getTheme()` بگیر و `ThemeBus.notifyThemeChanged(old, new, animated)` صدا بزن.
- مواظب باش notify توی **main thread** باشه (اگر از ترد دیگه صدا زده شد، `Handler(Looper.getMainLooper())`).

## قدم ۳ — گوش دادن در `BaseCompat`
در `BaseCompat`:
- در `onCreate`: `ThemeBus.register(this)` (پیاده‌سازی `ThemeChangeListener`).
- در `onDestroy`: `ThemeBus.unregister(this)`.
- در `onThemeChanged(old, new, animated)`:
  1. `M3Theme.applyTopLevel(getWindow().getDecorView())`
  2. `new ThemeUtils(new ThemeManager(this)).applyActivity(this)` (رنگ استاتوس/ناوبری بار)
  3. `applyJsonThemeBackground()`
  4. اگر `animated` → انیمیشن (قدم ۴) به‌جای اعمال مستقیم.

## قدم ۴ — انیمیشن lerp (اختیاری ولی مطلوب)
- `old` و `new` دو `GhostTheme` هستن. بین هر کلید JSON (مثل `widget.accent`, `m3.surfaceContainer`, `activity.background`) با `androidx.core.graphics.ColorUtils.blendARGB(old, new, progress)` پرش کن.
- یک `ValueAnimator` ۰→۱ با مدت ~۲۵۰ms.
- در هر فریم، یک «پالت موقت» بساز و به `M3Theme` بده.
- **تغییر لازم در `M3Theme`**: یک لایه‌ی override موقت اضافه کن (مثلاً `M3Theme.setPreview(Map<String,Integer>)` با مقدار `null` بعد از انیمیشن) که موقع انیمیشن، همه‌ی متدهای رنگی از اون پالت بخونن به‌جای `ThemeManager.getTheme()`. این تنها ادیت واقعیِ `M3Theme` است.

## قدم ۵ — رنگ‌های دستیِ خارج از M3Theme رو وصل کن
این `apply*` ها رنگ رو دستی ست می‌کنن؛ موقع رویداد تم باید دوباره صدا زده بشن (در هر صفحهای که استفاده شده):
- `ThemeUtils.applyActivity` / `applyViewPagePanel` / `apply(PowerMenu)` / `applyTabLayout` / `applySymbolBarLayout` / `applySymbolBarText` / `applyEditorStatusBar` / `applyGhostIdeEditorSearch` / `applyEditor` / `applyFab` / `applyTextView` / `applyImageView` / `setupBackgroundBlur`
- الگو: هر Activity که این‌ها رو در `onCreate` صدا می‌زنه، یک `applyTheme()` خصوصی بساز که همه‌ی این `apply*` ها رو صدا بزنه؛ و `onThemeChanged` فقط `applyTheme()` رو صدا بزنه.

---

## ترتیب پیشنهادی اجرا (به این ترتیب تست کن)
`قدم ۰ → قدم ۱ → قدم ۳ → قدم ۲ → قدم ۴ → قدم ۵`

## تست‌های موفقیت (صبح چک کن)
1. از SettingActivity تِم عوض کن → **بدون** بازگشتن صفحه به اول (اسکرول/تب/تب‌های باز از بین نرن).
2. رنگ ویوهای Material (کارد، دکمه، تب، متن) تغییر کنن.
3. رنگ ادیتور تغییر کنه (باز بودن فایل حفظ بشه).
4. انیمیشن نرم باشه و بعدش همه‌چی درست باشه.
5. هیچ کرش/پیژن روی عوض‌کردن تم نباشه.

## هشدارها
- `M3Theme.applyShallow` رو برای هر item لیست در Adapter ها با `listCard` صدا زدن **همین الان** لازم نیست — فقط اگر item رنگش دستی ست شد.
- قبل از شروع: `git log --oneline -5` و عکس بگیر از وضعیت فعلی که برگردی.