"""
Exporta la proyección de la Simulación para el informe técnico del admin (/admin/informe-tecnico).

Producción no tiene Python: la página lee este JSON, que se versiona en el repositorio.
Volver a ejecutarlo si cambia la historia sintética (data/generacion_historial.py):

    python data/generacion_historial.py
    python "mineria de datos/exportar_proyeccion.py"

Valida el método guardando los últimos 6 meses (182 días): error medio diario, error porcentual de los
totales semanales y la referencia ingenua (repetir el mismo día de hace 364 días). Luego proyecta con
toda la historia. Las fechas son ilustrativas: "fin" va en null y la página ancla la historia a hoy.
"""
import json
import sys
from datetime import date
from pathlib import Path

import numpy as np
import pandas as pd

sys.path.insert(0, str(Path(__file__).resolve().parent))
from proyeccion_co2 import ajustar, proyectar, variables  # noqa: E402

RAIZ = Path(__file__).resolve().parent.parent
HISTORIA = RAIZ / "data" / "raw" / "smartglass_historial.csv"
SALIDA = RAIZ / "backend" / "src" / "main" / "resources" / "informe" / "proyeccion-simulacion.json"
VALIDACION = 182


def redondear(v):
    return round(float(v), 2)


def metricas(y):
    entreno, prueba = y[:-VALIDACION], y[-VALIDACION:]
    n = len(entreno)
    modelo, _ = ajustar(entreno)
    pred = np.maximum(0, modelo.predict(variables(np.arange(n, n + VALIDACION), n)))
    ingenuo = y[-VALIDACION - 364:-364]
    semanas = VALIDACION // 7 * 7
    real_sem = prueba[:semanas].reshape(-1, 7).sum(axis=1)
    pred_sem = pred[:semanas].reshape(-1, 7).sum(axis=1)
    return {
        "mae": redondear(np.mean(np.abs(prueba - pred))),
        "mape_semanal": redondear(np.mean(np.abs(real_sem - pred_sem) / real_sem) * 100),
        "mae_ingenuo": redondear(np.mean(np.abs(prueba - ingenuo))),
    }


def main():
    y = pd.read_csv(HISTORIA)["co2_kg"].to_numpy(dtype=float)
    m = metricas(y)
    assert m["mae"] < m["mae_ingenuo"], f"el modelo no supera a la referencia ingenua: {m}"
    p = proyectar(y)
    datos = {
        "generado": date.today().isoformat(),
        "fin": None,
        "historia": [redondear(v) for v in p["historia"]],
        "futuro": [{k: redondear(b[k]) for k in ("y", "lo", "hi")} for b in p["futuro"]],
        "horizontes": [{**h, "total": redondear(h["total"]), "lo": redondear(h["lo"]), "hi": redondear(h["hi"])}
                       for h in p["horizontes"]],
        "metricas": m,
    }
    SALIDA.write_text(json.dumps(datos, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"{SALIDA.name}: {SALIDA.stat().st_size / 1024:.0f} KB · {len(y)} días")
    print("Validación", m)
    print("Horizontes", [(h["clave"], round(h["total"])) for h in datos["horizontes"]])


if __name__ == "__main__":
    sys.exit(main())
