# platform-copybook

The shared copybook DEPENDENCY for DCRE. Collections, payments and mandates all read files cut by
the same toolkit, so the layout tables and the reader are one library that each service imports in
its own `build.gradle`, not a fork per family.

This is a dependency, never a shared instance. No service code lives here and no service runs from
here: `crr`, `prr` and `mrr` stay separate deployables with separate databases, and each declares
`za.co.fnb.dcre:platform-copybook:0.1.0` for itself. That is Factor II of the
12FactorApp Alignment (https://12factor.net/dependencies), explicit declaration and isolation of
dependencies.

## What it holds

| Type | What it is |
|---|---|
| `LayoutField` | one field's NAME and WIDTH. No offset, by design |
| `FixedWidthLayout` | a field table; offsets DERIVE from the widths in order |
| `Layouts` | the recovered collections tables: header 109, V1 161, V2 169, V3 204 |
| `MandateLayouts` | the mandate instruction book tables: header 109, detail 285 (SYNTHETIC, A-61) |
| `LayoutResolver` | picks the layout for ONE record, by the format's own discriminator |
| `CopybookReader` | reads a whole file into records, resolving the layout per record |
| `FixedWidthRecord` | index, raw line, layout, and `field(name)` |
| `ShortRecordException` | a record shorter than its own layout, carrying the record index |

## API

```java
LayoutResolver collections = (index, line) -> index == 0 ? Layouts.HEADER : switch (line.length()) {
        case 204 -> Layouts.DETAIL_V3;
        case 169 -> Layouts.DETAIL_V2;
        case 161 -> Layouts.DETAIL_V1;
        default  -> throw new FileFatalException("detail LRECL " + line.length() + " matches no layout");
};

List<FixedWidthRecord> records = CopybookReader.read(path, collections);
String ref = records.get(1).field("end_to_end");
```

## Four contracts worth knowing before you use it

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

## Build

```bash
./gradlew test publishToMavenLocal
```

Services consume it from mavenLocal as `za.co.fnb.dcre:platform-copybook:0.1.0`.
`platform-files` depends on it too: the generic slicing lives here, and the family-specific exchange
plumbing (`ExchangeLayout`, `R31Filename`, `StagedWrite`) stays there. Specific depends on generic.
