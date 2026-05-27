import json

transcript_path = r'C:\Users\Majagatha\.gemini\antigravity\brain\aed1b721-08ec-4e09-a0a9-c98334a1fb2f\.system_generated\logs\transcript.jsonl'
edits = []
with open(transcript_path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            if 'tool_calls' in data:
                for tc in data['tool_calls']:
                    if tc['tool'] in ['default_api:replace_file_content', 'default_api:multi_replace_file_content']:
                        if 'MainActivity.kt' in tc['arguments'].get('TargetFile', ''):
                            edits.append({
                                'step': data['step_index'],
                                'tool': tc['tool'],
                                'args': tc['arguments']
                            })
        except Exception:
            pass

print(f"Found {len(edits)} edits to MainActivity.kt.")
if edits:
    first_edit = edits[0]
    print(f"First edit step: {first_edit['step']}, tool: {first_edit['tool']}")
    if 'ReplacementChunks' in first_edit['args']:
        for chunk in first_edit['args']['ReplacementChunks']:
            print(f"StartLine: {chunk.get('StartLine')}, EndLine: {chunk.get('EndLine')}")
    else:
        print(f"StartLine: {first_edit['args'].get('StartLine')}, EndLine: {first_edit['args'].get('EndLine')}")
