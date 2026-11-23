# dadb reflects over its own classes for the ADB wire protocol.
-keep class dadb.** { *; }
-keep class okio.** { *; }
-dontwarn okio.**
-dontwarn org.slf4j.**
