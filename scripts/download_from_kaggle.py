#!/usr/bin/env python3
"""
Download NASA C-MAPSS dataset from Kaggle
"""

import kagglehub
import shutil
from pathlib import Path

# Download latest version
print("=" * 60)
print("Downloading NASA C-MAPSS dataset from Kaggle...")
print("=" * 60)

path = kagglehub.dataset_download("behrad3d/nasa-cmaps")

print(f"\n✓ Dataset downloaded to: {path}")

# Setup target directory
script_dir = Path(__file__).parent
project_root = script_dir.parent
target_dir = project_root / "data" / "raw" / "NASA_CMAPSS"
target_dir.mkdir(parents=True, exist_ok=True)

# Copy files from Kaggle cache to project directory
source_path = Path(path)
print(f"\nCopying files to project directory: {target_dir}")

for file in source_path.glob("*"):
    if file.is_file():
        dest_file = target_dir / file.name
        shutil.copy2(file, dest_file)
        size_kb = dest_file.stat().st_size / 1024
        print(f"  ✓ {file.name:30} ({size_kb:>8.1f} KB)")

print("\n" + "=" * 60)
print("✅ NASA C-MAPSS dataset ready!")
print("=" * 60)

# List all files in target directory
print("\nDataset files in data/raw/NASA_CMAPSS/:")
for file in sorted(target_dir.glob("*")):
    if file.is_file():
        size_kb = file.stat().st_size / 1024
        print(f"  - {file.name:30} ({size_kb:>8.1f} KB)")

print("\nDataset structure:")
print("  • train_FD001.txt to train_FD004.txt - Training sets")
print("  • test_FD001.txt to test_FD004.txt   - Test sets")
print("  • RUL_FD001.txt to RUL_FD004.txt     - RUL ground truth labels")
print("\nReady for preprocessing and model training!")
