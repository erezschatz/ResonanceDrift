# 02 · Login and session

## Promise
I enter my server URL, username and password once. The app logs in, remembers
me, and stays logged in across restarts without asking again. If the session
expires, it silently re-authenticates; I never see a login screen again unless
I log out or the password changes.

## Acceptance
- [ ] Auth screen: server URL, username, password, Connect; errors shown inline
      (unreachable server, wrong credentials, non-JSON response)
- [ ] Handshake exactly as `docs/AMPACHE.md` (sha256(user + sha256(pw)),
      version 6.0.0); token and expiry stored
- [ ] `Settings` (DataStore) holds server URL, username, **password hash** (not
      the password), token, expiry
- [ ] `Session` re-handshakes transparently on `Session Expired` / 401 / 403 and
      retries the failed call once
- [ ] `ping` on return to foreground when < 20 min of session remain, throttled
      to once a minute
- [ ] Restart the app → lands past the auth screen, no network needed for that
- [ ] Log out clears everything and returns to the auth screen
- [ ] Unit tests: handshake hash against a known vector; URL building; expiry
      maths; re-auth-then-retry against MockWebServer

## Notes
The demo server (`https://demo.ampache.dev`, demo / demodemo) is fine for the
happy path; the real server must be tested before the step is done.
