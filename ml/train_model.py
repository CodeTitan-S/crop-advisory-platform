import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import LabelEncoder
import joblib
import os

# Create Mock Dataset
def generate_data(n=1000):
    data = {
        'N': np.random.randint(0, 150, n),
        'P': np.random.randint(0, 150, n),
        'K': np.random.randint(0, 150, n),
        'temperature': np.random.uniform(10, 40, n),
        'humidity': np.random.uniform(20, 100, n),
        'ph': np.random.uniform(5, 8, n),
        'rainfall': np.random.uniform(50, 300, n),
        'label': np.random.choice(['rice', 'maize', 'chickpea', 'kidneybeans', 'pigeonpeas'], n)
    }
    return pd.DataFrame(data)

df = generate_data()

# 2. Preprocess
X = df.drop('label', axis=1)
y = df['label']

le = LabelEncoder()
y_encoded = le.fit_transform(y)

# 3. Train
model = RandomForestClassifier(n_estimators=50, random_state=42)
model.fit(X, y_encoded)

# 4. Save
os.makedirs('ml/model', exist_ok=True)
joblib.dump(model, 'ml/model/crop_model.pkl')
joblib.dump(le, 'ml/model/label_encoder.pkl')
print("Model saved to ml/model/")
