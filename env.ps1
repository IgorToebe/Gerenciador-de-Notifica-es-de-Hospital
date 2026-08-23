# Carrega o ambiente do projeto (JDK 11 + Maven) sem alterar as variaveis globais do sistema.
# Uso: . .\env.ps1   (o ponto no inicio e obrigatorio, para aplicar no shell atual)

$env:JAVA_HOME = "C:\Program Files\Java\jdk-11.0.32"
$env:PATH = "C:\Users\julio\tools\apache-maven-3.9.16\bin;$env:JAVA_HOME\bin;$env:PATH"

if (Test-Path .env) {
    Get-Content .env | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), "Process")
        }
    }
}

Write-Output "JAVA_HOME = $env:JAVA_HOME"
java -version
mvn -version
