---
name: Commercial Print Engine
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#434655'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#747686'
  outline-variant: '#c4c5d7'
  surface-tint: '#2251da'
  primary: '#0037b1'
  on-primary: '#ffffff'
  primary-container: '#1f4fd8'
  on-primary-container: '#ccd4ff'
  inverse-primary: '#b7c4ff'
  secondary: '#9c4500'
  on-secondary: '#ffffff'
  secondary-container: '#ff7a1b'
  on-secondary-container: '#5e2700'
  tertiary: '#005122'
  on-tertiary: '#ffffff'
  tertiary-container: '#006c30'
  on-tertiary-container: '#89ec9b'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dce1ff'
  primary-fixed-dim: '#b7c4ff'
  on-primary-fixed: '#001551'
  on-primary-fixed-variant: '#0039b5'
  secondary-fixed: '#ffdbca'
  secondary-fixed-dim: '#ffb68e'
  on-secondary-fixed: '#331200'
  on-secondary-fixed-variant: '#773300'
  tertiary-fixed: '#95f8a7'
  tertiary-fixed-dim: '#79db8d'
  on-tertiary-fixed: '#00210a'
  on-tertiary-fixed-variant: '#005323'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style
The design system establishes a high-reliability, utility-driven commercial experience tailored for the Sri Lankan on-demand print industry. It addresses micro-businesses, corporate procurement officers, and creative agencies requiring transparent pricing, rapid turnarounds, and precise production status tracking.

The aesthetic follows Modern Material 3 conventions grounded in structural clarity and purpose. It eliminates decorative flourishes—such as purple gradients, complex blurs, and glassmorphism—in favor of crisp surfaces, intentional micro-elevations, tactile feedback, and definitive typographic hierarchy. Visual authority comes from structured data presentation, clear status badges, and an operational, print-shop-floor precision adapted for mobile Android touch interaction.

## Colors
The color hierarchy is optimized for daylight legibility and quick scanning of dense transactional details.

- **Primary (`#1F4FD8`)**: Industrial deep blue applied strictly to primary actions, active bottom navigation states, focused field strokes, and critical order progress indicators.
- **Accent / Secondary (`#FF7A1A`)**: Warm vibrant orange used sparingly for urgency highlights, special commercial deals, promo badges, and the active printing status tier.
- **Tertiary (`#15803D`)**: Functional green reserved for success states, payment confirmations, and the "Ready for Pickup/Delivery" milestone.
- **Background & Canvas (`#F6F7FB`)**: Cool, desaturated neutral slate canvas that prevents eye fatigue while providing high contrast against surface cards.
- **Surface (`#FFFFFF`)**: Pure crisp white for cards, bottom sheets, app bars, and dialogs.
- **Text Layers**:
  - Primary Content: High-contrast dark slate (`#0F172A`) for titles, body copy, and currency amounts.
  - Secondary Content: Muted slate (`#64748B`) for form labels, timestamps, and secondary specifications.
  - Subtle / Placeholder: Slate (`#94A3B8`) for placeholder inputs, inactive icons, and structural divider strokes (`#E2E8F0`).

### Production Lifecycle Status Matrix
- **Pending**: Container `#FEF9C3`, Content `#854D0E`
- **Processing**: Container `#DBEAFE`, Content `#1D4ED8`
- **Printing**: Container `#FFEDD5`, Content `#C2410C`
- **Ready**: Container `#DCFCE7`, Content `#15803D`
- **Completed**: Container `#F1F5F9`, Content `#475569`
- **Cancelled / Error**: Container `#FEE2E2`, Content `#B91C1C`

## Typography
Typography is anchored entirely in `Inter` to provide structural rhythm across complex configuration screens (paper weight, laminate finish, sizing, volume tiers). 

- Currency figures in Sri Lankan Rupees are always typeset using the medium or semibold weights (e.g., `Rs. 2,500`) with a non-breaking space between the currency code and numeric digits to prevent awkward wrapping.
- Numerical print counts, batch weights, and measurements (e.g., `350gsm`, `3.5" x 2.0"`) rely on tabular numeric figures to maintain vertical baseline alignment across cost breakdowns.
- Headlines are tight and confident; body scales prioritize reading comfort for dense multi-line print guidelines and upload checklists.

## Layout & Spacing
The layout uses a 4-column fluid mobile grid anchored by an 8dp/4dp baseline grid system designed for standard Android viewports (360dp to 412dp widths).

- **Outer Margins**: `16px` (`margin`) standard padding along screen edges ensures primary content clears device bezels and gesture insets.
- **Component Padding**: Interactive cards use `16px` internal padding, with a compact `12px` option for dense list cards.
- **Vertical Hierarchy**:
  - Micro-spacing (`4px` to `8px`) links label tags to inputs, price units to descriptions, or badges to order titles.
  - Section spacing (`16px` to `24px`) groups functional clusters such as file uploaders, paper spec chips, and delivery selectors.
  - Page block separation (`24px` to `32px`) distinguishes primary product parameters from total calculation summaries and fixed bottom app bar docks.

## Elevation & Depth
Depth relies on physical Android card surfaces cast over the cool neutral `#F6F7FB` base canvas. 

- **Resting Surface (Level 1)**: Primary cards and module groups utilize a pure white background `#FFFFFF` with a dual-layer tinted shadow: `box-shadow: 0 4px 20px -2px rgba(31, 79, 216, 0.06), 0 2px 6px -1px rgba(0, 0, 0, 0.04)`. The ambient primary-blue tint prevents gray wash-out on modern OLED displays.
- **Interactive Elevated (Level 2)**: Floating action buttons, active search drawers, and elevated card selections shift to: `box-shadow: 0 8px 24px -4px rgba(31, 79, 216, 0.12), 0 4px 8px -2px rgba(0, 0, 0, 0.06)`.
- **Modals & Bottom Sheets (Level 3)**: Fixed order checkouts and spec sheets use a top-oriented soft scrim shadow combined with an Android modal backdrop: `rgba(15, 23, 42, 0.40)`.
- Borders are kept minimal: structural outlines use `#E2E8F0` at 1px thickness; inputs use `#CBD5E1`.

## Shapes
The shape hierarchy matches standard Material 3 ergonomics tuned for handheld comfort:

- **Cards & Primary Modules**: Strict `16px` border-radius (`rounded-lg`), establishing clear tactile grouping for job tickets, spec panels, and product tiles.
- **Buttons, Action Bars & Input Fields**: `12px` border-radius (`rounded-md`), balancing finger targets with structural stability.
- **Badges, Filter Chips & Segment Controls**: Fully pill-shaped (`9999px`) or `8px` (`rounded-sm`), immediately distinguishing meta-information from clickable cards and input targets.

## Components

### Buttons
- **Primary Action**: Solid `#1F4FD8` fill, white `#FFFFFF` text, `font-weight: 500`, `font-size: 16px`. Full-width where applicable, minimum height `52px`, `12px` border-radius. Active state produces a deep blue ripple (`#173DA8`).
- **Secondary Action**: Transparent background with a `1.5px` border in `#1F4FD8` and text in `#1F4FD8`. Minimum height `52px`, `12px` border-radius.
- **Destructive Action**: Container `#FEE2E2`, text `#B91C1C`. Used for order cancellations and asset deletions.

### Input Fields
- **Container**: Outlined style with minimum height of `48px`, `12px` border-radius, background `#FFFFFF`.
- **Default State**: Border `1px solid #CBD5E1`, text `#0F172A`, placeholder `#94A3B8`.
- **Focused State**: Border `2px solid #1F4FD8`, label transition to `#1F4FD8`.
- **Error State**: Border `1.5px solid #B91C1C`, accompanied by an error icon and helper text in `#B91C1C` (`13px regular`).

### Status Badges & Chips
- **Status Badges**: Inline badges using `13px` medium text, `6px 10px` internal padding, and an `8px` corner radius. Colored strictly by current production lifecycle state (e.g., Printing uses `#FFEDD5` container with `#C2410C` text).
- **Selection Chips (Material/Finish selectors)**: Unselected states feature a `#FFFFFF` fill with `#CBD5E1` border and `#0F172A` text. Selected states feature a `#DBEAFE` container with `#1F4FD8` border, bolded `#1F4FD8` text, and a check icon.

### Cards
- **Product & Job Cards**: Pure white `#FFFFFF` surface, `16px` border-radius, and the ambient tinted shadow. Internal horizontal split separates visual proof thumbnails, specification metadata, job stage status badges, and formatted LKR price totals (`Rs. X,XXX`).

### Checkboxes & Radio Buttons
- High-contrast selectors. Unselected borders `#94A3B8`. Selected fill `#1F4FD8` with white checkmark/inner radio circle. Minimum touch target padded to `48px x 48px`.

### File Upload Dropzone
- Dashed stroke (`1.5px dashed #94A3B8`) on a `#FFFFFF` surface with `16px` radius. Features a cloud-upload icon in `#1F4FD8`, format recommendations (PDF, AI, CDR - max 50MB), and instant thumbnail validation badges upon upload.