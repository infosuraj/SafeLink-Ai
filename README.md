# 🛡️ SafeLink AI — Phishing URL Detector
### **A Machine Learning-Based Web Security System for Detecting Phishing URLs**

---

## 📋 Executive Summary
**SafeLink AI** is an intelligent web security system developed to detect **phishing and malicious URLs** using **Machine Learning** and structural URL analysis.

Unlike traditional solutions that rely on slow, resource-heavy web crawling, this system performs **near-instant lexical and statistical analysis**. Utilizing a **Random Forest classifier** trained via the Weka library, the system predicts whether a URL is *Legitimate* or *Phishing* within milliseconds.

---

## 🏗 Project Architecture

```text
phishing-url-detector/
│
├── TRAINER/         → ML Model training & feature extraction (Java + Weka)
├── BACKEND/         → Spring Boot 3.x REST API (Inference Engine)
├── FRONTEND/        → React Dashboard (Vite + Tailwind CSS)
├── DATA/            → Labeled datasets (CSV / ARFF formats)
└── README.md        → Project Documentation

```

---

## ⚙️ Component Overview

| Component | Purpose | Technology |
| --- | --- | --- |
| **TRAINER** | Dataset processing & .model serialization | Java, Weka, Maven |
| **BACKEND** | URL feature extraction & Prediction API | Spring Boot 3.x, Java 17 |
| **FRONTEND** | User interface & risk visualization | React 19, Vite, Tailwind |
| **DATA** | Historical phishing/benign URL samples | CSV, ARFF |

---

## 🧠 System Workflow

1. **User Input:** User submits a URL via the React dashboard.
2. **Feature Extraction:** The Spring Boot backend parses the URL into **11 unique features** (entropy, length, etc.).
3. **ML Inference:** The pre-trained Random Forest model processes these features.
4. **Result Delivery:** The system returns classification (Safe/Phishing) and a confidence score.

---

## 🔍 Extracted Features

The model analyzes the "DNA" of a URL using the following metrics:

* **URL Length:** Detects long, obfuscated paths.
* **Entropy:** Measures the randomness of characters (common in malicious links).
* **Structural Markers:** Presence of `@`, `//`, `-`, and multiple subdomains.
* **Security Protocol:** Checks for `HTTPS` vs `HTTP` usage.
* **Digit Ratio:** High numeric count often indicates automated domain generation.
* **Punycode:** Detects homograph attacks (e.g., using special characters to mimic real sites).

---

## 🛠 Tech Stack

**Frontend:** React 19, Vite, Tailwind CSS, Axios

**Backend:** Spring Boot 3.x, Java 17+, Maven

**Machine Learning:** Weka 3.8+, Random Forest Algorithm

**Deployment:** Railway (Backend), Static Hosting (Frontend)

---

## 🚀 Installation & Setup

### **1. Backend Setup**

```bash
cd BACKEND
mvn clean install
mvn spring-boot:run

```

### **2. Frontend Setup**

```bash
cd FRONTEND
npm install
npm run dev

```

### **3. Model Training (Optional)**

To retrain the model with fresh data:

```bash
cd TRAINER
mvn exec:java -Dexec.mainClass="com.trainer.ModelBuilder"

```

---

## 📡 API Documentation

**Endpoint:** `POST /api/check-url`

**Payload:** `{"url": "https://example.com"}`

**Response:**

```json
{
  "url": "[https://example.com](https://example.com)",
  "status": "Legitimate",
  "confidence": 0.98
}

```

---

## 📊 Model Performance

| Metric | Value |
| --- | --- |
| **Accuracy** | 97.2% |
| **Precision** | 96.8% |
| **Recall** | 97.5% |
| **F1-Score** | 0.97 |

---

## 🎓 Academic Information

* **Project Title:** Phishing URL Detection via Machine Learning
* **Project Type:** MCA Minor Project
* **Developed By:** M Suraj Kumar
* **Roll Number:** AA.SC.P2MCA24070070
* **Institution:** Amrita Vishwa Vidyapeetham (Amrita AHEAD)
* **Academic Year:** 2025 – 2027

---

## 📜 License

This project is developed strictly for **educational and research purposes**.

**Last Updated:** February 2026