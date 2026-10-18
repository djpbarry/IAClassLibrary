# IAClassLibrary Development Plan

This plan outlines a multi-phase effort to make IAClassLibrary more robust,
maintainable, and consumable as a library, while resolving the issues flagged by
the ADAPT project's `DEVELOPMENT_PLAN.md` (which treats IAClassLibrary as one of
its three upstream JitPack dependencies). It is grounded in a full review of this
repository as of the plan's writing.

## Overarching aim

**Modernise a codebase whose core is over a decade old.** Modernisation takes
precedence over backward compatibility with downstream consumers: ADAPT,
`TrackerLibrary`, and `AdaptDataProcessing` will themselves be updated and
modernised in a later, coordinated pass. Where a choice is between a cleaner,
more maintainable design and preserving a legacy API, prefer the cleaner design.

- Deprecate legacy symbols rather than preserving them indefinitely.
- Prefer deletion of dead/experimental code over keeping it "just in case"
  (it is recoverable from git).
- Record every decision, mistake, and lesson in
  [`REVISION_LOG.md`](REVISION_LOG.md) so the same modernisation of the sibling
  projects does not repeat them.
- Keep this plan and `REVISION_LOG.md` in sync: after any repo change, review and
  update one or both in the same pass (mark phases/milestones done, record
  decisions, log lessons).

## Current state (context for the plan)

- IAClassLibrary (`net.calm.iaclasslibrary`) is a **Java library** (not a
  runnable plugin) in the ImageJ/Fiji ecosystem, providing image-analysis
  primitives consumed by ADAPT, `TrackerLibrary`, and `AdaptDataProcessing`.
- **Build:** Maven, parent `org.scijava:pom-scijava:45.1.0`, version
  `2.0.27` (patch-bumped per commit; last release `2.0.26` / tag `v2.0.26`), declared license
  **GPL-3.0-or-later** (`license.licenseName=gpl_v3`). *(Reconciled 2026-09-24;
  released 2026-09-27.)*
- **CI:** `.github/workflows/maven.yml` runs `./mvnw --batch-mode
  --update-snapshots verify` on **JDK 21** with the `setup-java` Maven cache.
  *(Updated 2026-09-27.)*
- **Tests:** JUnit 5 (Jupiter) harness added — `src/test/java`, 16 classes,
  37 tests as of 2026-10-03 (incl. FFT/Fitter/Utils/RegionGrower/MultiThreadedProcess/
  RiemannianDistanceTransform tests). No lint/format tooling. (Phase C done for now.)
- **License:** a GPL-3.0-or-later `LICENSE` file now exists and `pom.xml` is
  corrected from BSD-2 to GPL-3. Source headers remain inconsistent (roughly
  half GPL-3, roughly a third NetBeans "change this header" stubs); header
  tidy-up is deferred (Decision 1). *(Reconciled 2026-09-24.)*
- **Legacy build:** `build.xml` + `nbproject/` have been **deleted** (Decision 6).
  The stray `nb-configuration.xml` (NetBeans config) and `out/` directory (IDE
  build output) are gitignored (`.gitignore`), not tracked. *(Reconciled 2026-09-24.)*
- **Structure:** ~100+ Java files across ~40 subpackages under
  `net.calm.iaclasslibrary`, split between a legacy `IAClasses` package and newer
  refactored packages (`Cell`, `Cell3D`, `Particle`, `Process`, `IO`, `ImgLib2`,
  `Math`, etc.).

### Key architectural facts

- **Two image representations coexist:** ImageJ `ImagePlus`/`ImageStack`
  (dominant, used across `IO`, `Process`, `Cell`, `IAClasses`) and ImgLib2 `Img`
  (only in `ImgLib2`, `ViewMaker`, and some converters). Know which paradigm a
  file belongs to before editing.
- **Two Bio-Formats loaders:** `IO.BioFormats.BioFormatsImg` (wraps
  `ImageReader`/`ImporterOptions`/`IMetadata`) and
  `IO.BioFormats.LocationAgnosticBioFormatsImg` (wraps `Importer`/`ImportProcess`).
  Kept (Decision 5) and documented (D4) — it is public API.
- **Process pipeline:** `Process.MultiThreadedProcess` (extends `Thread`,
  implements `Callable<BioFormatsImg>`) is the base for all processing steps,
  wired together by `Process.ProcessPipeline`. Workers follow a
  `MultiThreadedX` + `RunnableX` pairing.
- **Domain model:** `Cell.Cell` extends `Cell.CellRegion` (ImageJ `Roi` +
  `ImageStatistics`); `Particle.Particle` extends TrackMate `Spot`. `Cell3D`
  parallels this in 3D.
- **Dependencies are all used:** TrackMate (`Spot`), MorphoLibJ (`inra.ijpb`),
  mcib3d-core, imagescience (Hessian), ImgLib2, SCIFIO, Bio-Formats, and
  commons-csv are each referenced in source. No obviously unused dependency.

### Verified gotchas (from the code review — all fixed in D1/D5)

- `BioFormatsImg.validID` is **never set to `true`**, so `isValidID()` always
  returns `false`.
- `BioFormatsImg.clearImageData()` is a **no-op** (body commented out).
- `BioFormatsImg.getLoadedImage()` returns the **internal** `ImagePlus` directly
  (duplicate logic commented out) — callers share a mutable reference.
- `MultiThreadedProcess` has a **duplicate import** of `BioFormatsImg`.
- `Particle` **shadows** TrackMate `Spot`'s `x`/`y`; `getiD()` and `t` (frame)
  use non-conventional naming.
- `MultiThreadedStarDist` (see Phase D) contains **hardcoded Windows paths and an
  empty catch block** — experimental debug code that leaked into `main`.
- Widespread **commented-out debug code** (`IJ.saveAs(...)` to
  `C:\Users\...`/`E:\Debug\...` paths) across `RegionGrower`, `MultiThreadedWatershed`,
  `MultiThreadedMaximaFinder`, `CurvatureEstimator`, `StairsFitter`, etc.

---

## Phase A — Build & CI modernization

**Status: A1–A4 complete (2026-09-24).** The `.orig` backups were already
absent; the stray `nb-configuration.xml` and `out/` still need removing/ignoring
(see REVISION_LOG L7).

### A1. Add a Maven wrapper

Pin a reproducible toolchain by committing `mvnw`/`mvnw.cmd` + `.mvn/wrapper/`
so builds don't depend on an unknown system Maven (mirrors ADAPT's A1).
*(2026-10-04: the wrapper is now the jar-less `only-script` type (maven-wrapper
3.3.2, Maven 3.9.9), so no `maven-wrapper.jar` is needed; see L16.)*

### A2. Harden CI

`.github/workflows/maven.yml` currently runs a single `mvn verify` on JDK 11.

1. Use the wrapper (`./mvnw verify`) instead of a system `mvn`.
2. Cache Maven dependencies (`actions/cache` on `~/.m2/repository`).
3. Pin the Java target to 21 (`scijava.jvm.version=21`, Decision 3) and
   build on JDK 21.
4. Split future `build` and `test` jobs once Phase C lands.

### A3. Add a `.gitignore`

`.gitignore` only excludes `/build/`, `/dist/`, `/target/`. Add `.idea/`,
`*.iml`, and OS files. (`.idea/` is currently an untracked directory in the
working tree.)

### A4. Delete the legacy Ant build (Decision 6)

Delete `build.xml` (with its hardcoded Perl `-pre-init` hook), `nbproject/`,
and the `.orig` backups. They are machine-specific and non-portable, and not
part of the Maven build. Recoverable from git if the NetBeans workflow is ever
revived.

---

## Phase B — License & metadata (blocks ADAPT's Phase G1)

**Status (2026-09-27): B1 items 1–3 are done** (LICENSE added, `pom.xml`
corrected to GPL-3); B1 item 4 (header tidy-up) is deferred; **B2:** done —
released `2.0.1` and tagged `v2.0.1` (see B2 below). **B3:** resolved by
Decision 3 (TrackMate 8.0.0 via the parent); cross-repo coordination pending
(M6).

The ADAPT plan's Phase G1/G2 assumes IAClassLibrary just needs to "verify its
LICENSE file" and be tagged. The reality is more involved:

### B1. Resolve the license (Decision 1)

There is **no `LICENSE` file**, `pom.xml` declares BSD-2, yet most newer source
files carry GPL-3 headers and older files carry NetBeans stubs. This is the same
unresolved GPL-3-vs-BSD-2 contradiction ADAPT resolved in its Decision 1.

1. Confirm the intended license with the maintainer (GPL-3.0 is consistent with
   the newer headers and with ADAPT's choice; BSD-2 is also defensible and
   GPL-3-compatible).
2. Add the corresponding root `LICENSE` file.
3. Correct `pom.xml` (`<licenses>`, `license.licenseName`, and
   `license.copyrightOwners` = Francis Crick Institute / David Barry).
4. **Deferred (future tidy-up).** The source headers need cleaning up at some
   point: the NetBeans "change this header" stubs and other pre-GitHub-era
   boilerplate headers are largely historical (this is a ~15-year-old project)
   and are candidates for removal/simplification rather than one-for-one
   replacement. Leave for a later pass; not blocking the current work.

### B2. Tag the repo (blocks ADAPT's Phase G2 / Decision 2)

**Done (2026-09-27).** ADAPT pinned IAClassLibrary at commit `fe92f24c6e`, after
the only tag `v1.032` (`da53d73`). The release is now cut: version `2.0.1`, tag
`v2.0.1`, published to JitPack as `com.github.djpbarry:iaclasslibrary:2.0.1`.

The first attempt (`2.0.0` / `v2.0.0`) failed on both CI and JitPack — see
`REVISION_LOG.md` 2026-09-27 and Lessons L10–L12. In brief: the tag build failed
because `pom-scijava:45.1.0` drops the implicit Maven Central (fixed by declaring
`central`), and the JitPack build failed because JitPack defaults to JDK 8 (fixed
with `jitpack.yml` → `openjdk21`). A fresh `2.0.1` tag was cut rather than
force-moving `v2.0.0` (JitPack caches by ref name). The broken `v2.0.0` tag is
left as-is and must not be reused.

### B3. Resolve the TrackMate version web (blocks ADAPT's Phase G3)

**Resolved by Decision 3 (2026-09-24).** IAClassLibrary declares
`sc.fiji:TrackMate` **unversioned** (parent-managed); `pom-scijava:45.1.0`
manages it to **8.0.0** (verified). This supersedes the earlier "7.x for now"
note (the coordinated Java 21 / TrackMate 8.x move is now in progress).
Remaining: `TrackerLibrary` (7.10.0) and ADAPT (7.14.0) must bump to 8.0.0 and
Java 21 in lockstep (Phase F / M6).

### B4. Downstream migration off deprecated `IAClasses` (blocks the v2.0.1 re-pin)

Cross-checking `TrackerLibrary`'s `net.calm.iaclasslibrary.*` usage against the
v2.0.0-SNAPSHOT Javadoc found two legacy classes still in use by downstream code:

- `IAClasses.DataStatistics` (3× in `ParticleTrajectory.java`) — migrate to
  `org.apache.commons.math3.stat.descriptive.DescriptiveStatistics` (already
  imported in the same file).
- `IAClasses.ProgressDialog` (`TrajectoryBuilder.java`, `TrajectoryBridger.java`)
  — migrate to ImageJ's native progress.

This is a documented prerequisite for `TrackerLibrary` (and any other consumer)
to re-pin to `v2.0.1`, since both classes are `@Deprecated` (Decision 4) and
slated for removal in a later major version.

---

## Phase C — Introduce tests (the biggest maintainability win)

**Status: done (2026-09-25; extended 2026-10-03).** JUnit 5 harness wired
(`junit-jupiter-api`/`-engine`, parent-managed 5.13.4); 37 tests green across 16
classes — pure-logic (`Histogram`, `MSS`, `Rand`, `GenUtils`, `Smoother`,
`Interpolator`, `ClusterablePoint`, `DataWriter` transforms), CSV golden tests
(`DataIOTest`, `TrajectoryAnalysis`), plus FFT/Fitter/Utils/RegionGrower/
`MultiThreadedProcess` tests. Deferred: extracting pure logic from god methods (D2).

1. **Start with pure-logic, static-method classes** (no ImageJ runtime needed):
   - `Math/Histogram`, `Math/MSS`, `Math/Rand`
   - `Math/Clustering/*` (`ClusterablePoint`, `StairsFitter`,
     `ZeroSlopeClusterOptimiser`)
   - `DataProcessing/Interpolator`, `DataProcessing/Smoother`
   - `Binary/BinaryMaker`, `Binary/EDMMaker`
   - `IAClasses/DataStatistics`, `IAClasses/OnlyExt`
   - `UtilClasses/GenUtils` pure helpers (`differentiate`, `checkRange`,
     `checkFileSep`)
2. **CSV I/O golden tests** — `IO/DataReader`/`IO/DataWriter` and
   `Trajectory/TrajectoryAnalysis` use commons-csv; assert the output
   schema/headings are stable (catches silent schema drift).
3. **Extract pure logic from god methods** before testing them (see Phase D2).
   `RegionGrower`'s `getThreshold`/`getSeedPoints` and the
   `MultiThreadedProcess` sigma/calibration helpers are extractable pure parts.

---

## Phase D — Code hygiene & refactoring

### D1. Remove experimental/dead code (do first)

**Status: done (2026-09-24).**

- `Process/Segmentation/MultiThreadedStarDist.java` is **debug code**: hardcoded
  paths (`E:/Debug/Giani/pipeline_test/`,
  `C:/Users/davej/GitRepos/Python/stardist/...`), a hardcoded Windows
  `cmd.exe /C activate.bat` pipeline, `System.out.println` debug, and an empty
  `catch (InterruptedException | IOException e) {}`. Remove it (or extract behind
  a proper, parameterised interface — but it is not production-ready as-is).
- Strip the many commented-out `IJ.saveAs(...)`/`System.out.println` debug blocks
  with machine-specific paths (`RegionGrower`, `MultiThreadedWatershed`,
  `MultiThreadedMaximaFinder`, `CurvatureEstimator`, `CurveAnalyser`,
  `StairsFitter`). They are recoverable from git.

### D2. Decompose the largest methods

**Status: partially done (2026-09-25).** Removed the large commented-out bodies
in `BioFormatsImg.loadPixelData` (old `MultiThreadedImageLoader` path) and
deleted two IntelliJ "Commented out by Inspection" dead-code blocks (unused
private `getLimits` and `getThreshold`). The remaining decomposition — breaking
up `RegionGrower`/`MultiThreadedMaximaFinder` god methods and resolving the
`terminal`/`intermediate` static mutable state — is deferred until behavioural
tests exist (the sigma/calibration "pure math" is already in small helpers or
trivial, so there is little safe extraction to do without tests).

`RegionGrower` (400+ lines, static mutable state `terminal`/`intermediate`),
`MultiThreadedMaximaFinder` (560+ lines), and `BioFormatsImg.loadPixelData`
(contains large commented-out bodies) are the largest, most intertwined units.
Extract single-responsibility private/static helpers and move pure math out so
Phase C can test it.

### D3. Resolve legacy vs. refactored duplication (Decision 4)

The legacy `IAClasses` package (`Region`, `Region2`, `Region3D`, `Pixel`,
`Pixel2`, `BoundaryPixel`, `Utils`, `DataStatistics`, `FractalEstimator`, etc.)
overlaps conceptually with the newer `Cell`, `Cell3D`, and `Particle` packages.
Decide the canonical home for each concept and deprecate/remove the legacy
versions, or explicitly document which to use where. `RegionGrower` already
marks some methods `@Deprecated` — extend that discipline.

### D4. Document the image-loading surface (Decision 5)

**Status: done (2026-09-25).** Added a class-level Javadoc to
`LocationAgnosticBioFormatsImg` explaining the `Importer`/`ImportProcess` vs
filesystem `ImageReader` distinction, and updated the `AGENTS.md` note to drop
the stale "may be on its way out" remark in favour of the Decision 5 "kept"
resolution.

`LocationAgnosticBioFormatsImg` is **kept** (public API, possibly used by
external projects). Document its role relative to `BioFormatsImg` (location-
agnostic `Importer`/`ImportProcess` loading vs filesystem `ImageReader` loading)
in `AGENTS.md` and Javadoc so there is one clear way to open each kind of input.

### D5. Fix the verified gotchas

**Status: 4 of 5 done (2026-09-25).**

- [x] Set `validID = true` in `setId` (makes `isValidID()`/`getInfo()` meaningful).
- [x] Document `clearImageData()` as an intentional no-op (clearing caused GUI problems).
- [x] Document `getLoadedImage()` as returning the internal image (aliasing), not a copy.
- [x] Remove the duplicate `BioFormatsImg` import in `MultiThreadedProcess`.
- [ ] Add `@Override` and diamond generics where missing (deferred — tedious, low value).

Also removed now-unused imports in `BioFormatsImg` (`ij.ImageStack`,
`MultiThreadedImageLoader`) exposed by the D2 cleanup.

### D6. Normalise error handling

**Status: done (2026-09-25).** Fixed the two empty catch blocks (added an
actionable `GenUtils.logError` in `runStarDist` and a "skip it" comment in
`BioFormatsFileLister`) and converted `System.out`/`System.err` error logging to
`GenUtils.logError`/`IJ.log` in `GenUtils.createDirectory`, `CurvatureEstimator`,
`FluorescenceAnalyser`, and `ImageCorrelator`. Remaining `System.out.println`
calls are progress/debug output in the experimental `runStarDist`/`runIlastik`
and `CurvatureEstimator` loops (out of scope — D1 territory).

Replace empty catch blocks and mixed `System.out.println`/`IJ.log` logging with
the existing `GenUtils.logError`/`GenUtils.error` pattern, and give
`catch (Exception)` blocks actionable messages.

---

## Phase E — Documentation

**Status: done (2026-09-25).** Expanded `README.md` (overview, build, consuming,
package map, license); kept `AGENTS.md` in sync; and added class-level Javadoc to
the key ADAPT-consumed public API (`BioFormatsImg`, `MultiThreadedProcess`,
`RegionGrower`, `UserVariables`).

- **`README.md`** is currently a single `# IAClassLibrary` line. Expand it with:
  what the library is, how to build (`mvn verify`), how it is consumed (JitPack,
  by ADAPT/TrackerLibrary/AdaptDataProcessing), and the package map.
- **`AGENTS.md`** already documents architecture, conventions, and gotchas. Keep
  it in sync as Phase D lands.
- **Javadoc:** older files have minimal or no class/method Javadoc; add it to the
  public API of the packages consumed by ADAPT (`IO.BioFormats`, `Process`,
  `Segmentation.RegionGrower`, `UserVariables`).

### E-follow-up — Javadoc content & HTML hygiene (deferred, 2026-10-04)

Phase E only added class-level Javadoc to four classes. Two follow-up items were
identified but deferred (no code changes yet):

1. **Content enrichment** — most public methods carry empty/generated Javadoc
   (`@param`/`@return` with no text, NetBeans "change this header" stubs). Add
   contract-level docs for the ADAPT-consumed surface: units (µm via OME
   `Length`), nullability, return-value ownership (copy vs. aliased reference),
   blocking/thread-safety, and frame-vs-time semantics. Migrate the `AGENTS.md`
   "Gotchas" into the actual Javadoc so they travel with the API.
2. **HTML hygiene** — ~95 `@author` Javadoc tags contain `Name <email>`; the
   angle brackets are parsed as (malformed) HTML and break the published Javadoc
   ("invalid input"). Escape them as `&lt;`/`&gt;` (or drop the obfuscated
   email). The license-header `<http://www.gnu.org/licenses/>` is *not* affected —
   it sits in a non-Javadoc `/* ... */` comment and is never rendered.

---

## Phase F — Upstream coordination (with the ADAPT plan)

IAClassLibrary is itself an upstream dependency, so several items here are
**prerequisites** for ADAPT's `DEVELOPMENT_PLAN.md` Phase G / M10:

| ADAPT plan item | IAClassLibrary action | This plan |
|---|---|---|
| G1 license fix | B1 | add LICENSE, correct pom, normalise headers |
| G2 tag all three | B2 | done — tagged `v2.0.1` (see B2) |
| G3 TrackMate version web | B3 | single TrackMate version policy |
| G0 "no CI" | A2 | CI already exists; harden it |

Coordinate the Java-target decision (Decision 3) with ADAPT's Decision 3.
IAClassLibrary now targets **Java 21** (parent `pom-scijava:45.1.0`);
`TrackerLibrary`, `AdaptDataProcessing`, and ADAPT must bump to Java 21 in
lockstep and re-pin their IAClassLibrary dependency to `v2.0.1`.

---

## Phase G — Modern Java modernisation (Java 21)

**Status: done (2026-10-04).** G1–G9 complete — G1 (threading), G2 (static state),
G3 (collections/generics/boxing), G4 (try-with-resources), G5 (logging), G6
(language features), G8 (redundant reimplementations: Tiers 1–3 done, Tier 4
verified/deferred, Tier 5 deferred), G9 (RiemannianDistanceTransform). The core is over
a decade old and predates most of the language/API features now available on the
Java 21 target. A full-pass review found systematic opportunities to improve
efficiency, performance, readability, and thread-safety without changing public
behaviour. Items are ordered by risk/impact: thread-safety and resource leaks
first, mechanical language upgrades last. Each item lists the concrete sites
found.

**Guardrail — public API stays unchanged.** Throughout Phase G no public method
signature, return type, field type, or class is changed or removed. Where a
change would alter the API, `@Deprecate` the old symbol and add a new one instead
(e.g. `DateAndTime.Time.getDuration` would gain a new `Duration`-returning method
rather than changing its current return type). This tightens Decision 0.

### G1. Replace the hand-rolled `Thread` model with `java.util.concurrent`

The processing pipeline is built on `Thread` subclasses with manual
`start()`/`join()`:

- `Process.MultiThreadedProcess` (`extends Thread`, self-`start()`/`join()` in
  `getOutput()` at `MultiThreadedProcess.java:121-133`) is the base of ~15
  subclasses; it already holds an unused `ExecutorService` (`:39`).
- `Process.RunnableProcess` (`extends Thread`) is a second base for ~8 workers.
- `Process.DistanceTransform.RiemannianDistanceTransform` hand-rolls four
  `StepNThread` inner classes with manual `start()`/`join()` barriers
  (`:65-117`, `:201`, `:268`, `:333`, `:395`).
- `Process.Colocalise.MultiThreadedColocalise` uses raw `Thread[]` +
  `start()`/`join()` (`:152-183`, `:248`, `:292`).
- `Process.IO.MultiThreadedImageLoader` allocates an `Executors` pool then
  bypasses it (`:62-70`), calling `start()` on `RunnablePixelLoader`.
- `Extrema.MultiThreadedMaximaFinder` spawns anonymous `Thread`s to drain
  `ProcessBuilder` output (`:439`, `:514`).

Plan (revised 2026-10-02): keep `MultiThreadedProcess`/`RunnableProcess`
`extends Thread` (public-API freeze); modernise the internal worker threads to
`ExecutorService`/`ForkJoinPool` and use **virtual threads** (Java 21) for the
I/O- and process-bound stages. See the G1/G2 execution plan below. Do this only
after the D2 extraction + Phase C tests exist, so behavioural equivalence can be
asserted.

### G2. Eliminate static mutable state (thread-safety)

Shared mutable `static` fields are data-race hazards for a library whose
processing is multi-threaded:

- `Segmentation.RegionGrower` — `public static short terminal`/`intermediate`
  plus `lambda`/`filtRad` (`RegionGrower.java:51-53`), mutated by
  `RunnableRegionGrower` workers via static import. **Highest severity.**
  **Done (2026-10-03):** `terminal`/`intermediate` removed; computed locally and
  passed as constructor params (`lambda`/`filtRad` already `static final`).
- `Extrema.MultiThreadedMaximaFinder` — ~30 `static` prop-label index constants
  (`:70-109`) — **done (2026-10-03):** already `static final` (immutable indices,
  not config); per-run state is instance fields.
- `Process.MultiThreadedProcess.OUTPUT_SEP`, and per-class label/feature index
  constants across `MultiThreadedWatershed`, `MultiThreadedColocalise`,
  `MultiThreadedGaussianFilter`, `MultiThreadedTopHatFilter`,
  `MultiThreadedROIConstructor`, `SpotFeatures`, `Particle.COLOCALISED`.
- `Trajectory.TrajectoryAnalysis` (`:48-63`) — **done (2026-10-03):** 13 static
  fields → instance (`inputFile`, `minVel`, `minDist`, `framesPerSec`,
  `smoothingWindow`, `smooth`, `interpolate`, `labelledData`, `INPUT_*`;
  `minPointsForMSD` → `final`).
- `Trajectory.DiffusionAnalyser.plotLegend` (`:36`) — **N/A (global by design):**
  `plotLegend`/`msdPlot`/`globalMSD` form the population-MSD-plot accumulator,
  exposed via `public static getMsdPlot()`; left static (same decision as
  TrackerLibrary's `ParticleTrajectory`).
- `Graph.Dijkstra.index` (`:30`, unused), `GenUtils.maxline` (`:36`),
  `IAClasses.SkeletonProcessor.branchpoint` (`:21`).

Plan: convert to instance fields / configuration objects / method parameters;
make true constants `final`. Do this before or alongside G1 so concurrency is
actually safe.

### G1/G2 — execution plan (ordered)

**Decision:** keep `MultiThreadedProcess`/`RunnableProcess` `extends Thread`
(public-API freeze); modernise only the internal worker threads. Revisit a
`Runnable`-based base at the next major version.

Order: D2 decompose → behavioural tests → G2 static state → G1 threading.

0. **Characterise** — read `MultiThreadedProcess`, `RunnableProcess`, and the
   pipeline subclasses to lock their contracts (`setup`/`run`/`duplicate`/
   `getOutput`).
1. **D2 — decompose** — `RegionGrower`: extract `getThreshold`/`getSeedPoints`/
   region-growth core; move `terminal`/`intermediate`/`lambda`/`filtRad` out of
   static scope. `MultiThreadedMaximaFinder`: extract config parsing + local-max
   detection.
2. **Behavioural tests** — cover the extracted RegionGrower helpers, MaximaFinder
   config/labels, and `MultiThreadedProcess` mechanics (`getOutput` duplicate,
   `outputDests` wiring, `duplicate()`).
3. **G2 — static state (safe-first):** (a) delete unused statics (`Dijkstra.index`,
   `SkeletonProcessor.branchpoint`); (b) mark true constants `final` (`OUTPUT_SEP`,
   label/feature constants, `SpotFeatures`, `Particle.COLOCALISED`); (c) config
   statics → instance fields (`MultiThreadedMaximaFinder` ~30 already `final`
   constants; `TrajectoryAnalysis` done; `DiffusionAnalyser.plotLegend` N/A —
   global by design); (d)
   `RegionGrower.terminal/intermediate/lambda/
   filtRad` → instance/params — **done** (terminal/intermediate removed; passed
   as params).
4. **G1 — threading:** (a) `MultiThreadedProcess`/`RunnableProcess` route work
   through the existing `exec` / a managed pool — **done (2026-10-03):** added
   `MultiThreadedProcess.runWorkers(Runnable[], String)` + unit test; (b)
   `RiemannianDistanceTransform` 4 inner `Thread`s → `ExecutorService` + `Future`
   — **done (2026-10-03):** single fixed pool + `runWorkers` helper;
   (c) `MultiThreadedColocalise` raw `Thread[]` → `ExecutorService` — **done
   (2026-10-03):** `calcNucParticleDistances`/`calcNearestNeighbours` now use
   `runWorkers`; (d)
   `MultiThreadedImageLoader` use its executor — **done (2026-10-03):** now
   routes `RunnablePixelLoader`s through `runWorkers` instead of raw
   `start()`/`join()`; (e) `MultiThreadedMaximaFinder`
   process-drain → virtual threads — **done (2026-10-03):** StarDist/ilastik
   drain threads now `Thread.ofVirtual()`.

**G1 complete (2026-10-03):** Steps 4a–4e all landed; worker execution is now
pool/virtual-thread based while `MultiThreadedProcess`/`RunnableProcess` remain
`extends Thread`.

Each step compiles + tests green; record progress in `REVISION_LOG.md`.

### G3. Collections, generics, and boxing

**Status: done (2026-10-02).** Raw types, explicit type args, and deprecated
boxing constructors converted; raw `List` declarations parameterised. The
"manual array growth/copy" item (`Pixel2.associations`, `DataStatistics`
slice/copy loops) is **deferred** — a structural change in deprecated classes,
not a mechanical one.

- **Raw types (~35 sites):** `new ArrayList()`/`new LinkedList()`/
  `new LinkedHashMap()` across `Process.MultiThreadedProcess`, `IO.DataReader`,
  `IO.FileReader`, `Extrema.MultiThreadedMaximaFinder`,
  `Trajectory.TrajectoryAnalysis`, `Process.ProcessPipeline`,
  `Process.MapPixels`, `Graph.Graph`, `Math.Clustering.StairsFitter`/
  `ZeroSlopeClusterOptimiser`/`ClusterablePointScore`,
  `Curvature.CurvatureEstimator`, `Process.ROI.OverlayDrawer`/
  `RunnableRoiConstructor`.
- **Explicit type args → diamond (~20 sites):** `IO.FileReader`, `IAClasses.Region`,
  `IAClasses.DSPProcessor`, `IAClasses.Utils`, `Trajectory.DiffusionAnalyser`,
  `Math.Optimisation.FloatingMultiGaussFitter`/`MultiGaussFitter`.
- **Deprecated boxing constructors (7 sites):** `new Integer(...)`/`new Double(...)`
  in `IAClasses.DataStatistics`, `IAClasses.DSPProcessor`,
  `Extrema.MultiThreadedMaximaFinder`, `Curvature.CurveAnalyser`, `IO.DataWriter`.
- **Manual array growth/copy where a `List`/`Set` is clearer:**
  `IAClasses.Pixel2.associations` (`Pixel2.java:70-87`, manual `System.arraycopy`
  grow/shrink) and several `IAClasses.DataStatistics` slice/copy loops.

### G4. Resource management (try-with-resources)

**Status: done (2026-10-02).** All sites below converted to try-with-resources;
`BioFormatsImg` now implements `AutoCloseable` (`close()`), since its reader is
a field-lifetime resource.

Many streams/readers are closed manually or leak on exception/early return:

- `IO.BioFormats.BioFormatsImg.reader` (field) is **never closed**.
- `IO.BioFormats.BioFormatsFileReader` `ImageReader`s never closed (`:37`, `:43`).
- `IO.BioFormats.BioFormatsFileLister` `ImageReader` closes manually and leaks on
  `setId` throw (`:33-44`).
- `IO.PropertyWriter.loadProperties` `FileInputStream` never closed (`:57`).
- `IO.FileReader` `BufferedReader` leaks on the early `return` at `:129`.
- `Extrema.MultiThreadedMaximaFinder` `BufferedReader`s never closed (`:442`, `:517`).
- `UtilClasses.GenUtils` `BufferedReader` closed inside `try` not `finally` (`:198-210`).
- `IO.DataReader` `Scanner`, `IO.DataWriter`/`Trajectory.TrajectoryAnalysis`
  `CSVPrinter`, `IO.BioFormats.BioFormatsImageWriter` `TiffWriter`,
  `Fluorescence.FluorescenceAnalyser` `PrintWriter`s — all manual close.

Done: converted to try-with-resources; `CSVPrinter`, `Scanner`, `ImageReader`,
and `TiffWriter` all implement `AutoCloseable`.

### G5. Logging & error-handling normalisation

**Status: done (2026-10-02).** All `System.out`/`System.err` removed from
`src/main` (only `GenUtils.logError`'s intentional `printStackTrace` remains).

- Remaining `System.out.println` debug/progress (should be `IJ.log`/`GenUtils`):
  `Extrema.MultiThreadedMaximaFinder.java:432-538` (StarDist/ilastik process
  output) and `Curvature.CurvatureEstimator.java:56-87`.
- Direct `printStackTrace()` in `UtilClasses.GenUtils.logError` (`:177`) and
  `Revision.Revision` (`:57`).
- Broad/empty catches: `IO.BioFormats.BioFormatsFileLister.java:45-47` (comment-only),
  `Revision.Revision.java:56-58`,
  `Process.ROI.MultiThreadedROIConstructor.java:226-228`.

### G6. Modern language features

**Status: done (2026-10-02) for `switch`/`instanceof`/lambdas/`requireNonNull`.**
Skipped: records (API-breaking), `SpecifyInputsDialog` `AbstractAction`→lambda
(`Action` is not a functional interface), `MacroWriter` text block (risky string
change), and `Utilities.getDate`→`java.time` (`SimpleDateFormat` vs
`DateTimeFormatter` pattern syntax differ).

- **`switch` → arrow / switch expressions:** `UtilClasses.GenUtils` (`:269`),
  `Process.Segmentation.MultiThreadedWatershed` (`:126`), `IAClasses.Region` (`:666`),
  `Fluorescence.FluorescenceAnalyser` (`:101`),
  `IO.BioFormats.BioFormatsImageWriter` (`:92`, `:123`),
  `Image.ImageNormaliser` (`:39`), `IO.InputFileOpener` (`:63`),
  `IO.OutputFolderOpener` (`:67`).
- **`instanceof` + cast → pattern matching:** `Cell.Cell` (`:83`),
  `IAClasses.Utils` (`:559`), `ParticleWriter.ParticleWriter` (`:38-45`),
  `Process.ROI.MultiThreadedROIConstructor` (`:150`),
  `UIClasses.PropertyExtractor` (many), `Math.Correlation.ImageCorrelator` (`:83`).
- **Anonymous inner classes → lambdas:** `Extrema.MultiThreadedMaximaFinder`
  (`:439`, `:514`), `UIClasses.SpecifyInputsDialog` (`:64`).
- **String building:** `IO.FileReader` `concat` loop → `String.join` (`:104-110`);
  `Trajectory.DiffusionAnalyser` `concat` accumulation (`:100`);
  `MacroWriter.MacroWriter` multi-line `+` → text block (`:37-48`).
- **`java.time`:** `UtilClasses.Utilities` `Date`/`SimpleDateFormat` →
  `DateTimeFormatter` (`:154-158`). (`DateAndTime`/`TimeAndDate` already use
  `java.time`.)
- **Records:** `Cell.CellRegion` is a clean candidate (3 fields + accessors, no
  logic); `Graph.Node` and `IAClasses.RegionEdge` are partial candidates if made
  immutable. Other data holders (`Cell3D.CellRegion3D`, `Particle.Particle`,
  `ClusterablePoint`, `Spot3D`, `Cell3D`) extend a class, so they are not
  record-eligible.
- **Null handling / dead code:** `Cell.Cell` and `Cell3D.Cell3D` contain redundant
  `if (!(cell instanceof Cell)) throw new ClassCastException();` guards (the
  parameter is already typed) — replace with `Objects.requireNonNull`.

### G7. Sequencing & risk

Do the **low-risk, mechanical** items first, then the **structural** ones once
behavioural tests exist, and the **cosmetic** ones last:

1. **Done:** G4/G5 (resource + logging) and G3 (generics/boxing) — no behaviour
   change, testable.
2. **Next:** the pure-math subset of G8 — `Fitter.doFit` → `SimplexOptimizer`,
   `DSPProcessor.FFT`/`IFFT` → `FastFourierTransformer`, and the `Utils`/
   `DataStatistics` statistics → `StatUtils`/`DescriptiveStatistics`. These are
   self-contained, testable now, and simplify G2 (e.g. removing
   `Fitter.defaultRestarts`).
3. **Then:** G1/G2 (threading + static state) — after the D2 god-method
   decomposition and behavioural tests exist.
4. **Then:** the remaining G8 items (I/O, filename filters, date/time, internal
   duplication).
5. **Last:** G6 (language features) — a cosmetic readability pass; doing it
   earlier would churn code that G1/G2/G8 then rewrite. Keep records and
   `var`/streams opt-in.

Record each completed sub-item in `REVISION_LOG.md`.

### G8. Eliminate redundant reimplementations (surveyed 2026-10-02)

Several classes/methods hand-roll functionality already in the JDK or a
dependency on the classpath (Commons Math3/Lang3/IO, ImageJ). Replacing them
removes custom code that is more bug-prone than the mature equivalent.

**Done (2026-10-02, mechanical):** `Fitter.root2` → `Math.sqrt`; `Utils.calcDistance`
→ `Math.hypot`; `Utils.arcTan` → `Math.atan2` + `Math.toDegrees`;
`GenUtils.getDelimiter` → `File.separator`; `FileExtensionFilter.accept` →
`FilenameUtils.isExtension`; `FileReader.getParamsArray()` → `toArray`;
`GenVariables` charsets → `StandardCharsets`.

**Done (2026-10-02, numerical — behind characterisation tests):**
`DSPProcessor.FFT`/`IFFT` → `FastFourierTransformer` (with the unscaled-inverse
`×N` correction) and `Fitter.doFit()` (Nelder–Mead) → `SimplexOptimizer` +
`NelderMeadSimplex`. Public signatures unchanged.

**Done (2026-10-02, statistics):** `Utils.calcEuclidDist` →
`ml.distance.EuclideanDistance`; `Utils.generateGaussian` →
`distribution.NormalDistribution.density`. Signatures unchanged.

**Deferred:** `Utils.calcCovariance`/`covarianceMatrix` (math3 `Covariance` does
not accept the pre-computed means this API exposes), `Utils.getArrayMean`/
`calcEigenvalues` (hand-rolled versions are correct and simple — marginal),
`IAClasses.DataStatistics` (deprecated; rides on Decision 4, not an in-place
rewrite), the string-join items (trailing-delimiter behaviour), and
`DateAndTime.Time.getDuration` (buggy; would change its public return type).

**Clearly redundant (replace outright):**

- `Math.Optimisation.Fitter.doFit()` (`Fitter.java:59-138`) — a full hand-rolled
  Nelder–Mead simplex, inherited by `IsoGaussianFitter`, `GaussianFitter3D`,
  `NonIsoGaussianFitter`, `RoiFitter`, `PlateFitter`. Replace with
  `org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer` +
  `NelderMeadSimplex`.
- `Fitter.root2` (`:26`) = `Math.pow(2.0, 0.5)` → `Math.sqrt(2.0)`.
- `IAClasses.DSPProcessor.FFT`/`IFFT` (`DSPProcessor.java:238-332`) — hand-written
  recursive Cooley–Tukey FFT → `org.apache.commons.math3.transform.FastFourierTransformer`.
- `IAClasses.DataStatistics.calcMean`/`calcStdDev` (`:102-126`, `:150-163`) →
  `StatUtils.mean` / `populationStandardDeviation` (class already `@Deprecated`, see B4).
- `UtilClasses.GenVariables` charsets (`GenVariables.java:16-22`) → `java.nio.charset.StandardCharsets`.
- `IO.File.FileExtensionFilter.accept` (`:35-43`) → `FilenameUtils.isExtension`.
- `UtilClasses.GenUtils.getDelimiter()` (`:81-87`) → `File.separator`.
- `IO.FileReader.getParamString()` (`:104-111`) → `String.join`; `getParamsArray()`
  (`:82-89`) → `toArray(new String[0])`; `getParamIndex()` (`:161-163`) is a
  pass-through over `List.indexOf`.
- `IAClasses.Utils.calcDistance` (2-D, `:209-211`) → `Math.hypot`.
- `Overlay.OverlayToRoi` (`:30-53`, "Copied from OverlayCommands") → **not feasible**
  (no public ImageJ equivalent — see Step 7).
- `ImageProcessing.ImageBlurrer` (`:29-35`) → **not applicable** (already delegates
  to `GaussianBlur.blurGaussian` per slice; a thin public wrapper, not a
  reimplementation).
- `DateAndTime.Time.getDuration`/`getDurationAsString` (`:28-39`) → `java.time.Duration.between`
  (also buggy: subtracts wall-clock components rather than elapsed time).

**Partial overlap (inferior reimplementation; more involved):**

- `IAClasses.Utils.arcTan` (`:324-354`) → `Math.atan2` + `Math.toDegrees` (0–360);
  `calcEuclidDist` (`:213-223`) → `ml.distance.EuclideanDistance`;
  `calcCovariance`/`covarianceMatrix` (`:235-278`) → `Covariance`;
  `calcEigenvalues` (`:287-303`) → `EigenDecomposition` — **not worth it**;
  `generateGaussian` (`:400-433`) → `NormalDistribution.density`;
  `getArrayMean` (`:435-443`) → `StatUtils.mean` — **not worth it**.
- `IAClasses.DataStatistics.calcPercentiles` (`:89-100`) → `Percentile`;
  `findBestRegression`/`getRSquared` (`:214-258`) → `SimpleRegression`.
- `Math.Optimisation.MultiGaussFitter`/`FloatingMultiGaussFitter.doMultiFit` —
  finite-difference coordinate descent → `LevenbergMarquardtOptimizer` + `LeastSquaresBuilder`
  — **deferred** (see Step 12).
- `DataProcessing.Interpolator.interpolateLinearly` (`:14-40`) and `DSPProcessor.upScale`
  (`:186-226`) → **not worth it** (the interpolation is a one-line formula; see Step 10).
- `IO.DataWriter.convertArrayToString` (`:123-132`) → `String.join`/`StringUtils.join`;
  `transposeValues` (`:134-153`) → `MatrixUtils.createRealMatrix(...).transpose()`.
- `IO.DataReader.readTabbedFile` (`:84-131`) → `CSVParser` + `CSVFormat.TDF`; NaN parse
  (`:54-60`) → `NumberUtils.toDouble` — **done (NaN parse); `readTabbedFile` deferred**
  (see Step 9).
- `IO.File.FileName.makeValidFileName` (`:26-36`) → `FilenameUtils.removeExtension`.
- `UtilClasses.GenUtils.checkRange` (`:111-119`) → `Math.floorMod`; `checkFileSep`
  (`:154-167`) → `StringUtils.replaceChars`.
- `UtilClasses.Utilities.getDate` (`:154-158`) → `java.time.DateTimeFormatter`;
  `checkRange` (`:143-152`) → `Math.floorMod`.
- `Particle.ParticleArray` duplicates TrackMate `SpotCollection` storage/add;
  `Particle.refineCentroid` reimplements standard centroid localization.
- `Math.Clustering.ClusterablePoint` → `ml.clustering.DoublePoint` (for the `Clusterable` role).

**Internal duplication:**

- `Cell.Cell.compareTo` ≡ `Cell.compare`; `Cell3D.Cell3D` same — **done** (`compare`
  now delegates to `compareTo`).
- `Image.ImageChecker.isBinaryImage` ≡ `Binary.BinaryMaker.checkIfBinary`.
- `IO.File.FileExtensionFilter` / `ImageFilter` / `IAClasses.OnlyExt` — three
  near-identical filename filters.
- `Extrema.MaximaFinder` (deprecated) facades over `MultiThreadedMaximaFinder`,
  with 2-D local-max logic duplicated by `RunnableMaximaFinder`.

**Verified NOT redundant** (no stdlib/commons equivalent; do not touch):
`Math.Histogram.calcHistogram`, `Math.MSS`, `Math.Rand`, `Math.Correlation`,
`Math.Clustering.StairsFitter`/`ZeroSlopeClusterOptimiser`, `DataProcessing.Smoother`,
`Math.Optimisation.Plate`, `Graph.Dijkstra`, `IAClasses.SkeletonProcessor`,
`IAClasses.FractalEstimator`, `Binary.EDMMaker`.

**Sequencing:** see G7 — the pure-math items (`Fitter`, `DSPProcessor`, statistics)
run before G1/G2; the legacy `IAClasses` items (`DataStatistics`, `OnlyExt`) ride
on Decision 4 (deprecate-then-remove) rather than in-place rewrites.

**Remaining G8 work — execution steps (2026-10-03).** The survey above is
distilled into individual steps, ordered by risk. Tiers 1–4 are in scope now;
Tier 5 is deferred/skipped.

- **Tier 1 — mechanical swaps (low risk, no API change):**
  1. `GenUtils.checkRange` → `Math.floorMod` (also `Utilities.checkRange`). — **done (2026-10-03).**
  2. `GenUtils.checkFileSep` → `StringUtils.replaceChars`. — **done (2026-10-03).**
  3. `IO.FileReader.getParamString()` → `String.join` (verify trailing-delimiter
     behaviour). — **done (2026-10-03):** trailing `\t` dropped (was an unintended
     trailing separator).
  4. `IO.DataWriter.convertArrayToString` → `String.join`/`StringUtils.join`. — **done
     (2026-10-03):** used `StringBuilder` (not `String.join`) because the method's
     "delimiter *after* each element" + seed semantics, pinned by `DataWriterTest`,
     don't map to `String.join`'s "delimiter *between*" semantics.
  5. `IO.DataWriter.transposeValues` → `MatrixUtils.createRealMatrix(...).transpose()`. — **done
     (2026-10-03):** ragged/null rows now throw (was NaN-fill); rectangular case
     unchanged.
  6. `Utilities.getDate` → `java.time.DateTimeFormatter`. — **done (2026-10-03):**
     `LocalDateTime.now().format(DateTimeFormatter.ofPattern(...))`; caveat —
     `SimpleDateFormat` vs `DateTimeFormatter` pattern syntax differ (matches for
     common `yyyy/MM/dd/HH/mm/ss/SSS`, differs for timezone/`S`-fraction and
     uncommon letters).
- **Tier 2 — ImageJ/IO delegation (low risk):**
  7. `Overlay.OverlayToRoi` → `ij.plugin.OverlayCommands.overlayToRoi` — **not feasible
     (2026-10-03):** `OverlayCommands` has no public `overlayToRoi`; the equivalent
     `toRoiManager()` is package-private and has since diverged (`rm.setOverlay(...)`
     vs the copied `rm.add(...)` loop). Keep the copy.
  8. `ImageProcessing.ImageBlurrer` → `ij.plugin.filter.GaussianBlur` — **not
     applicable (2026-10-03):** `blurStack` already delegates to
     `GaussianBlur.blurGaussian` per slice; it is a thin public wrapper, not a
     redundant reimplementation. Nothing to change.
  9. `IO.DataReader.readTabbedFile` → `CSVParser` + `CSVFormat.TDF`; NaN parse →
     `NumberUtils.toDouble` — **partially done (2026-10-03):** NaN parse in
     `readCSVFile` → `NumberUtils.toDouble`. `readTabbedFile` → `CSVParser` +
     `CSVFormat.TDF` **deferred** (the Scanner impl collects only the leading run
     of doubles per line and treats blank lines as record separators, which
     `CSVParser`+`TDF` does not replicate cleanly; no test coverage).
- **Tier 3 — numerical swaps (medium risk, needs characterisation tests):**
  10. `DataProcessing.Interpolator.interpolateLinearly` + `DSPProcessor.upScale` →
      `LinearInterpolator` — **not worth it (2026-10-03):** both use a one-line
      linear-interpolation formula (`a + t·(b-a)`); `LinearInterpolator` is a 1-D
      spline that (a) adds boilerplate, (b) throws on a single-point input where
      `upScale` holds the value, and (c) extrapolates at the boundary where
      `upScale` clamps. `interpolateLinearly` is a bespoke 3-D/`ref`-column/`keys`
      traversal, not a 1-D interpolation. No genuine redundancy to eliminate.
  11. `Utils.getArrayMean` → `StatUtils.mean`; `Utils.calcEigenvalues` →
      `EigenDecomposition` — **not worth it (2026-10-03):** `getArrayMean` would
      need a 2-D → 1-D flatten (more code, no benefit — the loop is already O(n));
      `StatUtils.mean` has no `double[][]` overload. `calcEigenvalues` is a correct
      2×2 closed-form; `EigenDecomposition` adds `RealMatrix` boilerplate, does not
      guarantee the same (descending) order, and diverges on complex eigenvalues
      (current returns `NaN`, math3 returns real parts).
  12. `Math.Optimisation.MultiGaussFitter`/`FloatingMultiGaussFitter.doMultiFit` →
      `LevenbergMarquardtOptimizer` + `LeastSquaresBuilder` — **deferred
      (2026-10-03):** the LM classes exist (verified `LevenbergMarquardtOptimizer`
      + `LeastSquaresBuilder.model(MultivariateJacobianFunction)`), but this is a
      major rewrite: flatten the `xe/ye/mag/bg[/sigma]` 2-D arrays to a parameter
      vector, supply analytic Jacobians, enforce the `bg ≤ mag` clamp via
      `ParameterValidator`, and preserve the incremental "add one Gaussian at a
      time" loop. No test coverage; the current coordinate descent works and the
      problem is small (≤ `FIT_SIZE²` pixels). Revisit only if fit performance
      becomes an issue.
- **Tier 4 — internal duplication (low value, structural):**
  13. `Cell.Cell.compareTo` ≡ `Cell.compare`; `Cell3D` same — **done (2026-10-03):**
      `compare` now delegates to `compareTo`.
  14. `Image.ImageChecker.isBinaryImage` ≡ `Binary.BinaryMaker.checkIfBinary` —
      **verified NOT identical (2026-10-04), deferred.** `isBinaryImage` is a pure
      histogram test (`width*height == hist[0]+hist[255]`) with no type check;
      `checkIfBinary` additionally requires `ByteProcessor` and compares against
      `stats.pixelCount` (ROI-aware), not `width*height`. The shared logic is one
      comparison, so a dedupe helper would add public API for no meaningful saving.
  15. Dedup `FileExtensionFilter` / `ImageFilter` / `IAClasses.OnlyExt` —
      **deferred (2026-10-04).** `FileExtensionFilter` already delegates to
      `FilenameUtils.isExtension` (Tier 1). `ImageFilter` uses a loose
      `endsWith(lowercased)` match and `OnlyExt` is `@Deprecated` with TIF/TIFF +
      JPG/JPEG special-casing; neither is behaviourally equivalent, so collapsing
      them would change public semantics.
  16. `Extrema.MaximaFinder` (deprecated) facade dedup — **verified not a facade
      (2026-10-04), deferred.** Only two methods are `@Deprecated` and they already
      delegate to `MultiThreadedMaximaFinder`; the live 2-D methods
      (`findImageMaxima`, the `drawValue` `findLocalMaxima`, `isLocalMax`) operate
      on `ImageProcessor`/`short[]` with 2-D kernels and are not duplicated by the
      3-D `float[]` `RunnableMaximaFinder.isMax` (different dimensionality, type,
      and boundary clamping). Aggressive removal would break `Utils` callers.
- **Tier 5 — deferred / skip:**
  17. `IAClasses.DataStatistics` — **skip** (deprecated; rides on Decision 4).
  18. `Utils.calcCovariance`/`covarianceMatrix` → `Covariance` — **deferred** (API
      exposes pre-computed means).
  19. `DateAndTime.Time.getDuration` → `java.time.Duration` — **deferred** (buggy;
      would change public return type — add new method + `@Deprecated` only).
  20. `Particle.ParticleArray` / `Particle.refineCentroid` / `ClusterablePoint` →
      library equivalents — **structural/API risk**, needs a maintainer call.

**Order:** Tier 1 → 2 → 3 → 4, leaving Tier 5 as documented deferrals.

⚠️ `FileName.makeValidFileName → FilenameUtils.removeExtension` (from the survey)
is suspected wrong — `makeValidFileName` likely sanitises a filename rather than
strips an extension. Verify before touching.

### G9. Optimise `RiemannianDistanceTransform`

`Process.DistanceTransform.RiemannianDistanceTransform` is a gradient-weighted
(Riemannian/anisotropic) distance transform: it accumulates a metric
`w = (|∇I| + 1 + λ)/(1 + λ)` along each axis, then runs four separable 1D passes
(Z→Y→X→Z) of `min_j (f[j] + (g[j] − g[i])²)`. There is **no drop-in equivalent**
in the dependency tree (mcib3d `EdtFloat` is Euclidean; MorphoLibJ is
chamfer/geodesic; ImageJ `EDM` is Euclidean), so the logic stays but the
implementation should be tightened:

1. **Efficiency — O(n²) → O(n):** each `StepNThread` 1D minimisation is O(n²).
   Because `g` is a monotone cumulative sum of strictly positive weights, this is
   the *distance transform of a sampled function*, computable in **O(n)** with the
   Felzenszwalb–Huttenlocher lower-envelope algorithm. Output-identical; the
   largest win (runtime is currently quadratic in the longest axis). — **done
   (2026-10-03):** `distanceTransformSampled` (lower envelope) + `nearestForegroundDistance`
   (two-pass) extracted as static methods.
2. **Accuracy:** compute the cumulative metric and the envelope in `double` and
   cast to `float` only at output — `float32` squared distances lose precision
   past ~4096 per dimension. — **done (2026-10-03):** `compute*Distances` and the
   transforms now use `double`.
3. **Threading:** route the four stages through a shared
   `ExecutorService`/`ForkJoinPool` instead of spawning `4 × nbCPUs` fresh
   `Thread`s per call — **done (G1 Step 4(b)).**

**Status: done (2026-10-03).** Characterisation tests added
(`RiemannianDistanceTransformTest`, 2 tests) pinning the O(n) transforms against
naive O(n²) double reference implementations.

---

## Phase H — `BioFormatsImg` facade & time-dimension hardening

**Status: planned (2026-10-09), not started.** `IO.BioFormats.BioFormatsImg` is
the primary loader, but a review against the Bio-Formats Java API found the
pixel-loading path is incomplete and the time dimension is effectively
unsupported. The class juggles three Bio-Formats abstractions (`ImageReader`,
`IMetadata`, `ImporterOptions`/`BF`) inconsistently.

### H1. Re-point the metadata accessors at `IMetadata` — **done (2026-10-18)**

`getSizeX/Y/Z/C/T`, `getDimOrder`, `getSeriesName`, `getXYSpatialRes`,
`getZSpatialRes` previously went through `reader.setSeries(...)` + `reader.get*()`,
which mutated the reader's current series as a hidden side effect. They now read
from the side-effect-free, series-indexed `IMetadata` accessors. `ImageReader` is
kept only for `setId`, `checkID`, `getFileList`, `getPixelType`, `getRGBChannelCount`,
`close` (plus `getSeriesCount`/`getSeries`).

Implemented: `getSizeX/Y/Z/C/T` → `meta.getPixelsSize*(series).getValue()`;
`getDimOrder` → `meta.getPixelsDimensionOrder(series).getValue()`; the inline
`reader.getDimensionOrder()` in the `loadPixelData(series)` convenience overload →
`getDimOrder(series)`. `getPixelType` remains on `reader`. Public signatures
unchanged.

**Verified API (2026-10-18).** `IMetadata` is `loci.formats.meta.IMetadata`,
which extends `ome.xml.meta.IMetadata` / `MetadataRetrieve` (generated, in
`ome-xml`/`specification`). The accessors and their actual return types:

| `BioFormatsImg` method | Current | Replacement (`meta` = the `OMEXMLMetadata` field) |
| --- | --- | --- |
| `getSizeX/Y/Z/C/T(series)` | `reader.setSeries(series); reader.getSizeX()` | `meta.getPixelsSizeX(series).getValue()`, etc. (`PositiveInteger.getValue()` → `Integer`) |
| `getDimOrder(series)` → `String` | `reader.getDimensionOrder()` | `meta.getPixelsDimensionOrder(series).getValue()` (`DimensionOrder` enum → `String`, e.g. `"XYZCT"`) |
| `getSeriesName(series)` | *(already)* `meta.getImageName(series)` | unchanged |
| `getXYSpatialRes(series)` → `Length` | `meta.getPixelsPhysicalSizeX(series)` (already side-effect-free) | unchanged |
| `getZSpatialRes(series)` → `Length` | `meta.getPixelsPhysicalSizeZ(series)` + fallback | unchanged (H4 adds null-guard) |

`getPixelType(series)` stays on `reader` (still `reader.setSeries(...)` +
`reader.getPixelType()`) — H1 keeps it as-is for now; it is the only
`reader`-mutating size/type getter left, and `ImageReader` has no side-effect-free
`getPixelType(series)` variant.

**Reference implementation already exists.** `LocationAgnosticBioFormatsImg`
(currently `extends BioFormatsImg`) already reads dimensions/resolution from its
own `IMetadata` field using exactly `metadata.getPixelsSizeX(series).getValue()`
and `metadata.getPixelsPhysicalSizeX(series)`. H1 makes `BioFormatsImg` follow the
same pattern via its `meta` field (populated by `createOMEXMLMetadata()` +
`reader.setMetadataStore(meta)`), so both classes use one consistent, verified
idiom. This is purely internal — no public signature changes, and the subclass's
overrides are unaffected (it supplies its own `metadata` field).

**Null/edge notes (defer to H4):** `getPixelsSize*` returns non-null
`PositiveInteger` for valid series but `getPixelsPhysicalSize*` may return `null`
when the file lacks calibration, so the existing `series >= getSeriesCount()`
guards in `getXYSpatialRes`/`getZSpatialRes` are retained. Behaviour for valid
series is unchanged; the only observable change is that `getSizeX/Y/Z/C/T` and
`getDimOrder` no longer leave the `reader` on the queried series.

**`LocationAgnosticBioFormatsImg` scope decision (2026-10-18):** the OMERO
motivation for this subclass is gone (it now just drives Bio-Formats' generic
`Importer`/`ImportProcess`/`ImporterOptions` API, and the parent already uses
`ImporterOptions` via `BF.openImagePlus`). It is unreferenced in-repo. Rather than
merge the two classes, it is now `@Deprecated` (kept for external
source/binary compatibility per Decision 5), with removal/consolidation deferred
to the Decision-7 namespace hand-off. H1/H2 target the parent `BioFormatsImg`
only.

### H2. Rewrite `loadPixelData` as one dimension-complete method — **done (2026-10-18)**

- Added `loadPixelData(series, cBegin, cEnd, zBegin, zEnd, tBegin, tEnd, dimOrder)`:
  sets `setSpecifyRanges(true)` plus `setCBegin/CEnd/CStep`, `setZBegin/ZEnd/ZStep`,
  `setTBegin/TEnd/TStep`, and `setInputOrder(series, dimOrder)` (only when `dimOrder`
  is non-null). Retains the existing RGB channel-count clamp and title logic.
- Kept `loadPixelData(series)` and `loadPixelData(series, cBegin, cEnd, dimOrder)`
  as delegating conveniences (no public-API break).
- Fixed the `cEnd` off-by-one: the inclusive last index is now `getSizeC(series) - 1`
  (previously `getSizeC(series)`, one past the valid range), and the guard uses
  inclusive-last-index semantics (`cEnd >= getSizeC(series)` rejects out-of-range).
- Stopped ignoring `dimOrder`. In-repo callers pass `null` (Bio-Formats default
  order), which is preserved.

### H3. Add time accessors — **done (2026-10-18)**

- `getTimeIncrement(series)` → `meta.getPixelsTimeIncrement(series)`
  (`ome.units.quantity.Time`), null-guarded on `series >= getSeriesCount()`.
- `getTimeIncrementSeconds(series)` → `double`, converting to seconds via
  `UNITS.SECOND` (`Time.value(Unit<Time>)`), `Double.NaN` when absent/non-convertible.
- `getTimeCalibration(series)` → `double` (seconds), a dedicated accessor.
  `getCalibration(series)` is **unchanged** (`{x, y, z}`) to avoid widening the
  public array and breaking consumers that expect 3 elements.

### H4. Fix the NPE risks — **done (2026-10-18)**

`getZSpatialRes` (dereferences `getXYSpatialRes(...).unit()`), `toString`,
`getInfo`, `getUnits`, and `getCalibration` all dereference possibly-null
physical sizes. Added null-guards/defaults via two private helpers
(`lengthValue` → `Double.NaN`, `lengthSymbol` → `"microns"`); `getZSpatialRes`
now returns `null` rather than constructing a fallback `Length` when the XY
resolution is also absent.

### H5. Clarify the ImageJ caching split — **done (2026-10-18)**

`img` / `processedImage` (`getLoadedImage`, `getProcessedImage`,
`getProcessedStackPixels`) are ImageJ caching/processing concerns, not
Bio-Formats concerns. Documented their contract in Javadoc: the class now states
the two-image split up front, and `setProcessedImage`/`getProcessedImage`/
`getProcessedStackPixels` document nullability, the distinction from
`getLoadedImage`, and the `float[]`-slice assumption. No extraction needed — the
split is small and the loader is the natural owner.

### H6. Post-phase cleanup — **done (2026-10-18)**

Two leftover review findings, closed after H1–H5:

- `getDimOrder(series)` now null-guards `meta.getPixelsDimensionOrder(series)`
  (returns `null` instead of NPE; callers already tolerate `null`).
- Removed the parent `BioFormatsImg`'s write-only `protected ImporterOptions io`
  field (constructed + `setId`'d but never read by the parent; `loadPixelData`
  builds a fresh `ImporterOptions`). The field now lives in
  `LocationAgnosticBioFormatsImg` (its only consumer) as a `private` field
  initialised in the constructor.

**Guardrail:** keep the existing public method signatures (consumers unaffected);
the breaking rename/trim, if any, rides on the coordinated Decision-7 namespace
hand-off, not this phase.

## Phase R — Process pipeline refactor

**Status: planned (2026-10-18); none of R1–R4 implemented yet.** The R1 *scope*
was agreed with the GIANI team (`GIANI-time-support-open-questions` discussion;
default `time = 0`, 5 load sites, `_T<t>` naming), but no R1 code has landed —
the "Resolve R1" commit only updated the plan/log. `Process.MultiThreadedProcess`
has accreted four unrelated responsibilities and is the main maintenance drag in
the processing pipeline. This phase simplifies it into a typed, testable core,
executed one sub-phase at a time (R1 → R4). R1–R3 are additive/deprecation-only;
R4 is the only breaking change and is independent of the Decision-7 namespace
move.

### Current state (verified 2026-10-18)

`MultiThreadedProcess` (`Process/MultiThreadedProcess.java:37`) is simultaneously
a `Thread` (work in `run()`, started/joined manually by `getOutput()`
`:133-145`), a `Callable<BioFormatsImg>` (`call()` just returns `img`, `:129`),
a mutable pipeline node (`inputs`/`outputDests` + `duplicate()`/`updateInput`/
`updateOutputDests`), and a stringly-typed config holder (`props` + `String[]
propLabels`, parsed per subclass via `Integer.parseInt(props.getProperty(
propLabels[N]))`).

Thirteen subclasses fall into three kinds:

- **Source** (loads raw pixels on `inputs == null`): `MultiThreadedMaximaFinder`,
  `MultiThreadedGaussianFilter`, `MultiThreadedTopHatFilter`; plus
  `MultiThreadedROIConstructor` re-loads raw pixels for measurement (`:173`).
- **Transform** (reads `inputs`): `Watershed`, `RegionGrower`, `ROIConstructor`,
  `Colocalise`, `MapPixels`, `ImageCalculator`, `SobelFilter`, `MeasureROI`,
  `Hessian`.
- **Outlier**: `MultiThreadedImageLoader` — empty `setup`, broken `duplicate()`
  (`new MultiThreadedImageLoader(null, null, null)`), never sets `img`.

The magic-index config (`MultiThreadedMaximaFinder.SERIES_SELECT = 7`,
`N_PROP_LABELS = 28`) is the biggest tax; the `inputs == null` branch encodes
"source vs transform" as a null check.

### R1. Centralise dimensions & loading — **planned; scope agreed with GIANI (2026-10-18), not yet implemented**

Open questions resolved with the GIANI team (see `REVISION_LOG.md` 2026-10-18);
the code is still to be written. Add `series`/`channel`/`time` (typed, default
`time = 0`) plus `setSeries`/
`setChannel`/`setTime`/`getTime` and a `loadPixels(cBegin, cEnd)` helper to
`MultiThreadedProcess` that owns the dimension → load mapping:

```java
protected void loadPixels(int cBegin, int cEnd) {
    img.loadPixelData(series, cBegin, cEnd, 0, img.getSizeZ(series) - 1, time, time, null);
}
```

**Load sites (5, per the GIANI review).** The three source classes
(`MultiThreadedMaximaFinder`, `MultiThreadedGaussianFilter`,
`MultiThreadedTopHatFilter`) switch to `loadPixels(channel, channel)`. The
`MultiThreadedROIConstructor.processObjectPop` pixel-measurement loop uses the
8-arg load; `processObjectPop` is `public static` and is also reached from
`MultiThreadedColocalise.saveData()`, so it gains a `time` overload (the 4-arg
form delegates with `0`) and both callers pass `time`.

**Naming.** Expose `time` (`getTime()`); append `_T<t>` to `labelOutput` titles
and to the `saveAllRois`/`saveAllMasks` zip/mask names, gated on
`img.getSizeT(series) > 1` so single-T output names are unchanged (preserves
`TrackerLibrary`/`AdaptDataProcessing` output names).

**`duplicate()`.** Unchanged — GIANI's headless executor sets `time` after the
pipeline is built and before each run (never before `duplicate()`), so no
propagation is required. Propagating it through `duplicate()` is optional GUI
safety only.

### R2. Typed parameters — deprecation

Replace the `props`/`propLabels` stringly-typed config with **one shared typed
`ProcessParams`** object (flat, typed getters/setters) — decision 9. Keep
`setup(BioFormatsImg, Properties, String[])` as a `@Deprecated` adapter to the
new `setup(BioFormatsImg, ProcessParams)`. Migrate subclasses one at a time;
delete the `SERIES_SELECT`/`N_PROP_LABELS` magic constants. Dimension selection
(`series`/`channel`/`time`) stays on the base class as execution context, not in
`ProcessParams`.

### R3. Source/transform split + DAG cleanup — deprecation

Introduce `SourceProcess` (no `inputs`, owns `loadPixels`) and
`TransformProcess` (requires `inputs`); remove the `inputs == null` branch.
Replace the mutable `duplicate()`/`outputDests`/`updateInput` re-wiring with an
immutable pipeline rebuild. `@Deprecated` `MultiThreadedImageLoader` (do not
remove — public API, may be used externally) — decision 8.

### R4. Drop the `Thread`/`Callable` duality — breaking

Make `MultiThreadedProcess` a `Runnable`/`Callable` that produces output via a
`Future`; remove `extends Thread` and the manual `start()`/`join()` in
`getOutput()`. Run processes on a shared executor owned by the pipeline.
Consolidate the per-subclass `Executors.newFixedThreadPool` into the base
`runWorkers`, and convert worker classes (e.g. `RunnableHessianFilter extends
Thread implements Callable<Void>`) into plain `Callable`s. **Breaking** —
external code calls `.start()`/`.join()` directly (`MultiThreadedWatershed`
`:108-110`, `RiemannianDistanceTransform`:141-143`), so it ships as a major
version bump, independent of the Decision-7 namespace move — decision 10.

---

## Resolved decisions

Resolved with the maintainer on 2026-09-24. These supersede the open questions
in the phases above.

0. **IAClassLibrary is a public library with multiple downstream consumers**
   (ADAPT, `TrackerLibrary`, `AdaptDataProcessing`, and others), not just ADAPT's
   dependency. Therefore "not referenced inside this repo" does **not** mean
   "dead code": any public class may be used externally. Removal must be
   restricted to (a) commented-out debug code and private internals, or (b)
   public symbols only after a deprecation window and external-usage check.
1. **License — GPL-3.0-or-later.** Add a root `LICENSE`, correct `pom.xml` from
   BSD-2 to GPL-3 (`<licenses>` + `license.licenseName`). Source-header tidy-up
   (removing the old pre-GitHub-era boilerplate stubs) is deferred to a later
   pass — see Phase B1 item 4.
2. **Versioning — Conventional Commits.** Commit messages follow Conventional
   Commits (`fix:`, `feat:`, `chore:`, `refactor:`, `docs:`, `test:`;
   `BREAKING CHANGE:`/`!` for breaking changes). Bump `<version>` in `pom.xml` on
   every code change: `fix`/`refactor`/`chore`/`docs`/`test` → patch, `feat` →
   minor, breaking → major. No `-SNAPSHOT` suffix — each commit is a concrete
   version. *(The `2.0.1`/`v2.0.1` release and failed `v2.0.0` tag are historical —
   see B2.)* The existing `v1.032` tag is a mislabel of `v1.0.32`; use the `vX.Y.Z`
   form going forward. The JAR version is read from the manifest, not `pom.xml`
   (see `Revision.getVersion()`).
3. **Java target — 21** *(revised from 11)*. Upgrade the parent to
   `pom-scijava:45.1.0` and set `scijava.jvm.version=21`; build on JDK 21.
   This adopts TrackMate 8.0.0 (no version pin needed) and is the first step of
   a coordinated Java 21 bump across IAClassLibrary, `TrackerLibrary`,
   `AdaptDataProcessing`, and ADAPT.
4. **Legacy `IAClasses` package — deprecate, then remove.** Keep the load-bearing
   primitives `Region`, `Utils`, `BoundaryPixel`, `DSPProcessor`, and `Pixel`
   (used by `Cell`, `Segmentation`, `Process`, `Particle`, etc.). Mark the
   unreferenced remainder (`Region2`, `Region3D`, `RegionEdge`, `Pixel2`,
   `Gaussian3D`, `CrossCorrelation`, `FractalEstimator`, `DataStatistics`,
   `SkeletonProcessor`, `OnlyExt`, `ProgressDialog`, `StaticConstants`, and the
   `IAClasses` `FluorescenceAnalyser`) `@Deprecated` now, remove in a later major
   version after confirming no external consumers.
5. **`LocationAgnosticBioFormatsImg` — keep.** It is part of the public API and
   may be used by external projects even though it is unreferenced in this repo.
   Do not delete; document its role alongside `BioFormatsImg`.
6. **Legacy Ant build — delete.** Remove `build.xml`, `nbproject/`, and the
   `.orig` backups; they are non-portable, machine-specific, and not part of the
   Maven build. Recoverable from git if the NetBeans workflow is ever revived.

7. **Package namespace — `io.github.djpbarry`** *(resolved 2026-09-25)*. The
   `net.calm` namespace was based on a domain that does not exist (`calm.net`).
   Rename the package root from `net.calm.iaclasslibrary` to
   `io.github.djpbarry.iaclasslibrary` (and the Maven `groupId` from `net.calm`
   to `io.github.djpbarry`); the `artifactId` `iaclasslibrary` stays. This is a
   breaking change shared across the whole suite (`TrackerLibrary`, `Adapt`,
   `AdaptDataProcessing` all use the `net.calm.*` umbrella), so it is executed in
   lockstep during the coordinated v2.0.1 hand-off (Phase F / M6), not solo.
8. **`MultiThreadedImageLoader` — deprecate, do not remove.** *(2026-10-18)* It
   is public API and may be used externally even though it is unreferenced in-repo
   (empty `setup`, broken `duplicate()`). `@Deprecated` it in Phase R3; removal
   only after a deprecation window and an external-usage check.
9. **Process params — one shared typed `ProcessParams`.** *(2026-10-18)* Replace
   the `props`/`propLabels` stringly-typed config with a single flat, typed
   `ProcessParams` object (named getters/setters) rather than one params class per
   process. Dimension selection (`series`/`channel`/`time`) stays on the base
   `MultiThreadedProcess` as execution context, not in `ProcessParams`.
10. **R4 breaking change — independent of the namespace move.** *(2026-10-18)* The
    `extends Thread` → `Runnable`/`Callable` + shared-executor change (Phase R4)
    is breaking on its own and ships as a major version bump; it is not bundled
    with the Decision-7 `io.github.djpbarry` namespace move.
11. **Time selection & naming (GIANI timelapse).** *(2026-10-18)*
    `MultiThreadedProcess` gains `series`/`channel`/`time` (default `time = 0`)
    with setters and a `getTime()` getter. Per-timepoint output names append
    `_T<t>` gated on `img.getSizeT(series) > 1` (single-T names unchanged). The
    executor sets `time` after pipeline build and before each run, so
    `duplicate()` does not propagate it.

---

## Suggested sequencing & milestones

1. **M1 — Foundations (low risk, high value):** `.gitignore`, Maven wrapper, CI
   hardening (pin Java 21), delete the legacy Ant build, remove
   `MultiThreadedStarDist` + commented-out debug code, add GPL-3 `LICENSE` +
   fix pom metadata, release and tag. (Phase A, B, D1)
   *Status (2026-09-27): complete — released `2.0.1`, tagged `v2.0.1` (B2).*
2. **M2 — Test harness:** JUnit 5 + pure-logic unit tests + CSV golden tests.
   *Done (20 tests, 2026-09-25).* (Phase C)
3. **M3 — Gotchas & hygiene:** fix `validID`/`clearImageData`/`getLoadedImage`
   aliasing, duplicate import, error-handling normalisation. (Phase D5, D6)
4. **M4 — Refactor core:** decompose `RegionGrower`/`MultiThreadedMaximaFinder`,
   `@Deprecated` the dead legacy classes, document `LocationAgnosticBioFormatsImg`.
   (Phase D2–D4)
5. **M5 — Documentation:** expand `README.md`, sync `AGENTS.md`, Javadoc public
   API. (Phase E)
6. **M6 — Upstream hand-off:** confirm TrackMate version policy and Java target
   with the other three repos, so ADAPT can pin to tags. IAClassLibrary's own tag
   (`v2.0.1`) is now live on JitPack. (Phase F)
7. **M7 — Modern Java modernisation:** resource/logging fixes and mechanical
   generics/boxing first (G3–G5), then the pure-math redundant-reimplementation
   swaps (G8: `Fitter`, `DSPProcessor`, statistics), then the threading/
   static-state overhaul (G1–G2) once Phase C/D2 tests exist, then the remaining
   G8 items, the `RiemannianDistanceTransform` optimisation (G9), and the
   language-feature readability pass (G6). (Phase G)
8. **M8 — `BioFormatsImg` facade & time hardening:** re-point metadata accessors
   at `IMetadata` (H1), rewrite `loadPixelData` for full C/Z/T + `dimOrder` (H2),
   add time accessors (H3), fix NPEs (H4), clarify the ImageJ caching split (H5).
   (Phase H)
9. **M9 — Process pipeline refactor:** centralise dimensions/loading (R1), typed
   params (R2), source/transform split (R3), drop `Thread`/`Callable` duality
   (R4). (Phase R)

Each milestone is independently shippable. M1 is the immediate next step and
unblocks the ADAPT plan's M10 (upstream dependency hygiene).
