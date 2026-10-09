import joblib
import os

MODEL_PATH = os.path.join(os.path.dirname(__file__), '..', 'model', 'crop_model.pkl')
ENCODER_PATH = os.path.join(os.path.dirname(__file__), '..', 'model', 'label_encoder.pkl')

model = joblib.load(MODEL_PATH)
encoder = joblib.load(ENCODER_PATH)

def predict_crop(data):
    features = [[data.N, data.P, data.K, data.temperature, data.humidity, data.ph, data.rainfall]]
    prediction_proba = model.predict_proba(features)[0]

    # Get top 3
    top3_idx = prediction_proba.argsort()[-3:][::-1]
    top3_crops = encoder.inverse_transform(top3_idx)
    top3_probs = prediction_proba[top3_idx]

    return [{"crop": crop, "confidence": float(prob)} for crop, prob in zip(top3_crops, top3_probs)]
