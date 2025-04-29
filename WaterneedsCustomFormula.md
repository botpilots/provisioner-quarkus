# Hydration Formula for Endurance Activities in Varying Climates

This hydration formula is designed for athletes and adventurers involved in high-output endurance activities, such as ocean rowing, where hydration demands are extreme and environment-dependent. The formula combines established physiological models with practical adjustments for activity duration and environmental stress.

## 📌 Sources and Foundations

### 1. **Baseline Hydration — WHO/FAO Guidelines (Body Surface Area)**
- The base water requirement is calculated using the **DuBois formula** for Body Surface Area (BSA).
- Baseline daily water intake is estimated as `BSA × 1500 ml`, based on **WHO nutritional recommendations** for adults at rest.

**Reference**:  
> WHO/FAO/UNU. "Human energy requirements." (2004) – Chapter 5: Water requirements.

---

### 2. **Activity-Based Hydration — ACSM & Military Guidelines**
- Physical exertion increases water needs substantially. The **American College of Sports Medicine (ACSM)** recommends 0.4–0.8 L/hour for moderate exercise, and up to **1.0–1.2 L/hour** for endurance in heat.
- Military guidelines (e.g., **US Army Research Institute of Environmental Medicine**) suggest **1 L/hour** in hot, active conditions.

**Adaptation Used**:
> Base water need during activity is approximated as **900 ml/hour**, suitable for prolonged aerobic output (e.g., rowing).

---

### 3. **Temperature Multiplier — Nonlinear Environmental Adjustment**
- Instead of a flat or linear increase, we apply a **square-root function** to model how water loss accelerates from cold to warm climates and then plateaus.
- This captures the **nonlinear rise in sweat rate and evaporative loss** with temperature:
  
```text
multiplier = 1 + (√(T − 10) / 5)  ;  for T ≥ 10°C
