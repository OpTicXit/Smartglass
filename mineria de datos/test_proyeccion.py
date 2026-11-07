"""Comprobaciones de proyeccion_co2.py (sin pytest):  python "mineria de datos/test_proyeccion.py"."""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from proyeccion_co2 import proyectar  # noqa: E402


def cerca(a, b, tol=1e-6):
    return abs(a - b) <= tol * max(1.0, abs(b))


# Tendencia lineal sin ruido: el primer día futuro sigue la recta
p = proyectar([10 + 0.5 * t for t in range(60)])
assert cerca(p["futuro"][0]["y"], 40.0), p["futuro"][0]
assert [h["clave"] for h in p["horizontes"]] == ["semana", "mes"]
assert len(p["futuro"]) == 30

# Menos de 7 días: no proyecta
assert proyectar([5] * 6)["horizontes"] == []

# Caída fuerte: nada negativo
p = proyectar([max(0, 100 - 2 * t) for t in range(60)])
assert all(b["y"] >= 0 and b["lo"] >= 0 for b in p["futuro"])
assert all(h["total"] >= 0 and h["lo"] >= 0 for h in p["horizontes"])

# Todo en ceros: sin NaN
p = proyectar([0] * 30)
assert all(h["total"] == 0 and h["lo"] == 0 and h["hi"] == 0 for h in p["horizontes"])
assert not any(math.isnan(b["y"]) for b in p["futuro"])

# Mismos números que la versión en Java (ProyeccionCo2.proyectar con la misma serie)
p = proyectar([20 + 0.01 * t + (t % 5) - 2 for t in range(60)])
java = {"semana": (145.92954797226488, 137.38004115189563, 154.47905479263414),
        "mes": (631.2868421412946, 607.5118502659687, 655.0618340166205)}
for h in p["horizontes"]:
    total, lo, hi = java[h["clave"]]
    assert cerca(h["total"], total) and cerca(h["lo"], lo) and cerca(h["hi"], hi), (h, java[h["clave"]])
assert cerca(p["futuro"][0]["y"], 20.986549819744535)

print("ok")
