async function getCSRFToken() {
    return fetch("/csrf-token", { credentials: "same-origin" })
        .then(res => res.json())
        .then(data => data.token)
        .catch(() => null);
}

window.authFetch = async function authFetch(url, options = {}) {
    // const token = localStorage.getItem("jwt"); <-- For Authorization Bearer
    const method = (options.method || "GET").toUpperCase();

    const headers = new Headers(options.headers || {});
     /*
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }
      */

    if (method !== "GET") {
        const csrfToken = await getCSRFToken();
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
    //localStorage.removeItem("jwt");
    fetch("/api/auth/logout", {
        method: "POST",
        credentials: "same-origin"
    })
        .finally(() => {
            window.location.replace("/login");
        });
};

window.getCookie = function(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
};




