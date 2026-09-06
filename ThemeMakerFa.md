# تم سازی — گوست آیدی 👻

این راهنما **همهچیز** را برای ساخت تم اختصاصی گوست آیدی توضیح میدهد — حتی اگر **هیچ تجربهی برنامهنویسی** نداشته باشید و یکبار هم JSON ندیده باشید. اگر یک هوش مصنوعی یا دوستتان این صفحه را بخواند، بدون هیچ توضیح اضافهای از تو میفهمد *کدام تنظیم مربوط به کدام رنگ است*.

> این صفحه را از اول تا آخر بخوانید. تک تک فیلدها، کلید JSON و معنی سادهی هر یک در زیر آمده است.

---

## ۱. فایل تم چیست؟

تم گوست آیدی یک **فایل متنی** با پسوند **`.gth`** است. داخل آن یک آبجکت **JSON** است — یعنی فقط یک فهرست ساختاریافته از نام رنگها و مقدارشان.

- میتوانید با **ویرایشگر تم** داخلی (انتخاب رنگ بهصورت تصویری) بازش کنید — نه نیاز به کدنویسی دارد.
- یا بهصورت متن ساده بازش کنید و رنگها را با دست / با کمک هوش مصنوعی عوض کنید.

هر رنگ بهصورت هگز نوشته میشود مثل `"#61afef"` (یک `#` بههمراه ۸ رقم هگز: `#RRGGBBAA` — قرمز، سبز، آبی، آلفا/شفافیت).

---

## ۲. شروع سریع (ساخت اولین تم در ۱ دقیقه)

۱. **گوست آیدی** را باز کنید و به **مدیریت فایل** بروید.
۲. فایل تم فعلی (`.gth`) را پیدا کنید — یا هر فایل `.gth` موجود را کپی کنید.
۳. کپی کنید و نامش را عوض کنید (مثلاً `mytheme.gth`).
۴. روی فایل جدید بزنید → یک شیت باز میشود → گزینهی **ویرایش** (✎) را انتخاب کنید.
۵. **ویرایشگر تم** با ۴ تب در بالا باز میشود: **Activity** · **Editor** · **Widget** · **M3Color**.
۶. روی هر نمونهی رنگ بزنید → رنگ موردنظر را انتخاب کنید → OK. فایل خودکار ذخیره میشود.
۷. وقتی راضی بودید، دوباره روی فایل در مدیریت فایل بزنید → **اعمال**.

> هرچه در این سند آمده دقیقاً مطابق همان ۴ تب است. چیزی که در ویرایشگر میبینید همان چیزی است که در زیر توضیح داده شده.

---

## ۳. چهار بخش یک تم

تم JSON دارای **۴ بلوک اصلی** است که با ۴ تب ویرایشگر یکیاند:

| بلوک JSON | تب ویرایشگر | چه چیزی را کنترل میکند |
|------------|--------------|--------------------------|
| `activity` | **Activity** | پنجره: پسزمینه، نوار وضعیت، نوار ناوبری |
| `editor`   | **Editor**   | ویرایشگر کد: رنگهای سینتکس، انتخاب، اسکرولبار، پرانتزها و… |
| `widget`   | **Widget**   | ویجتهای رابط کاربری: متن، سطحها، دکمهی شناور، تبها، منو، بلور و تصویر پسزمینه |
| `material3`| **M3Color**  | سیستم رنگ Material 3 که کل رابط اپ را رنگ میکند |

در ادامه هر بلوک فیلدبهفیلد توضیح داده شده است.

---

## ۴. `activity` — رنگهای پنجره (تب: Activity)

| کلید JSON | معنی ساده |
|-----------|-----------|
| `background`    | رنگ پسزمینهی کلی پنجره |
| `statusBar`     | رنگ نوار وضعیت بالای صفحه (ناحیهی ساعت/نوتیفیکیشن) |
| `navigationBar` | رنگ نوار ناوبری پایین صفحه |

---

## ۵. `editor` — رنگهای ویرایشگر کد (تب: Editor)

اینها ظاهر کد هنگام تایپ را کنترل میکنند. دستهبندیشده بر اساس معنا:

### متن و سینتکس
| کلید JSON | معنی |
|-----------|------|
| `textNormal`      | رنگ پیشفرض متن |
| `keyword`         | کلیدواژهها (`if`، `for`، `return` و…) |
| `comment`         | کامنتها |
| `operator`        | عملگرها (`+`، `=`، `&&` و…) |
| `literal`         | مقادیر ثابت (عدد، رشته) |
| `identifierVar`   | شناسهی متغیر |
| `identifierName`  | نام شناسه |
| `functionName`    | نام توابع |
| `annotation`      | Annotation ها (`@Override` و…) |
| `htmlTag`         | تگهای HTML |
| `attributeName`   | نام ویژگیها/attribute ها |
| `attributeValue`  | مقدار ویژگیها/attribute ها |
| `nonPrintableChar`| کاراکترهای نامرئی / غیرقابلچاپ |
| `colornextdot`    | رنگ توکن بعد از نقطه |
| `colornextbrak`   | رنگ توکن بعد از پرانتز |
| `colornextchar`   | رنگ هایلایت کاراکتر بعدی |
| `coloruppercase`  | رنگ توکن حروف بزرگ |
| `colornextless`   | رنگ توکن مربوط به «کمتر از» |

### خطوط و شمارهی خط
| کلید JSON | معنی |
|-----------|------|
| `lineDivider`          | خط جداکننده بین بخشهای ویرایشگر |
| `currentLine`          | هایلایت خطی که مکاننما روی آن است |
| `lineNumber`           | رنگ متن شمارهی خط |
| `lineNumberCurrent`    | شمارهی خط جاری (فعال) |
| `lineNumberBackground` | پسزمینهی پشت شمارههای خط |
| `lineNumberPanel`      | پسزمینهی پنل شمارهی خط |
| `lineNumberPanelText`  | رنگ متن پنل شمارهی خط |
| `currentRowBorder`     | حاشیهی ردیف جاری |
| `blockLine`            | خط راهنمای بلوک کد |
| `blockLineCurrent`     | خط راهنمای بلوک کد در بلوک جاری |
| `sideBlockLine`        | خط راهنمای بلوک کناری |
| `hardWrapMarker`       | رنگ نشانگر شکستن خط (Hard wrap) |
| `strikeThrough`        | رنگ متن خطخورده (خطوط حذفشده) |

### انتخاب و مکاننما (cursor)
| کلید JSON | معنی |
|-----------|------|
| `selectedTextBackground` | پسزمینهی پشت متن انتخابشده |
| `selectedTextBorder`     | حاشیهی دور متن انتخابشده |
| `textSelected`           | رنگ خود متن انتخابشده |
| `selectionInsert`        | رنگ خط مکاننما |
| `selectionHandle`        | رنگ دستگیرهی انتخاب |
| `underline`              | رنگ خط زیرنویس |

### اسکرولبار
| کلید JSON | معنی |
|-----------|------|
| `scrollBarThumb`        | رنگ دستگیرهی (thumb) اسکرولبار |
| `scrollBarThumbPressed` | رنگ دستگیره هنگام لمس/فشردن |
| `scrollBarTrack`        | رنگ مسیر (Track) اسکرولبار |

### پنجرهی تکمیل خودکار (Autocomplete)
| کلید JSON | معنی |
|-----------|------|
| `completionWndBackground` | پسزمینهی پاپآپ تکمیل خودکار |
| `completionWndCorner`     | رنگ گوشهی پاپآپ تکمیل خودکار |
| `completionWndTextPrimary`   | متن اصلی در تکمیل خودکار |
| `completionWndTextSecondary` | متن ثانویه در تکمیل خودکار |
| `completionWndItemCurrent`   | هایلایت آیتم فعلی در تکمیل خودکار |
| `completionWndTextMatched`   | رنگ بخش منطبقشدهی متن آیتم |

### جستوجو / انطباق
| کلید JSON | معنی |
|-----------|------|
| `matchedTextBackground`        | پسزمینهی متن منطبقشده در جستوجو |
| `matchedTextBorder`            | حاشیهی متن منطبقشده |
| `highlightedDelimitersBackground` | پسزمینهی جفت پرانتز هایلایتشده |
| `highlightedDelimitersUnderline`  | خط زیر جفت پرانتز هایلایتشده |
| `highlightedDelimitersForeground` | رنگ جلوی جفت پرانتز هایلایتشده |
| `highlightedDelimitersBorder`     | حاشیهی جفت پرانتز هایلایتشده |

### هایلایت متن
| کلید JSON | معنی |
|-----------|------|
| `textHighlightBackground`        | پسزمینهی هایلایت عمومی متن |
| `textHighlightBorder`            | حاشیهی هایلایت عمومی متن |
| `textHighlightStrongBackground`  | پسزمینهی هایلایت پررنگ متن |
| `textHighlightStrongBorder`      | حاشیهی هایلایت پررنگ متن |
| `staticSpanBackground`           | پسزمینهی بخش استاتیک |
| `staticSpanForeground`           | رنگ جلوی بخش استاتیک |

### مشکلها / تشخیص (Diagnostics)
| کلید JSON | معنی |
|-----------|------|
| `problemError`   | رنگ/خط زیر خطا |
| `problemWarning` | رنگ/خط زیر هشدار |
| `problemTypo`    | رنگ/خط زیر غلط املایی |

### Tooltip ها (هوور، امضای تابع، تشخیص)
| کلید JSON | معنی |
|-----------|------|
| `signatureBackground`               | پسزمینهی پاپآپ امضای تابع |
| `signatureBorder`                   | حاشیهی پاپآپ امضای تابع |
| `signatureTextNormal`               | متن عادی امضای تابع |
| `signatureTextHighlightedParameter` | پارامتر هایلایتشده در امضای تابع |
| `hoverBackground`                   | پسزمینهی تولتیپ هوور |
| `hoverBorder`                       | حاشیهی تولتیپ هوور |
| `hoverTextNormal`                   | متن عادی تولتیپ هوور |
| `hoverTextHighlighted`              | متن هایلایتشدهی تولتیپ هوور |
| `diagnosticTooltipBackground`       | پسزمینهی تولتیپ تشخیص |
| `diagnosticTooltipBriefMsg`         | پیام کوتاه تشخیص |
| `diagnosticTooltipDetailedMsg`      | پیام جزئی تشخیص |
| `diagnosticTooltipAction`           | متن اکشن (قابلکلیک) تشخیص |
| `textActionWindowBackground`        | پسزمینهی پنجرهی اکشن متن |
| `textActionWindowIconColor`         | رنگ آیکون پنجرهی اکشن متن |

### Inlay Hint و Snippet
| کلید JSON | معنی |
|-----------|------|
| `textInlayHintBackground`   | پسزمینهی inlay hint |
| `textInlayHintForeground`   | رنگ جلوی inlay hint |
| `snippetBackgroundEditing`  | پسزمینهی snippet ای که در حال ویرایش است |
| `snippetBackgroundRelated`  | پسزمینهی ناحیهی مرتبط snippet |
| `snippetBackgroundInactive` | پسزمینهی snippet غیرفعال |
| `functionCharBackgroundStroke` | پسزمینه/خط کاراکتر تابع |

### مینیمپ
| کلید JSON | معنی |
|-----------|------|
| `minimapBackground`        | پسزمینهی مینیمپ |
| `minimapViewport`          | مستطیل نما (viewport) مینیمپ |
| `minimapViewportBorder`    | حاشیهی viewport مینیمپ |

### رنگهای سطح پرانتز (عمق تودرتو)
| کلید JSON | معنی |
|-----------|------|
| `bracketlevelmatch1` | رنگ سطح تودرتو ۱ پرانتز |
| `bracketlevelmatch2` | رنگ سطح تودرتو ۲ پرانتز |
| `bracketlevelmatch3` | رنگ سطح تودرتو ۳ پرانتز |
| `bracketlevelmatch4` | رنگ سطح تودرتو ۴ پرانتز |
| `bracketlevelmatch5` | رنگ سطح تودرتو ۵ پرانتز |
| `bracketlevelmatch6` | رنگ سطح تودرتو ۶ پرانتز |

### Sticky Scroll
| کلید JSON | معنی |
|-----------|------|
| `stickyScrollDivider` | خط جداکننده زیر سربرگ چسبان (Sticky) |

> ⚠️ چند کلید در پیشفرضهای موتور تم وجود دارد اما هنوز در تب Editor نمایش داده نمیشوند: `wholeBackground` (پسزمینهی کلی ویرایشگر). باز هم میتوانید با ویرایش مستقیم JSON آن را تنظیم کنید.

---

## ۶. `widget` — رنگهای ویجت رابط کاربری (تب: Widget)

| کلید JSON | معنی |
|-----------|------|
| `text`          | رنگ متن عمومی ویجتها |
| `hint`          | رنگ متن راهنما/placeholder (مثلاً در ورودیها) |
| `accent`        | رنگ تأکیدی (accent) که در ویجتها استفاده میشود |
| `background`    | رنگ پسزمینهی عمومی |
| `surface`       | رنگ سطح (کارت/پنل) |
| `stroke`        | رنگ حاشیه/خط دور |
| `fabBackground` | پسزمینهی دکمهی شناور |
| `fabIcon`       | رنگ آیکون دکمهی شناور |
| `tabSelected`   | رنگ تب انتخابشده |
| `tabUnselected` | رنگ تب انتخابنشده |
| `imageTint`     | رنگ اعمالشده روی آیکونها/تصاویر |
| `menubackground`    | پسزمینهی منوی بازشونده |
| `menutextcolor`     | رنگ متن منو |
| `selectedmenucolor` | رنگ هایلایت آیتم انتخابشدهی منو |

بهاضافهی دو کنترل دیگر (رنگ نیستند):
| کلید JSON | معنی |
|-----------|------|
| `imagepath` | مسیر تصویر/GIF/ویدیوی پسزمینه که بهصورت بلورشده استفاده میشود |
| `blursize`  | میزان بلور بودن تصویر پسزمینه (۰ تا ۲۵) |

---

## ۷. `material3` — سیستم رنگ Material 3 (تب: M3Color)

این **سیستم رنگ** است که همهی ویجتهای متریال اپ (دکمهها، سوییچها، دیالوگها، تبها، سطوح، ورودیهای متنی) را رنگ میکند. در Material 3 هر رنگ یک نسخهی **"on"** دارد (رنگ متن/آیکونی که روی آن مینشیند) تا کنتراست خوانا بماند.

### Primary (رنگ اصلی)
| کلید JSON | معنی |
|-----------|------|
| `primary`          | رنگ اصلی برند (دکمه، نشانگر فعال) |
| `onPrimary`        | متن/آیکون روی `primary` |
| `primaryContainer` | نسخهی نرمتر (کانتینر) رنگ اصلی |
| `onPrimaryContainer` | متن/آیکون روی `primaryContainer` |
| `primaryFixed`        | تُن روشن ثابت رنگ اصلی |
| `onPrimaryFixed`      | متن روی `primaryFixed` |
| `primaryFixedDim`     | تُن ثابت محو رنگ اصلی |
| `onPrimaryFixedVariant` | متن روی `primaryFixedDim` |
| `inversePrimary`      | رنگ اصلی برای سطوح معکوس |

### Secondary (رنگ دوم)
| کلید JSON | معنی |
|-----------|------|
| `secondary`          | رنگ دوم برند |
| `onSecondary`        | متن/آیکون روی `secondary` |
| `secondaryContainer` | کانتینر نرمتر رنگ دوم |
| `onSecondaryContainer` | متن روی `secondaryContainer` |
| `secondaryFixed`        | تُن ثابت روشن رنگ دوم |
| `onSecondaryFixed`      | متن روی `secondaryFixed` |
| `secondaryFixedDim`     | تُن ثابت محو رنگ دوم |
| `onSecondaryFixedVariant` | متن روی `secondaryFixedDim` |

### Tertiary (رنگ سوم)
| کلید JSON | معنی |
|-----------|------|
| `tertiary`          | رنگ تأکیدی سوم |
| `onTertiary`        | متن/آیکون روی `tertiary` |
| `tertiaryContainer` | کانتینر نرمتر رنگ سوم |
| `onTertiaryContainer` | متن روی `tertiaryContainer` |
| `tertiaryFixed`        | تُن ثابت روشن رنگ سوم |
| `onTertiaryFixed`      | متن روی `tertiaryFixed` |
| `tertiaryFixedDim`     | تُن ثابت محو رنگ سوم |
| `onTertiaryFixedVariant` | متن روی `tertiaryFixedDim` |

### Error (خطا)
| کلید JSON | معنی |
|-----------|------|
| `error`          | رنگ خطا (اعتبارسنجی، مشکلها) |
| `onError`        | متن/آیکون روی `error` |
| `errorContainer` | کانتینر نرمتر خطا |
| `onErrorContainer` | متن روی `errorContainer` |

### Neutral (پسزمینه / سطح)
| کلید JSON | معنی |
|-----------|------|
| `background`    | پسزمینهی اپ |
| `onBackground`  | متن/آیکون روی `background` |
| `surface`       | سطح اپ (کارتها، شیتها) |
| `onSurface`     | متن/آیکون روی `surface` |
| `surfaceVariant`   | نسخهی سطح (برای سطوح مجاور) |
| `onSurfaceVariant` | متن/آیکون روی `surfaceVariant` |
| `surfaceTint`      | رنگ تُن روی سطوح |

### Outline و متفرقه
| کلید JSON | معنی |
|-----------|------|
| `outline`         | رنگ خط دور/حاشیه |
| `outlineVariant`  | نسخهی روشنتر خط دور |
| `shadow`          | رنگ سایه |
| `scrim`           | لایهی تیرهی پشت دیالوگها/شیتها |

### Inverse (معکوس)
| کلید JSON | معنی |
|-----------|------|
| `inverseSurface`   | رنگ سطح معکوس |
| `inverseOnSurface` | متن/آیکون روی `inverseSurface` |

### سطحهای Container (لایههای تُن)
| کلید JSON | معنی |
|-----------|------|
| `surfaceDim`             | تُن سطح محو (تیرهترین) |
| `surfaceBright`          | تُن سطح روشن |
| `surfaceContainerLowest` | کانتینر سطح پایینترین (تیرهترین) |
| `surfaceContainerLow`    | کانتینر سطح پایین |
| `surfaceContainer`       | کانتینر سطح پیشفرض |
| `surfaceContainerHigh`   | کانتینر سطح بالا |
| `surfaceContainerHighest`| کانتینر سطح بالاترین (روشنترین) |

> **نکته:** قانون سرانگشتی Material 3 این است که برای هر رنگ یک نسخهی `on...` وجود دارد. اگر `primary` را عوض کردید، `onPrimary` را هم تغییر دهید تا متن خوانا بماند. همین قانون برای جفتهای `...Container` / `on...Container` هم صدق میکند.

---

## ۸. قالب کامل JSON (کپی-پیست)

این را با نام `mytheme.gth` ذخیره کنید و مقدارها را ویرایش کنید. این دقیقاً ساختاری است که گوست آیدی انتظار دارد.

```json
{
  "activity": {
    "background": "#282c34",
    "statusBar": "#282c34",
    "navigationBar": "#282c34"
  },

  "editor": {
    "lineDivider": "#3e4452",
    "wholeBackground": "#282c34",
    "lineNumber": "#5c6370",
    "lineNumberBackground": "#282c34",
    "textNormal": "#abb2bf",
    "selectedTextBackground": "#3e4452",
    "selectionInsert": "#528bff",
    "selectionHandle": "#528bff",
    "currentLine": "#2c313a",
    "underline": "#abb2bf",
    "scrollBarThumb": "#3e4452",
    "scrollBarThumbPressed": "#528bff",
    "scrollBarTrack": "#21252b",
    "blockLine": "#3e4452",
    "blockLineCurrent": "#528bff",
    "lineNumberPanel": "#21252b",
    "lineNumberPanelText": "#abb2bf",
    "completionWndBackground": "#282c34",
    "completionWndCorner": "#282c34",
    "keyword": "#c678dd",
    "comment": "#5c6370",
    "operator": "#56b6c2",
    "literal": "#d19a66",
    "identifierVar": "#e06c75",
    "identifierName": "#61afef",
    "functionName": "#61afef",
    "annotation": "#e5c07b",
    "matchedTextBackground": "#3e4452",
    "matchedTextBorder": "#528bff",
    "textSelected": "#ffffff",
    "nonPrintableChar": "#3e4452",
    "htmlTag": "#e06c75",
    "attributeName": "#d19a66",
    "attributeValue": "#98c379",
    "problemError": "#e06c75",
    "problemWarning": "#e5c07b",
    "problemTypo": "#98c379",
    "colornextdot": "#c678dd",
    "colornextbrak": "#56b6c2",
    "colornextchar": "#d19a66",
    "coloruppercase": "#61afef",
    "colornextless": "#98c379",
    "lineNumberCurrent": "#528bff",
    "selectedTextBorder": "#528bff",
    "currentRowBorder": "#3e4452",
    "highlightedDelimitersBackground": "#2c313a",
    "highlightedDelimitersUnderline": "#528bff",
    "highlightedDelimitersForeground": "#abb2bf",
    "highlightedDelimitersBorder": "#528bff",
    "textHighlightBackground": "#3e4452",
    "textHighlightBorder": "#528bff",
    "textHighlightStrongBackground": "#2c313a",
    "textHighlightStrongBorder": "#c678dd",
    "staticSpanBackground": "#282c34",
    "staticSpanForeground": "#abb2bf",
    "textInlayHintBackground": "#2c313a",
    "textInlayHintForeground": "#5c6370",
    "snippetBackgroundEditing": "#2c313a",
    "snippetBackgroundRelated": "#3e4452",
    "snippetBackgroundInactive": "#21252b",
    "hardWrapMarker": "#3e4452",
    "functionCharBackgroundStroke": "#3e4452",
    "diagnosticTooltipBackground": "#2c313a",
    "diagnosticTooltipBriefMsg": "#abb2bf",
    "diagnosticTooltipDetailedMsg": "#5c6370",
    "diagnosticTooltipAction": "#61afef",
    "stickyScrollDivider": "#3e4452",
    "strikeThrough": "#00000000",
    "sideBlockLine": "#3e4452",
    "completionWndTextPrimary": "#abb2bf",
    "completionWndTextSecondary": "#5c6370",
    "completionWndItemCurrent": "#2c313a",
    "completionWndTextMatched": "#61afef",
    "signatureBackground": "#282c34",
    "signatureBorder": "#3e4452",
    "signatureTextNormal": "#abb2bf",
    "signatureTextHighlightedParameter": "#e06c75",
    "hoverBackground": "#2c313a",
    "hoverBorder": "#528bff",
    "hoverTextNormal": "#abb2bf",
    "hoverTextHighlighted": "#61afef",
    "textActionWindowBackground": "#282c34",
    "textActionWindowIconColor": "#abb2bf",
    "minimapBackground": "#a0282c34",
    "minimapViewport": "#30ffffff",
    "minimapViewportBorder": "#b0ffffff",
    "bracketlevelmatch1": "#FFDD00",
    "bracketlevelmatch2": "#00D9FF",
    "bracketlevelmatch3": "#00FF55",
    "bracketlevelmatch4": "#FF6200",
    "bracketlevelmatch5": "#FF64F5",
    "bracketlevelmatch6": "#64FFD0"
  },

  "widget": {
    "text": "#abb2bf",
    "hint": "#5c6370",
    "accent": "#61afef",
    "background": "#282c34",
    "surface": "#2c313a",
    "stroke": "#3e4452",
    "fabBackground": "#61afef",
    "fabIcon": "#ffffff",
    "tabSelected": "#61afef",
    "tabUnselected": "#5c6370",
    "imageTint": "#abb2bf",
    "menubackground": "#282c34",
    "menutextcolor": "#abb2bf",
    "selectedmenucolor": "#3e4452",
    "imagepath": "",
    "blursize": 1
  },

  "material3": {
    "primary": "#B9C3FF",
    "surfaceTint": "#B9C3FF",
    "onPrimary": "#212C61",
    "primaryContainer": "#384379",
    "onPrimaryContainer": "#DDE1FF",
    "secondary": "#C3C5DD",
    "onSecondary": "#2C2F42",
    "secondaryContainer": "#424659",
    "onSecondaryContainer": "#DFE1F9",
    "tertiary": "#E5BAD8",
    "onTertiary": "#44263E",
    "tertiaryContainer": "#5C3C55",
    "onTertiaryContainer": "#FFD7F3",
    "error": "#FFB4AB",
    "onError": "#690005",
    "errorContainer": "#93000A",
    "onErrorContainer": "#FFDAD6",
    "background": "#121318",
    "onBackground": "#E3E1E9",
    "surface": "#121318",
    "onSurface": "#E3E1E9",
    "surfaceVariant": "#45464F",
    "onSurfaceVariant": "#C6C5D0",
    "outline": "#90909A",
    "outlineVariant": "#45464F",
    "shadow": "#000000",
    "scrim": "#000000",
    "inverseSurface": "#E3E1E9",
    "inverseOnSurface": "#303036",
    "inversePrimary": "#505B92",
    "primaryFixed": "#DDE1FF",
    "onPrimaryFixed": "#08164B",
    "primaryFixedDim": "#B9C3FF",
    "onPrimaryFixedVariant": "#384379",
    "secondaryFixed": "#DFE1F9",
    "onSecondaryFixed": "#171B2C",
    "secondaryFixedDim": "#C3C5DD",
    "onSecondaryFixedVariant": "#424659",
    "tertiaryFixed": "#FFD7F3",
    "onTertiaryFixed": "#2D1228",
    "tertiaryFixedDim": "#E5BAD8",
    "onTertiaryFixedVariant": "#5C3C55",
    "surfaceDim": "#121318",
    "surfaceBright": "#38393F",
    "surfaceContainerLowest": "#0D0E13",
    "surfaceContainerLow": "#1B1B21",
    "surfaceContainer": "#1F1F25",
    "surfaceContainerHigh": "#292A2F",
    "surfaceContainerHighest": "#34343A"
  }
}
```

---

## ۹. نحوهی اعمال تم

۱. در **مدیریت فایل** روی فایل `.gth` خود بزنید.
۲. یک شیت با دو گزینه باز میشود:
   - **ویرایش** (✎) — باز کردن ویرایشگر تصویری تم.
   - **اعمال** — فعال کردن این تم (کل اپ بلافاصله تغییر میکند).
۳. روی **اعمال** بزنید.

---

## ۱۰. قوانین کاربردی

- **فرمت رنگ:** همیشه `#RRGGBBAA` (۸ رقم هگز). معمولاً `#RRGGBB` با ۶ رقم هم پذیرفته میشود.
- **`##00000000`** یعنی کاملاً شفاف.
- **جفتها:** در `material3` هر وقت رنگی را عوض کردید، نسخهی `on...` آن را هم برای کنتراست خوانا بهروزرسانی کنید.
- **کلیدهای ناقص خودکار از تم پیشفرض پر میشوند**، پس لازم نیست همهی کلیدها را بنویسید — اما نوشتنشان به شما کنترل کامل میدهد.
- میتوانید فایل `.gth` را **بهصورت متن ساده** ویرایش کنید تا کلیدهایی را که ویرایشگر تصویری نشان نمیدهد (مثل `wholeBackground`) تنظیم کنید.

---

## ۱۱. ارجاع به رنگ دیگر (Reference / `@`)

بهجای نوشتن یک رنگ ثابت، میتوانید یک کلید دیگر از همین تم را بهعنوان مقدار بنویسید. با این کار تم شما «زنده» میشود: کافی است یک رنگ را در یک جا عوض کنید تا همهی کلیدهایی که به آن ارجاع میدهند خودکار آپدیت شوند.

قالب: `"@block.key"` — مثلاً:

```json
{
  "widget": {
    "surface": "@material3.surfaceContainerHigh",
    "stroke": "@material3.outlineVariant"
  }
}
```

قانونها:
- نام `block` یکی از `activity` ،`editor` ،`widget` یا `material3` است.
- `key` باید کلید واقعی همان بلوک باشد (مثلاً `surface` فقط در `widget` یا `material3` معنی دارد).
- ارجاعها در اعمال تم، در پیشنمایش و در رنگسنج خودکار **به رنگ نهایی حل میشوند** (چرخهی A→B و B→A مجاز نیست و حل نمیشود).

در ویرایشگر تم:
- 🖐️ **لمس بلند روی هر ردیف رنگ** → یک فهرست از همهی رنگهای قابل ارجاع با پیشنمایش باز میشود.
- در پنجرهی **انتخاب رنگ** هم یک فیلد «Reference (@block.key)» هست که با آن میتوانید مقدار فعلی را بهصورت ارجاع بنویسید یا رنگ را به رنگ ثابت برگردانید.

---

## ۱۲. ویرایش متن ساده و پشتیبانی LSP

کنار دکمههای «بازنشانی» و «پیشنمایش»، آیکون **Edit source** در تولبار ویرایشگر تم است. با آن همان فایل `.gth` در **ویرایشگر کد** باز میشود تا JSON را دستی ویرایش کنید.

این ویرایشگر کامل است:
- **تشخیص خطا:** کلید ناشناخته، بلوک ناشناخته، رنگ نامعتبر، مقدار خارج از بازه و ارجاع `@` خراب/چرخهای → زیر آن خط قرمز/زرد میآید.
- **تکمیل خودکار:** با تایپ، کلیدهای هر بلوک پیشنهاد میشوند و بعد از `@` هم بلوکها و کلیدهایشان.
- **هوور:** روی هر کلید رنگ، مقدار نهایی حلشدهی آن نمایش داده میشود.
- **رفتن به تعریف:** با لمس/کلیک روی یک ارجاع `@block.key` به خط تعریف آن کلید میروید.
- تم بهگونهای ساخته شده که با نوشتن در همین ویرایشگر سریعاً خطاهایتان را بگوید.

> با «ویرایش دستی» هر کلیدی را که ویرایشگر تصویری نشان نمیدهد میتوانید ست کنید؛ بعد از تغییر، فایل را ذخیره و دوباره اعمال کنید.

حالا برو تم خودت را بساز. اگر تا اینجا رسیدی، همهچیز را بلدی. 🎨
