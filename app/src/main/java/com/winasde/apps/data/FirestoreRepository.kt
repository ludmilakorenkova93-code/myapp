package com.winasde.apps.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getWebViewUrl(): String? = suspendCancellableCoroutine { cont ->
        firestore.collection("config").document("app").get()
            .addOnSuccessListener { document ->
                val url = document?.getString("url")
                if (cont.isActive) cont.resume(url)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }
}
