"""
Bayesian Knowledge Tracing (BKT) Service

BKT parametri:
- P(L₀): Prior - vjerojatnost da student već zna skill (default: 0.3)
- P(T): Transit - vjerojatnost učenja nakon vježbe (default: 0.2)
- P(G): Guess - vjerojatnost pogađanja bez znanja (default: 0.25)
- P(S): Slip - vjerojatnost greške iako zna (default: 0.1)
"""

import logging

logger = logging.getLogger(__name__)


class BKTService:
    """Bayesian Knowledge Tracing za procjenu znanja studenta."""

    DEFAULT_P_INIT = 0.3
    DEFAULT_P_TRANSIT = 0.2
    DEFAULT_P_GUESS = 0.25
    DEFAULT_P_SLIP = 0.1

    def __init__(
            self,
            p_init: float = DEFAULT_P_INIT,
            p_transit: float = DEFAULT_P_TRANSIT,
            p_guess: float = DEFAULT_P_GUESS,
            p_slip: float = DEFAULT_P_SLIP
    ):
        self.p_init = p_init
        self.p_transit = p_transit
        self.p_guess = p_guess
        self.p_slip = p_slip

    def update_mastery(
            self,
            current_mastery: float,
            is_correct: bool
    ) -> float:
        """
        Ažurira procjenu znanja (mastery) na temelju odgovora.
        """
        if is_correct:
            p_correct_given_knows = 1 - self.p_slip
            p_correct_given_not_knows = self.p_guess
            p_correct = (p_correct_given_knows * current_mastery +
                         p_correct_given_not_knows * (1 - current_mastery))

            if p_correct > 0:
                p_knows_given_correct = (p_correct_given_knows * current_mastery) / p_correct
            else:
                p_knows_given_correct = current_mastery
        else:
            p_incorrect_given_knows = self.p_slip
            p_incorrect_given_not_knows = 1 - self.p_guess
            p_incorrect = (p_incorrect_given_knows * current_mastery +
                           p_incorrect_given_not_knows * (1 - current_mastery))

            if p_incorrect > 0:
                p_knows_given_correct = (p_incorrect_given_knows * current_mastery) / p_incorrect
            else:
                p_knows_given_correct = current_mastery

        new_mastery = p_knows_given_correct + (1 - p_knows_given_correct) * self.p_transit
        new_mastery = max(0.0, min(1.0, new_mastery))

        logger.info(
            f"BKT Update: {current_mastery:.3f} -> {new_mastery:.3f} "
            f"(correct={is_correct})"
        )

        return new_mastery

    def get_initial_mastery(self) -> float:
        """Vraća početnu procjenu znanja za novog studenta."""
        return self.p_init

    def interpret_mastery(self, mastery: float) -> str:
        """Interpretira razinu znanja za prikaz studentu."""
        if mastery >= 0.95:
            return "Izvrsno - potpuno vladaš ovim konceptom!"
        elif mastery >= 0.8:
            return "Vrlo dobro - razumiješ koncept, još malo vježbe."
        elif mastery >= 0.6:
            return "Dobro - na pravom si putu, nastavi vježbati."
        elif mastery >= 0.4:
            return "Osrednje - trebat će više vježbe."
        else:
            return "Početnik - koncentriraj se na osnove."