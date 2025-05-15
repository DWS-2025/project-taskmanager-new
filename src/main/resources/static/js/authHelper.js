window.authFetch = function authFetch(url, options = {}) {
    const token = localStorage.getItem("jwt");
    const headers = options.headers || {};

    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    return fetch(url, {
        ...options,
        headers,
    });
};

window.logout = function logout() {
    localStorage.removeItem("jwt");
    document.cookie = "jwt=; path=/; Max-Age=0";
    window.location.href = "/login";
};
