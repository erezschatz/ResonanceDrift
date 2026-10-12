`debug.keystore` is the shared **debug** signing key — committed deliberately so
that builds from the Mac and the Windows machine install over each other on the
phone. Alias `androiddebugkey`, password `android` (the Android defaults).
It can only sign debug builds; it is not a secret worth protecting.

The **release** key is never in this repo. See SETUP.md §6.
