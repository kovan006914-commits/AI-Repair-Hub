# Requires Windows PowerShell 5.1, JDK 21, MySQL 8 and Node.js.
param(
    [ValidateSet('Setup','Backend','Frontend')][string]$Mode = 'Setup',
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MySqlExe = 'mysql.exe',
    [string]$RuntimeDirectory = ''
)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ([string]::IsNullOrWhiteSpace($RuntimeDirectory)) {
    $RuntimeDirectory = Join-Path (Split-Path $projectRoot -Parent) ((Split-Path $projectRoot -Leaf) + '.local')
}
$runtimeRoot = [IO.Path]::GetFullPath($RuntimeDirectory)
$configPath = Join-Path $runtimeRoot 'local-config.clixml'
$javaExe = 'java.exe'
if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
    $env:JAVA_HOME = $JavaHome
    $javaExe = Join-Path $JavaHome 'bin/java.exe'
}
function Reveal([Security.SecureString]$Value) {
    return [Net.NetworkCredential]::new('', $Value).Password
}
function RandomHex([int]$Bytes) {
    $buffer = New-Object byte[] $Bytes
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($buffer) } finally { $rng.Dispose() }
    return -join ($buffer | ForEach-Object { $_.ToString('x2') })
}
function Invoke-LocalMysql([string]$User, [Security.SecureString]$Password, [string]$Sql) {
    if ($User -notmatch '^[A-Za-z0-9_.-]+$') { throw 'Unsupported database username.' }
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $mysqlExe
    $info.Arguments = "--no-defaults --default-character-set=utf8mb4 --host=localhost --port=3306 --protocol=TCP --user=$User --batch --skip-column-names --connect-timeout=5"
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardInput = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = [Text.Encoding]::UTF8
    $info.StandardErrorEncoding = [Text.Encoding]::UTF8
    $info.EnvironmentVariables['MYSQL_PWD'] = Reveal $Password
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $info
    try {
        [void]$process.Start()
        $outTask = $process.StandardOutput.ReadToEndAsync()
        $errTask = $process.StandardError.ReadToEndAsync()
        $bytes = [Text.Encoding]::UTF8.GetBytes($Sql)
        $process.StandardInput.BaseStream.Write($bytes, 0, $bytes.Length)
        $process.StandardInput.Close()
        if (-not $process.WaitForExit(60000)) { $process.Kill(); throw 'MySQL command timed out.' }
        $stdout = $outTask.GetAwaiter().GetResult()
        $stderr = $errTask.GetAwaiter().GetResult()
        if ($process.ExitCode -ne 0) { throw "MySQL failed: $stderr" }
        return $stdout.Trim()
    } finally { $info.EnvironmentVariables.Remove('MYSQL_PWD'); $process.Dispose() }
}
if ($Mode -eq 'Backend') {
    $config = Import-Clixml -LiteralPath $configPath
    $env:DB_URL = 'jdbc:mysql://localhost:3306/ai_repair_hub?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
    $env:DB_USER = $config.DbUser
    $env:DB_PASSWORD = Reveal $config.DbPassword
    $env:JWT_SECRET = Reveal $config.JwtSecret
    $env:ADMIN_USERNAME = 'admin'
    $env:ADMIN_PASSWORD = Reveal $config.AdminPassword
    Set-Location (Join-Path $projectRoot 'backend')
    & $javaExe -jar target/repair-hub-0.1.0-SNAPSHOT.jar --server.address=127.0.0.1
    exit $LASTEXITCODE
}
if ($Mode -eq 'Frontend') {
    Set-Location (Join-Path $projectRoot 'frontend')
    & npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort
    exit $LASTEXITCODE
}
try {
    $Host.UI.RawUI.WindowTitle = 'AI-Repair-Hub local setup'
    New-Item -ItemType Directory -Force -Path $runtimeRoot | Out-Null
    [void](Get-Command $javaExe -ErrorAction Stop)
    [void](Get-Command $MySqlExe -ErrorAction Stop)
    [void](Get-Command npm.cmd -ErrorAction Stop)
    $jar = Join-Path $projectRoot 'backend/target/repair-hub-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw 'Backend build is not ready. Wait for the build, then run this script again.' }
    foreach ($port in 8080,5173) {
        if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "Port $port is already occupied. The existing process has not been stopped." }
    }
    if (-not (Test-Path -LiteralPath $configPath)) {
        Write-Host '请仅在此窗口输入密码；输入时不会显示字符。' -ForegroundColor Cyan
        $connected = $false
        do {
            $dbAdmin = Read-Host 'MySQL 用户名（通常为 root，直接回车即可）'
            if ([string]::IsNullOrWhiteSpace($dbAdmin)) { $dbAdmin = 'root' }
            $dbAdminPassword = Read-Host 'MySQL 数据库密码' -AsSecureString
            try {
                [void](Invoke-LocalMysql $dbAdmin $dbAdminPassword 'SELECT 1;')
                $connected = $true
            } catch {
                Write-Host '数据库连接失败，请检查用户名和密码。第一项应填写用户名（通常为 root），第二项才是密码。可重新输入，或按 Ctrl+C 退出。' -ForegroundColor Yellow
            }
        } until ($connected)
        do {
            $adminPassword = Read-Host '设置网页 admin 的登录密码（8-72 个字符）' -AsSecureString
            $validPassword = $adminPassword.Length -ge 8 -and $adminPassword.Length -le 72
            if (-not $validPassword) { Write-Host '密码长度须为 8-72 个字符。' -ForegroundColor Yellow }
        } until ($validPassword)
        $dbUser = 'repair_hub_' + (RandomHex 4)
        $dbPasswordText = RandomHex 24
        $schema = [IO.File]::ReadAllText((Join-Path $projectRoot 'sql/schema.sql'), [Text.Encoding]::UTF8)
        $sql = "CREATE DATABASE IF NOT EXISTS ai_repair_hub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`nUSE ai_repair_hub;`n" + $schema
        [void](Invoke-LocalMysql $dbAdmin $dbAdminPassword $sql)
        $userSql = "CREATE USER '$dbUser'@'localhost' IDENTIFIED BY '$dbPasswordText'; GRANT SELECT, INSERT, UPDATE, DELETE ON ai_repair_hub.* TO '$dbUser'@'localhost';"
        [void](Invoke-LocalMysql $dbAdmin $dbAdminPassword $userSql)
        $config = [pscustomobject]@{
            DbUser = $dbUser
            DbPassword = ConvertTo-SecureString $dbPasswordText -AsPlainText -Force
            JwtSecret = ConvertTo-SecureString (RandomHex 32) -AsPlainText -Force
            AdminPassword = $adminPassword
        }
        $tableCount = Invoke-LocalMysql $dbUser $config.DbPassword "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ai_repair_hub';"
        if ([int]$tableCount -lt 7) { throw 'Expected seven database tables after initialization.' }
        $config | Export-Clixml -LiteralPath $configPath
        $dbPasswordText = $null
        $dbAdminPassword = $null
        Write-Host "Database ready ($tableCount tables). Restricted database account: $dbUser" -ForegroundColor Green
        Write-Host 'Secrets are saved with Windows DPAPI outside the Git repository.'
        Write-Host 'An existing admin password is retained by the application.'
    }
    $powershellExe = Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
    $backend = Start-Process -FilePath $powershellExe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`" -Mode Backend -RuntimeDirectory `"$runtimeRoot`"" -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtimeRoot 'backend.stdout.log') -RedirectStandardError (Join-Path $runtimeRoot 'backend.stderr.log') -PassThru
    $frontend = Start-Process -FilePath $powershellExe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`" -Mode Frontend -RuntimeDirectory `"$runtimeRoot`"" -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtimeRoot 'frontend.stdout.log') -RedirectStandardError (Join-Path $runtimeRoot 'frontend.stderr.log') -PassThru
    [pscustomobject]@{ BackendLauncherPid=$backend.Id; FrontendLauncherPid=$frontend.Id; StartedAt=(Get-Date).ToString('o') } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtimeRoot 'processes.json') -Encoding UTF8
    $ready = $false
    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        if ($backend.HasExited -or $frontend.HasExited) { throw "A service exited. See log files in $runtimeRoot" }
        try {
            $frontResponse = Invoke-WebRequest 'http://localhost:5173/' -UseBasicParsing -TimeoutSec 3
            $apiResponse = Invoke-WebRequest 'http://localhost:8080/v3/api-docs' -UseBasicParsing -TimeoutSec 3
            if ($frontResponse.StatusCode -eq 200 -and $apiResponse.StatusCode -eq 200) { $ready=$true; break }
        } catch { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) { throw "Startup timed out. See log files in $runtimeRoot" }
    [pscustomobject]@{ Ready=$true; Frontend='http://localhost:5173'; Swagger='http://localhost:8080/swagger-ui/index.html' } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtimeRoot 'ready.json') -Encoding UTF8
    Write-Host 'READY: http://localhost:5173' -ForegroundColor Green
    Write-Host 'Swagger: http://localhost:8080/swagger-ui/index.html'
    Write-Host 'Web administrator: admin (password entered above, or existing password).'
    Write-Host 'Services continue running after this window closes.'
    [void](Read-Host '服务已启动。按回车关闭配置窗口')
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    [pscustomobject]@{ Ready=$false; Error=$_.Exception.Message } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtimeRoot 'setup-error.json') -Encoding UTF8
    [void](Read-Host '配置失败。按回车关闭窗口')
    exit 1
}
