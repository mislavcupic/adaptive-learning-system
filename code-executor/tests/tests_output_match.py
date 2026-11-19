import pytest

from app.executor import CodeExecutor


@pytest.fixture
def executor(tmp_path, monkeypatch):
    monkeypatch.setattr(CodeExecutor, "TEMP_DIR", str(tmp_path))
    return CodeExecutor()


@pytest.mark.parametrize("actual, expected", [
    ("6", "6"),
    ("6.000000", "6"),
    ("6.00", "6"),
    ("Povrsina je 6.00", "6"),
    ("upisite a i b stranice: 6", "6"),
    ("1.732051", "1.73"),
    ("g(1) = 21", "21"),
    ("Prosti brojevi od 1 do 100:\n2 3 5 7", "2 3 5 7"),
    ("Dobili ste odličan!", "Dobili ste odličan!"),
    ("Unesi broj: Dobili ste  odličan!", "Dobili ste odličan!"),
    ("DULJINA STRANICE MORA BITI VECA OD 0.", "Duljina stranice mora biti veca od 0."),
    ("6,5", "6.5"),
    ("Broj je paran", "paran"),
    ("PARAN", "paran"),
    ("Paran je!", "paran"),
    ("Broj 7 je neparan.", "neparan"),
    ("Broj nije paran", "neparan|nije paran"),
    ("7 je neparan broj", "neparan|nije paran"),
])
def test_accepts_equivalent_output(executor, actual, expected):
    assert executor._outputs_match(actual, expected)


@pytest.mark.parametrize("actual, expected", [
    ("15", "5"),
    ("550.00", "550.50"),
    ("6", "15"),
    ("", "6"),
    ("2 3 5", "2 3 5 7"),
    ("Dobili ste vrlo dobar!", "Dobili ste dobar!"),
    ("bilo sto", ""),
    ("nema broja", "6"),
    ("Broj je neparan", "paran"),
    ("Broj je paran", "neparan|nije paran"),
    ("bilo sto", "|"),
])
def test_rejects_different_output(executor, actual, expected):
    assert not executor._outputs_match(actual, expected)