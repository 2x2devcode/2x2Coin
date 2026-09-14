#include <jni.h>
#include <string>
#include <vector>

namespace {
jobjectArray makePinArray(JNIEnv* env, const std::vector<std::string>& pins) {
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(static_cast<jsize>(pins.size()), stringClass, nullptr);
    for (size_t i = 0; i < pins.size(); ++i) {
        jstring value = env->NewStringUTF(pins[i].c_str());
        env->SetObjectArrayElement(array, static_cast<jsize>(i), value);
        env->DeleteLocalRef(value);
    }
    return array;
}
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_x2xcoin_wallet_security_NativePinProvider_getApiPinnedHashes(
        JNIEnv* env,
        jobject /* thiz */) {
    // SHA-256 SPKI hex. OkHttp accepts a match on any pin in the presented chain.
    // Leaf (server.2x2coin.com) + Let's Encrypt YE1 intermediate.
    // Renew with `certbot --reuse-key` so the leaf pin survives certificate rotation.
    return makePinArray(env, {
            "f74d430abec99f630e85d292a1ed7eb44fe2b4cd1035ed1b1ab73becd8334ef9",
            "6ebcefb4210b088654a38b03fea3d7d1c711b4fb1ddc363a45f9b1a4e53da01e"
    });
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_x2xcoin_wallet_security_NativePinProvider_getExplorerPinnedHashes(
        JNIEnv* env,
        jobject /* thiz */) {
    // Leaf (serverexplorer.2x2coin.com) + Let's Encrypt YE1 intermediate.
    return makePinArray(env, {
            "c3788c5e66a19c2549db584ed646a9b286fc346790f469563a99828d8f3dfdd5",
            "6ebcefb4210b088654a38b03fea3d7d1c711b4fb1ddc363a45f9b1a4e53da01e"
    });
}
