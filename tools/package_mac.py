"""Create small, independently checked Mac clipboard scripts for a built mod JAR.
Usage: python tools/package_mac.py build/libs/scyer-1.6.1+mc26.2.jar /workspace/scyer-delivery/mac-1.6.1
"""
from pathlib import Path
import base64, gzip, hashlib, math, re, subprocess, sys
jar=Path(sys.argv[1]).resolve();output=Path(sys.argv[2]).resolve();output.mkdir(parents=True,exist_ok=True)
data=jar.read_bytes();sha=hashlib.sha256(data).hexdigest()
encoded=base64.b64encode(gzip.compress(data,mtime=0)).decode()
count=14;size=math.ceil(len(encoded)/count)
folder='.scyer-1-6-1';results=[]
for index in range(1,count+1):
    piece=encoded[(index-1)*size:index*size]
    payload='\n'.join(piece[j:j+160] for j in range(0,len(piece),160))+'\n'
    checksum=hashlib.sha256(payload.encode()).hexdigest()
    script=f'''/bin/bash <<'SCYER_RECOVERY_SCRIPT'
set -euo pipefail
scyer_parts="$HOME/Downloads/{folder}"
mkdir -p "$scyer_parts"
cat > "$scyer_parts/part-{index}.tmp" <<'SCYER_DATA_END'
{payload}SCYER_DATA_END
scyer_part_hash=$(shasum -a 256 "$scyer_parts/part-{index}.tmp" | cut -d " " -f 1)
[ "$scyer_part_hash" = "{checksum}" ] || {{ echo "Part {index} was copied incorrectly. Recopy this part."; exit 1; }}
mv "$scyer_parts/part-{index}.tmp" "$scyer_parts/part-{index}.b64"
printf "Saved Scyer 1.6.1 part {index} of {count}.\\n"
scyer_missing=""
for scyer_part in {' '.join(str(i) for i in range(1,count+1))}; do
  if [ ! -f "$scyer_parts/part-$scyer_part.b64" ]; then scyer_missing="$scyer_missing $scyer_part"; fi
done
if [ -n "$scyer_missing" ]; then
  printf "Still missing parts:%s\\n" "$scyer_missing"
else
  scyer_temp=$(mktemp "$HOME/Downloads/.scyer-verified.XXXXXX")
  trap 'rm -f "$scyer_temp"' EXIT
  cat {' '.join(chr(34)+'$scyer_parts/part-'+str(i)+'.b64'+chr(34) for i in range(1,count+1))} | base64 -D | gzip -dc > "$scyer_temp"
  scyer_actual=$(shasum -a 256 "$scyer_temp" | cut -d " " -f 1)
  [ "$scyer_actual" = "{sha}" ] || {{ echo "Integrity check failed. Recopy all 1.6.1 parts."; exit 1; }}
  scyer_output="$HOME/Downloads/{jar.name}"
  mv "$scyer_temp" "$scyer_output"
  printf "SUCCESS: Saved and verified %s\\n" "$scyer_output"
fi
SCYER_RECOVERY_SCRIPT
'''
    target=output/f'Part-{index}-of-{count}.txt';target.write_text(script)
    subprocess.run(['bash','-n',str(target)],check=True)
    extracted=re.search("<<'SCYER_DATA_END'\\n(.*?)SCYER_DATA_END",script,re.S)[1]
    assert hashlib.sha256(extracted.encode()).hexdigest()==checksum
    results.append(extracted)
assert gzip.decompress(base64.b64decode(''.join(results)))==data
(output/'SHA256.txt').write_text(sha+'  '+jar.name+'\n')
(output/'Instructions.md').write_text('Scyer 1.6.1 / Minecraft Java 26.2 / Fabric\n\n'
    'For each of the fourteen numbered text files: copy ALL text, then manually type pbpaste | /bin/bash in Mac Terminal. '
    'The last missing part assembles and verifies the JAR automatically. Parts from older versions cannot be mixed with these. '
    'If it prints missing part numbers, run only those parts. After SUCCESS, close Minecraft and replace the old Scyer JAR '
    'in the instance mods folder with scyer-1.6.1+mc26.2.jar from Downloads. Keep Fabric API installed.\n')
print(f'{jar.name}: {len(data)} bytes; SHA256 {sha}')
print(f'{count} scripts: bash syntax, part hashes, gzip reassembly verified; max script {max(p.stat().st_size for p in output.glob("Part-*.txt"))} bytes')
