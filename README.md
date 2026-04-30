# GenCatalog AI
### *Turning messy product data into something your marketing team won't cry over.*

---

![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)
![Frontend](https://img.shields.io/badge/frontend-React-blue?style=flat-square&logo=react)
![Backend](https://img.shields.io/badge/backend-SpringBoot-green?style=flat-square&logo=springboot)
![AI Engine](https://img.shields.io/badge/AI-FastAPI-orange?style=flat-square&logo=fastapi)
![License](https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square)

---

## What is this?

**GenCatalog AI** is a full-stack AI pipeline that takes raw product catalogs (CSV / Excel) and transforms them into:

- ✨ Clean product descriptions
- 🧠 Smart tags
- 🔍 SEO-ready content
- 🛡️ Reliable outputs *(even when AI tries to be creative in the wrong way)*

---

## 😏 Why I built this

Because I realised:

> AI is powerful…  
> but trusting it blindly is dangerous.

So instead of just *"calling an API and praying"*,  
I built a system that says:

> *"Even if AI fails… I still got this."*

---

## ⚙️ How it works *(simple version)*

```
📁 Upload CSV / Excel
        ↓
🧹 Clean & normalize data
        ↓
🧠 Send to AI model
        ↓
🧩 Extract structured JSON
        ↓
🛡️ Fallback kicks in (if AI messes up)
        ↓
📊 Clean enriched output
        ↓
⬇️ Export CSV
```

---

## 🧠 The interesting part *(not just another AI project)*

**Most AI projects:**
```
Input → LLM → Output  (hope for the best)
```

**This project:**
```
Input → LLM → Validate → Repair → Fallback → Output
```

---

## 🔥 Key Features

- 📂 CSV + Excel support
- 🧠 AI-generated descriptions & SEO content
- 🛡️ Fault-tolerant fallback system
- ⚡ Real-time processing UI
- 🧩 Flexible column handling *(no strict formats)*
- 📊 Clean table + export system
- 📱 Fully responsive

---

## 🧪 Input Flexibility

Because real users don't follow perfect formats:

| Your CSV | System understands |
|----------|-------------------|
| `name` / `title` | `product_name` |
| `main_cate` / `type` | `category` |
| `actual_price` / `cost` | `price` |

---

## 🖥️ Tech Stack

| Layer | Technology |
|-------|-----------|
| 🖥️ **Frontend** | React + TypeScript, Tailwind CSS |
| ⚙️ **Backend** | Spring Boot |
| 🧠 **AI Engine** | FastAPI (Python), LLM Integration |

---

## 📦 Project Structure

```
GenCatalog-AI/
│
├── frontend/     → UI layer
├── backend/      → API layer
├── ai-engine/    → AI processing
├── data/         → sample data
│
└── README.md
```

---

## 🚀 Live Demo

👉 [Add your deployed link here]

---

## ⚠️ Honest Notes

AI is not perfect.

That's exactly why this system exists.

---

## 😂 Small Reality Check

If you're still writing product descriptions manually:

> I'm not judging…  
> but this project is.

---

## 👨‍💻 Author

**Abhiraj Dixit**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Let's_Connect-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/abhiraj-dixit-6aa386313/)

> *Sent me a connection request. No message. No note. Just vibes - because that's peak LinkedIn etiquette.* 😂

---

## ⭐ If you found this useful

- ⭐ Star the repo
- 📤 Share it
- Or just silently judge my code *(acceptable)*

---

## 🧠 Final Thought

This project is not about AI.

It's about:

> **building systems that don't break when AI does.**
