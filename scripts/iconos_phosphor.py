"""U2 · Iconos Phosphor alojados en el proyecto, solo con los que se usan.

Antes se cargaban dos hojas de unpkg (164 KB de CSS que bloquean el pintado + 280 KB de fuentes)
para unos 100 iconos. Este script busca las clases ph-* en src/main, recorta las dos fuentes
(regular y fill) a esos glifos y escribe:

    src/main/resources/static/vendor/phosphor/phosphor.css
    src/main/resources/static/vendor/phosphor/Phosphor.woff2
    src/main/resources/static/vendor/phosphor/Phosphor-Fill.woff2

Uso (requiere: pip install fonttools brotli):
    python scripts/iconos_phosphor.py

Volver a ejecutarlo al usar un icono nuevo; IconosPhosphorTest falla si falta alguno.
"""
import io
import pathlib
import re
import urllib.request

from fontTools import subset
from fontTools.ttLib import TTFont

VERSION = '2.1.1'
BASE = 'https://unpkg.com/@phosphor-icons/web@' + VERSION + '/src/'
RAIZ = pathlib.Path(__file__).resolve().parent.parent
FUENTES = RAIZ / 'src' / 'main'
DESTINO = FUENTES / 'resources' / 'static' / 'vendor' / 'phosphor'
TOKEN = re.compile(r'ph-[a-z0-9]+(?:-[a-z0-9]+)*')
REGLA = re.compile(r'\.(ph|ph-fill)\.(ph-[a-z0-9-]+):before\s*\{\s*content:\s*"\\([0-9a-f]+)";', re.I)
ESTILOS = [('ph', 'Phosphor', 'regular/style.css', 'regular/Phosphor.woff2', 'Phosphor.woff2'),
           ('ph-fill', 'Phosphor-Fill', 'fill/style.css', 'fill/Phosphor-Fill.woff2', 'Phosphor-Fill.woff2')]


def bajar(ruta):
    with urllib.request.urlopen(BASE + ruta) as r:
        return r.read()


def usados():
    nombres = set()
    for p in FUENTES.rglob('*'):
        if p.suffix in ('.html', '.js', '.java', '.properties') and 'vendor' not in p.parts:
            nombres.update(TOKEN.findall(p.read_text(encoding='utf-8', errors='ignore')))
    nombres.discard('ph-fill')
    return nombres


def main():
    nombres = usados()
    DESTINO.mkdir(parents=True, exist_ok=True)
    css = ['/* Phosphor Icons ' + VERSION + ' (MIT) · subconjunto generado por scripts/iconos_phosphor.py: no editar a mano */']
    faltan = set(nombres)
    for clase, familia, hoja, fuente, salida in ESTILOS:
        reglas = {m.group(2): m.group(3) for m in REGLA.finditer(bajar(hoja).decode('utf-8')) if m.group(1) == clase}
        presentes = sorted(n for n in nombres if n in reglas)
        faltan -= set(presentes)
        fnt = TTFont(io.BytesIO(bajar(fuente)))
        opciones = subset.Options()
        opciones.flavor = 'woff2'
        opciones.layout_features = []      # el CSS usa el punto de codigo, no las ligaduras
        opciones.name_IDs = ['*']
        sub = subset.Subsetter(opciones)
        sub.populate(unicodes=[int(reglas[n], 16) for n in presentes])
        sub.subset(fnt)
        fnt.flavor = 'woff2'
        fnt.save(DESTINO / salida)
        css.append('@font-face { font-family: "%s"; src: url("./%s") format("woff2"); font-weight: normal; font-style: normal; font-display: block; }'
                   % (familia, salida))
        css.append('.%s { font-family: "%s" !important; speak: never; font-style: normal; font-weight: normal; font-variant: normal; '
                   'text-transform: none; line-height: 1; letter-spacing: 0; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale; }'
                   % (clase, familia))
        css.extend('.%s.%s:before { content: "\\%s"; }' % (clase, n, reglas[n]) for n in presentes)
        print(clase, len(presentes), 'iconos ->', salida, (DESTINO / salida).stat().st_size, 'bytes')
    if faltan:
        print('Clases ph-* que no son iconos de Phosphor (se ignoran):', ', '.join(sorted(faltan)))
    (DESTINO / 'phosphor.css').write_text('\n'.join(css) + '\n', encoding='utf-8', newline='\n')


if __name__ == '__main__':
    main()
