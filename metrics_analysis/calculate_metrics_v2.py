#!/usr/bin/env python3
"""
Calculate CK Metrics (CBO, LCOM, WMC, Cyclomatic Complexity, LOC) from Java source files.
Improved version with better Java parsing.
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
        # Check imports for class names
        for imp in self.imports:
            # Extract class name from import
            parts = imp.split('.')
            if parts:
                class_name = parts[-1].replace('*', '').strip()
                if class_name and class_name[0].isupper():
                    # Check if it's in our analyzed classes
                    for cls_name in all_classes:
                        if cls_name == class_name or cls_name.endswith('.' + class_name):
                            coupled.add(cls_name)
        # Also check used classes
        for used in self.used_classes:
            if used in all_classes:
                coupled.add(used)
        return len(coupled)
    
    def calculate_lcom(self) -> float:
        """Lack of Cohesion of Methods - simplified version"""
        if len(self.methods) <= 1 or len(self.fields) == 0:
            return 0.0
        # Simplified: ratio of methods to fields
        # Real LCOM is more complex (measures shared field usage)
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
    complexity += len(re.findall(r'\?\s*[^:]*\s*:', code))  # Ternary operators
    complexity += len(re.findall(r'\bcase\s+', code))
    return complexity

def find_method_bodies(content: str) -> List[Tuple[int, int, str]]:
    """Find method bodies by matching braces"""
    methods = []
    i = 0
    while i < len(content):
        # Look for method-like patterns: word( followed eventually by {
        # This is a simplified approach
        method_start = content.find('(', i)
        if method_start == -1:
            break
        
        # Find the opening brace after the method signature
        brace_pos = content.find('{', method_start)
        if brace_pos == -1:
            i = method_start + 1
            continue
        
        # Check if this looks like a method (has return type or void before the name)
        before_paren = content[max(0, method_start-50):method_start]
        # Look for method-like patterns
        if re.search(r'\b(public|private|protected|static|void|\w+)\s+\w+\s*\(', before_paren[-30:]):
            # Find matching closing brace
            brace_count = 1
            end_pos = brace_pos + 1
            while end_pos < len(content) and brace_count > 0:
                if content[end_pos] == '{':
                    brace_count += 1
                elif content[end_pos] == '}':
                    brace_count -= 1
                end_pos += 1
            
            if brace_count == 0:
                method_body = content[brace_pos+1:end_pos-1]
                methods.append((brace_pos+1, end_pos-1, method_body))
                i = end_pos
            else:
                i = method_start + 1
        else:
            i = method_start + 1
    
    return methods

def extract_method_name(content: str, method_start: int) -> str:
    """Extract method name from content before method_start"""
    # Look backwards for the method name
    before = content[max(0, method_start-100):method_start]
    # Find the last word before the opening parenthesis
    match = re.search(r'(\w+)\s*\(', before)
    if match:
        return match.group(1)
    return "unknown"

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
    # Count non-empty, non-comment lines
    java_class.loc = len([l for l in lines if l.strip() and not l.strip().startswith('//') and not l.strip().startswith('*')])
    
    # Extract package
    package_match = re.search(r'package\s+([\w.]+);', content)
    if package_match:
        java_class.package = package_match.group(1)
    
    # Extract imports
    import_matches = re.findall(r'import\s+(?:static\s+)?([\w.*]+);', content)
    for imp in import_matches:
        java_class.imports.add(imp)
        # Extract class name from import
        class_name_from_import = imp.split('.')[-1].replace('*', '').strip()
        if class_name_from_import and class_name_from_import[0].isupper():
            java_class.used_classes.add(class_name_from_import)
    
    # Extract fields - improved pattern
    # Match: [modifiers] type fieldName [= value];
    field_pattern = r'(?:public|private|protected)?\s*(?:static\s+)?(?:final\s+)?(?:transient\s+)?(?:volatile\s+)?\w+(?:<[^>]+>)?\s+(\w+)\s*[=;]'
    field_matches = re.findall(field_pattern, content)
    java_class.fields = list(set(field_matches))
    
    # Find all method bodies
    method_bodies = find_method_bodies(content)
    
    for start, end, method_body in method_bodies:
        # Extract method name (look before start)
        method_name = extract_method_name(content, start - 50)
        
        # Calculate LOC and complexity for this method
        method_lines = method_body.split('\n')
        method_loc = len([l for l in method_lines if l.strip() and not l.strip().startswith('//')])
        complexity = calculate_cyclomatic_complexity(method_body)
        
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
            import traceback
            traceback.print_exc()
    return classes

def print_metrics(classes: Dict[str, JavaClass], state_name: str):
    """Print metrics for all classes"""
    print(f"\n{'='*90}")
    print(f"Metrics for: {state_name}")
    print(f"{'='*90}")
    print(f"{'Class':<30} {'LOC':<8} {'Methods':<8} {'WMC':<8} {'CBO':<8} {'LCOM':<10} {'Avg CC':<10} {'Max CC':<10}")
    print(f"{'-'*90}")
    
    total_loc = 0
    total_methods = 0
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
        num_methods = len(java_class.methods)
        
        print(f"{class_name:<30} {java_class.loc:<8} {num_methods:<8} {wmc:<8} {cbo:<8} {lcom:<10.2f} {avg_cc:<10.2f} {max_cc:<10}")
        
        total_loc += java_class.loc
        total_methods += num_methods
        total_wmc += wmc
        total_cbo += cbo
        total_lcom += lcom
        total_avg_cc += avg_cc
        total_max_cc += max_cc
    
    num_classes = len(classes)
    print(f"{'-'*90}")
    print(f"{'TOTAL':<30} {total_loc:<8} {total_methods:<8} {total_wmc:<8} {total_cbo:<8} {'':<10}")
    print(f"{'AVERAGE':<30} {'':<8} {total_methods/num_classes if num_classes > 0 else 0:<8.1f} {total_wmc/num_classes if num_classes > 0 else 0:<8.1f} {total_cbo/num_classes if num_classes > 0 else 0:<8.1f} {total_lcom/num_classes if num_classes > 0 else 0:<10.2f} {total_avg_cc/num_classes if num_classes > 0 else 0:<10.2f} {total_max_cc:<10}")
    print(f"{'='*90}\n")

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 calculate_metrics_v2.py <directory1> [directory2] ...")
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

