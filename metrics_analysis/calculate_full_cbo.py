#!/usr/bin/env python3
"""
Calculate FULL CBO by analyzing all classes in the codebase, not just our 7 target classes.
This requires checking out each commit and analyzing the entire codebase.
"""

import os
import re
import sys
import subprocess
from pathlib import Path
from collections import defaultdict
from typing import Dict, Set

def discover_all_classes_in_codebase(codebase_root: str) -> Set[str]:
    """Discover all class names in the entire codebase"""
    all_classes = set()
    
    # Search in core/src/mindustry (main source directory)
    source_dir = Path(codebase_root) / "core" / "src" / "mindustry"
    if not source_dir.exists():
        return all_classes
    
    for filepath in source_dir.rglob("*.java"):
        try:
            with open(filepath, 'rb') as f:
                raw_content = f.read()
                if raw_content.startswith(b'\xef\xbb\xbf'):
                    raw_content = raw_content[3:]
                content = raw_content.decode('utf-8', errors='ignore')
            
            # Extract class name
            class_match = re.search(r'public\s+(?:abstract\s+)?(?:final\s+)?class\s+(\w+)', content)
            if not class_match:
                class_match = re.search(r'class\s+(\w+)', content)
            if class_match:
                all_classes.add(class_match.group(1))
        except Exception as e:
            pass
    
    return all_classes

def analyze_class_cbo(filepath: str, all_classes: Set[str]) -> Set[str]:
    """Analyze CBO for a single class file"""
    coupled = set()
    
    try:
        with open(filepath, 'rb') as f:
            raw_content = f.read()
            if raw_content.startswith(b'\xef\xbb\xbf'):
                raw_content = raw_content[3:]
            content = raw_content.decode('utf-8', errors='ignore')
    except:
        return coupled
    
    # Extract current class name
    class_match = re.search(r'public\s+(?:abstract\s+)?(?:final\s+)?class\s+(\w+)', content)
    if not class_match:
        class_match = re.search(r'class\s+(\w+)', content)
    if not class_match:
        return coupled
    
    current_class = class_match.group(1)
    
    # Find references to other classes
    for class_name in all_classes:
        if class_name == current_class:
            continue
        
        # Patterns for class usage
        patterns = [
            rf'\bnew\s+{re.escape(class_name)}\s*\(',
            rf'\b{class_name}\s+[a-zA-Z_]+\s*[=;]',
            rf'\.{class_name}\s*\(',
            rf'<{re.escape(class_name)}\s*>',
            rf'extends\s+{re.escape(class_name)}\b',
            rf'implements\s+.*{re.escape(class_name)}\b',
            rf'\b{class_name}\s*\.',  # ClassName.field or ClassName.method
        ]
        
        for pattern in patterns:
            if re.search(pattern, content):
                coupled.add(class_name)
                break
    
    return coupled

def calculate_full_cbo_for_commit(commit_hash: str, codebase_root: str, target_classes: Set[str]) -> Dict[str, int]:
    """Calculate full CBO for target classes at a specific commit"""
    print(f"\n{'='*70}")
    print(f"Analyzing commit: {commit_hash}")
    print(f"{'='*70}")
    
    # Checkout the commit
    print("Checking out commit...")
    result = subprocess.run(
        ['git', 'checkout', commit_hash, '--quiet'],
        cwd=codebase_root,
        capture_output=True
    )
    if result.returncode != 0:
        print(f"Error checking out commit: {result.stderr.decode()}")
        return {}
    
    # Discover all classes in codebase
    print("Discovering all classes in codebase...")
    all_classes = discover_all_classes_in_codebase(codebase_root)
    print(f"Found {len(all_classes)} classes in codebase")
    
    # Find target class files
    source_dir = Path(codebase_root) / "core" / "src" / "mindustry"
    target_files = {}
    
    for class_name in target_classes:
        # Search for the class file
        for filepath in source_dir.rglob("*.java"):
            try:
                with open(filepath, 'rb') as f:
                    raw_content = f.read()
                    if raw_content.startswith(b'\xef\xbb\xbf'):
                        raw_content = raw_content[3:]
                    content = raw_content.decode('utf-8', errors='ignore')
                
                class_match = re.search(r'public\s+(?:abstract\s+)?(?:final\s+)?class\s+(\w+)', content)
                if not class_match:
                    class_match = re.search(r'class\s+(\w+)', content)
                
                if class_match and class_match.group(1) == class_name:
                    target_files[class_name] = filepath
                    break
            except:
                continue
    
    print(f"Found {len(target_files)} target class files")
    
    # Calculate CBO for each target class
    cbo_results = {}
    for class_name, filepath in target_files.items():
        print(f"  Analyzing {class_name}...")
        coupled = analyze_class_cbo(str(filepath), all_classes)
        cbo_results[class_name] = len(coupled)
        print(f"    CBO = {len(coupled)} (coupled to {len(coupled)} classes)")
    
    return cbo_results

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 calculate_full_cbo.py <codebase_root>")
        print("Example: python3 calculate_full_cbo.py /path/to/mindustry")
        sys.exit(1)
    
    codebase_root = sys.argv[1]
    if not os.path.isdir(codebase_root):
        print(f"Error: {codebase_root} is not a directory")
        sys.exit(1)
    
    # Target classes
    target_classes = {'Control', 'GameState', 'GameStateManager', 'GameOverDialog', 
                     'PausedDialog', 'StatusDisplayBuilder', 'HudFragment'}
    
    # Get current commit to restore later
    result = subprocess.run(
        ['git', 'rev-parse', 'HEAD'],
        cwd=codebase_root,
        capture_output=True,
        text=True
    )
    original_commit = result.stdout.strip()
    print(f"Current commit: {original_commit}")
    
    try:
        # Analyze initial state
        initial_commit = "7d6a547680737678308252b76af2c8ab2941c96c"
        initial_cbo = calculate_full_cbo_for_commit(initial_commit, codebase_root, target_classes)
        
        # Analyze refactored state
        refactored_commit = "d25e64aef1f86cca31249503dcd4944130c42de4"
        refactored_cbo = calculate_full_cbo_for_commit(refactored_commit, codebase_root, target_classes)
        
        # Print results
        print(f"\n{'='*70}")
        print("FULL CBO RESULTS (All Classes in Codebase)")
        print(f"{'='*70}")
        print(f"\n{'Class':<30} {'Initial CBO':<15} {'Refactored CBO':<15} {'Change':<10}")
        print(f"{'-'*70}")
        
        for class_name in sorted(target_classes):
            initial = initial_cbo.get(class_name, 0)
            refactored = refactored_cbo.get(class_name, 0)
            change = refactored - initial
            change_str = f"{change:+d}" if change != 0 else "0"
            print(f"{class_name:<30} {initial:<15} {refactored:<15} {change_str:<10}")
        
        print(f"\n{'='*70}")
        
    finally:
        # Restore original commit
        print(f"\nRestoring original commit: {original_commit}")
        subprocess.run(['git', 'checkout', original_commit, '--quiet'], cwd=codebase_root)

if __name__ == '__main__':
    main()

