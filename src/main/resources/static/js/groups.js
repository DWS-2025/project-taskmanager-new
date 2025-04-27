document.addEventListener("DOMContentLoaded", function () {
    const modalGroup = document.getElementById("modalGroup");
    const modalNewOwner = document.getElementById("modalChangeOwner");
    const modalTitle = modalGroup.querySelector("h2");
    const btnNewGroup = document.getElementById("btnNewItem");
    const formNewGroup = document.getElementById("formNewGroup");
    const inputGroupName = formNewGroup.querySelector("input[name='name']");
    const groupUsersResult = document.getElementById("groupUsersResult");
    const btnAddSelectedUser = document.getElementById("btnAddSelectedUser");
    let currentGroupId = null;
    let currentGroupName = null;
    let clickInsideModal = false;
    let newOwner = null;

    // Assign event listeners to group buttons
    function assignGroupButtonEvents() {
        document.querySelectorAll(".btnChangeOwner").forEach(button => {
            button.removeEventListener("click", openNewOwnerModal);
            button.addEventListener("click", openNewOwnerModal);
        });

        document.querySelectorAll(".btnMoreOptions").forEach(button => {
            button.removeEventListener("click", handleMoreOptionsClick);
            button.addEventListener("click", handleMoreOptionsClick);
        });

        document.querySelectorAll(".btnLeaveGroup").forEach(button => {
            button.removeEventListener("click", handleLeaveGroup);
            button.addEventListener("click", handleLeaveGroup);
        });

        document.querySelectorAll(".btnDeleteGroup").forEach(button => {
            button.removeEventListener("click", handleDeleteGroup);
            button.addEventListener("click", handleDeleteGroup);
        });

        document.querySelectorAll(".btnManageMembers").forEach(button => {
            button.removeEventListener("click", handleManageMembers);
            button.addEventListener("click", handleManageMembers);
        });

        document.querySelectorAll(".btnEditGroup").forEach(button => {
            button.removeEventListener("click", handleEditGroup);
            button.addEventListener("click", handleEditGroup);
        });

        // Modal click detection
        document.querySelectorAll(".modal").forEach(modal => {
            modal.removeEventListener("mousedown", handleModalMouseDown);
            modal.removeEventListener("mouseup", handleModalMouseUp);
            modal.addEventListener("mousedown", handleModalMouseDown);
            modal.addEventListener("mouseup", handleModalMouseUp);
        });
    }
    window.assignGroupButtonEvents = assignGroupButtonEvents; // Expose the function globally

    // Show options modal for the group
    function handleMoreOptionsClick(event) {
        currentGroupId = event.currentTarget.dataset.groupid;
        const groupItem = event.currentTarget.closest(".group-item");
        const modal = groupItem.querySelector(".modalOptions");

        if (modal) {
            modal.classList.remove("hidden");
            modal.style.display = "flex";
        }
    }
    // Detect clicks inside modal
    function handleModalMouseDown(event) {
        clickInsideModal = !!event.target.closest(".modal-content");
    }
    // Close modal when clicking outside
    function handleModalMouseUp(event) {
        if (!clickInsideModal && event.target.classList.contains("modal")) {
            event.target.classList.add("hidden");
            event.target.style.display = "none";
        }
    }

    // Open modal for creating a new group
    function openNewGroupModal() {
        currentGroupId = null;
        inputGroupName.value = ""; // Clear input
        modalTitle.innerText = "Nuevo Grupo"; // Reset modal title
        modalGroup.style.display = "flex";
    }
    // Open modal to change group owner
    function openNewOwnerModal(event) {
        currentGroupId = event.target.dataset.groupid;
        currentGroupName = event.target.dataset.groupname;
        document.querySelectorAll(".modalOptions").forEach(modal => {
            modal.classList.add("hidden");
            modal.style.display = "none";
        });
        modalNewOwner.classList.remove("hidden");
        modalNewOwner.style.display = "flex";
        showGroupMembers();
    }

    // Navigate to group member management page
    function handleManageMembers(event) {
        const groupId = event.target.dataset.groupid;
        if (!groupId) {
            console.error("No group selected.");
            return;
        }
        window.location.href = `/${groupId}/members`;
    }

    // Save or update a group
    function saveGroup(event) {
        event.preventDefault();

        const formData = new URLSearchParams();
        formData.append("name", document.getElementById("name").value);
        formData.append("ownerID", currentUser.id);

        let url = "/api/groups";
        let method = "POST";

        if (currentGroupId) {
            url += `/${currentGroupId}`;
            method = "PUT";
            formData.delete("ownerID");
            formData.append("ownerID", "0");
        }

        fetch(url, {
            method: method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: formData.get("name"),
                ownerID: formData.get("ownerID")
            })
        })
            .then(response => response.text())
            .then(() => location.reload())
            .catch(error => console.error("Error saving/updating group:", error));
    }
    function handleEditGroup(event) {
        currentGroupId = event.currentTarget.dataset.groupid;
        const groupItem = event.currentTarget.closest(".group-item");
        inputGroupName.value = groupItem.querySelector("b").innerText; // Set current name
        modalTitle.innerText = "Cambiar Nombre"; // Change modal title
        modalGroup.style.display = "flex";

        const modal = groupItem.querySelector(".modalOptions");
        if (modal) {
            modal.classList.add("hidden");
            modal.style.display = "none";
        }
    }
    function handleDeleteGroup(event) {
        const groupId = event.target.dataset.groupid;
        const requesterId = currentUser.id;

        if (!groupId || !requesterId) {
            console.error("Missing groupId or requesterId.");
            return;
        }

        if (confirm("Are you sure you want to delete this group? This action is irreversible.")) {
            const groupItem = event.target.closest(".group-item");

            fetch(`/api/groups/${groupId}?requesterId=${requesterId}`, {
                method: "DELETE",
                headers: { "Content-Type": "application/json" }
            })
                .then(response => {
                    if (response.ok) {
                        groupItem.style.transition = "opacity 0.3s ease-out";
                        groupItem.style.opacity = "0";

                        setTimeout(() => {
                            groupItem.remove();
                            window.handlePaginationAfterDelete();
                        }, 300);
                    } else {
                        console.error("Error deleting the group.");
                    }
                })
                .catch(error => console.error("Request error:", error));
        }
    }
    function handleLeaveGroup(event) {
        const groupId = event.target.dataset.groupid;
        const currentUserId = document.body.dataset.userid;
        if (!groupId) {
            console.error("No group selected to leave.");
            return;
        }

        if (confirm("Are you sure you want to leave this group?")) {
            fetch(`/api/groups/l/${groupId}`, {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({currentUserId: currentUserId})
            })
                .then(response => {
                    if (!response.ok) throw new Error("Network response was not ok");
                    return response.json();
                })
                .then(data => {
                    if (data.success) {
                        location.reload();
                    } else {
                        console.error("Error leaving the group");
                    }
                })
                .catch(error => console.error("Request error:", error));
        }
    }

    // Fetch and show current group members to select a new owner
    function showGroupMembers() {
        fetch(`/api/groups/${currentGroupId}/members`)
            .then(res => {
                if (!res.ok) {
                    throw new Error(`Error fetching members: ${res.status}`);
                }
                return res.json();
            })
            .then(users => {
                groupUsersResult.innerHTML = "";
                users.forEach(user => {
                    const li = document.createElement('li');
                    li.classList.add("selectable-user");
                    const radio = document.createElement("input");
                    radio.type = "radio";
                    radio.value = user.id;
                    radio.id = `user-${user.id}`;
                    radio.classList.add("user-radio");
                    radio.name = "groupMember";

                    const label = document.createElement("label");
                    label.textContent = user.name;
                    label.setAttribute("for", `user-${user.id}`);

                    radio.addEventListener("change", function () {
                        if (radio.checked) {
                            newOwner = user.id;
                        } else {
                            newOwner = null;
                        }
                    });

                    li.appendChild(label);
                    li.appendChild(radio);
                    groupUsersResult.appendChild(li);
                });
            })
            .catch(err => {
                console.error("Error loading members:", err.message);
            });
    }
    // Change group owner
    function handleChangeOwner() {
        if (newOwner == null) {
            alert("No user selected");
            return;
        }

        fetch(`/api/groups/${currentGroupId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: currentGroupName,
                ownerID: newOwner
            })
        })
            .then(async response => {
                if (!response.ok) {
                    const err = await response.text();
                    alert(`Error: ${err || "Error changing owner"}`);
                    return;
                }

                modalNewOwner.classList.add("hidden");
                modalNewOwner.style.display = "none";
                location.reload();
                alert("Owner changed successfully");
            })
            .catch(error => console.error("Request error:", error));
    }

    // Initial event bindings on page load
    function assignEvents() {
        btnNewGroup.addEventListener("click", openNewGroupModal);
        formNewGroup.addEventListener("submit", saveGroup);
        btnAddSelectedUser.addEventListener("click", handleChangeOwner);
        assignGroupButtonEvents();
    }

    assignEvents();
});