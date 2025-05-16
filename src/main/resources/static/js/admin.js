document.addEventListener("DOMContentLoaded", () => {
    const userList = document.getElementById("user-list");
    let clickInsideModal = false;

    fetchAndRenderUsers();

    function fetchAndRenderUsers() {
        authFetch("/api/admin/users", {
            method: "GET"
        })
            .then(res => {
                if (!res.ok) throw new Error("No autorizado");
                return res.json();
            })
            .then(users => {
                if (!Array.isArray(users) || users.length === 0) {
                    userList.innerHTML = "<li>No hay usuarios.</li>";
                    return;
                }

                users.forEach(user => {
                    const li = document.createElement("li");
                    li.className = "user-item"; // reutiliza estilo visual
                    li.dataset.userid = user.id;

                    const content = document.createElement("div");
                    content.className = "user-content"; // mismo layout

                    const info = document.createElement("div");
                    info.innerHTML = `<b>${user.name} - ${user.email}</b>`;
                    info.style.flexGrow = "1";

                    const btnOptions = document.createElement("button");
                    btnOptions.className = "btnMoreOptions";
                    btnOptions.dataset.userid = user.id;
                    btnOptions.innerHTML = `<img src="/img/menu.png" alt="Más opciones">`;

                    content.append(info, btnOptions);
                    li.appendChild(content);

                    // Modal dinámico al estilo index.js
                    const modal = document.createElement("div");
                    modal.className = "modalOptions modal hidden";
                    modal.id = `modalOptions-${user.id}`;

                    const modalContent = document.createElement("div");
                    modalContent.className = "modal-content";
                    modalContent.innerHTML = `
                    <h2>${user.name}</h2>
                    <button class="btnDeleteUser" data-userid="${user.id}" data-useremail="${user.email}">Eliminar Usuario</button>
                `;

                    modal.appendChild(modalContent);
                    li.appendChild(modal);

                    userList.appendChild(li);
                });

                assignUserButtonEvents();
            })
            .catch(err => {
                console.error("Error al cargar usuarios:", err);
                userList.innerHTML = "<li>Error al cargar usuarios.</li>";
            });
    }

    function handleDeleteUser(event) {
        event.preventDefault();

        const email = event.currentTarget.dataset.useremail;
        if (!email) {
            alert("No se pudo determinar el usuario.");
            return;
        }

        if (!confirm(`¿Estás seguro de que quieres eliminar al usuario ${email}?`)) return;

        const url = `/api/users/${encodeURIComponent(email)}`;
        authFetch(url, { method: "DELETE" })
            .then(response => {
                if (response.ok) {
                    document.querySelectorAll(".modalOptions").forEach(m => {
                        m.classList.add("hidden");
                        m.style.display = "none";
                    });

                    const ul = document.getElementById("user-list");
                    ul.innerHTML = ``;
                    fetchAndRenderUsers();

                    alert("Usuario eliminado")
                } else {
                    return response.text().then(msg => {
                        alert(`❌ Error: ${msg || "No se pudo eliminar al usuario"}`);
                    });
                }
            })
            .catch(error => console.error("Error deleting user:", error));
    }


    function assignUserButtonEvents() {
        document.querySelectorAll(".btnMoreOptions").forEach(button => {
            button.removeEventListener("click", handleMoreOptionsClick);
            button.addEventListener("click", handleMoreOptionsClick);
        });

        document.querySelectorAll(".btnDeleteUser").forEach(button => {
            button.removeEventListener("click", handleDeleteUser);
            button.addEventListener("click", handleDeleteUser);
        });

        // Modal interaction for closing when clicking outside
        document.querySelectorAll(".modal").forEach(modal => {
            modal.removeEventListener("mousedown", handleModalMouseDown);
            modal.removeEventListener("mouseup", handleModalMouseUp);
            modal.addEventListener("mousedown", handleModalMouseDown);
            modal.addEventListener("mouseup", handleModalMouseUp);
        });
    }
    function handleMoreOptionsClick(event) {
        const userItem = event.currentTarget.closest(".user-item");
        const modal = userItem.querySelector(".modalOptions");

        if (modal) {
            modal.classList.remove("hidden");
            modal.style.display = "flex";
        }
    }
    // Check if the click was inside modal content
    function handleModalMouseDown(event) {
        clickInsideModal = !!event.target.closest(".modal-content");
    }
    // Close the modal if click was outside of it
    function handleModalMouseUp(event) {
        if (!clickInsideModal && event.target.classList.contains("modal")) {
            event.target.classList.add("hidden");
            event.target.style.display = "none";
        }
    }

    const btnBack = document.getElementById("btnBack");
    if (btnBack) {
        btnBack.addEventListener("click", () => {
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

});