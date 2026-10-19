# 02 · Login and session

## Promise
I enter my server URL, username and password once. The app logs in, remembers
me, and stays logged in across restarts without asking again. If the session
expires, it silently re-authenticates; I never see a login screen again unless
I log out or the password changes.

## Acceptance
- [ ] Auth screen: server URL, username, password, Connect; errors shown inline
      (unreachable server, wrong credentials, non-JSON response)
- [x] Handshake exactly as `docs/AMPACHE.md` (sha256(user + sha256(pw)),
      version 6.0.0); token and expiry stored
- [x] `Settings` (DataStore) holds server URL, username, **password hash** (not
      the password), token, expiry
- [x] `Session` re-handshakes transparently on `Session Expired` / 401 / 403 and
      retries the failed call once
- [x] `ping` on return to foreground when < 20 min of session remain, throttled
      to once a minute
- [ ] Restart the app → lands past the auth screen, no network needed for that
- [ ] Log out clears everything and returns to the auth screen
- [x] Unit tests: handshake hash against a known vector; URL building; expiry
      maths; re-auth-then-retry against MockWebServer

- [ ] Real server: log in, restart, log out — on the phone

## Notes
Status 2026-10-19: code and unit tests in (24 tests green on the Mac); the
handshake form was also checked by `curl` against the demo server. Still
needs the phone: inline errors (wrong password, bad host, a non-Ampache
URL), restart without network, log out, and the real server.

Where things are: `ampache/Session.kt` (state, re-auth, ping policy),
`ampache/AmpacheClient.kt` (handshake, ping, error mapping),
`ampache/AmpacheException.kt` (the user-facing messages), `data/Settings.kt`,
`ui/screens/auth/`. The logged-in screen is a placeholder in `MainActivity`
with a Log out button until task 07. Session rules are D20.

The demo server (`https://demo.ampache.dev`, demo / demodemo) is fine for the
happy path; the real server must be tested before the step is done.
