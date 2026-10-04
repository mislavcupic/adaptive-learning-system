"""
Testovi ANCOVA analize.

Provjerava se struktura odgovora, prepoznavanje razlike medu skupinama,
velicine ucinka i provjere pretpostavki.

Podaci namjerno sadrze sum. Savrseno linearni podaci daju rezidual
jednak nuli, pa F-omjer postaje besmisleno velik, a to ne odrazava
stvarne uvjete mjerenja.
"""

import random

import pytest

from app.services.statistics_service import StatisticsService


@pytest.fixture
def stats():
    return StatisticsService()


def record(group, pretest, posttest):
    return {"group": group, "pretest": pretest, "posttest": posttest}


def noise(seed):
    rng = random.Random(seed)
    return lambda: rng.uniform(-4, 4)


@pytest.fixture
def clear_difference():
    """Eksperimentalna skupina ima izrazito bolji posttest."""
    jitter = noise(42)
    control = [record("CONTROL", 50 + i + jitter(), 55 + i + jitter())
               for i in range(15)]
    experimental = [record("EXPERIMENTAL", 50 + i + jitter(), 75 + i + jitter())
                    for i in range(15)]
    return control + experimental


@pytest.fixture
def no_difference():
    """Skupine se ne razlikuju."""
    jitter = noise(7)
    control = [record("CONTROL", 50 + i + jitter(), 60 + i + jitter())
               for i in range(15)]
    experimental = [record("EXPERIMENTAL", 50 + i + jitter(), 60 + i + jitter())
                    for i in range(15)]
    return control + experimental


class TestStructure:

    def test_vraca_ocekivane_kljuceve(self, stats, clear_difference):
        result = stats.run_ancova(clear_difference)

        for key in ("n_total", "groups", "descriptives", "ancova",
                    "adjusted_means", "effect_size", "assumptions"):
            assert key in result

    def test_ancova_sadrzi_ucinak_skupine(self, stats, clear_difference):
        ancova = stats.run_ancova(clear_difference)["ancova"]

        assert "group_effect" in ancova
        for key in ("f", "p", "partial_eta_sq", "df"):
            assert key in ancova["group_effect"]

    def test_ancova_sadrzi_tablicu_izvora(self, stats, clear_difference):
        table = stats.run_ancova(clear_difference)["ancova"]["table"]

        sources = {row["source"] for row in table}
        assert {"group", "pretest", "Residual"}.issubset(sources)

    def test_broj_ispitanika_odgovara_podacima(self, stats, clear_difference):
        result = stats.run_ancova(clear_difference)

        assert result["n_total"] == len(clear_difference)


class TestDetection:

    def test_prepoznaje_razliku_medu_skupinama(self, stats, clear_difference):
        effect = stats.run_ancova(clear_difference)["ancova"]["group_effect"]

        assert effect["p"] < 0.05
        assert effect["f"] > 0

    def test_oznacava_rezultat_znacajnim(self, stats, clear_difference):
        assert stats.run_ancova(clear_difference)["ancova"]["significant"] is True

    def test_ne_prijavljuje_razliku_kad_je_nema(self, stats, no_difference):
        effect = stats.run_ancova(no_difference)["ancova"]["group_effect"]

        assert effect["p"] > 0.05

    def test_eta_kvadrat_je_u_intervalu(self, stats, clear_difference):
        effect = stats.run_ancova(clear_difference)["ancova"]["group_effect"]

        assert 0.0 <= effect["partial_eta_sq"] <= 1.0

    def test_veca_razlika_daje_veci_eta(self, stats):
        jitter = noise(1)
        small = ([record("CONTROL", 50 + i + jitter(), 60 + i + jitter())
                  for i in range(15)]
                 + [record("EXPERIMENTAL", 50 + i + jitter(), 63 + i + jitter())
                    for i in range(15)])

        jitter = noise(2)
        large = ([record("CONTROL", 50 + i + jitter(), 60 + i + jitter())
                  for i in range(15)]
                 + [record("EXPERIMENTAL", 50 + i + jitter(), 90 + i + jitter())
                    for i in range(15)])

        eta_small = stats.run_ancova(small)["ancova"]["group_effect"]["partial_eta_sq"]
        eta_large = stats.run_ancova(large)["ancova"]["group_effect"]["partial_eta_sq"]

        assert eta_large > eta_small


class TestEffectSize:

    def test_cohen_i_hedges_postoje(self, stats, clear_difference):
        effect = stats.run_ancova(clear_difference)["effect_size"]

        assert "cohens_d" in effect
        assert "hedges_g" in effect

    def test_hedges_je_manji_od_cohena(self, stats, clear_difference):
        """Korekcija za male uzorke uvijek smanjuje procjenu."""
        effect = stats.run_ancova(clear_difference)["effect_size"]

        assert abs(effect["hedges_g"]) < abs(effect["cohens_d"])

    def test_interval_pouzdanosti_obuhvaca_procjenu(self, stats, clear_difference):
        effect = stats.run_ancova(clear_difference)["effect_size"]

        assert effect["ci_lower"] < effect["cohens_d"] < effect["ci_upper"]

    def test_navodi_skupinu_u_prednosti(self, stats, clear_difference):
        effect = stats.run_ancova(clear_difference)["effect_size"]

        assert effect["favors"] == "EXPERIMENTAL"

    def test_opisuje_velicinu_ucinka(self, stats, clear_difference):
        effect = stats.run_ancova(clear_difference)["effect_size"]

        assert effect["magnitude"] in ("negligible", "small", "medium", "large")

    def test_nema_ucinka_kad_nema_razlike(self, stats, no_difference):
        effect = stats.run_ancova(no_difference)["effect_size"]

        assert abs(effect["cohens_d"]) < 0.5

    def test_standardizacija_koristi_sd_posttesta(self, stats, clear_difference):
        """
        Ranije je koristen korijen rezidualnog MSE, sto je davalo
        besmisleno velike vrijednosti. Objedinjena SD posttesta mora
        biti usporediva s rasprsenjem unutar skupina.
        """
        result = stats.run_ancova(clear_difference)
        pooled = result["effect_size"]["pooled_sd"]
        group_sd = result["descriptives"][0]["posttest_sd"]

        assert pooled == pytest.approx(group_sd, rel=0.5)


class TestAssumptions:

    def test_pretpostavke_su_provjerene(self, stats, clear_difference):
        assumptions = stats.run_ancova(clear_difference)["assumptions"]

        assert isinstance(assumptions, dict)
        assert len(assumptions) >= 3

    def test_homogenost_nagiba_je_zadovoljena(self, stats, clear_difference):
        """Podaci su konstruirani s priblizno paralelnim pravcima."""
        assumptions = stats.run_ancova(clear_difference)["assumptions"]

        slopes = assumptions.get("homogeneity_of_slopes")
        if slopes and slopes.get("p_value") is not None:
            assert slopes["p_value"] >= 0.05


class TestDescriptives:

    def test_obje_skupine_su_opisane(self, stats, clear_difference):
        descriptives = stats.run_ancova(clear_difference)["descriptives"]

        groups = {d["group"] for d in descriptives}
        assert groups == {"CONTROL", "EXPERIMENTAL"}

    def test_sadrzi_sredine_i_rasprsenja(self, stats, clear_difference):
        entry = stats.run_ancova(clear_difference)["descriptives"][0]

        for key in ("n", "pretest_mean", "pretest_sd",
                    "posttest_mean", "posttest_sd"):
            assert key in entry

    def test_zbroj_ispitanika_odgovara(self, stats, clear_difference):
        descriptives = stats.run_ancova(clear_difference)["descriptives"]

        assert sum(d["n"] for d in descriptives) == len(clear_difference)


class TestAdjustedMeans:

    def test_obje_skupine_imaju_prilagodenu_sredinu(self, stats, clear_difference):
        adjusted = stats.run_ancova(clear_difference)["adjusted_means"]

        assert len(adjusted) == 2

    def test_eksperimentalna_ima_visu_prilagodenu_sredinu(self, stats, clear_difference):
        adjusted = stats.run_ancova(clear_difference)["adjusted_means"]
        by_group = {a["group"]: a for a in adjusted}

        key = "adjusted_mean" if "adjusted_mean" in by_group["EXPERIMENTAL"] else "mean"

        assert by_group["EXPERIMENTAL"][key] > by_group["CONTROL"][key]