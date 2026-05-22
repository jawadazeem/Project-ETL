(function () {
    const SESSION_KEY = "blueprintSession";
    const ACCOUNT_KEY = "blueprintLocalAccount";

    const form = document.getElementById("loginForm");
    const guestBtn = document.getElementById("guestBtn");
    const statusMessage = document.getElementById("statusMessage");
    const signInTab = document.getElementById("signInTab");
    const signUpTab = document.getElementById("signUpTab");
    const submitAuthBtn = document.getElementById("submitAuthBtn");
    const confirmPasswordGroup = document.getElementById("confirmPasswordGroup");
    const clearLocalAccount = document.getElementById("clearLocalAccount");

    let mode = "signin";

    function showMessage(type, text) {
        statusMessage.style.display = "block";
        statusMessage.className = "status-message " + type;
        statusMessage.textContent = text;
    }

    function setMode(nextMode) {
        mode = nextMode;
        const isSignUp = mode === "signup";

        signInTab.classList.toggle("active", !isSignUp);
        signUpTab.classList.toggle("active", isSignUp);
        confirmPasswordGroup.classList.toggle("hidden", !isSignUp);
        submitAuthBtn.textContent = isSignUp ? "Create Account" : "Sign In";
        statusMessage.style.display = "none";
    }

    function saveSession(session) {
        const storage = document.getElementById("rememberMe").checked ? localStorage : sessionStorage;
        storage.setItem(SESSION_KEY, JSON.stringify({
            ...session,
            startedAt: new Date().toISOString()
        }));
    }

    function enterApp(session, message) {
        saveSession(session);
        showMessage("success", message);
        setTimeout(() => {
            window.location.href = "/";
        }, 500);
    }

    function getLocalAccount() {
        const raw = localStorage.getItem(ACCOUNT_KEY);
        return raw ? JSON.parse(raw) : null;
    }

    function saveLocalAccount(username, password) {
        localStorage.setItem(ACCOUNT_KEY, JSON.stringify({
            username,
            password,
            createdAt: new Date().toISOString()
        }));
    }

    function signUp(username, password, confirmPassword) {
        if (password.length < 4) {
            showMessage("error", "Use at least 4 characters for the password.");
            return;
        }
        if (password !== confirmPassword) {
            showMessage("error", "Passwords do not match.");
            return;
        }

        saveLocalAccount(username, password);
        enterApp({ mode: "local", username }, "Account created. Entering dashboard...");
    }

    function signIn(username, password) {
        const account = getLocalAccount();
        if (!account) {
            showMessage("error", "No local account exists yet. Sign up or continue as guest.");
            return;
        }
        if (account.username !== username || account.password !== password) {
            showMessage("error", "Username or password is incorrect.");
            return;
        }

        enterApp({ mode: "local", username }, "Signed in. Entering dashboard...");
    }

    signInTab?.addEventListener("click", () => setMode("signin"));
    signUpTab?.addEventListener("click", () => setMode("signup"));

    clearLocalAccount?.addEventListener("click", () => {
        localStorage.removeItem(ACCOUNT_KEY);
        showMessage("success", "Local demo account cleared.");
    });

    form?.addEventListener("submit", (e) => {
        e.preventDefault();

        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value.trim();
        const confirmPassword = document.getElementById("confirmPassword").value.trim();

        if (!username || !password) {
            showMessage("error", "Enter both username and password.");
            return;
        }

        if (mode === "signup") {
            signUp(username, password, confirmPassword);
        } else {
            signIn(username, password);
        }
    });

    guestBtn?.addEventListener("click", () => {
        enterApp({ mode: "guest", username: "Guest" }, "Entering as guest...");
    });
})();
