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

    return f"{inferred_year:04d}-{month_map[month]:02d}-{day:02d}"

def infer_year_from_syllabus(full_text: str) -> int:
    lines = [line.strip() for line in full_text.splitlines() if line.strip()]
    top_text = "\n".join(lines[:50])

    # Look for explicit years like 2025, 2026, 2027
    years = re.findall(r"\b(20\d{2})\b", top_text)
    if years:
        return int(years[0])

    # Fallback: use current year if nothing is found
    from datetime import datetime
    return datetime.now().year

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


def extract_events_with_rules(text: str, inferred_year: int):
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    events = []

    for line in lines:
        line_lower = line.lower()

        has_event_word = any(word in line_lower for word in [
            "exam", "midterm", "final", "quiz", "project",
            "assignment", "homework", "hw", "due", "presentation"
        ])
        parsed_date = parse_date_from_line(line,inferred_year)

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

def extract_course_title(full_text: str):
    lines = [line.strip() for line in full_text.splitlines() if line.strip()]

    patterns = [
        r"^[A-Z]{2,4}\s*\d{3,4}[:\-]?\s+.+$",       
        r"^[A-Z]{2,4}\s*\d{3,4}\.\d+[:\-]?\s+.+$",   
        r"^Course Title[:\s]+(.+)$",                
        r"^Course[:\s]+(.+)$"
    ]

    for line in lines[:30]:
        for pattern in patterns:
            match = re.match(pattern, line, re.IGNORECASE)
            if match:
                if match.groups():
                    return match.group(1).strip()
                return line.strip()

    return None

def extract_schedule_from_pdf(pdf_path):
    full_text = extract_text_from_pdf(pdf_path)

    # Extract course title from the full syllabus text
    course_title = extract_course_title(full_text)
    inferred_year = infer_year_from_syllabus(full_text)

    cleaned_text = narrow_to_schedule_section(full_text)

    # 1) Try AI first
    try:
        ai_events = extract_events_with_llm(cleaned_text)
        if ai_events:
            return {
                "events": ai_events,
                "source": "ai",
                "course_title": course_title
            }
    except Exception as e:
        print(f"AI extraction failed: {e}")

    # 2) Fallback to rules if AI fails or returns nothing
    rule_events = extract_events_with_rules(cleaned_text, inferred_year)
    if rule_events:
        return {
            "events": rule_events,
            "source": "rules_fallback",
            "course_title": course_title
        }

    # 3) Final empty result
    return {
        "events": [],
        "source": "none",
        "course_title": course_title
    }