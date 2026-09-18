---
name: Editorial Transit
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
  on-surface: '#e5e2e1'
  on-surface-variant: '#e9bdb3'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#af887f'
  outline-variant: '#5e3f38'
  surface-tint: '#ffb4a4'
  primary: '#ffb4a4'
  on-primary: '#630e00'
  primary-container: '#ff5633'
  on-primary-container: '#570b00'
  inverse-primary: '#b72200'
  secondary: '#ffffff'
  on-secondary: '#283500'
  secondary-container: '#c3f400'
  on-secondary-container: '#556d00'
  tertiary: '#92ccff'
  on-tertiary: '#003351'
  tertiary-container: '#3a97d8'
  on-tertiary-container: '#002c47'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdad3'
  primary-fixed-dim: '#ffb4a4'
  on-primary-fixed: '#3e0500'
  on-primary-fixed-variant: '#8c1800'
  secondary-fixed: '#c3f400'
  secondary-fixed-dim: '#abd600'
  on-secondary-fixed: '#161e00'
  on-secondary-fixed-variant: '#3c4d00'
  tertiary-fixed: '#cce5ff'
  tertiary-fixed-dim: '#92ccff'
  on-tertiary-fixed: '#001d31'
  on-tertiary-fixed-variant: '#004b73'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
  editorial-paper: '#FAFAFA'
  editorial-black: '#0A0A0A'
  hairline-gray: '#262626'
  rule-light: '#E5E5E5'
  surface-card: '#181818'
  muni-red: '#BA0C2F'
  cable-car-green: '#008559'
  electric-alert: '#FF3300'
  signal-chartreuse: '#CCFF00'
  telemetry-cyan: '#057DBC'
  route-bus: '#0072BC'
  route-lightrail: '#00897B'
  route-rapid: '#F57C00'
  route-express: '#D32F2F'
  route-latenight: '#7B1FA2'
typography:
  headline-xl:
    fontFamily: Space Grotesk
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.04em
  headline-xl-mobile:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.03em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-telemetry:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-badge:
    fontFamily: Space Grotesk
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.05em
  label-caption:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 14px
    letterSpacing: 0.04em
spacing:
  hairline: 1px
  space-2xs: 0.25rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
  gutter-mobile: 1rem
  gutter-tablet: 1.5rem
  column-gap: 0.75rem
---

## Brand & Style

This design system translates the stark, authoritative, tech-forward editorial aesthetic of high-impact print-digital journalism into an ultra-functional Android transit tracker for San Francisco Muni. 

The personality is hyper-legible, utilitarian, and uncompromisingly precise. It ditches generic commuter blue gradients and bubbly iconography in favor of razor-sharp editorial grids, heavy monospaced telemetry, and brutalist contrast. Commuters, navigators, and daily riders receive rapid, unambiguous transit data—arrival counts, real-time vehicle coordinates, delay warnings—framed with the visual gravity of front-page journalism.

The aesthetic leans on a **High-Contrast Editorial & Brutalist-Tactile** hybrid:
- Crisp 1px structural hairline rules separating transit lines and stops.
- Deep monochrome grounding punctured by high-visibility vermilion and chartreuse signal badges.
- Technical, monospaced arrival indicators paired with punchy grotesques for high-velocity scanability under harsh direct sunlight or fast-paced platform movements.

## Colors

The color system operates on an uncompromising dark-mode default inspired by high-density digital teleprompters, dispatch terminals, and editorial data journalism. 

### Palette Architecture
- **Primary (`#FF3300` / Electric Vermilion):** Used for live alerts, system disruptions, arrival countdowns under 2 minutes, and acute interactive states. Demands immediate commuter attention.
- **Secondary (`#CCFF00` / Signal Chartreuse):** Used for on-time verification, active vehicle pings, route badges (e.g., Rapid services), and active step markers.
- **Tertiary (`#057DBC` / Telemetry Blue):** Derived from technical editorial hyperlinks and maritime transit elements; handles rail/ferry integration, map paths, and supplementary metadata.
- **Neutral (`#121212` / Pitch Base):** The dark canvas background, complemented by `#181818` card containers and `#262626` hairline borders.

### Transit Badging Hierarchy
Route badges use the color associated with their route type:

- **Normal Bus:** `route-bus` (`#0072BC`)
- **Normal Light Rail:** `route-lightrail` (`#00897B`)
- **Rapid:** `route-rapid` (`#F57C00`)
- **Express:** `route-express` (`#D32F2F`)
- **Late Night:** `route-latenight` (`#7B1FA2`)

The route type color is used as the background/fill of the route badge, with text and icon treatment chosen to maintain strong contrast.

### Route Color Usage

- When a bus route is selected, its route type determines the active route color.

- While viewing bus stops for the selected route:

  - Use the selected route type's color for route-specific visual elements.
  - Route-specific elements may include the selected route indicator, route markers, stop markers, active states, and other UI elements that visually identify the selected route.
  - The route color should remain consistent across the screen.
  - Do not use route colors as general-purpose UI colors. General UI elements should continue to use the application's global color tokens.

## Typography

The typography mirrors high-density investigative journals and technical dispatch terminals:

- **Display & Headlines (`Space Grotesk`):** Industrial, mechanical grotesque with deliberate ink-trap personality. Used for transit route numbers, large stop names (e.g., "GEARY & POWELL"), and structural section dividers. Always set tight with negative letter-spacing.
- **Body (`Inter`):** Neutral, hyper-legible workhorse for route descriptions, stop rosters, and customer advisories.
- **Data & Telemetry (`JetBrains Mono`):** Monospaced precision for countdown meters (`03 MIN`, `18:42:01 PST`), live vehicle IDs, vehicle load capacity indicators (`[||||  ] 62%`), and distance telemetry.

## Layout & Spacing

The layout is disciplined by strict editorial column rules and structural grid gutters, avoiding floating elements without anchor points.

### The Editorial Wire Grid
- **Mobile (Phone):** 4-column fluid layout with `16px` side margins and `12px` gutters. Every card, route sheet, and arrival item is bounded by explicit `1px` hairlines (`#262626`).
- **Tablet & Foldables:** 8-column layout. Split-pane layout with persistent interactive vector route schematic on the left (5 columns) and dynamic arrival board/live feed on the right (3 columns).
- **Rhythm:** An 8pt base grid controls all padding, with 4pt half-steps dedicated exclusively to dense data rows (e.g., line-by-line stop breakdowns).

## Elevation & Depth

This design system intentionally rejects diffuse ambient drop shadows in favor of **Structural Flat Depth** and **Crisp Outlines**:

1. **Hairline Framing:** Depth is defined through high-contrast boundary lines (`1px solid #262626` or `#FFFFFF` in accent moments).
2. **Layered Surfaces:** 
   - Base canvas: `#0A0A0A`
   - Active tracking card: `#181818`
   - Nested inspection sheets: `#202020`
3. **Map Overlay HUD:** Floating Android map UI uses `backdrop-filter: blur(12px)` combined with an 85% opaque `#121212` tint and a 1px `#333333` stroke to maintain high legibility over complex vector cartography.
4. **Tactile Offset Shadows:** When elevated components (such as floating quick-action filters or active bus pinch-sheets) require physical separation, use rigid brutalist hard-edge drops: `box-shadow: 4px 4px 0px #000000`.

## Shapes

The design system employs a **Sharp (`0px`)** architectural language for cards, data panes, modal sheets, and system dividers, evoking newspaper block grids and print broadsheets. 

The sole exception to the zero-radius rule is the **Pill Route Tag**:
- Route indicators, status pills, and alert badges use fully rounded radii (`9999px`) to create an immediate visual juxtaposition against the rigorous, razor-sharp grid cards. This contrast ensures route identifiers (e.g., `38`, `14R`, `1`) pop instantaneously in peripheral vision.

## Components

### Route Badges & Pill Tags
- Route badges are  pills using the route type's semantic color as the badge fill.
- **Normal Bus:** `route-bus`
- **Normal Light Rail:** `route-lightrail`
- **Rapid:** `route-rapid`
- **Express:** `route-express`
- **Late Night:** `route-latenight`
- Route number glyphs use `Space Grotesk Bold`.
- The selected route badge uses the same route type color as associated selected-route UI.
- Route colors represent route identity and should not be replaced by status colors.

### Arrival & Schedule Cards
- Bounded by top and bottom 1px `#262626` borders; no rounded card corners.
- Left-aligned bold route badge, followed by destination terminus in `Space Grotesk` uppercase (`GEARY BLVD / OCEAN BEACH`).
- Right-aligned countdown cluster set in `JetBrains Mono`: primary arrival highlighted in massive 24px bold; subsequent arrivals formatted as a compact telemetry array: `04 MIN / 11 MIN / 22 MIN`.

### Buttons & Interactive Controls
- **Primary Button:** Strict 0px rectangular corners, solid `#FAFAFA` fill with `#0A0A0A` heavy typography. On active touch, inverts instantaneously to `#CCFF00` with zero transition lag for tactile responsiveness.
- **Secondary / Action Strip:** Black fill, 1px `#FAFAFA` hairline border, mono text.
- **Android Floating Action Button (FAB):** Sharp square `48x48px` box with a 4px hard black offset shadow, containing directional crosshairs or route search icons.

### Inputs & Search Bars
- Underline-dominant or stark boxed input with `0px` radius.
- Background: `#181818`; border: 1px solid `#333333`; focused border: 2px solid `#FF3300`.
- Integrated mono prefix (`STOP ID # / LINE:`) serving as structural editorial labels.

### Transit Map Interface
- Minimalist dark cartography (monochrome landmasses, roads as subtle `#1F1F1F` veins).
- Muni routes rendered as crisp vector lines color-coded by transit class (Red for Rapid, Telemetry Blue for Cross-town, White for standard trunk lines).
- Real-time bus icons rendered as directional monospaced chevrons (`>`) enclosed in high-contrast circular or pill enclosures.
