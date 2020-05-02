#include <jni.h>
#include <string>
#include <sstream>
#include <playapi/device_info.h>
#include <playapi/login.h>
#include <playapi/file_login_cache.h>
#include <playapi/util/config.h>
#include <playapi/util/http.h>
#include <playapi/api.h>
#include "../../../../deps/google-play-api/src/config.h"

JavaVM *jvm;

class JVMAttacher {
private:
    JNIEnv *envPtr;
    bool needsDetach = false;

public:
    JVMAttacher() {
        int ret = jvm->GetEnv((void **) &envPtr, JNI_VERSION_1_4);
        if (ret == JNI_EDETACHED) {
            needsDetach = true;
            if (jvm->AttachCurrentThread(&envPtr, NULL) != 0)
                throw std::runtime_error("AttachCurrentThread failed");
        } else if (ret != JNI_OK) {
            throw std::runtime_error("GetEnv failed");
        }
    }

    ~JVMAttacher() {
        if (needsDetach) {
            jvm->DetachCurrentThread();
        }
    }

    JNIEnv *env() const {
        return envPtr;
    }
};

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    jvm = vm;
    return JNI_VERSION_1_4;
}


struct NativePlayApi {
    playapi::device_info device;
    playapi::file_login_cache loginCache;
    playapi::login_api loginApi;
    playapi::api api;
    std::unique_ptr<device_config> deviceConfig;

    NativePlayApi(std::string dataPath) : loginCache(dataPath + "login_cache.json"), loginApi(device, loginCache), api(device) {
    }
};

static std::string stringFromJava(JNIEnv *env, jstring str) {
    const char *cstr = env->GetStringUTFChars(str, nullptr);
    std::string ret (cstr);
    env->ReleaseStringUTFChars(str, cstr);
    return ret;
}


extern "C" JNIEXPORT void JNICALL
Java_io_mrarm_mcversion_PlayApi_initCurlSsl(JNIEnv* env, jclass, jstring jCaPath) {
    std::string caPath = stringFromJava(env, jCaPath);
    playapi::http_request::set_platform_curl_init_hook([caPath](CURL *curl) {
        curl_easy_setopt(curl, CURLOPT_CAINFO, caPath.c_str());
    });
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_mrarm_mcversion_PlayApi_init(JNIEnv *env, jclass, jstring jDataPath) {
    auto obj = new NativePlayApi(stringFromJava(env, jDataPath));
    return (jlong) (uintptr_t) obj;
}

extern "C" JNIEXPORT void JNICALL
Java_io_mrarm_mcversion_PlayApi_setDevice(JNIEnv *env, jclass, jlong jSelf, jstring jDeviceConfig,
        jstring jStatePath) {
    auto self = (NativePlayApi *) (size_t) jSelf;

    {
        std::stringstream deviceConfig(stringFromJava(env, jDeviceConfig));
        playapi::config devInfoConfig;
        devInfoConfig.load(deviceConfig);
        self->device.load(devInfoConfig);
    }

    self->deviceConfig = std::make_unique<device_config>(stringFromJava(env, jStatePath));
    self->deviceConfig->load();
    self->deviceConfig->load_device_info_data(self->device);
    self->device.generate_fields();
    self->deviceConfig->set_device_info_data(self->device);
    self->deviceConfig->save();
    self->loginApi.set_checkin_data(self->deviceConfig->checkin_data);
}

extern "C" JNIEXPORT void JNICALL
Java_io_mrarm_mcversion_PlayApi_setLoginToken(JNIEnv *env, jclass, jlong jSelf,
        jstring jId, jstring jToken) {
    auto self = (NativePlayApi *) (size_t) jSelf;

    self->loginApi.set_token(stringFromJava(env, jId), stringFromJava(env, jToken));
}

static std::function<void (std::exception_ptr)> makeErrorCallback(jobject gCallback) {
    return [gCallback](std::exception_ptr err) {
        try {
            std::rethrow_exception(err);
        } catch (std::exception &e) {
            JVMAttacher attacher;
            auto mid = attacher.env()->GetMethodID(attacher.env()->GetObjectClass(gCallback), "onError", "(Ljava/lang/String;)V");
            attacher.env()->CallVoidMethod(gCallback, mid, attacher.env()->NewStringUTF(e.what()));
            attacher.env()->DeleteGlobalRef(gCallback);
        }
    };
}

extern "C" JNIEXPORT void JNICALL
Java_io_mrarm_mcversion_PlayApi_loginWithAccessToken(JNIEnv *env, jclass, jlong jSelf,
        jstring jId, jstring jToken, jobject callback) {
    auto self = (NativePlayApi *) (size_t) jSelf;
    auto gCallback = env->NewGlobalRef(callback);

    self->loginApi.perform_with_access_token(stringFromJava(env, jToken), stringFromJava(env, jId), true)->call([self, gCallback]() {
        JVMAttacher attacher;
        auto mid = attacher.env()->GetMethodID(attacher.env()->GetObjectClass(gCallback), "onSuccess", "(Ljava/lang/String;)V");
        attacher.env()->CallVoidMethod(gCallback, mid, attacher.env()->NewStringUTF(self->loginApi.get_token().c_str()));
        attacher.env()->DeleteGlobalRef(gCallback);
    }, makeErrorCallback(gCallback));
}

static void doCheckIn(NativePlayApi *self, std::function<void ()> successCb, std::function<void (std::exception_ptr)> errCb) {
    if (self->deviceConfig->checkin_data.android_id == 0) {
        auto checkin = std::make_shared<playapi::checkin_api>(self->device);
        checkin->add_auth(self->loginApi)->call([self, checkin, successCb, errCb]() {
            checkin->perform_checkin()->call([self, checkin, successCb](auto data) {
                self->deviceConfig->checkin_data = data;
                self->deviceConfig->save();
                successCb();
            }, errCb);
        }, errCb);
    } else {
        successCb();
    }
}

static void doAuthToApi(NativePlayApi *self, std::function<void ()> successCb, std::function<void (std::exception_ptr)> errCb) {
    self->api.set_checkin_data(self->deviceConfig->checkin_data);
    self->deviceConfig->load_api_data(self->loginApi.get_email(), self->api);
    self->api.set_auth(self->loginApi)->call([self, successCb, errCb]() {
        if (self->api.device_config_token.length() == 0) {
            self->api.fetch_toc()->call([self, successCb, errCb](auto toc) {
                if (toc.payload().tocresponse().requiresuploaddeviceconfig()) {
                    self->api.upload_device_config()->call([self, successCb](auto resp) {
                        self->api.info_mutex.lock();
                        self->api.device_config_token = resp.payload().uploaddeviceconfigresponse().uploaddeviceconfigtoken();
                        self->deviceConfig->set_api_data(self->loginApi.get_email(), self->api);
                        self->api.info_mutex.unlock();

                        self->deviceConfig->save();
                        successCb();
                    }, errCb);
                } else {
                    successCb();
                }
            }, errCb);
        } else {
            successCb();
        }
    }, errCb);
}

extern "C" JNIEXPORT void JNICALL
Java_io_mrarm_mcversion_PlayApi_authToApi(JNIEnv *env, jclass, jlong jSelf, jobject callback) {
    auto self = (NativePlayApi *) (size_t) jSelf;
    auto gCallback = env->NewGlobalRef(callback);

    auto errCb = makeErrorCallback(gCallback);

    doCheckIn(self, [self, errCb, gCallback]() {
        doAuthToApi(self, [gCallback]() {
            JVMAttacher attacher;
            auto mid = attacher.env()->GetMethodID(attacher.env()->GetObjectClass(gCallback), "onSuccess", "()V");
            attacher.env()->CallVoidMethod(gCallback, mid);
            attacher.env()->DeleteGlobalRef(gCallback);
        }, errCb);
    }, errCb);
}

extern "C" JNIEXPORT void JNICALL
Java_io_mrarm_mcversion_PlayApi_requestDelivery(JNIEnv *env, jclass, jlong jSelf,
        jstring jPkg, jint version, jobject callback) {
    auto self = (NativePlayApi *) (size_t) jSelf;
    auto gCallback = env->NewGlobalRef(callback);

    self->api.delivery(stringFromJava(env, jPkg), version, std::string())->call([self, gCallback](auto resp) {
        JVMAttacher attacher;
        auto env = attacher.env();

        auto const &dd = resp.payload().deliveryresponse().appdeliverydata();
        std::vector<std::string> downloadLinks;
        std::vector<jlong> downloadSizes;
        if (dd.has_downloadurl() || dd.has_gzippeddownloadurl()) {
            downloadLinks.push_back("main");
            downloadLinks.push_back(dd.downloadurl());
            downloadLinks.push_back(dd.gzippeddownloadurl());
            downloadSizes.push_back(dd.downloadsize());
        }
        for (auto const &d : dd.splitdeliverydata()) {
            downloadLinks.push_back(d.id());
            downloadLinks.push_back(d.downloadurl());
            downloadLinks.push_back(d.gzippeddownloadurl());
            downloadSizes.push_back(d.downloadsize());
        }

        auto arr = env->NewObjectArray((jsize) downloadLinks.size(), env->FindClass("java/lang/String"), nullptr);
        for (ssize_t i = downloadLinks.size() - 1; i >= 0; --i) {
            auto ref = env->NewStringUTF(downloadLinks[i].c_str());
            env->SetObjectArrayElement(arr, (jsize) i, ref);
            env->DeleteLocalRef(ref);
        }
        auto arr2 = env->NewLongArray((jsize) downloadSizes.size());
        env->SetLongArrayRegion(arr2, 0, (jsize) downloadSizes.size(), (const jlong *) downloadSizes.data());

        auto mid = env->GetMethodID(env->GetObjectClass(gCallback), "onSuccess", "([Ljava/lang/String;[J)V");
        env->CallVoidMethod(gCallback, mid, arr, arr2);
        env->DeleteGlobalRef(gCallback);
    }, makeErrorCallback(gCallback));
}
