"""
RAG (Retrieval-Augmented Generation) Service

Koristi PGVector za dohvat relevantnih prethodnih bilješki o studentu.
"""

import logging
from typing import Optional
import psycopg2
from psycopg2.extras import RealDictCursor
from openai import OpenAI

from ..config import get_settings

logger = logging.getLogger(__name__)


class RAGService:
    """Retrieval-Augmented Generation za personalizirani kontekst."""

    def __init__(self):
        self.settings = get_settings()
        self.client = OpenAI(api_key=self.settings.openai_api_key) if self.settings.openai_api_key else None

    def get_embedding(self, text: str) -> list[float]:
        """Generira embedding za tekst koristeći OpenAI."""
        if not self.client:
            logger.warning("OpenAI client not configured, returning empty embedding")
            return []

        try:
            response = self.client.embeddings.create(
                model=self.settings.embedding_model,
                input=text
            )
            return response.data[0].embedding
        except Exception as e:
            logger.error(f"Error generating embedding: {e}")
            return []

    def get_relevant_notes(
            self,
            student_id: str,
            query: str,
            limit: int = 5
    ) -> list[dict]:
        """
        Dohvaća relevantne prethodne bilješke o studentu iz analytic_notes.
        """
        query_embedding = self.get_embedding(query)

        if not query_embedding:
            logger.warning("No embedding generated, returning empty notes")
            return []

        try:
            conn = psycopg2.connect(self.settings.database_url)
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            cursor.execute("""
                           SELECT
                               id,
                               insight,
                               created_at
                           FROM analytic_notes
                           WHERE student_id = %s
                           ORDER BY embedding <=> %s::vector
                               LIMIT %s
                           """, (student_id, str(query_embedding), limit))

            notes = cursor.fetchall()
            cursor.close()
            conn.close()

            logger.info(f"Retrieved {len(notes)} relevant notes for student {student_id}")
            return [dict(note) for note in notes]

        except Exception as e:
            logger.error(f"Error retrieving notes: {e}")
            return []

    def save_note(
            self,
            student_id: str,
            insight: str,
            submission_id: Optional[str] = None
    ) -> bool:
        """
        Sprema novu bilješku o studentu (Agentic Memory).
        """
        embedding = self.get_embedding(insight)

        if not embedding:
            logger.warning("No embedding generated, note not saved")
            return False

        try:
            conn = psycopg2.connect(self.settings.database_url)
            cursor = conn.cursor()

            cursor.execute("""
                           INSERT INTO analytic_notes (student_id, submission_id, insight, embedding)
                           VALUES (%s, %s, %s, %s::vector)
                           """, (student_id, submission_id, insight, str(embedding)))

            conn.commit()
            cursor.close()
            conn.close()

            logger.info(f"Saved new note for student {student_id}")
            return True

        except Exception as e:
            logger.error(f"Error saving note: {e}")
            return False