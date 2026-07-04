"""
Adaptive Learning ML Service

FastAPI aplikacija za:
- Generiranje personaliziranog AI feedbacka
- Bayesian Knowledge Tracing (BKT)
- RAG (Retrieval-Augmented Generation)
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
    BKTResponse
)
from .services import FeedbackService, BKTService

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
    Ažurira BKT procjenu znanja za studenta.
    """
    logger.info(f"Updating BKT for student {request.student_id}, skill {request.skill_name}")

    current_mastery = bkt_service.get_initial_mastery()

    new_mastery = bkt_service.update_mastery(
        current_mastery=current_mastery,
        is_correct=request.is_correct
    )

    return BKTResponse(
        student_id=request.student_id,
        skill_name=request.skill_name,
        mastery_level=new_mastery,
        previous_level=current_mastery
    )


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": settings.app_name,
        "version": "1.0.0",
        "docs": "/docs"
    }