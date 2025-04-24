document.addEventListener("DOMContentLoaded", function () {

    const formEditUser = document.getElementById("formEditUser");
    const deleteButton = document.getElementById("deleteAccountBtn");
    const formNewUser = document.getElementById("formNewUser");
    const currentUserId = document.body.dataset.userid;

    function sendNewUserData(event) {
        event.preventDefault();
        const confirm_password = document.getElementById("confirm-password").value;

        let url = `/api/users?confirm_password=${confirm_password}`
        let method = "POST"
        fetch(url, {
            method: method,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: document.getElementById("new-username").value,
                email: document.getElementById("new-email").value,
                password: document.getElementById("new-password").value
            })
        })
            .then(response => {
                if (response.ok) {
                    window.location.href = "/";
                    alert("Usuario creado correctamente")
                } else {
                    return response.text().then(msg => {
                        alert(`Error: ${msg || "No se pudo crear el usuario"}`);
                    });
                }
            })
            .catch(error => console.error("Error creating user:", error));
    }

    function sendEditUserData(event) {
        event.preventDefault();

        let url = `/api/users/${currentUserId}`
        let method = "PUT"
        fetch(url, {
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
                    window.location.href = "/";
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

        let url = `/api/users/${currentUserId}?requesterId=${currentUserId}`;
        let method = "DELETE";
        fetch(url, {
            method: method,
            headers: {"Content-Type": "application/json"}
        })
            .then(response => {
                if (response.ok) {
                    window.location.href = "/logout";
                } else {
                    return response.text().then(msg => {
                        alert(`Error: ${msg || "No se pudo eliminar al usuario"}`);
                    });
                }
            })
            .catch(error => console.error("Error deleting user:", error));

    }

    function assignEvents() {
        if (formEditUser) formEditUser.addEventListener("submit", sendEditUserData);
        if (deleteButton) deleteButton.addEventListener("click", deleteAccount);
        if (formNewUser) formNewUser.addEventListener("submit", sendNewUserData);
    }
    assignEvents();
});