-classobfuscationdictionary obfuscation/classnames.txt
-obfuscationdictionary obfuscation/members.txt

-overloadaggressively
-dontusemixedcaseclassnames
-repackageclasses ''

-keep public class com.xapps.media.xmusic.** extends android.app.Activity
-keep public class com.xapps.media.xmusic.** extends android.app.Service
-keep public class com.xapps.media.xmusic.** extends android.content.BroadcastReceiver
-keep public class com.xapps.media.xmusic.** extends android.app.Application
-keep public class com.xapps.media.xmusic.** extends android.content.ContentProvider