Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force -Path out | Out-Null
javac -encoding UTF-8 -d out `
  src/com/riftbound/events/*.java `
  src/com/riftbound/events/json/*.java `
  src/com/riftbound/events/uvs/*.java `
  src/com/riftbound/events/playriftbound/*.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java -cp out com.riftbound.events.ListEvents @args
