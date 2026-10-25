"""
Feedback Service - Glavni servis za generiranje personaliziranog feedbacka.
"""

import logging
import re
from openai import OpenAI

from ..config import get_settings
from ..models.schemas import FeedbackRequest, FeedbackResponse, ResearchGroup
from .bkt_service import BKTService
from .rag_service import RAGService
from .mastery_repository import MasteryRepository

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
5. Daj konkretne primjere kada je moguće, ALI NIKAD gotova rješenja (vidi pravilo 8)
6. Odgovaraj na hrvatskom jeziku
7. Ako postoje prethodne bilješke, usporedi trenutni rad s njima — eksplicitno istakni napredak ili ponavljajuće greške (npr. "ovo je sličan problem kao prošli put..." ili "vidim da si od prošlog puta savladao...")

8. NAJVAŽNIJE PRAVILO — NE DAJEŠ GOTOVA RJEŠENJA:
   - NIKAD ne piši ispravljeni ili gotovi kod koji student može samo prekopirati.
   - NIKAD ne napiši točan izraz, točnu liniju koda ni cijelu funkciju koja rješava zadatak umjesto studenta.
   - Umjesto rješenja, VODI studenta: postavi pitanje koje ga navodi na uvid ("Što misliš da se dogodi kada je i jednako n?"), uputi ga na koncept koji treba proučiti, ili opiši GDJE je problem i ZAŠTO nastaje — ali NE i točan ispravak.
   - Smiješ pokazati opći princip ili sintaksu na NEUTRALNOM, nepovezanom primjeru (npr. kako općenito radi petlja), ali ne na studentovom konkretnom zadatku.
   - Cilj je da student sam dođe do rješenja. Ako bi tvoj odgovor omogućio studentu da prekopira ispravak bez razmišljanja, preformuliraj ga u smjernicu ili pitanje.

9. PRILAGODI RAZINU OBJAŠNJENJA PROCJENI USVOJENOSTI (BKT):
   - Procjena ispod 0.4: student je početnik u toj vještini. Objasni osnovni koncept korak po korak, jednostavnim riječima i na neutralnom primjeru.
   - Procjena 0.4 do 0.8: student djelomično vlada vještinom. Usmjeri ga izravno na konkretnu grešku i postavi pitanje koje ga vodi do uvida.
   - Procjena iznad 0.8: student dobro vlada vještinom. Budi sažet, a ako je rješenje točno, potakni ga na rubne slučajeve ili bolji stil.
   - Procjenu ne navodi kao broj u odgovoru; koristi je samo za odabir razine objašnjenja.

10. MEMORIJSKE GREŠKE (VALGRIND):
   - Ako kontekst sadrži Valgrind izvještaj s greškama (curenje memorije, čitanje ili pisanje izvan granica polja, korištenje neinicijalizirane vrijednosti, pristup oslobođenoj memoriji, rušenje programa), objasni studentu jednostavnim riječima što ta vrsta greške znači i zašto je važna, i kada su svi testovi prošli.
   - I ovdje vrijedi pravilo 8: opiši GDJE i ZAŠTO, ali ne piši ispravljeni kod.
   - Ne prepisuj sirovi izvještaj i ne navodi memorijske adrese ni tehničke oznake iz njega.

Format odgovora:
1. Kratki pregled (što je dobro)
2. Identificirane greške (GDJE i ZAŠTO, bez gotovog ispravka)
3. Smjernice i pitanja koja navode na rješenje (bez gotovog koda)
4. Ohrabrenje za dalje
"""

    INSIGHT_PROMPT = """Na temelju studentovog koda i danog feedbacka, napiši JEDNU sažetu analitičku bilješku (max 2 rečenice) koja bilježi:
- konkretne greške ili obrasce grešaka (npr. "off-by-one u granicama petlje", "ne provjerava raspon unosa")
- koncepte s kojima se student muči ili ih dobro vlada

Bilješka služi kao dugoročna memorija za praćenje napretka — piši je tako da bude korisna za usporedbu s budućim predajama. NE piši samo status "riješeno/nije riješeno". Fokusiraj se na SADRŽAJ učenja. Odgovori na hrvatskom, bez uvoda i bez markdown formatiranja."""

    NO_MEMORY_ERRORS = "Nema memorijskih grešaka."

    # Vrste Valgrind grešaka opisane jednostavnim riječima, da ih model ne preskoči
    VALGRIND_KINDS = [
        (r"Invalid write", "pisanje izvan granica zauzete memorije"),
        (r"Invalid read", "čitanje izvan granica zauzete memorije"),
        (r"uninitialised", "korištenje neinicijalizirane vrijednosti (varijabla ili element niza bez početne vrijednosti)"),
        (r"are (definitely|indirectly|possibly) lost", "curenje memorije (zauzeta memorija nije oslobođena)"),
        (r"Invalid free", "neispravno oslobađanje memorije"),
        (r"free'd", "pristup već oslobođenoj memoriji"),
        (r"Process terminating with default action of signal", "program se srušio tijekom izvođenja"),
    ]

    def __init__(self):
        self.settings = get_settings()
        self.client = OpenAI(api_key=self.settings.openai_api_key) if self.settings.openai_api_key else None
        self.bkt_service = BKTService()
        self.rag_service = RAGService()
        self.mastery_repository = MasteryRepository()

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

        # 2. Procjena usvojenosti vještine prije ove predaje (BKT)
        mastery = self._get_mastery(request)

        # 3. Pripremi kontekst za LLM
        context = self._build_context(request, previous_notes, mastery)

        # 4. Generiraj feedback pomoću GPT-4o-mini
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

        # 5. Izračunaj score
        score = self._calculate_score(request)

        # 6. Spremi novu bilješku (Agentic Memory)
        # Predaja u trenutku ovog poziva jos nije potvrdena u bazi,
        # pa veza na submission_id ne bi prosla strani kljuc.
        insight = self._generate_insight(request, ai_feedback)
        self.rag_service.save_note(
            student_id=request.student_id,
            insight=insight,
            submission_id=None
        )

        # 7. Identificiraj skills
        skills = self._identify_skills(request)

        logger.info(f"Generated EXPERIMENTAL feedback for submission {request.submission_id}")

        return FeedbackResponse(
            submission_id=request.submission_id,
            ai_feedback=ai_feedback,
            ai_score=score,
            skills_updated=skills
        )

    def _get_mastery(self, request: FeedbackRequest) -> dict | None:
        """Trenutna BKT procjena za vjestinu zadatka (stanje prije ove predaje)."""
        if not request.skill_name:
            return None

        row = self.mastery_repository.get_mastery(request.student_id, request.skill_name)

        if row:
            level = float(row["mastery_level"])
            return {
                "skill_name": request.skill_name,
                "level": level,
                "interpretation": self.bkt_service.interpret_mastery(level),
                "attempts": row["attempts_count"],
                "correct": row["correct_count"],
                "initial": False,
            }

        return {
            "skill_name": request.skill_name,
            "level": self.bkt_service.get_initial_mastery(),
            "interpretation": "",
            "attempts": 0,
            "correct": 0,
            "initial": True,
        }

    def _build_context(self, request: FeedbackRequest, previous_notes: list[dict], mastery: dict | None = None) -> str:
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

        valgrind = self._summarize_valgrind(request.valgrind_output)
        if valgrind:
            context += f"\n## Valgrind izvještaj (provjera memorije, ulaz prvog testa)\n```\n{valgrind}\n```\n"
            if valgrind != self.NO_MEMORY_ERRORS:
                context += "Valgrind je pronašao memorijske greške: obavezno ih objasni studentu (pravilo 10), i kada su svi testovi prošli.\n"

        if previous_notes:
            context += "\n## Prethodne bilješke o ovom studentu (kronološki, najnovije prvo)\n"
            for note in previous_notes:
                created = note.get('created_at', '')
                context += f"- ({created}) {note.get('insight', '')}\n"

        if mastery:
            context += "\n## Procjena usvojenosti vještine (BKT)\n"
            context += f"Vještina: {mastery['skill_name']}\n"
            if mastery['initial']:
                context += f"Procjena: {mastery['level']:.2f} (početna procjena, student još nema pokušaja za ovu vještinu)\n"
            else:
                context += (f"Procjena: {mastery['level']:.2f} ({mastery['interpretation']}), "
                            f"pokušaja: {mastery['attempts']}, točnih: {mastery['correct']}\n")

        context += "\n## Tvoj zadatak\nNapiši personalizirani feedback za ovog studenta na hrvatskom jeziku. Ako postoje prethodne bilješke, usporedi trenutni rad s njima i istakni napredak ili ponavljajuće greške. Razinu objašnjenja prilagodi procjeni usvojenosti vještine. VAŽNO: ne daj gotovo rješenje ni ispravljeni kod — vodi studenta smjernicama i pitanjima da sam dođe do rješenja."

        return context

    def _summarize_valgrind(self, raw: str | None) -> str | None:
        """
        Skraćuje Valgrind izvještaj prije slanja modelu.

        Uklanja oznaku procesa (==1234==) i uvodno zaglavlje. Ako nema
        grešaka, vraća kratku poruku umjesto cijelog izvještaja.
        """
        if not raw or raw.startswith("Valgrind error"):
            return None

        if re.search(r"ERROR SUMMARY: 0 errors", raw):
            return self.NO_MEMORY_ERRORS

        skip = ("Memcheck, a memory error detector", "Copyright (C)", "Using Valgrind-",
                "Command:", "For lists of detected", "For counts of detected", "rerun with")
        lines = []
        for line in raw.splitlines():
            line = re.sub(r"^\s*==\d+==\s?", "", line).rstrip()
            if not line or line.startswith(skip):
                continue
            lines.append(line)

        if not lines:
            return None

        kinds = []
        for pattern, label in self.VALGRIND_KINDS:
            if re.search(pattern, raw) and label not in kinds:
                kinds.append(label)

        total = re.search(r"ERROR SUMMARY: (\d+) errors", raw)
        summary = "Valgrind je pronašao memorijske greške"
        summary += f" (ukupno {total.group(1)})" if total else ""
        summary += ":\n" + "\n".join(f"- {k}" for k in kinds) if kinds else "."

        return summary + "\n\nIzvještaj:\n" + "\n".join(lines)[:1500]

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

        valgrind = self._summarize_valgrind(request.valgrind_output)
        if valgrind and valgrind != self.NO_MEMORY_ERRORS:
            context += f"\nValgrind izvještaj:\n{valgrind}\n"

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