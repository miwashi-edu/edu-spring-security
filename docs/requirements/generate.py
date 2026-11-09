#!/usr/bin/env python3
"""
Regenerates the "Tested by" section of every REQ-nnn.md and the matrix in README.md
from the @Tag("REQ-nnn") and @RunOnProfiles annotations in the tests.

Also creates/updates TEST-MATRIX.md, the student worksheet (code, caption, requirement and an
empty "Tested in" column). Whatever is written in "Tested in" is kept when regenerating.

Run from the repository root after adding or changing tests:

    python3 docs/requirements/generate.py

Everything above "## Tested by" in REQ-nnn.md is hand-written and kept as is.
"""
import glob
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DOCS = os.path.join(ROOT, 'docs', 'requirements')
SUITES = {'unit': 'Unit', 'smoke': 'Smoke', 'security': 'Security'}
ORDER = ['Unit', 'Smoke', 'Security']
UNIT_PROFILES = {'DevProfileTest': '`dev`', 'StageProfileTest': '`stage`',
                 'ProdProfileTest': '`prod`', 'DefaultProfileTest': 'none'}
# Requirements enforced by suite configuration instead of a test of their own
ENFORCED = {
    'REQ-007': ('Smoke', '⚙ all smoke tests',
                'No automated test: verified by running the smoke suite with `TEST_PROFILE=prod`.\n'
                'Enforced by `@RunOnProfiles({"dev", "stage"})` on every smoke test except '
                '`HealthEndpointTest.shouldReportUP`.'),
    'REQ-038': ('Security', '⚙ all security tests',
                'No automated test: verified by running the security suite with `TEST_PROFILE=prod`.\n'
                'Enforced by `@RunOnProfiles({"dev", "stage"})` on every security test class except '
                '`ProductionAccountsTest` (`@RunOnProfiles("prod")`).'),
}


def scan_tests():
    """rid -> [(suite, class, method, profiles, path)]"""
    tests = {}
    for path in sorted(glob.glob(os.path.join(ROOT, 'app/src/test/*/java/org/example/test/*Test.java'))):
        suite = path.split(os.sep)[-6]
        if suite not in SUITES:
            continue
        src = open(path).read()
        lines = src.split('\n')
        cls = os.path.basename(path)[:-5]
        m = re.search(r'@RunOnProfiles\(([^)]*)\)\s*\npublic class', src)
        class_profiles = re.findall(r'"(\w+)"', m.group(1)) if m else None
        for i, line in enumerate(lines):
            method = re.match(r'\s+void (\w+)\(', line)
            if not method:
                continue
            # annotations directly above the method (stop at blank line, '}' or end of Javadoc)
            j, block = i - 1, []
            while j >= 0 and lines[j].strip() not in ('', '}') and not lines[j].strip().endswith('*/'):
                block.append(lines[j])
                j -= 1
            annotations = '\n'.join(block)
            pm = re.search(r'@RunOnProfiles\(([^)]*)\)', annotations)
            profiles = re.findall(r'"(\w+)"', pm.group(1)) if pm else class_profiles
            shown = ', '.join(f'`{p}`' for p in profiles) if profiles else 'all'
            if suite == 'unit':
                shown = UNIT_PROFILES.get(cls, shown)
            rel = os.path.relpath(path, DOCS)
            for rid in re.findall(r'@Tag\("(REQ-\d+)"\)', annotations):
                tests.setdefault(rid, []).append((SUITES[suite], cls, method.group(1), shown, rel))
    return tests


def update_requirement_files(tests):
    titles = {}
    for path in sorted(glob.glob(os.path.join(DOCS, 'REQ-*.md'))):
        rid = os.path.basename(path)[:-3]
        text = open(path).read()
        titles[rid] = re.match(r'# REQ-\d+: (.*)', text).group(1)
        if rid in ENFORCED:
            body = ENFORCED[rid][2] + '\n'
        elif rid in tests:
            body = '| Suite | Test class | Test | Profile |\n|---|---|---|---|\n' + '\n'.join(
                f'| {s} | [`{c}`]({p}) | `{m}` | {pr} |' for s, c, m, pr, p in tests[rid]) + '\n'
        else:
            body = '*Not tested yet.*\n'
        open(path, 'w').write(text.split('## Tested by')[0].rstrip() + '\n\n## Tested by\n\n' + body)
    return titles


def update_readme(tests, titles):
    def cell(rid, suite):
        seen = []
        for s, c, m, pr, p in tests.get(rid, []):
            if s == suite and c not in [x[0] for x in seen]:
                seen.append((c, p))
        if rid in ENFORCED and ENFORCED[rid][0] == suite:
            return ENFORCED[rid][1]
        return '<br>'.join(f'[{c.replace("Test", "")}]({p})' for c, p in seen)

    rows, detail = [], []
    for rid, title in titles.items():
        count = len(tests.get(rid, []))
        rows.append(f'| [{rid}]({rid}.md) | {title} | ' + ' | '.join(cell(rid, s) for s in ORDER)
                    + f' | {count or "–"} |')
        for s, c, m, pr, p in tests.get(rid, []):
            detail.append(f'| [{rid}]({rid}.md) | {s} | `{c}.{m}` | {pr} |')
        if rid in ENFORCED:
            detail.append(f'| [{rid}]({rid}.md) | – | *enforced by `@RunOnProfiles`, verified with `TEST_PROFILE=prod`* | |')
    untested = [r for r in titles if r not in tests and r not in ENFORCED]
    coverage = 'Not tested yet: ' + ', '.join(untested) if untested else 'Every requirement is covered.'

    readme = os.path.join(DOCS, 'README.md')
    text = open(readme).read()
    start, end = text.index('## Test matrix'), text.index('## Test suites')
    generated = f'''## Test matrix

Where every requirement is tested (generated by `python3 docs/requirements/generate.py`).
⚙ = enforced by the suite configuration (`@RunOnProfiles`), not by a test of its own.
*Tests* = number of test methods tagged with the requirement.

| Id | Requirement | Unit | Smoke | Security | Tests |
|---|---|---|---|---|:-:|
{chr(10).join(rows)}

{coverage}

## Tests per requirement

| Requirement | Suite | Test | Profile |
|---|---|---|---|
{chr(10).join(detail)}

'''
    open(readme, 'w').write(text[:start] + generated + text[end:])
    return untested


def statement(rid):
    """The one-line requirement: the quote ('> ...') under the title, joined into one line."""
    lines = open(os.path.join(DOCS, rid + '.md')).read().split('\n')
    quote = [l[1:].strip() for l in lines if l.startswith('>')]
    return ' '.join(quote).replace('|', '\\|')


def suggested(rid):
    """'| **Suggested test** | ... |' from the requirement's test case table; one suggestion per line."""
    m = re.search(r'^\| \*\*Suggested test\*\* \| (.*) \|$', open(os.path.join(DOCS, rid + '.md')).read(), re.M)
    return m.group(1).replace(', ', '<br>') if m else ''


def update_student_matrix(titles):
    path = os.path.join(DOCS, 'TEST-MATRIX.md')
    kept = {}
    if os.path.exists(path):  # keep what students wrote in "Tested in"
        for line in open(path).read().split('\n'):
            m = re.match(r'\| \[(REQ-\d+)\]\(REQ-\d+\.md\) \|.*\|(.*)\|\s*$', line)
            if m:
                kept[m.group(1)] = m.group(2).strip()
    rows = '\n'.join(f'| [{rid}]({rid}.md) | {title} | {statement(rid)} | {suggested(rid)} | {kept.get(rid, "")} |'
                      for rid, title in titles.items())
    open(path, 'w').write(f'''# Test matrix

All requirements on one page. For each requirement, write in **Tested in** where QA tests it:
the suite (*Unit*, *Smoke*, *Security*, *Integration*) and the test class, e.g.
`Smoke: HealthEndpointTest`. Write *manual* if it is only checked by hand, and leave it empty if
it is not tested at all; those are the gaps.

**Suggested test** is where the requirement is best tested; the first suggestion is the main one,
a second one complements it. Do the tests QA actually runs match the suggestion?

| Type | Knows the code? | Group | Tests that … |
|---|---|---|---|
| White-box | yes, runs inside the application | Unit | one class or rule in isolation (validator, token service) |
| White-box | yes | Configuration | the application is configured right per profile (exposed endpoints, defaults) |
| Black-box | no, only HTTP against a running server | Smoke | the deployed server is up and basically works; fast, run after every deploy |
| Black-box | no | Integration | features work end to end (login, refresh, logout with real storage) |
| Black-box | no | Security | the server resists misuse and attacks (forged tokens, CSRF, brute force) |
| – | – | Test process | a rule for how tests are run, not a test of its own |

Compare your answers with the generated matrix in [README.md](README.md#test-matrix).

| Code | Caption | Requirement | Suggested test | Tested in |
|---|---|---|---|---|
{rows}
''')


if __name__ == '__main__':
    found = scan_tests()
    names = update_requirement_files(found)
    missing = update_readme(found, names)
    update_student_matrix(names)
    links = sum(len(v) for v in found.values())
    print(f'{len(names)} requirements, {links} requirement-test links, untested: {", ".join(missing) or "none"}')
