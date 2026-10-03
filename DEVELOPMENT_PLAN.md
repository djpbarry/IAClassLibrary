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
  `2.0.2` (patch-bumped per commit; last release `2.0.1` / tag `v2.0.1`), declared license
  **GPL-3.0-or-later** (`license.licenseName=gpl_v3`). *(Reconciled 2026-09-24;
  released 2026-09-27.)*
- **CI:** `.github/workflows/maven.yml` runs `./mvnw --batch-mode
  --update-snapshots verify` on **JDK 21** with the `setup-java` Maven cache.
  *(Updated 2026-09-27.)*
- **Tests:** JUnit 5 (Jupiter) harness added — `src/test/java`, 15 classes,
  34 tests as of 2026-10-02 (incl. FFT/Fitter/Utils/RegionGrower/MultiThreadedProcess
  tests). No lint/format tooling. (Phase C done for now.)
- **License:** a GPL-3.0-or-later `LICENSE` file now exists and `pom.xml` is
  corrected from BSD-2 to GPL-3. Source headers remain inconsistent (roughly
  half GPL-3, roughly a third NetBeans "change this header" stubs); header
  tidy-up is deferred (Decision 1). *(Reconciled 2026-09-24.)*
- **Legacy build:** `build.xml` + `nbproject/` have been **deleted** (Decision 6).
  A stray untracked `nb-configuration.xml` (NetBeans config) and an untracked
  `out/` directory (IDE build output) remain to be removed or gitignored.
  *(Reconciled 2026-09-24.)*
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

**Status: done (2026-09-25; revisiting later).** JUnit 5 harness wired
(`junit-jupiter-api`/`-engine`, parent-managed 5.13.4); 20 tests green across
pure-logic (`Histogram`, `MSS`, `Rand`, `GenUtils`, `Smoother`, `Interpolator`,
`ClusterablePoint`, `DataWriter` transforms) and CSV golden tests (`DataIOTest`,
`TrajectoryAnalysis`). Deferred: extracting pure logic from god methods (D2).

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

**Status: G3 + G4 + G5 + G6 done; G8 done for FFT + Fitter + mechanical + statistics
items (2026-10-02). Remaining: G1–G2 and a few deferred G8 items.** The core is over
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
- `Extrema.MultiThreadedMaximaFinder` — ~30 mutable `static` config ints/shorts
  (`:70-109`).
- `Process.MultiThreadedProcess.OUTPUT_SEP`, and per-class label/feature index
  constants across `MultiThreadedWatershed`, `MultiThreadedColocalise`,
  `MultiThreadedGaussianFilter`, `MultiThreadedTopHatFilter`,
  `MultiThreadedROIConstructor`, `SpotFeatures`, `Particle.COLOCALISED`.
- `Trajectory.TrajectoryAnalysis` (`:48-63`) and
  `Trajectory.DiffusionAnalyser.plotLegend` (`:36`).
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
   statics → instance fields (`MultiThreadedMaximaFinder` ~30, `TrajectoryAnalysis`,
   `DiffusionAnalyser.plotLegend`); (d) `RegionGrower.terminal/intermediate/lambda/
   filtRad` → instance/params (after 1–2).
4. **G1 — threading:** (a) `MultiThreadedProcess`/`RunnableProcess` route work
   through the existing `exec` / a managed pool; (b) `RiemannianDistanceTransform`
   4 inner `Thread`s → `ExecutorService` + `Future`; (c) `MultiThreadedColocalise`
   raw `Thread[]` → `ExecutorService`; (d) `MultiThreadedImageLoader` use its
   executor; (e) `MultiThreadedMaximaFinder` process-drain → virtual threads.

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
- `Overlay.OverlayToRoi` (`:30-53`, "Copied from OverlayCommands") → `ij.plugin.OverlayCommands.overlayToRoi`.
- `ImageProcessing.ImageBlurrer` (`:29-35`) → `ij.plugin.filter.GaussianBlur`.
- `DateAndTime.Time.getDuration`/`getDurationAsString` (`:28-39`) → `java.time.Duration.between`
  (also buggy: subtracts wall-clock components rather than elapsed time).

**Partial overlap (inferior reimplementation; more involved):**

- `IAClasses.Utils.arcTan` (`:324-354`) → `Math.atan2` + `Math.toDegrees` (0–360);
  `calcEuclidDist` (`:213-223`) → `ml.distance.EuclideanDistance`;
  `calcCovariance`/`covarianceMatrix` (`:235-278`) → `Covariance`;
  `calcEigenvalues` (`:287-303`) → `EigenDecomposition`;
  `generateGaussian` (`:400-433`) → `NormalDistribution.density`;
  `getArrayMean` (`:435-443`) → `StatUtils.mean`.
- `IAClasses.DataStatistics.calcPercentiles` (`:89-100`) → `Percentile`;
  `findBestRegression`/`getRSquared` (`:214-258`) → `SimpleRegression`.
- `Math.Optimisation.MultiGaussFitter`/`FloatingMultiGaussFitter.doMultiFit` —
  finite-difference coordinate descent → `LevenbergMarquardtOptimizer` + `LeastSquaresBuilder`.
- `DataProcessing.Interpolator.interpolateLinearly` (`:14-40`) and `DSPProcessor.upScale`
  (`:186-226`) → `analysis.interpolation.LinearInterpolator`.
- `IO.DataWriter.convertArrayToString` (`:123-132`) → `String.join`/`StringUtils.join`;
  `transposeValues` (`:134-153`) → `MatrixUtils.createRealMatrix(...).transpose()`.
- `IO.DataReader.readTabbedFile` (`:84-131`) → `CSVParser` + `CSVFormat.TDF`; NaN parse
  (`:54-60`) → `NumberUtils.toDouble`.
- `IO.File.FileName.makeValidFileName` (`:26-36`) → `FilenameUtils.removeExtension`.
- `UtilClasses.GenUtils.checkRange` (`:111-119`) → `Math.floorMod`; `checkFileSep`
  (`:154-167`) → `StringUtils.replaceChars`.
- `UtilClasses.Utilities.getDate` (`:154-158`) → `java.time.DateTimeFormatter`;
  `checkRange` (`:143-152`) → `Math.floorMod`.
- `Particle.ParticleArray` duplicates TrackMate `SpotCollection` storage/add;
  `Particle.refineCentroid` reimplements standard centroid localization.
- `Math.Clustering.ClusterablePoint` → `ml.clustering.DoublePoint` (for the `Clusterable` role).

**Internal duplication:**

- `Cell.Cell.compareTo` ≡ `Cell.compare`; `Cell3D.Cell3D` same.
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
   G8 items, then the language-feature readability pass (G6). (Phase G)

Each milestone is independently shippable. M1 is the immediate next step and
unblocks the ADAPT plan's M10 (upstream dependency hygiene).
