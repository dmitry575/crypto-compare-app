package com.cryptocompare.data.mapper

import com.cryptocompare.model.auth.AuthUser
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser

fun FirebaseUser.toAuthUser(): AuthUser =
    AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
        hasPasswordProvider =
            providerData.any { userInfo -> userInfo.providerId == EmailAuthProvider.PROVIDER_ID },
    )
