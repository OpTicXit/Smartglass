"""
Exporta las primeras preguntas de un árbol del Random Forest para el informe técnico
(apartado «Decisiones al construir el modelo»). No reentrena ni modifica el modelo: solo lo lee.

    python "mineria de datos/exportar_arbol.py"

Volver a ejecutarlo si se reentrena el modelo en analisis.ipynb (igual que exportar_informe.py).
"""
import json
import sys
from pathlib import Path

import joblib
import pandas as pd
from sklearn.model_selection import train_test_split

RAIZ = Path(__file__).resolve().parent.parent
CSV = RAIZ / "data" / "raw" / "smartglass_co2.csv"
MODELO = RAIZ / "mineria de datos" / "modelo_smartglass_co2.pkl"
SALIDA = RAIZ / "backend" / "src" / "main" / "resources" / "informe" / "arbol-modelo.json"

CAT = ["tipo_vidrio", "proceso"]
NUM = ["area_m2", "espesor_mm", "calcin_pct", "energia_kwh_kg"]
ARBOL = 0          # el primero de los 200: todos son igual de validos, ninguno se elige "por bonito"
PREGUNTAS = 3      # niveles que se muestran; el arbol real sigue hasta su profundidad completa


def nodo(t, nombres, n, nivel):
    """Pregunta (variable <= umbral: 'no' a la izquierda, 'si' a la derecha) o resumen de lo que queda debajo."""
    base = {"n": int(round(t.weighted_n_node_samples[n])), "co2": round(float(t.value[n][0][0]), 1)}
    if nivel >= PREGUNTAS or t.children_left[n] == -1:
        return base
    var = nombres[t.feature[n]].split("__", 1)[1]
    cat = next((c for c in CAT if var.startswith(c + "_")), None)
    pregunta = {"tipo": cat, "valor": var[len(cat) + 1:]} if cat else {"tipo": var, "umbral": round(float(t.threshold[n]), 2)}
    return {**base, "pregunta": pregunta,
            "no": nodo(t, nombres, t.children_left[n], nivel + 1),
            "si": nodo(t, nombres, t.children_right[n], nivel + 1)}


def camino(t, fila, nivel=0, n=0):
    """Respuestas (True = si) que da una pieza en las preguntas mostradas."""
    if nivel >= PREGUNTAS or t.children_left[n] == -1:
        return []
    si = fila[t.feature[n]] > t.threshold[n]
    return [bool(si)] + camino(t, fila, nivel + 1, t.children_right[n] if si else t.children_left[n])


def main():
    df = pd.read_csv(CSV)
    X, y = df[CAT + NUM], df["co2_kg"]
    _, X_test, _, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    pipeline = joblib.load(MODELO)
    pre, rf = pipeline.named_steps["preprocessor"], pipeline.named_steps["regressor"]
    estimador = rf.estimators_[ARBOL]
    t, nombres = estimador.tree_, pre.get_feature_names_out()

    # Pieza de ejemplo: la primera de prueba que es templada de 6 mm (el caso mas comun del cotizador)
    i = X_test.index[(X_test["tipo_vidrio"] == "templado") & (X_test["espesor_mm"] == 6)][0]
    pieza = X_test.loc[[i]]
    fila = pre.transform(pieza)
    fila = (fila.toarray() if hasattr(fila, "toarray") else fila)[0]

    datos = {
        "arbol": ARBOL + 1, "arboles": int(rf.n_estimators), "profundidad": int(estimador.get_depth()),
        "hojas": int(estimador.get_n_leaves()), "preguntas": PREGUNTAS,
        "raiz": nodo(t, nombres, 0, 0),
        "ejemplo": {
            **{c: (pieza.iloc[0][c] if c in CAT else round(float(pieza.iloc[0][c]), 2)) for c in CAT + NUM},
            "camino": camino(t, fila),
            "co2Arbol": round(float(estimador.predict(fila.reshape(1, -1))[0]), 1),
            "co2Bosque": round(float(pipeline.predict(pieza)[0]), 1),
            "co2Calculado": round(float(y_test.loc[i]), 1),
        },
    }
    SALIDA.write_text(json.dumps(datos, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"{SALIDA.name}: arbol {datos['arbol']} de {datos['arboles']}, profundidad {datos['profundidad']}, ejemplo {datos['ejemplo']}")


if __name__ == "__main__":
    sys.exit(main())
