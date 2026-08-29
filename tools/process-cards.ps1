# Crop green borders, upscale, and export card PNGs for drawable-nodpi.
# Ten-rank overrides (10*2 chosen over 10*): cleaner "10" without slashed zero.
#   tenc <- 10c2, tend <- 10d2, tenh <- 10h2, tens <- 10s2
param(
    [string]$SourceDir = "C:\Users\richa\Downloads\cards",
    [string]$DestDir = "C:\Users\richa\AndroidStudioProjects\CardWar\app\src\main\res\drawable-nodpi",
    [int]$OutWidth = 186,
    [int]$OutHeight = 240
)

Add-Type -AssemblyName System.Drawing

function Test-GreenPixel([System.Drawing.Color]$c) {
    return ($c.G -ge 80) -and ($c.G -gt ($c.R + 25)) -and ($c.G -gt ($c.B + 15))
}

function Get-ContentBounds([System.Drawing.Bitmap]$bmp) {
    $minX = $bmp.Width
    $minY = $bmp.Height
    $maxX = 0
    $maxY = 0
    $found = $false

    for ($y = 0; $y -lt $bmp.Height; $y++) {
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            if (-not (Test-GreenPixel $bmp.GetPixel($x, $y))) {
                $found = $true
                if ($x -lt $minX) { $minX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }

    if (-not $found) {
        throw "No non-green content found"
    }

    return [PSCustomObject]@{
        X = $minX
        Y = $minY
        Width = ($maxX - $minX + 1)
        Height = ($maxY - $minY + 1)
    }
}

function Convert-CardFile {
    param(
        [string]$InputPath,
        [string]$OutputPath,
        [int]$Width,
        [int]$Height
    )

    $source = [System.Drawing.Image]::FromFile($InputPath)
    try {
        $sourceBmp = New-Object System.Drawing.Bitmap $source
        $bounds = Get-ContentBounds $sourceBmp
        $cropRect = New-Object System.Drawing.Rectangle $bounds.X, $bounds.Y, $bounds.Width, $bounds.Height
        $cropped = $sourceBmp.Clone($cropRect, $sourceBmp.PixelFormat)

        $dest = New-Object System.Drawing.Bitmap $Width, $Height
        $graphics = [System.Drawing.Graphics]::FromImage($dest)
        try {
            $graphics.Clear([System.Drawing.Color]::White)
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
            $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
            $graphics.DrawImage($cropped, 0, 0, $Width, $Height)
            $dest.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $graphics.Dispose()
            $dest.Dispose()
            $cropped.Dispose()
        }
    } finally {
        $source.Dispose()
        $sourceBmp.Dispose()
    }
}

# Ten-rank cards have alternate JPGs (*2.jpg). Chosen variants (matches 2-9 sans-serif, no slashed zero):
#   10c2 -> tenc   cleaner crop, consistent connected "10"
#   10d2 -> tend   sharper red edges vs slashed-zero 10d
#   10h2 -> tenh   no green-edge bleed after crop vs 10h
#   10s2 -> tens   connected "10", cleaner spade edges vs 10s
$tenSourceOverrides = @{
    '10c' = '10c2.jpg'
    '10d' = '10d2.jpg'
    '10h' = '10h2.jpg'
    '10s' = '10s2.jpg'
}

$rankNames = @{
    '2' = 'two'
    '3' = 'three'
    '4' = 'four'
    '5' = 'five'
    '6' = 'six'
    '7' = 'seven'
    '8' = 'eight'
    '9' = 'nine'
    '10' = 'ten'
    'j' = 'eleven'
    'q' = 'twelve'
    'k' = 'thirtteen'
    'a' = 'ace'
}

$files = Get-ChildItem -Path $SourceDir -Filter '*.jpg' -File |
    Where-Object { $_.BaseName -notmatch '2$' } |
    Sort-Object Name

if ($files.Count -ne 52) {
    Write-Warning "Expected 52 card JPGs, found $($files.Count)"
}

$processed = 0
foreach ($file in $files) {
    if ($file.BaseName -notmatch '^([2-9]|10|[ajqk])([cdhs])$') {
        Write-Warning "Skipping unrecognized name: $($file.Name)"
        continue
    }

    $rankKey = $Matches[1]
    $suit = $Matches[2]
    $rankName = $rankNames[$rankKey]
    $outName = "$rankName$suit.png"
    $outPath = Join-Path $DestDir $outName

    $cardKey = "$rankKey$suit"
    if ($tenSourceOverrides.ContainsKey($cardKey)) {
        $inputPath = Join-Path $SourceDir $tenSourceOverrides[$cardKey]
    } else {
        $inputPath = $file.FullName
    }

    Convert-CardFile -InputPath $inputPath -OutputPath $outPath -Width $OutWidth -Height $OutHeight
    $processed++
    Write-Output "Wrote $outName"
}

Write-Output "Done. Processed $processed cards -> ${OutWidth}x${OutHeight}px PNG"
