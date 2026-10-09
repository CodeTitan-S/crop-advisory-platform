from fastapi import FastAPI
from .schemas import SoilReading
from .model_loader import predict_crop

app = FastAPI()

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/predict")
def predict(reading: SoilReading):
    recommendations = predict_crop(reading)
    return {"recommendations": recommendations}
