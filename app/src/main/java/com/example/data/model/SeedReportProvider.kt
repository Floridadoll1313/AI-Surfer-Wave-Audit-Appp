package com.example.data.model

object SeedReportProvider {

    const val BASELINE_REPORT_ID = "otdaisurfer-ai-surfer-run-2"

    fun createBaselineReport(): LhrAuditReport {
        val commits = listOf(
            CommitFix(
                indexLabel = "01",
                category = AuditCategory.PERFORMANCE,
                commitHash = "730994b",
                title = "Preconnect critical third-party origins and size brand logos",
                description = "Added preconnect resource hints for Google Tag Manager and Stripe in index.html, and added explicit width and height attributes to the landing brand logos in SitesLanding.tsx and SiteLogoHeader.tsx to reduce layout shifts and speed up third-party connections.",
                auditImproved = "Performance · Largest Contentful Paint & Cumulative Layout Shift",
                targetSelectors = listOf(
                    "head > link[rel='preconnect']",
                    "img.landing-logo",
                    "img[data-site-emblem='true']",
                ),
                files = listOf(
                    "index.html",
                    "src/pages/home/SitesLanding.tsx",
                    "src/components/SiteLogoHeader.tsx",
                ),
                unifiedDiff = """
diff --git a/index.html b/index.html
--- a/index.html
+++ b/index.html
@@ -7,6 +7,8 @@
     <meta name="description" content="Ocean Tide Drop AI Surfer. Ride the Wave 🌊 Grow with AI." />
     <link rel="icon" type="image/png" href="./ocean_tide_logo.png" />
     <title>Ocean Tide Drop AI SURFER 🌊</title>
+    <link rel="preconnect" href="https://www.googletagmanager.com" crossorigin />
+    <link rel="preconnect" href="https://js.stripe.com" crossorigin />
 
     <!-- Google tag (gtag.js) -->
     <script async src="https://www.googletagmanager.com/gtag/js?id=G-PB5GTDQHXT"></script>
diff --git a/src/pages/home/SitesLanding.tsx b/src/pages/home/SitesLanding.tsx
--- a/src/pages/home/SitesLanding.tsx
+++ b/src/pages/home/SitesLanding.tsx
@@ -84,6 +84,8 @@
         <img
           className="landing-logo"
           src="/images/brand/ai-surfer-wide.webp"
+          width={360}
+          height={96}
           alt=""
           aria-hidden="true"
           data-homepage-logo="true"
diff --git a/src/components/SiteLogoHeader.tsx b/src/components/SiteLogoHeader.tsx
--- a/src/components/SiteLogoHeader.tsx
+++ b/src/components/SiteLogoHeader.tsx
@@ -10,6 +10,8 @@
         <img
           src="/images/brand/ai-surfer-wide.webp"
           alt="Ocean Tide Drop AI Surfer"
+          width={360}
+          height={96}
           data-site-emblem="true"
           style={styles.emblem}
         />
                """.trimIndent(),
                isSelected = true,
            ),
            CommitFix(
                indexLabel = "02",
                category = AuditCategory.PERFORMANCE,
                commitHash = "fc2ac3a",
                title = "Replace inline base64 JPEG in SitesLanding with static asset",
                description = "Removed the inline base64-encoded JPEG data URI from SitesLanding.tsx and referenced a static WebP asset with explicit width and height attributes to shrink the JavaScript bundle and avoid blocking hydration.",
                auditImproved = "Performance · Total Byte Weight & Script Evaluation",
                targetSelectors = listOf(".lead-leak-visual img"),
                files = listOf("src/pages/home/SitesLanding.tsx"),
                unifiedDiff = """
diff --git a/src/pages/home/SitesLanding.tsx b/src/pages/home/SitesLanding.tsx
--- a/src/pages/home/SitesLanding.tsx
+++ b/src/pages/home/SitesLanding.tsx
@@ -142,8 +142,10 @@
       <div className="lead-leak-visual">
         <img
-          src="data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD..."
+          src="/images/landing/lead-leak-preview.webp"
+          width={640}
+          height={420}
+          loading="lazy"
+          decoding="async"
           alt="Lead capture funnel preview"
         />
       </div>
                """.trimIndent(),
                isSelected = true,
            ),
            CommitFix(
                indexLabel = "03",
                category = AuditCategory.PERFORMANCE,
                commitHash = "77988c7",
                title = "Defer Stripe pricing script and size remaining landing images",
                description = "Deferred the Stripe pricing-table script in index.html and added explicit width and height attributes to the customer journey icons and founder image in SitesLanding.tsx to prevent cumulative layout shifts.",
                auditImproved = "Performance · Cumulative Layout Shift & Script Scheduling",
                targetSelectors = listOf(
                    "script[src*='pricing-table.js']",
                    ".lead-leak-journey img",
                    ".founder-journey-art img",
                ),
                files = listOf("index.html", "src/pages/home/SitesLanding.tsx"),
                unifiedDiff = """
diff --git a/index.html b/index.html
--- a/index.html
+++ b/index.html
@@ -14,7 +14,7 @@
     <link rel="preconnect" href="https://js.stripe.com" crossorigin />
-    <script async src="https://js.stripe.com/v3/pricing-table.js"></script>
+    <script defer src="https://js.stripe.com/v3/pricing-table.js"></script>
   </head>
diff --git a/src/pages/home/SitesLanding.tsx b/src/pages/home/SitesLanding.tsx
--- a/src/pages/home/SitesLanding.tsx
+++ b/src/pages/home/SitesLanding.tsx
@@ -208,12 +208,16 @@
         <div className="lead-leak-journey">
           <img
             src="/images/icons/wave-step.webp"
+            width={48}
+            height={48}
             alt="Customer journey step"
           />
         </div>
         <div className="founder-journey-art">
           <img
             src="/images/brand/founder-portrait.webp"
+            width={320}
+            height={320}
             alt="Ocean Tide Drop founder"
           />
         </div>
                """.trimIndent(),
                isSelected = true,
            ),
            CommitFix(
                indexLabel = "04",
                category = AuditCategory.ACCESSIBILITY,
                commitHash = "be7cb81",
                title = "Add dialog semantics and accessible labels to checkout modal",
                description = "Added role=\"dialog\", aria-modal=\"true\", aria-labelledby, and explicit label associations for the email and promo code inputs in App.tsx so screen readers and keyboard users can navigate the checkout modal.",
                auditImproved = "Accessibility · ARIA Dialogs & Form Labels",
                targetSelectors = listOf(
                    "div[role='dialog']",
                    "#checkout-email",
                    "#checkout-promo",
                ),
                files = listOf("src/App.tsx"),
                unifiedDiff = """
diff --git a/src/App.tsx b/src/App.tsx
--- a/src/App.tsx
+++ b/src/App.tsx
@@ -312,14 +312,26 @@
       {isCheckoutOpen && (
-        <div className="checkout-modal-backdrop">
-          <div className="checkout-modal">
-            <h2>Complete your AI Surfer plan</h2>
-            <input
-              id="checkout-email"
-              type="email"
-              placeholder="you@domain.com"
-            />
-            <input
-              id="checkout-promo"
-              type="text"
-              placeholder="Promo code"
-            />
+        <div className="checkout-modal-backdrop">
+          <div
+            className="checkout-modal"
+            role="dialog"
+            aria-modal="true"
+            aria-labelledby="checkout-modal-title"
+          >
+            <h2 id="checkout-modal-title">Complete your AI Surfer plan</h2>
+            <label htmlFor="checkout-email">Email address</label>
+            <input
+              id="checkout-email"
+              type="email"
+              autoComplete="email"
+              placeholder="you@domain.com"
+            />
+            <label htmlFor="checkout-promo">Promo code</label>
+            <input
+              id="checkout-promo"
+              type="text"
+              placeholder="Promo code"
+            />
           </div>
         </div>
       )}
                """.trimIndent(),
                isSelected = true,
            ),
            CommitFix(
                indexLabel = "05",
                category = AuditCategory.ACCESSIBILITY,
                commitHash = "3f55679",
                title = "Add navigation and main landmarks to DashboardLayout",
                description = "Replaced generic div containers with semantic aside, nav, and main landmarks in DashboardLayout.jsx and added aria-current=\"page\" to the active navigation button.",
                auditImproved = "Accessibility · Landmarks & Navigation State",
                targetSelectors = listOf(
                    "aside nav[aria-label='Dashboard navigation']",
                    "main",
                ),
                files = listOf("src/DashboardLayout.jsx"),
                unifiedDiff = """
diff --git a/src/DashboardLayout.jsx b/src/DashboardLayout.jsx
--- a/src/DashboardLayout.jsx
+++ b/src/DashboardLayout.jsx
@@ -24,18 +24,21 @@
   return (
     <div className="dashboard-shell">
-      <div className="dashboard-sidebar">
-        <div className="dashboard-nav">
+      <aside className="dashboard-sidebar">
+        <nav className="dashboard-nav" aria-label="Dashboard navigation">
           {navItems.map((item) => (
             <button
               key={item.id}
               type="button"
               onClick={() => onSelect(item.id)}
+              aria-current={activeId === item.id ? "page" : undefined}
             >
               {item.label}
             </button>
           ))}
-        </div>
-      </div>
-      <div className="dashboard-content">{children}</div>
+        </nav>
+      </aside>
+      <main className="dashboard-content">{children}</main>
     </div>
   );
 }
                """.trimIndent(),
                isSelected = true,
            ),
            CommitFix(
                indexLabel = "06",
                category = AuditCategory.BEST_PRACTICES,
                commitHash = "faf8570",
                title = "Fix malformed JSX quote attributes in Landing.jsx",
                description = "Removed duplicate trailing double quotes from the Link 'to' props in src/Landing.jsx so the component parses cleanly without JSX syntax errors.",
                auditImproved = "Best practices · Valid Markup & Error-Free Runtime",
                targetSelectors = listOf("a[href='/members/pricing']"),
                files = listOf("src/Landing.jsx"),
                unifiedDiff = """
diff --git a/src/Landing.jsx b/src/Landing.jsx
--- a/src/Landing.jsx
+++ b/src/Landing.jsx
@@ -58,9 +58,9 @@
       <div className="hero-cta-group">
-        <Link to="/members/pricing"" className="btn-primary">
+        <Link to="/members/pricing" className="btn-primary">
           Start riding the wave
         </Link>
-        <Link to="/members/pricing"" className="btn-secondary">
+        <Link to="/members/pricing" className="btn-secondary">
           View plans
         </Link>
       </div>
                """.trimIndent(),
                isSelected = true,
            ),
            CommitFix(
                indexLabel = "07",
                category = AuditCategory.SEO,
                commitHash = "135f500",
                title = "Add canonical URL, Open Graph tags, and JSON-LD schema",
                description = "Added a canonical link tag, Open Graph and Twitter Card metadata, and an Organization JSON-LD script in index.html to improve search indexing and rich link previews.",
                auditImproved = "SEO · Crawling, Indexing & Rich Metadata",
                targetSelectors = listOf(
                    "head > link[rel='canonical']",
                    "head > meta[property^='og:']",
                    "head > script[type='application/ld+json']",
                ),
                files = listOf("index.html"),
                unifiedDiff = """
diff --git a/index.html b/index.html
--- a/index.html
+++ b/index.html
@@ -16,5 +16,24 @@
     <meta name="description" content="Ocean Tide Drop AI Surfer. Ride the Wave 🌊 Grow with AI." />
+    <link rel="canonical" href="https://oceantidedrop.com/" />
+    <meta property="og:type" content="website" />
+    <meta property="og:title" content="Ocean Tide Drop AI SURFER" />
+    <meta property="og:description" content="Ride the Wave. Grow with AI." />
+    <meta property="og:url" content="https://oceantidedrop.com/" />
+    <meta name="twitter:card" content="summary_large_image" />
+    <meta name="twitter:title" content="Ocean Tide Drop AI SURFER" />
+    <script type="application/ld+json">
+      {
+        "@context": "https://schema.org",
+        "@type": "Organization",
+        "name": "Ocean Tide Drop AI Surfer",
+        "url": "https://oceantidedrop.com/"
+      }
+    </script>
   </head>
                """.trimIndent(),
                isSelected = true,
            ),
        )

        val failingAudits = listOf(
            LhrFailingAudit(
                id = "uses-rel-preconnect",
                title = "Preconnect to required origins",
                description = "Establish early connections to Google Tag Manager and Stripe CDN.",
                category = AuditCategory.PERFORMANCE,
                scorePercent = 42,
                displayValue = "Potential savings of 310 ms",
                selectors = listOf("head > link[rel='preconnect']"),
                linkedCommitHash = "730994b",
            ),
            LhrFailingAudit(
                id = "unsized-images",
                title = "Image elements have explicit width and height",
                description = "Set explicit width and height on brand logos and journey art to avoid CLS.",
                category = AuditCategory.PERFORMANCE,
                scorePercent = 50,
                displayValue = "CLS 0.24",
                selectors = listOf(
                    "img.landing-logo",
                    "img[data-site-emblem='true']",
                    ".lead-leak-journey img",
                    ".founder-journey-art img",
                ),
                linkedCommitHash = "730994b",
            ),
            LhrFailingAudit(
                id = "total-byte-weight",
                title = "Avoid enormous network payloads & inline base64 data URIs",
                description = "Inline base64 JPEG increased JS bundle evaluation and hydration blocking.",
                category = AuditCategory.PERFORMANCE,
                scorePercent = 48,
                displayValue = "1,420 KiB saved",
                selectors = listOf(".lead-leak-visual img"),
                linkedCommitHash = "fc2ac3a",
            ),
            LhrFailingAudit(
                id = "render-blocking-resources",
                title = "Defer offscreen / non-critical third-party scripts",
                description = "Stripe pricing-table.js blocked early parsing before defer attribute.",
                category = AuditCategory.PERFORMANCE,
                scorePercent = 64,
                displayValue = "Potential savings of 280 ms",
                selectors = listOf("script[src*='pricing-table.js']"),
                linkedCommitHash = "77988c7",
            ),
            LhrFailingAudit(
                id = "aria-dialog-name",
                title = "ARIA dialog and form input labels",
                description = "Checkout modal lacked role='dialog', aria-modal, and form input <label> links.",
                category = AuditCategory.ACCESSIBILITY,
                scorePercent = 68,
                displayValue = "3 failing nodes",
                selectors = listOf("div[role='dialog']", "#checkout-email", "#checkout-promo"),
                linkedCommitHash = "be7cb81",
            ),
            LhrFailingAudit(
                id = "landmark-one-main",
                title = "Document has semantic navigation and main landmarks",
                description = "DashboardLayout used generic divs instead of aside, nav, and main elements.",
                category = AuditCategory.ACCESSIBILITY,
                scorePercent = 72,
                displayValue = "2 missing landmarks",
                selectors = listOf("aside nav[aria-label='Dashboard navigation']", "main"),
                linkedCommitHash = "3f55679",
            ),
            LhrFailingAudit(
                id = "errors-in-console",
                title = "Valid JSX markup & error-free runtime",
                description = "Duplicate trailing quotes in Landing.jsx Link 'to' attributes.",
                category = AuditCategory.BEST_PRACTICES,
                scorePercent = 78,
                displayValue = "2 attribute syntax errors",
                selectors = listOf("a[href='/members/pricing']"),
                linkedCommitHash = "faf8570",
            ),
            LhrFailingAudit(
                id = "canonical",
                title = "Document has a valid rel=canonical and structured metadata",
                description = "Added canonical link, Open Graph, Twitter Cards, and Organization JSON-LD.",
                category = AuditCategory.SEO,
                scorePercent = 82,
                displayValue = "3 head metadata nodes",
                selectors = listOf(
                    "head > link[rel='canonical']",
                    "head > meta[property^='og:']",
                    "head > script[type='application/ld+json']",
                ),
                linkedCommitHash = "135f500",
            ),
        )

        return LhrAuditReport(
            id = BASELINE_REPORT_ID,
            repoSlug = "otdaisurfer/AI-Surfer-",
            runLabel = "Run 2 (Lighthouse JSON) · Step 3 / 3",
            targetUrl = "https://oceantidedrop.com/",
            fetchTime = "2026-11-20T09:15:00Z",
            lighthouseVersion = "12.4.0",
            strategy = "Mobile",
            categoryScores = mapOf(
                AuditCategory.PERFORMANCE to 94,
                AuditCategory.ACCESSIBILITY to 98,
                AuditCategory.BEST_PRACTICES to 100,
                AuditCategory.SEO to 100,
            ),
            failingAudits = failingAudits,
            commits = commits,
            isBundledBaseline = true,
        )
    }
}
