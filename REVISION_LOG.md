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

## 2026-10-02 — Phase G: collections (G3) + resource management (G4) + logging (G5)

Started the Java 21 modernisation survey (Phase G) and completed the three
low-risk items first, as sequenced in G7.

### G3 — collections, generics, and boxing

- Converted raw `new ArrayList()`/`new LinkedList()`/`new LinkedHashMap()` to
  diamond (`<>`) across ~35 sites, and explicit type args to diamond across
  ~20 sites (including the kept `IAClasses.Region`/`DSPProcessor`/`Utils`).
- Replaced deprecated boxing constructors (`new Integer(...)`/`new Double(...)`)
  with autoboxing/primitive casts in `DataStatistics`, `DSPProcessor`,
  `MultiThreadedMaximaFinder`, `CurveAnalyser`, and `DataWriter`.
- Parameterised two raw `List points = cluster.getPoints()` declarations to
  `List<Clusterable>` in `ZeroSlopeClusterOptimiser`/`ClusterablePointScore`.
- Deferred the "manual array growth/copy" item (`Pixel2.associations`,
  `DataStatistics` slice/copy loops) — a structural change in deprecated classes.

### G4 — try-with-resources (resource management)

- Converted all method-scoped readers/writers/streams to try-with-resources:
  `BioFormatsFileReader` (2× `ImageReader`), `BioFormatsFileLister`,
  `PropertyWriter` (`FileOutputStream` + a leaked `FileInputStream`),
  `FileReader` (3× `BufferedReader`, incl. an early-`return` leak),
  `GenUtils.readData`, `DataReader.readTabbedFile` (`Scanner`),
  `DataWriter` (2× `CSVPrinter`), `TrajectoryAnalysis` (2× `CSVPrinter`),
  `BioFormatsImageWriter` (2× `TiffWriter`),
  `FluorescenceAnalyser` (2× `PrintWriter`), and
  `MultiThreadedMaximaFinder` (2× process-drain `BufferedReader`).
- `BioFormatsImg`'s `ImageReader` is a field-lifetime resource (used across
  `setId`/`loadPixelData`/accessors), so a method-local try-with-resources does
  not apply; made the class `implements AutoCloseable` with a `close()` method.

### G5 — logging & error-handling normalisation

- Removed all `System.out`/`System.err` from `src/main`: `MultiThreadedMaximaFinder`
  (StarDist/ilastik) and `CurvatureEstimator` progress/debug now use `IJ.log`;
  the per-pixel `System.out.println("%d %d")` debug line was deleted.
- `Revision` and `MultiThreadedROIConstructor` now log via `GenUtils.logError`
  (which includes the exception) instead of `printStackTrace` / a bare message.
- Kept `GenUtils.logError`'s `printStackTrace()` (the canonical logger) and the
  intentional silent skip in `BioFormatsFileLister`.

### G8 — redundant-reimplementation survey

Reviewed the codebase for classes/methods that reimplement functionality already
in the JDK or a dependency. Highest-value findings:

- **`Math.Optimisation.Fitter.doFit()`** — hand-rolled Nelder–Mead simplex,
  inherited by five fitters → `commons-math3` `SimplexOptimizer`+`NelderMeadSimplex`.
- **`IAClasses.DSPProcessor.FFT`/`IFFT`** — hand-written Cooley–Tukey FFT →
  `commons-math3` `FastFourierTransformer`.
- **`IAClasses.DataStatistics`** — mean/std-dev/percentile/regression →
  `commons-math3` `StatUtils`/`DescriptiveStatistics`/`SimpleRegression` (also B4).
- **`UtilClasses.GenVariables`** charsets → `java.nio.charset.StandardCharsets`.
- **`IO.File.FileExtensionFilter`/`ImageFilter`/`IAClasses.OnlyExt`** → `FilenameUtils.isExtension`.
- **`IAClasses.Utils.arcTan`** → `Math.atan2`+`Math.toDegrees`; `calcDistance` → `Math.hypot`.
- **`Overlay.OverlayToRoi`** — explicitly copied from ImageJ `OverlayCommands`.
- **`DateAndTime.Time.getDuration`** — reinvents (and misimplements) `java.time.Duration.between`.

Full list (with "clearly redundant" / "partial overlap" / "internal duplication" /
"not redundant" verdicts) is in `DEVELOPMENT_PLAN.md` Phase G8.

**Done (2026-10-02, mechanical):** `Fitter.root2`→`Math.sqrt`; `Utils.calcDistance`→
`Math.hypot`; `Utils.arcTan`→`Math.atan2`+`Math.toDegrees`; `GenUtils.getDelimiter`→
`File.separator`; `FileExtensionFilter`→`FilenameUtils.isExtension`;
`FileReader.getParamsArray()`→`toArray`; `GenVariables` charsets→`StandardCharsets`.
Compile + tests green.

**Deferred (need an API change or further tests):** the `Utils`/`DataStatistics`
statistics, the string-join items (trailing-delimiter behaviour), and
`DateAndTime.Time.getDuration` (buggy; would change its public return type).

### Characterisation tests for the G8 math rewrites

Added two test classes (4 tests) to pin current behaviour before replacing the
optimiser/FFT:

- `IAClasses/DSPProcessorFFTTest` — asserts `FFT(double[])` matches Commons Math3
  `FastFourierTransformer` (forward), and that `IFFT` equals **N ×** Math3's
  inverse. **Key finding:** `DSPProcessor.IFFT` is *unscaled*, whereas Math3's
  `DftNormalization.STANDARD` inverse applies a `1/N` factor — so the rewrite must
  multiply Math3's inverse by `N` (or use a custom normalisation).
- `Math/Optimisation/IsoGaussianFitterTest` — fits a synthetic 2-D Gaussian and
  checks `mag`/`x0`/`y0`/`sigma` are recovered within tolerance.

**Rewrites done (2026-10-02):** `DSPProcessor.FFT`/`IFFT` now delegate to
`FastFourierTransformer` (forward unscaled; inverse `×N` to match the original's
unscaled inverse), and `Fitter.doFit()` now delegates to `SimplexOptimizer` +
`NelderMeadSimplex`. Public signatures unchanged; both characterisation tests pass.
`Utils.calcEuclidDist` → `ml.distance.EuclideanDistance` and `Utils.generateGaussian`
→ `distribution.NormalDistribution.density` (behind `UtilsTest`). `IAClasses.DataStatistics`
left as-is (deprecated — Decision 4).

### G6 — modern language features

- Converted `switch` statements to arrow/switch expressions across `GenUtils`,
  `MultiThreadedWatershed`, `Region`, `FluorescenceAnalyser`, `ImageNormaliser`,
  `InputFileOpener`, `OutputFolderOpener`, and `BioFormatsImageWriter`.
- Converted `instanceof` + cast to pattern matching in `Cell`, `ParticleWriter`,
  `MultiThreadedROIConstructor`, `ImageCorrelator`, `Utils`, and `PropertyExtractor`.
- Replaced the redundant `null`/`instanceof` guards in `Cell`/`Cell3D` `compareTo`/
  `compare` with `Objects.requireNonNull`; converted the two anonymous `Thread`s in
  `MultiThreadedMaximaFinder` to lambdas.
- Skipped (deliberately): records (API-breaking), `SpecifyInputsDialog`
  `AbstractAction`→lambda (`Action` is not a functional interface), `MacroWriter`
  text block (risky string change), `Utilities.getDate`→`java.time` (pattern
  syntax differs between `SimpleDateFormat` and `DateTimeFormatter`).

### G1/G2 — progress (Step 0 + Step 1 start)

- **Step 0 (characterise):** `MultiThreadedProcess`/`RunnableProcess`/`ProcessPipeline`
  contracts locked — `getOutput()` self-`start()`/`join()`s; `exec` is subclass-owned;
  `RunnableProcess` is a named `Thread` worker base.
- **Step 1 (D2):** the pure-math helpers were already extracted (`calcDistance`,
  `getThreshold`, `getMinFilArea`, `getMinCellArea`). Hardened static constants to
  `final`: `RegionGrower.lambda`/`filtRad` and `MultiThreadedMaximaFinder`'s ~30
  `propLabels` index constants + `BACKGROUND` (verified unassigned). Version → `2.0.3`.
- **Remaining (the real race):** `RegionGrower.terminal`/`intermediate` are `public
  static`, written once in `findCellRegions` and read by `RunnableRegionGrower`
  workers via static import — the highest-severity G2 item, needs passing as params
  or instance state.
- **Step 2 (tests):** added `RegionGrowerTest` (`calcDistance`, `getMinCellArea`)
  and `MultiThreadedProcessTest` (`constructOutputName`, `getOutput` duplicate,
  `outputDests` wiring). Version → `2.0.4`.

---

## 2026-09-27 — Release 2.0.1 (B2, M6 kick-off)

The first release attempt (version `2.0.0`, tag `v2.0.0`) failed in two places,
and both were the same class of issue the sibling `TrackerLibrary` had already
hit and fixed. Rather than force-move the broken tag (JitPack caches by ref
name), the fixes were applied and a fresh patch version `2.0.1` was cut.

- **CI failure (GitHub Actions, tag build):** the `v2.0.0` tag build failed
  while the same commit built green on `development`. The `development` and tag
  runs fired simultaneously on a cold Maven cache (the version bump changed the
  cache key) and had to re-resolve every dependency from only `scijava.public`
  and JitPack — `pom-scijava:45.1.0` drops the implicit Maven Central, so there
  was no `central` mirror to fall back on. One build won the race; the other hit
  a transient resolution failure. Fixed by declaring `central` explicitly (L11)
  and switching CI to `cache: 'maven'`.
- **JitPack failure:** JitPack builds on **JDK 8** by default; the Java 21
  project fails its own `RequireJavaVersion` enforcer rule. Fixed by adding
  `jitpack.yml` with `jdk: [openjdk21]` (L10).
- Also added `.gitattributes` (`mvnw` LF / `mvnw.cmd` CRLF) for Linux-CI
  robustness (L12), matching `TrackerLibrary`.

**Tag:** `v2.0.1` (fresh name — the broken `v2.0.0` is left as-is; do not re-use
it). JitPack coordinate: `com.github.djpbarry:iaclasslibrary:2.0.1`.

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

- Release + tag — done: `2.0.1` / `v2.0.1` (2026-09-27).
- Remove/ignore the stray untracked `nb-configuration.xml` and `out/` (L7).

### 2026-09-24 — Version 2.0.0 and release automation

- Bumped `pom.xml` to `2.0.0-SNAPSHOT` (working toward the `2.0.0` release).
- Wired `maven-release-plugin` with `tagNameFormat=v@{project.version}` so a
  single `mvn release:prepare` moves version + tag + next-SNAPSHOT atomically.
- Replaced the pom-parsing version lookup with a manifest read:
  `Revision.getVersion()` reads `Implementation-Version`; `getVersionFromPom`
  is now `@Deprecated`.
- B3/Decision 3: confirmed TrackMate resolves to 8.0.0 via the parent;
  `TrackerLibrary`/ADAPT must bump to TrackMate 8 + Java 21 in lockstep (M6).
  (The release landed as `v2.0.1` — see the 2026-09-27 entry.)

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

### 2026-09-25 — D5 gotcha fixes (4 of 5)

- Set `validID = true` in `BioFormatsImg.setId` so `isValidID()`/`getInfo()` work.
- Documented `clearImageData()` as an intentional no-op and `getLoadedImage()` as
  returning the internal image (aliasing) rather than a copy.
- Removed the duplicate `BioFormatsImg` import in `MultiThreadedProcess` and
  now-unused `ij.ImageStack`/`MultiThreadedImageLoader` imports in `BioFormatsImg`.
- Deferred: `@Override`/diamond-generics pass (tedious, no behavioural value).

### 2026-09-25 — D6 error-handling normalisation

- Fixed the two empty catch blocks: `runStarDist` now logs via
  `GenUtils.logError`, and `BioFormatsFileLister` notes the intentional skip.
- Converted `System.out`/`System.err` error logging to `GenUtils.logError`/`IJ.log`
  in `GenUtils.createDirectory`, `CurvatureEstimator`, `FluorescenceAnalyser`,
  and `ImageCorrelator`. Remaining `System.out.println` is progress/debug in the
  experimental StarDist/Ilastik and `CurvatureEstimator` loops (left as-is).

### 2026-09-25 — D4 image-loading documentation

- Added class-level Javadoc to `LocationAgnosticBioFormatsImg` clarifying the
  `Importer`/`ImportProcess` (location-agnostic) vs filesystem `ImageReader`
  distinction, and updated `AGENTS.md` to record the Decision 5 "kept" outcome.

### 2026-09-25 — Phase E documentation

- Expanded `README.md` from a one-liner to a proper overview (build, consuming,
  package map, license).
- Added class-level Javadoc to the key ADAPT-consumed public API: `BioFormatsImg`,
  `MultiThreadedProcess`, `RegionGrower`, `UserVariables`.
- Added a GitHub Actions workflow (`.github/workflows/javadoc.yml`) that generates
  Javadoc and publishes it to GitHub Pages (`target/reports/apidocs`), so agents
  updating downstream projects can point at `djpbarry.github.io/IAClassLibrary/`
  rather than the raw source. Needs the one-time Pages source set to
  "GitHub Actions".

### 2026-09-25 — Downstream cross-check fixes

- Fixed a Javadoc bug: `Particle.getFrameNumber()` claimed to return "z-position
  within an image stack" but returns the frame/time index (`t`).
- Documented **B4**: `TrackerLibrary` must migrate off the deprecated
  `IAClasses.DataStatistics` (→ commons-math3 `DescriptiveStatistics`) and
  `IAClasses.ProgressDialog` (→ ImageJ native progress) before re-pinning `v2.0.1`.

### 2026-09-25 — Package namespace decision (Decision 7)

- Decided to rename the package root `net.calm.iaclasslibrary` →
  `io.github.djpbarry.iaclasslibrary` (and Maven `groupId` `net.calm` →
  `io.github.djpbarry`), since `net.calm` was based on a non-existent domain.
  Kept the project/`artifactId` name. Executed in lockstep with the sibling
  repos during the v2.0.1 hand-off (Phase F / M6), not solo.

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

### L10 — Pin the JDK in `jitpack.yml` for Java 21 projects

JitPack's default build image runs **JDK 8**. A Java 21 project (after the
`pom-scijava:45.1.0` move) fails JitPack's build at the enforcer's
`RequireJavaVersion` rule — not because the code is wrong, but because the runner
is on `1.8.0_292`. The failure only surfaces on the first real JitPack request,
since the local `mvnw verify` runs on the developer's JDK 21.

**Rule:** any Java 21 (or otherwise non-default-JDK) project consumed via JitPack
must carry a `jitpack.yml` with `jdk: [openjdk21]` (or the matching version). Add
it in the same pass as the Java 21 / parent-POM move, before cutting the release
tag. IAClassLibrary missed this because its earlier (pre-Java-21) commits built
`ok` on JitPack, so the gap only surfaced on the first Java 21 tag.

Corollary: **JitPack caches build results by ref name.** Force-moving a tag does
not reliably invalidate the cached `ref → commit` mapping, so a "fixed" tag can
keep serving the stale failed build. If a release tag fails on JitPack, cut a
fresh tag (next patch version) rather than force-moving the broken one.

### L11 — `pom-scijava:45.1.0` drops the implicit Maven Central repo

The parent POM no longer provides Maven Central, so a project that declares its
own `<repositories>` (as IAClassLibrary does, for `scijava.public` and JitPack)
must also declare `central` explicitly. Without it, resolution relies solely on
the flaky `maven.scijava.org` and JitPack mirrors — fine on a warm local
`~/.m2`, but fragile in CI when the version bump busts the Maven cache key and
concurrent builds race to re-resolve from the thin mirror set.

**Rule:** when moving to `pom-scijava:45.1.0`, declare
`<id>central</id><url>https://repo.maven.apache.org/maven2</url>` explicitly in
`<repositories>` in the same pass. `TrackerLibrary` hit and fixed this first (its
"M1 deviation 1").

### L12 — Add `.gitattributes` to pin wrapper line endings

The Maven wrapper shell script (`mvnw`) must be LF on Linux CI and `mvnw.cmd`
must be CRLF on Windows. Without a `.gitattributes` forcing this, a Windows
checkout can commit `mvnw` with CRLF endings that break the Linux runner.

**Rule:** commit a `.gitattributes` with `mvnw text eol=lf` and
`mvnw.cmd`/`*.bat text eol=crlf` alongside the wrapper, and verify `mvnw` is
tracked with the executable bit (`git ls-files -s mvnw` shows `100755`).

### L13 — Field-lifetime resources need `close()`/`AutoCloseable`, not try-with-resources

The Bio-Formats `ImageReader` in `BioFormatsImg` is held as a field and used
across many methods (`setId`, `loadPixelData`, accessors), so it cannot be
wrapped in a method-local try-with-resources. The correct fix is to make the
owning class `implements AutoCloseable` and expose a `close()`.

**Rule:** distinguish method-scoped resources (try-with-resources) from
object-lifetime resources (implement `AutoCloseable` + `close()`); never just
ignore a field-held reader/stream because it "can't be wrapped in a try".

### L14 — Version via Conventional Commits (bump on every change)

The agreed scheme is Conventional Commits, with `<version>` in `pom.xml` bumped on
every code change — `fix`/`refactor`/`chore`/`docs`/`test` → patch, `feat` → minor,
breaking → major — and no `-SNAPSHOT` suffix. After the manual `2.0.1` release the
pom was left pinned at `2.0.1`, so a batch of commits carried no version movement.

**Rule:** bump the version on every change (patch/minor/major per the commit type);
never leave the pom on a `-SNAPSHOT` or a stale version.
