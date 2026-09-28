# platform-copybook

> Part of the DCRE fleet. For the fleet map, the rulings and the diagrams that specify every stage, start at the [DCRE design register](https://github.com/sean-huni/dcre-design-register); the complete list of live repositories is its [Repositories](https://github.com/sean-huni/dcre-design-register#repositories) table.

The shared copybook dependency for DCRE: fixed-width layout tables and a streaming,
per-record-layout reader for the files that collections, payments and mandates all receive from the
same toolkit.

## What it does

A library, not a service: no service code lives here, it has no runtime of its own, and nothing runs
from it. Each consuming reader stays a separate deployable with its own database and declares
`za.co.fnb.dcre:platform-copybook:0.2.0` for itself, which is Factor II of the 12FactorApp Alignment
(https://12factor.net/dependencies): explicit declaration and isolation of dependencies. Plain Java,
zero runtime dependencies.

Public API, package `za.co.fnb.dcre.platform.copybook`:

| Type | What it is |
|---|---|
| `LayoutField` | one field's NAME and WIDTH. No offset, by design |
| `FixedWidthLayout` | a field table; offsets DERIVE from the widths in order; `slice(line, field)`, `length()` |
| `Layouts` | the recovered collections tables: `HEADER` 109, `DETAIL_V1` 161, `DETAIL_V2` 169, `DETAIL_V3` 204 |
| `MandateLayouts` | the mandate instruction book tables: `HEADER` 109, `DETAIL` 285 (SYNTHETIC, A-61) |
| `LayoutResolver` | picks the layout for ONE record, by the format's own discriminator |
| `CopybookReader` | `forEachRecord` streams a record at a time and returns the count; `read` gives the whole list |
| `FixedWidthRecord` | index, raw line, layout, and `field(name)` |
| `ShortRecordException` | an `IllegalArgumentException` for a record shorter than its own layout, carrying the record index, actual and declared length |

```java
LayoutResolver collections = (index, line) -> index == 0 ? Layouts.HEADER : switch (line.length()) {
        case 204 -> Layouts.DETAIL_V3;
        case 169 -> Layouts.DETAIL_V2;
        case 161 -> Layouts.DETAIL_V1;
        default  -> throw new FileFatalException("detail LRECL " + line.length() + " matches no layout");
};

// Streaming, one record held at a time. Returns the record count.
long records = CopybookReader.forEachRecord(path, collections, record -> sink.accept(record));

// Or the whole file at once, when random access to every record is genuinely needed.
String ref = CopybookReader.read(path, collections).get(1).field("end_to_end");
```

`FileFatalException` in the example is the caller's own type; the library only requires that a
resolver which cannot answer throws.

**Prefer `forEachRecord`.** These files are large, which is why the partitioned readers exist. `read`
holds the whole file plus a wrapper per record, and a caller that wants only the header and the count
should not pay that.

## Consumers

Counted from each fleet repo's `build.gradle` on `origin/dev` (local clones, not fetched), plus
AGT (checked 2026-09-28):

| Consumer | Version | Types imported in `src/main` |
|---|---|---|
| `crr` | `0.2.0` | `CopybookReader`, `FixedWidthLayout`, `FixedWidthRecord`, `LayoutResolver`, `Layouts`, `ShortRecordException` |
| `prr` | `0.2.0` | same as `crr` |
| `mrr` | `0.2.0` | `FixedWidthLayout`, `MandateLayouts` |
| `ctv` | `0.2.0` | declared, no import in `src/main` |
| AGT, the other platform libraries | not consumers | |

`platform-files` does NOT depend on this library: it carries its own `FixedWidthLayout`,
`LayoutField`, `Layouts` and `MandateLayouts` in package `za.co.fnb.dcre.platform.files`.

## Architecture and principles

Four contracts worth knowing before you use it:

**Offsets are DERIVED, never declared.** A field carries a width and its position in the table. A
gap or an overlap between two fields cannot be written down at all, so the class of defect where one
field's absolute offset is a column adrift does not exist here.

**The layout is chosen PER RECORD.** A DCRE file is multi-record-type. The resolver sees the record
INDEX as well as the line, because a generator pads the header out to the detail LRECL: a padded
169-byte header is length-identical to a V2 detail and only its position at index 0 tells them
apart. A resolver that cannot answer throws, and a `null` return is rejected: fail closed.

**Fields are returned RAW and UNTRIMMED.** A trailing space is data. A-83 is a mandate MsgId whose
trailing sequence is space-padded in the copybook header and zero-padded in the filename, and a
trimming reader cannot see that defect at all. Callers strip where they know stripping is safe.

**Decoding is ISO_8859_1, byte-transparent**: one byte is one char, so offsets and slice positions
never drift on non-UTF-8 bytes and a legacy byte such as `0xE9` survives the read intact. Strict
UTF-8 decoding anywhere in the pipeline crashes or mangles it.

A record shorter than the layout IT resolved to fails closed with a `ShortRecordException` naming
the record. A LONGER record is accepted and sliced by that layout's columns, so a padded header and
a real one ingest identically.

`FixedWidthRecord.toString()` prints the index and the length and nothing else. The generated record
`toString` would print the raw line, which on a detail record is debtor_name, debtor_account and
amount. One `log.debug(record)` downstream would land account numbers durably in the log collector.

## Prerequisites

- Java 25 (`.sdkmanrc`: `java=25-tem`; `build.gradle` sets `sourceCompatibility` /
  `targetCompatibility` 25)
- Gradle wrapper 9.5.1 (committed)
- No Docker, no database, no upstream platform library

## Build and publish

Coordinates: `za.co.fnb.dcre:platform-copybook:0.2.0` (binary and sources jar). Distribution is Maven
Local only; no remote repository is configured.

```bash
./gradlew test publishToMavenLocal
```

```groovy
repositories { mavenCentral(); mavenLocal() }
dependencies {
    implementation 'za.co.fnb.dcre:platform-copybook:0.2.0'
}
```

The library is baked into each consuming reader's image at that service's build; after a change,
publish a NEW version and bump the consumers.

**Why `0.2.0`, not `0.1.0`.** `0.1.0` was the first-attempt API: it carried `CopybookLayout` and
`FieldSpec` and a `read(Path, CopybookLayout)` that bound one layout to a whole file. Deleting two
public types and changing a public signature is a source-incompatible change, and SemVer
(https://semver.org/, clause 8 on the 0.y.z line) makes that a minor bump rather than a silent
in-place mutation of a fixed coordinate.

The lesson that earned this paragraph: republishing a fixed coordinate with types REMOVED broke every
trunk branch that still imported them, and nothing reported it, because a version that does not move
gives a consumer no signal. One coordinate, one artifact.

## Configuration

None. The library reads no environment variables, binds no properties and ships no
`application.yml`; the layout choice is the caller's `LayoutResolver`.

## Testing

```bash
./gradlew test
```

JUnit Jupiter (BOM 6.0.2) and AssertJ, no Docker. 4 test classes, 28 `@Test` methods (counted from
`src/test` at HEAD): `CopybookReaderTest`, `FixedWidthLayoutTest`, `LayoutsTest`,
`MandateLayoutsTest` (including the header 109 / detail 285 widths).

## Related repositories

The complete, current list of live DCRE repositories (stage services, orchestrator, platform libraries, infra and tooling) lives in one place: the [DCRE design register README](https://github.com/sean-huni/dcre-design-register#repositories). Deprecated and archived repositories are deliberately absent from it. This README does not copy that list, so it cannot drift.

- Design register: https://github.com/sean-huni/dcre-design-register (start at `docs/specs/DESIGN-REGISTER.md`; the diagrams in `docs/diagrams/` are the specification)
