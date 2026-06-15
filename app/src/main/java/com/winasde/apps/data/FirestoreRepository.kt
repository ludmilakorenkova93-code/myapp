package com.winasde.apps.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirestoreRepository {
    suspend fun getWebViewUrl(): String? = suspendCancellableCoroutine { cont ->
        try {
            FirebaseFirestore.getInstance()
                .collection("config")
                .document("app")
                .get()
                .addOnSuccessListener { document ->
                    val url = document?.getString("url")
                    if (cont.isActive) cont.resume(url)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        } catch (_: Throwable) {
            if (cont.isActive) cont.resume(null)
        }
    }
}
