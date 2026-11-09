/* SmartGlass - globo de emisiones del informe tecnico (Three.js).
   Continentes como puntos; se encienden al azar paises con muchas emisiones (datos reales del JSON).
   Arrastrar mueve el globo; rueda, pellizco y botones acercan o alejan. Modo fotosensible y movimiento
   reducido: sin giro ni destellos, los paises principales quedan encendidos fijos. */
const main = document.getElementById('contenido');
const escena = document.getElementById('globo-escena');
const lienzo = document.getElementById('globo-lienzo');
const etiquetas = document.getElementById('globo-etiquetas');
const listaZonas = document.getElementById('globo-zonas');
const botonPausa = document.getElementById('globo-pausa');

const ES_INGLES = /^en/.test(window.SG_LANG);
const ENTERO = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 });
const NUM = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });
const ZONAS_LISTA = 5;
const MAX_ETIQUETAS = 4;
const VIDA_ZONA = 5.5;          // segundos: encendido lento, sin destellos
const CADA_ZONA = 1300;         // ms entre zonas nuevas
const GIRO = 0.075;             // rad/s
const REPOSO_MS = 2500;         // tras soltar, vuelve a girar solo
const Z_INICIAL = 3.9, Z_CERCA = 1.65, Z_LEJOS = 5.2;
const PITCH_MAX = 1.25;

const movimientoReducido = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;

function aviso(texto) {
    const a = document.getElementById('globo-aviso');
    a.textContent = texto;
    a.hidden = false;
    document.querySelector('.globo-controles').hidden = true;
    lienzo.style.display = 'none';
}

/* lat/lon -> esfera de radio r; misma convencion en las zonas y en los puntos */
function haciaXYZ(THREE, lat, lon, r = 1) {
    const f = (90 - lat) * Math.PI / 180, t = (lon + 180) * Math.PI / 180;
    return new THREE.Vector3(-r * Math.sin(f) * Math.cos(t), r * Math.cos(f), r * Math.sin(f) * Math.sin(t));
}

function mascaraTierra(tierra) {
    const W = 2048, H = 1024, c = document.createElement('canvas');
    c.width = W; c.height = H;
    const x = c.getContext('2d', { willReadFrequently: true });
    x.fillStyle = '#fff';
    const geometrias = tierra.features ? tierra.features.map(f => f.geometry) : [tierra.geometry];
    for (const g of geometrias) {
        for (const poli of g.type === 'Polygon' ? [g.coordinates] : g.coordinates) {
            x.beginPath();
            for (const anillo of poli) anillo.forEach(([lo, la], i) => {
                const px = (lo + 180) / 360 * W, py = (90 - la) / 180 * H;
                if (i) x.lineTo(px, py); else x.moveTo(px, py);
            });
            x.fill('evenodd');
        }
    }
    const d = x.getImageData(0, 0, W, H).data;
    return (lat, lon) => d[(Math.min(H - 1, ((90 - lat) / 180 * H) | 0) * W + Math.min(W - 1, ((lon + 180) / 360 * W) | 0)) * 4] > 128;
}

function dentro(lon, lat, anillos) {
    let en = false;
    for (const a of anillos) {
        for (let i = 0, j = a.length - 1; i < a.length; j = i++) {
            const [xi, yi] = a[i], [xj, yj] = a[j];
            if ((yi > lat) !== (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) en = !en;
        }
    }
    return en;
}

/* Puntos de cada pais; la etiqueta va al centro del poligono con mas puntos (no en un territorio lejano) */
function puntosDelPais(THREE, geometria, puntos) {
    const indices = [];
    let mejor = [];
    for (const poli of geometria.type === 'Polygon' ? [geometria.coordinates] : geometria.coordinates) {
        let minX = 180, maxX = -180, minY = 90, maxY = -90;
        for (const [x, y] of poli[0]) { minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
        const aqui = [];
        puntos.forEach((p, i) => {
            if (p.lon >= minX && p.lon <= maxX && p.lat >= minY && p.lat <= maxY && dentro(p.lon, p.lat, poli)) aqui.push(i);
        });
        indices.push(...aqui);
        if (aqui.length > mejor.length) mejor = aqui;
    }
    const centro = new THREE.Vector3();
    for (const i of mejor) centro.add(puntos[i].v);
    if (centro.lengthSq() === 0) return null;
    centro.normalize();
    const radio = Math.max(...mejor.map(i => puntos[i].v.angleTo(centro)), 0.05);
    return { indices: indices.map(i => [i, Math.max(0.35, 1 - puntos[i].v.angleTo(centro) / (radio * 1.15))]), centro, radio };
}

function textura(THREE, dibujar, n = 128) {
    const c = document.createElement('canvas');
    c.width = c.height = n;
    dibujar(c.getContext('2d'), n);
    return new THREE.CanvasTexture(c);
}

async function arrancar() {
    let THREE, renderer;
    let datos, topo;
    try {
        [THREE, datos, topo] = await Promise.all([
            import(main.dataset.three),
            window.sgInformeDatos,
            fetch(main.dataset.mapa).then(r => r.json()),
        ]);
        renderer = new THREE.WebGLRenderer({ canvas: lienzo, antialias: true, alpha: true });
    } catch (e) {
        aviso(sgT('inf_sin_3d'));
        return;
    }
    renderer.setPixelRatio(Math.min(devicePixelRatio, 2));

    const nombre = f => ES_INGLES ? f.en : f.es;
    lienzo.setAttribute('aria-label', sgT('inf_globo_aria', '', datos.paises.filas.slice(0, 5).map(nombre).join(', ')));

    const scene = new THREE.Scene();
    const cam = new THREE.PerspectiveCamera(36, 1, 0.1, 100);
    const eje = new THREE.Group();
    eje.rotation.z = -0.3;
    scene.add(eje);
    const globo = new THREE.Group();
    eje.add(globo);

    globo.add(new THREE.Mesh(new THREE.SphereGeometry(0.995, 64, 64), new THREE.MeshBasicMaterial({ color: 0x0A1626 })));

    // Paralelos y meridianos
    const lineas = [];
    for (let la = -75; la <= 75; la += 15) for (let lo = 0; lo < 360; lo += 3) lineas.push(haciaXYZ(THREE, la, lo), haciaXYZ(THREE, la, lo + 3));
    for (let lo = 0; lo < 360; lo += 15) for (let la = -90; la < 90; la += 3) lineas.push(haciaXYZ(THREE, la, lo), haciaXYZ(THREE, la + 3, lo));
    globo.add(new THREE.LineSegments(new THREE.BufferGeometry().setFromPoints(lineas),
        new THREE.LineBasicMaterial({ color: 0x1E6C96, transparent: true, opacity: 0.16 })));

    // Tierra como puntos repartidos con espiral de Fibonacci: densidad pareja, sin amontonarse en los polos
    const esTierra = mascaraTierra(topojson.feature(topo, topo.objects.land));
    const base = new THREE.Color(0x38B6E8), caliente = new THREE.Color(0xF5B041);
    const puntos = [], pos = [], col = [];
    const N = 70000, oro = Math.PI * (3 - Math.sqrt(5));
    for (let i = 0; i < N; i++) {
        const y = 1 - 2 * (i + 0.5) / N, r = Math.sqrt(1 - y * y), t = oro * i;
        const v = new THREE.Vector3(Math.cos(t) * r, y, Math.sin(t) * r);
        const lat = Math.asin(y) * 180 / Math.PI;
        const lon = ((Math.atan2(v.z, -v.x) * 180 / Math.PI) + 360) % 360 - 180;
        if (esTierra(lat, lon)) {
            puntos.push({ v, lat, lon });
            pos.push(v.x, v.y, v.z);
            col.push(base.r, base.g, base.b);
        }
    }
    const geoPuntos = new THREE.BufferGeometry();
    geoPuntos.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    geoPuntos.setAttribute('color', new THREE.Float32BufferAttribute(col, 3));
    const puntoTex = textura(THREE, (x, n) => { x.fillStyle = '#fff'; x.beginPath(); x.arc(n / 2, n / 2, n / 2 - 2, 0, 7); x.fill(); }, 32);
    globo.add(new THREE.Points(geoPuntos, new THREE.PointsMaterial({ size: 0.0105, map: puntoTex, vertexColors: true, transparent: true, alphaTest: 0.4 })));
    const colores = geoPuntos.attributes.color;

    // Atmosfera (no gira con el globo: es un resplandor de borde)
    scene.add(new THREE.Mesh(new THREE.SphereGeometry(1.11, 64, 64), new THREE.ShaderMaterial({
        side: THREE.BackSide, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
        vertexShader: 'varying vec3 n; void main(){ n=normalize(normalMatrix*normal); gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.); }',
        fragmentShader: 'varying vec3 n; void main(){ float a=pow(.72-dot(n,vec3(0.,0.,1.)),3.2); gl_FragColor=vec4(0.,.62,1.,1.)*a; }',
    })));

    // Estrellas
    const estrellas = [];
    for (let i = 0; i < 700; i++) estrellas.push(new THREE.Vector3().randomDirection().multiplyScalar(9 + Math.random() * 6));
    scene.add(new THREE.Points(new THREE.BufferGeometry().setFromPoints(estrellas),
        new THREE.PointsMaterial({ color: 0x8FB6D8, size: 0.022, transparent: true, opacity: 0.55 })));

    // Zonas: un pais por cada fila de datos con su resplandor, anillo y etiqueta
    const brilloTex = textura(THREE, (x, n) => {
        const g = x.createRadialGradient(n / 2, n / 2, 0, n / 2, n / 2, n / 2);
        g.addColorStop(0, 'rgba(255,150,80,.75)'); g.addColorStop(0.2, 'rgba(255,110,40,.3)'); g.addColorStop(1, 'rgba(255,90,20,0)');
        x.fillStyle = g; x.fillRect(0, 0, n, n);
    });
    const porId = new Map(topojson.feature(topo, topo.objects.countries).features.map(f => [Number(f.id), f]));
    const maxCo2 = datos.paises.filas[0].co2_mt;
    const zonas = [];
    for (const fila of datos.paises.filas) {
        const pais = porId.get(fila.id);
        const info = pais && puntosDelPais(THREE, pais.geometry, puntos);
        if (!info) continue;   // sin geometria en el mapa 110m: sigue en la grafica de paises
        const brillo = new THREE.Sprite(new THREE.SpriteMaterial({ map: brilloTex, blending: THREE.AdditiveBlending, transparent: true, depthWrite: false, opacity: 0 }));
        brillo.position.copy(info.centro).multiplyScalar(1.015);
        globo.add(brillo);
        const anillo = new THREE.Mesh(new THREE.RingGeometry(0.92, 1, 48), new THREE.MeshBasicMaterial({ color: 0xF5B041, transparent: true, opacity: 0, side: THREE.DoubleSide, depthWrite: false }));
        anillo.position.copy(info.centro).multiplyScalar(1.006);
        anillo.lookAt(info.centro.clone().multiplyScalar(2));
        globo.add(anillo);
        const etq = document.createElement('div');
        etq.className = 'globo-etq';
        etq.textContent = nombre(fila);
        const sub = document.createElement('small');
        sub.textContent = sgT('inf_mt', '', ENTERO.format(fila.co2_mt), NUM.format(fila.pct));
        etq.appendChild(sub);
        etiquetas.appendChild(etq);
        zonas.push({ fila, ...info, brillo, anillo, etq, t: -1, fuerza: 0.55 + 0.45 * Math.sqrt(fila.co2_mt / maxCo2), tam: 0.12 + 0.1 * Math.sqrt(fila.co2_mt / maxCo2) });
    }

    // Vista: valores objetivo y actuales, para que todo movimiento sea suave
    const inicial = { yaw: 3.1, pitch: 0.28, z: Z_INICIAL };
    const vista = { ...inicial }, meta = { ...inicial };
    let pausado = false, reducido = movimientoReducido(), ultimaInteraccion = -Infinity;
    const zPorAncho = () => 1 / Math.min(1, escena.clientWidth / escena.clientHeight);

    const mover = (dYaw, dPitch) => {
        meta.yaw += dYaw;
        meta.pitch = Math.max(-PITCH_MAX, Math.min(PITCH_MAX, meta.pitch + dPitch));
        ultimaInteraccion = performance.now();
    };
    const zoom = factor => {
        meta.z = Math.max(Z_CERCA, Math.min(Z_LEJOS, meta.z * factor));
        ultimaInteraccion = performance.now();
    };
    const sensibilidad = () => (vista.z - 0.75) * 0.0016;

    /* Puntero: un dedo o raton mueve; dos dedos acercan */
    const activos = new Map();
    let pellizco = 0;
    lienzo.addEventListener('pointerdown', e => {
        lienzo.setPointerCapture(e.pointerId);
        activos.set(e.pointerId, [e.clientX, e.clientY]);
        lienzo.classList.add('arrastrando');
    });
    lienzo.addEventListener('pointermove', e => {
        const previo = activos.get(e.pointerId);
        if (!previo) return;
        if (activos.size === 1) {
            mover((e.clientX - previo[0]) * sensibilidad(), (e.clientY - previo[1]) * sensibilidad());
        }
        activos.set(e.pointerId, [e.clientX, e.clientY]);
        if (activos.size === 2) {
            const [a, b] = [...activos.values()];
            const dist = Math.hypot(a[0] - b[0], a[1] - b[1]);
            if (pellizco) zoom(pellizco / dist);
            pellizco = dist;
        }
    });
    const soltar = e => {
        activos.delete(e.pointerId);
        pellizco = 0;
        if (!activos.size) lienzo.classList.remove('arrastrando');
    };
    lienzo.addEventListener('pointerup', soltar);
    lienzo.addEventListener('pointercancel', soltar);

    /* Rueda: acerca o aleja solo con la escena entera a la vista; al tope de lejos deja bajar por la pagina */
    lienzo.addEventListener('wheel', e => {
        const caja = escena.getBoundingClientRect();
        const entera = caja.top > -40 && caja.bottom < innerHeight + 40;
        if (!entera || (e.deltaY > 0 && meta.z >= Z_LEJOS)) return;
        e.preventDefault();
        zoom(Math.exp(e.deltaY * 0.0012));
    }, { passive: false });

    const accion = {
        arriba: () => mover(0, -0.3), abajo: () => mover(0, 0.3),
        izquierda: () => mover(-0.3, 0), derecha: () => mover(0.3, 0),
        acercar: () => zoom(0.82), alejar: () => zoom(1.22),
        centro: () => { Object.assign(meta, inicial, { yaw: vista.yaw - (vista.yaw - inicial.yaw) % (Math.PI * 2) }); ultimaInteraccion = -Infinity; },
    };
    escena.querySelectorAll('[data-globo]').forEach(b => b.addEventListener('click', () => accion[b.dataset.globo]()));
    const TECLAS = { ArrowUp: 'arriba', ArrowDown: 'abajo', ArrowLeft: 'izquierda', ArrowRight: 'derecha', '+': 'acercar', '=': 'acercar', '-': 'alejar', '0': 'centro' };
    escena.addEventListener('keydown', e => {
        const a = TECLAS[e.key];
        if (!a || e.altKey || e.ctrlKey || e.metaKey) return;
        e.preventDefault();
        accion[a]();
    });

    const textoPausa = () => { botonPausa.textContent = sgT(pausado || reducido ? 'inf_reanudar' : 'inf_pausar'); };
    botonPausa.addEventListener('click', () => {
        if (reducido) { reducido = false; pausado = false; }   // la persona pide el giro expresamente
        else pausado = !pausado;
        limpiarZonas();
        textoPausa();
    });
    textoPausa();

    function limpiarZonas() {
        for (const z of zonas) {
            z.t = -1;
            z.brillo.material.opacity = 0;
            z.anillo.material.opacity = 0;
            z.etq.style.opacity = 0;
            for (const [i] of z.indices) colores.setXYZ(i, base.r, base.g, base.b);
        }
        colores.needsUpdate = true;
        if (reducido) pintarLista(datos.paises.filas.slice(0, ZONAS_LISTA));
    }
    new MutationObserver(() => {
        const ahora = movimientoReducido();
        if (ahora !== reducido) { reducido = ahora; limpiarZonas(); textoPausa(); }
    }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-a11y'] });

    const medir = () => {
        const w = escena.clientWidth, h = escena.clientHeight;
        renderer.setSize(w, h, false);
        cam.aspect = w / h;
        cam.updateProjectionMatrix();
    };
    addEventListener('resize', medir);
    medir();

    // Dibujar solo con la escena a la vista: leyendo graficas no hay por que gastar GPU
    let visible = true, enCola = false;
    const pedir = () => { if (visible && !enCola) { enCola = true; requestAnimationFrame(cuadro); } };
    new IntersectionObserver(([e]) => { visible = e.isIntersecting; pedir(); }).observe(escena);

    const v3 = new THREE.Vector3();
    const frente = z => v3.copy(z.centro).applyMatrix4(globo.matrixWorld).normalize().z;
    let recientes = [];
    let ultimaZona = 0, reloj = performance.now();
    limpiarZonas();

    function pintarLista(filas) {
        listaZonas.replaceChildren(...filas.map(f => {
            const li = document.createElement('li');
            const nom = document.createElement('span'), val = document.createElement('b');
            nom.textContent = nombre(f);
            val.textContent = ENTERO.format(f.co2_mt) + ' Mt';
            li.append(nom, val);
            return li;
        }));
    }

    function encender() {
        const libres = zonas.filter(z => z.t < 0 && frente(z) > 0.28);
        if (!libres.length) return;
        const z = libres[Math.random() * libres.length | 0];
        z.t = 0;
        recientes = [z.fila, ...recientes.filter(f => f !== z.fila)].slice(0, ZONAS_LISTA);
        pintarLista(recientes);
    }

    function pintarZona(z, e, conPulso) {
        for (const [i, peso] of z.indices) {
            const k = Math.min(1, e * (0.5 + 0.5 * peso) * z.fuerza * 1.7);
            colores.setXYZ(i, base.r + (caliente.r - base.r) * k, base.g + (caliente.g - base.g) * k, base.b + (caliente.b - base.b) * k);
        }
        z.brillo.material.opacity = e * 0.85;
        z.brillo.scale.setScalar(z.tam + 0.06 * e);
        if (conPulso) {
            const fase = (z.t % 2.4) / 2.4;
            z.anillo.scale.setScalar(0.02 + z.tam * 0.8 * fase);
            z.anillo.material.opacity = (1 - fase) * 0.7 * e;
        }
    }

    function cuadro(ahora) {
        enCola = false;
        const dt = Math.min(0.05, (ahora - reloj) / 1000);
        reloj = ahora;

        if (!pausado && !reducido && !activos.size && ahora - ultimaInteraccion > REPOSO_MS) meta.yaw += GIRO * dt;
        const suave = reducido ? 1 : 1 - Math.pow(0.0015, dt);
        vista.yaw += (meta.yaw - vista.yaw) * suave;
        vista.pitch += (meta.pitch - vista.pitch) * suave;
        vista.z += (meta.z - vista.z) * suave;
        globo.rotation.y = vista.yaw;
        eje.rotation.x = vista.pitch;
        cam.position.z = vista.z * zPorAncho();
        scene.updateMatrixWorld();

        const w = escena.clientWidth, h = escena.clientHeight, alturas = [];
        let conEtiqueta = 0;

        if (reducido) {
            // Fijo: los paises con mas CO2 encendidos, sin pulso
            zonas.slice(0, 12).forEach(z => pintarZona(z, 0.8, false));
        } else {
            if (ahora - ultimaZona > CADA_ZONA) { ultimaZona = ahora; encender(); }
            for (const z of zonas) if (z.t >= 0) for (const [i] of z.indices) colores.setXYZ(i, base.r, base.g, base.b);
        }

        const ordenadas = reducido ? zonas.slice(0, 12) : zonas;
        for (const z of ordenadas) {
            let e = 0.8;
            if (!reducido) {
                if (z.t < 0) continue;
                z.t += dt;
                if (z.t > VIDA_ZONA) {
                    z.t = -1;
                    z.brillo.material.opacity = 0;
                    z.anillo.material.opacity = 0;
                    z.etq.style.opacity = 0;
                    continue;
                }
                e = Math.pow(Math.sin(Math.PI * z.t / VIDA_ZONA), 0.7);
                pintarZona(z, e, true);
            }
            const delante = frente(z);
            v3.copy(z.centro).applyMatrix4(globo.matrixWorld).project(cam);
            const px = (v3.x * 0.5 + 0.5) * w, py = (-v3.y * 0.5 + 0.5) * h - 14, izq = px > w * 0.55;
            // Una etiqueta por franja de altura para que no se pisen
            if (delante > 0.2 && conEtiqueta < MAX_ETIQUETAS && px > 0 && px < w && py > 0 && py < h && !alturas.some(a => Math.abs(a - py) < 46)) {
                conEtiqueta++;
                alturas.push(py);
                z.etq.classList.toggle('izq', izq);
                z.etq.style.opacity = Math.min(1, e * 1.4);
                z.etq.style.transform = `translate(${px}px,${py}px)` + (izq ? ' translateX(-100%)' : '');
            } else {
                z.etq.style.opacity = 0;
            }
        }
        colores.needsUpdate = true;
        renderer.render(scene, cam);
        pedir();
    }
    pedir();
}

arrancar();
