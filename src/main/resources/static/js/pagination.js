document.addEventListener("DOMContentLoaded", () => {
    const uls = Array.from(document.querySelectorAll("ul"));

    const itemsPerPage = 4; // El máximo por página

    uls.forEach(ul => {
        const listItems = Array.from(ul.querySelectorAll(".group-item, .project-item"));
        if (listItems.length <= itemsPerPage) return; // Si no hay suficientes, no paginar

        // Crear controles de paginación
        const paginationControls = document.createElement("div");
        paginationControls.className = "pagination-controls";

        const prevBtn = document.createElement("button");
        prevBtn.textContent = "Anterior";
        prevBtn.disabled = true;

        const pageLabel = document.createElement("span");
        pageLabel.textContent = "Página 1";

        const nextBtn = document.createElement("button");
        nextBtn.textContent = "Siguiente";

        paginationControls.append(prevBtn, pageLabel, nextBtn);
        ul.insertAdjacentElement("afterend", paginationControls);

        const totalPages = Math.ceil(listItems.length / itemsPerPage);
        let currentPage = 1;

        const renderPage = (page) => {
            const start = (page - 1) * itemsPerPage;
            const end = start + itemsPerPage;

            listItems.forEach(item => {
                item.style.display = "none";
                item.style.opacity = 0;
                item.style.pointerEvents = "none";
            });

            listItems.slice(start, end).forEach(item => {
                item.style.display = "flex"; // O "block", depende del diseño
                void item.offsetWidth; // Forzar reflow
                item.style.opacity = 1;
                item.style.pointerEvents = "auto";
            });

            pageLabel.textContent = `P\u00E1gina ${page}`;
            prevBtn.disabled = page === 1;
            nextBtn.disabled = page === totalPages;
        };

        prevBtn.addEventListener("click", () => {
            if (currentPage > 1) {
                currentPage--;
                renderPage(currentPage);
            }
        });

        nextBtn.addEventListener("click", () => {
            if (currentPage < totalPages) {
                currentPage++;
                renderPage(currentPage);
            }
        });

        // Mostrar controles solo si hay más de itemsPerPage
        if (listItems.length > itemsPerPage) {
            paginationControls.style.display = "flex";
            renderPage(currentPage);
        } else {
            paginationControls.style.display = "none";
        }
    });
});
