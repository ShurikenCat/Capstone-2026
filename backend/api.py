from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
import tempfile
import os

from syllabus_extractor import extract_schedule_from_pdf

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def root():
    return {"message": "Ordo backend is running"}

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/extract")
async def extract(file: UploadFile = File(...)):
    with tempfile.NamedTemporaryFile(delete=False, suffix=".pdf") as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name

    try:
        result = extract_schedule_from_pdf(tmp_path)

        events = result.get("events", [])
        source = result.get("source", "unknown")
        course_title = result.get("course_title")

        for event in events:
            if "date" in event and event["date"] is not None:
                event["date"] = str(event["date"])

        return {
            "count": len(events),
            "events": events,
            "source": source,
            "course_title": course_title
        }

    finally:
        os.remove(tmp_path)