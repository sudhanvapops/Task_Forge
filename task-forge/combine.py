from pathlib import Path

# ==========================
# USER INPUT
# ==========================

SOURCE_FOLDER = input("Enter the source folder path: ").strip()
OUTPUT_FILE = input("Enter the output file path (e.g., C:\\combined.txt): ").strip()

source = Path(SOURCE_FOLDER).resolve()
output_path = Path(OUTPUT_FILE).resolve()

# ==========================
# FILE TYPES TO INCLUDE
# ==========================

EXTENSIONS = {
    ".txt",
    ".md",
    ".py",
    ".java",
    ".kt",
    ".kts",
    ".js",
    ".jsx",
    ".ts",
    ".tsx",
    ".html",
    ".css",
    ".scss",
    ".sass",
    ".json",
    ".xml",
    ".yaml",
    ".yml",
    ".properties",
    ".gradle",
    ".sql",
    ".c",
    ".cpp",
    ".cc",
    ".h",
    ".hpp",
    ".cs",
    ".go",
    ".rs",
    ".php",
    ".rb",
    ".swift",
    ".dart",
    ".sh",
    ".bat",
    ".ps1",
    ".env",
    ".gitignore"
}

# ==========================
# FOLDERS TO IGNORE
# ==========================

IGNORE_DIRS = {
    # Git
    ".git",

    # Node.js
    "node_modules",
    ".next",
    ".npm",
    ".pnpm-store",
    ".turbo",

    # React / Vite
    "dist",
    "build",
    "coverage",

    # Python
    "__pycache__",
    ".venv",
    "venv",
    "env",
    ".pytest_cache",
    ".mypy_cache",

    # Java / Spring Boot
    "target",
    ".gradle",
    "out",
    "bin",

    # Maven wrapper
    ".mvn",

    # IDEs
    ".idea",
    ".vscode",

    # OS
    ".DS_Store"
}

print("\nScanning files...\n")

files_processed = 0
files_skipped = 0

with open(output_path, "w", encoding="utf-8") as outfile:

    for file in sorted(source.rglob("*")):

        # Skip ignored folders
        if any(part in IGNORE_DIRS for part in file.parts):
            continue

        # Skip output file
        if file.resolve() == output_path:
            continue

        # Skip non-files
        if not file.is_file():
            continue

        # Skip unwanted extensions
        if file.suffix.lower() not in EXTENSIONS:
            continue

        print(f"Reading: {file.relative_to(source)}")

        outfile.write("=" * 100 + "\n")
        outfile.write(f"FILE: {file.relative_to(source)}\n")
        outfile.write("=" * 100 + "\n\n")

        try:
            with open(file, "r", encoding="utf-8") as infile:
                outfile.write(infile.read())
                files_processed += 1

        except Exception as e:
            outfile.write(f"[Could not read file: {e}]")
            files_skipped += 1

        outfile.write("\n\n")

print("\n" + "=" * 50)
print("Completed!")
print("=" * 50)
print(f"Files merged   : {files_processed}")
print(f"Files skipped  : {files_skipped}")
print(f"Output saved to: {output_path}")