#include <jni.h>
#include <string>
#include <vector>

namespace {
jobjectArray makePinArray(JNIEnv* env, const std::string& pin) {
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(1, stringClass, nullptr);
    jstring value = env->NewStringUTF(pin.c_str());
    env->SetObjectArrayElement(array, 0, value);
    env->DeleteLocalRef(value);
    return array;
}
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_x2xcoin_wallet_security_NativePinProvider_getApiPinnedHashes(
        JNIEnv* env,
        jobject /* thiz */) {
    // Update with SHA-256 SPKI hex of server.2x2coin.com after TLS is deployed.
    return makePinArray(env, "");
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_x2xcoin_wallet_security_NativePinProvider_getExplorerPinnedHashes(
        JNIEnv* env,
        jobject /* thiz */) {
    // Update with SHA-256 SPKI hex of serverexplorer.2x2coin.com after TLS is deployed.
    return makePinArray(env, "");
}
