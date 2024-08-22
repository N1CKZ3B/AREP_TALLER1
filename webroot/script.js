document.getElementById("fetchServices").addEventListener("click", () => {
    fetch('/api/services', { method: 'GET' })
        .then(response => response.json())
        .then(data => {
            const servicesDiv = document.getElementById("servicesData");
            servicesDiv.innerHTML = '<h3>Servicios Disponibles:</h3><ul>';
            data.services.forEach(service => {
                servicesDiv.innerHTML += `<li>${service}</li>`;
            });
            servicesDiv.innerHTML += '</ul>';
        });
});
