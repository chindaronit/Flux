package com.omnipad.other

internal const val ATTACHMENT_HOST = "omnipad.local"
internal const val LEGACY_ATTACHMENT_HOST = "flux.local"

internal fun isAttachmentHost(scheme: String?, host: String?): Boolean =
    scheme == "https" && (host == ATTACHMENT_HOST || host == LEGACY_ATTACHMENT_HOST)
