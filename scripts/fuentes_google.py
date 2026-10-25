"""U2 · Fuentes (Bricolage Grotesque y Geist, licencia OFL) alojadas en el proyecto.

Antes la hoja de Google Fonts bloqueaba el pintado y abria dos conexiones a terceros en cada
visita. Este script baja los subconjuntos latin y latin-ext (espanol e ingles) y escribe:

    src/main/resources/static/vendor/fuentes/fuentes.css
    src/main/resources/static/vendor/fuentes/*.woff2

Uso: python scripts/fuentes_google.py   (solo biblioteca estandar)
"""
import pathlib
import re
import urllib.request

URL = ('https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,500..800'
       '&family=Geist:wght@400..700&display=swap')
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36'
SUBCONJUNTOS = ('latin', 'latin-ext')
DESTINO = pathlib.Path(__file__).resolve().parent.parent / 'src' / 'main' / 'resources' / 'static' / 'vendor' / 'fuentes'
BLOQUE = re.compile(r'/\* ([a-z-]+) \*/\s*(@font-face \{.*?\})', re.S)


def bajar(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': UA})) as r:
        return r.read()


def main():
    DESTINO.mkdir(parents=True, exist_ok=True)
    salida = ['/* Bricolage Grotesque y Geist (SIL OFL 1.1) · generado por scripts/fuentes_google.py: no editar a mano */']
    for sub, cara in BLOQUE.findall(bajar(URL).decode('utf-8')):
        if sub not in SUBCONJUNTOS:
            continue
        familia = re.search(r"font-family: '([^']+)'", cara).group(1)
        archivo = familia.lower().replace(' ', '-') + '-' + sub + '.woff2'
        url = re.search(r'url\((https://[^)]+)\)', cara).group(1)
        (DESTINO / archivo).write_bytes(bajar(url))
        salida.append('/* ' + sub + ' */\n' + cara.replace(url, './' + archivo))
        print(archivo, (DESTINO / archivo).stat().st_size, 'bytes')
    (DESTINO / 'fuentes.css').write_text('\n'.join(salida) + '\n', encoding='utf-8', newline='\n')


if __name__ == '__main__':
    main()
