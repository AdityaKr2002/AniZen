# Wiki Style Guide

Standardized writing formation and style rules for the **AniZen Wiki**.

> [!NOTE]
> These rules apply **exclusively to pages within the Wiki** (`.github/wiki/` and the GitHub Wiki repository). They do not affect root documentation, code comments, or external repository files.

---

## 1. Page Hierarchy & Structure

- **Title (H1)**: Every page must begin with a single `# Heading`.
- **Sub-description**: Immediately under the H1 title, include a single concise sentence describing the page's purpose.
- **Section Dividers**: Use `---` horizontal rules after the sub-description and between major H2 sections.
- **Heading Order**: Use `## H2` for main topic sections and `### H3` for sub-sections. Never skip heading levels.

```markdown
# Page title

Single concise sentence explaining what this page covers.

---

## Section title

### Sub-section title
```

---

## 2. Voice, Tone & UI Formatting

- **Voice**: Instructional, direct, concise, and neutral (Aniyomi / VitePress documentation style).
- **Imperative Steps**: Use direct action verbs for instructions (*"Open AniZen"*, *"Navigate to Settings"*, *"Select the APK"*).
- **UI Elements**: **Bold** all UI labels, button text, menu paths, and tab titles.
  - Path format: **Settings → Player → Decoder**
  - Button format: Press the **Add to library** button.

---

## 3. Step-by-Step Instructions

- Use numbered lists (`1.`, `2.`, `3.`) for all procedures, installation steps, and setup guides.
- Keep individual steps concise and focused on a single action.

```markdown
1. Open **AniZen** and navigate to the **Browse** tab.
2. Select **Extension Repositories** from the top toolbar.
3. Paste the repository URL and tap **Add**.
```

---

## 4. Callouts & Admonitions

Use standard GitHub alert blockquotes for context-specific callouts:

- `> [!NOTE]` — Contextual information, package IDs, or default behaviors.
- `> [!TIP]` — Best practices, shortcuts, or optimization suggestions.
- `> [!WARNING]` — Performance costs, high GPU usage, or device heating warnings.
- `> [!IMPORTANT]` — Mandatory prerequisites or critical setup steps.

---

## 5. Tables & Comparisons

Use Markdown tables to present feature comparisons, preset profiles, gesture lists, and hardware requirements.

```markdown
| Preset | Settings | Recommended Use |
|:---|:---|:---|
| **Vivid Anime** | Contrast +5, Saturation +20 | Enhances line art and color vibrance |
```

---

## 6. Title Search Examples

When providing title search examples or alternative spellings, format them using blockquotes:

```markdown
> Example: **Boku no Hero Academia** instead of **My Hero Academia**.
```
