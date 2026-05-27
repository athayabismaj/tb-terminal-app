import json
import re

transcript_path = r'C:\Users\Majagatha\.gemini\antigravity\brain\aed1b721-08ec-4e09-a0a9-c98334a1fb2f\.system_generated\logs\transcript.jsonl'
with open(transcript_path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            if 'tool_calls' in data:
                for tc in data['tool_calls']:
                    if tc['tool'] == 'default_api:run_command':
                        arg_str = tc['arguments'].get('CommandLine', '')
                        if 'MainActivity.kt' in arg_str:
                            print(f"Step {data['step_index']}: {arg_str[:100]}")
        except Exception:
            pass
