# Release identity

The current audited build is **1.1.6**, on `audit/technical-checkup-1.1.6`. It has not been published to CurseForge by this audit. Its embedded FML version and filename agree:

```text
tnkprogression-1.1.6-neoforge-1.21.1.jar
```

| Label | Meaning |
| --- | --- |
| `1.1.6` | Technical audit and compatibility/performance fixes; see the audit report and artifact checksum |
| `1.1.5` | Previously verified published title-catalogue release; audit source baseline |
| `1.1.0` | Historical CurseForge-aligned release with the corrected Origin token texture |
| `1.5.0-private.1` | Earlier development label for the direct-XP/Ascension baseline, before the requested release-number alignment |
| `1.4.0-private.1` | Historical Ascension distribution used as the direct-XP development baseline |

The old development labels are retained as history. Comparing those numbers alone does not establish that a download is newer. Check its embedded metadata and recorded SHA-256. Renaming an old JAR does not update its embedded version.

The default branch remains the older 1.1.0 line; this audit preserves it. The new branch starts from verified 1.1.5 source `a01e74172cb266896cb1ee85d508560a5660eaad`. See [the 1.1.6 technical report](TECHNICAL-AUDIT-1.1.6.md) and `validation-1.1.6/artifact.json`. The historical [1.1.0 release receipt](../releases/1.1.0.json) remains unchanged.

Keep each deliberate release number stable through routine documentation edits. Any future changed published artifact needs a deliberate new release and its own checksum; retain historical identities rather than overwriting them.

[Changelog](../CHANGELOG.md) · [Project home](../README.md)
