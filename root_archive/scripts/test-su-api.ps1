# Su API Credential Test Script
# Used to verify Su API sandbox environment connection and credentials

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Su API Credential Test" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Su API Configuration
$baseUrl = "https://connect-sandbox.su-api.com"
$clientId = "c2FpdG9zaG9qaWNvbHRkLnN1aXNzdS5jb20="
$clientSecret = "NVI4NWo1Yzc6WWl5OHY2OVc"

Write-Host "Test Configuration:" -ForegroundColor Yellow
Write-Host "  Base URL: $baseUrl"
Write-Host "  Client ID: $clientId"
Write-Host "  Client Secret: $($clientSecret.Substring(0,10))..." -ForegroundColor Gray
Write-Host ""

# Test 1: Generate Access Token
Write-Host "[Test 1/4] Generating Access Token..." -ForegroundColor Yellow

$headers = @{
    "client-id" = $clientId
    "client-secret" = $clientSecret
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/SUAPI/jservice/auth/generate-access-token" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop

    Write-Host "Raw Response:" -ForegroundColor Gray
    Write-Host ($response | ConvertTo-Json -Depth 5) -ForegroundColor Gray
    Write-Host ""

    # Normalize Su token response format (some endpoints use Status/Data, some use success/data)
    $status = $response.status
    if (-not $status) { $status = $response.Status }

    $data = $response.data
    if (-not $data) { $data = $response.Data }

    $isSuccess = $false
    if ($response.success -eq $true) { $isSuccess = $true }
    if ($status -and ($status -eq "Success" -or $status -eq "SUCCESS")) { $isSuccess = $true }

    if (-not $isSuccess) {
        Write-Host "Failed: Access Token generation failed!" -ForegroundColor Red
        Write-Host "  Response: $($response | ConvertTo-Json -Depth 6)" -ForegroundColor Red
        exit 1
    }

    $accessToken = $data.token
    if (-not $accessToken) {
        Write-Host "Failed: No access token in response!" -ForegroundColor Red
        exit 1
    }

    $expireIn = $data.expire_in
    if (-not $expireIn) { $expireIn = $data.expireIn }
    if (-not $expireIn) { $expireIn = $data.expiresIn }

    Write-Host "Success: Access Token generated!" -ForegroundColor Green
    Write-Host "  Status: $status" -ForegroundColor Gray
    Write-Host "  Token: $($accessToken.Substring(0,20))..." -ForegroundColor Gray
    if ($expireIn) {
        Write-Host "  Expires In: $expireIn seconds" -ForegroundColor Gray
    }

} catch {
    Write-Host "Failed: API call failed!" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 2: Validate Access Token
Write-Host "[Test 2/4] Validating Access Token..." -ForegroundColor Yellow

$authHeaders = @{
    "Authorization" = "Bearer $accessToken"
    "client-id"     = $clientId
    "client-secret" = $clientSecret
    "Content-Type" = "application/json"
}

try {
    $pmsproperties = Invoke-RestMethod -Uri "$baseUrl/SUAPI/jservice/pmsproperty" `
        -Method Get `
        -Headers $authHeaders `
        -ErrorAction Stop

    Write-Host "Success: Access Token validated!" -ForegroundColor Green
    Write-Host "  API response received" -ForegroundColor Gray
} catch {
    Write-Host "Warning: Access Token validation endpoint may not be accessible" -ForegroundColor Yellow
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Gray
    Write-Host "  This is OK - token was generated successfully" -ForegroundColor Gray
}

Write-Host ""

# Test 3: Create/Upsert Property (required before Widget Token can succeed)
Write-Host "[Test 3/4] Creating/Upserting Property..." -ForegroundColor Yellow

$testHotelId = "STORETEST1"

$propertyHeaders = @{
    "Authorization" = "Bearer $accessToken"
    "client-id"     = $clientId
    "client-secret" = $clientSecret
    "app-id"        = $clientId
    "Content-Type"  = "application/json"
}

$propertyBody = @{
    HotelDescriptiveContents = @{
        HotelDescriptiveContent = @{
            HotelName                       = "HostHub Test Property"
            HotelType                       = "3"
            TimeZone                        = "Asia/Shanghai"
            Platform                        = "SU"
            hotelid                         = $testHotelId
            LanguageCode                    = "en"
            CurrencyCode                    = "CNY"
            HotelDescriptiveContentNotifType = "New"
            HotelInfo                       = @{
                Position = @{
                    Latitude  = "0"
                    Longitude = "0"
                }
            }
            ContactInfos                    = @{
                ContactInfo = @(
                    @{
                        ContactProfileType = "PhysicalLocation"
                        Addresses          = @{
                            Address = @{
                                AddressLine = "Test Address"
                                CityName    = "Shanghai"
                                PostalCode  = "000000"
                                CountryName = "CN"
                            }
                        }
                    },
                    @{
                        ContactProfileType = "availability"
                        Names              = @{
                            Name = @{
                                GivenName = "HostHub"
                                Surname   = "Admin"
                            }
                        }
                        Addresses          = @{
                            Address = @{
                                AddressLine = "Test Address"
                                CityName    = "Shanghai"
                                PostalCode  = "000000"
                                CountryName = "CN"
                            }
                        }
                        Emails             = @{
                            Email = @("admin@example.com")
                        }
                        Phones             = @{
                            Phone = @(
                                @{
                                    PhoneNumber   = "+8610000000000"
                                    PhoneTechType = "1"
                                }
                            )
                        }
                    }
                )
            }
        }
    }
} | ConvertTo-Json -Depth 12

try {
    $propertyResponse = Invoke-RestMethod -Uri "$baseUrl/SUAPI/jservice/OTA_HotelDescriptiveContentNotif" `
        -Method Post `
        -Headers $propertyHeaders `
        -Body $propertyBody `
        -ErrorAction Stop

    Write-Host "Raw Property Response:" -ForegroundColor Gray
    Write-Host ($propertyResponse | ConvertTo-Json -Depth 8) -ForegroundColor Gray

    if ($propertyResponse.Status -and $propertyResponse.Status -eq "Success") {
        Write-Host "Success: Property upserted!" -ForegroundColor Green
        Write-Host "  hotelid: $testHotelId" -ForegroundColor Gray
    } else {
        Write-Host "Warning: Property upsert response indicates non-success" -ForegroundColor Yellow
        Write-Host "  hotelid: $testHotelId" -ForegroundColor Gray
        if ($propertyResponse.Errors) {
            Write-Host "  Errors: $($propertyResponse.Errors | ConvertTo-Json -Depth 6)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "Warning: Property upsert failed" -ForegroundColor Yellow
    Write-Host "  hotelid: $testHotelId" -ForegroundColor Gray
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Gray
    if ($_.Exception.Response) {
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $body = $reader.ReadToEnd()
            if ($body) {
                Write-Host "  Response Body: $body" -ForegroundColor Gray
            }
        } catch {
            # ignore
        }
    }
}

Write-Host ""

# Test 4: Generate Widget Token (requires a valid hotelid)
Write-Host "[Test 4/4] Generating Widget Token..." -ForegroundColor Yellow

$widgetBody = @{
    hotelid = $testHotelId
} | ConvertTo-Json

try {
    $widgetResponse = Invoke-RestMethod -Uri "$baseUrl/SUAPI/jservice/widget/getWidgetAccessToken" `
        -Method Post `
        -Headers $authHeaders `
        -Body $widgetBody `
        -ErrorAction Stop

    Write-Host "Raw Widget Response:" -ForegroundColor Gray
    Write-Host ($widgetResponse | ConvertTo-Json -Depth 5) -ForegroundColor Gray
    Write-Host ""

    if ($widgetResponse.Status -eq "Success" -or $widgetResponse.status -eq "Success") {
        Write-Host "Success: Widget Token generated!" -ForegroundColor Green
        $data = $widgetResponse.Data
        if (-not $data) { $data = $widgetResponse.data }
        $tokenId = $data.token_id
        if (-not $tokenId) { $tokenId = $data.tokenId }
        Write-Host "  Token ID: $($tokenId.Substring(0,20))..." -ForegroundColor Gray
        Write-Host "  Property ID (encrypted): $($data.proppmsid)" -ForegroundColor Gray
        Write-Host "  PMS Name: $($data.pms_name)" -ForegroundColor Gray
    } else {
        Write-Host "Warning: Widget Token response format different" -ForegroundColor Yellow
        $status = $widgetResponse.Status
        if (-not $status) { $status = $widgetResponse.status }
        Write-Host "  Status: $status" -ForegroundColor Gray
        if ($widgetResponse.Message) {
            Write-Host "  Message: $($widgetResponse.Message)" -ForegroundColor Gray
        } elseif ($widgetResponse.message) {
            Write-Host "  Message: $($widgetResponse.message)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "Note: Widget Token generation returned error" -ForegroundColor Yellow
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Gray
    Write-Host "  This usually means the hotelid is invalid or the property does not exist in Su" -ForegroundColor Gray
    if ($_.Exception.Response) {
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $body = $reader.ReadToEnd()
            if ($body) {
                Write-Host "  Response Body: $body" -ForegroundColor Gray
            }
        } catch {
            # ignore
        }
    }
}

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Test Complete!" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Green
Write-Host "1. Configure environment variables in server/.env file" -ForegroundColor Gray
Write-Host "   SU_API_BASE_URL=https://connect-sandbox.su-api.com" -ForegroundColor Gray
Write-Host "   SU_CLIENT_ID=$clientId" -ForegroundColor Gray
Write-Host "   SU_CLIENT_SECRET=$clientSecret" -ForegroundColor Gray
Write-Host "   SU_PMS_NAME=HostHub" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Start backend: cd server; ./mvnw.cmd spring-boot:run" -ForegroundColor Gray
Write-Host "3. Start frontend: cd client; bun run dev" -ForegroundColor Gray
Write-Host "4. Test OTA connection in Channel Management page" -ForegroundColor Gray
Write-Host ""
