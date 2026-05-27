import json

transcript_path = r'C:\Users\Majagatha\.gemini\antigravity\brain\aed1b721-08ec-4e09-a0a9-c98334a1fb2f\.system_generated\logs\transcript.jsonl'
with open(transcript_path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            if 'tool_calls' in data:
                for tc in data['tool_calls']:
                    if 'MainActivity.kt' in tc['arguments'].get('TargetFile', ''):
                        print(f"Step {data['step_index']}: tool = {tc['tool']}")
        except Exception:
            pass
