import re
import pdfplumber

from llm_extractor import extract_events_with_llm

MONTHS = r"(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)"
YEAR = 2026  # you can improve this later by inferring from syllabus text


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


def dedupe_events(events):
    seen = set()
    unique_events = []

    for event in events:
        key = (
            event.get("date"),
            event.get("type"),
            (event.get("description") or "").strip().lower()
        )
        if key not in seen:
            seen.add(key)
            unique_events.append(event)

    return unique_events


def extract_text_from_pdf(pdf_path: str) -> str:
    full_text = ""

    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            full_text += "\n" + (page.extract_text() or "")

    return full_text


def narrow_to_schedule_section(full_text: str) -> str:
    lower = full_text.lower()
    schedule_markers = [
        "course schedule",
        "schedule",
        "calendar",
        "weekly schedule",
        "tentative schedule"
    ]

    start_positions = [lower.find(marker) for marker in schedule_markers if lower.find(marker) != -1]
    if start_positions:
        text = full_text[min(start_positions):]
    else:
        text = full_text

    end_markers = [
        "attendance",
        "academic integrity",
        "disability",
        "accommodations",
        "student support",
        "statement on",
        "university policies"
    ]

    lowered_text = text.lower()
    end_positions = [lowered_text.find(marker) for marker in end_markers if lowered_text.find(marker) != -1]
    if end_positions:
        text = text[:min(end_positions)]

    return text


def extract_events_with_rules(text: str):
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    events = []

    for line in lines:
        line_lower = line.lower()

        has_event_word = any(word in line_lower for word in [
            "exam", "midterm", "final", "quiz", "project",
            "assignment", "homework", "hw", "due", "presentation"
        ])
        parsed_date = parse_date_from_line(line)

        if not (has_event_word and parsed_date):
            continue

        events.append({
            "date": parsed_date,
            "type": normalize_event_type(line),
            "description": line,
            "assignment": None
        })

    events.sort(key=lambda e: e["date"])
    return dedupe_events(events)


def extract_schedule_from_pdf(pdf_path):
    full_text = extract_text_from_pdf(pdf_path)
    cleaned_text = narrow_to_schedule_section(full_text)

    # 1) Try rules first
    rule_events = extract_events_with_rules(cleaned_text)
    if len(rule_events) >= 3:
        return rule_events

    # 2) Fallback to AI if rules are weak
    try:
        ai_events = extract_events_with_llm(cleaned_text)
        if ai_events:
            return ai_events
    except Exception as e:
        print(f"LLM fallback failed: {e}")

    # 3) Final fallback: return whatever rules found
    return rule_events