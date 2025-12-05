# Simple script to run CKJM on compiled classes
# Assumes we're already at the correct commit and have compiled

$CKJM_JAR = "C:\Users\andre\OneDrive\Documentos\CONCORDIA\4 SEMESTER\SOEN 6491\Deliverable 4\ckjm-1.9\build\ckjm-1.9.jar"
$PROJECT_ROOT = "C:\Users\andre\OneDrive\Documentos\CONCORDIA\4 SEMESTER\SOEN 6491\Deliverable 4\Mindustry"

Write-Host "CKJM Metrics Calculator"
Write-Host "======================"
Write-Host ""

# Get current commit
Push-Location $PROJECT_ROOT
$CurrentCommit = (git rev-parse HEAD).Trim()
Write-Host "Current commit: $CurrentCommit"
Write-Host ""

# Function to find and run CKJM on compiled classes
function Run-CKJM {
    param([string]$OutputFile, [string]$Description)
    
    Write-Host "Processing: $Description"
    
    # Try to compile first
    Write-Host "Compiling..."
    .\gradlew.bat compileJava --quiet 2>&1 | Out-Null
    
    # Find compiled classes - try multiple possible paths
    $ClassPaths = @(
        "core\build\classes\java\main",
        "core\build\classes\java\main\mindustry",
        "build\classes\java\main",
        "build\classes\java\main\mindustry"
    )
    
    $FoundClasses = $null
    foreach ($Path in $ClassPaths) {
        $FullPath = Join-Path $PROJECT_ROOT $Path
        if (Test-Path $FullPath) {
            $Classes = Get-ChildItem -Path $FullPath -Recurse -Filter "*.class" | 
                Where-Object { 
                    $_.Name -eq "Control.class" -or
                    $_.Name -eq "GameState.class" -or
                    $_.Name -eq "GameStateManager.class" -or
                    $_.Name -eq "GameOverDialog.class" -or
                    $_.Name -eq "PausedDialog.class" -or
                    $_.Name -eq "StatusDisplayBuilder.class" -or
                    $_.Name -eq "HudFragment.class"
                }
            
            if ($Classes.Count -gt 0) {
                $FoundClasses = $Classes
                $ClassPath = $FullPath
                Write-Host "Found $($Classes.Count) class files in: $Path"
                break
            }
        }
    }
    
    if ($FoundClasses) {
        # Run CKJM - it needs the classpath root
        Write-Host "Running CKJM..."
        
        # CKJM format: java -jar ckjm.jar [options] <classpath>
        # The classpath should be the root of the package structure
        $ClassPathRoot = $ClassPath -replace "\\mindustry.*$", ""
        
        # Get all .class files in the mindustry package
        $AllClasses = Get-ChildItem -Path $ClassPathRoot -Recurse -Filter "*.class"
        
        # Create a temporary file with class names (fully qualified)
        $TempClassList = "$env:TEMP\ckjm_classes_$(Get-Date -Format 'yyyyMMddHHmmss').txt"
        $AllClasses | ForEach-Object {
            $RelativePath = $_.FullName.Replace($ClassPathRoot, "").Replace("\", ".").Replace(".class", "")
            $RelativePath.TrimStart(".")
        } | Where-Object { $_ -match "^(core|mindustry)\.(core|ui\.(dialogs|fragments))\.(Control|GameState|GameStateManager|GameOverDialog|PausedDialog|StatusDisplayBuilder|HudFragment)$" } | 
        Out-File -FilePath $TempClassList -Encoding UTF8
        
        # Run CKJM
        $Output = java -jar $CKJM_JAR $ClassPathRoot 2>&1
        
        # Filter output to only our classes
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
        
        # Save output
        $Output | Out-File -FilePath $OutputFile -Encoding UTF8
        
        Write-Host "Results saved to: $OutputFile"
        Write-Host ""
        
        # Display filtered results
        Write-Host "CKJM Output (filtered):"
        Write-Host "------------------------"
        $Output | Select-String -Pattern "mindustry\.(core|ui\.(dialogs|fragments))\.(Control|GameState|GameStateManager|GameOverDialog|PausedDialog|StatusDisplayBuilder|HudFragment)" -Context 0,6
        Write-Host ""
        
        Remove-Item $TempClassList -ErrorAction SilentlyContinue
    } else {
        Write-Host "Error: No class files found. Make sure the project is compiled."
    }
}

# Run for current state
$OutputFile = "$PROJECT_ROOT\metrics_analysis\ckjm_current.txt"
Run-CKJM -OutputFile $OutputFile -Description "Current State"

Pop-Location

Write-Host "Done!"

