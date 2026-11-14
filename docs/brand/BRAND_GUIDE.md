# Brand & Visual Identity Guide — Collect Your Rent

## 1. Brand Identity & Purpose
**Collect Your Rent** is an Android application designed for independent landlords who value speed, privacy, and clarity. It combines a strict **local-first** offline architecture with an executive-level visual polish inspired by modern Scandinavian design and Material 3 principles.

- **Primary Persona:** The independent landlord managing single units to multi-family properties.
- **Brand Voice:** Calm, dependable, organized, and straightforward. No corporate clutter, zero ads, no cloud lock-in.

---

## 2. Logo Mark Anatomy

The logo mark is an iconic geometric symbol set within an adaptive squircle:
- **Architectural Gable:** Represents the landlord's properties, stability, and structure.
- **Arched Threshold:** Symbolizes hospitality, active tenancies, and open doors.
- **Golden Rupee Medallion (`₹`):** Symbolizes the rent coming safely home. It is positioned at the heart of the home to communicate prompt, organized collections.

```
          /\           <- Gable Roof (Property)
         /  \
        /    \
       |  (₹) |        <- Golden Rupee Medallion (Rent Collected)
       |  |  ||        <- Welcoming Threshold
```

---

## 3. Color System

### Primary & Brand Colors
- **Royal Emerald (`#0F5132` / `#198754`):** Represents wealth, reliability, and growth. Used for primary buttons, prominent cards, and brand badges.
- **Mint Emerald (`#34D399` / `#6EE7B7`):** Primary tone in dark theme, maintaining high legibility and contrast against dark slate surfaces.
- **Luminous Gold / Amber (`#D97706` / `#F59E0B`):** Accent color for financial highlights, coins, and important milestones.

### Functional Status Color Palette (Dual-Channel Encoded)
Every status indicator is **always paired with an explicit icon and label** to comply with WCAG 2.1 Non-Text Contrast requirements:

| Status | Meaning | Light Mode (Text on Fill) | Dark Mode (Text on Fill) | Icon |
| :--- | :--- | :--- | :--- | :--- |
| **PAID** | Period rent fully settled | `#145A32` on `#D1E7DD` | `#75B798` on `#0A3622` | `CheckCircle` |
| **DUE SOON** | Due within 5 days | `#7D6608` on `#FFF3CD` | `#FFDA6A` on `#4D3A00` | `Schedule` |
| **OVERDUE** | Past grace period | `#842029` on `#F8D7DA` | `#EA868F` on `#2C0B0E` | `Warning` |
| **LEFT** | Tenant departed / archived | `#495057` on `#E9ECEF` | `#ADB5BD` on `#343A40` | `Logout` |
| **UPCOMING** | Next billing cycle | `#0A58CA` on `#CFE2FF` | `#6EA8FE` on `#031633` | `Event` |

---

## 4. Typography System
- **Scale:** Material 3 Type Scale with tuned line-heights and letter-spacing for mobile glanceability.
- **Tabular Numerals (`FontFeatureSettings = "tnum"`):** All monetary figures, amounts, and dates enforce tabular numbers to prevent jitter in lists and tables.
- **Indian Number Grouping:** Currency is strictly formatted in Indian numbering conventions (`₹1,25,000` rather than `₹125,000`).

---

## 5. Asset Catalog
- `docs/brand/logo.svg`: Master vector file.
- `docs/brand/logo-512.png`: 512x512 high-resolution icon.
- `docs/brand/logo-1024.png`: 1024x1024 store-ready graphic.

