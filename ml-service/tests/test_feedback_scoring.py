from types import SimpleNamespace

from app.models.schemas import FeedbackRequest
from app.services.feedback_service import FeedbackService


class FakeCompletions:
    def __init__(self, reply=None, error=None):
        self.reply = reply
        self.error = error
        self.calls = 0

    def create(self, **kwargs):
        self.calls += 1
        if self.error:
            raise self.error
        message = SimpleNamespace(content=self.reply)
        return SimpleNamespace(choices=[SimpleNamespace(message=message)])


def make_service(reply=None, error=None, with_client=True):
    service = FeedbackService.__new__(FeedbackService)
    service.settings = SimpleNamespace(openai_model="gpt-4o-mini")
    completions = FakeCompletions(reply, error)
    service.client = SimpleNamespace(chat=SimpleNamespace(completions=completions)) if with_client else None
    return service, completions


def make_request(**overrides):
    data = dict(
        submission_id="s1",
        student_id="u1",
        task_id="t1",
        task_title="Veći od dva broja",
        task_text="Učitaj dva broja i ispiši veći.",
        language_type="C",
        submitted_code="int main(){return 0;}",
        compiler_output="Kompilacija uspješna.",
        tests_passed=0,
        tests_total=0,
    )
    data.update(overrides)
    return FeedbackRequest(**data)


def test_score_from_tests_when_tests_exist():
    service, completions = make_service("100")
    assert service._calculate_score(make_request(tests_passed=4, tests_total=4)) == 100
    assert completions.calls == 0


def test_score_from_code_when_no_tests():
    service, completions = make_service("85")
    assert service._calculate_score(make_request()) == 85
    assert completions.calls == 1


def test_score_from_code_is_clamped():
    service, _ = make_service("Ocjena: 250")
    assert service._calculate_score(make_request()) == 100


def test_score_from_code_without_number_is_zero():
    service, _ = make_service("ne znam")
    assert service._calculate_score(make_request()) == 0


def test_score_from_code_with_compile_error_is_zero():
    service, completions = make_service("90")
    request = make_request(compiler_output="main.c:3: error: expected ';'")
    assert service._calculate_score(request) == 0
    assert completions.calls == 0


def test_score_from_code_without_client_is_zero():
    service, _ = make_service(with_client=False)
    assert service._calculate_score(make_request()) == 0


def test_score_from_code_survives_api_error():
    service, _ = make_service(error=RuntimeError("API nedostupan"))
    assert service._calculate_score(make_request()) == 0


def test_context_includes_task_text_and_no_tests_note():
    service, _ = make_service()
    service._summarize_valgrind = lambda raw: None
    context = service._build_context(make_request(), [], None)
    assert "Učitaj dva broja i ispiši veći." in context
    assert FeedbackService.NO_TESTS_NOTE in context
    assert "Prolaznost: 0/0" not in context


def test_context_shows_pass_rate_when_tests_exist():
    service, _ = make_service()
    service._summarize_valgrind = lambda raw: None
    context = service._build_context(make_request(tests_passed=3, tests_total=4, task_text=None), [], None)
    assert "Prolaznost: 3/4" in context
    assert "Tekst zadatka" not in context
