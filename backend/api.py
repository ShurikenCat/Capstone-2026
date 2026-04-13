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

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/extract")
async def extract(file: UploadFile = File(...)):
    with tempfile.NamedTemporaryFile(delete=False, suffix=".pdf") as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name

    try:
        events = extract_schedule_from_pdf(tmp_path)

        if events is None:
            return {
                "count": 0,
                "events": [],
                "warning": "extract_schedule_from_pdf returned None"
            }

        for event in events:
            if "date" in event and event["date"] is not None:
                event["date"] = str(event["date"])

        return {
            "count": len(events),
            "events": events
        }

    finally:
        os.remove(tmp_path)