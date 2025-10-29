import json
import os
import hashlib
import sys

# --- Configuration ---
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STORIES_JSON_PATH = os.path.join(SCRIPT_DIR, 'app', 'src', 'main', 'assets', 'stories.json')

def get_text_to_hash(item):
    """Extracts the core dialogue text to be hashed."""
    return item.get('text', '')

def rename_original_files_to_hash(audio_dir):
    """
    Reads stories.json to determine the original filename and the new hash-based filename,
    then performs the rename operation.
    This script is intended to be run ONCE after re-importing original audio files.
    """
    if not os.path.isdir(audio_dir):
        print(f"ERROR: Audio directory not found at '{audio_dir}'")
        print("Please provide a valid path to your audio files.")
        return

    try:
        with open(STORIES_JSON_PATH, 'r', encoding='utf-8') as f:
            stories_data = json.load(f)
    except FileNotFoundError:
        print(f"ERROR: stories.json not found at {STORIES_JSON_PATH}")
        return
    except json.JSONDecodeError as e:
        print(f"ERROR: Could not decode JSON from {STORIES_JSON_PATH}. Details: {e}")
        return

    print("Starting rename process...")
    files_renamed = 0
    files_not_found = 0
    already_exist = 0

    # --- Iterate through all stories and all dialogue items ---
    for story in stories_data:
        story_id = story.get('id', 'unknown_story')

        # 1. Process Dialogue in Acts
        acts = {
            "act1Setup": story.get("act1Setup"),
            "act2Core": story.get("act2Core"),
            "act3Aftermath": story.get("act3Aftermath"),
            "aftercareScript": story.get("aftercareScript")
        }

        for act_name, act_content in acts.items():
            if not act_content: continue
            for i, step in enumerate(act_content):
                if step.get("type") == "dialogue":
                    speaker = step.get('speaker', 'NARRATOR').replace('{', '').replace('}', '').replace(' ', '_')
                    original_filename = f"{story_id}_{act_name}_{i}_{speaker}.mp3"
                    
                    text_to_hash = get_text_to_hash(step)
                    hash_hex = hashlib.sha256(text_to_hash.encode('utf-8')).hexdigest()
                    new_filename = f"{hash_hex}.mp3"

                    original_filepath = os.path.join(audio_dir, original_filename)
                    new_filepath = os.path.join(audio_dir, new_filename)

                    if os.path.exists(original_filepath):
                        if not os.path.exists(new_filepath):
                            os.rename(original_filepath, new_filepath)
                            files_renamed += 1
                        else:
                            already_exist += 1 # New file already exists, old one will be left alone
                    elif not os.path.exists(new_filepath):
                        files_not_found += 1
        
        # 2. Process Command Library
        library = story.get("commandLibrary", {})
        for key, value in library.items():
            if not value: continue
            
            if isinstance(value, list):
                for i, command in enumerate(value):
                    original_filename = f"{story_id}_lib_{key}_{i}.mp3"
                    text_to_hash = get_text_to_hash(command)
                    hash_hex = hashlib.sha256(text_to_hash.encode('utf-8')).hexdigest()
                    new_filename = f"{hash_hex}.mp3"

                    original_filepath = os.path.join(audio_dir, original_filename)
                    new_filepath = os.path.join(audio_dir, new_filename)

                    if os.path.exists(original_filepath):
                        if not os.path.exists(new_filepath):
                            os.rename(original_filepath, new_filepath)
                            files_renamed += 1
                        else:
                            already_exist += 1
                    elif not os.path.exists(new_filepath):
                        files_not_found += 1

            elif isinstance(value, dict):
                for sub_key, sub_list in value.items():
                    sub_key_sanitized = sub_key.replace(' ', '_')
                    for i, command in enumerate(sub_list):
                        original_filename = f"{story_id}_lib_{key}_{sub_key_sanitized}_{i}.mp3"
                        text_to_hash = get_text_to_hash(command)
                        hash_hex = hashlib.sha256(text_to_hash.encode('utf-8')).hexdigest()
                        new_filename = f"{hash_hex}.mp3"

                        original_filepath = os.path.join(audio_dir, original_filename)
                        new_filepath = os.path.join(audio_dir, new_filename)

                        if os.path.exists(original_filepath):
                            if not os.path.exists(new_filepath):
                                os.rename(original_filepath, new_filepath)
                                files_renamed += 1
                            else:
                                already_exist += 1
                        elif not os.path.exists(new_filepath):
                            files_not_found += 1
    
    print("\n--- Rename Process Complete ---")
    print(f"{files_renamed} files were successfully renamed to their hash.")
    print(f"{files_not_found} original files were not found (and their hash equivalent also did not exist). This is expected for missing audio.")
    print(f"{already_exist} files were skipped because a file with the target hash name already existed.")
    print("\nThis script has done its job. You can now delete it.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python rename_audio_files.py <path_to_audio_directory>")
        print("Example: python rename_audio_files.py app/src/main/assets/audio")
        sys.exit(1)

    audio_directory = sys.argv[1]
    rename_original_files_to_hash(audio_directory)
