import joblib
import os


def _model_dir():
    """Locate the directory holding crop_model.pkl and label_encoder.pkl.

    On Render the model files are mounted as a Secret File at /etc/secrets/model/
    (the repo does not ship the *.pkl files — see .gitignore). Locally they live
    in ../../model relative to this module. RENDER env var is Render's standard
   """
    secrets_dir = "/etc/secrets/model"
    if os.path.isdir(secrets_dir):
        return secrets_dir
    return os.path.join(os.path.dirname(__file__), "..", "model")

MODEL_PATH = os.path.join(_model_dir(), "crop_model.pkl")
ENCODER_PATH = os.path.join(_model_dir(), "label_encoder.pkl")

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
