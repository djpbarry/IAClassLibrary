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

## Current state (context for the plan)

- IAClassLibrary (`net.calm.iaclasslibrary`) is a **Java library** (not a
  runnable plugin) in the ImageJ/Fiji ecosystem, providing image-analysis
  primitives consumed by ADAPT, `TrackerLibrary`, and `AdaptDataProcessing`.
- **Build:** Maven, parent `org.scijava:pom-scijava:45.1.0`, version
  `2.0.0-SNAPSHOT` (working toward the `2.0.0` release), declared license
  **GPL-3.0-or-later** (`license.licenseName=gpl_v3`). *(Reconciled 2026-09-24.)*
- **CI:** `.github/workflows/maven.yml` runs `./mvnw --batch-mode
  --no-transfer-progress verify` on **JDK 21** with dependency caching.
  *(Reconciled 2026-09-24.)*
- **Tests:** JUnit 5 (Jupiter) harness added — `src/test/java`, 10 classes,
  20 tests as of 2026-09-25. No lint/format tooling. (Phase C done for now.)
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
  A prior commit ("Removed references to LocationAgnosticBioFormatsImg") suggests
  the latter may be orphaned — verify before relying on it.
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

### Verified gotchas (from the code review)

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

**Status (2026-09-24): B1 items 1–3 are done** (LICENSE added, `pom.xml`
corrected to GPL-3); B1 item 4 (header tidy-up) is deferred; **B2:** versioning infrastructure done (`2.0.0-SNAPSHOT` + release plugin); the
`v2.0.0` tag is deferred to release time. **B3:** resolved by Decision 3
(TrackMate 8.0.0 via the parent); cross-repo coordination pending (M6).

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

ADAPT pins IAClassLibrary at commit `fe92f24c6e`, which is **after** the only
tag `v1.032` (`da53d73`) and after the version bump to `1.0.37`. Release `2.0.0`
and tag `v2.0.0` (reconciling the `v1.032` vs `1.0.37`
naming skew) so ADAPT and the other libraries can pin to a tag instead of a
commit hash.

### B3. Resolve the TrackMate version web (blocks ADAPT's Phase G3)

**Resolved by Decision 3 (2026-09-24).** IAClassLibrary declares
`sc.fiji:TrackMate` **unversioned** (parent-managed); `pom-scijava:45.1.0`
manages it to **8.0.0** (verified). This supersedes the earlier "7.x for now"
note (the coordinated Java 21 / TrackMate 8.x move is now in progress).
Remaining: `TrackerLibrary` (7.10.0) and ADAPT (7.14.0) must bump to 8.0.0 and
Java 21 in lockstep (Phase F / M6).

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
| G2 tag all three | B2 | tag `master` after `fe92f24`, reconcile version |
| G3 TrackMate version web | B3 | single TrackMate version policy |
| G0 "no CI" | A2 | CI already exists; harden it |

Coordinate the Java-target decision (Decision 3) with ADAPT's Decision 3.
IAClassLibrary now targets **Java 21** (parent `pom-scijava:45.1.0`);
`TrackerLibrary`, `AdaptDataProcessing`, and ADAPT must bump to Java 21 in
lockstep and re-pin their IAClassLibrary dependency to the new tag.

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
2. **Versioning — semver.** Jump directly to major version `2.0.0` (development
   version `2.0.0-SNAPSHOT`) to reflect the scale of the upcoming changes; tag
   `v2.0.0` on release. The existing `v1.032` tag is a mislabel of `v1.0.32`;
   use the `vX.Y.Z` form going forward. Version bumps are automated with
   `maven-release-plugin` (`tagNameFormat=v@{project.version}`); the JAR version
   is read from the manifest, not `pom.xml` (see `REVISION_LOG.md`).
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

---

## Suggested sequencing & milestones

1. **M1 — Foundations (low risk, high value):** `.gitignore`, Maven wrapper, CI
   hardening (pin Java 21), delete the legacy Ant build, remove
   `MultiThreadedStarDist` + commented-out debug code, add GPL-3 `LICENSE` +
   fix pom metadata, bump to `2.0.0` and tag `v2.0.0`. (Phase A, B, D1)
   *Status (2026-09-24): `.gitignore`/wrapper/CI/Ant-removal/LICENSE+pom are
   done, plus D1 (StarDist + debug-code removal). Remaining: B2 (release `2.0.0`
   + tag `v2.0.0`).*
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
   with the other three repos, so ADAPT can pin to tags. (Phase F)

Each milestone is independently shippable. M1 is the immediate next step and
unblocks the ADAPT plan's M10 (upstream dependency hygiene).
