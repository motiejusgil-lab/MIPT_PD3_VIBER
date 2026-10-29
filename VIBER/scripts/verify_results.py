"""Check saved JUnit/JaCoCo evidence; uses only Python's standard library."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]


def junit_summary(pattern):
    paths = sorted(ROOT.glob(pattern))
    if not paths:
        raise ValueError("Missing test results: " + pattern)
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    cases = []
    for path in paths:
        root = ET.parse(path).getroot()
        suites = [root] if root.tag == "testsuite" else root.findall(".//testsuite")
        for suite in suites:
            for key in totals:
                totals[key] += int(suite.get(key, "0"))
            cases.extend(case.get("name") for case in suite.findall("testcase"))
    if not totals["tests"] or any(totals[key] for key in ("failures", "errors", "skipped")):
        raise ValueError("Tests must all pass without skips: " + str(totals))
    return {**totals, "cases": cases, "files": [str(p.relative_to(ROOT)) for p in paths]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--unit-only", action="store_true")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    coverage_path = ROOT / "build/reports/jacoco/unitTestCoverage/unitTestCoverage.xml"
    coverage = ET.parse(coverage_path).getroot()
    classes = {node.get("name") for node in coverage.findall(".//class")}
    expected = {
        "lt/mipt/wordcounter/MainActivity",
        "lt/mipt/wordcounter/MainActivity$1",
        "lt/mipt/wordcounter/core/TextAnalyzer",
        "lt/mipt/wordcounter/core/TextMetrics",
        "lt/mipt/wordcounter/core/TextPreview",
    }
    if classes != expected:
        raise ValueError("Unexpected coverage class scope: " + str(sorted(classes)))
    counters = {node.get("type"): {key: int(node.get(key)) for key in ("missed", "covered")}
                for node in coverage.findall("counter")}
    for counter in ("CLASS", "METHOD"):
        if counters[counter]["missed"] or not counters[counter]["covered"]:
            raise ValueError("100% " + counter + " coverage required: " + str(counters[counter]))
    result = {
        "core": junit_summary("core/build/test-results/test/TEST-*.xml"),
        "app_unit": junit_summary("app/build/test-results/testDebugUnitTest/TEST-*.xml"),
        "coverage": counters,
        "classes": sorted(classes),
    }
    if not args.unit_only:
        result["ui"] = junit_summary("app/build/outputs/androidTest-results/connected/debug/TEST-*.xml")
        if len(set(result["ui"]["cases"])) < 4:
            raise ValueError("At least four distinct device UI scenarios are required")
    rendered = json.dumps(result, indent=2, ensure_ascii=False)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered + "\n", encoding="utf-8")
    print(rendered)


if __name__ == "__main__":
    main()
