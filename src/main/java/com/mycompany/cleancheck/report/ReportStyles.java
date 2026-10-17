package com.mycompany.cleancheck.report;

/**
 * Stylesheet embedded in every generated HTML report.
 * Supports light mode, automatic dark mode and printing.
 *
 * @author KA
 */
public final class ReportStyles {

    public static final String CSS = """
            :root {
                --bg: #f4f7fb;
                --surface: #ffffff;
                --surface-2: #f8fafc;
                --text: #0f172a;
                --muted: #64748b;
                --border: #e2e8f0;
                --brand: #4f46e5;
                --brand-2: #0ea5e9;
                --critical: #dc2626;
                --warning: #ea580c;
                --info: #2563eb;
                --success: #16a34a;
                --shadow: 0 4px 24px rgba(15, 23, 42, 0.07);
            }
            @media (prefers-color-scheme: dark) {
                :root {
                    --bg: #0b1120;
                    --surface: #111a2e;
                    --surface-2: #17223b;
                    --text: #e5e9f2;
                    --muted: #94a3b8;
                    --border: #24324f;
                    --shadow: 0 4px 24px rgba(0, 0, 0, 0.45);
                }
            }
            * { box-sizing: border-box; }
            body {
                margin: 0;
                font-family: "Segoe UI", Inter, system-ui, -apple-system, Arial, sans-serif;
                background: var(--bg);
                color: var(--text);
                line-height: 1.5;
            }
            .container { width: 94%; max-width: 1280px; margin: 32px auto; }
            .header {
                background: linear-gradient(135deg, #1e1b4b 0%, var(--brand) 55%, var(--brand-2) 100%);
                color: #ffffff;
                padding: 36px 40px;
                border-radius: 20px;
                margin-bottom: 28px;
                box-shadow: var(--shadow);
            }
            .header h1 { margin: 0 0 8px 0; font-size: 32px; letter-spacing: -0.5px; }
            .header p { margin: 4px 0; opacity: 0.9; }
            .section {
                background: var(--surface);
                padding: 28px;
                border-radius: 18px;
                margin-bottom: 24px;
                border: 1px solid var(--border);
                box-shadow: var(--shadow);
            }
            .section h2 { margin: 0 0 6px 0; font-size: 20px; }
            .subtitle { color: var(--muted); margin: 0 0 14px 0; }
            .cards {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
                gap: 14px;
                margin-top: 14px;
            }
            .card {
                padding: 18px 20px;
                border-radius: 14px;
                background: var(--surface-2);
                border: 1px solid var(--border);
                border-top: 4px solid var(--brand);
            }
            .card-title { font-size: 13px; color: var(--muted); text-transform: uppercase; letter-spacing: 0.6px; }
            .card-value { font-size: 30px; font-weight: 700; margin-top: 6px; }
            .score {
                font-size: 52px;
                font-weight: 800;
                background: linear-gradient(90deg, var(--brand), var(--brand-2));
                -webkit-background-clip: text;
                background-clip: text;
                color: transparent;
            }
            table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 14px; }
            th, td { padding: 12px 14px; border-bottom: 1px solid var(--border); text-align: left; }
            th {
                background: var(--surface-2);
                color: var(--muted);
                font-size: 12px;
                text-transform: uppercase;
                letter-spacing: 0.6px;
            }
            tbody tr:nth-child(even) { background: var(--surface-2); }
            tr:hover td { background: rgba(79, 70, 229, 0.08); }
            .critical { color: var(--critical); font-weight: 700; }
            .warning { color: var(--warning); font-weight: 700; }
            .info { color: var(--info); font-weight: 700; }
            .success { color: var(--success); font-weight: 700; }
            .danger { color: var(--critical); font-weight: 700; }
            .neutral { color: var(--muted); font-weight: 700; }
            .hotspot {
                padding: 14px 18px;
                margin-bottom: 10px;
                background: var(--surface-2);
                border: 1px solid var(--border);
                border-left: 5px solid var(--warning);
                border-radius: 12px;
            }
            .hotspot-title { font-weight: 700; }
            .risk { float: right; font-weight: 700; color: var(--warning); }
            .footer { text-align: center; color: var(--muted); padding: 24px; font-size: 13px; }
            @media (max-width: 700px) {
                .header { padding: 24px; }
                .section { padding: 18px; overflow-x: auto; }
            }
            @media print {
                body { background: #ffffff; color: #000000; }
                .header { background: #1e1b4b; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                .section { box-shadow: none; break-inside: avoid; }
            }
            """;

    private ReportStyles() {
    }
}
