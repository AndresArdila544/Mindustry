# Script to compile Java files and run CKJM for metrics calculation
# CKJM requires compiled .class files

$CKJM_JAR = "C:\Users\andre\OneDrive\Documentos\CONCORDIA\4 SEMESTER\SOEN 6491\Deliverable 4\ckjm-1.9\build\ckjm-1.9.jar"
$PROJECT_ROOT = "C:\Users\andre\OneDrive\Documentos\CONCORDIA\4 SEMESTER\SOEN 6491\Deliverable 4\Mindustry"

Write-Host "CKJM Metrics Calculator"
Write-Host "======================"
Write-Host ""

# Function to compile and run CKJM for a specific commit
function Run-CKJM-ForCommit {
    param(
        [string]$CommitHash,
        [string]$OutputFile,
        [string]$Description
    )
    
    Write-Host "Processing: $Description"
    Write-Host "Commit: $CommitHash"
    Write-Host ""
    
    # Create temporary directory for this commit
    $TempDir = "$PROJECT_ROOT\metrics_analysis\temp_$($CommitHash.Substring(0,7))"
    if (Test-Path $TempDir) {
        Remove-Item $TempDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $TempDir | Out-Null
    
    try {
        # Checkout the commit
        Push-Location $PROJECT_ROOT
        git checkout $CommitHash --quiet
        
        # Compile the project (or at least the classes we need)
        Write-Host "Compiling..."
        .\gradlew.bat compileJava --quiet 2>&1 | Out-Null
        
        # Find compiled classes
        $ClassFiles = Get-ChildItem -Path "core\build\classes\java\main\mindustry" -Recurse -Filter "*.class" | 
            Where-Object { 
                $_.FullName -match "\\Control\.class$" -or
                $_.FullName -match "\\GameState\.class$" -or
                $_.FullName -match "\\GameStateManager\.class$" -or
                $_.FullName -match "\\GameOverDialog\.class$" -or
                $_.FullName -match "\\PausedDialog\.class$" -or
                $_.FullName -match "\\StatusDisplayBuilder\.class$" -or
                $_.FullName -match "\\HudFragment\.class$"
            }
        
        if ($ClassFiles.Count -eq 0) {
            Write-Host "Warning: No class files found. Trying alternative path..."
            $ClassFiles = Get-ChildItem -Path "core\build" -Recurse -Filter "*.class" | 
                Where-Object { 
                    $_.FullName -match "\\Control\.class$" -or
                    $_.FullName -match "\\GameState\.class$" -or
                    $_.FullName -match "\\GameStateManager\.class$" -or
                    $_.FullName -match "\\GameOverDialog\.class$" -or
                    $_.FullName -match "\\PausedDialog\.class$" -or
                    $_.FullName -match "\\StatusDisplayBuilder\.class$" -or
                    $_.FullName -match "\\HudFragment\.class$"
                }
        }
        
        if ($ClassFiles.Count -gt 0) {
            Write-Host "Found $($ClassFiles.Count) class files"
            
            # Copy class files to temp directory
            $ClassPath = $ClassFiles[0].DirectoryName
            $ClassPath = $ClassPath -replace [regex]::Escape($PROJECT_ROOT), ""
            $ClassPath = $ClassPath.TrimStart("\")
            
            # Run CKJM
            Write-Host "Running CKJM..."
            $Output = java -jar $CKJM_JAR -x $ClassPath 2>&1
            
            # Save output
            $Output | Out-File -FilePath $OutputFile -Encoding UTF8
            
            Write-Host "Results saved to: $OutputFile"
            Write-Host ""
            
            # Display results
            Write-Host "CKJM Output:"
            Write-Host "------------"
            $Output | Select-Object -First 20
            Write-Host ""
        } else {
            Write-Host "Error: No class files found to analyze"
        }
    } catch {
        Write-Host "Error: $_"
    } finally {
        Pop-Location
    }
}

# Run CKJM for initial state
$InitialCommit = "7d6a547680737678308252b76af2c8ab2941c96c"
$InitialOutput = "$PROJECT_ROOT\metrics_analysis\ckjm_initial_state.txt"
Run-CKJM-ForCommit -CommitHash $InitialCommit -OutputFile $InitialOutput -Description "Initial State (Before Refactoring)"

# Run CKJM for refactored state
$RefactoredCommit = "d25e64aef1f86cca31249503dcd4944130c42de4"
$RefactoredOutput = "$PROJECT_ROOT\metrics_analysis\ckjm_refactored_before_fix.txt"
Run-CKJM-ForCommit -CommitHash $RefactoredCommit -OutputFile $RefactoredOutput -Description "Refactored Before Fix (After Refactoring)"

Write-Host "Done!"

