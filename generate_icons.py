#!/usr/bin/env python3
"""
Icon Generator Script for NoticeBoard App
Generates all required launcher icon sizes from SVG logo
"""

import os
import subprocess
import sys
from pathlib import Path

# Icon sizes for different densities
ICON_SIZES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

# Play Store icon sizes
PLAYSTORE_SIZES = {
    'app-icon': 512,
    'feature-graphic': (1024, 500),
    'screenshot': (1080, 1920),
}

def check_dependencies():
    """Check if required tools are installed"""
    try:
        subprocess.run(['inkscape', '--version'], capture_output=True, check=True)
        print("✓ Inkscape found")
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("✗ Inkscape not found. Please install Inkscape to generate icons.")
        print("  Download from: https://inkscape.org/release/")
        return False

def generate_icon(svg_path, output_path, size):
    """Generate PNG icon from SVG"""
    try:
        cmd = [
            'inkscape',
            '--export-type=png',
            f'--export-filename={output_path}',
            f'--export-width={size}',
            f'--export-height={size}',
            svg_path
        ]
        subprocess.run(cmd, check=True, capture_output=True)
        print(f"✓ Generated {output_path} ({size}x{size})")
        return True
    except subprocess.CalledProcessError as e:
        print(f"✗ Failed to generate {output_path}: {e}")
        return False

def generate_playstore_assets(svg_path, output_dir):
    """Generate Play Store assets"""
    assets_dir = Path(output_dir) / "playstore-assets"
    assets_dir.mkdir(exist_ok=True)
    
    # App icon (512x512)
    generate_icon(svg_path, assets_dir / "app-icon.png", PLAYSTORE_SIZES['app-icon'])
    
    # Feature graphic (1024x500) - create a banner version
    try:
        cmd = [
            'inkscape',
            '--export-type=png',
            f'--export-filename={assets_dir / "feature-graphic.png"}',
            f'--export-width={PLAYSTORE_SIZES["feature-graphic"][0]}',
            f'--export-height={PLAYSTORE_SIZES["feature-graphic"][1]}',
            svg_path
        ]
        subprocess.run(cmd, check=True, capture_output=True)
        print(f"✓ Generated feature graphic")
    except subprocess.CalledProcessError as e:
        print(f"✗ Failed to generate feature graphic: {e}")

def main():
    """Main function"""
    print("NoticeBoard App - Icon Generator")
    print("=" * 40)
    
    # Check dependencies
    if not check_dependencies():
        return 1
    
    # Get project paths
    project_root = Path(__file__).parent
    svg_path = project_root / "logo-icon.svg"
    res_dir = project_root / "app" / "src" / "main" / "res"
    
    if not svg_path.exists():
        print(f"✗ SVG file not found: {svg_path}")
        return 1
    
    print(f"Using SVG: {svg_path}")
    print(f"Output directory: {res_dir}")
    
    # Generate launcher icons
    print("\nGenerating launcher icons...")
    for density, size in ICON_SIZES.items():
        density_dir = res_dir / density
        density_dir.mkdir(exist_ok=True)
        
        # Generate regular icon
        icon_path = density_dir / "ic_launcher.png"
        generate_icon(svg_path, icon_path, size)
        
        # Generate round icon (same as regular for now)
        round_icon_path = density_dir / "ic_launcher_round.png"
        generate_icon(svg_path, round_icon_path, size)
    
    # Generate Play Store assets
    print("\nGenerating Play Store assets...")
    generate_playstore_assets(svg_path, project_root)
    
    print("\n✓ All icons generated successfully!")
    print("\nNext steps:")
    print("1. Convert PNG files to WebP format for Android")
    print("2. Update ic_launcher.xml and ic_launcher_round.xml")
    print("3. Update splash screen with logo")
    
    return 0

if __name__ == "__main__":
    sys.exit(main())

