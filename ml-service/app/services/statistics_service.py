"""
Statistics Service

Provodi statističku analizu istraživačkih podataka:
- ANCOVA (posttest ~ group + pretest kovarijat)
- Deskriptivna statistika po grupama
"""

import logging
from typing import List, Dict, Any, Optional

import pandas as pd
import pingouin as pg

logger = logging.getLogger(__name__)


class StatisticsService:
    """Servis za statističku analizu (ANCOVA i deskriptiva)."""

    def run_ancova(self, records: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        Provodi ANCOVA analizu.

        records: lista dictova s poljima:
            - group: "CONTROL" | "EXPERIMENTAL"
            - pretest: float (kovarijat)
            - posttest: float (zavisna varijabla)

        Vraća: deskriptivnu statistiku po grupama + ANCOVA rezultat
        (F, p, partial eta-squared za faktor grupe).
        """
        # Pretvori u DataFrame
        df = pd.DataFrame(records)

        # Validacija — trebamo obje grupe i dovoljno podataka
        if df.empty:
            raise ValueError("Nema podataka za analizu.")

        required_cols = {"group", "pretest", "posttest"}
        if not required_cols.issubset(df.columns):
            raise ValueError(f"Nedostaju stupci. Potrebno: {required_cols}")

        # Ukloni retke s nedostajućim vrijednostima
        df = df.dropna(subset=["group", "pretest", "posttest"])

        # Osiguraj numeričke tipove
        df["pretest"] = pd.to_numeric(df["pretest"], errors="coerce")
        df["posttest"] = pd.to_numeric(df["posttest"], errors="coerce")
        df = df.dropna(subset=["pretest", "posttest"])

        groups = df["group"].unique().tolist()
        n_total = len(df)

        # Deskriptivna statistika po grupama
        descriptives = self._compute_descriptives(df)

        result: Dict[str, Any] = {
            "n_total": n_total,
            "groups": groups,
            "descriptives": descriptives,
            "ancova": None,
            "warning": None,
        }

        # ANCOVA zahtijeva obje grupe i dovoljno podataka
        if len(groups) < 2:
            result["warning"] = (
                "ANCOVA zahtijeva obje grupe (CONTROL i EXPERIMENTAL). "
                f"Pronađena samo: {groups}."
            )
            return result

        if n_total < 4:
            result["warning"] = (
                f"Premalo podataka za ANCOVA (n={n_total}). "
                "Rezultati nisu pouzdani dok se ne prikupi više podataka."
            )

        # Pokreni ANCOVA
        try:
            ancova_df = pg.ancova(
                data=df,
                dv="posttest",
                covar="pretest",
                between="group"
            )
            result["ancova"] = self._parse_ancova(ancova_df)
        except Exception as e:
            logger.error(f"ANCOVA computation failed: {e}")
            result["warning"] = f"ANCOVA izračun nije uspio: {e}"

        return result

    def _compute_descriptives(self, df: pd.DataFrame) -> List[Dict[str, Any]]:
        """Prosjek, SD, n po grupi za pretest i posttest."""
        out = []
        for group_name, sub in df.groupby("group"):
            out.append({
                "group": group_name,
                "n": int(len(sub)),
                "pretest_mean": round(float(sub["pretest"].mean()), 2),
                "pretest_sd": round(float(sub["pretest"].std(ddof=1)), 2) if len(sub) > 1 else 0.0,
                "posttest_mean": round(float(sub["posttest"].mean()), 2),
                "posttest_sd": round(float(sub["posttest"].std(ddof=1)), 2) if len(sub) > 1 else 0.0,
            })
        return out

    def _parse_ancova(self, ancova_df: pd.DataFrame) -> Dict[str, Any]:
        """
        Izvuci ključne vrijednosti iz pingouin ANCOVA tablice.
        Redovi su faktori: 'group', 'pretest', 'Residual'.
        """
        rows = []
        group_result: Optional[Dict[str, Any]] = None

        for _, row in ancova_df.iterrows():
            source = str(row.get("Source", ""))
            # p-unc i np2 mogu biti NaN za Residual
            p_val = row.get("p-unc")
            eta = row.get("np2")

            entry = {
                "source": source,
                "ss": self._safe_float(row.get("SS")),
                "df": self._safe_float(row.get("DF")),
                "f": self._safe_float(row.get("F")),
                "p": self._safe_float(p_val),
                "partial_eta_sq": self._safe_float(eta),
            }
            rows.append(entry)

            if source == "group":
                group_result = entry

        significant = (
                group_result is not None
                and group_result["p"] is not None
                and group_result["p"] < 0.05
        )

        return {
            "table": rows,
            "group_effect": group_result,
            "significant": significant,
        }

    @staticmethod
    def _safe_float(val: Any) -> Optional[float]:
        """Pretvori u float, vrati None ako NaN/nevaljano."""
        try:
            f = float(val)
            if f != f:  # NaN check
                return None
            return round(f, 4)
        except (TypeError, ValueError):
            return None