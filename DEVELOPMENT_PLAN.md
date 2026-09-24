# IAClassLibrary Development Plan

This plan outlines a multi-phase effort to make IAClassLibrary more robust,
maintainable, and consumable as a library, while resolving the issues flagged by
the ADAPT project's `DEVELOPMENT_PLAN.md` (which treats IAClassLibrary as one of
its three upstream JitPack dependencies). It is grounded in a full review of this
repository as of the plan's writing.

## Current state (context for the plan)

- IAClassLibrary (`net.calm.iaclasslibrary`) is a **Java library** (not a
  runnable plugin) in the ImageJ/Fiji ecosystem, providing image-analysis
  primitives consumed by ADAPT, `TrackerLibrary`, and `AdaptDataProcessing`.
- **Build:** Maven, parent `org.scijava:pom-scijava:40.0.0`, version `1.0.37`,
  declared license **Simplified BSD** (`license.licenseName=bsd_2`).
- **CI:** `.github/workflows/maven.yml` runs
  `mvn --batch-mode --update-snapshots verify` on JDK 11. No Maven wrapper.
- **Tests:** none. There is no `src/test`, no test framework, no lint/format
  tooling.
- **License:** no `LICENSE` file exists; source headers are inconsistent —
  roughly half GPL-3, roughly a third NetBeans "change this header" stubs, and a
  handful with no header at all. The `pom.xml` declares BSD-2, so the effective
  license is ambiguous (same contradiction ADAPT's plan flags for its other two
  dependencies).
- **Legacy build:** `build.xml` + `nbproject/` are a stale NetBeans Ant build
  with hardcoded machine-specific paths (`C:\Users\barryd\...` Perl in
  `build.xml`, hundreds of `C:\Users\Dave\fiji-nojre\Fiji.app\...` jar refs in
  `nbproject/project.properties`). It does not run on a clean machine.
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

### A1. Add a Maven wrapper

Pin a reproducible toolchain by committing `mvnw`/`mvnw.cmd` + `.mvn/wrapper/`
so builds don't depend on an unknown system Maven (mirrors ADAPT's A1).

### A2. Harden CI

`.github/workflows/maven.yml` currently runs a single `mvn verify` on JDK 11.

1. Use the wrapper (`./mvnw verify`) instead of a system `mvn`.
2. Cache Maven dependencies (`actions/cache` on `~/.m2/repository`).
3. Confirm and pin the exact JDK; add a matrix over the JDKs the library is
   expected to support once the Java-target decision (Decision 3) is made.
4. Split future `build` and `test` jobs once Phase C lands.

### A3. Add a `.gitignore`

`.gitignore` only excludes `/build/`, `/dist/`, `/target/`. Add `.idea/`,
`*.iml`, and OS files. (`.idea/` is currently an untracked directory in the
working tree.)

### A4. Remove or archive the legacy Ant build

`build.xml` (with its hardcoded Perl `-pre-init` hook) and `nbproject/` are
machine-specific and non-portable. Options: delete them (they are recoverable
from git), or move them under a clearly-labelled `legacy/` directory so Maven is
unambiguously the canonical build. Decision 6 covers which.

---

## Phase B — License & metadata (blocks ADAPT's Phase G1)

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
4. Normalise all source headers to the chosen license; replace the NetBeans
   "change this header" stubs and add missing headers.

### B2. Tag the repo (blocks ADAPT's Phase G2 / Decision 2)

ADAPT pins IAClassLibrary at commit `fe92f24c6e`, which is **after** the only
tag `v1.032` (`da53d73`) and after the version bump to `1.0.37`. Tag current
`master` with a proper semver release (reconciling the `v1.032` vs `1.0.37`
naming skew) so ADAPT and the other libraries can pin to a tag instead of a
commit hash.

### B3. Resolve the TrackMate version web (blocks ADAPT's Phase G3)

IAClassLibrary declares `sc.fiji:TrackMate` **unversioned** (parent-managed),
while `TrackerLibrary` pins `7.10.0` and ADAPT pins `7.14.0`. Agree a single
TrackMate version policy across IAClassLibrary, `TrackerLibrary`,
`AdaptDataProcessing`, and ADAPT (7.x for now; the coordinated 8.x / Java 21 move
is ADAPT's Phase D5 and is out of scope here).

---

## Phase C — Introduce tests (the biggest maintainability win)

There is no test framework and no `src/test`. Follow the SciJava parent's
conventional JUnit 5 setup and wire it into `mvn verify`.

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

### D4. Consolidate the image-loading surface (Decision 5)

Decide the fate of `LocationAgnosticBioFormatsImg` (appears orphaned after
"Removed references to LocationAgnosticBioFormatsImg"). Either delete it or
document its use case relative to `BioFormatsImg`, so there is one obvious way to
open an image.

### D5. Fix the verified gotchas

- Set `validID = true` in `setId`/`checkID`, or remove `isValidID()`.
- Restore or remove `clearImageData()`; document the intended behaviour.
- Make `getLoadedImage()` return a duplicate (or rename to signal aliasing).
- Remove the duplicate `BioFormatsImg` import in `MultiThreadedProcess`.
- Add `@Override` and diamond generics where missing (without gratuitous churn).

### D6. Normalise error handling

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

Coordinate the Java-target decision (Decision 3) with ADAPT's Decision 3
(ADAPT targets Java 11 on a modern JDK). IAClassLibrary currently inherits its
Java target from `pom-scijava:40.0.0` (no explicit override); decide whether to
pin an explicit `maven.compiler.release` so downstream consumers know the floor.

---

## Decisions (open questions to resolve with the maintainer)

1. **License — GPL-3.0 or BSD-2?** The source headers (GPL-3, newer files) and
   `pom.xml` (BSD-2) disagree, and there is no `LICENSE` file. Pick one and
   normalise (Phase B1). GPL-3.0 matches ADAPT's resolution and is compatible
   with BSD-2, so there is no downstream blocker either way.
2. **Tagging/versioning — semver.** Reconcile the `v1.032` tag vs `1.0.37` pom
   version and tag `master` so ADAPT can un-pin from the commit hash.
3. **Java target.** Confirm the compile target (currently inherited from
   `pom-scijava:40.0.0`; CI builds on JDK 11). Pin an explicit
   `maven.compiler.release` and document the build JDK.
4. **Legacy `IAClasses` package.** Keep, deprecate, or remove the legacy
   `Region`/`Pixel`/`Region2`/`Region3D` classes now that `Cell`/`Cell3D`/
   `Particle` exist?
5. **`LocationAgnosticBioFormatsImg`.** Delete the orphaned loader or restore and
   document its role alongside `BioFormatsImg`?
6. **Legacy Ant build.** Delete `build.xml` + `nbproject/`, or archive them under
   `legacy/`?

---

## Suggested sequencing & milestones

1. **M1 — Foundations (low risk, high value):** `.gitignore`, Maven wrapper, CI
   hardening, remove `MultiThreadedStarDist` + commented-out debug code, add
   `LICENSE` + resolve license metadata, tag the repo. (Phase A, B, D1, D6)
2. **M2 — Test harness:** JUnit 5 + a few pure-logic unit tests + CSV golden
   tests. (Phase C)
3. **M3 — Gotchas & hygiene:** fix `validID`/`clearImageData`/`getLoadedImage`
   aliasing, duplicate import, error-handling normalisation. (Phase D5, D6)
4. **M4 — Refactor core:** decompose `RegionGrower`/`MultiThreadedMaximaFinder`,
   resolve legacy-vs-new duplication, decide `LocationAgnosticBioFormatsImg`.
   (Phase D2–D4)
5. **M5 — Documentation:** expand `README.md`, sync `AGENTS.md`, Javadoc public
   API. (Phase E)
6. **M6 — Upstream hand-off:** confirm TrackMate version policy and Java target
   with the other three repos, so ADAPT can pin to tags. (Phase F)

Each milestone is independently shippable. M1 is the immediate next step and
unblocks the ADAPT plan's M10 (upstream dependency hygiene).
