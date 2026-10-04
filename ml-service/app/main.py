"""
Adaptive Learning ML Service

FastAPI aplikacija za:
- Generiranje personaliziranog AI feedbacka
- Bayesian Knowledge Tracing (BKT)
- RAG (Retrieval-Augmented Generation)
- ANCOVA statisticka analiza
"""

import logging
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from .config import get_settings
from .models.schemas import (
    FeedbackRequest,
    FeedbackResponse,
    HealthResponse,
    BKTUpdateRequest,
    BKTResponse,
    AncovaRequest,
    AncovaResponse
)
from .services import FeedbackService, BKTService
from .services.statistics_service import StatisticsService
from .services.mastery_repository import MasteryRepository

# Logging setup
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# App setup
settings = get_settings()
app = FastAPI(
    title=settings.app_name,
    description="ML servis za adaptivno učenje programiranja",
    version="1.0.0"
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Services
feedback_service = FeedbackService()
bkt_service = BKTService()
statistics_service = StatisticsService()
mastery_repository = MasteryRepository()


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Provjera zdravlja servisa."""

    openai_configured = bool(settings.openai_api_key)

    db_connected = False
    try:
        import psycopg2
        conn = psycopg2.connect(settings.database_url)
        conn.close()
        db_connected = True
    except Exception as e:
        logger.warning(f"Database connection failed: {e}")

    return HealthResponse(
        status="healthy" if openai_configured else "degraded",
        service=settings.app_name,
        openai_configured=openai_configured,
        database_connected=db_connected
    )


@app.post("/api/feedback", response_model=FeedbackResponse)
async def generate_feedback(request: FeedbackRequest):
    """
    Generira AI feedback za submission.
    """
    logger.info(f"Generating feedback for submission {request.submission_id}")

    try:
        response = feedback_service.generate_feedback(request)
        return response
    except Exception as e:
        logger.error(f"Error generating feedback: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/bkt/update", response_model=BKTResponse)
async def update_bkt(request: BKTUpdateRequest):
    """
    Azurira BKT procjenu znanja za studenta.

    Procjena se cita iz baze, azurira Bayesovom formulom i sprema natrag,
    pa se znanje akumulira kroz niz predaja umjesto da svaki poziv krece
    od pocetne vrijednosti. Bez tog koraka BKT ne bi pratio napredak,
    sto je njegova osnovna svrha.
    """
    logger.info(
        f"BKT update: student {request.student_id}, skill {request.skill_name}, "
        f"correct={request.is_correct}"
    )

    try:
        # Prethodna procjena; ako je nema, koristi se prior P(L0)
        existing = mastery_repository.get_mastery(
            request.student_id, request.skill_name
        )
        current_mastery = (
            float(existing["mastery_level"]) if existing
            else bkt_service.get_initial_mastery()
        )

        new_mastery = bkt_service.update_mastery(
            current_mastery=current_mastery,
            is_correct=request.is_correct
        )

        mastery_repository.save_mastery(
            student_id=request.student_id,
            skill_name=request.skill_name,
            mastery_level=new_mastery,
            is_correct=request.is_correct,
            p_guess=bkt_service.p_guess,
            p_slip=bkt_service.p_slip,
            p_transit=bkt_service.p_transit
        )

        return BKTResponse(
            student_id=request.student_id,
            skill_name=request.skill_name,
            mastery_level=new_mastery,
            previous_level=current_mastery
        )

    except Exception as e:
        logger.error(f"BKT update error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/bkt/student/{student_id}")
async def get_student_mastery(student_id: str):
    """
    Sve procjene znanja za jednog studenta.

    Koristi se za prikaz napretka nastavniku i studentu.
    """
    logger.info(f"Fetching mastery profile for student {student_id}")

    try:
        records = mastery_repository.get_all_for_student(student_id)

        return {
            "student_id": student_id,
            "skills": [
                {
                    "skill_name": r["skill_name"],
                    "mastery_level": float(r["mastery_level"]),
                    "interpretation": bkt_service.interpret_mastery(
                        float(r["mastery_level"])
                    ),
                    "attempts_count": r["attempts_count"],
                    "correct_count": r["correct_count"],
                    "updated_at": r["updated_at"].isoformat() if r["updated_at"] else None
                }
                for r in records
            ]
        }
    except Exception as e:
        logger.error(f"Mastery fetch error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/statistics/ancova", response_model=AncovaResponse)
async def run_ancova(request: AncovaRequest):
    """
    Provodi ANCOVA analizu (posttest ~ group + pretest kovarijat).
    Vraca deskriptivnu statistiku po grupama + F, p, partial eta².
    """
    logger.info(f"Running ANCOVA on {len(request.records)} records")
    try:
        records = [r.model_dump() for r in request.records]
        result = statistics_service.run_ancova(records)
        return AncovaResponse(**result)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"ANCOVA error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": settings.app_name,
        "version": "1.0.0",
        "docs": "/docs"
    }