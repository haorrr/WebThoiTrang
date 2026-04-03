# Phase 01 — Python FastAPI Microservice

## New directory: `toxic-detector/`

```
toxic-detector/
├── Dockerfile
├── requirements.txt
├── main.py
└── model_loader.py
```

---

## `requirements.txt`

```
fastapi==0.111.0
uvicorn[standard]==0.29.0
transformers==4.41.2
py_vncorenlp==0.1.4
```

> torch installed separately in Dockerfile (CPU wheel)

---

## `model_loader.py` — singleton, loads once at startup

```python
import py_vncorenlp
from transformers import pipeline

_rdrseg = None
_clf = None

def get_classifier():
    global _rdrseg, _clf
    if _clf is None:
        _rdrseg = py_vncorenlp.VnCoreNLP(
            annotators=["wseg"], save_dir="/app/vncorenlp"
        )
        _clf = pipeline(
            "text-classification",
            model="jesse-tong/vietnamese_hate_speech_detection_phobert",
            tokenizer="vinai/phobert-base-v2",
            device=-1,          # CPU
            truncation=True,
            max_length=256,
        )
    return _rdrseg, _clf
```

---

## `main.py`

```python
from fastapi import FastAPI
from pydantic import BaseModel
from model_loader import get_classifier

app = FastAPI(title="Toxic Detector")

class Req(BaseModel):
    text: str

@app.on_event("startup")
async def preload():
    get_classifier()   # warm up model on container start

@app.post("/classify")
def classify(req: Req):
    seg, clf = get_classifier()
    # VNCoreNLP word segmentation (required for PhoBERT accuracy)
    segmented = " ".join(w for sent in seg.word_segment(req.text) for w in sent)
    r = clf(segmented)[0]
    label, score = r["label"], float(r["score"])
    # Labels vary by model — safe labels: CLEAN, NORMAL, LABEL_0
    is_hate = label.upper() not in ("CLEAN", "NORMAL", "LABEL_0")
    return {
        "toxic": is_hate,
        "score": score if is_hate else 1.0 - score,
        "label": label,
    }

@app.get("/health")
def health():
    return {"status": "ok"}
```

---

## `Dockerfile`

```dockerfile
FROM python:3.11-slim
WORKDIR /app

# Java required for VNCoreNLP .jar
RUN apt-get update && apt-get install -y default-jre-headless curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

# Install CPU-only torch (~600MB vs 2.8GB GPU build)
RUN pip install --no-cache-dir torch==2.3.0+cpu \
      --index-url https://download.pytorch.org/whl/cpu \
 && pip install --no-cache-dir -r requirements.txt

COPY . .

# Pre-download model weights — baked into image layer (no runtime download)
RUN python -c "from model_loader import get_classifier; get_classifier()"

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

---

## `docker-compose.yml` additions

```yaml
  toxic-detector:
    build: ./toxic-detector
    container_name: fashionshop-toxic-detector
    restart: unless-stopped
    ports:
      - "8000:8000"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 90s    # model load ~45-90s on CPU VPS
    networks:
      - fashionshop-net    # same network as Spring Boot
```

> Spring Boot container should declare `depends_on: toxic-detector` with `condition: service_started` (not service_healthy — rely on graceful degradation instead of hard dep).

---

## Notes

- **Image size**: ~2.5GB (model 550MB + torch CPU 600MB + base ~200MB)
- **First build time**: 10–20 min (downloads model weights)
- **Subsequent builds**: fast (Docker layer caching)
- **label mapping**: after first deploy, test with:
  ```bash
  curl -X POST http://localhost:8000/classify \
       -H "Content-Type: application/json" \
       -d '{"text":"sản phẩm tốt lắm"}'
  ```
  Verify label strings (CLEAN / OFFENSIVE / HATE) and adjust `is_hate` check in `main.py` if needed.
- **If RAM is tight** (<3GB total VPS): use `low_cpu_mem_usage=True` in pipeline call or consider `funa21/phobert-finetuned-victsd` as alternative (similar size)
