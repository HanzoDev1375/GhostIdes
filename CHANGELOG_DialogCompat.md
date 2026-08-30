# پرامپت ادامه‌ی کار

## چیزی که باید ادامه بدی

در پروژه‌ی `Ghostides` (پوشه: `/sdcard/AndroidIdeProjects/Ghostides`)، کاربر خواست:

> از `DialogCompat` به جای `MaterialAlertDialogBuilder` استفاده کن (به جز ماژول `editor`).

`DialogCompat` در `components` ماژول قرار دارد:
`ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat`

## وضعیت فعلی (تأیید شده)

### انجام‌شده (کامل)
- 39 فایل java (به جز ماژول `editor`) جایگزین شدند:
  - `MaterialAlertDialogBuilder` → `DialogCompat`
  - import قدیمی `com.google.android.material.dialog.MaterialAlertDialogBuilder` حذف شد
  - import جدید `ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat` اضافه شد (بدون تکراری، بدون blank line اضافه)
  - توزیع: app=25، jgit=9، components=3، prddownloader=2
- ماژول `editor` عمداً دست‌نخورده (هنوز 6 مورد `MaterialAlertDialogBuilder` دارد).

### نکته مهم / کار باقی‌مانده
1. **ماژول `prddownloader` (2 فایل) به `:components` وابستگی ندارد**، بنابراین با `DialogCompat` کامپایل نمی‌شود:
   - `prddownloader/src/main/java/ninja/coder/appuploader/main/ApkInstallerCompat.java`
   - `prddownloader/src/main/java/ninja/coder/appuploader/main/appupdate/UpadteAppView.java`
   - (کاربر گفت برنامه را compile نکن، اما اگر بخواهد اجرا شود باید یا `implementation project(":components")` به `prddownloader/build.gradle` اضافه شود، یا این 2 فایل به حالت قبل برگردند. تصمیم با کاربر است — بپرس.)

2. تغییراتِ خودِ کاربر که **نباید دست بزنی** (از قبل در working tree بودند):
   - `app/src/main/res/**` (strings.xml ها، view_floating_action_toolbar.xml)
   - `gradle.properties`
   - `components/.../sheet/customitemsheet/ui/GlassCompat.java`
   - `editor/src/main/java/.../setting/PreferencesUtils.java`
   - فایل untracked: `components/.../sheet/customitemsheet/ui/FabGlass.java`

## ساختار کلاس DialogCompat
- `DialogCompat extends LiquidGlassDialogBuilderJava extends GlassCompat extends LiquidGlassView`
- API مثل AlertDialog.Builder: سازنده `DialogCompat(Context)` + `.setTitle/.setMessage/.setPositiveButton/.setNegativeButton/.show()/...`
- برای context می‌توان Context، Activity، یا Fragment (با requireContext/requireActivity) داد.

## هشدار (اشتباهات قبلی که نباید تکرار شوند)
- `GlassCompat` یک **View** است (طراحی برای پس‌زمینه‌ی شیشه‌ای)، نه Dialog builder. برای دیالوگ فقط از `DialogCompat` استفاده کن.
- قبل از هر تغییر بزرگ، `git status` بگیر و فقط فایل‌های مربوط به همان تسک را تغییر بده؛ به فایل‌های خودِ کاربر دست نزن.
- تغییرات را commit نکن مگر کاربر صریحاً بگوید.
- اگر خواستی همه‌ی تغییرات این تسک را revert کنی، از `git restore <file>` روی فایل‌های java زیر استفاده کن (لیست کامل در ادامه). کاربر از git استفاده می‌کند.

## لیست فایل‌های تغییرکرده (برای revert احتمالی)
`git status --short` الان 51 فایل modified نشان می‌دهد (شامل فایل‌های خودِ کاربر). فقط فایل‌های java این تسک را revert کن.
