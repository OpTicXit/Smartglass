"""
Exporta los datos del informe técnico del panel de administración (/admin/informe-tecnico).

Producción no tiene Python: la página lee este JSON, que se versiona en el repositorio.
Volver a ejecutarlo después de reentrenar el modelo en analisis.ipynb:

    python "mineria de datos/exportar_informe.py"

Calcula en una sola salida las mismas gráficas del notebook, la comparación con modelos de
referencia y las emisiones por país del globo (Our World in Data / Global Carbon Budget, CC BY 4.0).
"""
import json
import sys
from datetime import date
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.dummy import DummyRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder

RAIZ = Path(__file__).resolve().parent.parent
CSV = RAIZ / "data" / "raw" / "smartglass_co2.csv"
MODELO = RAIZ / "mineria de datos" / "modelo_smartglass_co2.pkl"
METRICAS = RAIZ / "mineria de datos" / "metricas_modelo.csv"
SALIDA = RAIZ / "backend" / "src" / "main" / "resources" / "informe" / "informe-tecnico.json"
OWID = "https://raw.githubusercontent.com/owid/co2-data/master/owid-co2-data.csv"

CAT = ["tipo_vidrio", "proceso"]
NUM = ["area_m2", "espesor_mm", "calcin_pct", "energia_kwh_kg"]
TOP_PAISES = 30

# ISO alfa-3 de OWID -> ISO numérico (el id de world-atlas) y nombre en español.
# Los que no están en world-atlas 110m (islas pequeñas) no se pueden pintar en el globo.
PAISES = {
    "CHN": (156, "China"), "USA": (840, "Estados Unidos"), "IND": (356, "India"), "RUS": (643, "Rusia"),
    "JPN": (392, "Japón"), "IDN": (360, "Indonesia"), "IRN": (364, "Irán"), "SAU": (682, "Arabia Saudita"),
    "KOR": (410, "Corea del Sur"), "DEU": (276, "Alemania"), "CAN": (124, "Canadá"), "TUR": (792, "Turquía"),
    "BRA": (76, "Brasil"), "MEX": (484, "México"), "ZAF": (710, "Sudáfrica"), "AUS": (36, "Australia"),
    "VNM": (704, "Vietnam"), "GBR": (826, "Reino Unido"), "ITA": (380, "Italia"), "MYS": (458, "Malasia"),
    "KAZ": (398, "Kazajistán"), "POL": (616, "Polonia"), "THA": (764, "Tailandia"), "FRA": (250, "Francia"),
    "TWN": (158, "Taiwán"), "EGY": (818, "Egipto"), "IRQ": (368, "Irak"), "ARE": (784, "Emiratos Árabes Unidos"),
    "ESP": (724, "España"), "DZA": (12, "Argelia"), "COL": (170, "Colombia"),
}


def redondear(valores, decimales=2):
    return [round(float(v), decimales) for v in valores]


def metricas(y_real, y_pred):
    return {
        "mae": round(float(mean_absolute_error(y_real, y_pred)), 2),
        "rmse": round(float(np.sqrt(mean_squared_error(y_real, y_pred))), 2),
        "r2": round(float(r2_score(y_real, y_pred)), 4),
    }


def seccion_modelo():
    df = pd.read_csv(CSV)
    # Misma división y variables que analisis.ipynb y preparacion.py
    X = df[CAT + NUM]
    y = df["co2_kg"]
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    pipeline = joblib.load(MODELO)
    y_pred = pipeline.predict(X_test)

    lineal = Pipeline([
        ("pre", ColumnTransformer([("cat", OneHotEncoder(handle_unknown="ignore"), CAT)], remainder="passthrough")),
        ("reg", LinearRegression()),
    ]).fit(X_train, y_train)
    media = DummyRegressor(strategy="mean").fit(X_train, y_train)

    guardadas = pd.read_csv(METRICAS).iloc[0]
    propias = metricas(y_test, y_pred)
    # El notebook y este script deben describir el mismo modelo
    assert abs(propias["mae"] - guardadas["mae_test"]) < 0.01, "metricas_modelo.csv no corresponde al modelo"

    errores = y_test.to_numpy() - y_pred
    conteo, bordes = np.histogram(errores, bins=30)

    nombres = pipeline.named_steps["preprocessor"].get_feature_names_out()
    importancias = pipeline.named_steps["regressor"].feature_importances_
    top = sorted(zip(nombres, importancias), key=lambda p: -p[1])[:15]

    numericas = df.select_dtypes(include=[np.number])
    corr = numericas.corr()
    conteo_tipos = df["tipo_vidrio"].value_counts()
    co2_tipo = df.groupby("tipo_vidrio")["co2_kg"].mean().sort_values(ascending=False)

    prueba = X_test.assign(real=y_test.to_numpy(), pred=y_pred)
    por_tipo = prueba.groupby("tipo_vidrio")[["real", "pred"]].mean().sort_values("real", ascending=False)

    return {
        "registros": int(len(df)),
        "prueba": int(len(X_test)),
        "arboles": int(pipeline.named_steps["regressor"].n_estimators),
        "metricas": {**propias, "r2_cv": round(float(guardadas["cv_mean"]), 4), "r2_cv_std": round(float(guardadas["cv_std"]), 4)},
        "referencia": {"media": metricas(y_test, media.predict(X_test)), "lineal": metricas(y_test, lineal.predict(X_test))},
        "tipos": {"claves": conteo_tipos.index.tolist(), "valores": [int(v) for v in conteo_tipos]},
        "co2_tipo": {"claves": co2_tipo.index.tolist(), "valores": redondear(co2_tipo)},
        "correlacion": {"variables": corr.columns.tolist(), "valores": [redondear(f, 3) for f in corr.to_numpy()]},
        "real_pred": {"real": redondear(y_test), "pred": redondear(y_pred)},
        "errores": {"conteo": [int(v) for v in conteo], "bordes": redondear(bordes)},
        "importancia": [{"variable": n.split("__", 1)[1], "valor": round(float(v), 4)} for n, v in top],
        "real_pred_tipo": {"claves": por_tipo.index.tolist(), "real": redondear(por_tipo["real"]), "pred": redondear(por_tipo["pred"])},
    }


def seccion_paises():
    owid = pd.read_csv(OWID, usecols=["country", "year", "iso_code", "co2", "share_global_co2"])
    owid = owid.dropna(subset=["iso_code", "co2"])
    anio = int(owid["year"].max())
    ultimo = owid[owid["year"] == anio].sort_values("co2", ascending=False)
    elegidos = pd.concat([ultimo.head(TOP_PAISES), ultimo[ultimo["iso_code"] == "COL"]]).drop_duplicates("iso_code")

    faltan = set(elegidos["iso_code"]) - set(PAISES)
    assert not faltan, f"añadir a PAISES: {faltan}"

    filas = [{
        "iso": fila.iso_code,
        "id": PAISES[fila.iso_code][0],
        "es": PAISES[fila.iso_code][1],
        "en": fila.country,
        "co2_mt": round(float(fila.co2), 1),
        "pct": round(float(fila.share_global_co2), 2),
    } for fila in elegidos.itertuples()]
    return {"anio": anio, "filas": filas}


def main():
    datos = {"generado": date.today().isoformat(), "modelo": seccion_modelo(), "paises": seccion_paises()}
    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    SALIDA.write_text(json.dumps(datos, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    m = datos["modelo"]
    print(f"{SALIDA.name}: {SALIDA.stat().st_size / 1024:.0f} KB")
    print("Random Forest", m["metricas"])
    print("Referencia", m["referencia"])
    print("Paises", datos["paises"]["anio"], len(datos["paises"]["filas"]))


if __name__ == "__main__":
    sys.exit(main())
