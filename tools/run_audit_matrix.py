#!/usr/bin/env python3
"""Run already compiled opt-in fixtures in staged, disposable dedicated servers.

First compile verification/abyss using the Gradle properties in verification/README.md.
Supply staged run directories with original mod JARs, accepted EULA and test data.
This reuses ModDev's generated target runtime; it does not modify dependencies.
"""
from pathlib import Path
import argparse, concurrent.futures, json, subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--java', required=True, type=Path)
parser.add_argument('--runs', required=True, type=Path)
parser.add_argument('--cases', nargs='+', required=True)
parser.add_argument('--jobs', type=int, default=2)
args = parser.parse_args()
repo = Path(__file__).resolve().parents[1]
runtime = repo / 'build/moddev'
classpath = (runtime/'serverLegacyClasspath.txt').read_text().splitlines()
classpath.append(str(runtime/'artifacts/neoforge-21.1.262.jar'))
folders = ':'.join('tnkprogression%%'+str(repo/path) for path in ('build/classes/java/main','build/resources/main'))
base = [str(args.java), '-Xmx1536m', '-Dfml.modFolders='+folders,
        '-cp', ':'.join(classpath), '@'+str(runtime/'serverRunVmArgs.txt')]

def run(label):
    directory = args.runs / label
    suite = 'direct-xp' if label.startswith('direct-') else 'boss-activity' if label == 'boss-activity' else 'abyss'
    cmd = base + ['-Dtnkprogression.abyssSmoke='+str(suite=='abyss').lower(),
                  '-Dtnkprogression.directXpSmoke='+str(suite=='direct-xp').lower(),
                  '-Dtnkprogression.bossActivitySmoke='+str(suite=='boss-activity').lower(),
                  '@'+str(runtime/'serverRunProgramArgs.txt')]
    report = directory / 'abyss-validation.json'
    report.unlink(missing_ok=True)
    log = directory / 'audit-console.log'
    with log.open('w') as output:
        result = subprocess.run(cmd, cwd=directory, stdout=output, stderr=subprocess.STDOUT, timeout=240)
    text = log.read_text()
    marker = {'abyss':'TNK_ABYSS_SMOKE_OK', 'direct-xp':'TNK_DIRECT_XP_SMOKE_OK', 'boss-activity':'TNK_BOSS_ACTIVITY_SMOKE_OK'}[suite]
    ok = result.returncode == 0 and marker in text and '_SMOKE_FAILED' not in text
    if suite=='abyss':
        ok = ok and report.exists() and json.loads(report.read_text()).get('status')=='passed'
    summary = {'case':label,'status':'passed' if ok else 'failed','exit_code':result.returncode,'marker':marker}
    print(json.dumps(summary), flush=True)
    return summary

with concurrent.futures.ThreadPoolExecutor(max_workers=args.jobs) as executor:
    results = list(executor.map(run,args.cases))
(args.runs/'matrix-results.json').write_text(json.dumps(results,indent=2)+'\n')
raise SystemExit(0 if all(r['status']=='passed' for r in results) else 1)
