# Log関数削除
-assumenosideeffects public class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

#アプリ設定、Enum名をそのままキー名に使用しているため
-keep enum * {*;}

#java.lang.IncompatibleClassChangeError対応
-keep class * extends jp.okiislandsh.library.android.IConverter

#デバッグ用
#-dontobfuscate #シンプルな難読化無効

#NavigationのレイアウトにFragmentContainerViewを使用した場合、NavHostFragmentがClassNotFoundException
-keep class * extends androidx.fragment.app.Fragment{}
#noinspection ShrinkerUnresolvedReference
-keep class androidx.navigation.** {*;}

#UMP 2.0でリリースビルドでクラッシュするバグの対応
#https://neet-rookie.hatenablog.com/entry/2021/08/17/131116
#https://stackoverflow.com/questions/68639571/user-messaging-platform-2-0-0-release-crashes
-keep class com.google.android.gms.internal.consent_sdk.** { <fields>; }

#Android Room DataBase
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
