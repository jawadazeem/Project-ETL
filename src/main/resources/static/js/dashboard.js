const GUEST_USER_ID = "00000000-0000-0000-0000-000000000001";

let providerChartInstance = null;
let alarmsChartInstance = null;
let currentPeriod = null;
let currentDatasetId = null;
let currentPageAllRecords = 0;
let currentPageFilterByProvider = 0;
let currentPageTopRecords = 0;
let activeDataTab = "all";
let currentRecordsPage = [];
let currentProviderPage = [];
let currentTopRecords = [];
let currentOrgContextDocuments = [];
const pageSize = 20;

const numberFormatter = new Intl.NumberFormat(undefined);
const currencyFormatter = new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: "USD"
});

function asArray(payload) {
    if (Array.isArray(payload)) return payload;
    return payload && Array.isArray(payload.content) ? payload.content : [];
}

function formatNumber(value) {
    return numberFormatter.format(Number(value || 0));
}

function formatCurrency(value) {
    return currencyFormatter.format(Number(value || 0));
}

// ── Welcome overlay ───────────────────────────────────────────────────────

function showWelcome() {
    document.getElementById("welcome-overlay").style.display = "flex";
}

function hideWelcome() {
    document.getElementById("welcome-overlay").style.display = "none";
}

function isWelcomeVisible() {
    return document.getElementById("welcome-overlay").style.display !== "none";
}

// ── Toast notifications ───────────────────────────────────────────────────

function showToast(message, type = "error") {
    const container = document.getElementById("toast-container");
    const toast = document.createElement("div");
    toast.className = `bp-toast bp-toast-${type}`;
    toast.innerHTML = `<span>${message}</span><button class="bp-toast-close" aria-label="Dismiss">✕</button>`;

    const closeBtn = toast.querySelector(".bp-toast-close");
    closeBtn.addEventListener("click", () => toast.remove());

    container.appendChild(toast);
    setTimeout(() => toast.remove(), 5000);
}

// ── Skeleton helpers ──────────────────────────────────────────────────────

function showSkeleton(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = "block";
}

function hideSkeleton(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = "none";
}

// ── Dataset switcher ──────────────────────────────────────────────────────

async function loadDatasetList() {
    try {
        const res = await fetch("/datasets", {
            headers: { "X-User-Id": GUEST_USER_ID }
        });
        if (!res.ok) return;

        const datasets = await res.json();
        if (!datasets.length) return;

        const select = document.getElementById("datasetSelect");
        select.innerHTML = "";

        datasets.forEach(ds => {
            const opt = document.createElement("option");
            opt.value = ds.id;
            const date = ds.uploadedAt ? new Date(ds.uploadedAt).toLocaleDateString() : "";
            opt.textContent = `${ds.sourceFilename}${date ? " · " + date : ""} (${ds.status})`;
            select.appendChild(opt);
        });

        if (currentDatasetId) {
            select.value = currentDatasetId;
        }

        const notReady = datasets.find(d => d.id === currentDatasetId && d.status !== "READY");
        select.classList.toggle("dataset-not-ready", !!notReady);

        document.getElementById("datasetSwitcherContainer").classList.remove("d-none");
    } catch (e) {
        console.error("Failed to load dataset list", e);
    }
}

async function switchDataset() {
    const select = document.getElementById("datasetSelect");
    currentDatasetId = select.value;
    currentPageAllRecords = 0;
    currentPageFilterByProvider = 0;
    currentPageTopRecords = 0;
    currentTopRecords = [];

    await loadPeriods();
    if (currentPeriod) {
        changePeriod();
    }
}

// ── Summary ───────────────────────────────────────────────────────────────

async function loadSummary() {
    if (!currentDatasetId || !currentPeriod) return;
    showSkeleton("skeleton-summary");
    document.getElementById("summaryStats").style.display = "none";

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/summary/periods/${currentPeriod}`);
        if (!res.ok) throw new Error(`Summary fetch failed: ${res.status}`);
        const s = await res.json();

        document.getElementById("statRecords").textContent = formatNumber(s.totalRecords);
        document.getElementById("statTotal").textContent = formatCurrency(s.totalCharges);
        document.getElementById("statAvg").textContent = formatCurrency(s.averageCharge);

        const hi = s.highestChargeRecord;
        document.getElementById("statHigh").textContent = hi ? formatCurrency(hi.totalCharge) : formatCurrency(0);
        document.getElementById("statHighName").textContent = hi ? hi.accountName : "N/A";
        document.getElementById("summaryStats").style.display = "flex";
    } catch (e) {
        console.error("Failed to load summary", e);
        showToast("Could not load billing summary.");
    } finally {
        hideSkeleton("skeleton-summary");
    }
}

// ── Charts ────────────────────────────────────────────────────────────────

function countBySeverity(alarms) {
    const counts = { LOW: 0, MEDIUM: 0, HIGH: 0, UNKNOWN: 0 };
    for (const alarm of (alarms || [])) {
        const severity = String(alarm.alarmSeverity || "UNKNOWN").toUpperCase();
        if (counts[severity] === undefined) {
            counts.UNKNOWN++;
        } else {
            counts[severity]++;
        }
    }
    return counts;
}

async function loadAlarmsChart() {
    if (!currentPeriod || !currentDatasetId) return;
    showSkeleton("skeleton-alarms-chart");

    try {
        if (typeof Chart === "undefined") {
            throw new Error("Chart.js is unavailable");
        }

        const alarms = await fetchAlarms();
        const counts = countBySeverity(alarms);

        if (alarmsChartInstance) {
            alarmsChartInstance.destroy();
        }

        const alarmColors = {
            LOW: "#facc15",
            MEDIUM: "#f97316",
            HIGH: "#ef4444",
            UNKNOWN: "#94a3b8"
        };
        const barColors = Object.keys(counts).map(k => alarmColors[k] || "#94a3b8");

        alarmsChartInstance = new Chart(document.getElementById("alarmsChart"), {
            type: "bar",
            data: {
                labels: Object.keys(counts),
                datasets: [{
                    label: "Alarm Count",
                    data: Object.values(counts),
                    backgroundColor: barColors,
                    borderRadius: 6,
                    borderSkipped: false
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: { enabled: true }
                },
                scales: {
                    y: { beginAtZero: true, ticks: { precision: 0 } },
                    x: { grid: { display: false } }
                }
            }
        });
    } catch (e) {
        console.error("Failed to load alarms chart", e);
        showToast("Could not load alarms chart.");
    } finally {
        hideSkeleton("skeleton-alarms-chart");
    }
}

async function loadProviderChart() {
    if (!currentDatasetId || !currentPeriod) return;
    showSkeleton("skeleton-provider-chart");

    try {
        if (typeof Chart === "undefined") {
            throw new Error("Chart.js is unavailable");
        }

        const res = await fetch(`/datasets/${currentDatasetId}/summary/periods/${encodeURIComponent(currentPeriod)}`);
        if (!res.ok) throw new Error(`Provider chart fetch failed: ${res.status}`);
        const data = await res.json();
        const totals = data.chargesByProvider || {};

        if (providerChartInstance) {
            providerChartInstance.destroy();
        }

        const providerPalette = [
            "#6366f1", "#8b5cf6", "#a78bfa", "#818cf8",
            "#7c3aed", "#6d28d9", "#5b21b6", "#4f46e5",
            "#4338ca", "#3730a3"
        ];
        const providerLabels = Object.keys(totals);
        const providerColors = providerLabels.map((_, i) => providerPalette[i % providerPalette.length]);

        providerChartInstance = new Chart(document.getElementById("providerChart"), {
            type: "bar",
            data: {
                labels: providerLabels,
                datasets: [{
                    label: "Total Charges ($)",
                    data: Object.values(totals),
                    backgroundColor: providerColors,
                    borderRadius: 6,
                    borderSkipped: false
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: ctx => `$${ctx.raw.toFixed(2)}`
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: v => `$${v.toLocaleString()}`
                        }
                    },
                    x: { grid: { display: false } }
                }
            }
        });
    } catch (e) {
        console.error("Failed to load provider chart", e);
        showToast("Could not load cloud provider charges chart.");
    } finally {
        hideSkeleton("skeleton-provider-chart");
    }
}

// ── Records ───────────────────────────────────────────────────────────────

function recordTitle(record) {
    return record.serviceName || record.resourceId || record.accountName || "Billing record";
}

function clearDescriptionPanel(message = "Select a billing record to inspect its description.") {
    document.getElementById("descriptionTitle").textContent = "No record selected";
    document.getElementById("descriptionText").textContent = message;
    document.getElementById("descriptionResource").textContent = "--";
    document.getElementById("descriptionProvider").textContent = "--";
    document.getElementById("descriptionCharge").textContent = "--";
    document.getElementById("descriptionAccount").textContent = "--";
}

function selectRecord(record, row) {
    document.querySelectorAll(".selectable-row.selected-row").forEach(el => {
        el.classList.remove("selected-row");
        el.removeAttribute("aria-selected");
    });

    if (row) {
        row.classList.add("selected-row");
        row.setAttribute("aria-selected", "true");
    }

    document.getElementById("descriptionTitle").textContent = recordTitle(record);
    document.getElementById("descriptionText").textContent =
        record.description || "No description is available for this billing record.";
    document.getElementById("descriptionResource").textContent = record.resourceId || "--";
    document.getElementById("descriptionProvider").textContent = record.cloudProvider || "--";
    document.getElementById("descriptionCharge").textContent = formatCurrency(record.totalCharge);
    document.getElementById("descriptionAccount").textContent = record.accountName || "--";
}

function selectFirstVisibleRecord(tableSelector, records) {
    if (!records.length) {
        clearDescriptionPanel("No records are available for this view.");
        return;
    }

    requestAnimationFrame(() => {
        const firstRow = document.querySelector(`${tableSelector} tbody .selectable-row`);
        selectRecord(records[0], firstRow);
    });
}

function renderSelectableRows(tableSelector, records) {
    const tbody = document.querySelector(`${tableSelector} tbody`);
    tbody.innerHTML = "";

    if (records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">No records found.</td></tr>';
        return;
    }

    records.forEach(record => {
        const row = document.createElement("tr");
        row.className = "selectable-row";
        row.tabIndex = 0;
        row.setAttribute("role", "button");
        row.innerHTML = `
            <td>${escapeForHtml(record.serviceName || "")}</td>
            <td>${escapeForHtml(record.cloudProvider || "")}</td>
            <td>${formatCurrency(record.totalCharge)}</td>
            <td>${escapeForHtml(record.accountName || "")}</td>
        `;
        row.addEventListener("click", () => selectRecord(record, row));
        row.addEventListener("keydown", (e) => {
            if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                selectRecord(record, row);
            }
        });
        tbody.appendChild(row);
    });
}

async function loadRecords() {
    if (!currentDatasetId || !currentPeriod) return;
    showSkeleton("skeleton-records");

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/records/periods/${currentPeriod}?page=${currentPageAllRecords}&size=${pageSize}`);
        if (!res.ok) throw new Error(`Records fetch failed: ${res.status}`);
        const data = await res.json();
        const records = data.content || [];
        const totalPages = data.totalPages || 1;

        currentRecordsPage = records;
        renderSelectableRows("#recordsTable", records);
        if (activeDataTab === "all") selectFirstVisibleRecord("#recordsTable", records);

        document.getElementById("pageInfoAllRecords").textContent = `Page ${currentPageAllRecords + 1} of ${totalPages}`;
        document.getElementById("prevBtnAllRecords").disabled = currentPageAllRecords <= 0;
        document.getElementById("nextBtnAllRecords").disabled = currentPageAllRecords >= totalPages - 1;
    } catch (e) {
        console.error("Failed to load records", e);
        showToast("Could not load billing records.");
    } finally {
        hideSkeleton("skeleton-records");
    }
}

async function loadByProvider() {
    if (!currentDatasetId) return;
    const provider = document.getElementById("providerSelect").value;
    if (!provider) {
        currentProviderPage = [];
        renderSelectableRows("#providerTable", []);
        document.getElementById("pageInfoFilterByProvider").textContent = "Page 1";
        document.getElementById("prevBtnFilterByProviders").disabled = true;
        document.getElementById("nextBtnFilterByProviders").disabled = true;
        if (activeDataTab === "provider") clearDescriptionPanel("No cloud providers are available for this dataset.");
        return;
    }
    showSkeleton("skeleton-provider-records");

    try {
        const params = new URLSearchParams({
            page: currentPageFilterByProvider,
            size: pageSize
        });

        if (currentPeriod) {
            params.set("billingPeriod", currentPeriod);
        }

        const res = await fetch(`/datasets/${currentDatasetId}/records/providers/${encodeURIComponent(provider)}?${params}`);
        if (!res.ok) throw new Error(`Provider records fetch failed: ${res.status}`);
        const data = await res.json();
        const records = data.content || [];
        const totalPages = data.totalPages || 1;

        currentProviderPage = records;
        renderSelectableRows("#providerTable", records);
        if (activeDataTab === "provider") selectFirstVisibleRecord("#providerTable", records);

        document.getElementById("pageInfoFilterByProvider").textContent =
            `Page ${currentPageFilterByProvider + 1} of ${totalPages}`;
        document.getElementById("prevBtnFilterByProviders").disabled = currentPageFilterByProvider <= 0;
        document.getElementById("nextBtnFilterByProviders").disabled = currentPageFilterByProvider >= totalPages - 1;
    } catch (e) {
        console.error("Failed to load provider records", e);
        showToast("Could not load provider records.");
    } finally {
        hideSkeleton("skeleton-provider-records");
    }
}

async function loadTopN() {
    if (!currentDatasetId) return;
    const n = document.getElementById("topInput").value.trim();
    if (!n) return;
    showSkeleton("skeleton-top");

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/top/${n}`);
        if (!res.ok) throw new Error(`Top N fetch failed: ${res.status}`);
        const data = await res.json();
        const records = Array.isArray(data) ? data : (data.content || []);

        currentTopRecords = records;
        currentPageTopRecords = 0;
        renderTopRecordsPage();
    } catch (e) {
        console.error("Failed to load top N", e);
        showToast("Could not load top N records.");
    } finally {
        hideSkeleton("skeleton-top");
    }
}

function renderTopRecordsPage() {
    const totalPages = Math.max(1, Math.ceil(currentTopRecords.length / pageSize));
    currentPageTopRecords = Math.min(currentPageTopRecords, totalPages - 1);

    const start = currentPageTopRecords * pageSize;
    const records = currentTopRecords.slice(start, start + pageSize);

    renderSelectableRows("#topOutput", records);
    if (activeDataTab === "top") selectFirstVisibleRecord("#topOutput", records);

    document.getElementById("pageInfoTopRecords").textContent = `Page ${currentPageTopRecords + 1} of ${totalPages}`;
    document.getElementById("prevBtnTopRecords").disabled = currentPageTopRecords <= 0;
    document.getElementById("nextBtnTopRecords").disabled = currentPageTopRecords >= totalPages - 1;
}

function loadActiveDataTab() {
    if (activeDataTab === "provider") {
        currentPageFilterByProvider = 0;
        loadByProvider();
        return;
    }

    if (activeDataTab === "top") {
        renderTopRecordsPage();
        return;
    }

    loadRecords();
}

function setActiveDataTab(tab) {
    activeDataTab = tab;

    document.querySelectorAll(".data-tab").forEach(button => {
        const isActive = button.dataset.tab === tab;
        button.classList.toggle("active", isActive);
        button.setAttribute("aria-selected", String(isActive));
    });

    document.querySelectorAll(".data-panel").forEach(panel => {
        panel.classList.toggle("active", panel.dataset.panel === tab);
    });

    if (tab === "all") {
        if (currentRecordsPage.length) {
            selectFirstVisibleRecord("#recordsTable", currentRecordsPage);
        } else {
            loadRecords();
        }
    } else if (tab === "provider") {
        currentPageFilterByProvider = 0;
        loadByProvider();
    } else if (tab === "top") {
        if (currentTopRecords.length) {
            renderTopRecordsPage();
        } else {
            renderTopRecordsPage();
            clearDescriptionPanel("Load top records to inspect their descriptions.");
        }
    }
}

// ── Periods & providers ─────────────────────────────────────────────────

async function loadPeriods() {
    if (!currentDatasetId) return;

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/records/periods`);
        if (!res.ok) throw new Error(`Periods fetch failed: ${res.status}`);
        const data = await res.json();
        const periods = asArray(data);
        const select = document.getElementById("periodSelect");
        select.innerHTML = "";

        currentPeriod = null;
        periods.forEach((period, i) => {
            const opt = document.createElement("option");
            opt.value = period;
            opt.textContent = period;
            select.appendChild(opt);

            if (i === periods.length - 1) {
                currentPeriod = period;
            }
        });

        select.value = currentPeriod;
    } catch (e) {
        console.error("Failed to load periods", e);
        showToast("Could not load billing periods.");
    }
}

async function loadProviders() {
    if (!currentDatasetId || !currentPeriod) return;

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/records/providers`);
        if (!res.ok) return;
        const providers = asArray(await res.json());

        const seen = new Set();
        const select = document.getElementById("providerSelect");
        select.innerHTML = "";

        providers.forEach(provider => {
            if (provider && !seen.has(provider)) {
                seen.add(provider);
                const opt = document.createElement("option");
                opt.value = provider;
                opt.textContent = provider;
                select.appendChild(opt);
            }
        });
    } catch (e) {
        console.error("Failed to load providers", e);
    }
}

// ── Alarms ────────────────────────────────────────────────────────────────

async function fetchAlarms() {
    if (!currentPeriod || !currentDatasetId) return [];
    const res = await fetch(`/datasets/${currentDatasetId}/alarms/${encodeURIComponent(currentPeriod)}`);
    if (!res.ok) throw new Error("Failed to load alarms");
    return asArray(await res.json());
}

function renderAlarms(alarms) {
    const list = document.getElementById("alarms-list");
    const empty = document.getElementById("alarms-empty");
    list.innerHTML = "";

    if (!alarms || alarms.length === 0) {
        empty.style.display = "block";
        return;
    }

    empty.style.display = "none";

    for (const alarm of alarms) {
        const severity = (alarm.alarmSeverity || "").toLowerCase();
        const type = alarm.alarmType || "Alarm";
        const resource = alarm.resourceId || "--";
        const service = alarm.serviceName || "--";
        const period = alarm.billingPeriod || "--";
        const explanation = alarm.explanation || "";

        const li = document.createElement("li");
        li.className = "alarm-item";
        li.innerHTML = `
            <div class="alarm-left">
                <div class="alarm-title">${type}</div>
                <div class="alarm-meta">Resource: ${resource} &bull; Service: ${service} &bull; Period: ${period}</div>
                <div class="alarm-explain">${explanation}</div>
            </div>
            <div class="badge ${severity}">${alarm.alarmSeverity || "UNKNOWN"}</div>
        `;

        list.appendChild(li);
    }
}

function openAlarmsModal() {
    document.getElementById("alarms-overlay").style.display = "flex";
}

function closeAlarmsModal() {
    document.getElementById("alarms-overlay").style.display = "none";
}

async function onAlarmsClick() {
    openAlarmsModal();
    try {
        const alarms = await fetchAlarms();
        document.getElementById("alarms-title").textContent = `Alarms (${alarms.length})`;
        renderAlarms(alarms);
        document.getElementById("alarms-btn").textContent = `Alarms (${alarms.length})`;
    } catch (e) {
        console.error(e);
        renderAlarms([]);
    }
}

// ── CSV Export ───────────────────────────────────────────────────────────

function csvCell(value) {
    return `"${String(value ?? "").replace(/"/g, '""')}"`;
}

function downloadCsv(filename, records) {
    if (!records.length) {
        showToast("There are no records to export.", "info");
        return;
    }

    const columns = [
        "accountName",
        "resourceId",
        "cloudProvider",
        "billingPeriod",
        "computeHours",
        "storageGbUsed",
        "apiRequests",
        "totalCharge",
        "serviceName",
        "description"
    ];

    const lines = [
        columns.join(","),
        ...records.map(record => columns.map(column => csvCell(record[column])).join(","))
    ];

    const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
}

function exportRecordsCsv() {
    if (!currentDatasetId || !currentPeriod) {
        showToast("Select a billing period first.");
        return;
    }
    window.location.href = `/datasets/${currentDatasetId}/records/export?billingPeriod=${encodeURIComponent(currentPeriod)}`;
}

async function fetchProviderRecordsForExport(provider) {
    const exportedRecords = [];
    let page = 0;
    let totalPages = 1;

    do {
        const params = new URLSearchParams({
            page,
            size: 100
        });

        if (currentPeriod) {
            params.set("billingPeriod", currentPeriod);
        }

        const res = await fetch(`/datasets/${currentDatasetId}/records/providers/${encodeURIComponent(provider)}?${params}`);
        if (!res.ok) throw new Error(`Provider export failed: ${res.status}`);

        const data = await res.json();
        exportedRecords.push(...(data.content || []));
        totalPages = data.totalPages || 1;
        page += 1;
    } while (page < totalPages);

    return exportedRecords;
}

async function exportActiveDataTabCsv() {
    if (activeDataTab === "all") {
        exportRecordsCsv();
        return;
    }

    if (!currentDatasetId) {
        showToast("Select a dataset first.");
        return;
    }

    if (activeDataTab === "provider") {
        const provider = document.getElementById("providerSelect").value;
        if (!provider) {
            showToast("Select a cloud provider first.");
            return;
        }

        try {
            const records = await fetchProviderRecordsForExport(provider);
            downloadCsv(`billing-records-${provider}-${currentPeriod || "all-periods"}.csv`, records);
        } catch (e) {
            console.error("Provider export failed", e);
            showToast("Could not export provider records.");
        }
        return;
    }

    downloadCsv(`top-billing-records-${currentPeriod || "all-periods"}.csv`, currentTopRecords);
}

function exportAlarmsCsv() {
    if (!currentDatasetId || !currentPeriod) {
        showToast("Select a billing period first.");
        return;
    }
    window.location.href = `/datasets/${currentDatasetId}/alarms/${encodeURIComponent(currentPeriod)}/export`;
}

async function loadAlarmsCount() {
    if (!currentPeriod || !currentDatasetId) return;

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/alarms/${encodeURIComponent(currentPeriod)}`);
        if (!res.ok) throw new Error(`Alarms count fetch failed: ${res.status}`);
        const alarms = asArray(await res.json());
        const count = alarms.length;
        const btn = document.getElementById("alarms-btn");

        if (btn) {
            btn.textContent = `Alarms (${count})`;
            btn.style.opacity = count === 0 ? "0.4" : "1";
        }
    } catch (e) {
        console.error("Failed to load alarms count", e);
    }
}

// ── Chat ──────────────────────────────────────────────────────────────────

function appendChatMessage(sender, text) {
    const chat = document.getElementById("chatWindow");
    const empty = document.getElementById("chatEmpty");
    if (empty) empty.style.display = "none";

    const wrapper = document.createElement("div");
    wrapper.className = `chat-message ${sender === "user" ? "user" : "bot"}`;

    const bubble = document.createElement("div");
    bubble.className = "chat-bubble";
    bubble.textContent = text;

    wrapper.appendChild(bubble);
    chat.appendChild(wrapper);
    chat.scrollTop = chat.scrollHeight;
}

function showTypingIndicator() {
    const chat = document.getElementById("chatWindow");
    const wrapper = document.createElement("div");
    wrapper.className = "chat-message bot";
    wrapper.id = "typing-indicator";
    wrapper.innerHTML = '<div class="chat-typing"><span></span><span></span><span></span></div>';
    chat.appendChild(wrapper);
    chat.scrollTop = chat.scrollHeight;
}

function hideTypingIndicator() {
    const el = document.getElementById("typing-indicator");
    if (el) el.remove();
}

function setTracePrompt(prompt) {
    const input = document.getElementById("chatInput");
    if (!input) return;

    input.value = prompt;
    input.focus();
}

function showBackendPlaceholder(featureName) {
    showToast(`${featureName} is a frontend placeholder until the backend workflow is implemented.`, "info");
}

async function sendChat() {
    if (!currentDatasetId) return;
    const input = document.getElementById("chatInput");
    const sendBtn = document.getElementById("chatSendBtn");
    const text = input.value.trim();
    if (!text) return;

    appendChatMessage("user", text);
    input.value = "";
    sendBtn.disabled = true;
    showTypingIndicator();

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/trace`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ prompt: text, period: currentPeriod })
        });

        hideTypingIndicator();

        if (!res.ok) {
            appendChatMessage("bot", `Server error: ${res.status}`);
            return;
        }

        const data = await res.json();
        let msg = (data && (data.answer || data.reply)) || "No response returned.";

        if (data.sql) msg += `\n\nSQL:\n${data.sql}`;
        if (data.reasoning) msg += `\n\nWhy:\n${data.reasoning}`;

        appendChatMessage("bot", msg);
    } catch (e) {
        hideTypingIndicator();
        console.error("Chat send failed", e);
        appendChatMessage("bot", "Failed to send message — the chat service may be unavailable.");
        showToast("Trace is unavailable. Check the connection.");
    } finally {
        sendBtn.disabled = false;
    }
}

// ── Help modal ────────────────────────────────────────────────────────────

function closeHelpModal() {
    document.getElementById("help-overlay").style.display = "none";
}

function openHelpModal() {
    document.getElementById("help-overlay").style.display = "flex";
}

function openInfo() {
    window.Blueprint.openInfoPopup();
}

function openOrgContextModal() {
    document.getElementById("org-context-overlay").style.display = "flex";
    renderOrgContextFileList();
    loadOrgContextDocuments();
}

function closeOrgContextModal() {
    document.getElementById("org-context-overlay").style.display = "none";
}

function formatFileSize(bytes) {
    if (!bytes) return "0 KB";
    const sizeInKb = bytes / 1024;
    if (sizeInKb < 1024) {
        return `${sizeInKb.toFixed(1)} KB`;
    }
    return `${(sizeInKb / 1024).toFixed(1)} MB`;
}

function renderOrgContextFileList() {
    const input = document.getElementById("orgContextFiles");
    const list = document.getElementById("orgContextFileList");
    if (!input || !list) return;

    const files = Array.from(input.files || []);
    if (!files.length) {
        list.innerHTML = '<li class="text-muted">No files selected.</li>';
        return;
    }

    list.innerHTML = files.map(file => `
        <li>
            <strong>${escapeForHtml(file.name)}</strong>
            <span>${formatFileSize(file.size)}</span>
        </li>
    `).join("");
}

function renderOrgContextDocumentList() {
    const list = document.getElementById("orgContextDocumentList");
    if (!list) return;

    if (!currentOrgContextDocuments.length) {
        list.innerHTML = '<li class="text-muted">No organization context files uploaded yet.</li>';
        return;
    }

    list.innerHTML = currentOrgContextDocuments.map(doc => {
        const uploadedAt = doc.uploadedAt ? new Date(doc.uploadedAt).toLocaleString() : "Uploaded";
        return `
            <li>
                <div>
                    <strong>${escapeForHtml(doc.sourceFilename || "Untitled context file")}</strong>
                    <span>${escapeForHtml(uploadedAt)}</span>
                </div>
                <button class="btn btn-sm btn-outline-danger org-context-delete-btn" type="button" data-document-id="${escapeForHtml(doc.id)}">
                    Delete
                </button>
            </li>
        `;
    }).join("");
}

async function loadOrgContextDocuments() {
    const list = document.getElementById("orgContextDocumentList");
    if (list) {
        list.innerHTML = '<li class="text-muted">Loading uploaded files...</li>';
    }

    try {
        const res = await fetch("/org-contexts", {
            headers: { "X-User-Id": GUEST_USER_ID }
        });

        if (!res.ok) {
            throw new Error(`Org context list failed: ${res.status}`);
        }

        currentOrgContextDocuments = await res.json();
        renderOrgContextDocumentList();
    } catch (e) {
        console.error("Failed to load organization context documents", e);
        currentOrgContextDocuments = [];
        if (list) {
            list.innerHTML = '<li class="text-muted">Could not load uploaded files.</li>';
        }
        showToast("Could not load organization context files.");
    }
}

async function uploadOrgContextFiles() {
    const input = document.getElementById("orgContextFiles");
    const uploadBtn = document.getElementById("orgContextStageBtn");
    const status = document.getElementById("orgContextUploadStatus");
    const files = Array.from(input?.files || []);

    if (!files.length) {
        showToast("Choose one or more organization context files first.", "info");
        return;
    }

    uploadBtn.disabled = true;
    uploadBtn.textContent = "Uploading...";
    if (status) {
        status.textContent = `Uploading ${files.length} file${files.length === 1 ? "" : "s"}...`;
    }

    let uploadedCount = 0;

    try {
        for (const file of files) {
            const formData = new FormData();
            formData.append("file", file);

            const res = await fetch("/org-contexts", {
                method: "POST",
                headers: { "X-User-Id": GUEST_USER_ID },
                body: formData
            });

            if (!res.ok) {
                const errorText = await res.text();
                throw new Error(errorText || `Upload failed with status ${res.status}`);
            }

            uploadedCount++;
            if (status) {
                status.textContent = `Uploaded ${uploadedCount} of ${files.length} file${files.length === 1 ? "" : "s"}...`;
            }
        }

        input.value = "";
        renderOrgContextFileList();
        await loadOrgContextDocuments();
        showToast(`Uploaded ${uploadedCount} organization context file${uploadedCount === 1 ? "" : "s"}.`, "success");
        if (status) {
            status.textContent = "Upload complete.";
        }
    } catch (e) {
        console.error("Organization context upload failed", e);
        showToast(`Organization context upload failed: ${e.message || "Unknown error"}`);
        if (status) {
            status.textContent = "Upload failed.";
        }
    } finally {
        uploadBtn.disabled = false;
        uploadBtn.textContent = "Upload Files";
    }
}

async function deleteOrgContextDocument(documentId) {
    if (!documentId) return;

    if (!confirm("Delete this organization context file?")) {
        return;
    }

    try {
        const res = await fetch(`/org-contexts/${encodeURIComponent(documentId)}`, {
            method: "DELETE",
            headers: { "X-User-Id": GUEST_USER_ID }
        });

        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || `Delete failed with status ${res.status}`);
        }

        currentOrgContextDocuments = currentOrgContextDocuments.filter(doc => doc.id !== documentId);
        renderOrgContextDocumentList();
        showToast("Organization context file deleted.", "success");
    } catch (e) {
        console.error("Organization context delete failed", e);
        showToast("Could not delete organization context file.");
    }
}

function runBillingQueryPlaceholder() {
    const query = document.getElementById("billingQueryInput")?.value.trim();
    if (!query) {
        showToast("Enter a billing query first.", "info");
        return;
    }

    showBackendPlaceholder("Athena billing query");
}

function setPatternExample(pattern) {
    const input = document.getElementById("patternSearchInput");
    if (!input) return;

    input.value = pattern;
    input.focus();
}

function runPatternSearchPlaceholder() {
    const pattern = document.getElementById("patternSearchInput")?.value.trim();
    if (!pattern) {
        showToast("Enter a Ptern pattern first.", "info");
        return;
    }

    showBackendPlaceholder("Advanced Ptern pattern search");
}

// ── Upload & demo load ────────────────────────────────────────────────────

async function uploadFile() {
    const fileInput = document.getElementById("fileInput");
    const uploadBtn = document.getElementById("uploadBtn");

    if (fileInput.files.length === 0) {
        alert("Please select a CSV file first.");
        return;
    }

    const file = fileInput.files[0];
    const formData = new FormData();
    formData.append("file", file);

    uploadBtn.disabled = true;
    uploadBtn.textContent = "Uploading...";

    try {
        const response = await fetch("/datasets", {
            method: "POST",
            headers: { "X-User-Id": GUEST_USER_ID },
            body: formData
        });

        if (response.ok) {
            const dataset = await response.json();
            currentDatasetId = dataset.id;
            fileInput.value = "";
            hideWelcome();

            setTimeout(async () => {
                await waitForDataReady();
                await loadPeriods();
                await loadProviders();
                await loadDatasetList();
                changePeriod();
            }, 2000);
        } else {
            const errorText = await response.text();
            showToast(`Upload failed: ${errorText || response.status}`);
        }
    } catch (error) {
        console.error("Upload error:", error);
        showToast("Upload failed. Check your connection and try again.");
    } finally {
        uploadBtn.disabled = false;
        uploadBtn.textContent = "Upload";
    }
}

async function loadDummyData() {
    const btn = document.getElementById("loadDummyBtn");
    if (!btn) return;

    btn.disabled = true;
    btn.textContent = "Loading...";

    try {
        const res = await fetch("/demo-dataset", { method: "POST" });
        if (!res.ok) {
            const txt = await res.text();
            showToast(`Failed to load demo data: ${txt || res.status}`);
            return;
        }

        currentDatasetId = "00000000-0000-0000-0000-000000000000";
        hideWelcome();

        await waitForDataReady();
        await loadPeriods();
        await loadProviders();
        await loadDatasetList();
        changePeriod();
        closeHelpModal();
    } catch (e) {
        console.error("loadDummyData failed", e);
        showToast("Failed to load demo data. See console for details.");
    } finally {
        btn.disabled = false;
        btn.textContent = "Load Dummy Data";
    }
}

async function waitForDataReady() {
    if (!currentDatasetId) return;
    for (let i = 0; i < 10; i++) {
        try {
            const res = await fetch(`/datasets/${currentDatasetId}/records/periods`);
            const data = await res.json();
            if (asArray(data).length > 0) return;
        } catch (_) {
            // not ready yet
        }
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
}

// ── Period & page controls ────────────────────────────────────────────────

function changePeriod() {
    currentPeriod = document.getElementById("periodSelect").value;
    currentPageAllRecords = 0;
    currentPageFilterByProvider = 0;
    currentPageTopRecords = 0;
    currentTopRecords = [];
    loadSummary();
    loadActiveDataTab();
    loadProviderChart();
    loadAlarmsCount();
    loadAlarmsChart();
}

function changePageAllRecords(step) {
    currentPageAllRecords = Math.max(0, currentPageAllRecords + step);
    loadRecords();
}

function changePageFilterByProvider(step) {
    currentPageFilterByProvider = Math.max(0, currentPageFilterByProvider + step);
    loadByProvider();
}

function changePageTopRecords(step) {
    currentPageTopRecords = Math.max(0, currentPageTopRecords + step);
    renderTopRecordsPage();
}

// ── PDF Report ──────────────────────────────────────────────────────────

async function onGeneratePdfClick() {
    if (!currentDatasetId || !currentPeriod) {
        showToast("Select a dataset and billing period first.", "error");
        return;
    }

    try {
        const res = await fetch(`/users/${GUEST_USER_ID}/corporate-info`);

        if (res.status === 404) {
            openCorporateInfoModal();
            return;
        }

        if (!res.ok) throw new Error("Failed to check corporate info");
        await generatePdf();
    } catch (e) {
        console.error("PDF flow error", e);
        showToast("Could not start PDF generation.");
    }
}

function openCorporateInfoModal() {
    document.getElementById("corporate-info-overlay").style.display = "flex";
}

function closeCorporateInfoModal() {
    document.getElementById("corporate-info-overlay").style.display = "none";
}

async function saveCorporateInfoAndGenerate() {
    const companyName = document.getElementById("corpCompanyName").value.trim();
    if (!companyName) {
        showToast("Company name is required.", "error");
        return;
    }

    const body = {
        companyName,
        addressLine1: document.getElementById("corpAddress1").value.trim(),
        addressLine2: document.getElementById("corpAddress2").value.trim(),
        city: document.getElementById("corpCity").value.trim(),
        state: document.getElementById("corpState").value.trim(),
        zipCode: document.getElementById("corpZip").value.trim(),
        phone: document.getElementById("corpPhone").value.trim(),
        email: document.getElementById("corpEmail").value.trim()
    };

    try {
        const res = await fetch(`/users/${GUEST_USER_ID}/corporate-info`, {
            method: "PUT",
            headers: { "Content-Type": "application/json", "X-User-Id": GUEST_USER_ID },
            body: JSON.stringify(body)
        });

        if (!res.ok) throw new Error("Failed to save corporate info");

        closeCorporateInfoModal();
        showToast("Company info saved.", "success");
        await generatePdf();
    } catch (e) {
        console.error("Save corporate info failed", e);
        showToast("Could not save company information.");
    }
}

async function generatePdf() {
    const btn = document.getElementById("generate-pdf-btn");
    btn.disabled = true;
    btn.textContent = "Generating...";

    try {
        const res = await fetch(
            `/datasets/${currentDatasetId}/reports/pdf?period=${encodeURIComponent(currentPeriod)}`,
            {
                method: "POST",
                headers: { "X-User-Id": GUEST_USER_ID }
            }
        );

        if (!res.ok) throw new Error(`PDF generation failed: ${res.status}`);

        const report = await res.json();
        showToast("PDF generated successfully.", "success");

        const a = document.createElement("a");
        a.href = `/datasets/${currentDatasetId}/reports/pdf/${report.id}`;
        a.download = `billing-report-${currentPeriod}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
    } catch (e) {
        console.error("PDF generation failed", e);
        showToast("Could not generate PDF report.");
    } finally {
        btn.disabled = false;
        btn.textContent = "Generate PDF";
    }
}

// ── Notifications modal ──────────────────────────────────────────────────

function openNotificationsModal() {
    document.getElementById("notifications-overlay").style.display = "flex";
    loadNotifications();
}

function closeNotificationsModal() {
    document.getElementById("notifications-overlay").style.display = "none";
}

async function loadNotifications() {
    const rows = document.getElementById("notification-rows");
    rows.innerHTML = `<tr><td colspan="5" class="text-muted text-center">Loading notifications...</td></tr>`;

    try {
        const res = await fetch("/api/notifications?limit=50");
        if (!res.ok) throw new Error(`Request failed with ${res.status}`);

        const notifications = await res.json();

        const deliveries = notifications.flatMap(n => n.deliveries || []);
        document.getElementById("notif-sent-count").textContent =
            deliveries.filter(d => d.status === "succeeded").length;
        document.getElementById("notif-failed-count").textContent =
            deliveries.filter(d => d.status === "failed").length;
        document.getElementById("notif-alarm-count").textContent = notifications.length;

        if (!notifications.length) {
            rows.innerHTML = `<tr><td colspan="5" class="text-muted text-center">No notifications have been sent yet.</td></tr>`;
            return;
        }

        rows.innerHTML = notifications.map(n => {
            const dels = n.deliveries || [];
            const recipients = dels.map(d => `${d.channel}: ${escapeForHtml(d.recipient)}`).join("<br>");
            const failed = dels.some(d => d.status === "failed");
            const statusClass = failed ? "text-danger" : "text-success";
            const statusLabel = failed ? "Failed" : "Sent";
            const severityBadge = `<span class="badge ${n.severity?.toLowerCase()}">${n.severity}</span>`;
            const sentDate = n.createdAt ? new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(n.createdAt)) : "";

            return `<tr>
                <td><strong>${escapeForHtml(n.title)}</strong><br><small class="text-muted">${escapeForHtml(n.alarmId)}</small></td>
                <td>${severityBadge}</td>
                <td>${recipients || "None"}</td>
                <td><span class="${statusClass}" style="font-weight:700;">${statusLabel}</span></td>
                <td>${sentDate}</td>
            </tr>`;
        }).join("");
    } catch (e) {
        console.error("Failed to load notifications", e);
        rows.innerHTML = `<tr><td colspan="5" class="text-muted text-center">Could not load notifications.</td></tr>`;
    }
}

function escapeForHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// ── Delete dataset ──────────────────────────────────────────────────────

async function onDeleteDatasetClick() {
    if (!currentDatasetId) {
        showToast("No dataset selected.");
        return;
    }

    if (!confirm("Are you sure you want to permanently delete this dataset?\n\nThis will remove all billing records, alarms, and PDF reports associated with it. This action cannot be undone.")) {
        return;
    }

    const btn = document.getElementById("delete-dataset-btn");
    btn.disabled = true;

    try {
        const res = await fetch(`/datasets/${currentDatasetId}`, {
            method: "DELETE",
            headers: { "X-User-Id": GUEST_USER_ID }
        });

        if (!res.ok) throw new Error(`Delete failed: ${res.status}`);

        showToast("Dataset deleted successfully.", "success");
        currentDatasetId = null;
        currentPeriod = null;

        await loadDatasetList();
        const hasDatasets = await tryLoadExistingDatasets();
        if (!hasDatasets) {
            showWelcome();
        }
    } catch (e) {
        console.error("Delete dataset failed", e);
        showToast("Could not delete dataset.");
    } finally {
        btn.disabled = false;
    }
}

// ── Archive dataset ─────────────────────────────────────────────────────

async function onArchiveDatasetClick() {
    if (!currentDatasetId) {
        showToast("No dataset selected.");
        return;
    }

    if (!confirm("Archive this dataset?\n\nThe dataset will be hidden from the active view but not deleted. You can restore it later from the archived datasets view.")) {
        return;
    }

    const btn = document.getElementById("archive-dataset-btn");
    btn.disabled = true;

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/archive`, {
            method: "PATCH",
            headers: { "X-User-Id": GUEST_USER_ID }
        });

        if (!res.ok) throw new Error(`Archive failed: ${res.status}`);

        showToast("Dataset archived successfully.", "success");
        currentDatasetId = null;
        currentPeriod = null;

        await loadDatasetList();
        const hasDatasets = await tryLoadExistingDatasets();
        if (!hasDatasets) {
            showWelcome();
        }
    } catch (e) {
        console.error("Archive dataset failed", e);
        showToast("Could not archive dataset.");
    } finally {
        btn.disabled = false;
    }
}

// ── Archived datasets view ──────────────────────────────────────────────

let viewingArchived = false;

function toggleArchivedView() {
    viewingArchived = !viewingArchived;
    const toggleBtn = document.getElementById("toggle-archived-btn");
    toggleBtn.textContent = viewingArchived ? "Show Active" : "Show Archived";

    document.getElementById("delete-dataset-btn").style.display = viewingArchived ? "none" : "";
    document.getElementById("archive-dataset-btn").style.display = viewingArchived ? "none" : "";
    document.getElementById("generate-pdf-btn").style.display = viewingArchived ? "none" : "";
    document.getElementById("restore-dataset-btn").style.display = viewingArchived ? "" : "none";

    if (viewingArchived) {
        loadArchivedDatasetList();
    } else {
        loadDatasetList();
        tryLoadExistingDatasets();
    }
}

async function loadArchivedDatasetList() {
    try {
        const res = await fetch("/datasets/archived", {
            headers: { "X-User-Id": GUEST_USER_ID }
        });
        if (!res.ok) return;

        const datasets = await res.json();
        const select = document.getElementById("datasetSelect");
        select.innerHTML = "";

        if (!datasets.length) {
            const opt = document.createElement("option");
            opt.textContent = "No archived datasets";
            opt.disabled = true;
            select.appendChild(opt);
            currentDatasetId = null;
            return;
        }

        datasets.forEach(ds => {
            const opt = document.createElement("option");
            opt.value = ds.id;
            const date = ds.uploadedAt ? new Date(ds.uploadedAt).toLocaleDateString() : "";
            opt.textContent = `[Archived] ${ds.sourceFilename}${date ? " · " + date : ""}`;
            select.appendChild(opt);
        });

        currentDatasetId = datasets[0].id;
        select.value = currentDatasetId;
    } catch (e) {
        console.error("Failed to load archived datasets", e);
    }
}

async function onRestoreDatasetClick() {
    if (!currentDatasetId) {
        showToast("No dataset selected.");
        return;
    }

    const btn = document.getElementById("restore-dataset-btn");
    btn.disabled = true;

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/restore`, {
            method: "PATCH",
            headers: { "X-User-Id": GUEST_USER_ID }
        });

        if (!res.ok) throw new Error(`Restore failed: ${res.status}`);

        showToast("Dataset restored successfully.", "success");
        await loadArchivedDatasetList();
    } catch (e) {
        console.error("Restore dataset failed", e);
        showToast("Could not restore dataset.");
    } finally {
        btn.disabled = false;
    }
}

// ── Event wiring ──────────────────────────────────────────────────────────

function wireDashboardEvents() {
    document.getElementById("periodSelect")?.addEventListener("change", changePeriod);
    document.getElementById("datasetSelect")?.addEventListener("change", switchDataset);
    document.getElementById("uploadBtn")?.addEventListener("click", uploadFile);
    document.getElementById("fileInput")?.addEventListener("change", () => {
        if (isWelcomeVisible()) {
            uploadFile();
        }
    });
    document.getElementById("alarms-btn")?.addEventListener("click", onAlarmsClick);
    document.getElementById("alarms-close")?.addEventListener("click", closeAlarmsModal);
    document.getElementById("helpBtn")?.addEventListener("click", openHelpModal);
    document.getElementById("learnMoreBtn")?.addEventListener("click", openInfo);
    document.getElementById("help-close")?.addEventListener("click", closeHelpModal);
    document.getElementById("helpSecondaryClose")?.addEventListener("click", closeHelpModal);
    document.getElementById("logoutBtn")?.addEventListener("click", window.Blueprint.endSession);
    document.getElementById("loadDummyBtn")?.addEventListener("click", loadDummyData);
    document.getElementById("chatSendBtn")?.addEventListener("click", sendChat);
    document.getElementById("orgContextBtn")?.addEventListener("click", openOrgContextModal);
    document.getElementById("org-context-close")?.addEventListener("click", closeOrgContextModal);
    document.getElementById("orgContextCancelBtn")?.addEventListener("click", closeOrgContextModal);
    document.getElementById("orgContextFiles")?.addEventListener("change", renderOrgContextFileList);
    document.getElementById("orgContextStageBtn")?.addEventListener("click", uploadOrgContextFiles);
    document.getElementById("orgContextRefreshBtn")?.addEventListener("click", loadOrgContextDocuments);
    document.getElementById("orgContextDocumentList")?.addEventListener("click", event => {
        const btn = event.target.closest(".org-context-delete-btn");
        if (!btn) return;
        deleteOrgContextDocument(btn.dataset.documentId);
    });
    document.getElementById("billingQueryBtn")?.addEventListener("click", runBillingQueryPlaceholder);
    document.getElementById("patternSearchBtn")?.addEventListener("click", runPatternSearchPlaceholder);
    document.getElementById("runOptimizationBtn")?.addEventListener("click", () => {
        showBackendPlaceholder("Cost optimization analysis");
        setTracePrompt("Run a cost optimization analysis for the current billing period and rank the recommendations by projected savings.");
    });
    document.getElementById("prevBtnAllRecords")?.addEventListener("click", () => changePageAllRecords(-1));
    document.getElementById("nextBtnAllRecords")?.addEventListener("click", () => changePageAllRecords(1));
    document.getElementById("prevBtnFilterByProviders")?.addEventListener("click", () => changePageFilterByProvider(-1));
    document.getElementById("nextBtnFilterByProviders")?.addEventListener("click", () => changePageFilterByProvider(1));
    document.getElementById("prevBtnTopRecords")?.addEventListener("click", () => changePageTopRecords(-1));
    document.getElementById("nextBtnTopRecords")?.addEventListener("click", () => changePageTopRecords(1));
    document.getElementById("providerSearchBtn")?.addEventListener("click", loadByProvider);
    document.getElementById("topLoadBtn")?.addEventListener("click", loadTopN);
    document.getElementById("exportDataTabBtn")?.addEventListener("click", exportActiveDataTabCsv);
    document.getElementById("exportAlarmsBtn")?.addEventListener("click", exportAlarmsCsv);
    document.getElementById("infoBtn")?.addEventListener("click", openInfo);
    document.getElementById("generate-pdf-btn")?.addEventListener("click", onGeneratePdfClick);
    document.getElementById("corporate-info-close")?.addEventListener("click", closeCorporateInfoModal);
    document.getElementById("corpCancelBtn")?.addEventListener("click", closeCorporateInfoModal);
    document.getElementById("corpSaveBtn")?.addEventListener("click", saveCorporateInfoAndGenerate);
    document.getElementById("generateForecastBtn")?.addEventListener("click", generateForecast);
    document.getElementById("delete-dataset-btn")?.addEventListener("click", onDeleteDatasetClick);
    document.getElementById("archive-dataset-btn")?.addEventListener("click", onArchiveDatasetClick);
    document.getElementById("notifications-btn")?.addEventListener("click", openNotificationsModal);
    document.getElementById("notifications-close")?.addEventListener("click", closeNotificationsModal);
    document.getElementById("toggle-archived-btn")?.addEventListener("click", toggleArchivedView);
    document.getElementById("restore-dataset-btn")?.addEventListener("click", onRestoreDatasetClick);

    document.querySelectorAll(".data-tab").forEach(button => {
        button.addEventListener("click", () => setActiveDataTab(button.dataset.tab));
    });

    document.querySelectorAll(".finops-question-btn").forEach(button => {
        button.addEventListener("click", () => setTracePrompt(button.dataset.prompt || ""));
    });

    document.querySelectorAll(".pattern-example-btn").forEach(button => {
        button.addEventListener("click", () => setPatternExample(button.dataset.pattern || ""));
    });

    document.querySelectorAll(".audit-action-btn").forEach(button => {
        button.addEventListener("click", () => {
            const action = button.dataset.auditAction || "audit";
            showBackendPlaceholder(`Audit action: ${action}`);

            if (action === "explain-findings") {
                setTracePrompt("Explain the latest audit findings, including evidence, severity, confidence, and recommended next actions.");
            }
        });
    });

    document.getElementById("welcomeUploadBtn")?.addEventListener("click", () => {
        document.getElementById("fileInput").click();
    });
    document.getElementById("welcomeDemoBtn")?.addEventListener("click", loadDummyData);

    const chatInput = document.getElementById("chatInput");
    if (chatInput) {
        chatInput.addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                sendChat();
            }
        });
    }

    const topInput = document.getElementById("topInput");
    if (topInput) {
        topInput.addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                loadTopN();
            }
        });
    }
}

function renderSession(session) {
    const badge = document.getElementById("sessionBadge");
    if (!badge) return;

    const label = session.mode === "guest" ? "Guest" : session.username;
    badge.textContent = label ? `Signed in as ${label}` : "";
}

// ── Init ──────────────────────────────────────────────────────────────────

window.addEventListener("DOMContentLoaded", async () => {
    const session = window.Blueprint.requireSession();
    if (!session) return;

    renderSession(session);
    wireDashboardEvents();
    showWelcome();

    const existing = await tryLoadExistingDatasets();
    if (!existing) {
        // no datasets yet — welcome overlay stays visible
    }
});

async function tryLoadExistingDatasets() {
    try {
        const res = await fetch("/datasets", {
            headers: { "X-User-Id": GUEST_USER_ID }
        });
        if (!res.ok) return false;

        const datasets = await res.json();
        const ready = datasets.find(d => d.status === "READY");
        if (!ready) return false;

        currentDatasetId = ready.id;
        hideWelcome();

        await loadPeriods();
        await loadProviders();
        await loadDatasetList();
        await populatePredictionDatasets();

        if (currentPeriod) {
            changePeriod();
        }

        return true;
    } catch (e) {
        return false;
    }
}

// ── Predictions ───────────────────────────────────────────────────────────

let predictionChartInstance = null;

async function populatePredictionDatasets() {
    // Manual forecast dataset selection was removed from the UI. The backend will own
    // eligible dataset selection for forecasting.
}

async function generateForecast() {
    const wrapper = document.getElementById("predictionChartWrapper");
    const btn = document.getElementById("generateForecastBtn");
    const spinner = document.getElementById("forecastSpinner");

    btn.disabled = true;
    spinner.classList.remove("d-none");

    try {
        const res = await fetch("/api/predictions", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                datasetId: currentDatasetId,
                billingPeriod: currentPeriod,
                selectionMode: "AUTO"
            })
        });

        if (!res.ok) {
            throw new Error(`Prediction API failed: ${res.status}`);
        }

        const data = await res.json();
        renderPredictionChart(data.predictions);
        wrapper.style.display = "block";

        wrapper.scrollIntoView({ behavior: "smooth", block: "nearest" });
    } catch (e) {
        console.error("Forecast failed", e);
        showToast("Forecast backend is not wired for auto-selection yet.", "info");
        wrapper.style.display = "none";
    } finally {
        btn.disabled = false;
        spinner.classList.add("d-none");
    }
}

function renderPredictionChart(predictions) {
    if (predictionChartInstance) {
        predictionChartInstance.destroy();
    }

    const labels = predictions.map(p => p.period);
    const dataPoints = predictions.map(p => p.charge);

    const ctx = document.getElementById("predictionChart").getContext("2d");

    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, "rgba(37, 99, 235, 0.4)");
    gradient.addColorStop(1, "rgba(37, 99, 235, 0.0)");

    predictionChartInstance = new Chart(ctx, {
        type: "line",
        data: {
            labels: labels,
            datasets: [{
                label: "Forecasted Total Charge ($)",
                data: dataPoints,
                borderColor: "#2563eb",
                backgroundColor: gradient,
                borderWidth: 3,
                pointBackgroundColor: "#ffffff",
                pointBorderColor: "#2563eb",
                pointBorderWidth: 2,
                pointRadius: 5,
                pointHoverRadius: 7,
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: "rgba(30, 41, 59, 0.9)",
                    titleFont: { size: 14 },
                    bodyFont: { size: 14, weight: 'bold' },
                    padding: 12,
                    callbacks: {
                        label: ctx => `$${ctx.raw.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}`
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: "#f1f5f9" },
                    ticks: {
                        callback: v => `$${v.toLocaleString()}`,
                        color: "#64748b"
                    }
                },
                x: {
                    grid: { display: false },
                    ticks: { color: "#64748b" }
                }
            },
            animation: {
                duration: 1500,
                easing: 'easeOutQuart'
            }
        }
    });
}
