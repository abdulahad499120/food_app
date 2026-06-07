# Comprehensive Screen Architecture & UI/UX Product Requirements

This document establishes the absolute layout specifications, interactive components, data dependency rules, and micro-animations captured across the reference videos. It serves as the master blueprint for achieving feature parity.

---

## 1. Global Navigation & Structural Shell

### 1.1 Persistent App Bottom Navigation Bar
*   **Item Count:** 4 fixed tabs.
*   **Tab Layout:**
    1.  **Home:** Navigates to primary promotional and account discovery hub.
    2.  **Order:** Routes to the master multi-view item catalog.
    3.  **Gift:** Launches the eGift card purchase marketplace and design grid.
    4.  **Rewards:** Opens the gamified loyalty tier and point redemption overview dashboard.
*   **Visual State Anchors:** Selected tab utilizes the absolute primary brand color for both the vector graphic icon and label typography, while unselected tabs remain a muted gray.

### 1.2 Persistent Menu Footer Component (Store Selection Strip)
*   **Trigger Context:** Mounts exclusively on screens under the `Order` tab system (Catalog, Category Lists, Detail Sheets).
*   **Visual Structure:** A full-width sticky footer anchored directly above the global bottom navigation bar.
*   **Left-hand Element:** Displays an inline text label with a contextual chevron icon indicator (`Choose a store` or the active branch string name).
*   **Right-hand Element:** A floating green circular cart bubble badge enclosing a vector shopping bag asset and a numeric text counter tracking items in the current active session basket.

---

## 2. Screen-by-Screen Architectural Breakdown

### 2.1 Screen 01: Home Landing Hub
*   **Top App Bar Infrastructure:**
    *   **Left Element:** Text anchor `Sign in` prompting user session initialization.
    *   **Center Element:** A geographical location pin vector adjacent to the textual label `Stores`—acts as a global shortcut mapping to the full-screen location finder overlay.
    *   **Right Element:** A minimalist circular profile/avatar vector outline container routing to explicit account preference structures.
*   **Dynamic Scrollable Content:**
    *   **Primary Promotional Hero Unit:** Full-bleed card containing wide aspect-ratio food/beverage imagery, a dominant seasonal headline ("Summer's here. Dive right in."), an inviting body-copy block, and an explicit primary CTA button labeled `Explore the summer menu`.
    *   **Loyalty Conversion Module:** A full-width contrasting promotional block branded "STARBUCKS REWARDS" embedded with inline action buttons (`Join now` / `Sign in`) driving core user onboarding metrics.

### 2.2 Screen 02: Store Locator & Mapbox Fulfillment Matrix
*   **Header Selection Interface:** 
    *   A clean segmented control or pill-based layout pinning the core transactional channels: `[Pickup]` and `[Delivery]`.
    *   **Delivery Context Integration:** Selecting delivery updates the layout canvas to display a collaborative fulfillment graphic ("Today deserves delivery"), powered natively via integrated partner logistic frameworks (e.g., DoorDash).
*   **Full-Screen Geographic Map Interface:**
    *   **Map System Engine:** Employs an interactive gesture-responsive tile sheet allowing absolute pan, pinch-to-zoom, and map rotation capabilities.
    *   **Floating Spatial Utilities:**
        *   An upper-right magnifying glass search vector anchor triggering predictive inputs.
        *   A contextual geo-location arrow vector targeting user real-time spatial positioning.
        *   An interactive filter pill utility isolating store amenities or logistical constraints.
*   **Predictive Search Overlay View:**
    *   Triggered immediately upon clicking search inputs; exposes a text entry area with a trailing explicit cancel cross button (`X`).
    *   Generates a vertical list structure partitioned by historical text queries (`Recent Searches`) and structural location auto-complete suggestions (e.g., "Lahore Zoo Safari").
*   **Spatial "Search This Area" Trigger Control:**
    *   A floating pill button dynamically appearing at the top center of the map pane *only* when the map camera undergoes camera shifts or pan gestures away from the current location bounds.
*   **Contextual Store Card Bottom Overlay Sheets:**
    *   Slides up from the bottom boundary upon selecting a map marker pin.
    *   **Data Layout:** Features the absolute branch identifier name, distance scale indicator (e.g., "0.2 mi"), an explicit operation badge marked with text and dynamic color parameters (`Closed` / `Open`), and comprehensive weekday operational timetable logs.
    *   **Interactive Controls:** Dual balanced outlined actions labeled `Call store` and `Get directions`.

### 2.3 Screen 03: Master Menu Catalog
*   **Sub-Navigation Tab Menu Structure:**
    *   A horizontal scrolling text bar sub-segmented into four operational data filters: `[Menu]`, `[Featured]`, `[Previous]`, and `[Favorites]`.
*   **The "Featured" View Grid Layout:**
    *   Organized via nested scroll boundaries. Vertical scroll contains multiple horizontal scroll carousels.
    *   **Visual Node Hierarchy:** Circular imagery cards enclosing cropped recipe item photography, item naming strings, and a secondary right-aligned text label string `See all X` providing a structural pathway to that clean category grid.
*   **The "Menu" Nested Category Tree View:**
    *   A vertical scrolling list grouping the broader menu classification boundaries into separate sections:
        *   **Section: Drinks** (Subcategory items: Trending, Protein Beverages, Hot Coffee, Cold Coffee, Matcha, Hot Tea, Cold Tea, Refreshers).
        *   **Section: Food** (Subcategory items: Breakfast, Bakery, Treats, Lunch, Lite Bites).
        *   **Section: At Home Coffee** (Subcategory items: Whole Bean, Starbucks VIA Instant).

### 2.4 Screen 04: Filtered Category Grid List View
*   **Navigation Header:** Clean top bar mounting a standard back arrow vector layout next to the localized title tracking the selected category depth (e.g., "Trending (5)" or "Lite Bites (26)").
*   **Grid Specification:** A perfectly balanced two-column vertical `VerticalGrid` array layout.
*   **Product Card Node Architecture:**
    *   An elevated circular item image container.
    *   A descriptive text label mapping item name attributes.
    *   **Frictionless Cart Adder Utility:** An floating circular inline button wrapper hosting a generic `+` icon positioned in the upper right quadrant of the image asset. Clicking this pushes the raw item unit directly to the basket context *without* forcing sheet detail navigation, provided store context is satisfied.

### 2.5 Screen 05: Deep Product Customizer & Configuration Sheet
*   **Media Section:** Implements a smooth scrolling view. The hero element hosts a crisp high-res item render, an overlay export icon button vector, and an absolute close icon button (`X`) popping the modifier sheet off the view stack.
*   **Nutritional Metadata Context:** Displays the explicit title block string, item name string, and a live tracking numeric caloric string indicator value (e.g., "140 calories"). Features an inline diagnostic info badge tracking nutritional sheets.
*   **Store Fulfillment Constraint Banner:** A warning panel displaying an alert vector alongside the text string: `Choose a store to see availability`.
*   **Size Configuration Selector Matrix:**
    *   A horizontally aligned row of custom vector silhouettes rendering uniform cup assets of scaling heights.
    *   **Interactive Items:** Renders unique option tags (e.g., Tall - 12 fl oz, Grande - 16 fl oz, Venti - 24 fl oz, Trenta - 30 fl oz).
    *   **Reactive State Rule:** Selecting a size pill triggers a recalculation mapping to the caloric metadata readout at the header level (e.g., selecting Grande scales display to 140 calories, switching to Tall drops it to 90 calories, while Venti scales it to 200 calories). Selected states use a bold ring container.
*   **Standard Component Recipe Block ("What's Included"):**
    *   A clean vertical card list mapping out the baseline configuration formulas of the selected drink product (e.g., Lemonade options, Caffeine volumes).
    *   **Tappable Interaction Layer:** Tapping a default recipe card pops open a native full-width standard selection dialog sheet listing option metrics (e.g., `Extra Lemonade`, `No Lemonade`, `Light Lemonade`, `Lemonade [Standard]`). The active recipe profile item uses a contrasting background selection pill.
*   **Add-in Step Modifiers Matrix:**
    *   Provides explicit user configuration dials for non-standard inclusions (e.g., "Mango-Pineapple Pearls").
    *   **Structure:** Displays item labels alongside horizontal interactive steppers consisting of distinct decremental circle markers (`-`), a central bold quantitative state integer, and incremental circle markers (`+`).
*   **Micro-Action "Reset to Standard Recipe" Button:**
    *   A contextual, clean button text link that programmatically forces all active modifier indices back to factory baseline recipe settings.
*   **Persistent Bottom Action Area:** Encloses a wide solid green horizontal CTA bar mapping the literal text string `Add to order`.

### 2.6 Screen 06: Authentication & Security Onboarding Engine
*   **Sign-Up Context Matrix:**
    *   **Header:** Clear close control layout (`X`) above a prominent bold header title ("Sign up").
    *   **Input Fields Structure:** Vertical sequence of clean text input blocks processing: First Name, Last Name, Email Address, and Password fields.
    *   **Gift Card Accordion Menu:** An expandable drop-down header option element tracking the text query `Have a Starbucks gift card? (optional)`. Clicking this morphs the view tree to expose two dedicated fields parsing: `Card number` and `Security code`.
*   **Dynamic Password Integrity Validation Checker:**
    *   An interactive, live-updating validation checklist component rendering four security criteria bounds. Each criteria node maps a status validation cross marker (`X`) in a standard reddish tint when invalid, transforming instantly to a green checkmark asset upon satisfying specific regex rules:
        1.  `Between 8 and 25 characters`
        2.  `At least one number`
        3.  `At least one capital letter and one lower case letter`
        4.  `At least one special character such as exclamation point or comma`
*   **Legal Policy Consent Matrix:**
    *   A vertical list array of interactive custom toggle checkboxes monitoring optional marketing newsletters, accelerated biometric access bindings (`I'd like to sign in faster via Face ID`), and mandatory Terms of Use alignments. 
    *   **Validation Rule:** The primary green `Continue` CTA action remains locked or handles verification errors until the user checks the mandatory terms box.

### 2.7 Screen 07: eGift Marketplace & Procurement Flow
*   **Utility Selection Deck:** A top horizontal twin-card structure offering action blocks to manually attach physical card balances (`Got a gift card? Add it here`) or instantiate collaborative orders (`Start group gifting`).
*   **Design Catalog Layout:** Displays a vertical scrolling feed of horizontal card grids classified by occasion tag banners (e.g., Miffy x Starbucks, Graduation, Birthday, Thank You, Celebration).
*   **Gift Procurement Customizer Form:**
    *   Launches upon selecting a design asset card; embeds a clean preview of the chosen card aesthetic at the top margin.
    *   **Form Field Elements:** Parses text string metrics including `Recipient name`, a customized spinner/dropdown mapping `Gift amount` denominations (e.g., $25.00), a `Recipient email` text input layer, an interactive directory linkage button (`Choose from email contacts`), a `Sender name` input, and an optional `Personal message` note form with a hard 160-character counter check rule.
    *   **Dual Bottom Action Row:** Horizontal layout stacking an outlined secondary `Pay` button right next to a full-bleed primary green action button labeled `Checkout`.

### 2.8 Screen 08: Gamified Rewards Loyalty Tiers Screen
*   **Loyalty Mechanics Panel:** Renders an explanatory, scrollable system deck describing core program concepts ("How it works: Collect Stars, Enjoy free treats, Unlock benefits").
*   **Visual Status Threshold Matrix:**
    *   A horizontal-paged sliding carousel showcasing distinct loyalty tier structures based on annual performance criteria:
        *   **Green Status:** Less than 500 stars earned per year. Unlocks explicit reward variables (e.g., Birthday treats, Free Mod Mondays).
        *   **Gold Status:** 500+ stars earned per year. Unlocks advanced parameters (e.g., Stars won't expire, 7 days to redeem treats).
        *   **Reserve Status:** 2,500+ stars earned per year. Unlocks premium elite options (e.g., Access to exclusive global coffee experiences).
*   **Star Milestone Redemption Ledger:**
    *   A vertical list catalog displaying exactly what menu assets correspond to given point thresholds, displaying descriptive icon art rows:
        *   `25★` = $1 off a drink customization (espresso shot, syrup, cold foam).
        *   `60★` = Up to $2 off your order item.
        *   `100★` = Brewed coffee or tea, bakery items, packaged snacks up to $6 value.
        *   `200★` = Handcrafted drink or hot breakfast up to $10 value.
        *   `300★` = Sandwich, protein box, or packaged coffee up to $16 value.
        *   `400★` = Select Starbucks merchandise up to $20 value.

---

## 3. Global Interstitial System Modals (Failsafe Intercepts)

### 3.1 Modal A: Store Selection Prerequisite Intercept
*   **Trigger Condition:** Evaluates automatically when a user clicks the quick `+` add button on a product item node from a grid menu layout *before* any active store location context has been bound to the session state.
*   **UI Architecture:** Dimmed backdrop overlay mounting a centered rounded alert box container.
*   **Text Specifications:**
    *   **Header:** `Skip to menu?`
    *   **Body:** `We won't be able to show what items are in stock until you select a store.`
*   **Control Matrix:** Dual horizontal button row grouping an outlined button `Choose a store` right next to a contrasting filled button option labeled `Yes` (which bypasses validation but leaves items un-orderable).

### 3.2 Modal B: Transactional Cart Enforcement Intercept
*   **Trigger Condition:** Evaluates automatically if a user navigates deeper into a category catalog view, attempts to execute an additive item manipulation to their checkout cart, but has bypassed the store boundary.
*   **UI Architecture:** High-contrast blocking alert window.
*   **Text Specifications:**
    *   **Header:** `Choose a store before continuing to cart`
*   **Control Matrix:** Stacks an outlined secondary control layout `Cancel` directly adjacent to a solid colored confirmation choice item `Choose a store` which hooks explicitly back into the locator navigation routing tree.

### 3.3 Modal C: Guest Identity Blockade Intercept
*   **Trigger Condition:** Evaluates programmatically when an unauthenticated guest session user attempts to initialize a premium account flow transaction (such as initiating group eGift transactions).
*   **UI Architecture:** An elegant slide-up modal panel anchoring the lower edge boundary.
*   **Text Specifications:**
    *   **Header Title:** `Are you a Rewards member?`
*   **Control Matrix:** Vertical command stack providing an option line `Sign in to Rewards` positioned directly above a prominent primary option item labeled `Join now`.
