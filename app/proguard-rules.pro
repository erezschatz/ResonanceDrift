# Project-specific R8 rules. Media3, Room, OkHttp, Coil and kotlinx.serialization
# all ship their own consumer rules; nothing is needed here until something breaks
# in a release build. If it does, add the rule *and* a comment saying why.

# The playback log prints each exception's class name (PlaybackEvents.causeChain);
# shrunk, Media3's HttpDataSourceException read "n41" (1.0 on the phone, task 14).
-keepnames class * extends java.lang.Throwable
