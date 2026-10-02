$backendDir = $PSScriptRoot
$projectDir = Split-Path $backendDir -Parent

$env:MYCODA_ENV = "DEV"

$env:MYCODA_ONTOLOGY_FILE_PATH = Join-Path $projectDir "ontologies\MaCODA.owl"

$env:MYCODA_ONTOLOGY_IRI_PREFIX = "https://mycoda.ddns.net/ontologies/MYCODA#"

# Java 17
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Set-Location $backendDir

java -version

.\gradlew --stop
.\gradlew run