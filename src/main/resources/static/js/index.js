document.addEventListener("DOMContentLoaded", function () {
    const modalProject = document.getElementById("modalProject");
    const btnNewProject = document.getElementById("btnNewItem");
    const formNewProject = document.getElementById("formNewProject");
    const openUserOptions = document.getElementById("openUserOptions");
    const userOptionsModal = document.getElementById("user-options");
    const deleteAccountBtn = document.getElementById("deleteAccountBtn");

    let currentProjectId = null;
    let clickInsideModal = false;

    // Assigns event listeners to all dynamic project buttons
    function assignProjectButtonEvents() {
        document.querySelectorAll(".btnMoreOptions").forEach(button => {
            button.removeEventListener("click", handleMoreOptionsClick);
            button.addEventListener("click", handleMoreOptionsClick);
        });

        document.querySelectorAll(".btnDeleteProject").forEach(button => {
            button.removeEventListener("click", handleDeleteProject);
            button.addEventListener("click", handleDeleteProject);
        });

        document.querySelectorAll(".btnEditProject").forEach(button => {
            button.removeEventListener("click", handleEditProject);
            button.addEventListener("click", handleEditProject);
        });

        // Modal interaction for closing when clicking outside
        document.querySelectorAll(".modal").forEach(modal => {
            modal.removeEventListener("mousedown", handleModalMouseDown);
            modal.removeEventListener("mouseup", handleModalMouseUp);
            modal.addEventListener("mousedown", handleModalMouseDown);
            modal.addEventListener("mouseup", handleModalMouseUp);
        });
    }
    window.assignProjectButtonEvents = assignProjectButtonEvents; //make the function global

    // Handle the account deletion button click
    deleteAccountBtn.addEventListener("click", function(event) {
        event.preventDefault(); // Prevent form from submitting right away
        const confirmation = confirm("Are you sure you want to delete your account? This action is irreversible.");

        if (confirmation) {
            document.getElementById("deleteAccountForm").submit(); // Submit form only if confirmed
        }
    });

    function handleMoreOptionsClick(event) {
        currentProjectId = event.currentTarget.dataset.projectid;
        const projectItem = event.currentTarget.closest(".project-item");
        const modal = projectItem.querySelector(".modalOptions");

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

    // Open modal for creating a new project
    function openNewProjectModal() {
        currentProjectId = null;
        formNewProject.querySelector("input[name='name']").value = "";
        modalProject.style.display = "flex";
    }

    // Save or update a project on form submit
    function saveProject(event) {
        event.preventDefault();

        const formData = new URLSearchParams();
        formData.append("name", document.getElementById("name").value);

        // Get groupId from select or hidden input
        const groupSelect = formNewProject.querySelector("select[name='groupId']");
        if (groupSelect) {
            formData.append("groupId", groupSelect.value);
        } else {
            formData.append("groupId", formNewProject.querySelector("input[name='groupId']").value);
        }

        let url = "/api/projects";
        let method = "POST";
        if (currentProjectId) {
            url += `/${currentProjectId}`;
            method = "PUT";
        }

        authFetch(url, {
            method: method,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: formData.get("name"),
                groupId: formData.get("groupId")
            })
        })
            .then(async response => {
                if (!response.ok) throw new Error(`Error ${response.status}`);
                const contentType = response.headers.get("content-type");
                if (contentType && contentType.includes("application/json")) {
                    return response.json();
                }
                return null;
            })
            .then(data => {
                console.log(data);
                location.reload(); // Reload page to show new project
            })
            .catch(error => console.error("Error saving/updating project:", error));
    }
    function handleEditProject(event) {
        currentProjectId = event.currentTarget.dataset.projectid;
        const projectItem = event.currentTarget.closest(".project-item");
        // set form title to project name
        formNewProject.querySelector("input[name='name']").value = projectItem.querySelector("b").innerText;

        // set form select to current groupId
        const selectGroup = formNewProject.querySelector("select[name='groupId']");
        const projectGroupId = projectItem.dataset.groupid;
        if (selectGroup && projectGroupId) {
            selectGroup.value = projectGroupId;
        }

        // Hide all open modals
        document.querySelectorAll(".modalOptions").forEach(modal => {
            modal.classList.add("hidden");
            modal.style.display = "none";
        });

        // Show project modal
        modalProject.style.display = "flex";
    }
    function handleDeleteProject(event) {
        const projectId = event.target.dataset.projectid;
        if (!projectId) {
            console.error("No project selected for deletion.");
            return;
        }

        const taskItem = event.target.closest(".project-item");
        authFetch(`/api/projects/${projectId}`, {
            method: "DELETE"
        })
            .then(response => {
                if (response.ok) {
                    taskItem.style.transition = "opacity 0.3s ease-out";
                    taskItem.style.opacity = "0";

                    // Delete from DOM after 300ms
                    setTimeout(() => {
                        taskItem.remove();
                        window.handlePaginationAfterDelete();
                    }, 300);
                } else {
                    console.error("Error al eliminar el proyecto.");
                }
            })
            .catch(error => console.error("Request error:", error));

    }

    // Show user options modal
    function openUserOptionsModal() {
        userOptionsModal.style.display = "flex";
    }
    // Hide user options modal if click is outside
    function closeUserOptionsModal(event) {
        if (event.target === userOptionsModal) {
            userOptionsModal.style.display = "none";
        }
    }

    // Assign all main event listeners
    function assignEvents() {
        btnNewProject.addEventListener("click", openNewProjectModal);
        formNewProject.addEventListener("submit", saveProject);
        assignProjectButtonEvents();

        if (openUserOptions && userOptionsModal) {
            openUserOptions.addEventListener("click", openUserOptionsModal);
            window.addEventListener("click", closeUserOptionsModal);
        }
    }

    // Initialize all events on page load
    assignEvents();
});
