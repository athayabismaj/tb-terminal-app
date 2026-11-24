package com.tbterminal.app.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CloudinaryImageUrlTest {

    @Test
    fun `transform inserts preset right after image upload marker for Cloudinary URLs`() {
        val raw = "https://res.cloudinary.com/tbterminal/image/upload/v1700000123/tbterminal/products/semen.jpg"

        assertEquals(
            "https://res.cloudinary.com/tbterminal/image/upload/c_fill,w_160,h_160,q_auto,f_webp/v1700000123/tbterminal/products/semen.jpg",
            CloudinaryImageUrl.transform(raw, ImageSize.THUMB_SMALL)
        )
        assertEquals(
            "https://res.cloudinary.com/tbterminal/image/upload/c_fill,w_320,h_320,q_auto,f_webp/v1700000123/tbterminal/products/semen.jpg",
            CloudinaryImageUrl.transform(raw, ImageSize.THUMB_MEDIUM)
        )
        assertEquals(
            "https://res.cloudinary.com/tbterminal/image/upload/c_limit,w_1080,q_auto,f_webp/v1700000123/tbterminal/products/semen.jpg",
            CloudinaryImageUrl.transform(raw, ImageSize.FULL)
        )
    }

    @Test
    fun `transform does not double insert directive when already transformed`() {
        val already = "https://res.cloudinary.com/tbterminal/image/upload/c_fill,w_160,h_160,q_auto,f_webp/v1/products/abc.jpg"
        assertEquals(already, CloudinaryImageUrl.transform(already, ImageSize.FULL))
    }

    @Test
    fun `transform leaves non-Cloudinary URLs unchanged and handles null or blank`() {
        val other = "https://example.com/image/upload/products/abc.jpg"
        assertEquals(other, CloudinaryImageUrl.transform(other, ImageSize.THUMB_SMALL))
        assertNull(CloudinaryImageUrl.transform(null, ImageSize.FULL))
        assertNull(CloudinaryImageUrl.transform("   ", ImageSize.FULL))
    }
}
