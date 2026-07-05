"""
Feedback Service - Glavni servis za generiranje personaliziranog feedbacka.
"""

import logging
from openai import OpenAI

from ..config import get_settings
from ..models.schemas import FeedbackRequest, FeedbackResponse, ResearchGroup
from .bkt_service import BKTService
from .rag_service import RAGService

logger = logging.getLogger(__name__)


class FeedbackService:
    """Glavni servis za generiranje AI feedbacka."""

    SYSTEM_PROMPT = """Ti si prijateljski asistent za učenje programiranja. 
Tvoj zadatak je dati konstruktivan, personaliziran feedback studentu na temelju njegovog koda.

Pravila:
1. Budi ohrabrujući ali iskren
2. Fokusiraj se na konkretne greške i kako ih ispraviti
3. Ako ima prethodnih bilješki o studentu, uzmi ih u obzir
4. Koristi jednostavan jezik prilagođen početnicima
5. Daj konkretne primjere kada je moguće
6. Odgovaraj na hrvatskom jeziku
7. Ako postoje prethodne bilješke, usporedi trenutni rad s njima — eksplicitno istakni napredak ili ponavljajuće greške (npr. "ovo je sličan problem kao prošli put..." ili "vidim da si od prošlog puta savladao...")

Format odgovora:
1. Kratki pregled (što je dobro)
2. Identificirane greške (ako ih ima)
3. Konkretni savjeti za poboljšanje
4. Ohrabrenje za dalje
"""

    INSIGHT_PROMPT = """Na temelju studentovog koda i danog feedbacka, napiši JEDNU sažetu analitičku bilješku (max 2 rečenice) koja bilježi:
- konkretne greške ili obrasce grešaka (npr. "off-by-one u granicama petlje", "ne provjerava raspon unosa")
- koncepte s kojima se student muči ili ih dobro vlada

Bilješka služi kao dugoročna memorija za praćenje napretka — piši je tako da bude korisna za usporedbu s budućim predajama. NE piši samo status "riješeno/nije riješeno". Fokusiraj se na SADRŽAJ učenja. Odgovori na hrvatskom, bez uvoda i bez markdown formatiranja."""

    def __init__(self):
        self.settings = get_settings()
        self.client = OpenAI(api_key=self.settings.openai_api_key) if self.settings.openai_api_key else None
        self.bkt_service = BKTService()
        self.rag_service = RAGService()

    def generate_feedback(self, request: FeedbackRequest) -> FeedbackResponse:
        """
        Generira feedback na temelju research grupe studenta.
        """
        if request.research_group == ResearchGroup.CONTROL:
            return self._generate_control_feedback(request)
        else:
            return self._generate_experimental_feedback(request)

    def _generate_control_feedback(self, request: FeedbackRequest) -> FeedbackResponse:
        """Generira feedback za kontrolnu grupu - samo score, bez AI feedbacka."""

        # Izračunaj score isto kao za EXPERIMENTAL
        score = self._calculate_score(request)

        logger.info(f"Generated CONTROL feedback for submission {request.submission_id}")

        return FeedbackResponse(
            submission_id=request.submission_id,
            ai_feedback="",  # Prazan feedback za CONTROL
            ai_score=score,
            skills_updated=[]
        )

    def _generate_experimental_feedback(self, request: FeedbackRequest) -> FeedbackResponse:
        """Generira personalizirani AI feedback za eksperimentalnu grupu."""

        if not self.client:
            logger.error("OpenAI client not configured!")
            return self._generate_control_feedback(request)

        # 1. Dohvati prethodne bilješke (RAG)
        context_query = f"Greške u {request.task_title}: {request.compiler_output or ''} {request.execution_output or ''}"
        previous_notes = self.rag_service.get_relevant_notes(
            student_id=request.student_id,
            query=context_query[:500],
            limit=3
        )

        # 2. Pripremi kontekst za LLM
        context = self._build_context(request, previous_notes)

        # 3. Generiraj feedback pomoću GPT-4o-mini
        try:
            response = self.client.chat.completions.create(
                model=self.settings.openai_model,
                messages=[
                    {"role": "system", "content": self.SYSTEM_PROMPT},
                    {"role": "user", "content": context}
                ],
                max_tokens=1000,
                temperature=0.7
            )

            ai_feedback = response.choices[0].message.content

        except Exception as e:
            logger.error(f"OpenAI API error: {e}")
            return self._generate_control_feedback(request)

        # 4. Izračunaj score
        score = self._calculate_score(request)

        # 5. Spremi novu bilješku (Agentic Memory)
        insight = self._generate_insight(request, ai_feedback)
        self.rag_service.save_note(
            student_id=request.student_id,
            insight=insight,
            submission_id=request.submission_id
        )

        # 6. Identificiraj skills za BKT update
        skills = self._identify_skills(request)

        logger.info(f"Generated EXPERIMENTAL feedback for submission {request.submission_id}")

        return FeedbackResponse(
            submission_id=request.submission_id,
            ai_feedback=ai_feedback,
            ai_score=score,
            skills_updated=skills
        )

    def _build_context(self, request: FeedbackRequest, previous_notes: list[dict]) -> str:
        """Gradi kontekst za LLM prompt."""

        context = f"""## Informacije o zadatku
Naziv: {request.task_title}
Programski jezik: {request.language_type}

## Kod studenta
```{request.language_type.lower()}
{request.submitted_code}
```

## Rezultati testova
Prolaznost: {request.tests_passed}/{request.tests_total}
"""

        if request.compiler_output:
            context += f"\n## Output kompajlera\n```\n{request.compiler_output}\n```\n"

        if request.execution_output:
            context += f"\n## Output izvršavanja\n```\n{request.execution_output}\n```\n"

        if request.test_results:
            context += f"\n## Detalji testova\n{request.test_results}\n"

        if previous_notes:
            context += "\n## Prethodne bilješke o ovom studentu (kronološki, najnovije prvo)\n"
            for note in previous_notes:
                created = note.get('created_at', '')
                context += f"- ({created}) {note.get('insight', '')}\n"

        context += "\n## Tvoj zadatak\nNapiši personalizirani feedback za ovog studenta na hrvatskom jeziku. Ako postoje prethodne bilješke, usporedi trenutni rad s njima i istakni napredak ili ponavljajuće greške."

        return context

    def _calculate_score(self, request: FeedbackRequest) -> int:
        """Izračunava score na temelju rezultata."""

        if request.tests_total == 0:
            return 0

        test_score = (request.tests_passed / request.tests_total) * 70
        compile_bonus = 15 if not (request.compiler_output and "error" in request.compiler_output.lower()) else 0
        structure_bonus = 15 if request.tests_passed > 0 else 0

        total = int(test_score + compile_bonus + structure_bonus)
        return min(100, max(0, total))

    def _generate_insight(self, request: FeedbackRequest, ai_feedback: str) -> str:
        """Generira analitičku bilješku za Agentic Memory pomoću LLM-a."""

        if not self.client:
            return self._fallback_insight(request)

        context = f"""Zadatak: {request.task_title}
Rezultat testova: {request.tests_passed}/{request.tests_total}

Kod studenta:
```{request.language_type.lower()}
{request.submitted_code}
```

Generirani feedback:
{ai_feedback}
"""

        try:
            response = self.client.chat.completions.create(
                model=self.settings.openai_model,
                messages=[
                    {"role": "system", "content": self.INSIGHT_PROMPT},
                    {"role": "user", "content": context}
                ],
                max_tokens=150,
                temperature=0.3
            )
            insight = response.choices[0].message.content.strip()
            # Prefiksiraj zadatkom radi konteksta pri retrievalu
            return f"[{request.task_title}] {insight}"[:500]

        except Exception as e:
            logger.error(f"Error generating insight: {e}")
            return self._fallback_insight(request)

    def _fallback_insight(self, request: FeedbackRequest) -> str:
        """Rezervna bilješka ako LLM nije dostupan."""
        success = request.tests_passed == request.tests_total
        status = "uspješno" if success else f"djelomično ({request.tests_passed}/{request.tests_total})"
        return f"[{request.task_title}] Riješeno {status}."[:500]

    def _identify_skills(self, request: FeedbackRequest) -> list[str]:
        """Identificira skills koje treba ažurirati u BKT."""

        skills = []
        code_lower = request.submitted_code.lower()

        if "for" in code_lower or "while" in code_lower:
            skills.append("loops")
        if "if" in code_lower:
            skills.append("conditionals")
        if "int " in code_lower or "float " in code_lower or "char " in code_lower:
            skills.append("variables")
        if "printf" in code_lower or "scanf" in code_lower:
            skills.append("io_operations")
        if "*" in code_lower and ("malloc" in code_lower or "int *" in code_lower):
            skills.append("pointers")
        if "malloc" in code_lower or "free" in code_lower:
            skills.append("memory_management")

        return skills
