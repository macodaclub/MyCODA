$backendDir = $PSScriptRoot
$projectDir = Split-Path $backendDir -Parent

$env:MYCODA_ENV = "DEV"

$env:MYCODA_ONTOLOGY_FILE_PATH = Join-Path $projectDir "ontologies\MaCODA.owl"

$env:MYCODA_ONTOLOGY_IRI_PREFIX = "https://mycoda.ddns.net/ontologies/MYCODA#"

Set-Location $backendDir

.\gradlew run