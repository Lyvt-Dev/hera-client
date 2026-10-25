"""Release QA only: verify a public updater download without installing it.

Requires PyNaCl (may be installed under ignored .local/release-tools).
"""
import base64
import hashlib
import json
from pathlib import Path
import sys
from urllib.request import urlopen

root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(root / '.local' / 'release-tools'))
from nacl.exceptions import BadSignatureError
from nacl.signing import VerifyKey

channel = 'https://github.com/Lyvt-Dev/hera-client/releases/latest/download/latest.json'
with urlopen(channel, timeout=30) as response:
    manifest = json.loads(response.read(1024 * 1024))
asset = manifest['platforms']['windows-x86_64']
expected = f"https://github.com/Lyvt-Dev/hera-client/releases/download/v{manifest['version']}/Hera.Client_{manifest['version']}_x64-setup.exe"
if asset['url'] != expected:
    raise ValueError('Unexpected release URL')
with urlopen(asset['url'], timeout=60) as response:
    binary = response.read(64 * 1024 * 1024 + 1)
if not binary or len(binary) > 64 * 1024 * 1024:
    raise ValueError('Invalid installer size')
config = json.loads((root / 'apps/launcher/src-tauri/tauri.conf.json').read_text(encoding='utf-8-sig'))
public_lines = base64.b64decode(config['plugins']['updater']['pubkey']).decode().splitlines()
key = base64.b64decode(public_lines[1])
signature_lines = base64.b64decode(asset['signature']).decode().splitlines()
packet = base64.b64decode(signature_lines[1])
if len(key) != 42 or len(packet) != 74 or key[2:10] != packet[2:10]:
    raise ValueError('Invalid signature key or packet')
if packet[:2] not in (b'ED', b'Ed') or not signature_lines[2].startswith('trusted comment: '):
    raise ValueError('Invalid signature format')
verifier = VerifyKey(key[10:])
def verify(data):
    payload = hashlib.blake2b(data).digest() if packet[:2] == b'ED' else data
    verifier.verify(payload, packet[10:])
    trusted = signature_lines[2][17:].encode()
    verifier.verify(packet[10:] + trusted, base64.b64decode(signature_lines[3]))
verify(binary)
try:
    verify(bytes([binary[0] ^ 1]) + binary[1:])
except BadSignatureError:
    pass
else:
    raise ValueError('Tampered installer was accepted')
print(f"Hera {manifest['version']}: {len(binary):,} downloaded bytes; signature verified; modified installer rejected.")
