# CustodyLog — Privacy Policy

**Effective Date:** October 22, 2026  
**Application Name:** CustodyLog (`com.aistudio.custodylog.vqrkmp`)  
**Jurisdictions Covered:** United States (CCPA/CPRA) & United Kingdom (UK GDPR / Data Protection Act)  
**Privacy Contact:** `privacy@custodylog.app`

---

## 1. Overview

CustodyLog ("we", "our", or "the App") is a co-parenting and child custody documentation platform designed to help separated and divorced parents manage parenting schedules, document custody exchanges, record incidents, track shared child expenses, and communicate respectfully.

We built CustodyLog on a **Zero-Trust Privacy Architecture**:
- Each parent authenticates with a separate account.
- Private account settings are isolated strictly to your authenticated user ID (`UID`).
- Shared family records are accessible **only** to the two parent accounts explicitly connected via a one-time invitation token (`CL-XXXXXXXX`).
- We never sell, rent, or share your personal or family data with data brokers or advertising networks.

---

## 2. Information We Collect

### A. Account & Authentication Data
- **Google Sign-In Credentials:** When you sign in using Google Sign-In (`CredentialManager`), we receive your Firebase Authentication User ID (`UID`), display name, and email address to authenticate your session and secure your records.

### B. User-Authored Co-Parenting Data
- **Child Profiles:** Child first/full name, date of birth, school/childcare notes, medical notes, and parenting schedule pattern (`2-2-3`, `2-2-5-5`, `Alternating Weekends`, or `Custom`).
- **Custody Schedule & Exchange Logs:** Scheduled dates/times, actual arrival timestamps, calculated lateness in minutes, pickup/drop-off locations, verification status (`Scheduled`, `User-Reported`, `Mutually Confirmed`, `Disputed`), and optional photo attachments.
- **Incident Journal & Evidence Attachments:** Incident category, neutral description, timestamps, revision history (`v1`, `v2`, etc.), and optional photo or PDF attachments (selected via the zero-permission Android Photo Picker or system document picker) along with on-device **SHA-256 file integrity hashes**.
- **Shared Expense Records:** Expense category, amount, currency (`USD` or `GBP`), split percentage, payment status (`Unpaid`, `Paid`, `Disputed`), and optional receipt images.
- **Co-Parent Messages:** Timestamped text messages, read receipts, edit/retraction audit logs, and optional attachments.

### C. Optional AI Calm Tone Assistant
- When you explicitly tap **"Rewrite in Calm, Child-Focused Tone"** while drafting a co-parent message, the draft text is sent over an encrypted HTTPS connection (`TLS 1.3`) to the Gemini API to generate a neutral, BIFF-style (Brief, Informative, Friendly, Firm) suggestion.
- **No Auto-Send:** AI suggestions are never automatically sent to your co-parent.

### D. Subscription & Billing Information
- Subscription purchases (`Premium` and `Family Premium`) are processed directly by **Google Play Billing**. CustodyLog does not collect or store your credit card or bank account numbers.

---

## 3. How We Protect & Store Your Data

- **Encryption in Transit & at Rest:** All network traffic between the Android app and Google Cloud / Firebase Firestore is encrypted in transit using HTTPS/TLS, and data is encrypted at rest.
- **Zero-Trust Security Rules:** Firestore security rules enforce strict `request.auth.uid` checks on every read and write operation.
- **Least-Privilege Device Access:** CustodyLog uses the Android system Photo Picker and Document Picker, meaning the app never requests broad access to your device's photo library or external storage.

---

## 4. Data Retention, Export & Account Deletion

- **Data Portability:** You can export your custody calendars, exchange histories, incident timelines, expense statements, and communication transcripts to **PDF** or **CSV** at any time inside the app (**Reports** icon in the top bar).
- **Account & Personal Data Deletion:** You have the right to permanently delete your CustodyLog account and personal data at any time:
  1. **In-App Instant Deletion:** Open **CustodyLog → Settings (Gear Icon) → Delete Account & Personal Data** (or tap **Delete Account & Data** on the Home or Login screen), check the confirmation box, and tap **Permanently Delete Account & Data Now**.
  2. **Web / Email Deletion Request (No Login Required):** Visit our Account & Data Deletion page (`delete-account.html` / `DELETE_ACCOUNT.md`) or email `privacy@custodylog.app` with the subject **"CustodyLog Account and Data Deletion Request"**.

---

## 5. Contact Us

For privacy inquiries, GDPR/CCPA data access requests, or account deletion assistance, contact:  
**Email:** `privacy@custodylog.app`
