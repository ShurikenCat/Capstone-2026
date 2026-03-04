from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
import tempfile, os

from syllabus_extractor import extract_schedule_from_pdf

app = FastAPI()

# Local Testing Purposes
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
def health():
    return {"Status": "ok"}

@app.post("/extract")
async def extract(file: UploadFile = File(...)):
    #Save uploaded pdf to a temp path
    suffix = os.path.splitext(file.filename)[1] or ".pdf"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name

    try:
        events =extract_schedule_from_pdf(tmp_path)

        #convert datetime objects to ISO strings for JSON
        for e in events:
            if "date" in e and e["date"] is not None:
                e["date"] = e["date"].isoformat()

            return{"count": len(events), "events": events}
    finally:
        os.remove(tmp_path)