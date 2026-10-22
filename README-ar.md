# CleanCheck

أداة محلية لتحليل جودة وأمان مشاريع Java، وشرح النتائج، واقتراح إصلاحات قابلة للتحقق.

## المتطلبات

- JDK 26
- Maven 3.9 أو أحدث
- اتصال بالإنترنت عند أول بناء لتنزيل الاعتماديات

## تشغيل الواجهة

```powershell
mvn clean javafx:run
```

## فحص مشروع من سطر الأوامر

```powershell
mvn exec:java "-Dexec.mainClass=com.mycompany.cleancheck.application.CleanCheck" "-Dexec.args=C:/path/to/project --analyze-only"
```

## التقارير وبوابة الجودة

```powershell
mvn exec:java "-Dexec.mainClass=com.mycompany.cleancheck.application.CiRunner" "-Dexec.args=C:/path/to/project --html report.html --json report.json --sarif report.sarif --csv report.csv --min-score 80"
```

رمز الخروج `0` يعني اجتياز البوابة، و`1` يعني عدم اجتيازها، و`2` يعني خطأ استخدام أو قراءة. تقارير SARIF مناسبة للربط مع أدوات مراجعة الشيفرة.

## الإعدادات والتاريخ

- انسخ `cleancheck.properties.example` إلى مجلد المشروع باسم `cleancheck.properties` لتعديل القواعد والحدود والاستثناءات.
- تُحفظ النتائج التاريخية محليًا في `~/.cleancheck/database/cleancheck.db`. يمكن تحديد قاعدة أخرى عبر `-Dcleancheck.database.path=/absolute/path/cleancheck.db`.
- شغّل الاختبارات باستخدام `mvn test`.

## حدود الفحص

الفحص الأمني مبدئي وقائم على أنماط AST داخل الملف الواحد؛ قد يفوته تدفق بيانات بين ملفات ولا يفحص إصدارات الاعتماديات الضعيفة. راجع النتائج يدويًا، ولا تعتبر الدرجة أو البوابة ضمانًا أمنيًا. الإصلاحات الآلية محدودة وتُراجع بإعادة التحليل وفحص الترجمة عند توافر JDK.

راجع [دليل القواعد بالعربية](docs/rules-ar.md) و[دليل التشغيل](docs/RUN.md).
