# Vietnamese Toxic Detection - Research Summary

**Status:** Research Complete
**Date:** 2026-03-31
**Project:** Fashion E-Commerce Review Moderation (WebThoiTrang)

---

## Quick Answer

**Best approach for your e-commerce site:**

1. **Immediate MVP (Week 1):** Deploy existing checkpoint from HuggingFace
   - Model: `jesse-tong/vietnamese_hate_speech_detection_phobert`
   - Alternative: `funa21/phobert-finetuned-victsd`
   - Setup time: 5 minutes
   - Expected accuracy: 75-85% F1

2. **Production (Weeks 2-4):** Add FastAPI + VNCoreNLP segmentation
   - Containerized microservice
   - <150ms latency per review
   - ~1-2 GB memory on CPU

3. **Optional Enhancement (Week 4+):** Fine-tune on domain data
   - Collect 2k-5k labeled fashion reviews
   - Expected improvement: +5-10% F1
   - Effort: 2-3 weeks

---

## Key Repositories

### Production-Ready (Start Here)

| Repo | Purpose | Status |
|---|---|---|
| [tarudesu/ViCTSD](https://github.com/tarudesu/ViCTSD) | Constructive & toxic detection | ⭐ Official reference |
| [nhattan040102/Vietnamese-Hate-and-Offensive-Detection](https://github.com/nhattan040102/Vietnamese-Hate-and-Offensive-Detection-using-PhoBERT-CNN-and-Social-Media-Streaming-Data) | PhoBERT-CNN hybrid (F1: 67%) | ⭐ Production-ready |
| [hoangcaobao/Vietnamese-Toxic-Comment-Classifier](https://github.com/hoangcaobao/Vietnamese-Toxic-Comment-Classifier) | PyTorch classifier | ⭐ Clean implementation |

### Latest Research

| Repo | Status | Key Result |
|---|---|---|
| [tarudesu/ViHateT5](https://github.com/tarudesu/ViHateT5) | ACL 2024 Findings | SOTA alternative (T5-based) |
| [phusroyal/ViHOS](https://github.com/phusroyal/ViHOS) | EACL 2023 | Span-level detection |

---

## HuggingFace Models - Ready to Use

### Recommended (Use These)

| Model | Task | F1 Score | Link |
|---|---|---|---|
| **jesse-tong/vietnamese_hate_speech_detection_phobert** | Binary toxic detection | 75-80% | [🔗](https://huggingface.co/jesse-tong/vietnamese_hate_speech_detection_phobert) |
| **funa21/phobert-finetuned-victsd** | Constructive/toxic (2-class) | 78.59% | [🔗](https://huggingface.co/funa21/phobert-finetuned-victsd) |
| **tarudesu/ViHateT5-base** | Multi-class (3-class) SOTA | 75-90% | [🔗](https://huggingface.co/tarudesu/ViHateT5-base) |

### Base Models (Fine-Tune Yourself)

| Model | Params | Size | Link |
|---|---|---|---|
| **vinai/phobert-base-v2** | 135M | 420 MB | [🔗](https://huggingface.co/vinai/phobert-base-v2) |
| vinai/phobert-large | 370M | 1.2 GB | [🔗](https://huggingface.co/vinai/phobert-large) |

---

## Datasets for Training/Evaluation

| Dataset | Size | Classes | Access |
|---|---|---|---|
| **ViHSD** | 33k+ comments | CLEAN, OFFENSIVE, HATE | [🔗](https://huggingface.co/datasets/uitnlp/vihsd) |
| **UIT-ViCTSD** | 10k comments | Constructive, Toxic | [🔗](https://github.com/tarudesu/ViCTSD) |
| **ViHOS** | 11k comments | Hate/Offensive spans | [🔗](https://github.com/phusroyal/ViHOS) |
| **UIT-VSFC** | 16k sentences | Sentiment, Topic | [🔗](https://nlp.uit.edu.vn/datasets) |

**Recommended for your use case:** ViHSD (largest, most social media comments like reviews)

---

## Accuracy Benchmarks (What to Expect)

```
Model: PhoBERT on standard Vietnamese datasets

Binary Classification (Toxic vs. Clean):
  - Baseline (jesse-tong checkpoint): 75-80% F1
  - Fine-tuned on domain: 82-88% F1
  - With CNN enhancement: 85-95% F1

3-Class Classification (CLEAN/OFFENSIVE/HATE):
  - PhoBERT-CNN: 67.46% macro-F1 (ViHSD)
  - ViHateT5: 75-90% macro-F1
  - Fine-tuned: 78-85% macro-F1

Special Tasks:
  - Span-level detection (ViHOS): 0.837 macro-F1
  - Constructive speech (UIT-ViCTSD): 78.59% F1
```

**For fashion reviews:** Expect 75-85% F1 with baseline, 82-90% with fine-tuning.

---

## Resource Requirements

### Hardware Specs

**Minimal (CPU only, <1k reviews/day):**
- RAM: 1.5-2 GB
- CPU: Any modern processor
- Latency: 100-150ms per review
- Cost: $0 (existing machine)

**Production (GPU, 1k-10k reviews/day):**
- VRAM: 2-4 GB (RTX 3080, RTX 4070)
- RAM: 4-8 GB
- Latency: 20-50ms per review
- Throughput: 50-100 requests/sec
- Cost: $5-20/month

**Scale (10k+ reviews/day):**
- Multi-GPU setup or inference service
- Cost: $50-200/month

### Memory Breakdown

```
PhoBERT-base model:
  - Model weights (FP32): 420 MB
  - Model weights (FP16): 210 MB
  - Model weights (INT8): 100-120 MB ← Recommended
  - Activations (batch=1): 50-100 MB
  - Total (production): ~600-800 MB

FastAPI service (with VNCoreNLP):
  - Dependencies: ~300-500 MB
  - JVM (VnCoreNLP): ~500 MB
  - Total system: ~1.5-2 GB RAM
```

---

## Critical Implementation Detail: Word Segmentation

**PhoBERT requires Vietnamese word segmentation before use.**

```python
# ❌ WRONG - Will underperform
text = "Sản phẩm tồi"
tokenizer.encode(text)

# ✅ CORRECT - Use VNCoreNLP
from vncorenlp import VnCoreNLP
annotator = VnCoreNLP("/path/to/VnCoreNLP-1.0.jar", annotators=["wseg"])
segmented = annotator.tokenize(text)[0]
# Output: "Sản_phẩm tồi"
tokenizer.encode(segmented)
```

**Setup cost:** 2 minutes (download + 1 pip install)

---

## Implementation Roadmap

### Phase 1: MVP (Days 1-3)
```
✓ Deploy jesse-tong checkpoint to FastAPI
✓ Test on 100 sample reviews
✓ Measure latency & accuracy
Effort: 5-8 hours
```

### Phase 2: Production Ready (Days 4-7)
```
✓ Add VNCoreNLP word segmentation
✓ Docker containerization
✓ Error handling & logging
✓ Basic monitoring
Effort: 16-20 hours
```

### Phase 3: Optimization (Week 2)
```
✓ Enable 8-bit quantization (4x memory reduction)
✓ Batch processing for throughput
✓ Redis caching for identical reviews
Effort: 8-12 hours
```

### Phase 4: Custom Fine-Tuning (Week 3-4, Optional)
```
✓ Collect 2k-5k labeled fashion reviews
✓ Fine-tune vinai/phobert-base-v2
✓ A/B test against baseline
✓ Deploy winner
Effort: 2-3 weeks (mostly annotation time)
```

---

## Cost Analysis

### Option 1: Baseline (jesse-tong checkpoint)
- Model: Free
- Compute (CPU): Free-$5/month (t3.medium)
- Setup effort: 5 hours
- Expected accuracy: 75-85% F1
- **Total cost:** $0-5/month + 5 hours

### Option 2: Production + Fine-Tuning
- Model: Free
- Annotation (2k-5k reviews): $1k-3k
- Compute: $50-100/month
- Setup effort: 60-80 hours
- Expected accuracy: 85-92% F1
- **Total cost:** $1.5k-3.5k + 60 hours

---

## Next Steps for Your Project

### Immediate (Today)
1. Read: `/docs/vietnamese-toxic-detection-research.md` (comprehensive)
2. Read: `/docs/toxic-detection-quickstart.md` (implementation guide)
3. Try: Copy 20-line FastAPI example and test with 1 model

### This Week
1. Pick model (jesse-tong or funa21)
2. Create basic FastAPI microservice
3. Test on 50-100 real review samples from your DB
4. Measure accuracy and false positive rate

### Next Week
1. Add VNCoreNLP segmentation
2. Dockerize the service
3. Deploy to staging
4. Monitor performance

### Decision Point (Week 2)
- **If accuracy ≥80% F1:** Go to production
- **If accuracy <80% F1:** Start fine-tuning on domain data

---

## Key Resources

### GitHub
- [ViCTSD](https://github.com/tarudesu/ViCTSD) - Official reference
- [PhoBERT-CNN](https://github.com/nhattan040102/Vietnamese-Hate-and-Offensive-Detection-using-PhoBERT-CNN-and-Social-Media-Streaming-Data)
- [ViHateT5](https://github.com/tarudesu/ViHateT5)
- [ViHOS](https://github.com/phusroyal/ViHOS)

### HuggingFace Models
- [jesse-tong/vietnamese_hate_speech_detection_phobert](https://huggingface.co/jesse-tong/vietnamese_hate_speech_detection_phobert)
- [funa21/phobert-finetuned-victsd](https://huggingface.co/funa21/phobert-finetuned-victsd)
- [vinai/phobert-base-v2](https://huggingface.co/vinai/phobert-base-v2)

### Papers
- [PhoBERT (EMNLP 2020)](https://arxiv.org/abs/2003.00744)
- [Vietnamese Hate Detection (2022)](https://arxiv.org/abs/2206.00524)
- [ViHOS (EACL 2023)](https://aclanthology.org/2023.eacl-main.47/)
- [ViHateT5 (ACL 2024)](https://arxiv.org/abs/2405.14141)

### Datasets
- [ViHSD](https://huggingface.co/datasets/uitnlp/vihsd) - 33k comments
- [UIT NLP Group](https://nlp.uit.edu.vn/datasets) - All UIT datasets

---

## Bottom Line

**For a fashion e-commerce site needing Vietnamese review moderation:**

1. **Best tool:** PhoBERT (proven, 135M params, efficient)
2. **Quickest path:** Use `jesse-tong/vietnamese_hate_speech_detection_phobert` (5 min setup)
3. **Expected performance:** 75-85% F1 (baseline), 85-92% F1 (with fine-tuning)
4. **Deployment:** FastAPI + VNCoreNLP segmentation (containerized)
5. **Cost:** $0-5/month (baseline) or $50-100/month with GPU + fine-tuning
6. **Effort:** 5-8 hours MVP, 60-80 hours production with custom tuning

**Start with MVP this week, evaluate accuracy, then decide on fine-tuning.**

---

## Documents

Full research documents created in `/docs/`:
- `vietnamese-toxic-detection-research.md` - Detailed 10-section research report
- `toxic-detection-quickstart.md` - Implementation guide with code examples

