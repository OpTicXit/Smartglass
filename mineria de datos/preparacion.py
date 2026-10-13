"""
Preprocesamiento del dataset SmartGlass CO2 - Minería de Datos I.
Módulo reutilizable para cargar, dividir y preprocesar los datos.

Decisiones documentadas:
- Sin nulos ni duplicados => no se imputa.
- Atípicos IQR = piezas grandes válidas => no se eliminan.
- masa_kg excluida (colinealidad exacta: area * espesor * 2.5).
- One-hot solo categóricas; sin escalado (Random Forest no lo requiere).
- Transformaciones ajustadas solo con train (Pipeline).
"""

import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import OneHotEncoder

# Configuración de variables
CAT = ["tipo_vidrio", "proceso"]
NUM = ["area_m2", "espesor_mm", "calcin_pct", "energia_kwh_kg"]
TARGET = "co2_kg"
SEMILLA = 42

def cargar(ruta="data/raw/smartglass_co2.csv"):
    """Carga el dataset desde la ruta especificada."""
    return pd.read_csv(ruta)

def dividir(df, test_size=0.20):
    """Divide el dataset en conjuntos de entrenamiento y prueba."""
    X = df[CAT + NUM]
    y = df[TARGET]
    return train_test_split(X, y, test_size=test_size, random_state=SEMILLA)

def preprocesador():
    """Crea el transformador de columnas para variables categóricas."""
    return ColumnTransformer(
        [("onehot", OneHotEncoder(handle_unknown="ignore"), CAT)],
        remainder="passthrough"
    )