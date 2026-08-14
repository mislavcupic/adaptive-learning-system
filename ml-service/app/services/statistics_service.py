"""
Statistics Service

Provodi statističku analizu istraživačkih podataka:
- ANCOVA (posttest ~ group + pretest kovarijat)
- Deskriptivna statistika po grupama
- Veličine učinka (Cohen's d, Hedges' g s 95% CI)
- Prilagođene aritmetičke sredine (adjusted means)
- Provjera pretpostavki (homogenost nagiba, Levene, Shapiro-Wilk)
"""

import logging
import math
from typing import List, Dict, Any, Optional

import numpy as np
import pandas as pd
import pingouin as pg
import statsmodels.formula.api as smf
from scipy import stats

logger = logging.getLogger(__name__)

# Redoslijed grupa: referentna (kontrolna) prva
CONTROL_LABEL = "CONTROL"
EXPERIMENTAL_LABEL = "EXPERIMENTAL"


class StatisticsService:
    """Servis za statističku analizu (ANCOVA, veličine učinka, pretpostavke)."""

    def run_ancova(self, records: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        Provodi ANCOVA analizu s pripadajućim veličinama učinka.

        records: lista dictova s poljima:
            - group: "CONTROL" | "EXPERIMENTAL"
            - pretest: float (kovarijat)
            - posttest: float (zavisna varijabla)

        Vraća: deskriptivu, ANCOVA tablicu, prilagođene sredine,
        veličine učinka i provjere pretpostavki.
        """
        df = pd.DataFrame(records)

        if df.empty:
            raise ValueError("Nema podataka za analizu.")

        required_cols = {"group", "pretest", "posttest"}
        if not required_cols.issubset(df.columns):
            raise ValueError(f"Nedostaju stupci. Potrebno: {required_cols}")

        df = df.dropna(subset=["group", "pretest", "posttest"])
        df["pretest"] = pd.to_numeric(df["pretest"], errors="coerce")
        df["posttest"] = pd.to_numeric(df["posttest"], errors="coerce")
        df = df.dropna(subset=["pretest", "posttest"])

        groups = df["group"].unique().tolist()
        n_total = len(df)

        descriptives = self._compute_descriptives(df)

        result: Dict[str, Any] = {
            "n_total": n_total,
            "groups": groups,
            "descriptives": descriptives,
            "ancova": None,
            "adjusted_means": None,
            "effect_size": None,
            "assumptions": None,
            "warning": None,
        }

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

        # --- Glavna ANCOVA ---
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

        # --- Prilagođene sredine, veličine učinka, pretpostavke ---
        try:
            model = smf.ols("posttest ~ C(group) + pretest", data=df).fit()

            adjusted = self._compute_adjusted_means(df, model)
            result["adjusted_means"] = adjusted

            result["effect_size"] = self._compute_effect_sizes(
                df, model, adjusted
            )
            result["assumptions"] = self._check_assumptions(df, model)
        except Exception as e:
            logger.error(f"Effect size / assumption computation failed: {e}")
            existing = result.get("warning")
            msg = f"Veličine učinka nisu izračunate: {e}"
            result["warning"] = f"{existing} {msg}".strip() if existing else msg

        return result

    # ------------------------------------------------------------------
    # Deskriptiva
    # ------------------------------------------------------------------

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

    # ------------------------------------------------------------------
    # Prilagođene aritmetičke sredine
    # ------------------------------------------------------------------

    def _compute_adjusted_means(
            self, df: pd.DataFrame, model: Any
    ) -> List[Dict[str, Any]]:
        """
        Prilagođena sredina = predviđeni posttest za svaku grupu
        pri ukupnom prosjeku kovarijata (grand mean pretesta).

        M_adj = M_posttest - b * (M_pretest_grupe - M_pretest_ukupno)
        """
        grand_pretest = float(df["pretest"].mean())
        b = float(model.params.get("pretest", 0.0))

        out = []
        for group_name, sub in df.groupby("group"):
            observed = float(sub["posttest"].mean())
            group_pretest = float(sub["pretest"].mean())
            adjusted = observed - b * (group_pretest - grand_pretest)

            # Standardna pogreška prilagođene sredine
            n_g = len(sub)
            mse = float(model.mse_resid)
            ss_pretest = float(((df["pretest"] - grand_pretest) ** 2).sum())
            se_component = (group_pretest - grand_pretest) ** 2 / ss_pretest if ss_pretest > 0 else 0.0
            se = math.sqrt(mse * (1.0 / n_g + se_component)) if n_g > 0 else None

            out.append({
                "group": group_name,
                "n": int(n_g),
                "observed_mean": round(observed, 2),
                "adjusted_mean": round(adjusted, 2),
                "se": round(se, 3) if se is not None else None,
            })

        return out

    # ------------------------------------------------------------------
    # Veličine učinka
    # ------------------------------------------------------------------

    def _compute_effect_sizes(
            self,
            df: pd.DataFrame,
            model: Any,
            adjusted: List[Dict[str, Any]],
    ) -> Optional[Dict[str, Any]]:
        """
        Cohen's d i Hedges' g na PRILAGOĐENIM sredinama,
        standardizirano korijenom rezidualnog MSE iz ANCOVA modela.

        Pozitivna vrijednost = eksperimentalna grupa postiže više.
        """
        adj_map = {a["group"]: a for a in adjusted}

        if CONTROL_LABEL not in adj_map or EXPERIMENTAL_LABEL not in adj_map:
            return None

        exp = adj_map[EXPERIMENTAL_LABEL]
        con = adj_map[CONTROL_LABEL]

        n1 = exp["n"]
        n2 = con["n"]
        if n1 < 1 or n2 < 1:
            return None

        # Pooled SD posttesta BEZ kovarijata — standardna konvencija,
        # osigurava usporedivost s d-ovima iz drugih istraživanja.
        sd1 = float(df[df["group"] == EXPERIMENTAL_LABEL]["posttest"].std(ddof=1))
        sd2 = float(df[df["group"] == CONTROL_LABEL]["posttest"].std(ddof=1))

        if n1 + n2 - 2 <= 0:
            return None

        pooled_var = (((n1 - 1) * sd1 ** 2) + ((n2 - 1) * sd2 ** 2)) / (n1 + n2 - 2)
        if pooled_var <= 0 or pooled_var != pooled_var:
            return None

        pooled_sd = math.sqrt(pooled_var)
        mean_diff = exp["adjusted_mean"] - con["adjusted_mean"]
        d = mean_diff / pooled_sd

        # Hedges' korekcija za mali uzorak
        # df rezidualа = N - k - 1 (k grupa, 1 kovarijat)
        df_resid = float(model.df_resid)
        if df_resid > 1:
            correction = 1.0 - (3.0 / (4.0 * df_resid - 1.0))
        else:
            correction = 1.0
        g = d * correction

        # 95% CI za d (Hedges & Olkin aproksimacija)
        se_d = math.sqrt((n1 + n2) / (n1 * n2) + (d ** 2) / (2.0 * (n1 + n2)))
        crit = float(stats.t.ppf(0.975, df_resid)) if df_resid > 0 else 1.96
        ci_low = d - crit * se_d
        ci_high = d + crit * se_d

        return {
            "cohens_d": round(d, 3),
            "hedges_g": round(g, 3),
            "se": round(se_d, 3),
            "ci_lower": round(ci_low, 3),
            "ci_upper": round(ci_high, 3),
            "mean_difference": round(mean_diff, 2),
            "pooled_sd": round(pooled_sd, 2),
            "magnitude": self._interpret_effect(abs(g)),
            "favors": EXPERIMENTAL_LABEL if mean_diff > 0 else CONTROL_LABEL,
        }

    @staticmethod
    def _interpret_effect(abs_g: float) -> str:
        """Cohenove konvencionalne granice."""
        if abs_g < 0.2:
            return "negligible"
        if abs_g < 0.5:
            return "small"
        if abs_g < 0.8:
            return "medium"
        return "large"

    # ------------------------------------------------------------------
    # Pretpostavke
    # ------------------------------------------------------------------

    def _check_assumptions(
            self, df: pd.DataFrame, model: Any
    ) -> Dict[str, Any]:
        """
        Tri ključne pretpostavke ANCOVE:
        1. Homogenost regresijskih nagiba (interakcija grupa × pretest)
        2. Homogenost varijanci (Levene)
        3. Normalnost reziduala (Shapiro-Wilk)
        """
        out: Dict[str, Any] = {
            "homogeneity_of_slopes": None,
            "levene": None,
            "shapiro": None,
        }

        # 1. Homogenost regresijskih nagiba
        try:
            inter_model = smf.ols(
                "posttest ~ C(group) * pretest", data=df
            ).fit()
            from statsmodels.stats.anova import anova_lm
            tbl = anova_lm(inter_model, typ=2)

            # Redak interakcije
            inter_row = None
            for idx in tbl.index:
                if ":" in str(idx):
                    inter_row = tbl.loc[idx]
                    break

            if inter_row is not None:
                p_inter = self._safe_float(inter_row.get("PR(>F)"))
                out["homogeneity_of_slopes"] = {
                    "f": self._safe_float(inter_row.get("F")),
                    "p": p_inter,
                    # Pretpostavka je ZADOVOLJENA kad interakcija NIJE značajna
                    "satisfied": p_inter is not None and p_inter >= 0.05,
                    "note": (
                        "Interakcija grupa × pretest nije značajna — "
                        "pretpostavka homogenosti nagiba je zadovoljena."
                        if p_inter is not None and p_inter >= 0.05
                        else "Interakcija je značajna — nagibi se razlikuju "
                             "među grupama, pa ANCOVA rezultat treba tumačiti "
                             "s oprezom (razmotriti Johnson-Neyman postupak)."
                    ),
                }
        except Exception as e:
            logger.warning(f"Homogeneity of slopes test failed: {e}")

        # 2. Levene test homogenosti varijanci
        try:
            lev = pg.homoscedasticity(
                data=df, dv="posttest", group="group", method="levene"
            )
            p_lev = self._safe_float(lev["pval"].iloc[0])
            out["levene"] = {
                "statistic": self._safe_float(lev["W"].iloc[0]) if "W" in lev.columns else None,
                "p": p_lev,
                "satisfied": p_lev is not None and p_lev >= 0.05,
            }
        except Exception as e:
            logger.warning(f"Levene test failed: {e}")

        # 3. Shapiro-Wilk na rezidualima
        try:
            resid = np.asarray(model.resid)
            if 3 <= len(resid) <= 5000:
                w, p_sw = stats.shapiro(resid)
                out["shapiro"] = {
                    "statistic": self._safe_float(w),
                    "p": self._safe_float(p_sw),
                    "satisfied": p_sw >= 0.05,
                }
        except Exception as e:
            logger.warning(f"Shapiro-Wilk test failed: {e}")

        return out

    # Parsiranje ANCOVA tablice

    def _parse_ancova(self, ancova_df: pd.DataFrame) -> Dict[str, Any]:
        """
        Izvuci ključne vrijednosti iz pingouin ANCOVA tablice.
        Redovi su faktori: 'group', 'pretest', 'Residual'.
        """
        rows = []
        group_result: Optional[Dict[str, Any]] = None

        for _, row in ancova_df.iterrows():
            source = str(row.get("Source", ""))
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