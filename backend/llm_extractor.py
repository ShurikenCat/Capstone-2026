import os
import json
import re
from groq import Groq

ALLOWED_EVENT_TYPES = {"exam", "quiz", "assignment", "project", "class"}

def build_prompt(cleaned_text: str) -> str:
    return f"""
You are extracting calendar events from a university course syllabus.

Return ONLY valid JSON in this exact format:

{{
  "events": [
    {{
      "date": "YYYY-MM-DD",
      "type": "exam|quiz|assignment|project|class",
      "description": "short event description",
      "assignment": null
    }}
  ]
}}

Rules:
- Only include events that have a clearly stated date.
- Do not invent dates.
- Ignore grading policies, attendance, accommodations, academic integrity, and general course policies.
- If something is ambiguous, skip it.
- description should be short but useful.
- assignment can be null if not applicable.
- Return JSON only, with no markdown fences.

Syllabus text:
{cleaned_text}
""".strip()


def extract_json_object(text: str) -> dict:
    text = (text or "").strip()

    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    text = re.sub(r"^```json\s*", "", text, flags=re.IGNORECASE)
    text = re.sub(r"^```\s*", "", text)
    text = re.sub(r"\s*```$", "", text)

    start = text.find("{")
    end = text.rfind("}")
    if start == -1 or end == -1 or end <= start:
        raise ValueError("No JSON object found in model response.")

    return json.loads(text[start:end + 1])


def normalize_event(event: dict) -> dict | None:
    if not isinstance(event, dict):
        return None

    date = str(event.get("date", "")).strip()
    event_type = str(event.get("type", "class")).strip().lower()
    description = str(event.get("description", "")).strip()
    assignment = event.get("assignment")

    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", date):
        return None

    if event_type not in ALLOWED_EVENT_TYPES:
        event_type = "class"

    if not description:
        return None

    if assignment is not None:
        assignment = str(assignment).strip() or None

    return {
        "date": date,
        "type": event_type,
        "description": description,
        "assignment": assignment
    }


def dedupe_events(events: list[dict]) -> list[dict]:
    seen = set()
    unique = []

    for event in events:
        key = (
            event["date"],
            event["type"],
            event["description"].strip().lower()
        )
        if key not in seen:
            seen.add(key)
            unique.append(event)

    return unique


def extract_events_with_llm(cleaned_text: str) -> list[dict]:
    api_key = os.getenv("GROQ_API_KEY")
    if not api_key:
        raise ValueError("GROQ_API_KEY environment variable is not set.")

    client = Groq(api_key=api_key)

    prompt = build_prompt(cleaned_text)

    completion = client.chat.completions.create(
        model="llama-3.3-70b-versatile",
        messages=[
            {
                "role": "system",
                "content": "You extract structured syllabus events and return JSON only."
            },
            {
                "role": "user",
                "content": prompt
            }
        ],
        temperature=0,
        response_format={"type": "json_object"}
    )

    content = completion.choices[0].message.content
    payload = extract_json_object(content)

    raw_events = payload.get("events", [])
    if not isinstance(raw_events, list):
        return []

    cleaned_events = []
    for event in raw_events:
        normalized = normalize_event(event)
        if normalized:
            cleaned_events.append(normalized)

    cleaned_events.sort(key=lambda e: e["date"])
    return dedupe_events(cleaned_events)