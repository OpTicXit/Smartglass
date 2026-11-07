"""
Proyección del CO2 total estimado a partir de una historia diaria (kg CO2e por día).

Misma fórmula que backend/.../service/ProyeccionCo2.java: si se cambia una, se cambia la otra
(test_proyeccion.py compara contra números calculados con la versión en Java).

- Variables: 1, t; Fourier semanal (k = 1, 2) con 21+ días; Fourier anual (k = 1, 2) con 730+ días.
- Mínimos cuadrados ordinarios (scikit-learn, sin intercepto propio: la columna de unos va en la matriz).
- Nunca proyecta más lejos que la historia disponible ni devuelve valores negativos.
"""
import numpy as np
from sklearn.linear_model import LinearRegression

CLAVES = ["semana", "mes", "cinco_meses", "anio", "cinco_anios"]
DIAS = [7, 30, 150, 365, 1826]
MIN_DIAS = 7
Z = 1.96


def variables(t, n):
    """Matriz de variables para los instantes t (array) con una historia de n días."""
    t = np.asarray(t, dtype=float)
    columnas = [np.ones_like(t), t]
    if n >= 21:
        for k in (1, 2):
            a = 2 * np.pi * k * t / 7.0
            columnas += [np.sin(a), np.cos(a)]
    if n >= 730:
        for k in (1, 2):
            a = 2 * np.pi * k * t / 365.25
            columnas += [np.sin(a), np.cos(a)]
    return np.column_stack(columnas)


def ajustar(historia):
    """Devuelve (modelo, sigma) ajustados a la historia diaria."""
    y = np.asarray(historia, dtype=float)
    n = len(y)
    x = variables(np.arange(n), n)
    modelo = LinearRegression(fit_intercept=False).fit(x, y)
    residuos = y - modelo.predict(x)
    sigma = float(np.sqrt(np.sum(residuos ** 2) / max(1, n - x.shape[1])))
    return modelo, sigma


def proyectar(historia):
    """Dict con historia, futuro y horizontes (forma del JSON del informe; fin lo pone quien exporta)."""
    y = [float(v) for v in historia]
    n = len(y)
    if n < MIN_DIAS:
        return {"historia": y, "futuro": [], "horizontes": []}
    maximo = max(d for d in DIAS if d <= n)
    modelo, sigma = ajustar(y)
    h = np.arange(1, maximo + 1)
    estimado = modelo.predict(variables(n - 1 + h, n))
    semiancho = Z * sigma * np.sqrt(1 + h / n)
    puntos = np.maximum(0, estimado)
    futuro = [{"y": float(p), "lo": float(max(0, e - s)), "hi": float(max(0, e + s))}
              for p, e, s in zip(puntos, estimado, semiancho)]
    acumulado = np.concatenate([[0.0], np.cumsum(puntos)])
    horizontes = []
    for clave, d in zip(CLAVES, DIAS):
        if d > n:
            break
        total = float(acumulado[d])
        ancho = Z * sigma * np.sqrt(d) * (1 + d / n)
        horizontes.append({"clave": clave, "dias": d, "total": total,
                           "lo": float(max(0, total - ancho)), "hi": float(total + ancho)})
    return {"historia": y, "futuro": futuro, "horizontes": horizontes}
