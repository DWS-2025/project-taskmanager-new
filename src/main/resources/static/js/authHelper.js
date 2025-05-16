
function loadCsrfToken() {
    return fetch("/csrf-token", { credentials: "same-origin" })
        .then(res => res.json())
        .then(data => {
            sessionStorage.setItem("XSRF-TOKEN", data.token);
        })
        .catch(() => {
            console.warn("No se pudo obtener el token CSRF");
        });
}
loadCsrfToken();

async function getCookieToken() {
    return fetch("/csrf-token", { credentials: "same-origin" })
        .then(res => res.json())
        .then(data => data.token)
        .catch(() => null);
}

window.authFetch = async function authFetch(url, options = {}) {
    const token = localStorage.getItem("jwt");
    const method = (options.method || "GET").toUpperCase();

    const headers = new Headers(options.headers || {});

    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    if (method !== "GET") {
        const csrfToken = await getCookieToken();
        if (csrfToken) {
            headers.set("X-XSRF-TOKEN", csrfToken);
        }
    }

    return fetch(url, {
        ...options,
        method,
        headers,
        credentials: 'same-origin'
    });
};

window.logout = function logout() {
    localStorage.removeItem("jwt");
    document.cookie = "jwt=; path=/; Max-Age=0";
    window.location.href = "/login";
};

