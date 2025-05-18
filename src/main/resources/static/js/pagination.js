document.addEventListener("DOMContentLoaded", () => {
    let activeLoadData = null;
    let activeCurrentPage = 0;
    const itemHeight = 120; // every item 120px aprox.
    const windowHeight = window.innerHeight;
    let itemsPerPage = Math.max(1, Math.floor(windowHeight / itemHeight));
    let listContainer = document.getElementById("group-list");
    if (!listContainer) listContainer = document.getElementById("project-list");
    listContainer.style.minHeight = `${itemsPerPage * 100}px`;
    listContainer.style.maxHeight = `${itemsPerPage * 100}px`;

    const config = [
        {
            listId: "group-list",
            endpoint: `/api/groups/p`,
            renderItem: renderGroupItem
        },
        {
            listId: "project-list",
            endpoint: `/api/projects/p`,
            renderItem: renderProjectItem
        }
    ];
    config.forEach(({ listId, endpoint, renderItem }) => {
        const ul = document.getElementById(listId);
        if (!ul) return;

        let currentPage = 0;
        let totalPages = 1;

        const paginationControls = document.createElement("div");
        paginationControls.className = "pagination-controls";

        const prevBtn = document.createElement("button");
        prevBtn.textContent = "Anterior";
        prevBtn.disabled = true;

        const pageLabel = document.createElement("span");
        pageLabel.textContent = "P&#225;gina 1";

        const nextBtn = document.createElement("button");
        nextBtn.textContent = "Siguiente";

        paginationControls.append(prevBtn, pageLabel, nextBtn);
        ul.insertAdjacentElement("afterend", paginationControls);

        const loadData = async (page = 0) => {
            try {
                const response = await authFetch(`${endpoint}?page=${page}&size=${itemsPerPage}`, {
                    method: 'GET'
                });

                /**
                 * @typedef {Object} PaginationData
                 * @property {number} totalPages
                 * @property {number} number
                 * @property {Array<Object>} content
                 */
                /**
                 * @type {PaginationData}
                 */
                const data = await response.json();

                ul.innerHTML = "";
                totalPages = data.totalPages;
                currentPage = data.number;

                if (totalPages <= 1 || data.content.length === 0) {
                    paginationControls.style.display = "none";
                } else {
                    paginationControls.style.display = "flex";
                }

                data.content.forEach(item => {
                    const li = renderItem(item);
                    ul.appendChild(li);
                });

                pageLabel.textContent = `P&#225;gina ${currentPage + 1}`;
                prevBtn.disabled = currentPage === 0;
                nextBtn.disabled = currentPage + 1 >= totalPages;

                if (window.assignGroupButtonEvents) {
                    window.assignGroupButtonEvents();
                }
                if (window.assignProjectButtonEvents) {
                    window.assignProjectButtonEvents();
                }

                if (ul.offsetParent !== null) {
                    activeLoadData = loadData;
                    activeCurrentPage = currentPage;
                }
            } catch (err) {
                console.error(`Error cargando ${listId}:`, err);
            }
        };

        prevBtn.addEventListener("click", () => {
            if (currentPage > 0) void loadData(currentPage - 1);
        });

        nextBtn.addEventListener("click", () => {
            if (currentPage + 1 < totalPages) void loadData(currentPage + 1);
        });

        void loadData();
    });

    /**
     * @param {{ id: number, name: string, isPersonal: boolean, isOwner: boolean }} group
     */
    function renderGroupItem(group) {
        const li = document.createElement("li");
        li.className = "group-item";
        li.dataset.groupid = group.id.toString(10);

        const content = document.createElement("div");
        content.className = "group-content";

        const title = document.createElement("b");
        title.textContent = group.name;

        const btnOptions = document.createElement("button");
        btnOptions.className = "btnMoreOptions";
        btnOptions.dataset.groupid = group.id.toString(10);
        btnOptions.innerHTML = `<img src="/img/menu.png" alt="More options">`;

        content.append(title, btnOptions);
        li.appendChild(content);

        const modal = document.createElement("div");
        modal.className = "modalOptions modal hidden";
        modal.id = `modalOptions-${group.id}`;

        const modalContent = document.createElement("div");
        modalContent.className = "modal-content";
        modalContent.innerHTML = `<h2>${group.name}</h2>`;

        if (!group.isPersonal) {
            if (group.isOwner) {
                modalContent.innerHTML += `
                    <button class="btnEditGroup" data-groupid="${group.id}">Editar Grupo</button>
                    <button class="btnManageMembers" data-groupid="${group.id}">Gestionar Miembros</button>
                    <button class="btnChangeOwner" data-groupid="${group.id}" data-groupname="${group.name}">Cambiar Propietario</button>
                    <button class="btnDeleteGroup" data-groupid="${group.id}">Eliminar Grupo</button>
                `;
            } else {
                modalContent.innerHTML += `
                    <button class="btnLeaveGroup" data-groupid="${group.id}">Salir del Grupo</button>
                `;
            }
        }

        modal.appendChild(modalContent);
        li.appendChild(modal);

        return li;
    }

    /**
     * @param {{ id: number, name: string, groupId: number, owner: boolean }} project
     */
    function renderProjectItem(project) {
        const li = document.createElement("li");
        li.className = "project-item";
        li.dataset.projectid = project.id.toString(10);
        li.dataset.groupid = project.groupId.toString(10);

        const content = document.createElement("div");
        content.className = "project-content";

        const link = document.createElement("a");
        link.href = "#";
        link.innerHTML = `<b>${project.name}</b>`;

        link.addEventListener("click", (e) => {
            e.preventDefault();
            const url = `/project/${project.id}`;

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
                    console.error("Error al cargar el proyecto:", err);
                    alert("No autorizado o error de carga.");
                });
        });

        const btnOptions = document.createElement("button");
        btnOptions.className = "btnMoreOptions";
        btnOptions.dataset.projectid = project.id.toString(10);
        btnOptions.innerHTML = `<img src="/img/menu.png" alt="More options">`;

        content.append(link, btnOptions);
        li.appendChild(content);

        const modal = document.createElement("div");
        modal.className = "modalOptions modal hidden";
        modal.id = `modalOptions-${project.id}`;

        const modalContent = document.createElement("div");
        modalContent.className = "modal-content";
        modalContent.innerHTML = `<h2>${project.name}</h2>`;
        if (project.owner) {
            modalContent.innerHTML += `
            <button class="btnDeleteProject" data-projectid="${project.id}">Eliminar Proyecto</button>
            <button class="btnEditProject" data-projectid="${project.id}">Editar Proyecto</button>
        `;
        }

        modal.appendChild(modalContent);
        li.appendChild(modal);

        return li;
    }

    function handlePaginationAfterDelete() {
        if (activeLoadData) {
            const remainingItems = document.querySelectorAll("#group-list li, #project-list li").length;

            if (remainingItems === 0 && activeCurrentPage > 0) {
                activeLoadData(activeCurrentPage - 1);
            } else {
                activeLoadData(activeCurrentPage);
            }
        }
    }
    window.handlePaginationAfterDelete = handlePaginationAfterDelete;

    // Global listener to resize and adapt the content
    window.addEventListener("resize", () => {
        itemsPerPage = Math.max(1, Math.floor(window.innerHeight / itemHeight));

        let listContainer = document.getElementById("group-list") || document.getElementById("project-list");
        if (listContainer) {
            listContainer.style.minHeight = `${itemsPerPage * 100}px`;
            listContainer.style.maxHeight = `${itemsPerPage * 100}px`;
        }

        if (activeLoadData) {
            activeLoadData(activeCurrentPage);
        }
    });
});
