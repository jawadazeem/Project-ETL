from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from collections import Counter

app = FastAPI(title="Audit Service", version="1.0.0")


class BillingRecord(BaseModel):
    serviceName: Optional[str] = None
    cloudProvider: Optional[str] = None
    totalCharge: float
    accountName: Optional[str] = None
    resourceId: Optional[str] = None
    billingPeriod: Optional[str] = None
    description: Optional[str] = None


class AuditRequest(BaseModel):
    records: List[BillingRecord]


class DuplicateDetail(BaseModel):
    serviceName: Optional[str] = None
    cloudProvider: Optional[str] = None
    totalCharge: float
    accountName: Optional[str] = None
    count: int


class AuditFinding(BaseModel):
    type: str
    severity: str
    description: str
    duplicates: List[DuplicateDetail]


class AuditResponse(BaseModel):
    findings: List[AuditFinding]
    totalRecordsScanned: int


@app.get("/health")
def health_check():
    return {"status": "healthy"}


@app.post("/audit", response_model=AuditResponse)
def run_audit(request: AuditRequest):
    if not request.records:
        raise HTTPException(status_code=400, detail="No records provided for audit.")

    findings = []

    # Duplicate charge detection
    # Group by (serviceName, cloudProvider, totalCharge, accountName)
    keys = []
    for r in request.records:
        key = (r.serviceName or "", r.cloudProvider or "", r.totalCharge, r.accountName or "")
        keys.append(key)

    counts = Counter(keys)
    duplicates = []

    for key, count in counts.items():
        if count > 1:
            duplicates.append(DuplicateDetail(
                serviceName=key[0] or None,
                cloudProvider=key[1] or None,
                totalCharge=key[2],
                accountName=key[3] or None,
                count=count,
            ))

    if duplicates:
        total_dup_charges = sum(d.totalCharge * (d.count - 1) for d in duplicates)
        findings.append(AuditFinding(
            type="DUPLICATE_CHARGE",
            severity="HIGH",
            description=f"Found {len(duplicates)} group(s) of duplicate charges "
                        f"totaling ${total_dup_charges:,.2f} in excess billing.",
            duplicates=duplicates,
        ))

    return AuditResponse(
        findings=findings,
        totalRecordsScanned=len(request.records),
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5001)