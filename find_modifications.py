import json
import os

transcript_path = r'C:\Users\Majagatha\.gemini\antigravity\brain\aed1b721-08ec-4e09-a0a9-c98334a1fb2f\.system_generated\logs\transcript.jsonl'
with open(transcript_path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            if 'tool_calls' in data:
                for tc in data['tool_calls']:
                    if tc['tool'] in ['write_to_file', 'replace_file_content', 'multi_replace_file_content', 'run_command']:
                        arg_str = json.dumps(tc['arguments'])
                        if 'MainActivity.kt' in arg_str:
                            if tc['tool'] != 'run_command':
                                print(f"Step {data['step_index']}: {tc['tool']} on MainActivity.kt")
                            elif 'Add-Content' in arg_str or 'sed' in arg_str or 'echo' in arg_str:
                                print(f"Step {data['step_index']}: {tc['tool']} modifying MainActivity.kt")
        except Exception:
            pass
