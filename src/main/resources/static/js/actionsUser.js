document.addEventListener("DOMContentLoaded", function () {
    const formEditUser = document.getElementById("formEditUser");
    const deleteButton = document.getElementById("deleteAccountBtn");
    const formNewUser = document.getElementById("formNewUser");
    const formLogin = document.getElementById("formLogin");
    const logoutBtn = document.getElementById("logoutButton");
    const btnManageGroups = document.getElementById("btnManageGroups");
    const btnEditUser = document.getElementById("btnEditUser");
    const currentUserName = document.body.dataset.uname;

    function assignEvents() {
        if (formEditUser) formEditUser.addEventListener("submit", sendEditUserData);
        if (deleteButton) deleteButton.addEventListener("click", deleteAccount);
        if (formNewUser) formNewUser.addEventListener("submit", sendNewUserData);
    }

    function sendNewUserData(event) {
        event.preventDefault();
        const confirm_password = document.getElementById("confirm-password").value;

        let url = `/api/users`
        let method = "POST"
        authFetch(url, {
            method: method,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: document.getElementById("new-username").value,
                email: document.getElementById("new-email").value,
                password: document.getElementById("new-password").value,
                confirmPassword: confirm_password
            })
        })
            .then(response => {
                if (response.ok) {
                    alert("Usuario registrado correctamente")
                    logout();
                } else {
                    return response.text().then(msg => {
                        alert(`Error: ${msg || "No se pudo registrar el usuario"}`);
                    });
                }
            })
            .catch(error => console.error("Error creating user:", error));
    }

    function sendEditUserData(event) {
        const currentUserId = document.body.dataset.userid;
        event.preventDefault();

        let url = `/api/users/${currentUserId}`
        let method = "PUT"
        authFetch(url, {
            method: method,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: document.getElementById("name").value,
                email: document.getElementById("email").value,
                password: document.getElementById("password").value
            })
        })
            .then(response => {
                if (response.ok) {
                    alert("Datos actualizados correctamente")
                    window.location.href = "/projects";
                } else {
                    return response.text().then(msg => {
                        alert(`Error: ${msg || "No se pudo actualizar el usuario"}`);
                    });
                }
            })
            .catch(error => console.error("Error updating user:", error));
    }

    function deleteAccount(event) {
        event.preventDefault();


        let url = `/api/users/${currentUserName}`;
        let method = "DELETE";
        authFetch(url, {
            method: method
        })
            .then(response => {
                if (response.ok) {
                    logout();
                } else {
                    return response.text().then(msg => {
                        alert(`Error: ${msg || "No se pudo eliminar al usuario"}`);
                    });
                }
            })
            .catch(error => console.error("Error deleting user:", error));

    }

    if (formLogin) {
        formLogin.addEventListener("submit", function(event) {
            event.preventDefault();

            const email = document.getElementById("email").value;
            const password = document.getElementById("password").value;

            authFetch("/api/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ username: email, password })
            })
                .then(response => {
                    if (!response.ok) throw new Error("Credenciales incorrectas");
                    return response.json();
                })
                .then(data => {
                    localStorage.setItem("jwt", data.token);
                    document.cookie = `jwt=${data.token}; path=/; Max-Age=3600; Secure; SameSite=Strict`;
                    setTimeout(() => {
                        window.location.href = "/projects";
                    }, 300);
                })
                .catch(err => alert("Error: " + err.message));
        });
    }

    if (btnEditUser) {
        btnEditUser.addEventListener("click", () => {
            const url = "/user_data";
            authFetch(url)
                .then(res => {
                    if (!res.ok) throw new Error("No autorizado");
                    return res.text();
                })
                .then(html => {
                    document.open();
                    document.write(html);
                    document.close();
                    window.history.pushState({}, "", url);
                })
                .catch(err => {
                    console.error("Error al cargar /user_data:", err);
                    alert("No autorizado o error de carga.");
                });
        });
    }

    if (btnManageGroups) {
        btnManageGroups.addEventListener("click", () => {
            const url = "/user_groups";
            authFetch(url)
                .then(res => {
                    if (!res.ok) throw new Error("No autorizado");
                    return res.text();
                })
                .then(html => {
                    document.open();
                    document.write(html);
                    document.close();
                    window.history.pushState({}, "", url);
                })
                .catch(err => {
                    console.error("Error al cargar /user_groups:", err);
                    alert("No autorizado o error de carga.");
                });
        });
    }

    if (logoutBtn) {
        logoutBtn.addEventListener("click", function() {
            logout(); // usa la función global
        });
    }

    const btnCancel = document.getElementById("btnCancel");
    if (btnCancel) {
        btnCancel.addEventListener("click", () => {
            const url = "/projects";
            authFetch(url)
                .then(res => {
                    if (!res.ok) throw new Error("No autorizado");
                    return res.text();
                })
                .then(html => {
                    document.open();
                    document.write(html);
                    document.close();
                    window.history.pushState({}, "", url);
                })
                .catch(err => {
                    console.error("Error al volver a /projects:", err);
                    alert("No autorizado o error de carga.");
                });
        });
    }

    assignEvents();
});