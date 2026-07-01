const rows = document.querySelector("#notification-rows");
const refreshButton = document.querySelector("#refresh-button");
const sentCount = document.querySelector("#sent-count");
const failedCount = document.querySelector("#failed-count");
const alarmCount = document.querySelector("#alarm-count");

refreshButton.addEventListener("click", loadNotifications);

loadNotifications();

async function loadNotifications() {
  rows.innerHTML = `<tr><td colspan="5">Loading notifications...</td></tr>`;

  try {
    const response = await fetch("/notifications");

    if (!response.ok) {
      throw new Error(`Request failed with ${response.status}`);
    }

    const notifications = await response.json();
    renderSummary(notifications);
    renderRows(notifications);
  } catch (error) {
    rows.innerHTML = `<tr><td colspan="5">Could not load notifications.</td></tr>`;
    console.error(error);
  }
}

function renderSummary(notifications) {
  const deliveries = notifications.flatMap((notification) => notification.deliveries ?? []);

  sentCount.textContent = deliveries.filter((delivery) => delivery.status === "succeeded").length;
  failedCount.textContent = deliveries.filter((delivery) => delivery.status === "failed").length;
  alarmCount.textContent = notifications.length;
}

function renderRows(notifications) {
  if (notifications.length === 0) {
    rows.innerHTML = `<tr><td colspan="5">No notifications have been sent yet.</td></tr>`;
    return;
  }

  rows.innerHTML = notifications
    .map((notification) => {
      const deliveries = notification.deliveries ?? [];
      const recipients = deliveries
        .map((delivery) => `${delivery.channel}: ${delivery.recipient}`)
        .join("<br>");
      const status = deliveries.some((delivery) => delivery.status === "failed") ? "failed" : "sent";

      return `
        <tr>
          <td>
            <strong>${escapeHtml(notification.title)}</strong>
            <span>${escapeHtml(notification.alarmId)}</span>
          </td>
          <td><mark class="${notification.severity.toLowerCase()}">${notification.severity}</mark></td>
          <td>${recipients || "None"}</td>
          <td><span class="status ${status}">${status}</span></td>
          <td>${formatDate(notification.createdAt)}</td>
        </tr>
      `;
    })
    .join("");
}

function formatDate(value) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
