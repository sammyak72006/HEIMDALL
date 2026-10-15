# PhishGuard AI - Intelligent Phishing & Digital Fraud Detection

PhishGuard AI is a real-time cybersecurity defense mobile application designed to detect, analyze, and neutralize deceptive phishing communications, malicious URLs, look-alike domains, and digital fraud scenarios. Combining instant multi-layer local heuristics with Gemini AI reasoning, it provides transparent risk scores, explainable indicators, and actionable guidance while safeguarding user privacy.

## User Review & Critical Decisions

> [!IMPORTANT]
> Based on your selections during Phase 1, the following core architecture and design parameters have been established for the prototype:

- **Confirmed Decision 1 (Detection Pipeline)**: **Hybrid Detection Engine**. Instant local heuristic pattern matching (homoglyphs, urgency lexicons, credential harvest signatures, entropy scoring) followed by deep context reasoning using Google Gemini AI (`gemini-3.5-flash`).
- **Confirmed Decision 2 (Verification & Benchmarking)**: **Interactive Test Lab**. A built-in validation suite containing 20+ verified test cases (legitimate vs. malicious across banking, government, fake job offers, and crypto/URL scams) with a live confusion matrix (TP, FP, TN, FN), precision, recall, and detection latency reporting.
- **Confirmed Decision 3 (Branding & UX)**: **PhishGuard AI**. A modern cybersecurity command center featuring high-contrast dark obsidian aesthetics, tactical status rings, explainable indicator tags, and clear risk triage.

---

## 1. Overview & Core Concept

- **What It Does**: Analyzes user-submitted or pasted SMS messages, suspicious emails, payment requests, and URLs. It inspects domain reputation, detect homoglyph/typosquatting attacks, detects urgency and coercion linguistics, calculates a normalized risk score (0–100% categorized into Low, Medium, High Risk), explains every underlying trigger, and issues direct action playbooks (e.g., "Do not click", "Verify via official hotline", "Report fraud").
- **Target Audience**: Mobile users, enterprise employees, senior citizens, and security researchers seeking immediate, understandable verification of suspect messages and links.
- **Key Value**: Bridges the gap between opaque blacklist checkers and user intuition by offering fast on-device heuristic triage paired with explainable AI reasoning that exposes subtle brand impersonation, deceptive routing, and psychological manipulation.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Interactive Scanner / Inspector**:
   - Tabbed input: **Message / Email / SMS** scanner vs. **URL / Website** scanner.
   - Quick "Paste from Clipboard" and "Load Realistic Scenario" shortcut chips (e.g., *Wells Fargo Alert*, *USPS Package Delivery*, *IRS Tax Refund*, *Remote Job Offer*, *Crypto KYC*).
   - "Scan Item" action initiates instant multi-tier analysis with real-time stage progress indicator (Heuristics -> Threat Intelligence Feed -> Gemini AI Deep Analysis).
2. **Analysis Result & Explainable Report**:
   - Prominent Risk Gauge (0-100 score with visual color coding: Emerald Green for Safe, Amber Yellow for Suspicious, Crimson Red for High Risk/Malicious).
   - Scenario Identification tag (e.g., `Fake Banking Alert`, `Credential Harvesting`, `Government Impersonation`, `Look-alike Domain`).
   - Detailed Threat Indicators breakdown (Domain entropy, HTTPS presence, Suspicious TLD, High-urgency keywords, Sensitive credential prompts).
   - AI Security Summary: Clear, non-technical explanation of why this was flagged.
   - Recommended Action Playbook: Step-by-step guidance (Block Sender, Contact Official Support, Never Share OTP, Report to FTC/CERT).
3. **Interactive Benchmark & Validation Lab**:
   - Comprehensive test suite of labelled legitimate and phishing samples.
   - Run individual scenario tests or "Run All Tests" batch evaluation.
   - Live **Confusion Matrix** visualizer (True Positives, True Negatives, False Positives, False Negatives).
   - Accuracy, Precision, Recall, and F1-Score telemetry metrics, with documented edge cases and model limitations.
4. **Threat Intelligence & Feed Explorer**:
   - Local database of known phishing indicators, suspicious TLDs (`.xyz`, `.top`, `.tk`, `.icu`), look-alike patterns (`paypa1`, `arnazon`, `micros0ft`), and flagged threat signatures.
   - Option to query, filter, and inspect community threat indicators.
5. **Scan History & Evidence Vault**:
   - Room-persisted history of past scans with timestamps, risk ratings, and quick re-inspection.
   - Privacy clear button to securely wipe inspection history on demand.

### Visual Identity & Theme
- **Aesthetic Direction**: High-tech cybersecurity command center—sleek, authoritative, high-contrast, and focused on clarity and data legibility.
- **Color Palette**:
  - `Background`: Deep Midnight Slate (`#0B0F19`)
  - `Surface / Cards`: Elevated Obsidian (`#161E2E`) with subtle borders (`#2D3748`)
  - `Primary / Accent`: Cyber Electric Cyan (`#00E5FF` / `#00B4D8`)
  - `Risk Levels`:
    - *Low Risk / Clean*: Cyber Emerald (`#00E676`)
    - *Medium Risk / Suspicious*: Warning Amber (`#FFD600`)
    - *High Risk / Malicious*: Threat Crimson (`#FF1744`)
- **Typography & Hierarchy**: Clean monospace accents for domains, hashes, and risk indicators paired with high-legibility sans-serif for explanatory narratives and action steps.
- **Component Styling**: Pill status badges, glowing risk meters, segmented input bars, collapsible indicator cards with risk contribution chips, and zero-clutter card hierarchies.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Hybrid Detection Engine**
  - *Chosen Approach*: Multi-tier architecture. Tier 1 executes sub-5ms on-device deterministic heuristics (regex keyword detection, entropy calculation, TLD scoring, homoglyph check, HTTP/HTTPS validation). Tier 2 checks local threat intelligence feeds. Tier 3 engages Gemini AI (`gemini-3.5-flash`) via structured JSON schema for deep semantic understanding, intent recognition, and contextual explanations.
  - *Why*: Guaranteed instant feedback even offline or on slow networks; local heuristics catch known patterns instantly, while AI detects nuanced spear-phishing and brand impersonation.
  - *Alternatives Considered*: Pure AI (too slow and fails offline) or pure regex (misses creative evasion tactics).
- **Decision 2: Privacy-Preserving Scanner Design**
  - *Chosen Approach*: On-device PII masking (redacting phone numbers, email addresses, and credit card patterns locally before dispatching text to Gemini AI).
  - *Why*: Directly satisfies Requirement 8 ("Privacy: minimise collection and storage of personal information and handle submitted items securely").
- **Decision 3: Interactive Evaluation Lab & Confusion Matrix**
  - *Chosen Approach*: In-app test laboratory with a curated dataset of 20+ realistic samples across 4 attack categories and legitimate controls. Users and evaluators can trigger real-time benchmarking that populates a dynamic 2x2 confusion matrix with accuracy, precision, and recall metrics.
  - *Why*: Directly satisfies Success Criteria & Requirement 5 ("Testing against legitimate and malicious examples, with false positives and false negatives reported").
- **Decision 4: Local Storage via Room Database**
  - *Chosen Approach*: Room Database for storing historical scans, threat intelligence signatures, and benchmark results locally on the device.
  - *Why*: Maximizes data privacy, zero server-side exposure of user message logs, and instant offline retrieval.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PhishGuard UI Layer                             │
│  ┌───────────────────────┬──────────────────────┬────────────────────┐ │
│  │    Quick Scanner      │   Interactive Lab    │   Threat Intel     │ │
│  │ (SMS/Email/URL Input) │ (Confusion Matrix)   │ & Scan History     │ │
│  └───────────┬───────────┴──────────┬───────────┴──────────┬─────────┘ │
└──────────────┼──────────────────────┼──────────────────────┼───────────┘
               ▼                      ▼                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        PhishGuard ViewModel                            │
│  - Scan State (Idle, Scanning, Result, Error)                          │
│  - Benchmark State (Active tests, Confusion Matrix, Latency)           │
│  - History & Threat Feeds StateFlow                                    │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     PhishDetectionCoordinator                          │
│                                                                        │
│   ┌────────────────────────┐         ┌─────────────────────────────┐   │
│   │ Tier 1: Local Engine   │         │ Tier 2: Threat Feed Store   │   │
│   │ - Homoglyph / Typosquat│         │ - Known malicious domains   │   │
│   │ - Entropy & TLD checks │         │ - Suspicious keywords DB    │   │
│   │ - Urgency & PII checks │         │ - Known sender spoof list   │   │
│   └──────────┬─────────────┘         └──────────────┬──────────────┘   │
│              │                                      │                  │
│              └───────────────────┬──────────────────┘                  │
│                                  ▼                                     │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ Tier 3: Privacy Filter + Gemini 3.5 Flash Reasoning            │   │
│   │ - Local PII Redactor (hashes/masks sensitive values)           │   │
│   │ - Structured JSON Output (Scenario, Risk Score, Explanations,  │   │
│   │   Recommendations, Specific Indicators)                        │   │
│   └────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       Data & Persistence Layer                         │
│  - AppDatabase (Room): ScanResultDao, ThreatFeedDao, BenchmarkDao      │
│  - Gemini REST Service (Retrofit + Kotlinx Serialization)              │
└────────────────────────────────────────────────────────────────────────┘
```

### Core Data Models
- `ScanItem`: Input type (`MESSAGE`, `EMAIL`, `SMS`, `URL`), content, submission timestamp.
- `ScanResult`:
  - `overallScore`: 0–100 risk percentage
  - `riskLevel`: `LOW` (0-39), `MEDIUM` (40-69), `HIGH` (70-100)
  - `scenario`: `BANKING_FRAUD`, `GOVERNMENT_IMPERSONATION`, `FAKE_JOB_OFFER`, `MALICIOUS_URL`, `INVESTMENT_SCAM`, `LEGITIMATE`
  - `indicators`: List of specific identified signals (e.g., "Look-alike domain detected: `paypa1-verify.com`", "Urgency coercion: 24-hour account suspension threat", "Unsecured HTTP connection for login")
  - `explanation`: Plain-language explanation of findings
  - `recommendedActions`: Bulleted actionable safety guidance
  - `heuristicBreakdown`: Score contributions from local rules vs. AI context
- `BenchmarkCase`: Pre-configured ground-truth test item (title, text, ground truth label, category, expected indicators).
- `BenchmarkSummary`: True Positives (TP), True Negatives (TN), False Positives (FP), False Negatives (FN), Accuracy, Precision, Recall, F1, Average Latency (ms).

### Implementation Stages
1. **Secrets & Dependencies**: Uncomment `GEMINI_API_KEY` in `.env.example`, ensure Kotlin serialization and Room dependencies are active in `build.gradle.kts` and `AndroidManifest.xml` has `INTERNET`.
2. **Detection Engine Core**: Build heuristic analyzer (regex patterns, URL parsing, homoglyph detector, entropy calculator, urgency scorer) and PII sanitizer.
3. **Gemini AI Integration**: Build Retrofit client with `gemini-3.5-flash` using structured JSON schema response for prompt reasoning.
4. **Room Database**: Create entities and DAOs for scan history and threat feeds.
5. **Interactive UI**:
   - Main Command Center with bottom navigation (Scanner, Test Lab, Threat Feeds, History).
   - Scanner screen with scenario prefill presets, live scanner animation, and rich explainable results breakdown.
   - Test Lab screen with interactive runner, category filter, and visual confusion matrix.
   - Threat intelligence screen with searchable IOC (Indicators of Compromise) and security advice.
6. **Robolectric & Unit Tests**: Create comprehensive unit tests verifying heuristic rules, scoring, confusion matrix math, and PII redactor.
