import json
import os
import re

# --- Configuration ---
# Get the directory where the script is located
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
# Build the path to stories.json relative to the script's location
STORIES_JSON_PATH = os.path.join(SCRIPT_DIR, 'app', 'src', 'main', 'assets', 'stories.json')
OUTPUT_FILE = "audio_generation_list.txt"

def generate_audio_script():
    """
    Parses the stories.json file and generates a structured text file
    listing all dialogue lines and their corresponding MP3 filenames.
    """
    try:
        with open(STORIES_JSON_PATH, 'r', encoding='utf-8') as f:
            stories_data = json.load(f)
    except FileNotFoundError:
        return f"ERROR: stories.json not found at {STORIES_JSON_PATH}"
    except json.JSONDecodeError:
        return f"ERROR: Could not decode JSON from {STORIES_JSON_PATH}"

    output_lines = []

    for story in stories_data:
        story_id = story.get('id', 'unknown_story')
        output_lines.append(f"====================================================================")
        output_lines.append(f"STORY: {story_id} - {story.get('title', 'No Title')}")
        output_lines.append(f"====================================================================\n")

        # 1. Process Dialogue in Acts
        output_lines.append(f"--- Dialogue Lines by Act ---\n")
        acts = {
            "act1Setup": story.get("act1Setup"),
            "act2Core": story.get("act2Core"),
            "act3Aftermath": story.get("act3Aftermath"),
            "aftercareScript": story.get("aftercareScript")
        }

        for act_name, act_content in acts.items():
            if not act_content:
                continue
            
            output_lines.append(f"  ## {act_name}:")
            
            for local_index, step in enumerate(act_content):
                if step.get("type") == "dialogue":
                    speaker = step.get('speaker', 'NO_SPEAKER').replace(' ', '_')
                    text = step.get('text', '')
                    
                    filename = f"{story_id}_{act_name}_{local_index}_{speaker}.mp3"
                    output_lines.append(f'    {filename}: "{text}"')
            output_lines.append("") # Add a blank line for readability

        # 2. Process Command Library
        output_lines.append(f"\n--- Command Library ---\n")
        library = story.get("commandLibrary", {})
        if not library:
            output_lines.append("  No command library found for this story.\n")
            continue

        for key, value in library.items():
            if not value:
                continue
            
            output_lines.append(f"  ## {key}:")
            # Handle simple lists of commands
            if isinstance(value, list):
                for i, command in enumerate(value):
                    text = command.get('text', '')
                    filename = f"{story_id}_lib_{key}_{i}.mp3"
                    output_lines.append(f'    {filename}: "{text}"')
            
            # Handle map-based lists of commands (like 'toy_use' or 'kinkActions')
            elif isinstance(value, dict):
                for sub_key, sub_list in value.items():
                    output_lines.append(f"    ### {sub_key}:")
                    for i, command in enumerate(sub_list):
                        text = command.get('text', '')
                        filename = f"{story_id}_lib_{key}_{sub_key.replace(' ', '_')}_{i}.mp3"
                        output_lines.append(f'      {filename}: "{text}"')
            output_lines.append("") # Add a blank line

        output_lines.append("\n")

    # Write to output file
    try:
        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            f.write('\n'.join(output_lines))
        return f"Successfully generated audio list to: {OUTPUT_FILE}"
    except IOError as e:
        return f"ERROR: Could not write to output file {OUTPUT_FILE}. Reason: {e}"


if __name__ == "__main__":
    result = generate_audio_script()
    print(result)
