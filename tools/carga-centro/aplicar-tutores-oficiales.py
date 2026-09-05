#!/usr/bin/env python3
"""S137 — Aplica al catálogo derivado la lista oficial de tutores entregada por el
centro (jefatura de estudios) y arrastra todo lo que ese dato vuelve falso:

  (1) las 7 tutorías de Bachillerato pasan a los tutores oficiales;
  (2) requiereTutor -> 0 en las 6 actividades de bloque PTEV/PTVE-Religión, cuyo
      flag desciende del Hallazgo E (refutado en S134);
  (3) _meta.advertencia deja de decir que no hay importador (D-catalogo-meta-enganosa);
  (4) _meta.verificacionInvariantes: el recuento de S8 baja de 22 a 16 y el «28/28
      derivados (ver ambiguedad A5)» de I4 se matiza: 7 son oficiales y A5 está resuelta.

(1) y (2) son un PAR INDIVISIBLE: reescribir las 7 sin bajar el flag rompe S8 en las
seis actividades, porque las 7 filas viejas son PORTANTES (medido en S136).

Las guardas buscan por CONTENIDO, no por nombre de clave, y abortan sin escribir nada.
NO reejecutable a propósito. Por defecto no escribe; con --aplicar reescribe in situ.
"""
import json, argparse, sys, re, os

CATALOGO = "/home/luis/desarrollo/educhronos/docs/horario-referencia/catalogo-derivado.json"

OFICIALES = {                      # grupo (código del centro) -> profesor oficial
    "1B-A": "LEN7", "1B-B": "MAT7", "1B-C": "LEN2", "1B-D": "LEN1",
    "2B-A": "MAT2", "2B-B": "GH3",  "2B-C": "GH5",
}
META_VIEJA = "No hay importador"
META_NUEVA = ("La aplicacion NO lee este fichero. Lo lee "
              "tools/carga-centro/cargar-centro.py, que con el escribe el centro "
              "entero por la API REST del producto: es la sede unica de la "
              "estructura del centro y donde aterriza cualquier correccion de "
              "catalogo.")
FRAG_S8 = "22 actividades requiereTutor"
FRAG_I4 = "28/28 derivados"
FRAG_A5 = "ver ambiguedad A5"
I4_NUEVO = "28/28, 21 derivados y 7 oficiales del centro"
A5_NUEVO = "A5 resuelta: el centro entrego la lista oficial"

def indent_medido(ruta):
    """S135: el indent se MIDE del original, no se supone."""
    with open(ruta, encoding="utf-8") as f:
        for linea in f:
            m = re.match(r"^( +)\S", linea)
            if m:
                return len(m.group(1))
    return 2

def unica_clave_con(d, fragmento):
    """Devuelve la única clave cuyo valor contiene el fragmento, o None."""
    ks = [k for k, v in d.items() if isinstance(v, str) and fragmento in v]
    return ks[0] if len(ks) == 1 else None

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--aplicar", action="store_true")
    args = ap.parse_args()

    if not os.path.exists(CATALOGO):
        sys.exit(f"FATAL: no existe {CATALOGO}")
    cat = json.load(open(CATALOGO, encoding="utf-8"))
    ind = indent_medido(CATALOGO)

    tuts, acts = cat["tutorias"], cat["actividades"]
    campo_rt = next((k for k in acts[0] if "equiere" in k and "utor" in k), None)
    campo_nom = next((k for k in acts[0] if k in ("nombre", "codigo", "descripcion")), None)
    bloque = [a for a in acts
              if campo_nom and ("PTEV" in str(a[campo_nom]) or "PTVE" in str(a[campo_nom]))]
    con_rt = sum(1 for a in acts if a.get(campo_rt))
    meta = cat.get("_meta", {})
    vi = meta.get("verificacionInvariantes")

    print(f"indent medido: {ind}   tutorias={len(tuts)}   actividades={len(acts)}")
    print(f"campo_rt='{campo_rt}'   con_rt={con_rt}   bloque={len(bloque)}")

    # ---------- GUARDAS: si algo no cuadra, no se escribe NADA ----------
    fallos = []
    if len(bloque) != 6:
        fallos.append(f"esperadas 6 actividades de bloque, halladas {len(bloque)}")
    if any(not a.get(campo_rt) for a in bloque):
        fallos.append("alguna de las 6 ya tiene el flag a 0: estado inesperado")
    if con_rt != 22:
        fallos.append(f"esperadas 22 actividades con {campo_rt}, halladas {con_rt}")
    for g in OFICIALES:
        n = sum(1 for t in tuts if t.get("grupo") == g)
        if n != 1:
            fallos.append(f"grupo {g}: {n} entradas de tutoría (esperada 1)")

    k_adv = unica_clave_con(meta, META_VIEJA)
    if not k_adv:
        fallos.append(f"_meta: no hay exactamente un campo con {META_VIEJA!r}")
    if not isinstance(vi, dict):
        fallos.append("_meta.verificacionInvariantes ausente o no es objeto")
        k_s8 = k_i4 = None
    else:
        k_s8 = unica_clave_con(vi, FRAG_S8)
        k_i4 = unica_clave_con(vi, FRAG_I4)
        if not k_s8:
            fallos.append(f"verificacionInvariantes: no hay exactamente un campo con {FRAG_S8!r}")
        if not k_i4:
            fallos.append(f"verificacionInvariantes: no hay exactamente un campo con {FRAG_I4!r}")
    if fallos:
        print("\nABORTA, guardas incumplidas:")
        for f in fallos:
            print("   -", f)
        return 1

    # ---------- (1) TUTORIAS ----------
    print("\n(1) TUTORIAS:")
    cambios = 0
    for t in tuts:
        g = t.get("grupo")
        if g in OFICIALES:
            viejo, nuevo = t.get("tutorPrincipal"), OFICIALES[g]
            print(f"    {g:5s} {viejo!r:8s} -> {nuevo!r}")
            if viejo != nuevo:
                cambios += 1
            if args.aplicar:
                t["tutorPrincipal"] = nuevo
    if cambios != 7:
        print(f"ABORTA: cambian {cambios} tutorías, esperadas 7")
        return 1

    # ---------- (2) ACTIVIDADES ----------
    nuevo_total = con_rt - 6
    print(f"\n(2) ACTIVIDADES: {campo_rt} -> 0 en las 6; total {con_rt} -> {nuevo_total}")
    for a in bloque:
        print(f"    {a[campo_nom]!r}")
        if args.aplicar:
            a[campo_rt] = False if isinstance(a[campo_rt], bool) else 0

    # ---------- (3) ADVERTENCIA DE _meta ----------
    print(f"\n(3) _meta.{k_adv} (D-catalogo-meta-enganosa):")
    print(f"    ANTES:   {meta[k_adv]!r}")
    print(f"    DESPUES: {META_NUEVA!r}")
    if args.aplicar:
        meta[k_adv] = META_NUEVA

    # ---------- (4) INVARIANTES DECLARADOS EN _meta ----------
    print(f"\n(4) _meta.verificacionInvariantes:")
    s8_ant = vi[k_s8]
    s8_nue = s8_ant.replace(FRAG_S8, f"{nuevo_total} actividades requiereTutor")
    if s8_nue == s8_ant:
        print("ABORTA: el recuento de S8 no cambió; revisar a mano")
        return 1
    print(f"    {k_s8} ANTES:   {s8_ant!r}")
    print(f"    {k_s8} DESPUES: {s8_nue!r}")

    i4_ant = vi[k_i4]
    i4_nue = i4_ant.replace(FRAG_I4, I4_NUEVO).replace(FRAG_A5, A5_NUEVO)
    if i4_nue == i4_ant or "A5" in i4_nue.replace(A5_NUEVO, ""):
        print(f"ABORTA: I4 no quedó como se espera; texto actual: {i4_ant!r}")
        return 1
    print(f"    {k_i4} ANTES:   {i4_ant!r}")
    print(f"    {k_i4} DESPUES: {i4_nue!r}")
    if args.aplicar:
        vi[k_s8], vi[k_i4] = s8_nue, i4_nue

    intactos = [k for k in vi if k not in (k_s8, k_i4)]
    print(f"\n    resto de invariantes SIN TOCAR (no dependen de este cambio): {intactos}")
    print(f"    contadores de _meta SIN TOCAR: "
          f"{[k for k in meta if k not in (k_adv, 'verificacionInvariantes')]}")

    if not args.aplicar:
        print("\nCORRIDA EN SECO: no se ha escrito nada. Relanzar con --aplicar.")
        return 0
    with open(CATALOGO, "w", encoding="utf-8") as f:
        json.dump(cat, f, ensure_ascii=False, indent=ind)   # sin salto final: fiel al original
    print("\nESCRITO.")
    return 0

sys.exit(main())
