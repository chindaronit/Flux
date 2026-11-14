package com.omnipad.other

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentLinksTest {
    @Test
    fun recognizesCurrentAndLegacyHttpsHosts() {
        assertTrue(isAttachmentHost("https", "omnipad.local"))
        assertTrue(isAttachmentHost("https", "flux.local"))
        assertFalse(isAttachmentHost("http", "flux.local"))
        assertFalse(isAttachmentHost("https", "flux.local.example.com"))
        assertFalse(isAttachmentHost("https", null))
    }
}
