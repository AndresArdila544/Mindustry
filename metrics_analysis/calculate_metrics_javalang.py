#!/usr/bin/env python3
"""
Calculate CK Metrics using javalang for proper Java parsing.
Metrics: CBO, LCOM, WMC, Cyclomatic Complexity, LOC
"""

import os
import sys
import javalang
from pathlib import Path
from collections import defaultdict
from typing import Dict, List, Set

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
        
        # Check imports for class names from our analyzed set
        for imp in self.imports:
            # Extract potential class names from import
            # e.g., "mindustry.core.Control" -> "Control"
            # e.g., "mindustry.ui.fragments.HudFragment" -> "HudFragment"
            parts = imp.split('.')
            if parts:
                class_name = parts[-1].replace('*', '').strip()
                # Check if this class is in our analyzed set
                if class_name in all_classes:
                    coupled.add(class_name)
                # Also check if any part of the import path matches our classes
                for part in parts:
                    if part in all_classes and part[0].isupper():
                        coupled.add(part)
        
        # Check used classes (from code analysis)
        for used in self.used_classes:
            if used in all_classes:
                coupled.add(used)
        
        # Remove self-coupling
        if self.name in coupled:
            coupled.remove(self.name)
        
        return len(coupled)
    
    def calculate_lcom(self) -> float:
        """Lack of Cohesion of Methods - simplified version"""
        if len(self.methods) <= 1 or len(self.fields) == 0:
            return 0.0
        # Simplified: ratio of methods to fields
        # Real LCOM measures shared field usage, but this gives an approximation
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

def calculate_cyclomatic_complexity(tree) -> int:
    """Calculate cyclomatic complexity from AST"""
    complexity = 1  # Base complexity
    
    def count_complexity(node):
        nonlocal complexity
        if isinstance(node, javalang.tree.IfStatement):
            complexity += 1
        elif isinstance(node, javalang.tree.WhileStatement):
            complexity += 1
        elif isinstance(node, javalang.tree.ForStatement):
            complexity += 1
        elif isinstance(node, javalang.tree.SwitchStatement):
            complexity += 1
        elif isinstance(node, javalang.tree.CatchClause):
            complexity += 1
        elif isinstance(node, javalang.tree.TernaryExpression):
            complexity += 1
        elif isinstance(node, javalang.tree.SwitchStatementCase):
            complexity += 1
    
    # Walk the tree and count decision points
    for path, node in tree:
        count_complexity(node)
    
    return complexity

def parse_java_file(filepath: str) -> JavaClass:
    """Parse a Java file using javalang"""
    with open(filepath, 'rb') as f:
        raw_content = f.read()
        # Remove BOM if present
        if raw_content.startswith(b'\xef\xbb\xbf'):
            raw_content = raw_content[3:]
        content = raw_content.decode('utf-8', errors='ignore')
        lines = content.split('\n')
    
    try:
        tree = javalang.parse.parse(content)
    except javalang.parser.JavaSyntaxError as e:
        print(f"Warning: Syntax error in {filepath}: {e}", file=sys.stderr)
        # Try to continue with basic info
        class_name = Path(filepath).stem
        java_class = JavaClass(class_name, filepath)
        java_class.loc = len([l for l in lines if l.strip() and not l.strip().startswith('//')])
        return java_class
    
    # Extract class name
    class_name = tree.types[0].name if tree.types else Path(filepath).stem
    java_class = JavaClass(class_name, filepath)
    
    # Count LOC (non-empty, non-comment lines)
    java_class.loc = len([l for l in lines if l.strip() and not l.strip().startswith('//') and not l.strip().startswith('*')])
    
    # Extract package
    if tree.package:
        java_class.package = '.'.join(tree.package.name)
    
    # Extract imports and analyze for coupling
    for imp in tree.imports:
        import_path = '.'.join(imp.path)
        java_class.imports.add(import_path)
        # Extract class name from import
        class_name_from_import = import_path.split('.')[-1].replace('*', '').strip()
        if class_name_from_import and class_name_from_import[0].isupper():
            java_class.used_classes.add(class_name_from_import)
        
        # Also check if the import path contains any of our analyzed class names
        # e.g., "mindustry.core.Control" should be detected
        for part in import_path.split('.'):
            if part and part[0].isupper():
                java_class.used_classes.add(part)
    
    # Process class declaration
    if tree.types:
        class_decl = tree.types[0]
        
        # Extract fields
        if hasattr(class_decl, 'fields'):
            for field in class_decl.fields:
                for declarator in field.declarators:
                    java_class.fields.append(declarator.name)
        
        # Extract methods
        if hasattr(class_decl, 'methods'):
            for method in class_decl.methods:
                method_name = method.name
                
                # Calculate complexity for this method
                method_complexity = 1  # Base
                
                # Walk method body using tree walker
                if method.body:
                    try:
                        for path, node in javalang.tree.walk_tree(method.body):
                            if isinstance(node, javalang.tree.IfStatement):
                                method_complexity += 1
                            elif isinstance(node, javalang.tree.WhileStatement):
                                method_complexity += 1
                            elif isinstance(node, javalang.tree.ForStatement):
                                method_complexity += 1
                            elif isinstance(node, javalang.tree.SwitchStatement):
                                method_complexity += 1
                            elif isinstance(node, javalang.tree.CatchClause):
                                method_complexity += 1
                            elif isinstance(node, javalang.tree.TernaryExpression):
                                method_complexity += 1
                            elif isinstance(node, javalang.tree.SwitchStatementCase):
                                method_complexity += 1
                    except:
                        # If walking fails, use simple count
                        pass
                
                # Calculate method LOC (approximate - count statements)
                method_loc = 0
                if method.body:
                    # Count statements in method body
                    if isinstance(method.body, list):
                        method_loc = len(method.body)
                    else:
                        method_loc = 1
                
                java_class.add_method(method_name, method_complexity, method_loc)
    
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
    if num_classes > 0:
        print(f"{'AVERAGE':<30} {'':<8} {total_methods/num_classes:<8.1f} {total_wmc/num_classes:<8.1f} {total_cbo/num_classes:<8.1f} {total_lcom/num_classes:<10.2f} {total_avg_cc/num_classes:<10.2f} {total_max_cc:<10}")
    print(f"{'='*90}\n")

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 calculate_metrics_javalang.py <directory1> [directory2] ...")
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

