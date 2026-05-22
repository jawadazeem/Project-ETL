const GUEST_USER_ID = "00000000-0000-0000-0000-000000000001";

let deptChartInstance = null;
let alarmsChartInstance = null;
let currentPeriod = null;
let currentDatasetId = null;
let currentPageAllRecords = 0;
let currentPageFilterByDepartment = 0;
const pageSize = 20;

function asArray(payload) {
    if (Array.isArray(payload)) return payload;
    return payload && Array.isArray(payload.content) ? payload.content : [];
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
    currentPageFilterByDepartment = 0;

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

        document.getElementById("statRecords").textContent = s.totalRecords;
        document.getElementById("statTotal").textContent = `$${s.totalCharges.toFixed(2)}`;
        document.getElementById("statAvg").textContent = `$${s.averageCharge.toFixed(2)}`;

        const hi = s.highestChargeRecord;
        document.getElementById("statHigh").textContent = hi ? `$${hi.totalCharge.toFixed(2)}` : "$0.00";
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
        const alarms = await fetchAlarms();
        const counts = countBySeverity(alarms);

        if (alarmsChartInstance) {
            alarmsChartInstance.destroy();
        }

        alarmsChartInstance = new Chart(document.getElementById("alarmsChart"), {
            type: "bar",
            data: {
                labels: Object.keys(counts),
                datasets: [{
                    label: "Alarm Count",
                    data: Object.values(counts)
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
                    y: { beginAtZero: true, ticks: { precision: 0 } }
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

async function loadDeptChart() {
    if (!currentDatasetId || !currentPeriod) return;
    showSkeleton("skeleton-dept-chart");

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/records/periods/${currentPeriod}?page=0&size=1000`);
        if (!res.ok) throw new Error(`Dept chart fetch failed: ${res.status}`);
        const data = await res.json();
        const records = Array.isArray(data) ? data : (data.content || []);
        const totals = {};

        records.forEach(record => {
            totals[record.department] = (totals[record.department] || 0) + record.totalCharge;
        });

        if (deptChartInstance) {
            deptChartInstance.destroy();
        }

        deptChartInstance = new Chart(document.getElementById("deptChart"), {
            type: "bar",
            data: {
                labels: Object.keys(totals),
                datasets: [{
                    label: "Total Charges ($)",
                    data: Object.values(totals)
                }]
            }
        });
    } catch (e) {
        console.error("Failed to load dept chart", e);
        showToast("Could not load department charges chart.");
    } finally {
        hideSkeleton("skeleton-dept-chart");
    }
}

// ── Records ───────────────────────────────────────────────────────────────

async function loadRecords() {
    if (!currentDatasetId || !currentPeriod) return;
    showSkeleton("skeleton-records");

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/records/periods/${currentPeriod}?page=${currentPageAllRecords}&size=${pageSize}`);
        if (!res.ok) throw new Error(`Records fetch failed: ${res.status}`);
        const data = await res.json();
        const records = data.content || [];
        const totalPages = data.totalPages || 1;

        const tbody = document.querySelector("#recordsTable tbody");
        tbody.innerHTML = "";

        if (records.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted py-3">No records found.</td></tr>';
        } else {
            records.forEach(record => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${record.phoneNumber}</td>
                    <td>${record.department}</td>
                    <td>$${record.totalCharge.toFixed(2)}</td>
                `;
                tbody.appendChild(row);
            });
        }

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

async function loadByDepartment() {
    if (!currentDatasetId) return;
    const department = document.getElementById("departmentSelect").value;
    if (!department) return;
    showSkeleton("skeleton-dept-records");

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/records/departments/${department}?page=${currentPageFilterByDepartment}&size=${pageSize}`);
        if (!res.ok) throw new Error(`Dept records fetch failed: ${res.status}`);
        const data = await res.json();
        const records = data.content || [];
        const totalPages = data.totalPages || 1;

        const tbody = document.querySelector("#departmentTable tbody");
        tbody.innerHTML = "";

        if (records.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted py-3">No records found.</td></tr>';
        } else {
            records.forEach(record => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${record.phoneNumber}</td>
                    <td>${record.department}</td>
                    <td>$${record.totalCharge.toFixed(2)}</td>
                `;
                tbody.appendChild(row);
            });
        }

        document.getElementById("pageInfoFilterByDepartment").textContent =
            `Page ${currentPageFilterByDepartment + 1} of ${totalPages}`;
        document.getElementById("prevBtnFilterByDepartments").disabled = currentPageFilterByDepartment <= 0;
        document.getElementById("nextBtnFilterByDepartments").disabled = currentPageFilterByDepartment >= totalPages - 1;
    } catch (e) {
        console.error("Failed to load dept records", e);
        showToast("Could not load department records.");
    } finally {
        hideSkeleton("skeleton-dept-records");
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

        const tbody = document.querySelector("#topOutput tbody");
        tbody.innerHTML = "";

        if (records.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted py-3">No records found.</td></tr>';
        } else {
            records.forEach(record => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${record.phoneNumber}</td>
                    <td>${record.department}</td>
                    <td>$${record.totalCharge.toFixed(2)}</td>
                `;
                tbody.appendChild(row);
            });
        }
    } catch (e) {
        console.error("Failed to load top N", e);
        showToast("Could not load top N records.");
    } finally {
        hideSkeleton("skeleton-top");
    }
}

// ── Periods & departments ─────────────────────────────────────────────────

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

async function loadDepartments() {
    if (!currentDatasetId || !currentPeriod) return;

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/records/periods/${currentPeriod}?page=0&size=1000`);
        if (!res.ok) return;
        const data = await res.json();
        const records = data.content || [];

        const seen = new Set();
        const select = document.getElementById("departmentSelect");
        select.innerHTML = "";

        records.forEach(record => {
            if (record.department && !seen.has(record.department)) {
                seen.add(record.department);
                const opt = document.createElement("option");
                opt.value = record.department;
                opt.textContent = record.department;
                select.appendChild(opt);
            }
        });
    } catch (e) {
        console.error("Failed to load departments", e);
    }
}

// ── Alarms ────────────────────────────────────────────────────────────────

async function fetchAlarms() {
    if (!currentPeriod || !currentDatasetId) return [];
    const res = await fetch(`/datasets/${currentDatasetId}/alarms/${currentPeriod}`);
    if (!res.ok) throw new Error("Failed to load alarms");
    return await res.json();
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
        const phone = alarm.phoneNumber || "—";
        const employee = alarm.employeeId || "—";
        const period = alarm.billingPeriod || "—";
        const explanation = alarm.explanation || "";

        const li = document.createElement("li");
        li.className = "alarm-item";
        li.innerHTML = `
            <div class="alarm-left">
                <div class="alarm-title">${type}</div>
                <div class="alarm-meta">Employee: ${employee} • Phone: ${phone} • Period: ${period}</div>
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

async function loadAlarmsCount() {
    if (!currentPeriod || !currentDatasetId) return;

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/alarms/${currentPeriod}`);
        const alarms = await res.json();
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

async function sendChat() {
    if (!currentDatasetId) return;
    const input = document.getElementById("chatInput");
    const text = input.value.trim();
    if (!text) return;

    appendChatMessage("user", text);
    input.value = "";

    try {
        const res = await fetch(`/datasets/${currentDatasetId}/martin`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ prompt: text, period: currentPeriod })
        });

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
        console.error("Chat send failed", e);
        appendChatMessage("bot", "Failed to send message — the chat service may be unavailable.");
        showToast("Martin is unavailable. Check the connection.");
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
                await loadDepartments();
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
        await loadDepartments();
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
    currentPageFilterByDepartment = 0;
    loadSummary();
    loadRecords();
    loadDeptChart();
    loadAlarmsCount();
    loadAlarmsChart();
}

function changePageAllRecords(step) {
    currentPageAllRecords = Math.max(0, currentPageAllRecords + step);
    loadRecords();
}

function changePageFilterByDepartment(step) {
    currentPageFilterByDepartment = Math.max(0, currentPageFilterByDepartment + step);
    loadByDepartment();
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
    document.getElementById("help-close")?.addEventListener("click", closeHelpModal);
    document.getElementById("helpSecondaryClose")?.addEventListener("click", closeHelpModal);
    document.getElementById("logoutBtn")?.addEventListener("click", window.Blueprint.endSession);
    document.getElementById("loadDummyBtn")?.addEventListener("click", loadDummyData);
    document.getElementById("chatSendBtn")?.addEventListener("click", sendChat);
    document.getElementById("prevBtnAllRecords")?.addEventListener("click", () => changePageAllRecords(-1));
    document.getElementById("nextBtnAllRecords")?.addEventListener("click", () => changePageAllRecords(1));
    document.getElementById("prevBtnFilterByDepartments")?.addEventListener("click", () => changePageFilterByDepartment(-1));
    document.getElementById("nextBtnFilterByDepartments")?.addEventListener("click", () => changePageFilterByDepartment(1));
    document.getElementById("departmentSearchBtn")?.addEventListener("click", loadByDepartment);
    document.getElementById("topLoadBtn")?.addEventListener("click", loadTopN);
    document.getElementById("infoBtn")?.addEventListener("click", openInfo);

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
        await loadDepartments();
        await loadDatasetList();

        if (currentPeriod) {
            changePeriod();
        }

        return true;
    } catch (e) {
        return false;
    }
}
