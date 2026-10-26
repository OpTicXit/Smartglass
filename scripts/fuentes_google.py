"""U2 · Fuentes alojadas en el proyecto (licencia OFL). Manual de marca: Poppins (titulares y logotipo,
pesos 500 a 800) e Inter (texto e interfaz, variable 400 a 700).

Antes la hoja de Google Fonts bloqueaba el pintado y abria dos conexiones a terceros en cada
visita. Este script baja los subconjuntos latin y latin-ext (espanol e ingles) y escribe:

    src/main/resources/static/vendor/fuentes/fuentes.css
    src/main/resources/static/vendor/fuentes/*.woff2

Uso: python scripts/fuentes_google.py   (solo biblioteca estandar)
"""
import pathlib
import re
import urllib.request

URL = ('https://fonts.googleapis.com/css2?family=Poppins:wght@500;600;700;800'
       '&family=Inter:wght@400..700&display=swap')
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36'
SUBCONJUNTOS = ('latin', 'latin-ext')
DESTINO = pathlib.Path(__file__).resolve().parent.parent / 'src' / 'main' / 'resources' / 'static' / 'vendor' / 'fuentes'
BLOQUE = re.compile(r'/\* ([a-z-]+) \*/\s*(@font-face \{.*?\})', re.S)


def bajar(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': UA})) as r:
        return r.read()


def main():
    DESTINO.mkdir(parents=True, exist_ok=True)
    for viejo in DESTINO.glob('*.woff2'):
        viejo.unlink()   # quita las fuentes que ya no se usan
    salida = ['/* Poppins e Inter (SIL OFL 1.1) · generado por scripts/fuentes_google.py: no editar a mano */']
    for sub, cara in BLOQUE.findall(bajar(URL).decode('utf-8')):
        if sub not in SUBCONJUNTOS:
            continue
        familia = re.search(r"font-family: '([^']+)'", cara).group(1)
        peso = re.search(r'font-weight: ([0-9 ]+);', cara).group(1)
        # Poppins llega en un archivo por peso; una fuente variable ("400 700") va en uno solo
        archivo = familia.lower().replace(' ', '-') + ('' if ' ' in peso else '-' + peso) + '-' + sub + '.woff2'
        url = re.search(r'url\((https://[^)]+)\)', cara).group(1)
        (DESTINO / archivo).write_bytes(bajar(url))
        salida.append('/* ' + sub + ' */\n' + cara.replace(url, './' + archivo))
        print(archivo, (DESTINO / archivo).stat().st_size, 'bytes')
    (DESTINO / 'fuentes.css').write_text('\n'.join(salida) + '\n', encoding='utf-8', newline='\n')


if __name__ == '__main__':
    main()
