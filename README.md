# platform-copybook

The shared fixed-width copybook reader for DCRE. Collections, payments and mandates all read a
copybook cut by the same toolkit, so the reader is a module dependency rather than a fork per
family: a fix here reaches all three instead of drifting.

Extracted from the live `crr` collections boundary reader (SCRUM-107). `crr` was repointed at this
module first, and its 38 tests are the control that the extraction moved code without changing
behaviour.

## API

```java
CopybookLayout layout = CopybookLayout.of(169, List.of(
        new FieldSpec("record_type", 0, 2),
        new FieldSpec("end_to_end", 2, 35)));

List<FixedWidthRecord> records = CopybookReader.read(path, layout);
String ref = records.get(0).field("end_to_end");
```

- `CopybookLayout.of(int lrecl, List<FieldSpec> fields)`: a record length plus the field table that
  cuts it. An empty field table declares a pure record-length gate.
- `CopybookReader.read(Path file, CopybookLayout layout)`: the whole file as records.
- `FixedWidthRecord.field(String name)`: the field's columns.

## Two contracts worth knowing before you use it

**Fields are returned RAW and UNTRIMMED.** A trailing space is data. A-83 is a mandate MsgId whose
trailing sequence is space-padded in the copybook header and zero-padded in the filename, and a
trimming reader cannot see that defect at all. Callers strip at the point where they know stripping
is safe.

**Decoding is ISO_8859_1, byte-transparent**: one byte is one char, so offsets and slice positions
never drift on non-UTF-8 bytes and a legacy byte such as `0xE9` survives the read intact. Strict
UTF-8 decoding anywhere in the pipeline crashes or mangles it.

A record shorter than the declared LRECL fails closed with a `ShortRecordException` naming the
record. A LONGER record is accepted and sliced by the layout's columns: generator files pad the
header out to the detail LRECL while real files do not, and both must ingest identically.

## Build

```bash
./gradlew test publishToMavenLocal
```

Services consume it from mavenLocal as `za.co.fnb.dcre:platform-copybook:0.1.0`.
