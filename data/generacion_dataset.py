"""Genera el dataset SmartGlass CO2 (1000 registros) - Minería de Datos I.
Dataset derivado de factores de emisión documentados:
- Branca et al. (2020); MITECO (2013); Guardian Glass; Glass for Europe.
- Densidad del vidrio: 2500 kg/m3 -> masa_kg = area_m2 * espesor_mm * 2.5
- Factor red eléctrica Colombia: ~0.25 kg CO2/kWh
- El calcín reduce ~2.5% de energía por cada 10% y sustituye CO2 de materia prima.
Los 20 tipos de vidrio y sus multiplicadores son fieles al catálogo de SmartGlass.
Reproducible: semilla fija 42.
"""
import os
import numpy as np
import pandas as pd

rng = np.random.default_rng(42)
n = 1000

# ===========================================================================
# 20 TIPOS DE VIDRIO del catálogo SmartGlass con multiplicadores de CO2
# ===========================================================================
# Factor base: 1.00 = float crudo estándar (referencia)
# Multiplicador = cuánto contamina más/menos ese tipo vs float crudo
TIPOS_VIDRIO = {
    # Básicos / estándar
    "float":           1.00,   # vidrio crudo base, referencia
    "crudo":           1.00,   # sin tratamiento posterior
    "incoloro":        1.02,   # transparente estándar, mínimo refinado
    # Seguridad
    "templado":        1.15,   # recalentado a 620°C + enfriamiento rápido
    "laminado":        1.25,   # PVB entre capas + segundo horno
    "blindado":        1.80,   # multicapa policarbonato/vidrio
    "antivandálico":   1.35,   # laminado reforzado
    # Térmicos / energéticos
    "bajo_emisivo":    1.35,   # Low-E, recubrimiento metálico al vacío
    "insulado":        1.45,   # doble acristalamiento DVH + sellado
    "termoreflector":  1.30,   # capa metálica reflectiva
    # Decorativos / arquitectónicos
    "esmerilado":      1.10,   # tratamiento ácido o chorro arena
    "tintado":         1.08,   # óxidos metálicos en masa (Fe, Co, Se)
    "reflectivo":      1.25,   # capa espejo exterior
    "texturizado":     1.10,   # molde texturizado en línea
    # Especiales
    "antirreflejo":    1.40,   # multicapa antirreflejo (museos)
    "acústico":        1.55,   # laminado con PVB acústico especial
    "cortafuego":      1.70,   # gel intumescente + múltiples capas
    "fotovoltaico":    2.10,   # celdas de silicio integradas
    "inteligente":     2.20,   # electrocrómico (cambia opacidad)
    "reciclado":       0.85,   # alto contenido calcín, reduce materia prima
}

# Probabilidades realistas de producción en planta colombiana
# (los especiales son minoría; float/templado/laminado dominan)
PROBS_TIPO = {
    "float": 0.20, "crudo": 0.08, "incoloro": 0.05,
    "templado": 0.15, "laminado": 0.12, "blindado": 0.01, "antivandálico": 0.02,
    "bajo_emisivo": 0.05, "insulado": 0.08, "termoreflector": 0.03,
    "esmerilado": 0.04, "tintado": 0.05, "reflectivo": 0.03, "texturizado": 0.03,
    "antirreflejo": 0.01, "acústico": 0.02, "cortafuego": 0.01,
    "fotovoltaico": 0.005, "inteligente": 0.005, "reciclado": 0.01,
}

tipos_nombres = list(TIPOS_VIDRIO.keys())
tipos_probs = [PROBS_TIPO[t] for t in tipos_nombres]

# ===========================================================================
# Variables del dataset
# ===========================================================================
# Procesos productivos
proceso = rng.choice(
    ["recocido", "templado", "laminado", "especial"],
    n,
    p=[0.40, 0.30, 0.20, 0.10]
)

# Dimensiones realistas de piezas
area = np.round(rng.uniform(0.5, 12.0, n), 2)                   # m2
espesor = rng.choice(
    [4, 6, 8, 10, 12],
    n,
    p=[0.25, 0.30, 0.25, 0.12, 0.08]
)

# % de calcín en la mezcla
# - tipo "reciclado" tiene alto calcín (50-80%)
# - los demás tienen calcín bajo/medio (0-30%) como porcentaje de mezcla estándar
tipo_arr = rng.choice(tipos_nombres, n, p=tipos_probs)
calcin = np.array([
    int(rng.choice([50, 60, 70, 80])) if t == "reciclado"
    else int(rng.choice([0, 0, 10, 10, 20, 30]))
    for t in tipo_arr
])

# ===========================================================================
# Cálculos técnicos
# ===========================================================================
base_energia = {
    "recocido": 4.0,     # kWh/kg horno estándar
    "templado": 5.5,     # horno de templado
    "laminado": 6.5,     # horno + autoclave PVB
    "especial": 7.5,     # procesos especiales (Low-E, cortafuego, etc.)
}

masa = area * espesor * 2.5                                                  # kg
energia = np.array([base_energia[p] for p in proceso]) * (1 - 0.025 * calcin / 10)

# Emisiones por kg de vidrio
emision_materia = 0.42 * (1 - calcin / 100)        # kg CO2/kg materia prima
emision_energia = energia * 0.25                   # kg CO2/kg electricidad

# Aplicar multiplicador por tipo + ruido de proceso 3%
mult_tipo = np.array([TIPOS_VIDRIO[t] for t in tipo_arr])
co2 = masa * (emision_materia + emision_energia) * mult_tipo
co2 = np.round(co2 * rng.normal(1, 0.03, n), 2)

# ===========================================================================
# DataFrame final
# ===========================================================================
df = pd.DataFrame({
    "tipo_vidrio": tipo_arr,
    "proceso": proceso,
    "area_m2": area,
    "espesor_mm": espesor,
    "calcin_pct": calcin,
    "masa_kg": np.round(masa, 2),
    "energia_kwh_kg": np.round(energia, 3),
    "co2_kg": co2,
})

# ===========================================================================
# Guardado (CREA LA CARPETA AUTOMÁTICAMENTE)
# ===========================================================================
os.makedirs("data/raw", exist_ok=True)  # <-- Esta línea crea data/raw si no existe
df.to_csv("data/raw/smartglass_co2.csv", index=False)

# ===========================================================================
# Verificación en consola
# ===========================================================================
print("✅ OK:", df.shape[0], "registros x", df.shape[1], "variables")
print("💾 Guardado en: data/raw/smartglass_co2.csv\n")
print("📊 Tipos de vidrio generados:")
print(df["tipo_vidrio"].value_counts().to_string())
print("\n📈 Estadísticas descriptivas:")
print(df.describe().round(2).to_string())
print(f"\n🌱 CO2 promedio: {df['co2_kg'].mean():.2f} kg/pieza")
print(f"🏭 Tipo más contaminante (media): {df.groupby('tipo_vidrio')['co2_kg'].mean().idxmax()}")
print(f"♻️  Tipo menos contaminante (media): {df.groupby('tipo_vidrio')['co2_kg'].mean().idxmin()}")