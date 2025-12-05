#!/usr/bin/env python3
"""
Manually calculate CBO by analyzing class references in source code
"""

import re
import sys
from pathlib import Path
from collections import defaultdict

def analyze_cbo(directory: str, all_class_names: set) -> dict:
    """Analyze CBO for classes in a directory"""
    cbo_results = defaultdict(set)
    
    for filepath in Path(directory).glob('*.java'):
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        # Extract class name from file
        class_match = re.search(r'public\s+(?:abstract\s+)?(?:final\s+)?class\s+(\w+)', content)
        if not class_match:
            class_match = re.search(r'class\s+(\w+)', content)
        if not class_match:
            continue
        
        current_class = class_match.group(1)
        
        # Find references to other classes in our set
        for class_name in all_class_names:
            if class_name == current_class:
                continue
            
            # Look for references: class_name.field, class_name.method(), new class_name(), etc.
            patterns = [
                rf'\b{re.escape(class_name)}\s*\(',
                rf'\bnew\s+{re.escape(class_name)}\s*\(',
                rf'\b{re.escape(class_name)}\s+[a-zA-Z_]+\s*[=;]',
                rf'\.{re.escape(class_name)}\b',
                rf'<{re.escape(class_name)}\s*>',
                rf'extends\s+{re.escape(class_name)}\b',
                rf'implements\s+.*{re.escape(class_name)}\b',
            ]
            
            for pattern in patterns:
                if re.search(pattern, content):
                    cbo_results[current_class].add(class_name)
                    break
    
    return {k: len(v) for k, v in cbo_results.items()}

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 calculate_cbo_manual.py <directory1> [directory2] ...")
        sys.exit(1)
    
    # Define all classes we're analyzing
    all_classes = {'Control', 'GameState', 'GameStateManager', 'GameOverDialog', 
                   'PausedDialog', 'StatusDisplayBuilder', 'HudFragment'}
    
    for directory in sys.argv[1:]:
        print(f"\n{'='*60}")
        print(f"CBO Analysis for: {Path(directory).name}")
        print(f"{'='*60}")
        
        cbo = analyze_cbo(directory, all_classes)
        
        print(f"{'Class':<30} {'CBO':<10}")
        print(f"{'-'*40}")
        for class_name in sorted(all_classes):
            cbo_value = cbo.get(class_name, 0)
            print(f"{class_name:<30} {cbo_value:<10}")
        print()

if __name__ == '__main__':
    main()

