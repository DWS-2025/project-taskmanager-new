document.addEventListener("DOMContentLoaded", function () {
    const modalTask = document.getElementById("modalTask");
    const btnNewTask = document.getElementById("btnNewItem");
    const formNewTask = document.getElementById("formNewTask");
    const projectID = document.getElementById("project-info").dataset.projectid;
    const fileInput = document.getElementById('fileInput');
    const fileName = document.getElementById('fileName');
    const quill = new Quill('#quill-editor', {
        theme: 'snow'
    });


    let currentTaskId = null;
    let clickInsideModal = false;

    // Assign event listeners to task buttons
    function assignEventsButtons() {
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

        document.querySelectorAll(".btnGetLastReport").forEach(button => {
            button.removeEventListener("click", handleGetLatestReport);
            button.addEventListener("click", handleGetLatestReport);
        });

        document.querySelectorAll(".btnGenerateReport").forEach(button => {
            button.removeEventListener("click", handleGenerateReport);
            button.addEventListener("click", handleGenerateReport);
        });

        // For modal closing when clicking outside
        document.querySelectorAll(".modal").forEach(modal => {
            modal.removeEventListener("mousedown", handleModalMouseDown);
            modal.removeEventListener("mouseup", handleModalMouseUp);
            modal.addEventListener("mousedown", handleModalMouseDown);
            modal.addEventListener("mouseup", handleModalMouseUp);
        });
    }
    // Display selected file name
    fileInput.addEventListener('change', function () {
        if (fileInput.files.length > 0) {
            fileName.textContent = fileInput.files[0].name;
        } else {
            fileName.textContent = "No file selected";
        }
    });
    btnNewTask.addEventListener("click", function () {
        currentTaskId = null;

        formNewTask.querySelector("input[name='title']").value = "";
        quill.setContents([]); //
        formNewTask.querySelector("input[name='description']").value = "";
        formNewTask.querySelector("input[name='image']").value = "";

        const hiddenImageInput = formNewTask.querySelector("input[name='imagePath']");
        if (hiddenImageInput) {
            hiddenImageInput.remove();
        }

        modalTask.style.display = "flex";
    });
    formNewTask.addEventListener("submit", function (event) {
        event.preventDefault();

        const description = document.getElementById("description").value = DOMPurify.sanitize(quill.root.innerHTML);

        const formData = new FormData(formNewTask);
        const file = formData.get("image");
        const body = {
            title: formData.get("title"),
            description: description,
            projectId: parseInt(projectID)
        };
        if (file instanceof File && file.size > 0) {
            const reader = new FileReader();
            reader.onloadend = function () {
                body.image = reader.result; // Base64
                sendTask(body);
            };
            reader.readAsDataURL(file);
        } else {
            sendTask(body);
        }
    });

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
    // Handle clicking outside the modal to close it
    function handleModalMouseDown(event) {
        clickInsideModal = !!(event.target.closest(".modal-content") || event.target.closest("#formNewTask"));
    }
    function handleModalMouseUp(event) {
        if (!clickInsideModal && event.target.classList.contains("modal")) {
            event.target.style.display = "none";
        }
    }

    function sendTask(body) {
        body.ownerId = parseInt(document.body.dataset.userid, 10);

        let url = "/api/tasks";
        let method = "POST";
        if (currentTaskId) {
            url += `/${currentTaskId}`;
            method = "PUT";
        }

        authFetch(url, {
            method: method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        })
            .then(response => {
                if (response.status === 401) {
                    alert("No est\u00E1s autorizado para " + (method === "POST" ? "crear" : "editar") + " esta tarea.");
                    throw new Error("UNAUTHORIZED");
                }
                if (!response.ok) {
                    return response.text().then(msg => {
                        throw new Error(msg || "Error inesperado");
                    });
                }
                return response.json();
            })
            .then(data => {
                console.log(data);
                location.reload(); // Reload page to show new task
            })
            .catch(error => alert("Error saving/updating task"));
    }
    function handleEditTask(event) {
        currentTaskId = event.currentTarget.dataset.taskid;

        const taskItem = event.currentTarget.closest(".task-item");
        const title = taskItem.querySelector(".task-content b").innerText;
        const description = taskItem.querySelector(".task-content p")?.innerHTML || "";

        const imageElement = taskItem.querySelector(".task-image");
        const imagePath = imageElement ? imageElement.style.backgroundImage.replace('url("', '').replace('")', '') : "";

        formNewTask.querySelector("input[name='title']").value = title;
        quill.root.innerHTML = DOMPurify.sanitize(description);
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
    function handleDeleteTask(event) {
        const taskId = event.target.dataset.taskid;
        if (!taskId) {
            console.error("No task selected to delete.");
            return;
        }

        console.log("Deleting task ID: " + taskId);

        const taskItem = event.target.closest(".task-item");

        authFetch(`/api/tasks/${taskId}`, {
            method: "DELETE"
        })
            .then(response => {
                if (response.ok) {
                    taskItem.style.transition = "opacity 0.3s ease-out";
                    taskItem.style.opacity = "0";
                    setTimeout(() => {
                        taskItem.remove();
                    }, 300);
                } else if (response.status === 401) {
                    alert("No tienes permiso para eliminar esta tarea.");
                } else if (response.status === 404) {
                    alert("Tarea no encontrada.");
                } else {
                    console.error("Error deleting task:", response.status);
                }
            })
            .catch(error => console.error("Request error:", error));

    }

    function handleGetLatestReport(event) {
        const taskId = event.currentTarget.dataset.taskid;

        authFetch(`/api/tasks/${taskId}/file`, {
            method: "HEAD"
        })
            .then(response => {
                if (response.ok) {
                    // Open in a new tab
                    window.open(`/api/tasks/${taskId}/file`, "_blank");

                    setTimeout(() => {
                        location.reload();
                    }, 500);
                } else if (response.status === 404) {
                    alert("Informe no encontrado en disco. Genera uno nuevo.");
                } else if (response.status === 401) {
                    alert("No tienes permiso para ver este informe.");
                } else {
                    alert("Error al acceder al informe.");
                }
            })
            .catch(error => {
                console.error("Error comprobando informe:", error);
                alert("Error al comprobar si existe el informe.");
            });
    }
    function handleGenerateReport(event) {
        const taskId = event.currentTarget.dataset.taskid;

        authFetch(`/api/tasks/${taskId}/report`, {
            method: "GET"
        })
            .then(response => {
                if (!response.ok) {
                    if (response.status === 401) {
                        alert("No tienes permiso.");
                    } else {
                        alert("Error generando el informe.");
                    }
                    throw new Error("Error generando informe");
                }
                return response.blob().then(blob => {
                    const contentDisposition = response.headers.get("Content-Disposition");
                    let filename = "informe.pdf";
                    if (contentDisposition && contentDisposition.includes("filename=")) {
                        filename = contentDisposition.split("filename=")[1].replace(/"/g, "");
                    }

                    const blobUrl = window.URL.createObjectURL(blob);
                    const link = document.createElement("a");
                    link.href = blobUrl;
                    link.download = filename;
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);

                    window.open(blobUrl, '_blank');

                    setTimeout(() => {
                        location.reload();
                    }, 500);
                });
            })
            .catch(err => console.error("Error:", err));
    }

    /**
     * @typedef {Object} Task
     * @property {number} id
     * @property {string} title
     * @property {string} description
     * @property {boolean} hasImage
     */
    /**
     * @typedef {Object} ImageData
     * @property {string} base64
     */
    /**
     * @param {Task[]} tasks
     */
    function renderTaskList(tasks) {
        const taskList = document.getElementById("task-list");
        taskList.innerHTML = "";

        tasks.forEach(task => {
            const li = document.createElement("li");
            li.className = "task-item";
            li.dataset.taskid = task.id.toString(10);

            const content = document.createElement("div");
            content.className = "task-content";
            content.innerHTML = `<b>${task.title}</b><p>${task.description}</p>`;

            const btn = document.createElement("button");
            btn.className = "btnMoreOptions";
            btn.dataset.taskid = task.id.toString(10);
            btn.innerHTML = `<img src="/img/menu.png" alt="More options" style="width:16px; height:16px;">`;
            content.appendChild(btn);

            li.appendChild(content);

            const modal = document.createElement("div");
            modal.className = "modalOptions modal";
            modal.innerHTML = `
            <div class="modal-content">
                <h2>${task.title}</h2>
                <button class="btnDeleteTask" data-taskid="${task.id}">Eliminar Tarea</button>
                <button class="btnEditTask" data-taskid="${task.id}">Editar Tarea</button>
            </div>
        `;
            li.appendChild(modal);

            taskList.appendChild(li);

            // Loads image after
            if (task.hasImage) {
                /** @type {Promise<ImageData>} */
                authFetch(`/api/tasks/${task.id}/image`, {
                    method: 'GET'
                })
                    .then(res => res.json())
                    .then(data => {
                        const img = document.createElement("img");
                        img.src = `data:image/jpeg;base64,${data.base64}`;
                        img.style.maxWidth = "200px";
                        content.appendChild(img);
                    });
            }
        });

        assignEventsButtons();
    }

    document.getElementById("searchForm").addEventListener("submit", function (e) {
        e.preventDefault();

        const title = document.getElementById("searchTitle").value;
        const hasImage = document.getElementById("filterImage").checked;

        const url = new URL("/api/tasks/search", window.location.origin);
        if (title) url.searchParams.append("title", title);
        url.searchParams.append("hasImage", hasImage);
        url.searchParams.append("projectID", projectID);

        authFetch(url, {
            method: 'GET'
        })
            .then(res => res.json())
            .then(tasks => {
                renderTaskList(tasks);
                assignEventsButtons();
            })
            .catch(err => console.error("Error al buscar tareas:", err));
    });

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


    // Initial event setup
    assignEventsButtons();
});

