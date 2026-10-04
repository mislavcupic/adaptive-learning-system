import logging
import psycopg2
from psycopg2.extras import RealDictCursor
from typing import Optional

from ..config import get_settings

logger = logging.getLogger(__name__)


class MasteryRepository:

    def __init__(self):
        self.database_url = get_settings().database_url

    def _connect(self):
        return psycopg2.connect(self.database_url)

    def get_mastery(self, student_id: str, skill_name: str) -> Optional[dict]:
        """Trenutna procjena za vjestinu; None ako jos ne postoji."""
        query = """
                SELECT id, mastery_level, attempts_count, correct_count
                FROM skill_mastery
                WHERE student_id = %s AND skill_name = %s \
                """
        try:
            with self._connect() as conn:
                with conn.cursor(cursor_factory=RealDictCursor) as cur:
                    cur.execute(query, (student_id, skill_name))
                    row = cur.fetchone()
                    return dict(row) if row else None
        except Exception as e:
            logger.error(f"Dohvat procjene nije uspio: {e}")
            return None

    def save_mastery(self, student_id: str, skill_name: str,
                     mastery_level: float, is_correct: bool,
                     p_guess: float, p_slip: float, p_transit: float) -> bool:
        """
        Sprema novu procjenu; stvara zapis ako ga nema.

        Uz procjenu se cuvaju i koristeni BKT parametri, kako bi se
        kasnije moglo rekonstruirati kako je vrijednost dobivena.
        """
        query = """
                INSERT INTO skill_mastery
                (id, student_id, skill_name, mastery_level,
                 attempts_count, correct_count,
                 p_guess, p_slip, p_transit,
                 created_at, updated_at)
                VALUES
                    (gen_random_uuid(), %s, %s, %s, 1, %s, %s, %s, %s, NOW(), NOW())
                    ON CONFLICT (student_id, skill_name) DO UPDATE SET
                    mastery_level  = EXCLUDED.mastery_level,
                                                                attempts_count = skill_mastery.attempts_count + 1,
                                                                correct_count  = skill_mastery.correct_count + %s,
                                                                updated_at     = NOW() \
                """
        increment = 1 if is_correct else 0
        try:
            with self._connect() as conn:
                with conn.cursor() as cur:
                    cur.execute(query, (
                        student_id, skill_name, mastery_level, increment,
                        p_guess, p_slip, p_transit, increment
                    ))
                conn.commit()
            logger.info(f"Procjena spremljena: {skill_name} = {mastery_level:.3f}")
            return True
        except Exception as e:
            logger.error(f"Spremanje procjene nije uspjelo: {e}")
            return False