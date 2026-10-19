# 02 · Login and session

## Promise
I enter my server URL, username and password once. The app logs in, remembers
me, and stays logged in across restarts without asking again. If the session
expires, it silently re-authenticates; I never see a login screen again unless
I log out or the password changes.

## Acceptance
- [x] Auth screen: server URL, username, password, Connect; errors shown inline
      (unreachable server, wrong credentials, non-JSON response)
- [x] Handshake exactly as `docs/AMPACHE.md` (sha256(user + sha256(pw)),
      version 6.0.0); token and expiry stored
- [x] `Settings` (DataStore) holds server URL, username, **password hash** (not
      the password), token, expiry
- [x] `Session` re-handshakes transparently on `Session Expired` / 401 / 403 and
      retries the failed call once
- [x] `ping` on return to foreground when < 20 min of session remain, throttled
      to once a minute
- [x] Restart the app → lands past the auth screen, no network needed for that
- [x] Log out clears everything and returns to the auth screen
- [x] Unit tests: handshake hash against a known vector; URL building; expiry
      maths; re-auth-then-retry against MockWebServer

- [x] Real server: log in, restart, log out — on the phone

## Notes
Verified 2026-10-19 on the phone (1.1): wrong password and bad hosts show
inline errors; real server logs in; restart in airplane mode lands logged in;
log out returns to an empty auth screen. Follow-ups in 1.2: autocorrect off in
the URL and username fields (Gboard put a space after each "."), whitespace
stripped from the URL, and plain error sentences instead of the server's text.

Where things are: `ampache/Session.kt` (state, re-auth, ping policy),
`ampache/AmpacheClient.kt` (handshake, ping, error mapping),
`ampache/AmpacheException.kt` (the user-facing messages), `data/Settings.kt`,
`ui/screens/auth/`. The logged-in screen is a placeholder in `MainActivity`
with a Log out button until task 07. Session rules are D20.

The demo server (`https://demo.ampache.dev`, demo / demodemo) is fine for the
happy path; the real server must be tested before the step is done.
