# universal_agent_validator.py
import json
import re


def validate_agent_output(output, expected_schema=None):
    issues = []

    # 1. كشف الـ mock data
    mock_keywords = ['mock', 'test', 'dummy', 'example', 'TODO', 'placeholder']
    for kw in mock_keywords:
        if kw in str(output).lower():
            issues.append(f"⚠️ Possible mock data found: '{kw}'")

    # 2. كشف الـ static data
    if len(set(str(output).split())) < 10 and len(str(output)) < 200:
        issues.append("⚠️ Output suspiciously static/repetitive")

    # 3. كشف الـ spurious data (أرقام عشوائية أو نصوص غريبة)
    spurious_patterns = [r'\b\d{10,}\b', r'[^\w\s]{5,}']
    for pattern in spurious_patterns:
        if re.search(pattern, str(output)):
            issues.append("⚠️ Spurious/unexpected data pattern detected")

    # 4. فحص completeness
    if len(str(output).strip()) < 50:
        issues.append("❌ Output too short - likely incomplete")

    return {
        "valid": len(issues) == 0,
        "issues": issues,
        "confidence": 0 if issues else 100
    }
