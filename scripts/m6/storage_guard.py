"""Read-only disk guard shared by the sequential M6 import runner."""

from pathlib import Path
import shutil

FLOOR = 4_200_000_000
CASE_RESERVE = 512_000_000
DIRTY_RESERVE = 32_000_000


def require_free_space(reserve=0):
    drives = {drive: shutil.disk_usage(drive).free for drive in ('C:/', 'D:/')}
    if min(drives.values()) < FLOOR + reserve:
        raise RuntimeError(
            f'Storage guard: require at least {FLOOR} free bytes plus {reserve} reserve on both C: and D:; '
            f'available {drives}. No next case may start.')
    return drives


def directory_bytes(directory):
    return sum(path.stat().st_size for path in Path(directory).rglob('*') if path.is_file())
