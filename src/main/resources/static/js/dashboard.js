let deptChartInstance = null;
let alarmsChartInstance = null;
let currentPeriod = null;
let currentDatasetId = null;
let currentPageAllRecords = 0;
let currentPageFilterByDepartment = 0;
const pageSize = 20;

async function loadSummary() {
    const res = await fetch(`/datasets/${currentDatasetId}/summary/periods/${currentPeriod}`);
    const s = await res.json();

    document.getElementById("statRecords").textContent = s.totalRecords;
    document.getElementById("statTotal").textContent = `$${s.totalCharges.toFixed(2)}`;
    document.getElementById("statAvg").textContent = `$${s.averageCharge.toFixed(2)}`;

    const hi = s.highestChargeRecord;
    document.getElementById("statHigh").textContent = hi ? `$${hi.totalCharge.toFixed(2)}` : "$0.00";
    document.getElementById("statHighName").textContent = hi ? hi.accountName : "N/A";
    document.getElementById("summaryStats").style.display = "flex";
}

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
            body: JSON.stringify({
                prompt: text,
                period: currentPeriod
            })
        });

        if (!res.ok) {
            appendChatMessage("bot", `Server error: ${res.status}`);
            return;
        }

        const data = await res.json();
        let msg = (data && (data.answer || data.reply)) || "No response returned.";

        if (data.sql) {
            msg += `\n\nSQL:\n${data.sql}`;
        }

        if (data.reasoning) {
            msg += `\n\nWhy:\n${data.reasoning}`;
        }

        appendChatMessage("bot", msg);
    } catch (e) {
        console.error("Chat send failed", e);
        appendChatMessage("bot", "Failed to send message — the chat service may be unavailable.");
    }
}

async function loadAlarmsChart() {
    if (!currentPeriod || !currentDatasetId) return;

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
    }
}

async function loadRecords() {
    if (!currentDatasetId || !currentPeriod) return;
    const res = await fetch(`/datasets/${currentDatasetId}/records/periods/${currentPeriod}?page=${currentPageAllRecords}&size=${pageSize}`);
    const data = await res.json();
    const records = data.content || [];
    const totalPages = data.totalPages || 1;

    const tbody = document.querySelector("#recordsTable tbody");
    tbody.innerHTML = "";

    records.forEach(record => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${record.phoneNumber}</td>
            <td>${record.department}</td>
            <td>$${record.totalCharge.toFixed(2)}</td>
        `;
        tbody.appendChild(row);
    });

    document.getElementById("pageInfoAllRecords").textContent = `Page ${currentPageAllRecords + 1} of ${totalPages}`;
    document.getElementById("prevBtnAllRecords").disabled = currentPageAllRecords <= 0;
    document.getElementById("nextBtnAllRecords").disabled = currentPageAllRecords >= totalPages - 1;
}

function changePageAllRecords(step) {
    currentPageAllRecords = Math.max(0, currentPageAllRecords + step);
    loadRecords();
}

function changePageFilterByDepartment(step) {
    currentPageFilterByDepartment = Math.max(0, currentPageFilterByDepartment + step);
    loadByDepartment();
}

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

async function loadByDepartment() {
    if (!currentDatasetId) return;
    const department = document.getElementById("departmentSelect").value;
    if (!department) return;

    const res = await fetch(`/datasets/${currentDatasetId}/records/departments/${department}?page=${currentPageFilterByDepartment}&size=${pageSize}`);
    const data = await res.json();
    const records = data.content || [];
    const totalPages = data.totalPages || 1;

    const tbody = document.querySelector("#departmentTable tbody");
    tbody.innerHTML = "";

    records.forEach(record => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${record.phoneNumber}</td>
            <td>${record.department}</td>
            <td>$${record.totalCharge.toFixed(2)}</td>
        `;
        tbody.appendChild(row);
    });

    document.getElementById("pageInfoFilterByDepartment").textContent =
        `Page ${currentPageFilterByDepartment + 1} of ${totalPages}`;
    document.getElementById("prevBtnFilterByDepartments").disabled = currentPageFilterByDepartment <= 0;
    document.getElementById("nextBtnFilterByDepartments").disabled = currentPageFilterByDepartment >= totalPages - 1;
}

async function loadTopN() {
    if (!currentDatasetId) return;
    const n = document.getElementById("topInput").value.trim();
    if (!n) return;

    const res = await fetch(`/datasets/${currentDatasetId}/top/${n}`);
    const data = await res.json();
    const records = Array.isArray(data) ? data : (data.content || []);

    const tbody = document.querySelector("#topOutput tbody");
    tbody.innerHTML = "";

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

async function loadPeriods() {
    if (!currentDatasetId) return;
    const res = await fetch(`/datasets/${currentDatasetId}/records/periods`);
    const data = await res.json();
    const periods = data.content || [];
    const select = document.getElementById("periodSelect");
    select.innerHTML = "";

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
}

async function loadDepartments() {
    if (!currentDatasetId || !currentPeriod) return;
    const res = await fetch(`/datasets/${currentDatasetId}/records/periods/${currentPeriod}?page=0&size=1000`);
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
}

function openInfo() {
    window.Blueprint.openInfoPopup();
}

async function loadDeptChart() {
    if (!currentDatasetId || !currentPeriod) return;
    const res = await fetch(`/datasets/${currentDatasetId}/records/periods/${currentPeriod}?page=0&size=1000`);
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
}

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

function closeHelpModal() {
    document.getElementById("help-overlay").style.display = "none";
}

function openHelpModal() {
    document.getElementById("help-overlay").style.display = "flex";
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
            alert(`Failed to load dummy data: ${txt || res.status}`);
            return;
        }

        currentDatasetId = "00000000-0000-0000-0000-000000000000";
        await waitForDataReady();
        await loadPeriods();
        await loadDepartments();
        changePeriod();
        closeHelpModal();
        alert("Dummy data loaded successfully.");
    } catch (e) {
        console.error("loadDummyData failed", e);
        alert("Failed to load dummy data. See console for details.");
    } finally {
        btn.disabled = false;
        btn.textContent = "Load Dummy Data";
    }
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
        console.error("Failed to load alarms", e);
    }
}

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
            headers: { "X-User-Id": "guest-user" },
            body: formData
        });

        if (response.ok) {
            const dataset = await response.json();
            currentDatasetId = dataset.id;
            fileInput.value = "";

            setTimeout(async () => {
                await waitForDataReady();
                await loadPeriods();
                await loadDepartments();
                changePeriod();
            }, 2000);
        } else {
            const errorText = await response.text();
            alert(`Upload failed: ${errorText}`);
        }
    } catch (error) {
        console.error("Upload error:", error);
    } finally {
        uploadBtn.disabled = false;
        uploadBtn.textContent = "Upload";
    }
}

async function waitForDataReady() {
    if (!currentDatasetId) return;
    for (let i = 0; i < 10; i++) {
        const res = await fetch(`/datasets/${currentDatasetId}/records/periods`);
        const data = await res.json();

        if (data.content && data.content.length > 0) return;

        await new Promise(resolve => setTimeout(resolve, 1000));
    }
}

function wireDashboardEvents() {
    document.getElementById("periodSelect")?.addEventListener("change", changePeriod);
    document.getElementById("uploadBtn")?.addEventListener("click", uploadFile);
    document.getElementById("alarms-btn")?.addEventListener("click", onAlarmsClick);
    document.getElementById("alarms-close")?.addEventListener("click", closeAlarmsModal);
    document.getElementById("helpBtn")?.addEventListener("click", openHelpModal);
    document.getElementById("help-close")?.addEventListener("click", closeHelpModal);
    document.getElementById("helpSecondaryClose")?.addEventListener("click", closeHelpModal);
    document.getElementById("loadDummyBtn")?.addEventListener("click", loadDummyData);
    document.getElementById("chatSendBtn")?.addEventListener("click", sendChat);
    document.getElementById("prevBtnAllRecords")?.addEventListener("click", () => changePageAllRecords(-1));
    document.getElementById("nextBtnAllRecords")?.addEventListener("click", () => changePageAllRecords(1));
    document.getElementById("prevBtnFilterByDepartments")?.addEventListener("click", () => changePageFilterByDepartment(-1));
    document.getElementById("nextBtnFilterByDepartments")?.addEventListener("click", () => changePageFilterByDepartment(1));
    document.getElementById("departmentSearchBtn")?.addEventListener("click", loadByDepartment);
    document.getElementById("topLoadBtn")?.addEventListener("click", loadTopN);
    document.getElementById("infoBtn")?.addEventListener("click", openInfo);

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

window.addEventListener("DOMContentLoaded", () => {
    wireDashboardEvents();
});
