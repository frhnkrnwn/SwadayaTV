# Started by class name from the shell (app_process -cp <apk> ... MediaHelper), never referenced.
-keep class com.swadaya.tv.MediaHelper {
    public static void main(java.lang.String[]);
}
# JNI callbacks invoked by the local traffic monitor.
-keep class com.swadaya.tv.TrafficVpnService { *; }
