import os
from groq import Groq
import base64
import time
from pdf2image import convert_from_path
from pathlib import Path

from icalendar import Calendar, Event
from icalendar.prop import vRecur
from datetime import datetime
import json
import re

#Gets pdfs and turns them into images
def convert_all_pdfs_to_images(image_format="png"):
    pdf_folder = Path("/data/data/com.example.capstone2026/files")
    output_root = Path("syllabi_images")
    output_root.mkdir(exist_ok=True)

    for pdf_path in sorted(pdf_folder.glob("*.pdf")):
        images = convert_from_path(str(pdf_path), dpi=200)
        pdf_output_folder = output_root / pdf_path.stem
        pdf_output_folder.mkdir(exist_ok=True)

        for i, image in enumerate(images, start=1):
            image.save(pdf_output_folder / f"page_{i}.{image_format.upper()}", image_format.upper())

#PDF Parser. Probably best to get your own API key as this is tuned for ONE user per key
def process_all_syllabus_images(
    image_root="syllabi_images",
    output_root="groq_results",
    model="meta-llama/llama-4-scout-17b-16e-instruct",
    sleep_seconds=10
):
    api_key = os.getenv("GROQ_API_KEY")
    if not api_key:
        raise ValueError("GROQ_API_KEY environment variable is not set.")
    client = Groq(api_key=api_key)
    image_root = Path(image_root)
    output_root = Path(output_root)

    output_root.mkdir(parents=True, exist_ok=True)

    #Gets compiled images
    image_paths = sorted(
        list(image_root.rglob("*.png")) +
        list(image_root.rglob("*.jpg")) +
        list(image_root.rglob("*.jpeg"))
    )

    for image_path in image_paths:
        relative_path = image_path.relative_to(image_root)
        result_path = output_root / relative_path.with_suffix(".json")
        result_path.parent.mkdir(parents=True, exist_ok=True)

        if result_path.exists():
            continue

        with open(image_path, "rb") as f:
            encoded = base64.b64encode(f.read()).decode("utf-8")

        mime_type = "image/png" if image_path.suffix.lower() == ".png" else "image/jpeg"

        completion = client.chat.completions.create(
            model=model,
            messages=[
                {
                    "role": "system",
                    "content": "Extract text and key syllabus fields accurately. Return concise JSON only."
                },
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "text",
                            "text": (
                                "Extract this syllabus page into JSON with keys: "
                                "course_title, instructor, meeting_times, "
                                "assignments, page_summary."
                            )
                        },
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:{mime_type};base64,{encoded}"
                            }
                        }
                    ]
                }
            ],
            response_format={"type": "json_object"},
            temperature=0,
            max_completion_tokens=2048
        )

        content = completion.choices[0].message.content

        with open(result_path, "w", encoding="utf-8") as f:
            f.write(content)

        time.sleep(sleep_seconds)

#Removes the markdown that the AI returns even when explicitly telling JSON
def extract_json_object(text: str) -> dict:
    text = (text or "").strip()

    #I never noticed any issue but we do this anyways
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    text = re.sub(r"^```json\s*", "", text, flags=re.IGNORECASE)
    text = re.sub(r"^```\s*", "", text)
    text = re.sub(r"\s*```$", "", text)

    start = text.find("{")
    end = text.rfind("}")

    #Same as above
    if start == -1 or end == -1 or end <= start:
        raise ValueError("No JSON object found in model response.")

    return json.loads(text[start:end + 1])

#AI assisted section. Not really sure what it's for
def get_choice_text(response) -> str:
    def normalize_content(content):
        if content is None:
            return ""
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts = []
            for item in content:
                if isinstance(item, str):
                    parts.append(item)
                elif isinstance(item, dict):
                    if "text" in item and item["text"] is not None:
                        parts.append(str(item["text"]))
                    elif "content" in item and item["content"] is not None:
                        parts.append(str(item["content"]))
                else:
                    parts.append(str(item))
            return "".join(parts)
        return str(content)

    # Normal object-style SDK response
    try:
        choices = response.choices
        if choices:
            first = choices[0]

            if hasattr(first, "message"):
                msg = first.message
                if hasattr(msg, "content"):
                    text = normalize_content(msg.content)
                    if text:
                        return text

            if hasattr(first, "delta"):
                delta = first.delta
                if hasattr(delta, "content"):
                    text = normalize_content(delta.content)
                    if text:
                        return text
                if hasattr(delta, "reasoning_content"):
                    text = normalize_content(delta.reasoning_content)
                    if text:
                        return text

            if hasattr(first, "content"):
                text = normalize_content(first.content)
                if text:
                    return text
    except Exception:
        pass

    # Dict-style response
    try:
        if isinstance(response, dict):
            choices = response.get("choices", [])
            if choices:
                first = choices[0]

                if isinstance(first, dict):
                    if "message" in first and isinstance(first["message"], dict):
                        text = normalize_content(first["message"].get("content"))
                        if text:
                            return text

                    if "delta" in first and isinstance(first["delta"], dict):
                        text = normalize_content(first["delta"].get("content"))
                        if text:
                            return text
                        text = normalize_content(first["delta"].get("reasoning_content"))
                        if text:
                            return text

                    if "content" in first:
                        text = normalize_content(first["content"])
                        if text:
                            return text
    except Exception:
        pass

    # Pydantic-style dump if available
    try:
        dumped = response.model_dump()
        if isinstance(dumped, dict):
            return get_choice_text(dumped)
    except Exception:
        pass

    raise ValueError(
        f"Could not extract message content from Groq response. "
        f"Response type: {type(response)} | Response repr: {repr(response)[:1000]}"
    )


def validate_syllabus_event_payload(data: dict) -> dict:
    if not isinstance(data, dict):
        raise ValueError("Top-level response is not an object.")

    events = data.get("events")
    if not isinstance(events, list):
        raise ValueError("Missing 'events' list.")

    cleaned = []
    for item in events:
        if not isinstance(item, dict):
            continue

        event_kind = str(item.get("event_kind", "")).strip().lower()
        if event_kind not in {"class", "assignment"}:
            continue

        categories = item.get("categories", [])
        if not isinstance(categories, list):
            categories = []

        cleaned.append({
            "uid": str(item.get("uid") or uuid.uuid4()),
            "event_kind": event_kind,
            "summary": str(item.get("summary", "Untitled Event")),
            "description": str(item.get("description", "")),
            "location": str(item.get("location", "")),
            "dtstart": str(item.get("dtstart", "")),
            "dtend": str(item.get("dtend", "")),
            "rrule": str(item.get("rrule", "")),
            "categories": [str(x) for x in categories]
        })

    return {"events": cleaned}


def syllabus_json_to_event_json(
    syllabus_json_file,
    output_json_file=None,
    model="qwen/qwen3-32b",
    max_retries=3,
    retry_sleep=2.0
):
    syllabus_json_file = Path(syllabus_json_file)

    if not syllabus_json_file.exists():
        raise FileNotFoundError(f"Missing syllabus JSON file: {syllabus_json_file}")
    if not syllabus_json_file.is_file():
        raise FileNotFoundError(f"Not a file: {syllabus_json_file}")

    if output_json_file is None:
        output_json_file = syllabus_json_file.with_name(f"{syllabus_json_file.stem}_calendar.json")
    output_json_file = Path(output_json_file)

    with open(syllabus_json_file, "r", encoding="utf-8") as f:
        syllabus_data = json.load(f)

    prompt = {
        "rules": [
            "Convert this syllabus JSON into ICS-style VEVENT JSON objects.",
            "Return exactly one JSON object with top-level key events.",
            "You must include BOTH recurring class meeting events and assignment or deadline events whenever present.",
            "A class event is the actual lecture, seminar, discussion, or lab meeting time.",
            "An assignment event is a due date, exam, quiz, project, homework, or other deadline.",
            "Each event must have only these keys: uid, event_kind, summary, description, location, dtstart, dtend, rrule, categories.",
            "event_kind must be one of: class, assignment.",
            "Format dtstart and dtend as YYYY-MM-DDTHH:MM:SS.",
            "For classes, preserve the listed meeting times exactly.",
            "For recurring classes, use an RRULE string like RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR.",
            "For recurring classes, dtstart must be the first real class occurrence.",
            "Assignments should default to 23:59:00 unless an exact due time is explicitly stated.",
            "For assignments, create a one-minute event ending one minute after dtstart.",
            "If an event does not repeat, set rrule to an empty string.",
            "If an event has no categories, use an empty array.",
            "Return only JSON and nothing else."
        ],
        "syllabus_object": syllabus_data
    }

    client = Groq(api_key="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")

    parsed = None
    last_error = None

    for attempt in range(max_retries):
        try:
            response = client.chat.completions.create(
                model=model,
                messages=[
                    {
                        "role": "system",
                        "content": (
                            "You are a calendar-to-JSON converter. "
                            "Be concise. Do not think step-by-step. "
                            "Return exactly one valid JSON object and nothing else. "
                            "No markdown, no commentary, no explanation. "
                            "You must include both class meeting events and assignment events when present. "
                            "Output format: "
                            "{"
                            "\"events\":["
                            "{"
                            "\"uid\":\"string\","
                            "\"event_kind\":\"class|assignment\","
                            "\"summary\":\"string\","
                            "\"description\":\"string\","
                            "\"location\":\"string\","
                            "\"dtstart\":\"YYYY-MM-DDTHH:MM:SS\","
                            "\"dtend\":\"YYYY-MM-DDTHH:MM:SS\","
                            "\"rrule\":\"RRULE:FREQ=WEEKLY;BYDAY=MO,WE\" or \"\","
                            "\"categories\":[\"string\"]"
                            "}"
                            "]"
                            "}"
                        )
                    },
                    {
                        "role": "user",
                        "content": json.dumps(prompt)
                    }
                ],
                temperature=0,
                reasoning_format="hidden",
                reasoning_effort="none",
                max_completion_tokens=4096,
                stream=False
            )

            raw_content = get_choice_text(response)
            parsed = validate_syllabus_event_payload(extract_json_object(raw_content))
            break
        except Exception as e:
            last_error = e
            if attempt < max_retries - 1:
                time.sleep(retry_sleep)

    if parsed is None:
        raise RuntimeError(f"Failed to process {syllabus_json_file.name}: {last_error}")

    output_json_file.parent.mkdir(parents=True, exist_ok=True)
    with open(output_json_file, "w", encoding="utf-8") as f:
        json.dump(parsed, f, ensure_ascii=False, indent=2)

    return {
        "output_json_file": str(output_json_file),
        "class_event_count": sum(1 for e in parsed["events"] if e["event_kind"] == "class"),
        "assignment_event_count": sum(1 for e in parsed["events"] if e["event_kind"] == "assignment")
    }


def build_user_event_item(item: dict) -> dict | None:
    if not isinstance(item, dict):
        return None

    start_date = item.get("start_date")
    end_date = item.get("end_date", start_date)
    start_time = item.get("start_time", "00:00:00")
    end_time = item.get("end_time", "00:00:00")

    if not start_date:
        return None

    event = {
        "uid": str(uuid.uuid4()),
        "summary": str(item.get("title", "User Event")),
        "description": str(item.get("description", "")),
        "location": str(item.get("location", "")),
        "dtstart": f"{start_date}T{start_time}",
        "dtend": f"{end_date}T{end_time}",
        "rrule": "",
        "categories": ["INFLEXIBLE"] if item.get("inflexible") else []
    }

    if item.get("repeat") and isinstance(item.get("repeat_days"), list):
        weekday_map = {
            "monday": "MO",
            "tuesday": "TU",
            "wednesday": "WE",
            "thursday": "TH",
            "friday": "FR",
            "saturday": "SA",
            "sunday": "SU"
        }
        bydays = [
            weekday_map[d.strip().lower()]
            for d in item["repeat_days"]
            if isinstance(d, str) and d.strip().lower() in weekday_map
        ]

        if bydays:
            rule = f"RRULE:FREQ=WEEKLY;BYDAY={','.join(bydays)}"
            repeat_until = item.get("repeat_until")
            if repeat_until:
                rule += f";UNTIL={repeat_until.replace('-', '')}T235959"
            event["rrule"] = rule

    return event


def add_event_to_calendar(cal: Calendar, item: dict):
    dtstart = item.get("dtstart", "")
    dtend = item.get("dtend", "")

    if not dtstart or not dtend:
        return

    try:
        start = datetime.fromisoformat(dtstart)
        end = datetime.fromisoformat(dtend)
    except ValueError:
        return

    event = Event()
    event.add("uid", str(item.get("uid") or uuid.uuid4()))
    event.add("summary", item.get("summary", "Untitled Event"))
    event.add("description", item.get("description", ""))

    if item.get("location"):
        event.add("location", item["location"])

    event.add("dtstart", start)
    event.add("dtend", end)

    rrule_text = str(item.get("rrule", "")).strip()
    if rrule_text:
        if rrule_text.upper().startswith("RRULE:"):
            rrule_text = rrule_text[6:]
        try:
            event["rrule"] = vRecur.from_ical(rrule_text)
        except Exception:
            pass

    categories = item.get("categories", [])
    if isinstance(categories, list) and categories:
        event.add("categories", categories)

    cal.add_component(event)


def merge_event_jsons_to_ics(
    event_json_folder="calendar_json",
    user_events_file=None,
    output_ics="final_schedule.ics"
):
    event_json_folder = Path(event_json_folder)
    output_ics = Path(output_ics)

    if not event_json_folder.exists():
        raise FileNotFoundError(f"Missing event JSON folder: {event_json_folder}")
    if not event_json_folder.is_dir():
        raise NotADirectoryError(f"Not a folder: {event_json_folder}")

    cal = Calendar()
    cal.add("prodid", "-//Schedule Builder//mxm.dk//")
    cal.add("version", "2.0")

    merged_count = 0

    for json_file in sorted(event_json_folder.rglob("*.json")):
        with open(json_file, "r", encoding="utf-8") as f:
            data = json.load(f)

        for item in data.get("events", []):
            add_event_to_calendar(cal, item)
            merged_count += 1

    user_event_count = 0
    if user_events_file is not None:
        user_events_path = Path(user_events_file)
        if user_events_path.exists() and user_events_path.is_file():
            with open(user_events_path, "r", encoding="utf-8") as f:
                user_events = json.load(f)

            if isinstance(user_events, list):
                for item in user_events:
                    event_item = build_user_event_item(item)
                    if event_item is not None:
                        add_event_to_calendar(cal, event_item)
                        user_event_count += 1

    output_ics.parent.mkdir(parents=True, exist_ok=True)
    with open(output_ics, "wb") as f:
        f.write(cal.to_ical())

    return {
        "ics_path": str(output_ics),
        "merged_model_events": merged_count,
        "merged_user_events": user_event_count
    }


def process_all_syllabi_then_merge(
    syllabus_json_folder="groq_results",
    calendar_json_folder="calendar_json",
    user_events_file=None,
    output_ics="final_schedule.ics",
    sleep_seconds=3
):
    syllabus_json_folder = Path(syllabus_json_folder)
    calendar_json_folder = Path(calendar_json_folder)

    if not syllabus_json_folder.exists():
        raise FileNotFoundError(f"Missing syllabus JSON folder: {syllabus_json_folder}")
    if not syllabus_json_folder.is_dir():
        raise NotADirectoryError(f"Not a folder: {syllabus_json_folder}")

    calendar_json_folder.mkdir(parents=True, exist_ok=True)

    results = []

    for syllabus_json_file in sorted(syllabus_json_folder.rglob("*.json")):
        relative = syllabus_json_file.relative_to(syllabus_json_folder)
        output_json_file = calendar_json_folder / relative.parent / f"{relative.stem}_calendar.json"

        result = syllabus_json_to_event_json(
            syllabus_json_file=syllabus_json_file,
            output_json_file=output_json_file
        )
        results.append(result)
        time.sleep(sleep_seconds)

    merge_result = merge_event_jsons_to_ics(
        event_json_folder=calendar_json_folder,
        user_events_file=user_events_file,
        output_ics=output_ics
    )

    return {
        "ics_path": merge_result["ics_path"],
        "processed_syllabi": len(results),
        "total_class_events": sum(r["class_event_count"] for r in results),
        "total_assignment_events": sum(r["assignment_event_count"] for r in results),
        "merged_user_events": merge_result["merged_user_events"]
    }


"""PDFs -> Images -> Encoded Images -> AI Data Extractor -> AI Schedule Generator -> ICS"""
#Important note: I couldn't figure out how to clean out the folders between potentially different generations
#THAT WILL cause issues, but as long as you just delete all the files (specifically in groq_results, syllabi_images, and calendar_json)
#should be fine. Any irrelevant pdfs in syllabi_pdfs should also be deleted.

#All of these will likely need to be modified to be the actual string containing the path
convert_all_pdfs_to_images()

process_all_syllabus_images()

#User_events_file is a filepath. Defaults to None (aka no events submitted). Output ics is just a name that is outputted. Output file dir can probably be changed.
process_all_syllabi_then_merge(
        syllabus_json_folder="groq_results",
        calendar_json_folder="calendar_json",
        user_events_file=None,
        output_ics="final_schedule.ics",
        sleep_seconds=3
    )