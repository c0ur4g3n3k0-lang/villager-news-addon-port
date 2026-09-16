param(
    [string]$Atlas = 'src/main/resources/assets/villager-news-addon-port/textures/entity/sign_text_ru_ru.png',
    [string]$OutputDirectory = 'build/sign-inspection-ru'
)

Add-Type -AssemblyName System.Drawing
$atlasPath = (Resolve-Path -LiteralPath $Atlas).Path
$outputPath = Join-Path (Get-Location) $OutputDirectory
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null

$bitmap = [System.Drawing.Bitmap]::FromFile($atlasPath)
try {
    if ($bitmap.Width -ne 96 -or $bitmap.Height -ne 3045) { throw 'Expected a 96x3045 atlas' }
    $font = [System.Drawing.Font]::new('Segoe UI', 12, [System.Drawing.FontStyle]::Bold)
    try {
        for ($page = 0; $page -lt 8; $page++) {
            $sheet = [System.Drawing.Bitmap]::new(1200, 672)
            $graphics = [System.Drawing.Graphics]::FromImage($sheet)
            try {
                $graphics.Clear([System.Drawing.Color]::White)
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                for ($slot = 0; $slot -lt 12; $slot++) {
                    $index = $page * 12 + $slot
                    if ($index -ge 87) { break }
                    $x = ($slot % 3) * 400 + 8
                    $y = [Math]::Floor($slot / 3) * 168 + 24
                    $graphics.DrawString("#$index", $font, [System.Drawing.Brushes]::Black, $x, $y - 23)
                    $source = [System.Drawing.Rectangle]::new(0, $index * 35, 96, 35)
                    $destination = [System.Drawing.Rectangle]::new($x, $y, 384, 140)
                    $graphics.DrawImage($bitmap, $destination, $source, [System.Drawing.GraphicsUnit]::Pixel)
                }
                $sheet.Save((Join-Path $outputPath ("signs-{0:D2}.png" -f $page)), [System.Drawing.Imaging.ImageFormat]::Png)
            } finally {
                $graphics.Dispose()
                $sheet.Dispose()
            }
        }
    } finally { $font.Dispose() }
} finally { $bitmap.Dispose() }

Write-Output "Created indexed previews in $outputPath"
