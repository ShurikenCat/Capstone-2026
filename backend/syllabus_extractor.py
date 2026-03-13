import re
import pdfplumber

MONTHS = r"(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)"
YEAR = 2026  # set this manually for now, or infer from syllabus header


def normalize_event_type(text: str) -> str:
    t = text.lower()
    if any(x in t for x in ["midterm", "final", "exam", "test"]):
        return "exam"
    if "quiz" in t:
        return "quiz"
    if "project" in t or "presentation" in t:
        return "project"
    if any(x in t for x in ["assignment", "homework", "hw", "due"]):
        return "assignment"
    return "class"


def parse_date_from_line(line: str):
    # Example matches:
    # (Thu Feb 15)
    # Thu Feb 15
    # Mar 28
    m = re.search(rf"(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)?\s*({MONTHS})\s+(\d{{1,2}})", line)
    if not m:
        return None

    month = m.group(1)
    day = int(m.group(2))

    month_map = {
        "Jan": 1, "Feb": 2, "Mar": 3, "Apr": 4,
        "May": 5, "Jun": 6, "Jul": 7, "Aug": 8,
        "Sep": 9, "Oct": 10, "Nov": 11, "Dec": 12
    }

    return f"{YEAR:04d}-{month_map[month]:02d}-{day:02d}"


def extract_schedule_from_pdf(pdf_path):
    full_text = ""

    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            full_text += "\n" + (page.extract_text() or "")

    # Narrow to schedule section
    lower = full_text.lower()
    start = lower.find("course schedule")
    if start != -1:
        text = full_text[start:]
    else:
        text = full_text

    # Stop before policy-heavy sections if present
    end_markers = [
        "attendance",
        "academic integrity",
        "disability",
        "accommodations",
        "student support",
        "statement on"
    ]

    lowered_text = text.lower()
    end_positions = [lowered_text.find(marker) for marker in end_markers if lowered_text.find(marker) != -1]
    if end_positions:
        text = text[:min(end_positions)]

    lines = [line.strip() for line in text.splitlines() if line.strip()]

    events = []

    for line in lines:
        line_lower = line.lower()

        # Require the line to look like a real schedule item
        has_event_word = any(word in line_lower for word in [
            "exam", "midterm", "final", "quiz", "project", "assignment", "homework", "due"
        ])
        has_date = parse_date_from_line(line) is not None

        if not (has_event_word and has_date):
            continue

        events.append({
            "date": parse_date_from_line(line),
            "type": normalize_event_type(line),
            "description": line,
            "assignment": None
        })

    # dedupe
    seen = set()
    unique_events = []
    for e in events:
        key = (e["date"], e["type"], e["description"])
        if key not in seen:
            seen.add(key)
            unique_events.append(e)

    return unique_events