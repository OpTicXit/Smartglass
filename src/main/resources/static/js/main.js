/* ============================================================
   SmartGlass — Experiencia de Usuario Premium y Estética
============================================================ */

document.addEventListener('DOMContentLoaded', () => {
    revelarAlEntrar();
    rebotarCarritoSiHuboExito();
    scrollSuaveEnAnclas();
    efectoIluminacionSmartGlass();
    efectoMagnetico();
});

/* --- 1. Scroll-reveal fluido al estilo Apple --- */
function revelarAlEntrar() {
    const selectores = [
        '.producto-card', '.stat-box', '.stat-card', '.stat-pedido',
        '.pedido-card', '.cotizacion-card', '.card-panel', '.pilar-box',
        '.info-box', '.tech-card', '.co2-card'
    ];
    const elementos = document.querySelectorAll(selectores.join(','));
    if (!elementos.length) return;

    elementos.forEach(el => el.classList.add('sg-reveal'));

    if (!('IntersectionObserver' in window)) {
        elementos.forEach(el => el.classList.add('sg-reveal-visible'));
        return;
    }

    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry, index) => {
            if (entry.isIntersecting) {
                // Staggering (retraso en cascada) dinámico para que no aparezcan de golpe
                setTimeout(() => {
                    entry.target.classList.add('sg-reveal-visible');
                }, index * 80); 
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.15, rootMargin: "0px 0px -50px 0px" });

    elementos.forEach(el => observer.observe(el));
}

/* --- 2. Interacción dinámica de iluminación en el "Cristal" (Efecto Hover) --- */
function efectoIluminacionSmartGlass() {
    const tarjetas = document.querySelectorAll('.producto-card, .info-box, .tech-card, .card-panel');
    
    tarjetas.forEach(tarjeta => {
        tarjeta.addEventListener('mousemove', (e) => {
            const rect = tarjeta.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            
            // Pasamos las coordenadas exactas del ratón al CSS
            tarjeta.style.setProperty('--mouse-x', `${x}px`);
            tarjeta.style.setProperty('--mouse-y', `${y}px`);
        });
    });
}

/* --- 3. Efecto Magnético para Iconos (Carrito, Botones) --- */
function efectoMagnetico() {
    const magnetos = document.querySelectorAll('.sg-magnetico, a[href="/carrito"]');
    
    magnetos.forEach(magneto => {
        magneto.addEventListener('mousemove', (e) => {
            const posicion = magneto.getBoundingClientRect();
            const x = e.clientX - posicion.left - posicion.width / 2;
            const y = e.clientY - posicion.top - posicion.height / 2;
            
            magneto.style.transform = `translate(${x * 0.3}px, ${y * 0.3}px) scale(1.1)`;
        });

        magneto.addEventListener('mouseleave', () => {
            magneto.style.transform = 'translate(0px, 0px) scale(1)';
        });
    });
}

/* --- 4. Rebote Orgánico del carrito tras una compra exitosa --- */
function rebotarCarritoSiHuboExito() {
    const flashExito = document.querySelector('.alert-success, .flash-success');
    if (!flashExito) return;

    const linkCarrito = document.querySelector('a[href="/carrito"]');
    if (!linkCarrito) return;

    // Añadimos una clase para un rebote elástico
    setTimeout(() => {
        linkCarrito.classList.add('sg-cart-bounce-premium');
    }, 500);

    linkCarrito.addEventListener('animationend', () => {
        linkCarrito.classList.remove('sg-cart-bounce-premium');
    }, { once: true });
}

/* --- 5. Scroll suave para anclas --- */
function scrollSuaveEnAnclas() {
    document.querySelectorAll('a[href^="#"]').forEach(link => {
        link.addEventListener('click', (event) => {
            const destino = document.querySelector(link.getAttribute('href'));
            if (!destino) return;
            event.preventDefault();
            destino.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    });
}