/* SmartGlass - vista 3D en vivo del editor de corte (corte-editor.js). Three.js se carga solo al abrir el editor.
   Muestra el contorno con su espesor y el diseno pintado encima. Es una referencia visual: precio y CO2 no salen de aqui.
   Arrastrar gira, la rueda acerca. Con movimiento reducido o fotosensible no gira sola. */
(function () {
    'use strict';

    var URL_THREE = document.currentScript.dataset.three;
    var LIENZO = 420;                    // cm del lienzo: el diseno lo cubre entero
    var ESPESOR_MINIMO = 0.035;          // fraccion del tamano: 6 mm en 3 m no se verian de canto
    var ZOOM_MIN = 2.2, ZOOM_MAX = 7;

    var THREE = null, cargando = null, sinWebgl = false;
    var renderer, escena, camara, grupo, cuerpo, canto, capa, textura;
    var caja = null, activo = false, ultimo = null, clave = '', reloj = 0, cola = false;
    var vista = { yaw: 0.55, pitch: 0.28, zoom: 3.6 }, girando = true, arrastre = null;

    function reducido() { return window.matchMedia('(prefers-reduced-motion: reduce)').matches; }

    function cargar() {
        if (!cargando) {
            cargando = import(URL_THREE).then(function (m) {
                THREE = m;
                crearEscena();
            }).catch(function () { avisarSinWebgl(); });
        }
        return cargando;
    }

    function avisarSinWebgl() {
        sinWebgl = true;
        var aviso = caja && caja.querySelector('.sgc-3d-aviso');
        if (aviso) aviso.hidden = false;
    }

    function crearEscena() {
        var lienzo = caja.querySelector('canvas');
        renderer = new THREE.WebGLRenderer({ canvas: lienzo, antialias: true, alpha: true });
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
        escena = new THREE.Scene();
        camara = new THREE.PerspectiveCamera(34, 1, 0.1, 50);
        escena.add(new THREE.AmbientLight(0xffffff, 1.1));
        var sol = new THREE.DirectionalLight(0xffffff, 2.4);
        sol.position.set(2.5, 3, 4);
        escena.add(sol);
        var contraluz = new THREE.DirectionalLight(0x7fd8ff, 1.5);
        contraluz.position.set(-3, -1.5, -3);
        escena.add(contraluz);
        grupo = new THREE.Group();
        escena.add(grupo);
        textura = new THREE.CanvasTexture(document.createElement('canvas'));
        textura.colorSpace = THREE.SRGBColorSpace;
        conectarPuntero(lienzo);
        new ResizeObserver(medir).observe(caja);
        medir();
    }

    function medir() {
        if (!renderer) return;
        var w = caja.clientWidth, h = caja.clientHeight;
        if (!w || !h) return;
        renderer.setSize(w, h, false);
        camara.aspect = w / h;
        camara.updateProjectionMatrix();
    }

    function conectarPuntero(lienzo) {
        lienzo.addEventListener('pointerdown', function (e) {
            lienzo.setPointerCapture(e.pointerId);
            arrastre = { x: e.clientX, y: e.clientY };
        });
        lienzo.addEventListener('pointermove', function (e) {
            if (!arrastre) return;
            vista.yaw += (e.clientX - arrastre.x) * 0.012;
            vista.pitch = Math.max(-1.3, Math.min(1.3, vista.pitch + (e.clientY - arrastre.y) * 0.012));
            arrastre = { x: e.clientX, y: e.clientY };
        });
        var soltar = function () { arrastre = null; };
        lienzo.addEventListener('pointerup', soltar);
        lienzo.addEventListener('pointercancel', soltar);
        lienzo.addEventListener('wheel', function (e) {
            e.preventDefault();
            vista.zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, vista.zoom * Math.exp(e.deltaY * 0.001)));
        }, { passive: false });
        lienzo.addEventListener('keydown', function (e) {
            var paso = { ArrowLeft: [-0.2, 0], ArrowRight: [0.2, 0], ArrowUp: [0, -0.2], ArrowDown: [0, 0.2] }[e.key];
            if (!paso) return;
            e.preventDefault();
            e.stopPropagation();
            vista.yaw += paso[0];
            vista.pitch = Math.max(-1.3, Math.min(1.3, vista.pitch + paso[1]));
        });
    }

    function liberar(objeto) {
        if (!objeto) return;
        grupo.remove(objeto);
        objeto.geometry.dispose();
        objeto.material.dispose();
    }

    function reconstruir(d) {
        liberar(cuerpo); liberar(canto); liberar(capa);
        cuerpo = canto = capa = null;
        var pts = d.puntos;
        if (!pts || pts.length < 3) return;

        var minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
        pts.forEach(function (p) {
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
        });
        var cx = (minX + maxX) / 2, cy = (minY + maxY) / 2, lado = Math.max(maxX - minX, maxY - minY, 1), s = 2 / lado;
        var forma = new THREE.Shape(pts.map(function (p) { return new THREE.Vector2((p.x - cx) * s, -(p.y - cy) * s); }));
        var hondo = Math.max((d.espesorMm || 6) / 10 * s, ESPESOR_MINIMO);

        var volumen = new THREE.ExtrudeGeometry(forma, { depth: hondo, bevelEnabled: false });
        volumen.translate(0, 0, -hondo / 2);
        cuerpo = new THREE.Mesh(volumen, new THREE.MeshStandardMaterial({
            color: 0xbfeaff, transparent: true, opacity: 0.34, roughness: 0.08, metalness: 0.15, side: THREE.DoubleSide, depthWrite: false
        }));
        grupo.add(cuerpo);
        canto = new THREE.LineSegments(new THREE.EdgesGeometry(volumen, 20), new THREE.LineBasicMaterial({ color: 0x8fe3ff, transparent: true, opacity: 0.95 }));
        grupo.add(canto);

        // El diseno cubre el lienzo entero: cada punto de la pieza toma su pixel del mismo sitio donde se pinto
        var cara = new THREE.ShapeGeometry(forma);
        var pos = cara.attributes.position, uv = cara.attributes.uv;
        for (var i = 0; i < pos.count; i++) {
            uv.setXY(i, (pos.getX(i) / s + cx) / LIENZO, 1 - (-pos.getY(i) / s + cy) / LIENZO);
        }
        capa = new THREE.Mesh(cara, new THREE.MeshBasicMaterial({ map: textura, transparent: true, opacity: 0.9, side: THREE.DoubleSide, depthWrite: false }));
        capa.position.z = hondo / 2 + 0.002;
        capa.visible = !!d.diseno;
        grupo.add(capa);
    }

    function aplicar(d) {
        if (!THREE || sinWebgl || !renderer) return;
        var nueva = JSON.stringify(d.puntos) + '|' + d.espesorMm;
        if (nueva !== clave) { clave = nueva; reconstruir(d); }
        if (capa) capa.visible = !!d.diseno;
        if (d.diseno && textura.image !== d.diseno) textura.image = d.diseno;
        if (d.diseno) textura.needsUpdate = true;
    }

    function cuadro(ahora) {
        cola = false;
        if (!activo) return;
        if (renderer && !document.hidden) {
            var dt = Math.min(0.05, (ahora - reloj) / 1000);
            if (girando && !arrastre && !reducido()) vista.yaw += 0.35 * dt;
            grupo.rotation.set(vista.pitch, vista.yaw, 0);
            camara.position.z = vista.zoom;
            renderer.render(escena, camara);
        }
        reloj = ahora;
        pedir();
    }
    function pedir() { if (activo && !cola) { cola = true; requestAnimationFrame(cuadro); } }

    window.SGCorte3D = {
        /* contenedor con un <canvas> y un .sgc-3d-aviso oculto */
        abrir: function (contenedor) {
            caja = contenedor;
            activo = true;
            reloj = performance.now();
            cargar().then(function () {
                medir();
                if (ultimo) aplicar(ultimo);
                pedir();
            });
        },
        cerrar: function () { activo = false; },
        actualizar: function (datos) { ultimo = datos; aplicar(datos); },
        alternarGiro: function () { girando = !girando; return girando; },
        girando: function () { return girando && !reducido(); }
    };
})();
