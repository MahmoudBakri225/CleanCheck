# Committee Demo (5-7 minutes)

## One-liner
CleanCheck analyzes whole Java projects offline with stable rules, explains the principle behind each issue, applies only safe verified fixes, and tracks quality over time.

## Live path
1. `mvn clean javafx:run`
2. Choose Project → any Java project folder (one project or several)
3. Start Analysis
4. Dashboard → show score + severity cards
5. Issues → open a finding → show Principle / Why / Example
6. Apply Verified Fix on Data Type or Encapsulation
7. Show Status = Fixed
7b. Security Review tab → grade, CWE / OWASP per finding, how it is exploited and the safe fix (open `docs/security-demo/` for a file that triggers every rule)
8. Class Mode → a folder of student projects → ranking + project details
9. Header → **Report** → save `<project>_Report.html` and open it in a browser
10. About → answer SonarQube / ChatGPT in 20 seconds

## Answers
**Why not SonarQube?**  
Enterprise platform. CleanCheck is local, lightweight, and educational.

**Why not ChatGPT?**  
Chat is one-off. CleanCheck is repeatable full-project analysis with local fixes and history, without uploading source.
