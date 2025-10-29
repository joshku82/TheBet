import json
import os
import hashlib

# --- Configuration ---
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STORIES_JSON_PATH = os.path.join(SCRIPT_DIR, 'app', 'src', 'main', 'assets', 'stories.json')
AUDIO_DIR = os.path.join(SCRIPT_DIR, 'app', 'src', 'main', 'assets', 'audio')
OUTPUT_FILE = "audio_generation_list.txt"

def get_existing_audio_files():
    """Scans the audio directory and returns a set of existing filenames."""
    if not os.path.isdir(AUDIO_DIR):
        print(f"Warning: Audio directory not found at {AUDIO_DIR}. Assuming no files exist.")
        return set()
    return set(os.listdir(AUDIO_DIR))

def get_text_to_hash(item):
    """Extracts the core dialogue text to be hashed. MUST BE IDENTICAL to the rename script."""
    return item.get('text', '')

def construct_display_line(item):
    """Constructs the human-readable text line for the output file."""
    text = item.get('text', '')
    speaker = item.get('speaker', 'NARRATOR') 
    emotion = item.get('emotion')
    
    line = f"[{speaker}: '{text}']"
    if emotion:
        line = f"[{emotion}] {line}"
    return line

def generate_full_audio_manifest():
    """
    Parses stories.json and generates a COMPLETE, structured manifest of ALL audio files,
    marking those that already exist. Uses the hash-based naming convention.
    """
    try:
        with open(STORIES_JSON_PATH, 'r', encoding='utf-8') as f:
            stories_data = json.load(f)
    except FileNotFoundError:
        return f"ERROR: stories.json not found at {STORIES_JSON_PATH}"
    except json.JSONDecodeError as e:
        return f"ERROR: Could not decode JSON from {STORIES_JSON_PATH}. Details: {e}"

    existing_files = get_existing_audio_files()
    output_sections = []

    for story in stories_data:
        story_id = story.get('id', 'unknown_story')
        story_output = []

        story_output.append(f"====================================================================")
        story_output.append(f"STORY: {story_id} - {story.get('title', 'No Title')}")
        story_output.append(f"====================================================================\n")

        # --- Process Dialogue in Acts ---
        acts_output = []
        acts = {
            "act1Setup": story.get("act1Setup"),
            "act2Core": story.get("act2Core"),
            "act3Aftermath": story.get("act3Aftermath"),
            "aftercareScript": story.get("aftercareScript")
        }

        for act_name, act_content in acts.items():
            if not act_content: continue
            act_lines = []
            for step in act_content:
                if step.get("type") == "dialogue":
                    text_to_hash = get_text_to_hash(step)
                    hash_hex = hashlib.sha256(text_to_hash.encode('utf-8')).hexdigest()
                    filename = f"{hash_hex}.mp3"
                    
                    exists_marker = " * (exists)" if filename in existing_files else ""
                    display_line = construct_display_line(step)
                    act_lines.append(f"    {filename}: {display_line}{exists_marker}")
            
            if act_lines:
                acts_output.append(f"  ## {act_name}:")
                acts_output.extend(act_lines)
                acts_output.append("")
        
        # --- Process Command Library ---
        library_output = []
        library = story.get("commandLibrary", {})
        for key, value in library.items():
            if not value: continue
            key_lines = []
            
            items_to_process = []
            if isinstance(value, list):
                items_to_process.extend(value)
            elif isinstance(value, dict):
                # This handles nested dictionaries of lists, like in 'toy_use'
                for sub_list in value.values():
                    items_to_process.extend(sub_list)

            for command in items_to_process:
                text_to_hash = get_text_to_hash(command)
                hash_hex = hashlib.sha256(text_to_hash.encode('utf-8')).hexdigest()
                filename = f"{hash_hex}.mp3"

                exists_marker = " * (exists)" if filename in existing_files else ""
                display_line = construct_display_line(command)
                key_lines.append(f"    {filename}: {display_line}{exists_marker}")

            if key_lines:
                library_output.append(f"  ## {key}:")
                library_output.extend(key_lines)
                library_output.append("")

        # --- Assemble Story Output ---
        if acts_output:
            story_output.append("--- Dialogue Lines by Act ---")
            story_output.extend(acts_output)
        if library_output:
            story_output.append("--- Command Library ---")
            story_output.extend(library_output)
        
        output_sections.extend(story_output)

    # --- Write to File ---
    try:
        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            f.write('\n'.join(output_sections))
            return f"Successfully generated full audio manifest: {OUTPUT_FILE}"
    except IOError as e:
        return f"ERROR: Could not write to output file {OUTPUT_FILE}. Reason: {e}"

if __name__ == "__main__":
    result = generate_full_audio_manifest()
    print(result)
