document.addEventListener("DOMContentLoaded", () => {
    const userId = document.body.dataset.userid;
    const itemsPerPage = 5;

    let listContainer = document.getElementById("group-list");
    if (!listContainer) listContainer = document.getElementById("project-list");
    listContainer.style.minHeight = `${itemsPerPage * 100}px`;
    listContainer.style.maxHeight = `${itemsPerPage * 100}px`;

    const config = [
        {
            listId: "group-list",
            endpoint: `/api/groups/p/${userId}`,
            itemClass: "group-item",
            renderItem: renderGroupItem
        },
        {
            listId: "project-list",
            endpoint: `/api/projects/p/${userId}`,
            itemClass: "project-item",
            renderItem: renderProjectItem
        }
    ];

    config.forEach(({ listId, endpoint, itemClass, renderItem }) => {
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
        pageLabel.textContent = "P\u00E1gina 1";

        const nextBtn = document.createElement("button");
        nextBtn.textContent = "Siguiente";

        paginationControls.append(prevBtn, pageLabel, nextBtn);
        ul.insertAdjacentElement("afterend", paginationControls);

        const loadData = async (page = 0) => {
            try {
                const response = await fetch(`${endpoint}?page=${page}&size=${itemsPerPage}`);
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

                pageLabel.textContent = `P\u00E1gina ${currentPage + 1}`;
                prevBtn.disabled = currentPage === 0;
                nextBtn.disabled = currentPage + 1 >= totalPages;

                if (window.assignGroupButtonEvents) {
                    window.assignGroupButtonEvents(); // solo se aplica a grupos
                }
            } catch (err) {
                console.error(`Error cargando ${listId}:`, err);
            }
        };

        prevBtn.addEventListener("click", () => {
            if (currentPage > 0) loadData(currentPage - 1);
        });

        nextBtn.addEventListener("click", () => {
            if (currentPage + 1 < totalPages) loadData(currentPage + 1);
        });

        loadData();
    });

    function renderGroupItem(group) {
        const li = document.createElement("li");
        li.className = "group-item";
        li.dataset.groupid = group.id;

        const content = document.createElement("div");
        content.className = "group-content";

        const title = document.createElement("b");
        title.textContent = group.name;

        const btnOptions = document.createElement("button");
        btnOptions.className = "btnMoreOptions";
        btnOptions.dataset.groupid = group.id;
        btnOptions.innerHTML = `<img src="/img/menu.png" alt="Más opciones">`;

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

    function renderProjectItem(project) {
        const li = document.createElement("li");
        li.className = "project-item";
        li.dataset.projectid = project.id;

        const content = document.createElement("div");
        content.className = "project-content";

        const link = document.createElement("a");
        link.href = `/project/${project.id}`;
        link.innerHTML = `<b>${project.name}</b>`;

        const btnOptions = document.createElement("button");
        btnOptions.className = "btnMoreOptions";
        btnOptions.dataset.projectid = project.id;
        btnOptions.innerHTML = `<img src="/img/menu.png" alt="Más opciones">`;

        content.append(link, btnOptions);
        li.appendChild(content);

        const modal = document.createElement("div");
        modal.className = "modalOptions modal hidden";
        modal.id = `modalOptions-${project.id}`;

        const modalContent = document.createElement("div");
        modalContent.className = "modal-content";
        modalContent.innerHTML = `
            <h2>${project.name}</h2>
            <button class="btnDeleteProject" data-projectid="${project.id}">Eliminar Proyecto</button>
            <button class="btnEditProject" data-projectid="${project.id}">Editar Proyecto</button>
        `;

        modal.appendChild(modalContent);
        li.appendChild(modal);

        return li;
    }
});
