# دليل القواعد | Rule reference

يحتوي CleanCheck على 22 قاعدة جودة لكل ملف، و5 قواعد تعليمية لـSOLID، و10 قواعد أمنية مبدئية، وقاعدة
للمقارنة بين الملفات لاكتشاف الكود المكرر. إذا تعذر تحليل ملف Java، تظهر نتيجة **Parse Error** بدرجة حرجة بدلًا من
اعتباره سليمًا. حدود طول الدالة والكلاس والتعقيد والتداخل وعدد المعاملات وحد التكرار قابلة للضبط في
`cleancheck.properties`.

## قواعد الجودة | Quality rules

| Rule | بتفحص إيه | What it checks |
|---|---|---|
| Encapsulation | الحقول التي ليست `private` | Fields that are not private |
| Empty Method | الدوال التي جسمها فارغ | Methods with an empty body |
| Empty If | جمل `if` التي جسمها فارغ | Empty `if` bodies |
| Empty While | حلقات `while` التي جسمها فارغ | Empty `while` bodies |
| Infinite Loop | الحلقات غير المحدودة مع مراعاة مخارج ظاهرة | Unbounded loops and visible exits |
| Too Many Parameters | عدد معاملات الدالة فوق الحد المضبوط | Methods over the configured parameter threshold |
| Naming Convention | أنماط تسمية الأنواع والدوال والحقول | Type, method, and field naming heuristics |
| Data Type | أخطاء شائعة مثل `string` و`Int` وأنواع أموال غير مناسبة | Common type misspellings and risky floating-point money types |
| Empty Catch | `catch` التي تتجاهل الاستثناء | Catch blocks that discard exceptions |
| Magic Number | أرقام حرفية غير مسماة مع استثناء القيم الشائعة | Unnamed numeric literals, with common values allowed |
| Long Method | الدوال الأطول من الحد المضبوط | Methods over the configured source-line threshold |
| System Out | الاستخدام المباشر لـ`System.out` و`System.err` | Direct console output |
| Missing Package | ملفات المصدر التي لا تحتوي تصريح `package` | Source files without a package declaration |
| Unused Import | الاستيرادات التي لا يظهر استخدامها في الملف | Imports not referenced in the source |
| Unused Variable | المتغيرات المحلية التي لا يظهر أنها مقروءة | Locals that appear not to be read |
| Cyclomatic Complexity | التعقيد الناتج عن فروع القرار | Decision complexity |
| Deep Nesting | التداخل العميق للتحكم | Excessively nested control flow |
| String Comparison | مقارنة النصوص باستخدام `==` أو `!=` | String values compared with `==` / `!=` |
| Generic Catch | التقاط أنواع استثناءات عامة أكثر من اللازم | Overly broad catch types |
| Unclosed Resource | أنماط قد تترك الموارد مفتوحة | Resource handling that may leave resources open |
| Equals HashCode | تنفيذ `equals` من دون `hashCode` مطابق | `equals` without a matching `hashCode` |
| Class Length | الكلاسات الأطول من الحد المضبوط | Classes over the configured source-line threshold |

## قواعد SOLID التعليمية | SOLID heuristics

هذه إشارات تعليمية تقريبية وليست إثباتًا رسميًا لمخالفة التصميم:

| Rule | الفكرة | Heuristic |
|---|---|---|
| SOLID SRP | مسؤوليات متعددة تبدو غير مترابطة في كلاس واحد | Several apparently unrelated class responsibilities |
| SOLID OCP | إضافة الحالات قد تتطلب تعديل كود موجود | New cases may require editing existing code |
| SOLID LSP | override يرمي `UnsupportedOperationException` فقط | Override only throws `UnsupportedOperationException` |
| SOLID ISP | واجهة قد تفرض عمليات لا تستخدمها بعض التطبيقات | Interface may impose unused operations |
| SOLID DIP | اعتماد الكود عالي المستوى على تنفيذات ملموسة | High-level code appears coupled to concrete implementations |

## قاعدة المشروع | Project rule

| Rule | بتفحص إيه | What it checks |
|---|---|---|
| Duplicate Code | أجسام دوال أو constructors متطابقة عبر الملفات؛ تتجاهل التعليقات والتنسيق وتحافظ على محتوى النصوص | Identical method/constructor bodies across files; ignores comments/formatting, preserves literal contents |

## مراجعة الأمان المبدئية | Preliminary security review

| Rule | CWE | بتفحص إيه | What it checks |
|---|---|---|---|
| SQL Injection | CWE-89 | استعلام ديناميكي يمر إلى API شائع، بما في ذلك `StringBuilder` داخل الدالة | Dynamic SQL passed to common APIs, including same-method builder appends |
| Hardcoded Secret | CWE-798 | قيمة حرفية لمتغير اسمه يوحي بسر أو لصيغ مفاتيح معروفة | Literal values in secret-like variables or recognized key formats |
| Weak Cryptography | CWE-327 | خوارزميات أو أوضاع تشفير ضعيفة | Weak hashes, ciphers, or modes |
| Insecure Random | CWE-338 | مولد عشوائي غير تشفيري في سياق حساس | Non-cryptographic randomness in security-sensitive contexts |
| Command Injection | CWE-78 | أمر ديناميكي يمر إلى واجهات تشغيل العمليات | Dynamic commands passed to process APIs |
| Path Traversal | CWE-22 | مدخل يشبه طلب المستخدم يصل لقراءة/كتابة ملف بلا تطبيع وفحص احتواء داخل مجلد أساس | Request-like input reaches file I/O without normalization and base containment |
| Unsafe Deserialization | CWE-502 | فك تسلسل بلا إعدادات تصفية ظاهرة | Deserialization without visible filtering |
| Disabled Certificate Check | CWE-295 | تحقق متساهل من الشهادة أو اسم المضيف | Permissive certificate/hostname verification |
| Information Leak | CWE-209 | تسريب stack trace أو تفاصيل الاستثناء في المخرجات أو الرد | Stack traces or exception details exposed in output/response paths |
| XML External Entity | CWE-611 | تحليل XML بلا تحصين ظاهر ضد الكيانات الخارجية | XML parsing without visible external-entity hardening |

النتائج الأمنية مؤشرات للمراجعة وليست إثباتًا لقابلية الاستغلال. التحليل يعتمد على AST وتتبّع محدود محليًا؛ لا
يفحص إصدارات الاعتماديات الضعيفة، ولا يستطيع تتبع كل انتقال للبيانات عبر الملفات والمكتبات والـreflection والأطر.

## الإصلاحات التلقائية | Automatic fixes

الإصلاح التلقائي متاح لقواعد: Data Type، Encapsulation، System Out، Naming Convention، Missing Package،
Magic Number، Unused Import، String Comparison، وUnused Variable. توجد أيضًا معالجة ضيقة لـ`printStackTrace()` ضمن
Information Leak. بعد كل محاولة يعاد التحليل؛ إذا ظهرت نتائج جديدة أو أخطاء ترجمة جديدة يرجع الملف إلى نسخته
الأصلية. الإصلاح المقبول يحتفظ بنقطة تراجع محلية.

Automatic fixes are also listed in [the English rule reference](rules.md).
