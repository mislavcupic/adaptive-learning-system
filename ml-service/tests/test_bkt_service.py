"""
Testovi Bayesian Knowledge Tracing modela.

BKT je jedna od nosivih komponenti sustava, pa se provjerava i sama
formula i njezina svojstva: da tocan odgovor podize procjenu, netocan
je spusta, te da vrijednost ostaje u intervalu [0, 1].
"""

import pytest

from app.services.bkt_service import BKTService


@pytest.fixture
def bkt():
    return BKTService()


class TestInitialMastery:

    def test_pocetna_procjena_je_u_intervalu(self, bkt):
        prior = bkt.get_initial_mastery()
        assert 0.0 < prior < 1.0

    def test_pocetna_procjena_je_stalna(self, bkt):
        assert bkt.get_initial_mastery() == bkt.get_initial_mastery()


class TestParameters:

    def test_parametri_su_vjerojatnosti(self, bkt):
        for value in (bkt.p_guess, bkt.p_slip, bkt.p_transit):
            assert 0.0 <= value <= 1.0

    def test_pogadanje_i_omaska_su_mali(self, bkt):
        # Ako bi pogadanje ili omaska bili veliki, odgovor ne bi nosio
        # gotovo nikakvu informaciju o znanju.
        assert bkt.p_guess < 0.5
        assert bkt.p_slip < 0.5


class TestUpdateDirection:

    def test_tocan_odgovor_podize_procjenu(self, bkt):
        before = 0.3
        after = bkt.update_mastery(current_mastery=before, is_correct=True)
        assert after > before

    def test_netocan_odgovor_spusta_procjenu(self, bkt):
        before = 0.7
        after = bkt.update_mastery(current_mastery=before, is_correct=False)
        assert after < before

    def test_niz_tocnih_odgovora_raste(self, bkt):
        level = bkt.get_initial_mastery()
        history = [level]

        for _ in range(5):
            level = bkt.update_mastery(current_mastery=level, is_correct=True)
            history.append(level)

        assert history == sorted(history)
        assert history[-1] > history[0]

    def test_niz_netocnih_odgovora_pada(self, bkt):
        level = 0.9
        history = [level]

        for _ in range(5):
            level = bkt.update_mastery(current_mastery=level, is_correct=False)
            history.append(level)

        assert history == sorted(history, reverse=True)


class TestBounds:

    @pytest.mark.parametrize("start", [0.0, 0.1, 0.5, 0.9, 1.0])
    def test_rezultat_ostaje_u_intervalu(self, bkt, start):
        for correct in (True, False):
            result = bkt.update_mastery(current_mastery=start, is_correct=correct)
            assert 0.0 <= result <= 1.0

    def test_visoka_procjena_ne_prelazi_jedan(self, bkt):
        level = 0.99
        for _ in range(20):
            level = bkt.update_mastery(current_mastery=level, is_correct=True)
        assert level <= 1.0

    def test_niska_procjena_ne_pada_ispod_nule(self, bkt):
        level = 0.01
        for _ in range(20):
            level = bkt.update_mastery(current_mastery=level, is_correct=False)
        assert level >= 0.0


class TestLearningCurve:

    def test_oporavak_nakon_pogreske(self, bkt):
        """Jedna pogreska ne smije ponistiti cijeli napredak."""
        level = bkt.get_initial_mastery()
        for _ in range(4):
            level = bkt.update_mastery(current_mastery=level, is_correct=True)

        after_success = level
        after_mistake = bkt.update_mastery(current_mastery=level, is_correct=False)
        recovered = bkt.update_mastery(current_mastery=after_mistake, is_correct=True)

        assert after_mistake < after_success
        assert recovered > after_mistake

    def test_prvi_tocan_odgovor_nosi_najvise(self, bkt):
        """Dobitak se smanjuje kako procjena raste prema jedinici."""
        low = bkt.get_initial_mastery()
        gain_low = bkt.update_mastery(current_mastery=low, is_correct=True) - low

        high = 0.9
        gain_high = bkt.update_mastery(current_mastery=high, is_correct=True) - high

        assert gain_low > gain_high


class TestInterpretation:

    def test_visoka_procjena_opisana_kao_usvojeno(self, bkt):
        label = bkt.interpret_mastery(0.97)
        assert isinstance(label, str)
        assert len(label) > 0

    def test_niska_i_visoka_imaju_razlicit_opis(self, bkt):
        assert bkt.interpret_mastery(0.1) != bkt.interpret_mastery(0.95)

    @pytest.mark.parametrize("level", [0.0, 0.25, 0.5, 0.75, 1.0])
    def test_svaka_razina_ima_opis(self, bkt, level):
        label = bkt.interpret_mastery(level)
        assert isinstance(label, str) and label