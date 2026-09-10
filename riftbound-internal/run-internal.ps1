Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$lib = Join-Path $PSScriptRoot "..\riftbound-events\riftbound-events\src"
if (-not (Test-Path (Join-Path $lib "com\riftbound\events"))) {
  throw "Expected library sources at $lib"
}
New-Item -ItemType Directory -Force -Path out | Out-Null
& javac -encoding UTF-8 -d out `
  "$lib\com\riftbound\events\*.java" `
  "$lib\com\riftbound\events\json\*.java" `
  "$lib\com\riftbound\events\uvs\*.java" `
  "$lib\com\riftbound\events\playriftbound\*.java" `
  "src\com\riftbound\internal\*.java"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp out com.riftbound.internal.InternalMain @args
