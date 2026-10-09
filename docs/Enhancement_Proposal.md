# Enhancement Proposal: AI Crop Recommendation Engine (Phase 3)

## 1. Problem
Currently, the crop advisory platform relies entirely on manual interaction between farmers and officers. Farmers submit soil readings, and officers manually advise on crops based on their own expertise. This process can be slow, inconsistent, and does not fully leverage data for improved decision-making.

## 2. Solution: AI-Powered Recommendation Engine
We propose integrating an AI-powered Crop Recommendation Engine. When an officer reviews a farmer's request, the system will offer AI-generated crop suggestions based on the farmer's soil profile (N, P, K, pH, humidity, temperature, rainfall).

### Key Features:
- **ML Model**: A Random Forest classifier trained on the public Kaggle Crop Recommendation dataset.
- **Microservice Architecture**: A dedicated Python/FastAPI microservice to serve predictions.
- **Human-in-the-Loop Integration**: AI suggestions are presented to the officer, who can accept, edit, or reject the recommendation before responding to the farmer.

## 3. Technology Choice
- **Language**: Python (industry standard for ML).
- **Service**: FastAPI (high-performance, easy to document with Swagger).
- **Deployment**: Render as a separate Python Web Service.

## 4. Integration Workflow
1.  **Farmer** submits soil reading.
2.  **Officer** opens `AdvisoryRequest` (with soil reading data).
3.  **Officer** clicks "Get AI Suggestion".
4.  **Spring Boot Backend** calls the **ML Microservice** via REST.
5.  **ML Microservice** returns top 3 crop recommendations with confidence levels.
6.  **Officer** reviews recommendations, edits the response text area, and sends it to the **Farmer**.

## 5. Ethical Considerations & Human-in-the-Loop
- The AI acts **only as a support tool**, not an automated decision-maker.
- The officer retains full accountability for the final advice, ensuring agricultural safety and relevance.
- Limitations of the model (data bias, regional applicability) are documented in the Model Card.
