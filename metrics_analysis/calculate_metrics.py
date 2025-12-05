#!/usr/bin/env python3
"""
Calculate CK Metrics (CBO, LCOM, WMC, Cyclomatic Complexity, LOC) from Java source files.
This script analyzes Java source code directly without requiring compilation.
"""

import os
import re
import sys
from pathlib import Path
from collections import defaultdict
from typing import Dict, List, Set, Tuple

class JavaClass:
    def __init__(self, name: str, filepath: str):
        self.name = name
        self.filepath = filepath
        self.methods: List[Dict] = []
        self.fields: List[str] = []
        self.imports: Set[str] = set()
        self.used_classes: Set[str] = set()
        self.loc = 0
        self.package = ""
        
    def add_method(self, method_name: str, complexity: int, loc: int):
        self.methods.append({
            'name': method_name,
            'complexity': complexity,
            'loc': loc
        })
    
    def calculate_wmc(self) -> int:
        """Weighted Methods per Class - sum of method complexities"""
        return sum(m['complexity'] for m in self.methods)
    
    def calculate_cbo(self, all_classes: Dict[str, 'JavaClass']) -> int:
        """Coupling Between Objects - classes this class is coupled to"""
        coupled = set()
        # Check imports
        for imp in self.imports:
            class_name = imp.split('.')[-1]
            if class_name in all_classes:
                coupled.add(class_name)
        # Check used classes in code
        for used in self.used_classes:
            if used in all_classes:
                coupled.add(used)
        return len(coupled)
    
    def calculate_lcom(self) -> float:
        """Lack of Cohesion of Methods"""
        if len(self.methods) <= 1 or len(self.fields) == 0:
            return 0.0
        
        # Simplified LCOM: count methods that don't share fields
        # This is a simplified version - full LCOM is more complex
        method_field_usage = defaultdict(set)
        for i, method in enumerate(self.methods):
            # In a real implementation, we'd parse which fields each method uses
            # For now, we'll use a heuristic based on method complexity
            method_field_usage[i] = set()  # Placeholder
        
        # Simplified calculation
        return len(self.methods) / max(len(self.fields), 1)
    
    def calculate_avg_complexity(self) -> float:
        """Average cyclomatic complexity per method"""
        if not self.methods:
            return 0.0
        return sum(m['complexity'] for m in self.methods) / len(self.methods)
    
    def calculate_max_complexity(self) -> int:
        """Maximum cyclomatic complexity in any method"""
        if not self.methods:
            return 0
        return max(m['complexity'] for m in self.methods)

def calculate_cyclomatic_complexity(code: str) -> int:
    """Calculate cyclomatic complexity of a method"""
    # Count decision points
    complexity = 1  # Base complexity
    complexity += len(re.findall(r'\bif\s*\(', code))
    complexity += len(re.findall(r'\belse\s+if\s*\(', code))
    complexity += len(re.findall(r'\bwhile\s*\(', code))
    complexity += len(re.findall(r'\bfor\s*\(', code))
    complexity += len(re.findall(r'\bswitch\s*\(', code))
    complexity += len(re.findall(r'\bcatch\s*\(', code))
    complexity += len(re.findall(r'\?\s*.*\s*:', code))  # Ternary operators
    complexity += len(re.findall(r'\bcase\s+', code))
    return complexity

def parse_java_file(filepath: str) -> JavaClass:
    """Parse a Java file and extract class information"""
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
        lines = content.split('\n')
    
    # Extract class name
    class_match = re.search(r'public\s+(?:abstract\s+)?(?:final\s+)?class\s+(\w+)', content)
    if not class_match:
        class_match = re.search(r'class\s+(\w+)', content)
    class_name = class_match.group(1) if class_match else Path(filepath).stem
    
    java_class = JavaClass(class_name, filepath)
    java_class.loc = len([l for l in lines if l.strip() and not l.strip().startswith('//')])
    
    # Extract package
    package_match = re.search(r'package\s+([\w.]+);', content)
    if package_match:
        java_class.package = package_match.group(1)
    
    # Extract imports
    import_matches = re.findall(r'import\s+(?:static\s+)?([\w.*]+);', content)
    for imp in import_matches:
        java_class.imports.add(imp)
        # Extract class name from import
        class_name_from_import = imp.split('.')[-1].split('*')[0]
        if class_name_from_import:
            java_class.used_classes.add(class_name_from_import)
    
    # Extract fields (simplified)
    field_matches = re.findall(r'(?:public|private|protected)?\s*(?:static\s+)?(?:final\s+)?\w+\s+(\w+)\s*[=;]', content)
    java_class.fields = list(set(field_matches))
    
    # Extract methods and calculate complexity
    # Match method declarations
    method_pattern = r'(?:public|private|protected)?\s*(?:static\s+)?(?:abstract\s+)?(?:final\s+)?(?:\w+\s+)*(\w+)\s*\([^)]*\)\s*\{'
    method_matches = list(re.finditer(method_pattern, content))
    
    for i, match in enumerate(method_matches):
        method_name = match.group(1)
        start_pos = match.end()
        
        # Find method end (simplified - find matching brace)
        brace_count = 1
        end_pos = start_pos
        while end_pos < len(content) and brace_count > 0:
            if content[end_pos] == '{':
                brace_count += 1
            elif content[end_pos] == '}':
                brace_count -= 1
            end_pos += 1
        
        method_code = content[start_pos:end_pos-1]
        method_lines = method_code.split('\n')
        method_loc = len([l for l in method_lines if l.strip() and not l.strip().startswith('//')])
        complexity = calculate_cyclomatic_complexity(method_code)
        
        java_class.add_method(method_name, complexity, method_loc)
    
    return java_class

def analyze_directory(directory: str) -> Dict[str, JavaClass]:
    """Analyze all Java files in a directory"""
    classes = {}
    for filepath in Path(directory).glob('*.java'):
        try:
            java_class = parse_java_file(str(filepath))
            classes[java_class.name] = java_class
        except Exception as e:
            print(f"Error parsing {filepath}: {e}", file=sys.stderr)
    return classes

def print_metrics(classes: Dict[str, JavaClass], state_name: str):
    """Print metrics for all classes"""
    print(f"\n{'='*80}")
    print(f"Metrics for: {state_name}")
    print(f"{'='*80}")
    print(f"{'Class':<30} {'LOC':<8} {'WMC':<8} {'CBO':<8} {'LCOM':<10} {'Avg CC':<10} {'Max CC':<10}")
    print(f"{'-'*80}")
    
    total_loc = 0
    total_wmc = 0
    total_cbo = 0
    total_lcom = 0
    total_avg_cc = 0
    total_max_cc = 0
    
    for class_name, java_class in sorted(classes.items()):
        wmc = java_class.calculate_wmc()
        cbo = java_class.calculate_cbo(classes)
        lcom = java_class.calculate_lcom()
        avg_cc = java_class.calculate_avg_complexity()
        max_cc = java_class.calculate_max_complexity()
        
        print(f"{class_name:<30} {java_class.loc:<8} {wmc:<8} {cbo:<8} {lcom:<10.2f} {avg_cc:<10.2f} {max_cc:<10}")
        
        total_loc += java_class.loc
        total_wmc += wmc
        total_cbo += cbo
        total_lcom += lcom
        total_avg_cc += avg_cc
        total_max_cc += max_cc
    
    print(f"{'-'*80}")
    print(f"{'TOTAL/AVG':<30} {total_loc:<8} {total_wmc:<8} {total_cbo:<8} {total_lcom/len(classes):<10.2f} {total_avg_cc/len(classes):<10.2f} {total_max_cc:<10}")
    print(f"{'='*80}\n")

def main():
    if len(sys.argv) < 2:
        print("Usage: python calculate_metrics.py <directory1> [directory2] ...")
        sys.exit(1)
    
    for directory in sys.argv[1:]:
        if not os.path.isdir(directory):
            print(f"Error: {directory} is not a directory", file=sys.stderr)
            continue
        
        state_name = Path(directory).name
        classes = analyze_directory(directory)
        print_metrics(classes, state_name)

if __name__ == '__main__':
    main()

