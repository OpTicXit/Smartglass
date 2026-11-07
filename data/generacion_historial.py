"""Genera la historia diaria sintética de la Simulación (5 años) - Minería de Datos I.

Cada día tiene un número de piezas fabricadas y su CO2 estimado, tomado del dataset de 10.000 piezas
(data/raw/smartglass_co2.csv, sin modificarlo): la historia hereda su composición (tipos, espesores, calcín).

- Piezas por día ~ Poisson(lambda), lambda = 12 · crecimiento (0,6 -> 1,0) · semana · ola anual.
- Semana: 1,1 de lunes a viernes, 0,75 el sábado y 0,25 el domingo.
- Ola anual suave (+-15 %): más obra en temporada seca.
Es una historia ilustrativa para mostrar el método de proyección, no actividad medida.
Reproducible (semilla 42). Salida: data/raw/smartglass_historial.csv (fecha, piezas, co2_kg).
"""
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import pandas as pd

RAIZ = Path(__file__).resolve().parent
DATASET = RAIZ / "raw" / "smartglass_co2.csv"
SALIDA = RAIZ / "raw" / "smartglass_historial.csv"
DIAS = 1826
BASE = 12

rng = np.random.default_rng(42)
co2_piezas = pd.read_csv(DATASET)["co2_kg"].to_numpy()

fin = date.today()
filas = []
for i in range(DIAS):
    dia = fin - timedelta(days=DIAS - 1 - i)
    crecimiento = 0.6 + 0.4 * i / (DIAS - 1)
    semana = {5: 0.75, 6: 0.25}.get(dia.weekday(), 1.1)
    anual = 1 + 0.15 * np.sin(2 * np.pi * dia.timetuple().tm_yday / 365.25)
    piezas = int(rng.poisson(BASE * crecimiento * semana * anual))
    co2 = float(rng.choice(co2_piezas, size=piezas, replace=True).sum()) if piezas else 0.0
    filas.append({"fecha": dia.isoformat(), "piezas": piezas, "co2_kg": round(co2, 2)})

pd.DataFrame(filas).to_csv(SALIDA, index=False)
print(f"{SALIDA.name}: {len(filas)} días, {sum(f['piezas'] for f in filas)} piezas, "
      f"{sum(f['co2_kg'] for f in filas) / 1000:.1f} t de CO2 estimado")
