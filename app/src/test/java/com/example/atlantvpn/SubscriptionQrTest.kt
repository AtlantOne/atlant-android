package com.example.atlantvpn

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionQrTest {
    @Test fun linksRoundTripWithoutNetworkOrTruncation() {
        for (url in listOf("https://vpn.example.test/sub/" + "a".repeat(43), "https://例子.测试/sub/" + "b".repeat(43), "https://vpn.example.test/" + "a".repeat(1200))) {
            val matrix = subscriptionQrMatrix(url)
            val pixels = IntArray(matrix.width * matrix.height) { i -> if (matrix[i % matrix.width, i / matrix.width]) -0x1000000 else -1 }
            val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels)))
            assertEquals(url, QRCodeReader().decode(bitmap).text)
        }
    }
    @Test(expected = IllegalArgumentException::class) fun oversizedLinkIsRejected() { subscriptionQrMatrix("a".repeat(2049)) }
}
