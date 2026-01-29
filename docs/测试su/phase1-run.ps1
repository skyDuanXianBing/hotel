param(
  [Parameter(Mandatory=$false)][string]$EnvFile = "server/.env",
  [Parameter(Mandatory=$false)][string]$ExcelPath = "",
  [Parameter(Mandatory=$false)][string]$OutDir = "",
  [Parameter(Mandatory=$false)][string]$ValidHotelId = "",
  [Parameter(Mandatory=$false)][string]$ValidRoomTypeId = "15",
  [Parameter(Mandatory=$false)][string]$ValidRatePlanId = "7"
)

$ErrorActionPreference = "Stop"

# Force TLS1.2 for Windows PowerShell 5.1 environments
try {
  [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
} catch {
  # ignore
}

function Read-DotEnv([string]$path) {
  if (!(Test-Path $path)) { throw "env file not found: $path" }
  $map = @{}
  Get-Content $path | ForEach-Object {
    $line = $_.Trim()
    if ($line.Length -eq 0) { return }
    if ($line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -le 0) { return }
    $k = $line.Substring(0, $idx).Trim()
    $v = $line.Substring($idx + 1).Trim()
    if ($k.Length -eq 0) { return }
    $map[$k] = $v
  }
  return $map
}

function Ensure-Dir([string]$p) {
  if (!(Test-Path $p)) { New-Item -ItemType Directory -Path $p | Out-Null }
}

function Write-JsonFile([string]$path, [object]$obj) {
  $json = $obj | ConvertTo-Json -Depth 30
  $json | Out-File -FilePath $path -Encoding UTF8
}

function Try-ParseJson([string]$text) {
  if ([string]::IsNullOrWhiteSpace($text)) { return $null }
  try {
    $cmd = Get-Command ConvertFrom-Json -ErrorAction Stop
    if ($cmd.Parameters.ContainsKey("Depth")) {
      return ($text | ConvertFrom-Json -Depth 50)
    }
    return ($text | ConvertFrom-Json)
  } catch {
    return $null
  }
}

function Invoke-Su([string]$method, [string]$url, [hashtable]$headers, [object]$bodyObj, [string]$caseId, [string]$outDir) {
  $reqPath = Join-Path $outDir ("{0}.request.json" -f $caseId.Replace("#","").Replace(" ",""))
  $respPath = Join-Path $outDir ("{0}.response.json" -f $caseId.Replace("#","").Replace(" ",""))

  $req = @{
    method = $method
    url = $url
    headers = ($headers.Keys | Sort-Object | ForEach-Object { @{ name=$_; value=$headers[$_] } })
    body = $bodyObj
  }
  Write-JsonFile $reqPath $req

  $body = $null
  if ($bodyObj -ne $null) { $body = ($bodyObj | ConvertTo-Json -Depth 30) }

  try {
    # Invoke-WebRequest on Windows PowerShell 5.1 may throw NullReferenceException for some TLS/response combos.
    # Use Invoke-RestMethod for success path, and manually read response for error path.
    $respObj = Invoke-RestMethod -Method $method -Uri $url -Headers $headers -ContentType "application/json" -Body $body
    $text = $null
    try { $text = ($respObj | ConvertTo-Json -Depth 50) } catch { $text = [string]$respObj }
    $json = Try-ParseJson $text
    $out = @{
      httpStatus = 200
      bodyText = $text
      bodyJson = $json
    }
    Write-JsonFile $respPath $out
    return $out
  } catch {
    $status = $null
    $text = $null
    if ($_.Exception.Response) {
      try { $status = [int]$_.Exception.Response.StatusCode.value__ } catch {}
      try {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $text = $reader.ReadToEnd()
      } catch {}
    }
    $json = Try-ParseJson $text
    $out = @{
      httpStatus = $status
      error = $_.Exception.Message
      bodyText = $text
      bodyJson = $json
    }
    Write-JsonFile $respPath $out
    return $out
  }
}

function Get-StatusText([object]$resp) {
  if ($resp -eq $null) { return $null }
  $j = $resp.bodyJson
  if ($j -eq $null -and $resp.bodyText) {
    # try parse again with trimmed text (some Su responses include leading BOM/whitespace)
    $t = [string]$resp.bodyText
    $t = $t.TrimStart([char]0xFEFF).Trim()
    $j = Try-ParseJson $t
  }

  if ($j -ne $null) {
    if ($j.Status) { return [string]$j.Status }
    if ($j.status) { return [string]$j.status }
    if ($j.Success) { return [string]$j.Success }
    if ($j.success -eq $true) { return "Success" }
  }

  # last resort: regex on bodyText
  $text = [string]$resp.bodyText
  if ($text -match '"Status"\s*:\s*"([^"]+)"') { return $Matches[1] }
  if ($text -match '"Success"\s*:\s*"([^"]+)"') { return $Matches[1] }
  return $null
}

function Is-Success([object]$resp) {
  $st = Get-StatusText $resp
  if ($st) {
    $n = $st.Trim().ToLowerInvariant()
    return ($n -eq "success" -or $n -eq "sucess") # Su excel typo
  }
  return $false
}

function Decide-PassFail([string]$expected, [object]$resp) {
  $exp = [string]$expected
  $expLow = $exp.ToLowerInvariant()
  $ok = Is-Success $resp
  if ($expLow.Contains("success")) { return $ok }
  if ($expLow.Contains("fail") -or $expLow.Contains("bad request") -or $expLow.Contains("invalid") -or $expLow.Contains("duplicate") -or $expLow.Contains("duplicated") -or $expLow.Contains("exists") -or $expLow.Contains("missing")) { return (-not $ok) }
  # fallback: treat 2xx as pass for non-empty expectation
  if ($resp.httpStatus -ge 200 -and $resp.httpStatus -lt 300) { return $true }
  return $false
}

function Excel-Open([string]$path) {
  $excel = New-Object -ComObject Excel.Application
  $excel.Visible = $false
  $excel.DisplayAlerts = $false
  $full = (Get-Item -LiteralPath $path).FullName
  $wb = $excel.Workbooks.Open($full)
  return @{ excel=$excel; wb=$wb }
}

function Excel-Close($ctx, [bool]$save) {
  $ctx.wb.Close($save)
  $ctx.excel.Quit()
  [System.Runtime.InteropServices.Marshal]::ReleaseComObject($ctx.wb) | Out-Null
  [System.Runtime.InteropServices.Marshal]::ReleaseComObject($ctx.excel) | Out-Null
}

function Excel-FindHeaderRow($ws) {
  $used = $ws.UsedRange
  $rows = $used.Rows.Count
  $cols = $used.Columns.Count
  for ($r=1; $r -le [Math]::Min($rows, 60); $r++) {
    for ($c=1; $c -le $cols; $c++) {
      $t = [string]$used.Item($r,$c).Text
      if ($t -eq "Test Case Id") { return @{ row=$r; cols=$cols } }
    }
  }
  throw "cannot find header row (Test Case Id)"
}

function Excel-GetColIndex($ws, $headerRow, [string]$name) {
  $used = $ws.UsedRange
  $cols = $used.Columns.Count
  for ($c=1; $c -le $cols; $c++) {
    $t = [string]$used.Item($headerRow,$c).Text
    if ($t -eq $name) { return $c }
  }
  throw "cannot find column: $name"
}

function Excel-FindCaseRowMap($ws, $headerRow) {
  $used = $ws.UsedRange
  $rows = $used.Rows.Count
  $cols = $used.Columns.Count
  $map = @{}
  for ($r=$headerRow+1; $r -le $rows; $r++) {
    $id = [string]$used.Item($r,1).Text
    if ($id -match '^Case #') {
      $map[$id] = $r
    }
  }
  return $map
}

Write-Host "=== Phase I Runner (SU Certification) ===" -ForegroundColor Cyan

$envMap = Read-DotEnv $EnvFile
$baseUrl = $envMap["SU_API_BASE_URL"]
$clientId = $envMap["SU_CLIENT_ID"]
$clientSecret = $envMap["SU_CLIENT_SECRET"]
if ([string]::IsNullOrWhiteSpace($baseUrl) -or [string]::IsNullOrWhiteSpace($clientId) -or [string]::IsNullOrWhiteSpace($clientSecret)) {
  throw "missing SU env in $EnvFile. need SU_API_BASE_URL / SU_CLIENT_ID / SU_CLIENT_SECRET"
}

if ([string]::IsNullOrWhiteSpace($OutDir)) {
  $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
  $OutDir = "docs/su_phase1_outputs_$stamp"
}
Ensure-Dir $OutDir

if ([string]::IsNullOrWhiteSpace($ValidHotelId)) {
  # Prefer a known-valid sandbox hotelid used in our existing Su webhook logs.
  $ValidHotelId = "S15KQEKHXP"
}

Write-Host "Output Dir: $OutDir" -ForegroundColor Gray
Write-Host "ValidHotelId: $ValidHotelId" -ForegroundColor Gray
Write-Host "ValidRoomTypeId(override): $ValidRoomTypeId" -ForegroundColor Gray
Write-Host "ValidRatePlanId(override): $ValidRatePlanId" -ForegroundColor Gray

Write-Host "[0] Generate access token" -ForegroundColor Yellow
$tokenHeaders = @{
  "client-id" = $clientId
  "client-secret" = $clientSecret
  "Content-Type" = "application/json"
}
$tokenJson = $null
try {
  $tokenJson = Invoke-RestMethod -Method Get -Uri "$baseUrl/SUAPI/jservice/auth/generate-access-token" -Headers $tokenHeaders
} catch {
  throw "generate-access-token failed: $($_.Exception.Message)"
}
$token = $null
if ($tokenJson.data -and $tokenJson.data.token) { $token = $tokenJson.data.token }
if (-not $token -and $tokenJson.Data -and $tokenJson.Data.token) { $token = $tokenJson.Data.token }
if (-not $token) { throw "cannot parse token: $($tokenJson | ConvertTo-Json -Depth 10)" }

$authHeaders = @{
  "Authorization" = "Bearer $token"
  "client-id" = $clientId
  "client-secret" = $clientSecret
  "app-id" = $clientId
  "Content-Type" = "application/json"
}

function Build-PropertyPayload([string]$hotelId, [switch]$MissingCurrency) {
  $address = @{
    AddressLine = "Test Address"
    CityName = "Tokyo"
    PostalCode = "000000"
    CountryName = "JP"
  }

  $physicalLocationContact = @{
    ContactProfileType = "PhysicalLocation"
    Addresses = @{ Address = $address }
  }

  # Su sandbox may require an "availability" ContactInfo for property creation.
  $availabilityContact = @{
    ContactProfileType = "availability"
    Addresses = @{ Address = $address }
    Names = @{ Name = @{ GivenName = "HostHub"; Surname = "Admin" } }
    Emails = @{ Email = @("test@example.com") }
    Phones = @{ Phone = @(@{ PhoneNumber = "+811000000000"; PhoneTechType = "1" }) }
  }

  $content = @{
    HotelName = "SaitoShoji Test Property"
    HotelType = "3"
    TimeZone = "Asia/Tokyo"
    Platform = "SU"
    hotelid = $hotelId
    LanguageCode = "en"
    HotelDescriptiveContentNotifType = "New"
    HotelInfo = @{ Position = @{ Latitude="0"; Longitude="0" } }
    ContactInfos = @{
      ContactInfo = @(
        @{
          ContactProfileType = "PhysicalLocation"
          Addresses = @{ Address = $address }
        },
        $availabilityContact
      )
    }
  }
  if (-not $MissingCurrency) { $content["CurrencyCode"] = "JPY" }
  return @{ HotelDescriptiveContents = @{ HotelDescriptiveContent = $content } }
}

function Build-RoomTypeCreatePayload([string]$hotelId, [string]$roomTypeId) {
  $room = @{
    roomid = $roomTypeId
    RoomRate = "1"
    Quantity = "1"
    RoomType = "Double"
  }
  $occupancy = @{ MaxOccupancy="2"; MaxChildOccupancy="0" }
  $description = @{ Text = "RoomType-$roomTypeId" }
  $guestRoom = @{ Occupancy=$occupancy; Room=$room; Description=$description }
  $sellableProduct = @{ InvStatusType="Initial"; GuestRoom=$guestRoom }
  return @{ SellableProducts = @{ hotelid=$hotelId; SellableProduct=@($sellableProduct) } }
}

function Build-RoomTypeDeactivatePayload([string]$hotelId, [string]$roomTypeId) {
  # Best-effort: use Modify/Overlay and set Quantity=0 to emulate deactivation.
  $room = @{
    RoomRate = "1"
    Quantity = "0"
    RoomType = "Double"
  }
  $description = @{ Text = "RoomType-$roomTypeId" }
  $guestRoom = @{ Room=$room; Description=$description }
  $sellableProduct = @{ InvNotifType="Overlay"; InvStatusType="Modify"; roomid=$roomTypeId; GuestRoom=$guestRoom }
  return @{ SellableProducts = @{ hotelid=$hotelId; SellableProduct=@($sellableProduct) } }
}

function Build-RatePlanCreatePayload([string]$hotelId, [string]$ratePlanId, [switch]$MissingDescription) {
  $rp = @{
    RatePlanNotifType = "New"
    rateplanid = $ratePlanId
    MealPlanID = "15"
  }
  if (-not $MissingDescription) {
    $rp["Description"] = @{ Name="Rate-$ratePlanId"; Text="Rate-$ratePlanId" }
  }
  return @{ RatePlans = @{ hotelid=$hotelId; RatePlan=@($rp) } }
}

function Build-RatePlanOverlayPayload([string]$hotelId, [string]$ratePlanId, [string]$newName) {
  $rp = @{
    RatePlanNotifType = "Overlay"
    rateplanid = $ratePlanId
    MealPlanID = "15"
    Description = @{ Name=$newName; Text=$newName }
  }
  return @{ RatePlans = @{ hotelid=$hotelId; RatePlan=@($rp) } }
}

function Build-InvRateControlPayload([string]$hotelId, [string]$roomId, [string]$rateId, [string]$from, [string]$to, [string]$value, [switch]$IncludeInventory, [switch]$IncludeRate) {
  $payload = @{ hotelid = $hotelId }
  if ($IncludeInventory) {
    $payload["inventorycontrol"] = @(
      @{
        roomid = $roomId
        date = @(
          @{
            from = $from
            to = $to
            OTARule = @(
              @{
                OTACode = @(19,244)
                rule = @{ type="Fixed"; value=$value }
              }
            )
          }
        )
      }
    )
  }
  if ($IncludeRate) {
    $payload["ratecontrol"] = @(
      @{
        roomid = $roomId
        rateid = $rateId
        date = @(
          @{
            from = $from
            to = $to
            OTARule = @(
              @{
                OTACode = @(19,244)
                rule = @{ type="Fixed"; value=$value }
              }
            )
          }
        )
      }
    )
  }
  return $payload
}

# Locate Excel if not provided (avoid Unicode path parsing issues in Windows PowerShell)
if ([string]::IsNullOrWhiteSpace($ExcelPath) -or !(Test-Path -LiteralPath $ExcelPath)) {
  $found = Get-ChildItem -Path "docs" -Recurse -File -Filter "SaitoShoji Co - Certification.xlsx" | Select-Object -First 1
  if (-not $found) { throw "cannot find excel under docs: SaitoShoji Co - Certification.xlsx" }
  $ExcelPath = $found.FullName
}

# Open Excel and prepare mapping
$ctx = Excel-Open $ExcelPath
try {
  $ws = $ctx.wb.Worksheets.Item(1)
  $headerInfo = Excel-FindHeaderRow $ws
  $hr = $headerInfo.row
  $colExpected = Excel-GetColIndex $ws $hr "Expected Result"
  $colActual = Excel-GetColIndex $ws $hr "Actual Result"
  $colStatus = Excel-GetColIndex $ws $hr "Status (Pass/Fail)"
  $caseRow = Excel-FindCaseRowMap $ws $hr

  $results = @()

  function Run-Case([string]$caseId, [string]$url, [object]$payload) {
    if (-not $caseRow.ContainsKey($caseId)) { throw "case row not found in excel: $caseId" }
    $row = $caseRow[$caseId]
    $expected = [string]$ws.UsedRange.Item($row, $colExpected).Text
    $resp = Invoke-Su -method "POST" -url $url -headers $authHeaders -bodyObj $payload -caseId $caseId -outDir $OutDir
    $pass = Decide-PassFail $expected $resp
    $actualText = $resp.bodyText
    if ([string]::IsNullOrWhiteSpace($actualText)) { $actualText = ("error=" + $resp.error) }
    if ($actualText.Length -gt 2000) { $actualText = $actualText.Substring(0,2000) + "..." }
    $ws.UsedRange.Item($row, $colActual).Value2 = $actualText
    $ws.UsedRange.Item($row, $colStatus).Value2 = $(if($pass){"Pass"}else{"Fail"})
    $results += @{ caseId=$caseId; pass=$pass; httpStatus=$resp.httpStatus; expected=$expected }
    Write-Host ("{0}: {1}" -f $caseId, $(if($pass){"PASS"}else{"FAIL"})) -ForegroundColor $(if($pass){"Green"}else{"Red"})
  }

  # URLs
  $urlProperty = "$baseUrl/SUAPI/jservice/OTA_HotelDescriptiveContentNotif"
  $urlRoom = "$baseUrl/SUAPI/jservice/OTA_HotelRoom"
  $urlRatePlan = "$baseUrl/SUAPI/jservice/OTA_HotelRatePlan"
  $urlInv = "$baseUrl/SUAPI/jservice/invratecontrol"

  # Ensure the "new hotel id" case runs first (we need a valid property for later cases).
  $newHotelId = "SS" + (Get-Date -Format "yyMMddHHmmss")
  Run-Case "Case # 1_2" $urlProperty (Build-PropertyPayload -hotelId $newHotelId)
  Run-Case "Case # 1" $urlProperty (Build-PropertyPayload -hotelId $ValidHotelId)
  Run-Case "Case # 1_1" $urlProperty (Build-PropertyPayload -hotelId $ValidHotelId -MissingCurrency)

  # Room type
  $roomTypeIdForTest = "RT" + (Get-Date -Format "HHmmss")
  Run-Case "Case # 2" $urlRoom (Build-RoomTypeCreatePayload -hotelId "XYZ" -roomTypeId $roomTypeIdForTest)
  Run-Case "Case # 2_3" $urlRoom (Build-RoomTypeCreatePayload -hotelId $ValidHotelId -roomTypeId $roomTypeIdForTest)
  Run-Case "Case # 2_1" $urlRoom (Build-RoomTypeCreatePayload -hotelId $ValidHotelId -roomTypeId $roomTypeIdForTest)
  Run-Case "Case # 2_2" $urlRoom @{ SellableProducts = @{ hotelid=$ValidHotelId; SellableProduct=@(@{}) } }

  # Deactivate room type (best-effort payload)
  Run-Case "Case # 3" $urlRoom (Build-RoomTypeDeactivatePayload -hotelId "XYZ" -roomTypeId $roomTypeIdForTest)
  Run-Case "Case # 3_1" $urlRoom (Build-RoomTypeDeactivatePayload -hotelId $ValidHotelId -roomTypeId "999999")
  Run-Case "Case # 3_2" $urlRoom (Build-RoomTypeDeactivatePayload -hotelId $ValidHotelId -roomTypeId $roomTypeIdForTest)

  # Rate plan
  $ratePlanIdForTest = "RP" + (Get-Date -Format "HHmmss")
  Run-Case "Case # 4" $urlRatePlan (Build-RatePlanCreatePayload -hotelId "KC5" -ratePlanId $ratePlanIdForTest)
  Run-Case "Case # 4_3" $urlRatePlan (Build-RatePlanCreatePayload -hotelId $ValidHotelId -ratePlanId $ratePlanIdForTest)
  Run-Case "Case # 4_1" $urlRatePlan (Build-RatePlanCreatePayload -hotelId $ValidHotelId -ratePlanId $ratePlanIdForTest)
  Run-Case "Case # 4_2" $urlRatePlan (Build-RatePlanCreatePayload -hotelId $ValidHotelId -ratePlanId ("RPMISS" + (Get-Date -Format "HHmm")) -MissingDescription)

  # Change rate plan name (best-effort overlay)
  Run-Case "Case # 5" $urlRatePlan (Build-RatePlanOverlayPayload -hotelId "KC5" -ratePlanId "12345889Ab" -newName "RateNameInvalidHotel")
  Run-Case "Case # 5_1" $urlRatePlan (Build-RatePlanOverlayPayload -hotelId $ValidHotelId -ratePlanId "999999" -newName "RateNameInvalidPlan")
  Run-Case "Case # 5_2" $urlRatePlan (Build-RatePlanOverlayPayload -hotelId $ValidHotelId -ratePlanId $ratePlanIdForTest -newName ("Rate-" + (Get-Date -Format "HHmmss")))

  # Deactivate/Activate rate plan (no dedicated payload in repo; try overlay as placeholder)
  Run-Case "Case # 6" $urlRatePlan (Build-RatePlanOverlayPayload -hotelId "KC5" -ratePlanId "BAR" -newName "BAR")
  Run-Case "Case # 6_1" $urlRatePlan (Build-RatePlanOverlayPayload -hotelId $ValidHotelId -ratePlanId "BAR2" -newName "BAR2")
  Run-Case "Case # 6_2" $urlRatePlan (Build-RatePlanOverlayPayload -hotelId $ValidHotelId -ratePlanId $ratePlanIdForTest -newName ("Rate-" + (Get-Date -Format "HHmmss")))

  # Availability (Rates & Inventory)
  # Prefer a known roomid pattern used in Su reservation payloads.
  $roomIdForInv = "15-999"
  $today = Get-Date
  $fromValid = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
  $toValid = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
  $fromPast = "2020-08-30"
  $toPast = "2020-08-30"

  Run-Case "Case # 7" $urlInv (Build-InvRateControlPayload -hotelId $ValidHotelId -roomId "999999-1" -rateId $ratePlanIdForTest -from $fromValid -to $toValid -value "1" -IncludeInventory -IncludeRate)
  Run-Case "Case # 7_1" $urlInv (Build-InvRateControlPayload -hotelId $ValidHotelId -roomId $roomIdForInv -rateId "999999" -from $fromValid -to $toValid -value "1" -IncludeInventory -IncludeRate)
  Run-Case "Case # 7_2" $urlInv (Build-InvRateControlPayload -hotelId $ValidHotelId -roomId $roomIdForInv -rateId $ratePlanIdForTest -from $fromPast -to $toPast -value "1" -IncludeInventory -IncludeRate)
  Run-Case "Case # 7_3" $urlInv (Build-InvRateControlPayload -hotelId $ValidHotelId -roomId $roomIdForInv -rateId $ratePlanIdForTest -from $fromValid -to $toValid -value "1" -IncludeInventory -IncludeRate)
  Run-Case "Case # 7_4" $urlInv (Build-InvRateControlPayload -hotelId $ValidHotelId -roomId $roomIdForInv -rateId $ratePlanIdForTest -from $fromValid -to $toValid -value "1" -IncludeInventory -IncludeRate)

  $filledStamp = Get-Date -Format "yyyyMMdd-HHmmss"
  $filledPath = Join-Path (Split-Path $ExcelPath) ("SaitoShoji Co - Certification_phase1_filled_{0}.xlsx" -f $filledStamp)
  $ctx.wb.SaveAs($filledPath)
  Write-Host "Saved filled excel: $filledPath" -ForegroundColor Green
} finally {
  Excel-Close $ctx $false
}

Write-Host "Done. Outputs in: $OutDir" -ForegroundColor Green
