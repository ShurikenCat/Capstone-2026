from syllabus_extractor import normalize_event_type

def test_exam():
    assert normalize_event_type("Midterm Exam") == "exam"

def test_quiz():
    assert normalize_event_type("Quiz 1") == "quiz"

def test_assignment():
    assert normalize_event_type("Homework due") == "assignment"