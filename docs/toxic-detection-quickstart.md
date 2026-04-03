# Vietnamese Toxic Detection - Quick Start Implementation

## 1. Instant Setup (5 minutes)

### Option A: Using Existing Checkpoint (Recommended for MVP)

```python
# install_dependencies.sh
pip install transformers torch fastapi uvicorn pydantic python-dotenv

# main.py - FastAPI service
from fastapi import FastAPI
from transformers import pipeline
import torch

app = FastAPI()

# Load pre-trained model
classifier = pipeline(
    "text-classification",
    model="jesse-tong/vietnamese_hate_speech_detection_phobert",
    device=0 if torch.cuda.is_available() else -1
)

@app.post("/detect")
async def detect_toxic(text: str):
    """
    Returns: {"label": "TOXIC" or "CLEAN", "score": 0.0-1.0}
    """
    if not text or len(text) < 1:
        return {"error": "Text cannot be empty"}

    result = classifier(text[:512])
    return {
        "label": result[0]["label"],
        "score": round(result[0]["score"], 3),
        "text_length": len(text)
    }

# Run: uvicorn main:app --reload
```

### Option B: Alternative Model (ViHateT5 - SOTA)

```python
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch

tokenizer = AutoTokenizer.from_pretrained("tarudesu/ViHateT5-base")
model = AutoModelForSequenceClassification.from_pretrained("tarudesu/ViHateT5-base")

text = "Sản phẩm này tệ lắm"
inputs = tokenizer(text, return_tensors="pt", truncation=True)
outputs = model(**inputs)
prediction = torch.argmax(outputs.logits, dim=-1).item()
# 0: CLEAN, 1: OFFENSIVE, 2: HATE
```

---

## 2. With Word Segmentation (Recommended for Production)

```python
# requirements.txt
transformers==4.35.0
torch==2.0.0
fastapi==0.104.1
uvicorn==0.24.0
vncorenlp==1.0.3

# main.py
from fastapi import FastAPI, HTTPException
from transformers import pipeline
from vncorenlp import VnCoreNLP
import logging

app = FastAPI()
logger = logging.getLogger(__name__)

# Initialize segmenter (requires VnCoreNLP.jar)
try:
    annotator = VnCoreNLP(
        "path/to/VnCoreNLP-1.0.jar",
        annotators=["wseg"],
        max_heap_size="-Xmx500m"
    )
except Exception as e:
    logger.warning(f"VnCoreNLP not available: {e}. Using fallback segmentation.")
    annotator = None

# Load classifier
classifier = pipeline(
    "text-classification",
    model="jesse-tong/vietnamese_hate_speech_detection_phobert",
    device=0 if torch.cuda.is_available() else -1
)

def segment_text(text):
    """Word segmentation for Vietnamese"""
    if annotator:
        try:
            sentences = annotator.tokenize(text)
            return " ".join([" ".join(sent) for sent in sentences])
        except:
            return text
    return text

@app.post("/classify")
async def classify_review(text: str):
    """
    Vietnamese toxic comment detection
    Input: Vietnamese text (any length)
    Output: {"label": "TOXIC"/"CLEAN", "score": 0.0-1.0, "segmented_text": "..."}
    """
    try:
        # Clean input
        text = text.strip()
        if not text:
            raise HTTPException(status_code=400, detail="Text cannot be empty")

        # Word segment
        segmented = segment_text(text)

        # Classify (truncate to 512 tokens)
        result = classifier(segmented[:512])

        return {
            "label": result[0]["label"],
            "confidence": round(result[0]["score"], 3),
            "original_text": text,
            "segmented_text": segmented,
            "length": len(text)
        }
    except Exception as e:
        logger.error(f"Error: {str(e)}")
        raise HTTPException(status_code=500, detail="Classification failed")

# Test endpoint
@app.get("/health")
def health():
    return {"status": "ok"}

# Run: uvicorn main:app --reload
```

### Download VnCoreNLP

```bash
# Linux/Mac
wget https://github.com/undertheseanlp/VnCoreNLP/releases/download/v1.0/VnCoreNLP-1.0.jar

# Or clone and build
git clone https://github.com/undertheseanlp/VnCoreNLP.git
# Place VnCoreNLP-1.0.jar in your project root
```

---

## 3. Docker Deployment

```dockerfile
# Dockerfile
FROM python:3.9-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy VnCoreNLP
COPY VnCoreNLP-1.0.jar .

# Copy app
COPY main.py .

# Expose port
EXPOSE 8000

# Run
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

```bash
# Build and run
docker build -t vietnamese-toxic-detector .
docker run -p 8000:8000 vietnamese-toxic-detector
```

---

## 4. Testing & Validation

```python
# test_classifier.py
import requests
import json

API_URL = "http://localhost:8000"

test_cases = [
    ("Sản phẩm rất tốt, giao hàng nhanh", "CLEAN"),
    ("Sản phẩm tệ lắm, lừa đảo", "TOXIC"),
    ("Thằng ngu, bán hàng giả", "TOXIC"),
    ("Chất lượng ổn, giá hợp lý", "CLEAN"),
    ("Đồ cứt, thiếu mẹ nó", "TOXIC"),
]

for text, expected in test_cases:
    response = requests.post(
        f"{API_URL}/classify",
        json={"text": text}
    )
    result = response.json()

    label = result.get("label")
    confidence = result.get("confidence")
    status = "✓" if label == expected else "✗"

    print(f"{status} [{label}] {confidence:.3f} | {text}")
```

---

## 5. Performance Optimization

### Option A: Quantization (4x memory reduction)

```python
import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from transformers import BitsAndBytesConfig

# 8-bit quantization
quantization_config = BitsAndBytesConfig(
    load_in_8bit=True,
    bnb_8bit_compute_dtype=torch.float16
)

model = AutoModelForSequenceClassification.from_pretrained(
    "jesse-tong/vietnamese_hate_speech_detection_phobert",
    quantization_config=quantization_config
)

# Memory: 420 MB → 100-120 MB
# Speed: ~10% slower
# Accuracy: ~0.5% loss (acceptable)
```

### Option B: Batch Processing

```python
from transformers import pipeline
import torch

classifier = pipeline(
    "text-classification",
    model="jesse-tong/vietnamese_hate_speech_detection_phobert"
)

texts = [
    "Sản phẩm tốt",
    "Sản phẩm tệ",
    "Giao hàng nhanh"
]

# Batch classification (2-3x faster)
results = classifier(texts, batch_size=8)
for text, result in zip(texts, results):
    print(f"{text}: {result['label']} ({result['score']:.3f})")
```

### Option C: Caching

```python
from functools import lru_cache

@lru_cache(maxsize=1000)
def classify_cached(text: str):
    """Cache identical reviews"""
    return classifier(text[:512])

# After 1000 identical/similar reviews, hits 99%+ from cache
```

---

## 6. Integration with E-Commerce Backend

### Django Example

```python
# views.py
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods
import requests

TOXIC_DETECTOR_URL = "http://localhost:8000/classify"

@require_http_methods(["POST"])
def check_review(request):
    review_text = request.POST.get("review", "")

    response = requests.post(
        TOXIC_DETECTOR_URL,
        json={"text": review_text}
    )

    if response.status_code == 200:
        detection = response.json()
        is_toxic = detection["label"] == "TOXIC"

        # Save review
        Review.objects.create(
            product_id=request.POST.get("product_id"),
            user=request.user,
            text=review_text,
            is_toxic=is_toxic,
            toxicity_score=detection["confidence"]
        )

        if is_toxic:
            return JsonResponse({
                "status": "pending_moderation",
                "message": "Your review contains inappropriate content and requires moderation"
            })
        else:
            return JsonResponse({"status": "published"})

    return JsonResponse({"error": "Classification service error"}, status=500)
```

### Spring Boot Example

```java
// ReviewService.java
@Service
public class ReviewService {
    @Value("${toxic.detector.url:http://localhost:8000}")
    private String detectorUrl;

    public ToxicityResult checkToxicity(String reviewText) {
        RestTemplate rest = new RestTemplate();
        String url = detectorUrl + "/classify";

        HttpEntity<String> request = new HttpEntity<>(
            "{\"text\":\"" + reviewText.replace("\"", "\\\"") + "\"}"
        );

        ResponseEntity<ToxicityResult> response = rest.exchange(
            url, HttpMethod.POST, request, ToxicityResult.class
        );

        return response.getBody();
    }
}

// ReviewController.java
@PostMapping("/reviews")
public ResponseEntity<?> createReview(@RequestBody ReviewDTO dto) {
    ToxicityResult toxicity = reviewService.checkToxicity(dto.getText());

    Review review = new Review();
    review.setText(dto.getText());
    review.setToxic(toxicity.getLabel().equals("TOXIC"));
    review.setToxicityScore(toxicity.getConfidence());

    if (review.isToxic()) {
        review.setStatus(ReviewStatus.PENDING_MODERATION);
    } else {
        review.setStatus(ReviewStatus.PUBLISHED);
    }

    return ResponseEntity.ok(reviewRepository.save(review));
}
```

---

## 7. Monitoring & Logging

```python
# monitoring.py
import logging
from datetime import datetime
from pathlib import Path

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('toxic_detection.log'),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)

@app.post("/classify")
async def classify_review(text: str):
    """With monitoring"""
    start_time = datetime.now()

    try:
        segmented = segment_text(text)
        result = classifier(segmented[:512])

        latency_ms = (datetime.now() - start_time).total_seconds() * 1000
        logger.info(f"Classification: {result[0]['label']} ({latency_ms:.1f}ms)")

        return {
            "label": result[0]["label"],
            "confidence": round(result[0]["score"], 3),
            "latency_ms": round(latency_ms, 1)
        }
    except Exception as e:
        logger.error(f"Classification error: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Classification failed")
```

---

## 8. Model Comparison Matrix

| Model | Setup Time | Accuracy | Speed (CPU) | Memory | Flexibility |
|---|---|---|---|---|---|
| jesse-tong/hate_speech | **5 min** | **75-80%** | 100-150ms | **~1 GB** | Limited |
| funa21/phobert-finetuned-victsd | **5 min** | **78-85%** | 100-150ms | **~1 GB** | Limited |
| vinai/phobert-base-v2 (fine-tune) | **1-2 weeks** | **82-88%** | 100-150ms | **~1.5 GB** | **High** |
| tarudesu/ViHateT5-base | **5 min** | **75-90%** | 150-200ms | **~2 GB** | Medium |

**Recommendation for Fashion E-Commerce:**
- **MVP (Week 1):** Use `jesse-tong` or `funa21`
- **Production (Week 4):** Consider fine-tuning `vinai/phobert-base-v2` if needed

---

## Common Issues & Solutions

### Issue 1: "ModuleNotFoundError: No module named 'vncorenlp'"

```bash
pip install vncorenlp
# Download VnCoreNLP-1.0.jar
wget https://github.com/undertheseanlp/VnCoreNLP/releases/download/v1.0/VnCoreNLP-1.0.jar
```

### Issue 2: Slow inference (>500ms per request)

```python
# Solution: Use GPU or batch processing
# With GPU: 20-50ms
# With batch: 13-20 requests/sec instead of 7-10

import torch
device = "cuda" if torch.cuda.is_available() else "cpu"
model = model.to(device)
```

### Issue 3: Out of memory during inference

```python
# Solution: Enable quantization
from transformers import BitsAndBytesConfig
quantization_config = BitsAndBytesConfig(load_in_8bit=True)
model = AutoModelForSequenceClassification.from_pretrained(
    model_id, quantization_config=quantization_config
)
# Memory reduction: 420 MB → 100 MB
```

### Issue 4: Model underfits on fashion reviews

```python
# Solution: Fine-tune on domain data
# See main research doc section 3 for fine-tuning approach
```

---

## Next Steps

1. **Deploy MVP:** Use ready-made checkpoint (jesse-tong or funa21) → 5 minutes
2. **Test & Monitor:** Collect metrics on false positives/negatives → 1 week
3. **Fine-tune (Optional):** If accuracy <75%, fine-tune on 2k-5k labeled reviews → 2-3 weeks
4. **Scale:** Add caching, quantization, batch processing → ongoing

See `/docs/vietnamese-toxic-detection-research.md` for detailed analysis.
