(() => {
    const listContainer = document.querySelector("#group-list");
    const spinner = document.querySelector("#spinner");
    const prevPageBtn = document.querySelector("#prevPage");
    const nextPageBtn = document.querySelector("#nextPage");
    const currentPageDisplay = document.querySelector("#currentPage");

    let pageIndex = 0; // Página inicial
    const itemsPerPage = 5; // Número de grupos por página
    let maxPages = Infinity;

    const createGroupElement = ({ id, name }) => {
        const item = document.createElement("li");
        item.classList.add("group-item");
        item.dataset.groupid = id;

        const contentDiv = document.createElement("div");
        contentDiv.classList.add("group-content");
        contentDiv.innerHTML = `
            <b>${name}</b>
            <button class="btnMoreOptions" data-groupid="${id}">
                <img src="/img/menu.png" alt="Más opciones">
            </button>
        `;

        // Modal de opciones para este grupo
        const modalDiv = document.createElement("div");
        modalDiv.classList.add("modalOptions", "modal", "hidden");
        modalDiv.id = `modalOptions-${id}`;
        modalDiv.innerHTML = `
            <div class="modal-content">
                <h2>${name}</h2>
                <button class="btnEditGroup" data-groupid="${id}">Editar Grupo</button>
                <button class="btnManageMembers" data-groupid="${id}">Gestionar Miembros</button>
                <button class="btnDeleteGroup" data-groupid="${id}">Eliminar Grupo</button>
            </div>
        `;

        item.appendChild(contentDiv);
        item.appendChild(modalDiv);
        return item;
    };

    const addGroupsToDOM = (groups) => {
        listContainer.innerHTML = ""; // Limpia la lista actual
        const fragments = document.createDocumentFragment();
        groups.forEach(group => fragments.appendChild(createGroupElement(group)));
        listContainer.appendChild(fragments);
    };

    const updatePaginationControls = () => {
        currentPageDisplay.textContent = `Página ${pageIndex + 1}`;
        prevPageBtn.disabled = pageIndex === 0;
        nextPageBtn.disabled = pageIndex + 1 >= maxPages;
    };

    const loadGroups = async () => {
        spinner.style.display = "block";
        try {
            const res = await fetch(`/paginated_groups?page=${pageIndex}&size=${itemsPerPage}`);
            if (!res.ok) {
                throw new Error("Error al cargar los grupos");
            }
            const { content, totalPages } = await res.json();

            addGroupsToDOM(content);
            maxPages = totalPages;

            // Reasigna los eventos a los botones de los elementos dinámicamente creados
            if (typeof assignGroupButtonEvents === "function") {
                assignGroupButtonEvents();
            }

            updatePaginationControls();
        } catch (error) {
            console.error("Error cargando los grupos:", error);
        } finally {
            spinner.style.display = "none";
        }
    };

    // Manejo de botones de paginación
    prevPageBtn.addEventListener("click", () => {
        if (pageIndex > 0) {
            pageIndex--;
            loadGroups();
        }
    });

    nextPageBtn.addEventListener("click", () => {
        if (pageIndex + 1 < maxPages) {
            pageIndex++;
            loadGroups();
        }
    });

    // Cargar los grupos al inicio
    loadGroups();
})();