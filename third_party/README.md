# Vendored third-party artifacts

These libraries were only published to jcenter, which has shut down. They are
kept here as a local Maven repository (`third_party/maven`) so the build needs
no extra mirror. The files are the original, unmodified published artifacts.

| Artifact | Version | Source | License |
|---|---|---|---|
| com.zzhoujay.richtext:richtext | 3.0.4 | https://github.com/zzhoujay/RichText | MIT |
| com.zzhoujay.markdown:markdown | 1.0.5 | https://github.com/zzhoujay/Markdown | MIT |

## Source modules

`third_party/dataautoaccess/` holds the DataAutoAccess source (Apache-2.0,
https://github.com/ThirtyDegreesRay/DataAutoAccess, tag v1.2.8) as three Gradle
modules wired in `settings.gradle`. Changes from upstream: new build files;
the processor registers via `META-INF/services` instead of `@AutoService`, and
reports `SourceVersion.latestSupported()`; the library manifest is empty (the
namespace lives in `build.gradle`).
