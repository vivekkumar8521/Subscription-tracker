# AI Subscription Detection Service

Python FastAPI service for detecting recurring subscriptions from transaction data.

## Setup

```bash
cd ai-service
pip install -r requirements.txt
python main.py
```

Service runs on `http://localhost:8000`

## Endpoints

- `POST /detect-subscriptions` - Analyze transactions and detect subscriptions
- `GET /health` - Health check
