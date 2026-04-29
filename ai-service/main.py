from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Optional
from datetime import datetime, timedelta
from collections import defaultdict
import re

app = FastAPI(title="Subscription Detection AI Service")

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class Transaction(BaseModel):
    merchantName: str
    amount: str
    date: str
    description: str

class DetectionRequest(BaseModel):
    transactions: List[Transaction]

class DetectedSubscription(BaseModel):
    name: str
    amount: float
    billingCycle: str
    nextRenewalDate: str
    confidence: float

class DetectionResponse(BaseModel):
    subscriptions: List[DetectedSubscription]

def detect_recurring_patterns(transactions: List[Transaction]) -> List[DetectedSubscription]:
    """
    AI-powered subscription detection using pattern analysis:
    1. Group transactions by merchant
    2. Analyze frequency patterns
    3. Detect recurring amounts
    4. Calculate next renewal date
    """
    subscriptions = []
    
    # Group by merchant name (normalized)
    merchant_groups = defaultdict(list)
    for tx in transactions:
        merchant = normalize_merchant_name(tx.merchantName)
        if merchant:
            merchant_groups[merchant].append(tx)
    
    # Analyze each merchant group
    for merchant, txs in merchant_groups.items():
        if len(txs) < 2:  # Need at least 2 transactions
            continue
        
        # Extract amounts
        amounts = [float(tx.amount) for tx in txs]
        unique_amounts = set(amounts)
        
        # If same amount appears multiple times, likely subscription
        if len(unique_amounts) == 1 and len(txs) >= 2:
            amount = unique_amounts.pop()
            
            # Calculate billing cycle from dates
            dates = sorted([parse_date(tx.date) for tx in txs])
            if len(dates) >= 2:
                intervals = []
                for i in range(1, len(dates)):
                    diff = (dates[i] - dates[i-1]).days
                    if 1 <= diff <= 365:  # Reasonable interval
                        intervals.append(diff)
                
                if intervals:
                    avg_interval = sum(intervals) / len(intervals)
                    billing_cycle = determine_billing_cycle(avg_interval)
                    
                    # Calculate next renewal
                    last_date = dates[-1]
                    next_renewal = calculate_next_renewal(last_date, avg_interval)
                    
                    # Confidence based on pattern consistency
                    confidence = min(0.95, 0.5 + (len(txs) * 0.1))
                    
                    subscriptions.append(DetectedSubscription(
                        name=merchant,
                        amount=amount,
                        billingCycle=billing_cycle,
                        nextRenewalDate=next_renewal.isoformat(),
                        confidence=confidence
                    ))
    
    return subscriptions

def normalize_merchant_name(name: str) -> str:
    """Normalize merchant names (remove common prefixes, lowercase)"""
    if not name:
        return ""
    name = name.lower().strip()
    # Remove common prefixes
    prefixes = ["pay to", "paid to", "upi", "upi payment"]
    for prefix in prefixes:
        if name.startswith(prefix):
            name = name[len(prefix):].strip()
    # Remove special chars but keep spaces
    name = re.sub(r'[^\w\s]', '', name)
    return name.title() if name else ""

def parse_date(date_str: str) -> datetime:
    """Parse date string to datetime"""
    try:
        return datetime.fromisoformat(date_str.replace('Z', '+00:00'))
    except:
        return datetime.now()

def determine_billing_cycle(days: float) -> str:
    """Determine billing cycle from average days"""
    if days <= 10:
        return "WEEKLY"
    elif days <= 35:
        return "MONTHLY"
    elif days <= 100:
        return "QUARTERLY"
    else:
        return "YEARLY"

def calculate_next_renewal(last_date: datetime, interval_days: float) -> datetime:
    """Calculate next renewal date"""
    return last_date + timedelta(days=int(interval_days))

@app.post("/detect-subscriptions", response_model=DetectionResponse)
async def detect_subscriptions(request: DetectionRequest):
    try:
        subscriptions = detect_recurring_patterns(request.transactions)
        return DetectionResponse(subscriptions=subscriptions)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health():
    return {"status": "healthy", "service": "subscription-detection-ai"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
