#!/usr/bin/env python3
"""Validate Google Play store asset files using only the Python standard library."""

from __future__ import annotations

import argparse
import struct
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


@dataclass(frozen=True)
class ImageInfo:
    format: str
    width: int
    height: int
    has_alpha: bool
    details: str


@dataclass(frozen=True)
class AssetRule:
    group: str
    label: str
    relative_path: str
    allowed_formats: tuple[str, ...]
    exact_size: tuple[int, int] | None = None
    max_bytes: int | None = None
    png_alpha_required: bool | None = None
    min_dimension: int | None = None
    max_dimension: int | None = None
    max_aspect_ratio: float | None = None
    portrait_warning: bool = False


@dataclass
class Result:
    status: str
    path: Path
    image_format: str
    dimensions: str
    reason: str


ASSET_RULES = [
    AssetRule(
        group="icon",
        label="App icon",
        relative_path="icon/local-find-icon-512.png",
        allowed_formats=("PNG",),
        exact_size=(512, 512),
        max_bytes=1024 * 1024,
        png_alpha_required=True,
    ),
    AssetRule(
        group="feature graphic",
        label="Feature graphic",
        relative_path="feature-graphic/local-find-feature-1024x500.png",
        allowed_formats=("PNG", "JPEG"),
        exact_size=(1024, 500),
        png_alpha_required=False,
    ),
    AssetRule(
        group="screenshots",
        label="Find Me service screenshot",
        relative_path="screenshots/en-US/01-find-me-service.png",
        allowed_formats=("PNG", "JPEG"),
        png_alpha_required=False,
        min_dimension=320,
        max_dimension=3840,
        max_aspect_ratio=2.0,
        portrait_warning=True,
    ),
    AssetRule(
        group="screenshots",
        label="Controller devices screenshot",
        relative_path="screenshots/en-US/02-controller-devices.png",
        allowed_formats=("PNG", "JPEG"),
        png_alpha_required=False,
        min_dimension=320,
        max_dimension=3840,
        max_aspect_ratio=2.0,
        portrait_warning=True,
    ),
    AssetRule(
        group="screenshots",
        label="QR pairing screenshot",
        relative_path="screenshots/en-US/03-qr-pairing.png",
        allowed_formats=("PNG", "JPEG"),
        png_alpha_required=False,
        min_dimension=320,
        max_dimension=3840,
        max_aspect_ratio=2.0,
        portrait_warning=True,
    ),
    AssetRule(
        group="screenshots",
        label="Language settings screenshot",
        relative_path="screenshots/en-US/04-language-settings.png",
        allowed_formats=("PNG", "JPEG"),
        png_alpha_required=False,
        min_dimension=320,
        max_dimension=3840,
        max_aspect_ratio=2.0,
        portrait_warning=True,
    ),
]


def parse_png(path: Path) -> ImageInfo:
    with path.open("rb") as handle:
        header = handle.read(33)
    if len(header) < 33 or not header.startswith(PNG_SIGNATURE):
        raise ValueError("not a valid PNG signature or IHDR header")

    ihdr_length = struct.unpack(">I", header[8:12])[0]
    chunk_type = header[12:16]
    if chunk_type != b"IHDR" or ihdr_length != 13:
        raise ValueError("missing valid IHDR chunk")

    width, height = struct.unpack(">II", header[16:24])
    bit_depth = header[24]
    color_type = header[25]
    if color_type in (4, 6):
        has_alpha = True
    elif color_type in (0, 2, 3):
        has_alpha = False
    else:
        raise ValueError(f"unsupported PNG color type {color_type}")

    return ImageInfo(
        format="PNG",
        width=width,
        height=height,
        has_alpha=has_alpha,
        details=f"bit_depth={bit_depth}, color_type={color_type}",
    )


def read_marker(handle) -> int | None:
    byte = handle.read(1)
    while byte and byte != b"\xff":
        byte = handle.read(1)
    if not byte:
        return None

    marker = handle.read(1)
    while marker == b"\xff":
        marker = handle.read(1)
    if not marker:
        return None
    return marker[0]


def parse_jpeg(path: Path) -> ImageInfo:
    with path.open("rb") as handle:
        if handle.read(2) != b"\xff\xd8":
            raise ValueError("not a valid JPEG SOI marker")

        while True:
            marker = read_marker(handle)
            if marker is None:
                break
            if marker == 0xD9:
                break
            if marker == 0x01 or 0xD0 <= marker <= 0xD7:
                continue

            length_bytes = handle.read(2)
            if len(length_bytes) != 2:
                raise ValueError("truncated JPEG segment length")
            segment_length = struct.unpack(">H", length_bytes)[0]
            if segment_length < 2:
                raise ValueError("invalid JPEG segment length")

            sof_markers = {
                0xC0,
                0xC1,
                0xC2,
                0xC3,
                0xC5,
                0xC6,
                0xC7,
                0xC9,
                0xCA,
                0xCB,
                0xCD,
                0xCE,
                0xCF,
            }
            if marker in sof_markers:
                data = handle.read(segment_length - 2)
                if len(data) < 5:
                    raise ValueError("truncated JPEG SOF segment")
                precision = data[0]
                height, width = struct.unpack(">HH", data[1:5])
                return ImageInfo(
                    format="JPEG",
                    width=width,
                    height=height,
                    has_alpha=False,
                    details=f"sof=0x{marker:02X}, precision={precision}",
                )

            handle.seek(segment_length - 2, 1)

    raise ValueError("JPEG SOF marker not found")


def parse_image(path: Path) -> ImageInfo:
    with path.open("rb") as handle:
        signature = handle.read(8)
    if signature.startswith(PNG_SIGNATURE):
        return parse_png(path)
    if signature.startswith(b"\xff\xd8"):
        return parse_jpeg(path)
    raise ValueError("unsupported image format")


def validate_rule(root: Path, rule: AssetRule, allow_missing: bool) -> Result:
    path = root / rule.relative_path
    if not path.exists():
        reason = "file is missing"
        if allow_missing:
            reason += " (allowed by --allow-missing)"
        return Result("MISSING", path, "-", "-", reason)

    try:
        info = parse_image(path)
    except OSError as exc:
        return Result("ERROR", path, "-", "-", f"could not read file: {exc}")
    except ValueError as exc:
        return Result("ERROR", path, "-", "-", str(exc))

    dimensions = f"{info.width}x{info.height}"
    errors: list[str] = []
    warnings: list[str] = []

    if info.format not in rule.allowed_formats:
        errors.append(f"format {info.format} is not allowed")

    if rule.exact_size and (info.width, info.height) != rule.exact_size:
        expected_width, expected_height = rule.exact_size
        errors.append(f"expected {expected_width}x{expected_height}")

    if rule.max_bytes is not None:
        size = path.stat().st_size
        if size > rule.max_bytes:
            errors.append(f"file size {size} exceeds {rule.max_bytes} bytes")

    if info.format == "PNG" and rule.png_alpha_required is not None:
        if rule.png_alpha_required and not info.has_alpha:
            errors.append("PNG must include alpha")
        if not rule.png_alpha_required and info.has_alpha:
            errors.append("PNG must not include alpha")

    minimum = min(info.width, info.height)
    maximum = max(info.width, info.height)
    if rule.min_dimension is not None and minimum < rule.min_dimension:
        errors.append(f"minimum dimension {minimum} is below {rule.min_dimension}")
    if rule.max_dimension is not None and maximum > rule.max_dimension:
        errors.append(f"maximum dimension {maximum} exceeds {rule.max_dimension}")
    if rule.max_aspect_ratio is not None and maximum > minimum * rule.max_aspect_ratio:
        errors.append(
            f"max dimension {maximum} is more than {rule.max_aspect_ratio:g}x min dimension {minimum}"
        )
    if rule.portrait_warning and info.height <= info.width:
        warnings.append("screenshot is not portrait")

    if errors:
        return Result("ERROR", path, info.format, dimensions, "; ".join(errors))
    if warnings:
        return Result("WARNING", path, info.format, dimensions, "; ".join(warnings))
    return Result("OK", path, info.format, dimensions, info.details)


def print_group(group: str, results: Iterable[Result]) -> None:
    print(f"\n{group}")
    print("-" * len(group))
    for result in results:
        print(f"{result.status}: {result.path}")
        print(f"  format: {result.image_format}")
        print(f"  dimensions: {result.dimensions}")
        print(f"  reason: {result.reason}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate Local Find Google Play store assets.")
    parser.add_argument(
        "--root",
        default="store-assets/google-play",
        help="asset root directory (default: store-assets/google-play)",
    )
    parser.add_argument(
        "--allow-missing",
        action="store_true",
        help="treat missing expected files as warnings for pre-asset planning",
    )
    args = parser.parse_args(argv)

    root = Path(args.root)
    results = [validate_rule(root, rule, args.allow_missing) for rule in ASSET_RULES]

    for group in ("icon", "feature graphic", "screenshots"):
        print_group(group, [result for rule, result in zip(ASSET_RULES, results) if rule.group == group])

    checked_count = len(results)
    ok_count = sum(1 for result in results if result.status == "OK")
    warning_count = sum(1 for result in results if result.status == "WARNING")
    missing_count = sum(1 for result in results if result.status == "MISSING")
    error_count = sum(1 for result in results if result.status == "ERROR")

    print("\nsummary")
    print("-------")
    print(f"checked count: {checked_count}")
    print(f"ok count: {ok_count}")
    print(f"warning count: {warning_count}")
    print(f"missing count: {missing_count}")
    print(f"error count: {error_count}")

    if error_count > 0:
        return 1
    if missing_count > 0 and not args.allow_missing:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
