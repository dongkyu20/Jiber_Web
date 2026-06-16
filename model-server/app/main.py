from fastapi import FastAPI

from app.api.internal import router as internal_router


app = FastAPI(
    title="Jiber Model Server",
    version="0.1.0",
    description=(
        "Phase 1 deterministic skeleton for internal apartment valuation and "
        "SHAP explanation APIs. This is not a real model server."
    ),
)
app.include_router(internal_router)


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "service": "model-server"}
