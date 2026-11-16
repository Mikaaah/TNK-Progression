#!/usr/bin/env python3
"""Run a standalone Java 21 JSON/help fixture against an existing baseline JAR.
This never invokes Gradle, starts Minecraft, or writes the mod's source/JAR.
"""
from pathlib import Path
import argparse
import hashlib
import os
import shutil
import subprocess
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--baseline-jar', type=Path, required=True)
parser.add_argument('--minecraft-jar', type=Path, required=True,
                    help='Mojang-mapped NeoForge merged development classes')
parser.add_argument('--runtime-jar', type=Path, action='append', default=[])
parser.add_argument('--dependencies-dir', type=Path,
                    default=Path.home() / '.gradle/caches/modules-2/files-2.1')
parser.add_argument('--java-home', type=Path, default=os.environ.get('JAVA_HOME'))
parser.add_argument('--warmup', type=int, default=2000)
parser.add_argument('--iterations', type=int, default=10000)
parser.add_argument('--rounds', type=int, default=3)
args = parser.parse_args()
if min(args.warmup, args.iterations, args.rounds) <= 0:
    parser.error('Warmup, iterations and rounds must be positive')

suffix = '.exe' if os.name == 'nt' else ''
java = args.java_home / ('bin/java' + suffix) if args.java_home else shutil.which('java')
javac = args.java_home / ('bin/javac' + suffix) if args.java_home else shutil.which('javac')
if not java or not javac:
    parser.error('Java 21 JDK is required; pass --java-home or JAVA_HOME')
fixed = [args.baseline_jar, args.minecraft_jar, *args.runtime_jar]
if any(not path.is_file() for path in fixed):
    parser.error('A supplied baseline/runtime JAR does not exist')
extra = sorted(path for path in args.dependencies_dir.rglob('*.jar')
               if not path.name.endswith(('-sources.jar', '-javadoc.jar')))
print('baseline_jar=' + str(args.baseline_jar.resolve()), flush=True)
print('baseline_sha256=' + hashlib.sha256(args.baseline_jar.read_bytes()).hexdigest(), flush=True)
print('runtime_jars=' + str(len(fixed) + len(extra)), flush=True)
print('Only raw JSON/help fixtures are measured; no server TPS or wire compression.', flush=True)

with tempfile.TemporaryDirectory(prefix='tnk-snapshot-fixture-') as tmp:
    output = Path(tmp)
    source = Path(__file__).with_name('Perf.java').resolve()
    subprocess.run([str(javac), '--release', '21', '-d', tmp, str(source)], check=True)
    # Baseline implementation takes precedence. Remaining jars supply lazy-linked
    # Minecraft types; Gson source is printed to identify its actual resolved jar.
    paths = list(dict.fromkeys(str(p.resolve()) for p in fixed + [output] + extra))
    classpath = os.pathsep.join(paths)
    quote = lambda value: '"' + str(value).replace('\\', '\\\\').replace('"', '\\"') + '"'
    arguments = ['-Xms256m', '-Xmx512m', '-cp', classpath, 'Perf',
                 args.warmup, args.iterations, args.rounds]
    argfile = output / 'java.args'
    argfile.write_text('\n'.join(quote(arg) for arg in arguments) + '\n')
    subprocess.run([str(java), '@' + str(argfile)], check=True)
