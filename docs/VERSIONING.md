# Release identity

The current release is **1.1.0**. Its embedded FML version and filename agree:

```text
tnkprogression-1.1.0-neoforge-1.21.1.jar
```

| Label | Meaning |
| --- | --- |
| `1.1.0` | Current CurseForge-aligned release with the corrected Origin token texture |
| `1.5.0-private.1` | Earlier development label for the direct-XP/Ascension baseline, before the requested release-number alignment |
| `1.4.0-private.1` | Historical Ascension distribution used as the direct-XP development baseline |

The old development labels are retained as history. Comparing those numbers alone does not establish that a download is newer. Check its embedded metadata and recorded SHA-256. Renaming an old JAR does not update its embedded version.

The default branch contains the current release source and guides. The [release receipt](../releases/1.1.0.json) records the exact token-fix source commit and delivered JAR checksum; later documentation commits do not create another mod version. [Verification and final JAR checksum](VERIFICATION-1.1.0.md).

Keep each deliberate release number stable through routine documentation edits. Any future changed published artifact needs a deliberate new release and its own checksum; retain historical identities rather than overwriting them.

[Changelog](../CHANGELOG.md) · [Project home](../README.md)
