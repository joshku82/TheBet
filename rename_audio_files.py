import os

# --- Configuration ---
PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
AUDIO_DIR = os.path.join(PROJECT_ROOT, 'app', 'src', 'main', 'assets', 'audio')

def rename_double_extension_files():
    """Renames files in the audio directory that end with .mp3.mp3"""
    if not os.path.isdir(AUDIO_DIR):
        print(f"ERROR: Audio directory not found at {AUDIO_DIR}")
        return

    renamed_count = 0
    for filename in os.listdir(AUDIO_DIR):
        if filename.endswith(".mp3.mp3"):
            old_path = os.path.join(AUDIO_DIR, filename)
            new_filename = filename[:-4] # Remove the extra .mp3
            new_path = os.path.join(AUDIO_DIR, new_filename)
            
            try:
                os.rename(old_path, new_path)
                print(f"Renamed: {filename} -> {new_filename}")
                renamed_count += 1
            except OSError as e:
                print(f"ERROR renaming {filename}: {e}")

    if renamed_count == 0:
        print("No files with .mp3.mp3 extension found.")
    else:
        print(f"\nFinished renaming {renamed_count} files.")

if __name__ == "__main__":
    rename_double_extension_files()
