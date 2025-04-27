document.addEventListener("DOMContentLoaded", function () {
    const btnNewMember = document.getElementById("btnNewItem");
    const modalSearchUsers = document.getElementById("modalSearchUsers");
    const searchUserInput = document.getElementById("searchUserInput");
    const userSearchResults = document.getElementById("userSearchResults");
    const btnAddSelectedUsers = document.getElementById("btnAddSelectedUsers");
    let selectedUsers = new Set(); // Set of user IDs selected via checkbox
    let currentUserId = null;
    let clickInsideModal = false;

    // Assign all event listeners to buttons and modals
    function assignUserButtonEvents() {
        // Options button for each user
        document.querySelectorAll(".btnMoreOptions").forEach(button => {
            button.removeEventListener("click", handleMoreOptionsClick);
            button.addEventListener("click", handleMoreOptionsClick);
        });

        // Delete member buttons
        document.querySelectorAll(".btnDeleteMember").forEach(button => {
            button.removeEventListener("click", handleDeleteMember);
            button.addEventListener("click", handleDeleteMember);
        });

        // Modal behavior: detect clicks inside or outside the modal
        document.querySelectorAll(".modal").forEach(modal => {
            modal.removeEventListener("mousedown", handleModalMouseDown);
            modal.removeEventListener("mouseup", handleModalMouseUp);
            modal.addEventListener("mousedown", handleModalMouseDown);
            modal.addEventListener("mouseup", handleModalMouseUp);
        });

        // Open user search modal
        btnNewMember.removeEventListener("click", openSearchModal);
        btnNewMember.addEventListener("click", openSearchModal);

        // Search input change
        searchUserInput.removeEventListener("input", handleSearchUsers);
        searchUserInput.addEventListener("input", handleSearchUsers);

        // Add selected users button
        btnAddSelectedUsers.removeEventListener("click", handleAddSelectedUsers);
        btnAddSelectedUsers.addEventListener("click", handleAddSelectedUsers);
    }

    // Open options modal for specific user
    function handleMoreOptionsClick(event) {
        currentUserId = event.currentTarget.dataset.userid;
        const userItem = event.currentTarget.closest(".user-item");
        const modal = userItem.querySelector(".modalOptions");

        if (modal) {
            modal.classList.remove("hidden");
            modal.style.display = "flex";
        }
    }
    // Detect click inside modal
    function handleModalMouseDown(event) {
        clickInsideModal = !!event.target.closest(".modal-content");
    }
    // Close modal if clicked outside
    function handleModalMouseUp(event) {
        if (!clickInsideModal && event.target.classList.contains("modal")) {
            event.target.classList.add("hidden");
            event.target.style.display = "none";
        }
    }

    // Show modal to search users
    function openSearchModal() {
        modalSearchUsers.classList.remove("hidden");
        modalSearchUsers.style.display = "flex"
    }

    // Search users via AJAX when typing
    function handleSearchUsers() {
        const query = searchUserInput.value.trim();
        const groupId = parseInt(document.body.dataset.groupid, 10);
        if (query.length < 2) {
            userSearchResults.innerHTML = "";
            return;
        }

        fetch(`/api/groups/${groupId}/search_users?q=${encodeURIComponent(query)}`)
            .then(res => {
                if (!res.ok) {
                    throw new Error(`Error searching user(s): ${res.status}`);
                }
                return res.json();
            })
            .then(users => {
                userSearchResults.innerHTML = "";
                users.forEach(user => {
                    const li = document.createElement("li");
                    li.classList.add("selectable-user");

                    // Create checkbox
                    const checkbox = document.createElement("input");
                    checkbox.type = "checkbox";
                    checkbox.dataset.userid = user.id;
                    checkbox.classList.add("user-checkbox");

                    // Create label with username
                    const label = document.createElement("label");
                    label.textContent = user.name;
                    label.setAttribute("for", `user-${user.id}`);

                    // Add/remove from selectedUsers set
                    checkbox.addEventListener("change", function () {
                        if (checkbox.checked) {
                            selectedUsers.add(user.id);
                        } else {
                            selectedUsers.delete(user.id);
                        }
                    });

                    li.appendChild(label);
                    li.appendChild(checkbox);
                    userSearchResults.appendChild(li);
                });
            })
            .catch(error => console.error("User search error:", error.message));
    }
    // Add selected users to the group
    function handleAddSelectedUsers() {
        const checkboxes = document.querySelectorAll(".user-checkbox:checked");
        selectedUsers.clear();
        checkboxes.forEach(checkbox => selectedUsers.add(parseInt(checkbox.dataset.userid, 10)));

        if (selectedUsers.size === 0) {
            alert("No users selected.");
            return;
        }

        const groupId = parseInt(document.body.dataset.groupid, 10);
        const currentUserId = parseInt(document.body.dataset.userid, 10); // Logged-in user ID

        if (!groupId || !currentUserId) {
            console.error("Group ID or User ID is invalid.");
            return;
        }

        fetch(`/api/groups/${groupId}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                currentUserId: currentUserId,
                userIds: Array.from(selectedUsers)
            })
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    location.reload();
                } else {
                    alert("Error adding members");
                }
            })
            .catch(error => console.error("Request error:", error));
    }
    // Delete a member from the group
    function handleDeleteMember(event) {
        const userId = event.target.dataset.userid;
        const groupId = document.body.dataset.groupid;
        currentUserId = document.body.dataset.userid;
        if (!userId || !groupId) {
            console.error("No user or group selected for deletion.");
            return;
        }

        if (confirm("Are you sure you want to remove this member from the group?")) {
            fetch(`/api/groups/${groupId}/${userId}`, {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({currentUserId: currentUserId})
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(errorData => {
                            alert(errorData.message);
                            throw new Error("Network response was not ok");
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    if (data.success) {
                        console.log("Member successfully removed");
                        document.querySelector(`[data-userid='${userId}']`).remove();
                        if (data.message === "own") {
                            // If an admin removes themselves from a group
                            window.location.href = `/${groupId}/members`;
                        }
                    } else {
                        alert(data.message);
                    }
                })
                .catch(error => console.error("Request error:", error));
        }
    }

    // Initial event assignment on page load
    function assignEvents() {
        btnNewMember.addEventListener("click", function () {
            modalSearchUsers.classList.remove("hidden");
        });
        searchUserInput.addEventListener("input", handleSearchUsers);
        btnAddSelectedUsers.addEventListener("click", handleAddSelectedUsers);
        assignUserButtonEvents();
    }

    assignEvents();
});
