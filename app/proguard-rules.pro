# Project-specific ProGuard rules. See https://developer.android.com/build/shrink-code

# Play In-App Review (review-ktx) references a Play services annotation that is compile-time only.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
