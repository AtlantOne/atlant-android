# Contributing to Atlant

Use an isolated server and disposable profiles for development. Live network
tests are opt-in; ordinary tests must not connect to production VPN accounts.

Before submitting a change:

1. Explain the observed problem and expected behavior, including platform and transport.
2. Run the relevant module tests. Protocol changes also require interoperability
   and negative authentication tests; document any wire compatibility change.
3. Run UI tests with `node --test tests/ui/*.test.cjs` for interface changes.
4. Run `python3 -m unittest discover -s management -p 'test_*.py'` for control changes.
5. Never commit deployment keys, credentials, raw traffic captures, databases or
   subscription links. Run `python3 scripts/release_audit.py --workspace`.

Include test results and known limitations in pull requests. Use synthetic data
in issues. Report vulnerabilities through the channel described in SECURITY.md.

Original contributions follow the license of their directory; see LICENSING.md.
Keep upstream notices intact when modifying vendored code.
