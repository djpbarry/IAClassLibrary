# Revision Log

A chronological record of significant development decisions and changes to
IAClassLibrary, and — more importantly — the mistakes made and lessons learned,
so that the same modernisation applied to the sibling projects
(`TrackerLibrary`, `AdaptDataProcessing`, ADAPT) does not repeat them.

Git commit messages provide the fine-grained record; this file is the distilled,
dated narrative plus the "what not to do again" notes.

## Conventions

- Entries are dated and reference commits and/or `DEVELOPMENT_PLAN.md` phases.
- "Lesson" entries state what went wrong and the rule to apply next time.

---

## 2026-09-24 — Modernisation kick-off (M1, Phases A & B)

The first modernisation milestone landed in a single session. Decision history
lives in `DEVELOPMENT_PLAN.md` "Resolved decisions". The over-arching aim —
modernise a >10-year-old codebase, even at the cost of temporarily breaking
downstream consumers — takes precedence throughout.

| Commit | What |
|---|---|
| `f6f01df` | Added `AGENTS.md` (architecture, conventions, gotchas). |
| `d844d6e` | Added `DEVELOPMENT_PLAN.md` (six-phase roadmap). |
| `b987a80` | Pinned Java target to 11; finalised the Ant-removal and `LocationAgnosticBioFormatsImg` decisions. |
| `d447b3e` | Switched CI to the Maven wrapper; Temurin 11. |
| `afed15d` | Upgraded Java target to 21 and parent to `pom-scijava:45.1.0`. |
| `b3f0d99` | Deleted the legacy Ant build (`build.xml`, `nbproject/`). |
| `62a37a4` | Added the GPL-3.0-or-later `LICENSE`. |
| `c91dbb0` | Corrected licensing + Java version in plan and `pom.xml`; deferred source-header cleanup. |

Still outstanding from M1 (carried into the next session):

- Release `2.0.0` (pom is now `2.0.0-SNAPSHOT`) and tag `v2.0.0` (Decision 2 / B2).
- Remove/ignore the stray untracked `nb-configuration.xml` and `out/` (L7).

### 2026-09-24 — Version 2.0.0 and release automation

- Bumped `pom.xml` to `2.0.0-SNAPSHOT` (working toward the `2.0.0` release).
- Wired `maven-release-plugin` with `tagNameFormat=v@{project.version}` so a
  single `mvn release:prepare` moves version + tag + next-SNAPSHOT atomically.
- Replaced the pom-parsing version lookup with a manifest read:
  `Revision.getVersion()` reads `Implementation-Version`; `getVersionFromPom`
  is now `@Deprecated`.
- B3/Decision 3: confirmed TrackMate resolves to 8.0.0 via the parent; the
  `v2.0.0` tag (B2) is deferred to release time, and `TrackerLibrary`/ADAPT must
  bump to TrackMate 8 + Java 21 in lockstep (M6).

### 2026-09-24 — D1 dead/experimental code removal

- Deleted `MultiThreadedStarDist` (experimental: hardcoded Windows paths, empty
  catch block, `System.out.println` debug; zero in-repo references).
- Removed 60+ commented-out `IJ.saveAs(...)`/`System.out.println` debug lines
  with machine-specific paths across ~20 files.
- `@Deprecated`-tagged the 13 legacy `IAClasses` classes from Decision 4
  (`Region2`, `Region3D`, `RegionEdge`, `Pixel2`, `Gaussian3D`,
  `CrossCorrelation`, `FractalEstimator`, `DataStatistics`, `SkeletonProcessor`,
  `OnlyExt`, `ProgressDialog`, `StaticConstants`, `FluorescenceAnalyser`).
  Kept: `Region`, `Utils`, `BoundaryPixel`, `DSPProcessor`, `Pixel`.

---

## 2026-09-25 — Phase C: JUnit 5 harness + CSV golden tests

- Added JUnit 5 (Jupiter) test harness (`junit-jupiter-api`/`-engine`,
  parent-managed 5.13.4) and 20 passing tests across pure-logic classes
  (`Histogram`, `MSS`, `Rand`, `GenUtils`, `Smoother`, `Interpolator`,
  `ClusterablePoint`, `DataWriter` transforms) and CSV golden tests
  (`DataIOTest`, `TrajectoryAnalysis`). Phase C parked here; pure-logic
  extraction (D2) deferred until after the refactoring.
- **Bug found by golden tests:** `DataReader` leaked file handles — `CSVParser`
  (in `readCSVFile`/`readFileHeadings`) and `Scanner` (in `readTabbedFile`) were
  never closed, causing Windows "file in use" failures. Fixed with
  try-with-resources (`scan.close()` for the tabbed reader).

### 2026-09-25 — D2 dead-code cleanup (partial)

- Removed the large commented-out bodies in `BioFormatsImg.loadPixelData` (the
  old `MultiThreadedImageLoader` loading path).
- Deleted two IntelliJ "Commented out by Inspection" blocks — unused private
  `getLimits` (BioFormatsImg) and `getThreshold` (MultiThreadedMaximaFinder).
- Deferred the remaining decomposition (RegionGrower/MultiThreadedMaximaFinder
  god methods, the `terminal`/`intermediate` static state) until behavioural
  tests exist; the sigma/calibration helpers are already small/stateful and
  offer little safe pure-math to extract.

## 2024 — Bio-Formats loader consolidation

- `3815dad` added `LocationAgnosticBioFormatsImg` (the `Importer`/`ImportProcess`
  loader alongside the `ImageReader` loader in `BioFormatsImg`).
- `216117f` removed the internal references to it, which was later
  re-interpreted: the class is public API and must be kept (Decision 5). See L4.

## 2020 — Mavenisation

- `516ad2c` Mavenised the repo; `06b820b` renamed packages to match the
  `group/artifact` id; `5de2c02` made the POM SciJava-compliant.

## 2012 — Inception

- `9d39498` initial commit (`2012-07-06`). The legacy `IAClasses` package and
  the `Region`/`Pixel`/`Utils` primitives date from here and are the primary
  modernisation target.

---

## Lessons learned (mistakes to avoid in sibling projects)

### L1 — Decide the Java target *before* touching build/CI

The plan pinned Java 11 (`b987a80`) and reversed to Java 21 two commits later
(`afed15d`). The coordinated Java 21 move (Decision 3) was not settled upfront,
so CI and the parent POM were effectively done twice.

**Rule:** resolve the cross-project Java/parent-POM decision and version pins in
writing before any build or CI edit.

### L2 — Use consistent semver tag names

The only pre-existing tag is `v1.032`, a mislabel of `v1.0.32`, while `pom.xml`
says `1.0.37`. Consumers cannot trust tag names against POM versions.

**Rule:** use `vX.Y.Z` tags and never elide the patch zero; reconcile the
tag/version skew before tagging (Decision 2 / B2).

### L3 — Resolve the licence before tagging

`pom.xml` declared BSD-2 while ~half the sources carried GPL-3 headers and there
was no `LICENSE` file. Resolved to GPL-3.0-or-later.

**Rule:** add the root `LICENSE`, align `pom.xml` (`<licenses>`,
`license.licenseName`, `license.copyrightOwners`), and treat header tidy-up as a
separate, deferred item so it does not block tagging.

### L4 — "No references in this repo" ≠ "dead code"

A commit "Removed references to `LocationAgnosticBioFormatsImg`" was later
overridden by Decision 5: the class is public API, possibly used by external
projects, so it must be kept.

**Rule:** in a library, removing a public symbol requires an external-usage check
and a deprecation window, not just a grep of this repo.

### L5 — Check JitPack compatibility when moving the parent POM

An earlier `8e10706` ("Downgraded pom-scijava version for JitPack compatibility")
was later superseded by the upgrade to `45.1.0`.

**Rule:** verify the parent-POM version is consumable via JitPack before adopting
it; document any downgrade and its reason.

### L6 — Experimental code must not leak into `main`

`MultiThreadedStarDist` (hardcoded Windows paths, an empty catch block) and
dozens of commented-out `IJ.saveAs(...)` blocks with machine-specific paths are
still in `main`.

**Rule:** gate experiments behind a flag or keep them on a branch; strip debug
`saveAs`/`println` before merge (recoverable from git).

### L7 — Clean up *all* legacy IDE artifacts, not just the obvious ones

Deleting the Ant build removed `build.xml` + `nbproject/` but left an untracked
`nb-configuration.xml` and an untracked `out/` (IDE output).

**Rule:** after removing an IDE/legacy build, sweep for and remove or gitignore
the remaining config and output files.

### L8 — Pin the canonical branch explicitly

Work landed on the `development` branch while the plan still says "tag current
`master`" and `origin/HEAD` points at `master`.

**Rule:** name the canonical branch in the plan and keep `origin/HEAD` consistent
before tagging.

### L9 — Java LSP setup (parked, unresolved)

Attempted to add Eclipse JDTLS (`jdtls`) as a Java LSP for a reference-based
dead-code sweep. Installed to `H:\GitRepos\Java\jdtls` and configured in
`.crush.json`, but Crush never started it (`lsp_configured: java = not_started`,
no start attempt). Suspected cause: the project lives on `H:` while Python/JDK
live on `C:`, and Crush's LSP service fails to relativize the launcher path
across drives (`Error getting relpath: can't make C:\...\jdtls.py relative to
H:\...`).

**Parked for later.** Next attempts, in order: (1) put the LSP launcher and a
Python runtime on the same drive as the project; (2) drop `filetypes`/
`root_markers` and rely on the LSP-name convention; (3) enable `options.debug_lsp`
for verbose startup logs. Fallback for the dead-code sweep is IntelliJ's built-in
"unused declaration" inspection rather than a Crush LSP.
