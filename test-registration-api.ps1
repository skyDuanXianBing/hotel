param(
  [Parameter(Mandatory=$false)][string]$BaseUrl = "http://localhost:8080",
  [Parameter(Mandatory=$true)][string]$BearerToken,
  [Parameter(Mandatory=$true)][string]$StoreId,
  [Parameter(Mandatory=$true)][string]$OrderNumber,
  [Parameter(Mandatory=$false)][string]$OutDir = ".\\tmp_registration_test",
  [Parameter(Mandatory=$false)][string]$PassportImagePath = "",
  [Parameter(Mandatory=$false)][switch]$SendMessage,
  [Parameter(Mandatory=$false)][ValidateSet('APPROVED_INFO','REJECT_REQUEST','REMINDER')][string]$MessageType = "APPROVED_INFO",
  [Parameter(Mandatory=$false)][string]$MessageContent = "Your registration has been reviewed. Thank you! {{registration_link}}",
  [Parameter(Mandatory=$false)][string]$SenderName = "客服"
)

$ErrorActionPreference = "Stop"

function Get-AuthHeaders() {
  return @{ 
    Authorization = "Bearer $BearerToken";
    "X-Store-Id" = $StoreId;
    Accept = "application/json";
  }
}

if (!(Test-Path $OutDir)) {
  New-Item -ItemType Directory -Path $OutDir | Out-Null
}

Write-Host "[1] Generate public link token" -ForegroundColor Cyan
$linkResp = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/registrations/link/$OrderNumber" -Headers (Get-AuthHeaders)
if (-not $linkResp.success) { throw "link api failed: $($linkResp.message)" }
$link = [string]$linkResp.data
Write-Host "link = $link"

$token = ""
if ($link -match "[\?&]t=([^&]+)") {
  $token = $Matches[1]
}
if ([string]::IsNullOrWhiteSpace($token)) { throw "cannot parse token from link" }

Write-Host "[2] Public GET" -ForegroundColor Cyan
$pubGet = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/public/registration/$OrderNumber?t=$token"
if (-not $pubGet.success) { throw "public get failed: $($pubGet.message)" }
$form = $pubGet.data
Write-Host ("status={0}, guests={1}, attachments={2}" -f $form.status, ($form.guests | Measure-Object).Count, (($form.attachments | Measure-Object).Count))

Write-Host "[3] Public PUT (saveDraft)" -ForegroundColor Cyan
# Example: set residenceType=OTHER for first guest (only for testing)
if (($form.guests | Measure-Object).Count -gt 0) {
  $form.guests[0].residenceType = "OTHER"
  if (-not $form.guests[0].passportNumber) { $form.guests[0].passportNumber = "P12345678" }
}
$payload = @{ guests = $form.guests } | ConvertTo-Json -Depth 10
$pubPut = Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/public/registration/$OrderNumber?t=$token" -Body $payload -ContentType "application/json"
if (-not $pubPut.success) { throw "public put failed: $($pubPut.message)" }

Write-Host "[4] Optional upload passport image" -ForegroundColor Cyan
if ($PassportImagePath -and (Test-Path $PassportImagePath)) {
  $guestId = $pubPut.data.guests[0].id
  $uploadUrl = "$BaseUrl/api/public/registration/$OrderNumber/attachments/passport?t=$token&guestId=$guestId"

  $boundary = [System.Guid]::NewGuid().ToString()
  $fileBytes = [System.IO.File]::ReadAllBytes((Resolve-Path $PassportImagePath))
  $fileName = [System.IO.Path]::GetFileName($PassportImagePath)

  $bodyLines = @(
    "--$boundary",
    "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"",
    "Content-Type: image/jpeg",
    "",
    $fileBytes,
    "--$boundary--",
    ""
  )

  $ms = New-Object System.IO.MemoryStream
  $writer = New-Object System.IO.BinaryWriter($ms)
  foreach ($line in $bodyLines) {
    if ($line -is [byte[]]) {
      $writer.Write($line)
    } else {
      $writer.Write([System.Text.Encoding]::UTF8.GetBytes($line + "`r`n"))
    }
  }
  $writer.Flush()

  $uploadResp = Invoke-RestMethod -Method Post -Uri $uploadUrl -ContentType "multipart/form-data; boundary=$boundary" -Body $ms.ToArray()
  if (-not $uploadResp.success) { throw "upload failed: $($uploadResp.message)" }
  Write-Host "uploaded attachmentId=$($uploadResp.data.id)"
} else {
  Write-Host "skip upload (PassportImagePath not provided or not found)"
}

Write-Host "[5] Public submit" -ForegroundColor Cyan
$pubSubmit = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/public/registration/$OrderNumber/submit?t=$token"
if (-not $pubSubmit.success) { throw "public submit failed: $($pubSubmit.message)" }
Write-Host "submitted ok, status=$($pubSubmit.data.status)"

Write-Host "[6] Admin download PDF" -ForegroundColor Cyan
$formId = $pubSubmit.data.formId
$pdfPath = Join-Path $OutDir ("registration-$OrderNumber.pdf")
Invoke-WebRequest -Method Get -Uri "$BaseUrl/api/v1/registrations/$formId/pdf" -Headers (Get-AuthHeaders) -OutFile $pdfPath | Out-Null
Write-Host "saved: $pdfPath"

if ($SendMessage) {
  Write-Host "[7] Admin send message" -ForegroundColor Cyan
  $sendPayload = @{
    type = $MessageType
    content = $MessageContent
    senderName = $SenderName
  } | ConvertTo-Json -Depth 10

  $sendResp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/registrations/$formId/messages/send" -Headers (Get-AuthHeaders) -Body $sendPayload -ContentType "application/json"
  if (-not $sendResp.success) { throw "send message failed: $($sendResp.message)" }
  Write-Host ("sendStatus={0} type={1}" -f $sendResp.data.sendStatus, $sendResp.data.type)

  Write-Host "[8] Admin fetch detail (verify messageLogs)" -ForegroundColor Cyan
  $detailResp = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/registrations/$formId" -Headers (Get-AuthHeaders)
  if (-not $detailResp.success) { throw "detail failed: $($detailResp.message)" }

  $logs = $detailResp.data.messageLogs
  if ($logs -and ($logs | Measure-Object).Count -gt 0) {
    $last = $logs[($logs | Measure-Object).Count - 1]
    Write-Host ("lastLog: sendStatus={0} type={1} createdAt={2}" -f $last.sendStatus, $last.type, $last.createdAt)
  } else {
    Write-Host "detail returned no messageLogs (or empty)."
  }
}

Write-Host "Done." -ForegroundColor Green
