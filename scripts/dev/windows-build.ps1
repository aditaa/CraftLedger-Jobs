$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")

if (-not $env:JAVA_HOME) {
    $tempJdk = Join-Path $env:TEMP "craftledger-jdk17\jdk-17.0.19+10"
    if (Test-Path (Join-Path $tempJdk "bin\java.exe")) {
        $env:JAVA_HOME = $tempJdk
    }
}

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME is not set. Install a Java 17 JDK, or use WSL with scripts/dev/wsl-build.sh."
}

$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location $repoRoot
.\gradlew.bat build --console=plain --no-daemon
