import { useState } from "react";

export default function App() {
    const [url, setUrl] = useState("");
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const [toasts, setToasts] = useState([]);

    const addToast = (message, type = "info") => {
        const id = Date.now();
        setToasts((prev) => [...prev, { id, message, type }]);
        setTimeout(() => {
            setToasts((prev) => prev.filter((t) => t.id !== id));
        }, 4000);
    };

    async function checkUrl() {
        const u = url.trim();
        if (!u) {
            addToast("Please enter a valid URL first", "warning");
            return;
        }
        setLoading(true);
        setResult(null);

        try {
            const res = await fetch("http://localhost:8080/api/check-url", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ url: u }),
            });
            const data = await res.json().catch(() => ({}));

            if (!res.ok) {
                addToast(data?.message || "Analysis failed", "error");
            } else {
                setResult(data);
                addToast("Scan completed successfully", "success");
            }
        } catch (e) {
            addToast("Engine unreachable. Check local port 8080.", "error");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div style={styles.page}>
            <div style={styles.toastContainer}>
                {toasts.map((t) => (
                    <div key={t.id} style={{ ...styles.toast, ...styles[t.type] }}>
                        {t.message}
                    </div>
                ))}
            </div>

            <main style={styles.main}>
                <div style={styles.card}>
                    <header style={styles.header}>
                        <div style={{ display: "flex", alignItems: "center" }}>
                            <img src="/logo.png" alt="SafeLink AI Logo" style={{ width: "30px" }} />
                            <div style={{ marginLeft: "12px" }} />
                            <h1 style={styles.title}>SafeLink AI</h1>
                        </div>
                        <p style={{...styles.tagline, marginLeft: "42px"}}>Phishing URL Detector</p>
                    </header>

                    <div style={styles.inputGroup}>
                        <input
                            style={styles.input}
                            value={url}
                            onChange={(e) => setUrl(e.target.value)}
                            placeholder="https://example.com"
                            onKeyDown={(e) => e.key === "Enter" && checkUrl()}
                        />
                        <button
                            onClick={checkUrl}
                            disabled={loading}
                            className="btn-primary"
                            style={{ ...styles.button, ...(loading ? styles.buttonDisabled : {}) }}
                        >
                            {loading ? <div style={styles.spinner} /> : "Analyze URL"}
                        </button>
                    </div>

                    {result && (
                        <div style={{
                            ...styles.resultArea,
                            borderLeftColor: result.status === "Phishing" ? "#ef4444" : "#22c55e"
                        }}>
                            <div style={styles.statusRow}>
                                <div style={styles.statusText}>
                                    <span style={{
                                        ...styles.statusDot,
                                        backgroundColor: result.status === "Phishing" ? "#ef4444" : "#22c55e"
                                    }} />
                                    <span style={styles.statusLabel}>
                                        Result: <b>{result.status}</b>
                                    </span>
                                </div>
                                <span style={styles.confText}>
                                    Confidence Score: {(result.confidence * 100).toFixed(1)}%
                                </span>
                            </div>
                            <div style={styles.urlDisplay}>{result.url}</div>
                        </div>
                    )}

                    <footer style={styles.footer}>
                        <span>Academic Research Project</span>
                    </footer>
                </div>
            </main>

            <style>{`
                .btn-primary:hover { background-color: #1d4ed8 !important; transform: translateY(-1px); }
                .btn-primary:active { transform: translateY(0); }
                @keyframes spin { to { transform: rotate(360deg); } }
                @media (max-width: 600px) {
                    .input-group-responsive { flex-direction: column !important; }
                }
            `}</style>
        </div>
    );
}

const styles = {
    page: {
        minHeight: "100vh",
        background: "#f8fafc", // Professional Off-White
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "20px",
        fontFamily: "'Inter', system-ui, sans-serif",
    },
    main: { width: "100%", maxWidth: "650px" },
    card: {
        background: "#ffffff",
        borderRadius: "12px",
        padding: "40px",
        boxShadow: "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)",
        border: "1px solid #e2e8f0",
    },
    header: { marginBottom: "32px", textAlign: "left" },
    title: { fontSize: "28px", fontWeight: "800", color: "#0f172a", margin: 0 }, // Off-Black
    tagline: { color: "#64748b", fontSize: "15px", marginTop: "4px" },

    inputGroup: {
        display: "flex",
        gap: "12px",
        marginBottom: "24px",
        flexWrap: "wrap", // Basic responsiveness
    },
    input: {
        flex: 1,
        minWidth: "250px",
        background: "#fff",
        border: "1px solid #cbd5e1",
        padding: "14px 18px",
        borderRadius: "8px",
        fontSize: "16px",
        outline: "none",
        color: "#1e293b",
        transition: "border-color 0.2s",
    },
    button: {
        background: "#2563eb", // Professional Blue
        color: "#fff",
        border: "none",
        padding: "14px 28px",
        borderRadius: "8px",
        fontSize: "16px",
        fontWeight: "600",
        cursor: "pointer",
        transition: "all 0.2s",
    },
    buttonDisabled: { background: "#94a3b8", cursor: "not-allowed" },

    resultArea: {
        marginTop: "32px",
        padding: "24px",
        background: "#f1f5f9",
        borderRadius: "8px",
        borderLeft: "5px solid",
    },
    statusRow: { display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "10px" },
    statusText: { display: "flex", alignItems: "center", gap: "8px" },
    statusDot: { width: "10px", height: "10px", borderRadius: "50%" },
    statusLabel: { fontSize: "16px", color: "#334155" },
    confText: { fontSize: "14px", color: "#64748b", fontWeight: "500" },
    urlDisplay: { marginTop: "12px", fontSize: "13px", color: "#94a3b8", wordBreak: "break-all", fontStyle: "italic" },

    toastContainer: { position: "fixed", top: "24px", right: "24px", zIndex: 100 },
    toast: { padding: "12px 24px", borderRadius: "8px", color: "#fff", marginBottom: "8px", fontWeight: "500", fontSize: "14px" },
    error: { background: "#dc2626" },
    success: { background: "#16a34a" },
    warning: { background: "#d97706" },

    footer: {
        marginTop: "40px",
        display: "flex",
        justifyContent: "center",
        gap: "12px",
        fontSize: "12px",
        color: "#94a3b8",
        borderTop: "1px solid #f1f5f9",
        paddingTop: "20px"
    },
    spinner: {
        width: "18px", height: "18px", border: "2px solid rgba(255,255,255,0.3)",
        borderTopColor: "#fff", borderRadius: "50%", animation: "spin 0.8s linear infinite"
    }
};