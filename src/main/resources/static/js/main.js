(function () {
    window.Blueprint = window.Blueprint || {};

    window.Blueprint.sessionKey = "blueprintSession";

    window.Blueprint.getSession = function () {
        const raw =
            sessionStorage.getItem(window.Blueprint.sessionKey) ||
            localStorage.getItem(window.Blueprint.sessionKey);
        return raw ? JSON.parse(raw) : null;
    };

    window.Blueprint.requireSession = function () {
        const session = window.Blueprint.getSession();
        if (!session) {
            window.location.href = "/login.html";
            return null;
        }
        return session;
    };

    window.Blueprint.endSession = function () {
        sessionStorage.removeItem(window.Blueprint.sessionKey);
        localStorage.removeItem(window.Blueprint.sessionKey);
        window.location.href = "/login.html";
    };

    window.Blueprint.goHome = function () {
        window.location.href = "/";
    };

    window.Blueprint.openDashboard = function () {
        window.open("/", "_self");
    };

    window.Blueprint.openInfoPopup = function () {
        window.open(
            "/info.html",
            "infoPopup",
            "width=600,height=650,resizable=yes,scrollbars=yes"
        );
    };
})();
