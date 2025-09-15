# NoticeBoard App - Logo and Asset Generation Guide

## Overview
This guide explains how to convert the SVG logos to all required formats for your NoticeBoard app.

## Files Created
- `logo.svg` - Full logo with text (for marketing materials)
- `logo-icon.svg` - Icon-only version (for app icons)
- `playstore-feature-graphic.svg` - Play Store feature graphic (1024x500)
- `playstore-app-icon.svg` - High-res app icon (512x512)

## Required Conversions

### 1. App Launcher Icons
Convert `logo-icon.svg` to PNG files in these sizes:

**Android Launcher Icons:**
- `mipmap-mdpi/ic_launcher.png` - 48x48px
- `mipmap-hdpi/ic_launcher.png` - 72x72px
- `mipmap-xhdpi/ic_launcher.png` - 96x96px
- `mipmap-xxhdpi/ic_launcher.png` - 144x144px
- `mipmap-xxxhdpi/ic_launcher.png` - 192x192px

**Round Icons (same sizes):**
- `mipmap-mdpi/ic_launcher_round.png` - 48x48px
- `mipmap-hdpi/ic_launcher_round.png` - 72x72px
- `mipmap-xhdpi/ic_launcher_round.png` - 96x96px
- `mipmap-xxhdpi/ic_launcher_round.png` - 144x144px
- `mipmap-xxxhdpi/ic_launcher_round.png` - 192x192px

### 2. Play Store Assets
Convert these SVG files to PNG:

**App Icon:**
- `playstore-app-icon.svg` → `app-icon.png` (512x512px)

**Feature Graphic:**
- `playstore-feature-graphic.svg` → `feature-graphic.png` (1024x500px)

## Conversion Methods

### Method 1: Using Inkscape (Recommended)
1. Install Inkscape from https://inkscape.org/
2. Run the provided `generate_icons.py` script
3. The script will generate all required PNG files

### Method 2: Using Android Studio
1. Open Android Studio
2. Right-click on `app/src/main/res` → New → Image Asset
3. Choose "Launcher Icons (Adaptive and Legacy)"
4. Select your SVG file as the source
5. Android Studio will generate all required sizes

### Method 3: Online Converters
1. Use online SVG to PNG converters like:
   - https://convertio.co/svg-png/
   - https://cloudconvert.com/svg-to-png
2. Manually resize to required dimensions

### Method 4: Using Command Line Tools
If you have ImageMagick installed:
```bash
# Convert SVG to PNG with specific size
magick logo-icon.svg -resize 48x48 mipmap-mdpi/ic_launcher.png
magick logo-icon.svg -resize 72x72 mipmap-hdpi/ic_launcher.png
magick logo-icon.svg -resize 96x96 mipmap-xhdpi/ic_launcher.png
magick logo-icon.svg -resize 144x144 mipmap-xxhdpi/ic_launcher.png
magick logo-icon.svg -resize 192x192 mipmap-xxxhdpi/ic_launcher.png
```

## File Structure After Conversion
```
app/src/main/res/
├── mipmap-mdpi/
│   ├── ic_launcher.png (48x48)
│   └── ic_launcher_round.png (48x48)
├── mipmap-hdpi/
│   ├── ic_launcher.png (72x72)
│   └── ic_launcher_round.png (72x72)
├── mipmap-xhdpi/
│   ├── ic_launcher.png (96x96)
│   └── ic_launcher_round.png (96x96)
├── mipmap-xxhdpi/
│   ├── ic_launcher.png (144x144)
│   └── ic_launcher_round.png (144x144)
├── mipmap-xxxhdpi/
│   ├── ic_launcher.png (192x192)
│   └── ic_launcher_round.png (192x192)
└── drawable/
    ├── ic_launcher_background.xml (updated)
    ├── ic_launcher_foreground.xml (updated)
    └── ic_noticeboard_logo.xml (new)
```

## Play Store Assets
```
playstore-assets/
├── app-icon.png (512x512)
└── feature-graphic.png (1024x500)
```

## Next Steps
1. Convert SVG files to PNG using one of the methods above
2. Replace existing icon files in the `mipmap-*` directories
3. Test the app to ensure icons display correctly
4. Use Play Store assets when uploading to Google Play Console

## Notes
- The vector drawable files (`ic_launcher_foreground.xml`, `ic_launcher_background.xml`) have been updated to use the new NoticeBoard design
- The app theme has been updated to use NoticeBoard brand colors
- The splash screen now uses the new logo
- All assets follow Material Design guidelines and Android icon requirements

