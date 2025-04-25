document.addEventListener("DOMContentLoaded", () => {
    const groupList = document.getElementById("group-list");
    const listItems = Array.from(groupList.querySelectorAll(".group-item"));
    const prevBtn = document.getElementById("prevPage");
    const nextBtn = document.getElementById("nextPage");
    const currentPageLabel = document.getElementById("currentPage");

    const itemsPerPage = 4;
    let currentPage = 1;
    const totalPages = Math.ceil(listItems.length / itemsPerPage);


    const renderPage = (page) => {
        const start = (page - 1) * itemsPerPage;
        const end = start + itemsPerPage;

        // PRIMERO, ocultar todos directamente
        listItems.forEach(item => {
            item.style.display = "none";
            item.style.opacity = 0;
            item.style.pointerEvents = "none";
        });

        // LUEGO, mostrar los de la página actual con fade-in
        listItems.slice(start, end).forEach(item => {
            item.style.display = "flex"; // O "block", depende de tu diseño
            item.style.opacity = 0; // Empieza invisible
            void item.offsetWidth; // Forzar reflow para que reconozca el cambio
            item.style.opacity = 1; // Fade-in bonito
            item.style.pointerEvents = "auto";
        });

        currentPageLabel.textContent = `P\u00E1gina ${page}`;
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

    // Si hay más de 10, activar paginación
    if (listItems.length > itemsPerPage) {
        renderPage(currentPage);
        document.querySelector(".pagination-controls").style.display = "flex";
    } else {
        // No hay necesidad de paginación
        document.querySelector(".pagination-controls").style.display = "none";
    }
});
