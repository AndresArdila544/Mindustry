# Script to checkout commits, compile, and run CKJM
# This will analyze both initial and refactored states

$CKJM_JAR = "C:\Users\andre\OneDrive\Documentos\CONCORDIA\4 SEMESTER\SOEN 6491\Deliverable 4\ckjm-1.9\build\ckjm-1.9.jar"
$PROJECT_ROOT = "C:\Users\andre\OneDrive\Documentos\CONCORDIA\4 SEMESTER\SOEN 6491\Deliverable 4\Mindustry"

# Save current commit
Push-Location $PROJECT_ROOT
$OriginalCommit = (git rev-parse HEAD).Trim()
Write-Host "Saving current commit: $OriginalCommit"
Write-Host ""

function Run-CKJM-ForCommit {
    param(
        [string]$CommitHash,
        [string]$OutputFile,
        [string]$Description
    )
    
    Write-Host "========================================"
    Write-Host "Processing: $Description"
    Write-Host "Commit: $CommitHash"
    Write-Host "========================================"
    Write-Host ""
    
    # Checkout the commit
    Write-Host "Checking out commit..."
    git checkout $CommitHash --quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: Failed to checkout commit $CommitHash"
        return
    }
    
    # Clean and compile
    Write-Host "Cleaning previous build..."
    .\gradlew.bat clean --quiet 2>&1 | Out-Null
    
    Write-Host "Compiling..."
    $CompileOutput = .\gradlew.bat compileJava 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Warning: Compilation had errors. Continuing anyway..."
        $CompileOutput | Select-Object -Last 10
    }
    
    # Find compiled classes
    Write-Host "Searching for compiled classes..."
    $ClassPaths = @(
        "core\build\classes\java\main",
        "build\classes\java\main"
    )
    
    $ClassPathRoot = $null
    foreach ($Path in $ClassPaths) {
        $FullPath = Join-Path $PROJECT_ROOT $Path
        if (Test-Path $FullPath) {
            $Classes = Get-ChildItem -Path $FullPath -Recurse -Filter "*.class" -ErrorAction SilentlyContinue
            if ($Classes.Count -gt 0) {
                $ClassPathRoot = $FullPath
                Write-Host "Found classes in: $Path ($($Classes.Count) total .class files)"
                break
            }
        }
    }
    
    if (-not $ClassPathRoot) {
        Write-Host "Error: No compiled classes found"
        return
    }
    
    # Find our specific classes
    $TargetClasses = Get-ChildItem -Path $ClassPathRoot -Recurse -Filter "*.class" | 
        Where-Object { 
            $_.Name -eq "Control.class" -or
            $_.Name -eq "GameState.class" -or
            $_.Name -eq "GameStateManager.class" -or
            $_.Name -eq "GameOverDialog.class" -or
            $_.Name -eq "PausedDialog.class" -or
            $_.Name -eq "StatusDisplayBuilder.class" -or
            $_.Name -eq "HudFragment.class"
        }
    
    Write-Host "Found $($TargetClasses.Count) target class files"
    
    if ($TargetClasses.Count -eq 0) {
        Write-Host "Warning: No target classes found. Running CKJM on all classes..."
    }
    
    # Run CKJM - pass individual class files
    Write-Host "Running CKJM..."
    
    # Get full paths to our target class files
    $ClassFiles = $TargetClasses | ForEach-Object { $_.FullName }
    
    if ($ClassFiles.Count -eq 0) {
        Write-Host "No target classes found, using all classes in package..."
        $ClassFiles = Get-ChildItem -Path $ClassPathRoot -Recurse -Filter "*.class" | 
            Where-Object { $_.FullName -match "\\mindustry\\(core|ui\\(dialogs|fragments))\\" } | 
            ForEach-Object { $_.FullName }
    }
    
    Write-Host "Analyzing $($ClassFiles.Count) class files..."
    Write-Host ""
    
    # CKJM expects class files as arguments
    $Output = java -jar $CKJM_JAR $ClassFiles 2>&1
    
    # Filter to our classes
    $FilteredOutput = $Output | Where-Object {
        $_ -match "mindustry\.(core|ui\.(dialogs|fragments))\.(Control|GameState|GameStateManager|GameOverDialog|PausedDialog|StatusDisplayBuilder|HudFragment)" -or
        $_ -match "^Class\s+" -or
        $_ -match "^WMC\s+" -or
        $_ -match "^DIT\s+" -or
        $_ -match "^NOC\s+" -or
        $_ -match "^CBO\s+" -or
        $_ -match "^RFC\s+" -or
        $_ -match "^LCOM\s+"
    }
    
    # Save full output
    $Output | Out-File -FilePath $OutputFile -Encoding UTF8
    
    Write-Host "Results saved to: $OutputFile"
    Write-Host ""
    
    # Display relevant results
    Write-Host "CKJM Results (filtered):"
    Write-Host "------------------------"
    $Output | Select-String -Pattern "mindustry\.(core|ui\.(dialogs|fragments))\.(Control|GameState|GameStateManager|GameOverDialog|PausedDialog|StatusDisplayBuilder|HudFragment)" -Context 0,1
    Write-Host ""
    Write-Host ""
}

# Run for initial state
$InitialCommit = "7d6a547680737678308252b76af2c8ab2941c96c"
$InitialOutput = "$PROJECT_ROOT\metrics_analysis\ckjm_initial_state.txt"
Run-CKJM-ForCommit -CommitHash $InitialCommit -OutputFile $InitialOutput -Description "Initial State (Before Refactoring)"

# Run for refactored state
$RefactoredCommit = "d25e64aef1f86cca31249503dcd4944130c42de4"
$RefactoredOutput = "$PROJECT_ROOT\metrics_analysis\ckjm_refactored_before_fix.txt"
Run-CKJM-ForCommit -CommitHash $RefactoredCommit -OutputFile $RefactoredOutput -Description "Refactored Before Fix (After Refactoring)"

# Return to original commit
Write-Host "Returning to original commit: $OriginalCommit"
git checkout $OriginalCommit --quiet

Pop-Location

Write-Host ""
Write-Host "Done! Results saved to:"
Write-Host "  - Initial: $InitialOutput"
Write-Host "  - Refactored: $RefactoredOutput"

