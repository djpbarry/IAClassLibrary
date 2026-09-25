# AGENTS.md

## Project overview

Image Analysis Class Library (`net.calm.iaclasslibrary`) - a Java library in the
ImageJ/Fiji ecosystem, maintained by David Barry (Francis Crick Institute). It
provides reusable image-processing and analysis primitives consumed by other
Fiji/ImageJ plugins, not a runnable application (there is no `main` method).

- Build system: **Maven** (canonical), parent `org.scijava:pom-scijava` 45.1.0
- Version in `pom.xml`: `2.0.0-SNAPSHOT` (working toward `2.0.0`)
- Key dependencies: `net.imagej:ij` / `imagej`, Bio-Formats (`ome`,
  `loci.formats`), ImgLib2 (`io.scif`, `net.imglib2`), TrackMate, MorphoLibJ,
  mcib3d-core, Apache Commons Math3/Lang3/CSV, imagescience.

## Build / test

- Build/verify: `./mvnw verify`
- CI command (see `.github/workflows/maven.yml`): `./mvnw --batch-mode --no-transfer-progress verify`
- Required JDK: **21** (per the CI workflow `setup-java` step)
- There is a **Maven wrapper** (`mvnw`/`mvnw.cmd`) - use it instead of a system `mvn`.
- Tests use **JUnit 5** (Jupiter) under `src/test/java`; run with `./mvnw test`.
  "Verify" means compile + package + SciJava parent-POM checks + tests.
- `mvn -q -DskipTests compile` is the fastest way to check a change compiles.

### Legacy Ant build (removed)

The NetBeans Ant build (`build.xml` + `nbproject/`) has been **deleted** (Decision 6). It never ran on a clean machine:

- `build.xml` has a `-pre-init` target that runs a Perl script via a hardcoded
  machine-specific path (`C:\Users\barryd\Strawberry\perl\bin\perl.exe`).
- `nbproject/project.properties` lists hundreds of hardcoded
  `file.reference.*` jars under `C:\Users\Dave\fiji-nojre\Fiji.app\...`.
- The Ant build will not run on a clean machine. Maven + `pom.xml` is the source
  of truth for dependencies and the build.

`.gitignore` ignores `/build/` and `/dist/` (Ant outputs) and `/target/` (Maven).

## Architecture

All code is under `src/main/java/net/calm/iaclasslibrary/`, organized into many
small subpackages by function. There are two generations of code that coexist:

- **`IAClasses`** - the original monolith/legacy classes (`Region`, `Pixel`,
  `Pixel2`, `Region2`, `Region3D`, `Utils`, `StaticConstants`, `DataStatistics`,
  `BoundaryPixel`, etc.). Uses ImageJ `ImageProcessor`, `ImagePlus`, `Roi`, and
  raw `short[]`/`float[]` for pixel/coordinate data.
- **Newer, refactored packages** - `Cell`, `Cell3D`, `Particle`, `Process`,
  `IO`, `ImgLib2`, `Math`, etc. These wrap the legacy classes or replace them
  with cleaner abstractions.

### Two image representations (important)

The codebase uses two parallel image paradigms. Know which one a class belongs
to before editing:

1. **ImageJ `ImagePlus`/`ImageStack`** - used throughout `IO`, `Process`, `Cell`,
   and `IAClasses`. Image loading is via `IO.BioFormats.BioFormatsImg`, which
   wraps a Bio-Formats `ImageReader` + `ImporterOptions` + `IMetadata` and loads
   pixel data into an `ImagePlus`.
2. **ImgLib2 `Img`** - used only in the `ImgLib2` package (e.g.
   `ImgLib2.ImageOpener`, which opens via SCIFIO `io.scif.img.ImgOpener`), plus
   the `ImgLib2.Filters` and `ImgLib2.SimpleConverters` subpackages.

### Bio-Formats loading

- `IO.BioFormats.BioFormatsImg` - the primary loader. `setId`/`checkID`/`loadPixelData`
  (series, channel range, dimension order), spatial-resolution accessors, and
  ImageJ `ImagePlus` output. Static separators: `SERIES_SEP='-'`, `LABEL_SEP='_'`,
  `REPLACEMENT_SEP='.'` are used to reformat filenames (`reformatFileName`).
- `IO.BioFormats.LocationAgnosticBioFormatsImg` - a subclass that instead drives
  Bio-Formats through the `Importer`/`ImportProcess`/`ImporterOptions` API and
  reads metadata from `ome.xml.meta.IMetadata`. Its constructors take an options
  string rather than a raw file id. A recent commit ("Removed references to
  LocationAgnosticBioFormatsImg") suggests this class may be on its way out; check
  for current usages before relying on it.

### Process pipeline (threading model)

- `Process.MultiThreadedProcess` is the abstract base for all processing steps.
  It **extends `Thread`** and also implements `Callable<BioFormatsImg>`
  (`call()` just returns the image). Subclasses implement `setup(...)`, `run()`,
  and `duplicate()`.
- Processes are wired together into a `Process.ProcessPipeline` (a
  `LinkedList<MultiThreadedProcess>`). Each process knows its `inputs` and
  `outputDests`; `duplicate()` plus `updateOutputDests`/`updateInput` support
  re-running/rewiring a pipeline.
- `getOutput()` lazily calls `start()` then `join()` on the thread and returns a
  **duplicate** `ImagePlus` (the internal `output` field is duplicated to avoid
  aliasing).
- The convention is a `MultiThreadedX` base class paired with a `RunnableX`
  worker (e.g. `MultiThreadedImageLoader` + `RunnablePixelLoader`,
  `MultiThreadedColocalise` + `RunnableColocalise`). Workers extend `Thread` and
  are started/joined inside `run()` rather than submitted to the `ExecutorService`
  in practice (see `MultiThreadedImageLoader.run()`).

### Cell / Particle model

- `Cell.Cell` extends `Cell.CellRegion` and implements `Comparable<Cell>` and
  `Comparator<Cell>` (ordered by `ID`). It holds `ArrayList<Particle>` (detected
  particles), `ArrayList<CellRegion>` (regions), and `ArrayList<Cell>` (links to
  other cells across time).
- `Cell.CellRegion` wraps an ImageJ `Roi` + `ImageStatistics`. Subclasses include
  `Nucleus` and `Cytoplasm`. `Cell.getNucleus()` and `getRegion(CellRegion)` find
  regions by `instanceof`.
- `Particle.Particle` extends TrackMate's `Spot`. It stores its own `x`, `y`,
  `t` (frame), `magnitude`, `link`, `colocalisedParticle`, and `region`. Note the
  field `t` means frame number, not time.
- `Cell3D` has a parallel set of 3D classes (`Cell3D`, `CellRegion3D`,
  `Nucleus3D`, `Cytoplasm3D`, `Spot3D`, `SpotFeatures`).

### Other notable packages

- `Math` - statistics (`Histogram`, `MSS`, `Rand`), `Clustering`
  (clusterable points, stairs/zero-slope optimisers), `Optimisation` (Gaussian
  fitters: `GaussianFitter3D`, `IsoGaussianFitter`, `NonIsoGaussianFitter`,
  `MultiGaussFitter`, `FloatingMultiGaussFitter`, `RoiFitter`, `PlateFitter`),
  and `Correlation`.
- `IO` - generic file I/O (`DataReader`/`DataWriter`, `FileReader`, `PropertyWriter`,
  `InputFileOpener`/`OutputFolderOpener`) plus `IO.File` filters.
- `Stacks`, `Image`, `ImageProcessing`, `Segmentation`, `Thresholding`,
  `Binary`, `Extrema`, `Curvature`, `Fluorescence`, `Trajectory`, `Graph`,
  `Overlay`, `Lut`, `Profile` - domain-specific utilities.

## Conventions

- **Logging/errors**: `GenUtils.logError(Exception, String)` logs via ImageJ
  (`IJ.log`) and `printStackTrace()`. `GenUtils.error(String)` beeps and calls
  `IJ.error`. Use these, not `System.out`, for ImageJ-facing code.
- **Constants**: foreground/background pixel values are `0`/`255`
  (`IAClasses.StaticConstants.FOREGROUND`/`BACKGROUND`, and `Region.MASK_FOREGROUND`
  = 0 / `MASK_BACKGROUND` = 255).
- **Output naming**: `MultiThreadedProcess.constructOutputName` uses
  `OUTPUT_SEP = "_"` and `StringUtils.substringBefore` to build names as
  `<basename>_<label>`.
- **Units/spatial calibration**: distances are handled with OME `ome.units.quantity.Length`;
  `getCalibration(series)` returns `[xy, xy, z]` physical sizes.
- **Style**: Java with 4-space indentation; older files carry auto-generated
  NetBeans "To change this template..." headers and use raw generic types
  (`new LinkedList()`), newer files use `@Override` and diamond syntax. Match the
  surrounding file rather than "modernizing" it.
- **License headers are inconsistent**: `pom.xml` declares BSD-2, but many source
  files carry GPL-v3 headers and others have NetBeans placeholder headers. Do not
  add/rewrite license headers; leave existing ones alone.

## Gotchas

- **Tests exist** under `src/test/java` (JUnit 5). Run with `./mvnw test`.
- **`BioFormatsImg.validID` is never set to `true`** anywhere, so
  `isValidID()` always returns `false`. Don't rely on it for validity checks.
- **`BioFormatsImg.clearImageData()` is a no-op** (the body is commented out,
  with a note that clearing caused GUI problems). The internal `ImagePlus` is
  intentionally kept.
- **`BioFormatsImg.getLoadedImage()` returns the internal `img` directly** (the
  duplicate logic is commented out) - callers get a shared reference, not a copy.
- **`MultiThreadedProcess.getOutput()`** starts and joins the thread; calling it
  on an already-started/finished thread can throw `IllegalThreadStateException`,
  which is caught and logged.
- **`Particle` shadows `Spot`'s coordinates**: it redefines `x`/`y` fields while
  also calling `super(x, y, ...)`. Its `getX()`/`getY()` return the shadowed
  fields. Be careful when mixing `Spot` and `Particle` APIs.
- **`Particle`'s `t` field** is the frame/time index, and the getter is
  `getFrameNumber()`; the id getter is the non-conventional `getiD()`.
- **Legacy package vs refactored package duplication**: some concepts exist in
  both `IAClasses` (e.g. `Region`, `Region3D`) and newer packages (`Cell`,
  `Cell3D`). Check whether a symbol you plan to change is the legacy or current
  one before editing.
- **Raw generic types and no `@Override`** appear in older files; do not "fix"
  them as part of unrelated changes.
- The only tracked metadata/build config that matters for CI is `pom.xml` and
  `.github/workflows/maven.yml`. Changes to `build.xml`/`nbproject/` have no
  effect on CI.
