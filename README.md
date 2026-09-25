# IAClassLibrary

Image Analysis Class Library (`net.calm.iaclasslibrary`) — a reusable library of
image-processing and analysis primitives for the ImageJ/Fiji ecosystem. It is
consumed as a dependency by ADAPT, `TrackerLibrary`, and `AdaptDataProcessing`.

It is a **library**, not a plugin — there is no `main` method.

## Building

Requires JDK 21 and Maven (a wrapper is included):

```sh
./mvnw verify
```

On Windows, use `mvnw.cmd` in place of `./mvnw`.

The fastest way to check that a change compiles:

```sh
./mvnw -q -DskipTests compile
```

Tests use JUnit 5 (Jupiter); run them with:

```sh
./mvnw test
```

## Consuming

The library is distributed via JitPack. Downstream projects (ADAPT,
`TrackerLibrary`, `AdaptDataProcessing`) pin a tagged release (e.g. `v2.0.0`).

## API Documentation

Javadoc for the public API is published to
<https://djpbarry.github.io/IAClassLibrary/> on every push to `development`
(see `.github/workflows/javadoc.yml`).

## Packages

Source lives under `src/main/java/net/calm/iaclasslibrary/`, organised into
small sub-packages by function:

| Package | Purpose |
| --- | --- |
| `IAClasses` | Legacy primitives (`Region`, `Pixel`, `Utils`, ...). |
| `Cell` / `Cell3D` / `Particle` | Domain model (cells, regions, spots). |
| `Process` | Processing pipeline (`MultiThreadedProcess`, `ProcessPipeline`). |
| `IO` | File I/O, including Bio-Formats loaders (`IO.BioFormats`). |
| `ImgLib2` | ImgLib2 image operations and converters. |
| `Math` | Statistics, clustering, and optimisation. |
| `Segmentation` / `Thresholding` / `Binary` | Image segmentation utilities. |
| `Stacks` / `Image` / `ImageProcessing` | Image stack and processing helpers. |
| `Extrema` / `Curvature` / `Fluorescence` / `Trajectory` | Analysis utilities. |
| `Graph` / `Overlay` / `Lut` / `Profile` | Miscellaneous helpers. |

## License

GPL-3.0-or-later. See [`LICENSE`](LICENSE).
