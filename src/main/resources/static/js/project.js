// Run this once the DOM has fully loaded
document.addEventListener("DOMContentLoaded", function () {
    // Elements used throughout the script
    const modalTask = document.getElementById("modalTask");
    const btnNewTask = document.getElementById("btnNewItem");
    const formNewTask = document.getElementById("formNewTask");
    const projectID = document.getElementById("project-info").dataset.projectid;
    const fileInput = document.getElementById('fileInput');
    const fileName = document.getElementById('fileName');

    let currentTaskId = null;
    let clickInsideModal = false;

    // Display selected file name
    fileInput.addEventListener('change', function () {
        if (fileInput.files.length > 0) {
            fileName.textContent = fileInput.files[0].name;
        } else {
            fileName.textContent = "No file selected";
        }
    });

    // Assign event listeners to task buttons
    function asignarEventosBotones() {
        document.querySelectorAll(".btnMoreOptions").forEach(button => {
            button.removeEventListener("click", handleMoreOptionsClick);
            button.addEventListener("click", handleMoreOptionsClick);
        });

        document.querySelectorAll(".btnDeleteTask").forEach(button => {
            button.removeEventListener("click", handleDeleteTask);
            button.addEventListener("click", handleDeleteTask);
        });

        document.querySelectorAll(".btnEditTask").forEach(button => {
            button.removeEventListener("click", handleEditTask);
            button.addEventListener("click", handleEditTask);
        });

        // For modal closing when clicking outside
        document.querySelectorAll(".modal").forEach(modal => {
            modal.removeEventListener("mousedown", handleModalMouseDown);
            modal.removeEventListener("mouseup", handleModalMouseUp);
            modal.addEventListener("mousedown", handleModalMouseDown);
            modal.addEventListener("mouseup", handleModalMouseUp);
        });
    }

    // Handle "More Options" button click
    function handleMoreOptionsClick(event) {
        currentTaskId = event.currentTarget.dataset.taskid;
        console.log("currentTaskId = " + currentTaskId);

        const taskItem = event.currentTarget.closest(".task-item");
        const modal = taskItem.querySelector(".modal");

        if (modal) {
            modal.style.display = "flex";

            // Avoid re-assigning events multiple times
            if (!modal.dataset.eventAssigned) {
                modal.addEventListener("mousedown", handleModalMouseDown);
                modal.addEventListener("mouseup", handleModalMouseUp);
                modal.dataset.eventAssigned = "true";
            }
        }
    }

    // Handle deleting a task
    function handleDeleteTask(event) {
        const taskId = event.target.dataset.taskid;
        if (!taskId) {
            console.error("No task selected to delete.");
            return;
        }

        console.log("Deleting task ID: " + taskId);

        const taskItem = event.target.closest(".task-item");
        taskItem.style.transition = "opacity 0.3s ease-out";
        taskItem.style.opacity = "0";

        fetch(`/project/${projectID}/delete_task?taskId=${taskId}`, {
            method: "DELETE",
            headers: { "Content-Type": "application/json" }
        })
            .then(response => response.json())
            .then(data => {
                if (data.message) {
                    console.log("Task deleted successfully");
                    document.querySelector(`[data-taskid='${taskId}']`).remove();
                    actualizarListaTareas(); // Refresh task list
                } else {
                    console.error("Error deleting task");
                }
            })
            .catch(error => console.error("Request error:", error));
    }

    // Refresh the list of tasks from server
    function actualizarListaTareas() {
        fetch(`/project/${projectID}`)
            .then(response => response.text())
            .then(html => {
                document.getElementById("task-list").innerHTML =
                    new DOMParser().parseFromString(html, "text/html")
                        .querySelector("#task-list").innerHTML;
                asignarEventosBotones(); // Reassign event listeners after reload
            })
            .catch(error => console.error("Error updating task list:", error));
    }

    // Handle editing a task (fill form with current values)
    function handleEditTask(event) {
        currentTaskId = event.currentTarget.dataset.taskid;

        const taskItem = event.currentTarget.closest(".task-item");
        const title = taskItem.querySelector(".task-content b").innerText;
        const description = taskItem.querySelector(".task-content").textContent.split(":")[1]?.trim() || "";

        const imageElement = taskItem.querySelector(".task-image");
        const imagePath = imageElement ? imageElement.style.backgroundImage.replace('url("', '').replace('")', '') : "";

        formNewTask.querySelector("input[name='title']").value = title;
        formNewTask.querySelector("textarea[name='description']").value = description;
        formNewTask.querySelector("input[name='image']").value = null;

        let hiddenImageInput = formNewTask.querySelector("input[name='imagePath']");
        if (!hiddenImageInput && imagePath) {
            hiddenImageInput = document.createElement("input");
            hiddenImageInput.type = "hidden";
            hiddenImageInput.name = "imagePath";
            formNewTask.appendChild(hiddenImageInput);
        }

        if (hiddenImageInput) {
            hiddenImageInput.value = imagePath || "";
        }

        const modal = taskItem.querySelector(".modalOptions");
        if (modal) {
            modal.style.display = "none";
        }

        modalTask.style.display = "flex";
    }

    // Handle clicking outside the modal to close it
    function handleModalMouseDown(event) {
        if (event.target.closest(".modal-content") || event.target.closest("#formNewTask")) {
            clickInsideModal = true;
        } else {
            clickInsideModal = false;
        }
    }

    function handleModalMouseUp(event) {
        if (!clickInsideModal && event.target.classList.contains("modal")) {
            event.target.style.display = "none";
        }
    }

    // Handle "New Task" button click
    btnNewTask.addEventListener("click", function () {
        currentTaskId = null;

        formNewTask.querySelector("input[name='title']").value = "";
        formNewTask.querySelector("textarea[name='description']").value = "";
        formNewTask.querySelector("input[name='image']").value = "";

        const hiddenImageInput = formNewTask.querySelector("input[name='imagePath']");
        if (hiddenImageInput) {
            hiddenImageInput.remove();
        }

        modalTask.style.display = "flex";
    });

    // Handle task form submission (create or update)
    formNewTask.addEventListener("submit", function (event) {
        event.preventDefault();

        const formData = new FormData(formNewTask);
        formData.append("projectID", projectID);

        if (currentTaskId) {
            formData.append("taskId", currentTaskId);

            fetch(`/project/${projectID}/edit_task`, {
                method: "PUT",
                body: formData
            })
                .then(response => response.json())
                .then(data => {
                    console.log(data);
                    location.reload(); // Reload page to reflect changes
                })
                .catch(error => console.error("Error updating task:", error));
        } else {
            fetch(`/project/${projectID}/save_task`, {
                method: "POST",
                body: formData
            })
                .then(response => response.text())
                .then(data => {
                    console.log(data);
                    location.reload(); // Reload page to show new task
                })
                .catch(error => console.error("Error saving task:", error));
        }
    });

    // Initial event setup
    asignarEventosBotones();

    document.getElementById("searchForm").addEventListener("submit", function (e) {
        e.preventDefault();

        const title = document.getElementById("searchTitle").value;
        const hasImage = document.getElementById("filterImage").checked;

        const url = new URL("/api/tasks/search", window.location.origin);
        if (title) url.searchParams.append("title", title);
        url.searchParams.append("hasImage", hasImage);

        fetch(url)
            .then(res => res.json())
            .then(tasks => {
                renderTaskList(tasks);
                asignarEventosBotones();
            })
            .catch(err => console.error("Error al buscar tareas:", err));
    });

    function renderTaskList(tasks) {
        const taskList = document.getElementById("task-list");
        taskList.innerHTML = "";

        tasks.forEach(task => {
            const li = document.createElement("li");
            li.className = "task-item";
            li.dataset.taskid = task.id;

            const content = document.createElement("div");
            content.className = "task-content";
            content.innerHTML = `<b>${task.title}</b><p>${task.description}</p>`;

            if (task.image) {
                const img = document.createElement("img");
                img.src = `data:image/jpeg;base64,${task.image}`;
                img.style.maxWidth = "200px";
                content.appendChild(img);
            }

            const btn = document.createElement("button");
            btn.className = "btnMoreOptions";
            btn.dataset.taskid = task.id;
            btn.innerHTML = `<img src="/img/menu.png" alt="Más opciones" style="width:16px; height:16px;">`;
            content.appendChild(btn);

            li.appendChild(content);
            taskList.appendChild(li);
        });
    }

});

