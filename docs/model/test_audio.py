"""Compatibility entry point for the current static audio validation."""
import runpy
from pathlib import Path
runpy.run_path(str(Path(__file__).resolve().parents[1]/'audio/verify.py'),run_name='__main__')
