#!/usr/bin/env python3
"""
Full metrics calculation including:
1. Proper method detection (fixing the 0 methods issue)
2. Full CBO calculation (all classes in codebase, not just our 7)
"""

import os
import re
import sys
import javalang
from pathlib import Path
from collections import defaultdict
from typing import Dict, List, Set

class JavaClass:
    def __init__(self, name: str, filepath: str, package: str = ""):
        self.name = name
        self.filepath = filepath
        self.package = package
        self.methods: List[Dict] = []
        self.fields: List[str] = []
        self.imports: Set[str] = set()
        self.used_classes: Set[str] = set()  # All classes used in this class
        self.loc = 0
        
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

def calculate_cyclomatic_complexity_from_ast(method_body) -> int:
    """Calculate cyclomatic complexity from AST"""
    complexity = 1  # Base
    
    try:
        for path, node in javalang.tree.walk_tree(method_body):
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
    except:
        pass
    
    return complexity

def extract_methods_regex(content: str) -> List[Dict]:
    """Extract methods using regex as fallback"""
    methods = []
    
    # Pattern for method declarations
    # Matches: [modifiers] return_type method_name(params) [throws] {
    method_pattern = r'(?:public|private|protected)?\s*(?:static\s+)?(?:abstract\s+)?(?:final\s+)?(?:synchronized\s+)?(?:\w+(?:<[^>]+>)?\s+)+(\w+)\s*\([^)]*\)\s*(?:throws\s+[^{]+)?\s*\{'
    
    for match in re.finditer(method_pattern, content):
        method_name = match.group(1)
        start_pos = match.end() - 1  # Start from opening brace
        
        # Find matching closing brace
        brace_count = 1
        end_pos = start_pos + 1
        while end_pos < len(content) and brace_count > 0:
            if content[end_pos] == '{':
                brace_count += 1
            elif content[end_pos] == '}':
                brace_count -= 1
            end_pos += 1
        
        method_body = content[start_pos:end_pos]
        
        # Calculate complexity from method body
        complexity = 1  # Base
        complexity += len(re.findall(r'\bif\s*\(', method_body))
        complexity += len(re.findall(r'\belse\s+if\s*\(', method_body))
        complexity += len(re.findall(r'\bwhile\s*\(', method_body))
        complexity += len(re.findall(r'\bfor\s*\(', method_body))
        complexity += len(re.findall(r'\bswitch\s*\(', method_body))
        complexity += len(re.findall(r'\bcatch\s*\(', method_body))
        complexity += len(re.findall(r'\?\s*[^:]*\s*:', method_body))
        complexity += len(re.findall(r'\bcase\s+', method_body))
        
        # Calculate LOC
        method_lines = method_body.split('\n')
        method_loc = len([l for l in method_lines if l.strip() and not l.strip().startswith('//')])
        
        methods.append({
            'name': method_name,
            'complexity': complexity,
            'loc': method_loc
        })
    
    return methods

def parse_java_file(filepath: str, all_classes_in_codebase: Set[str] = None) -> JavaClass:
    """Parse a Java file using javalang with regex fallback"""
    if all_classes_in_codebase is None:
        all_classes_in_codebase = set()
    
    with open(filepath, 'rb') as f:
        raw_content = f.read()
        # Remove BOM if present
        if raw_content.startswith(b'\xef\xbb\xbf'):
            raw_content = raw_content[3:]
        content = raw_content.decode('utf-8', errors='ignore')
        lines = content.split('\n')
    
    # Extract class name
    class_match = re.search(r'public\s+(?:abstract\s+)?(?:final\s+)?class\s+(\w+)', content)
    if not class_match:
        class_match = re.search(r'class\s+(\w+)', content)
    class_name = class_match.group(1) if class_match else Path(filepath).stem
    
    # Extract package
    package_match = re.search(r'package\s+([\w.]+);', content)
    package = package_match.group(1) if package_match else ""
    
    java_class = JavaClass(class_name, filepath, package)
    java_class.loc = len([l for l in lines if l.strip() and not l.strip().startswith('//') and not l.strip().startswith('*')])
    
    # Try parsing with javalang first
    methods_detected = False
    try:
        tree = javalang.parse.parse(content)
        
        # Extract imports
        for imp in tree.imports:
            import_path = '.'.join(imp.path)
            java_class.imports.add(import_path)
            class_name_from_import = import_path.split('.')[-1].replace('*', '').strip()
            if class_name_from_import and class_name_from_import[0].isupper():
                java_class.used_classes.add(class_name_from_import)
                if class_name_from_import in all_classes_in_codebase:
                    java_class.used_classes.add(class_name_from_import)
        
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
                    method_complexity = 1
                    
                    if method.body:
                        method_complexity = calculate_cyclomatic_complexity_from_ast(method.body)
                    
                    # Calculate method LOC
                    if method.body:
                        method_lines = str(method.body).split('\n')
                        method_loc = len([l for l in method_lines if l.strip()])
                    else:
                        method_loc = 1
                    
                    java_class.add_method(method_name, method_complexity, method_loc)
                    methods_detected = True
    except Exception as e:
        # If javalang fails, use regex fallback
        pass
    
    # If no methods detected, use regex fallback
    if not methods_detected:
        methods = extract_methods_regex(content)
        for method in methods:
            java_class.add_method(method['name'], method['complexity'], method['loc'])
    
    # Extract imports using regex (in case javalang failed)
    if not java_class.imports:
        import_matches = re.findall(r'import\s+(?:static\s+)?([\w.*]+);', content)
        for imp in import_matches:
            java_class.imports.add(imp)
            class_name_from_import = imp.split('.')[-1].replace('*', '').strip()
            if class_name_from_import and class_name_from_import[0].isupper():
                java_class.used_classes.add(class_name_from_import)
                if class_name_from_import in all_classes_in_codebase:
                    java_class.used_classes.add(class_name_from_import)
    
    # Find all class references in code (for CBO)
    # Look for: new ClassName(), ClassName.method(), ClassName.field, etc.
    for class_name_ref in all_classes_in_codebase:
        if class_name_ref == java_class.name:
            continue
        patterns = [
            rf'\bnew\s+{re.escape(class_name_ref)}\s*\(',
            rf'\b{class_name_ref}\s+[a-zA-Z_]+\s*[=;]',
            rf'\.{class_name_ref}\s*\(',
            rf'<{re.escape(class_name_ref)}\s*>',
            rf'extends\s+{re.escape(class_name_ref)}\b',
            rf'implements\s+.*{re.escape(class_name_ref)}\b',
        ]
        for pattern in patterns:
            if re.search(pattern, content):
                java_class.used_classes.add(class_name_ref)
                break
    
    return java_class

def discover_all_classes(directory: str) -> Set[str]:
    """Discover all class names in a directory"""
    all_classes = set()
    for filepath in Path(directory).glob('*.java'):
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
    
    return all_classes

def analyze_directory(directory: str, target_classes: Set[str] = None) -> Dict[str, JavaClass]:
    """Analyze all Java files in a directory"""
    if target_classes is None:
        target_classes = set()
    
    # First, discover all classes in the directory
    print(f"Discovering all classes in {directory}...")
    all_classes = discover_all_classes(directory)
    print(f"Found {len(all_classes)} classes")
    
    # Analyze target classes (or all if target_classes is empty)
    classes_to_analyze = target_classes if target_classes else all_classes
    
    classes = {}
    for filepath in Path(directory).glob('*.java'):
        try:
            java_class = parse_java_file(str(filepath), all_classes)
            if java_class.name in classes_to_analyze:
                classes[java_class.name] = java_class
        except Exception as e:
            print(f"Error parsing {filepath}: {e}", file=sys.stderr)
    
    return classes

def print_metrics(classes: Dict[str, JavaClass], state_name: str, target_classes: Set[str] = None):
    """Print metrics for all classes"""
    if target_classes:
        classes = {k: v for k, v in classes.items() if k in target_classes}
    
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
        print("Usage: python3 calculate_metrics_full.py <directory1> [directory2] ...")
        sys.exit(1)
    
    # Target classes for detailed analysis
    target_classes = {'Control', 'GameState', 'GameStateManager', 'GameOverDialog', 
                     'PausedDialog', 'StatusDisplayBuilder', 'HudFragment'}
    
    for directory in sys.argv[1:]:
        if not os.path.isdir(directory):
            print(f"Error: {directory} is not a directory", file=sys.stderr)
            continue
        
        state_name = Path(directory).name
        classes = analyze_directory(directory, target_classes)
        print_metrics(classes, state_name, target_classes)

if __name__ == '__main__':
    main()

