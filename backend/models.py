# backend/models.py
from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    hashed_password = Column(String)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    farm_sites = relationship("FarmSite", back_populates="owner")

class FarmSite(Base):
    __tablename__ = "farm_sites"

    id = Column(Integer, primary_key=True, index=True)
    owner_id = Column(Integer, ForeignKey("users.id"), nullable=True) # Nullable for existing data
    site_name = Column(String, index=True)
    location_coordinates = Column(String)
    soil_type = Column(String)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    owner = relationship("User", back_populates="farm_sites")
    soil_readings = relationship("SoilReading", back_populates="farm_site")
    season_logs = relationship("SeasonLog", back_populates="farm_site")
    disease_reports = relationship("DiseaseReport", back_populates="farm_site")
    recommendations = relationship("CropRecommendation", back_populates="farm_site")

class SoilReading(Base):
    __tablename__ = "soil_readings"

    id = Column(Integer, primary_key=True, index=True)
    farm_site_id = Column(Integer, ForeignKey("farm_sites.id"))
    
    # N-P-K and Environmental metrics (matching your ML dataset needs)
    nitrogen = Column(Float)
    phosphorus = Column(Float)
    potassium = Column(Float)
    ph_level = Column(Float)
    temperature = Column(Float)
    humidity = Column(Float)
    rainfall = Column(Float)
    
    timestamp = Column(DateTime(timezone=True), server_default=func.now())

    farm_site = relationship("FarmSite", back_populates="soil_readings")

class CropRecommendation(Base):
    __tablename__ = "crop_recommendations"

    id = Column(Integer, primary_key=True, index=True)
    farm_site_id = Column(Integer, ForeignKey("farm_sites.id"))
    recommended_crop = Column(String)
    confidence_score = Column(Float)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    farm_site = relationship("FarmSite", back_populates="recommendations")

class SeasonLog(Base):
    __tablename__ = "season_logs"

    id = Column(Integer, primary_key=True, index=True)
    farm_site_id = Column(Integer, ForeignKey("farm_sites.id"))
    crop_planted = Column(String)
    plant_date = Column(DateTime(timezone=True))
    harvest_date = Column(DateTime(timezone=True), nullable=True)
    yield_amount_kg = Column(Float, nullable=True)
    notes = Column(Text, nullable=True)

    farm_site = relationship("FarmSite", back_populates="season_logs")

class DiseaseReport(Base):
    __tablename__ = "disease_reports"

    id = Column(Integer, primary_key=True, index=True)
    farm_site_id = Column(Integer, ForeignKey("farm_sites.id"))
    image_url = Column(String) # Path to stored image
    detected_disease = Column(String)
    confidence_score = Column(Float)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    farm_site = relationship("FarmSite", back_populates="disease_reports")

class KnowledgeBaseEntry(Base):
    __tablename__ = "knowledge_base"
    # This acts as your static dictionary for diseases and treatments

    id = Column(Integer, primary_key=True, index=True)
    disease_name = Column(String, unique=True, index=True)
    symptoms = Column(Text)
    treatment = Column(Text)
    prevention = Column(Text)