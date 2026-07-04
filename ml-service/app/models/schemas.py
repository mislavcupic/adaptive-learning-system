"""
Pydantic modeli za ML servis.
"""

from enum import Enum
from typing import Optional
from pydantic import BaseModel


class ResearchGroup(str, Enum):
    NOT_ASSIGNED = "NOT_ASSIGNED"
    EXPERIMENTAL = "EXPERIMENTAL"
    CONTROL = "CONTROL"


class FeedbackRequest(BaseModel):
    submission_id: str
    student_id: str
    task_id: str
    task_title: str
    language_type: str
    submitted_code: str
    compiler_output: Optional[str] = None
    execution_output: Optional[str] = None
    test_results: Optional[str] = None
    tests_passed: int = 0
    tests_total: int = 0
    research_group: ResearchGroup = ResearchGroup.NOT_ASSIGNED


class FeedbackResponse(BaseModel):
    submission_id: str
    ai_feedback: str
    ai_score: int
    skills_updated: list[str] = []


class HealthResponse(BaseModel):
    status: str
    service: str
    openai_configured: bool
    database_connected: bool


class BKTUpdateRequest(BaseModel):
    student_id: str
    skill_name: str
    is_correct: bool


class BKTResponse(BaseModel):
    student_id: str
    skill_name: str
    mastery_level: float
    previous_level: float