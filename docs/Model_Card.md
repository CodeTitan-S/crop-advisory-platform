---
name: model-card
description: Documentation for the Crop Advisory AI model
---

# Model Card: Crop Recommendation AI

## Model Details
- **Description:** A Random Forest classifier trained to suggest optimal crops based on soil nutrient levels (N, P, K), environmental factors (temperature, humidity, pH), and local rainfall data.
- **Goal:** Assist agricultural officers in providing evidence-based crop recommendations to farmers.
- **Model Type:** Supervised Random Forest Classifier.
- **Framework:** `scikit-learn` (Python).

## Training Data
- **Source:** Synthetic dataset engineered to maintain consistency with industry-standard soil data distribution.
- **Features:** Nitrogen (N), Phosphorus (P), Potassium (K), pH, Rainfall, Temperature, Humidity.
- **Target:** Crop Label (e.g., rice, maize, chickpea, kidneybeans, pigeonpeas, mothbeans, mungbean, blackgram, lentil, pomegranate, banana, mango, grapes, watermelon, muskmelon, apple, orange, papaya, coconut, cotton, jute, coffee).

## System Integration
- **Microservice:** FastAPI acts as the ML inference engine, exposing a `POST /predict` endpoint.
- **Backend Integration:** The Spring Boot `AdvisoryRequestService` client (`MlServiceClient`) consumes this endpoint and attaches the top-3 ranked suggestions (with confidence scores) to an advisory request for officer review (human-in-the-loop).

## Limitations
- **Accuracy:** Performance depends on the accuracy of soil readings provided by the farmer. This is not a substitute for agricultural expertise.
- **Data Bias:** As the model is trained on a synthetic dataset, predictions may behave unpredictably if applied to highly novel soil profiles not represented during training.
- **Dependencies:** The model relies on the availability of the `ml-service` microservice.
